package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

private const val TAG = "Ads"

/** The result of one [NativeAdSource.claim]. */
internal sealed interface NativeClaim {
    /** The claimer owns [ad] now and destroys it once it leaves the screen. */
    data class Ad(val ad: LoadedNativeAd) : NativeClaim

    /** No ad came (error, timeout, source closed). */
    data object NoAd : NativeClaim

    /** The gate was closed right before the send, or the placement is off or consent withdrawn: not a failure. */
    data object Blocked : NativeClaim
}

/** One load of a [NativeAdSource]: its result, and the cell its metrics read the placement from (PL-03). */
internal class NativeSourceLoad(val state: NativeAdState, val holder: PlacementCell = PlacementCell())

/** The slot state of a claim; null when the claim was blocked (like "request blocked" of [retryingNativeAdStates]). */
internal fun NativeClaim.toStateOrNull(): NativeAdState? = when (this) {
    is NativeClaim.Ad -> NativeAdState.Loaded(ad)
    NativeClaim.NoAd -> NativeAdState.Unavailable
    NativeClaim.Blocked -> null
}

/**
 * Times of the native single-ad rules (docs/ops/ads-business-rules.md, O-04, CL-02, section 5.7). The refresh interval
 * is a key of each layer ([AdsController.reloadIntervalMillis], KB-34).
 */
internal object NativeAdPolicy {
    /** O-04: a native ad is not shown from 55 minutes after it was loaded. */
    const val MAX_AGE_MILLIS = 55 * 60 * 1_000L

    /** CL-02: a refresh waits at most 10 s for an ad, then keeps the old one. */
    const val REFRESH_WAIT_MILLIS = 10_000L

    /** Section 5.7: the native bottom slot refreshes this long after the collapsible layer above it. */
    const val BOTTOM_REFRESH_DELAY_AFTER_COLLAPSIBLE_MILLIS = 500L
}

/**
 * The stock of one native format and ad unit (plan T-086, KB-33), shared by every slot of the placements that use it,
 * across screens (plan T-040 for the rules of one source):
 *
 * - O-01: each claim gets an ad of its own; taking the pending ad (the ad of the stock) starts the next load
 *   (`PRELOAD`), whose ad waits for the next claim. A claim with an ad in the stock gets it at once.
 * - At most one load in flight; a claimer that comes while one flies waits for it. Loads run in [scope], not in the
 *   claimer: a claimer cancelled meanwhile leaves the ad to the stock as the pending ad.
 * - O-02: of claimers waiting on one load, the first gets the ad; each other one gets exactly one more load (with its
 *   own use and placement), one at a time; that load failing gives it [NativeClaim.NoAd].
 * - O-03 and its exception for the stock: [attach] and [detach] count the slots on screen; the last slot leaving
 *   keeps the pending ad and the load in flight, whose ad becomes the pending ad. [shutDown] (consent withdrawn, every
 *   placement of the stock off, or the layer off) closes the stock: pending ad destroyed, load in flight cancelled (a
 *   late ad is destroyed), waiters get NoAd at once, nothing retried; it moves [closed]. A later [fill] or claim opens
 *   it again.
 * - [fill]: the stock loads one ad of its own (`PRELOAD`) when it has none and none is in flight. The stock itself,
 *   [fill] and the next load after a claim, only loads while [mayFill]; a failed load of its own is not retried.
 * - O-04: the stock drops its pending ad 55 minutes after its load, by a timer in [scope], then fills again; a
 *   claim checks the age on [clock] too, so an ad 55 minutes old is never given.
 * - PL-03: each load comes with a [PlacementCell], set to the placement of the slot the ad is handed to.
 * - O-09: a load that throws (not a cancellation) is a failed load, logged by class name only.
 * - Every send asks [sendGate] right before it goes (L-072); a closed gate gives [NativeClaim.Blocked].
 *
 * Main thread only ([scope] runs on the main thread).
 */
