package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * What [NativeFullScreenTurns] reads and writes in [AdsController] (plan T-026), so the turns are tested on the JVM
 * with a fake. Main thread only.
 */
internal interface NativeFullScreenPort {
    /** `is_show_native_after_inter`, with `is_show_ads` applied, read now (K-01). */
    suspend fun comboEnabled(): Boolean

    /** Raises the showing flag for the combo (F-01); false while another full-screen ad shows. */
    fun beginPresence(): Boolean

    fun endPresence()

    /** Holds the block of the ads that show by themselves (K-05, G-03 way 2). */
    fun holdResumeAds(): ForegroundAdSuppressor.Hold

    fun releaseResumeAds(hold: ForegroundAdSuppressor.Hold)

    /** True once an activity of the app is resumed again: the interstitial left the screen (K-02). */
    fun interGone(): Boolean

    /** The consent gate allows showing ads now (S-11). */
    fun consentOpen(): Boolean

    /** `load_state` of the combo slot now, for the opportunity (section 8.2). */
    fun loadStateAtOpen(): String

    /** Takes the preloaded ad of `native_combo` out of the slot; the turn owns it. */
    fun takeAd(): LoadedNativeAd?

    /** Starts a load of `native_combo` if needed; true when a load is running after the call. */
    fun ensureLoading(): Boolean

    /** The number of load results of [placement] so far (L-068). */
    fun loadGeneration(placement: AdPlacement): Int

    /** Waits for a load result of [placement] (an ad or an error) after [since]. */
    suspend fun awaitLoadResult(placement: AdPlacement, since: Int)

    /** Writes the `ads_interval` mark that closing [source] moves (I-02, I-05). */
    fun recordIntervalMark(source: AdPlacement)

    /** The `ad_opportunity` of a combo (K-07). */
    fun recordOpportunity(result: String, loadState: String)

    /** Preloads [placement] for the next time (K-06). */
    fun reload(placement: AdPlacement)

    /** `time_close_native` read now, already in 1..30 (K-02). */
    fun closeCountdownSeconds(): Long
}

/**
 * The turns of the native full-screen ad drawn by the overlay of [FullScreenAdHost] (HD-05): the combo after an
 * interstitial ([afterInterstitial], KB-16, K-01 to K-08) and the standalone turn ([showStandalone], KB-5). One turn
 * at a time for the whole process. The deadline of the countdown is kept here, on the monotonic [clock], so a
 * recreated activity does not restart it (K-02). Main thread only: [scope] dispatches on the main thread.
 */