internal class NativeAdSource(
    private val load: suspend (NativeLoadUse, AdPlacement?) -> NativeSourceLoad,
    private val sendGate: suspend () -> Boolean,
    private val clock: MonotonicClock,
    private val scope: CoroutineScope,
    private val logger: AppLogger,
    /** The log key: format code and number of the stock, never the ad unit (ADR-0019). */
    private val placementKey: String,
    /** Latest switch of the placement of a claim and consent; false makes the claim [NativeClaim.Blocked] (L-061). */
    private val allowed: (AdPlacement) -> Boolean = { true },
    /** Whether the stock may start a load of its own; [slotOnScreen] is true while a slot of it is attached. */
    private val mayFill: (slotOnScreen: Boolean) -> Boolean = { true },
) {
    private class Waiter(
        val use: NativeLoadUse,
        val placement: AdPlacement,
        val continuation: CancellableContinuation<NativeClaim>,
    ) {
        /** Lost a load to another claimer: the load now flying, or the next one, is its one retry (O-02). */
        var lost = false
    }

    private class Pending(val ad: LoadedNativeAd, val holder: PlacementCell, val loadedAtMillis: Long) {
        /** The timer of O-04 for this ad. */
        var expiry: Job? = null
    }

    private val closedCount = MutableStateFlow(0L)

    /** How many times the stock was shut down (consent withdrawn, every placement of it off, the layer off). */
    val closed: StateFlow<Long> = closedCount.asStateFlow()

    private var slots = 0

    /** Moves at every close, so a load of an earlier opening is never taken. */
    private var session = 0L
    private var pendingAd: Pending? = null
    private var inFlight: Job? = null
    private val waiters = ArrayDeque<Waiter>()

    val isLoading: Boolean get() = inFlight != null

    /** At least one slot of this stock is on screen. */
    val hasSlots: Boolean get() = slots > 0

    /** The ad loaded for the next claim, if any. */
    val pending: LoadedNativeAd? get() = pendingAd?.ad

    /** A slot of this stock came on screen. */
    fun attach() {
        slots++
    }

    /** A slot of this stock left the screen; the stock keeps its pending ad and its load in flight. */
    fun detach() {
        if (slots == 0) return
        slots--
        if (slots == 0) logger.info(TAG, "Native $placementKey kept after last slot left")
    }

    /** Consent withdrawn, every placement of the stock off, or the layer off: close, and the slots stop. */
    fun shutDown() {
        close()
        closedCount.value += 1
    }

    /**
     * The slots of [placement] stop (it was switched off, the stock stays open for another placement): their waiting
     * claims get [NativeClaim.Blocked] at once; the load they waited on goes on, its ad for the stock.
     */
    fun release(placement: AdPlacement) {
        val gone = waiters.filter { it.placement == placement }
        if (gone.isEmpty()) return
        waiters.removeAll(gone)
        gone.forEach { it.continuation.resume(NativeClaim.Blocked) }
    }

    /** One ad for one showing by a slot of [placement]; see the class rules. */
    suspend fun claim(use: NativeLoadUse, placement: AdPlacement): NativeClaim {
        if (!allowed(placement)) return NativeClaim.Blocked
        takeFresh()?.let { kept ->
            kept.holder.value = placement
            logger.info(TAG, "Native $placementKey claimed")
            startNextLoad()
            return NativeClaim.Ad(kept.ad)
        }
        return suspendCancellableCoroutine { continuation ->
            val waiter = Waiter(use, placement, continuation)
            waiters.addLast(waiter)
            continuation.invokeOnCancellation { waiters.remove(waiter) }
            if (inFlight == null) startLoad(use, placement)
        }
    }

    /** Loads one ad of the stock's own when it has none, none is in flight and [mayFill] allows it. */
    fun fill() {
        pendingAd?.let { if (isExpired(it)) expire(it) else return }
        if (inFlight != null || !mayFill(slots > 0)) return
        logger.info(TAG, "Native $placementKey preload")
        startLoad(NativeLoadUse.PRELOAD, null)
    }

    /** No load in flight: the preload group may move on (S-22). */
    fun settled(): Boolean = inFlight == null

    private fun isExpired(kept: Pending): Boolean =
        clock.nowMillis() - kept.loadedAtMillis >= NativeAdPolicy.MAX_AGE_MILLIS

    private fun takeFresh(): Pending? {
        val kept = pendingAd ?: return null
        if (isExpired(kept)) {
            expire(kept)
            return null
        }
        pendingAd = null
        kept.expiry?.cancel()
        return kept
    }

    /** O-04: drops [kept], the pending ad, then fills again (a claimer that waits for it gets that load). */
    private fun expire(kept: Pending) {
        kept.expiry?.cancel()
        pendingAd = null
        kept.ad.destroy()
        logger.info(TAG, "Native $placementKey expired")
        fill()
    }

    private fun startNextLoad() {
        if (inFlight != null || waiters.isNotEmpty() || !mayFill(slots > 0)) return
        logger.info(TAG, "Native $placementKey next load")
        startLoad(NativeLoadUse.PRELOAD, null)
    }

    private fun startLoad(use: NativeLoadUse, placement: AdPlacement?) {
        if (inFlight != null) return
        val opening = session
        // Started after it is kept in inFlight: a load that ends without suspending still clears it.
        val job = scope.launch(start = CoroutineStart.LAZY) {
            val result = sendAndLoad(use, placement)
            val ad = (result?.state as? NativeAdState.Loaded)?.ad
            if (session != opening) {
                ad?.let(::destroyLate)
                return@launch
            }
            inFlight = null
            when {
                result == null -> blockAll()
                ad != null -> deliver(ad, result.holder, clock.nowMillis())
                else -> failed()
            }
        }
        inFlight = job
        job.start()
    }

    /** The load of [use] through the gate; null when the gate is closed. Never throws but for cancellation. */
    private suspend fun sendAndLoad(use: NativeLoadUse, placement: AdPlacement?): NativeSourceLoad? = try {
        if (sendGate()) load(use, placement) else null
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Never the message of the error: it may hold anything (O-09).
        logger.info(TAG, "Native $placementKey load failed: ${e.javaClass.simpleName}")
        NativeSourceLoad(NativeAdState.Unavailable)
    }

    private fun deliver(ad: LoadedNativeAd, holder: PlacementCell, loadedAtMillis: Long) {
        val head = waiters.removeFirstOrNull()
        if (head == null) {
            keep(ad, holder, loadedAtMillis)
            return
        }
        holder.value = head.placement
        // Handed over and removed in one step; a claimer cancelled before it resumes gives the ad back.
        head.continuation.resume(NativeClaim.Ad(ad)) { _, _, _ -> deliver(ad, holder, loadedAtMillis) }
        logger.info(TAG, "Native $placementKey claimed")
        if (waiters.isEmpty()) {
            startNextLoad()
            return
        }
        waiters.forEach { it.lost = true }
        logger.info(TAG, "Native $placementKey claim lost, retry once")
        waiters.first().let { startLoad(it.use, it.placement) }
    }

    private fun failed() {
        // The first waiter, and every one that has not lost yet, waited on this very load.
        val head = waiters.firstOrNull() ?: return
        val done = waiters.filter { it === head || !it.lost }
        waiters.removeAll(done)
        done.forEach { it.continuation.resume(NativeClaim.NoAd) }
        waiters.firstOrNull()?.let { startLoad(it.use, it.placement) }
    }

    private fun blockAll() {
        val all = waiters.toList()
        waiters.clear()
        all.forEach { it.continuation.resume(NativeClaim.Blocked) }
    }

    /** [ad] becomes the pending ad, with its O-04 timer; one pending ad at most. */
    private fun keep(ad: LoadedNativeAd, holder: PlacementCell, loadedAtMillis: Long) {
        pendingAd?.let {
            it.expiry?.cancel()
            it.ad.destroy()
        }
        val kept = Pending(ad, holder, loadedAtMillis)
        pendingAd = kept
        val wait = NativeAdPolicy.MAX_AGE_MILLIS - (clock.nowMillis() - loadedAtMillis)
        kept.expiry = scope.launch {
            delay(wait)
            if (pendingAd === kept) expire(kept)
        }
    }

    private fun destroyLate(ad: LoadedNativeAd) {
        ad.destroy()
        logger.info(TAG, "Native $placementKey late ad destroyed")
    }

    private fun close() {
        session++
        inFlight?.cancel()
        inFlight = null
        pendingAd?.let {
            it.expiry?.cancel()
            it.ad.destroy()
        }
        pendingAd = null
        val all = waiters.toList()
        waiters.clear()
        all.forEach { it.continuation.resume(NativeClaim.NoAd) }
        logger.info(TAG, "Native $placementKey source closed")
    }
}