internal class NativeFullScreenTurns(
    private val port: NativeFullScreenPort,
    private val clock: MonotonicClock,
    private val scope: CoroutineScope,
    private val logger: AppLogger,
) {
    private class Turn(
        /** The placement whose mark moves at the close: the interstitial of a combo, or the standalone placement. */
        val source: AdPlacement,
        val combo: Boolean,
        val hold: ForegroundAdSuppressor.Hold,
        val onFinished: () -> Unit,
    ) {
        val jobs = mutableListOf<Job>()
        var opened = false
        var closed = false
        var deadline = 0L
        var countdownSeconds = 0L
        var ad: LoadedNativeAd? = null
        var loadFailed = false
        var everShown = false
        var loadStateAtOpen = ""
    }

    private var turn: Turn? = null
    private val _view = MutableStateFlow<NativeFullScreenView?>(null)

    /** True while a turn is open, waiting for the interstitial or on screen, past the cap of F-02 too. */
    val busy: Boolean get() = turn != null

    /** What the overlay draws; null while no turn is open. */
    val view: StateFlow<NativeFullScreenView?> = _view.asStateFlow()

    /**
     * The completion of an interstitial turn that passed the gate (KB-16). The showing flag and the resume block are
     * taken here, on the calling stack, so no full-screen ad (F-01) and no resume ad (K-05) slips in between the
     * interstitial and the native ad. Then the key is read (K-01): off finishes at once; on waits for the
     * interstitial to leave (K-02) and opens the native ad. [onFinished] runs when the native ad closes.
     */
    fun afterInterstitial(source: AdPlacement, onFinished: () -> Unit) {
        if (turn != null || !port.beginPresence()) {
            logger.info(TAG, "Native combo skipped: busy")
            onFinished()
            return
        }
        val current = Turn(source, combo = true, hold = port.holdResumeAds(), onFinished = onFinished)
        turn = current
        current.jobs += scope.launch {
            if (!port.comboEnabled()) {
                logger.info(TAG, "Native combo skipped: off")
                turn = null
                port.releaseResumeAds(current.hold)
                port.endPresence()
                onFinished()
                return@launch
            }
            val gone = awaitInterGone(port::interGone)
            logger.info(TAG, "Native combo opened (inter gone: $gone)")
            open(current)
        }
    }

    /**
     * A standalone native full-screen turn (KB-5): the controller passed its gate, raised the showing flag and took
     * [ad]. Opens at once with the countdown, never waits for an interstitial and never shows a placeholder. Rejected,
     * finishing at once, while another turn is open.
     */
    fun showStandalone(placement: AdPlacement, ad: LoadedNativeAd, onFinished: () -> Unit) {
        if (turn != null) {
            logger.info(TAG, "Native full-screen standalone skipped: busy")
            ad.destroy()
            onFinished()
            return
        }
        val current = Turn(placement, combo = false, hold = port.holdResumeAds(), onFinished = onFinished)
        current.ad = ad
        current.everShown = true
        turn = current
        logger.info(TAG, "Native full-screen standalone opened")
        open(current)
    }

    private fun open(current: Turn) {
        current.opened = true
        // Read once, when the screen opens: a change of the key applies from the next turn (K-02).
        current.countdownSeconds = port.closeCountdownSeconds()
        current.deadline = clock.nowMillis() + current.countdownSeconds * TICK_MILLIS
        if (current.combo) {
            // Taken before any load of the combo (section 8.2).
            current.loadStateAtOpen = port.loadStateAtOpen()
            startLoad(current)
        }
        publish(current)
        current.jobs += scope.launch { countDown(current) }
    }

    /** K-04: an ad in the slot, else a load and a wait for its result; nothing to send fails at once. */
    private fun startLoad(current: Turn) {
        if (!port.consentOpen()) {
            current.loadFailed = true
            return
        }
        val ad = port.takeAd()
        if (ad != null) {
            current.ad = ad
            current.everShown = true
            return
        }
        // Taken before the load, so a result that comes right after it is not missed (L-068).
        val since = port.loadGeneration(AdPlacement.NATIVE_AFTER_INTER)
        if (!port.ensureLoading()) {
            current.loadFailed = true
            return
        }
        current.jobs += scope.launch {
            port.awaitLoadResult(AdPlacement.NATIVE_AFTER_INTER, since)
            val arrived = port.takeAd()
            if (arrived != null) {
                logger.info(TAG, "Native combo ad arrived")
                current.ad = arrived
                current.everShown = true
                publish(current)
            } else {
                onLoadFailed(current)
            }
        }
    }

    private fun onLoadFailed(current: Turn) {
        current.loadFailed = true
        publish(current)
        if (clock.nowMillis() >= current.deadline) {
            // The countdown is over already: the screen closes when the failure is known (K-04).
            logger.info(TAG, "Native combo load failed: closed at once")
            closeTurn(current)
        } else {
            logger.info(TAG, "Native combo load failed: auto close at countdown end")
        }
    }

    /** Updates the seconds at each boundary, read from the clock each time, never counted (K-02). */
    private suspend fun countDown(current: Turn) {
        val boundaries = (current.countdownSeconds - 1 downTo 0).map { current.deadline - it * TICK_MILLIS }
        for (boundary in boundaries) {
            val wait = boundary - clock.nowMillis()
            if (wait > 0) delay(wait)
            publish(current)
        }
        // K-04: a failed load closes the screen at the end of the countdown.
        if (current.loadFailed) closeTurn(current)
    }

    private fun publish(current: Turn) {
        if (turn !== current || current.closed) return
        _view.value = NativeFullScreenView(
            ad = current.ad,
            secondsLeft = countdownSecondsLeft(current.deadline, clock.nowMillis(), current.countdownSeconds),
            loadFailed = current.loadFailed,
        )
    }

    /** The close button: only once it is enabled (K-03), only once (K-04). */
    fun close() {
        val current = turn ?: return
        if (_view.value?.closeEnabled != true) return
        closeTurn(current)
    }

    /** The back button (K-08): closes when the close button would, else does nothing. */
    fun onBack() {
        if (_view.value?.backCloses == true) close()
    }

    private fun closeTurn(current: Turn) {
        if (current.closed || turn !== current) return
        current.closed = true
        // First line (I-05, K-05): the mark is written before anything else can fail.
        port.recordIntervalMark(current.source)
        try {
            turn = null
            _view.value = null
            current.jobs.forEach { it.cancel() }
            current.ad?.destroy()
        } finally {
            try {
                port.releaseResumeAds(current.hold)
                port.endPresence()
                if (current.combo) recordOpportunity(current)
                port.reload(if (current.combo) AdPlacement.NATIVE_AFTER_INTER else current.source)
                if (current.combo) {
                    logger.info(TAG, "Native combo closed (shown: ${current.everShown})")
                } else {
                    logger.info(TAG, "Native full-screen standalone closed")
                }
            } finally {
                current.onFinished()
            }
        }
    }

    /** One opportunity per combo (K-07); a failing recorder never stops the user from going on. */
    private fun recordOpportunity(current: Turn) {
        val (result, loadState) = comboOpportunity(current.everShown, current.loadStateAtOpen)
        try {
            port.recordOpportunity(result, loadState)
        } catch (e: RuntimeException) {
            logger.warn(TAG, "Native combo opportunity not recorded", e)
        }
    }

    private companion object {
        const val TAG = "Ads"
        const val TICK_MILLIS = 1_000L
    }
}
