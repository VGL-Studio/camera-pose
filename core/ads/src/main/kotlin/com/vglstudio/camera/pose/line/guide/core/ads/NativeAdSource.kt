package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
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

/** The slot state of a claim; null when the claim was blocked (like "request blocked" of [retryingNativeAdStates]). */
internal fun NativeClaim.toStateOrNull(): NativeAdState? = when (this) {
    is NativeClaim.Ad -> NativeAdState.Loaded(ad)
    NativeClaim.NoAd -> NativeAdState.Unavailable
    NativeClaim.Blocked -> null
}

/** Times of the native single-ad rules (docs/ops/ads-business-rules.md, O-04, CL-02, section 5.7). */
internal object NativeAdPolicy {
    /** O-04: a native ad is not shown from 55 minutes after it was loaded. */
    const val MAX_AGE_MILLIS = 55 * 60 * 1_000L

    /** CL-02: a single-ad native slot replaces its ad every 60 s in foreground. */
    const val REFRESH_INTERVAL_MILLIS = 60_000L

    /** CL-02: a refresh waits at most 10 s for an ad, then keeps the old one. */
    const val REFRESH_WAIT_MILLIS = 10_000L

    /** Section 5.7: the native bottom slot refreshes this long after the collapsible layer above it. */
    const val BOTTOM_REFRESH_DELAY_AFTER_COLLAPSIBLE_MILLIS = 500L
}

/**
 * The single-ad source of one native placement (and format), shared by every slot of it on screen (plan T-040):
 *
 * - O-01: each claim gets an ad of its own; taking the pending ad starts the next load (`REFRESH`), whose ad waits
 *   for the next claim.
 * - At most one load in flight; a claimer that comes while one flies waits for it. Loads run in [scope], not in the
 *   claimer: a claimer cancelled meanwhile leaves the ad to the source as the pending ad.
 * - O-02: of claimers waiting on one load, the first gets the ad; each other one gets exactly one more load (with its
 *   own use), one at a time; that load failing gives it [NativeClaim.NoAd].
 * - O-03: [attach] and [detach] count the slots on screen; at 0 the source closes: pending ad destroyed, load in
 *   flight cancelled (a late ad is destroyed), waiters get NoAd at once, nothing retried. [shutDown] does the same
 *   when the placement is switched off or consent withdrawn, and moves [closed].
 * - O-04: a pending ad 55 minutes old is destroyed instead of given.
 * - O-09: a load that throws (not a cancellation) is a failed load, logged by class name only.
 * - Every send asks [sendGate] right before it goes (L-072); a closed gate gives [NativeClaim.Blocked].
 *
 * Main thread only ([scope] runs on the main thread).
 */
internal class NativeAdSource(
    private val load: suspend (NativeLoadUse) -> NativeAdState,
    private val sendGate: suspend () -> Boolean,
    private val clock: MonotonicClock,
    private val scope: CoroutineScope,
    private val logger: AppLogger,
    private val placementKey: String,
    /** Latest switch of the placement and consent; false makes every claim [NativeClaim.Blocked] (L-061). */
    private val allowed: () -> Boolean = { true },
) {
    private class Waiter(val use: NativeLoadUse, val continuation: CancellableContinuation<NativeClaim>) {
        /** Lost a load to another claimer: the load now flying, or the next one, is its one retry (O-02). */
        var lost = false
    }

    private class Pending(val ad: LoadedNativeAd, val loadedAtMillis: Long)

    private val closedCount = MutableStateFlow(0L)

    /** How many times the source was shut down because the placement was switched off or consent withdrawn. */
    val closed: StateFlow<Long> = closedCount.asStateFlow()

    private var slots = 0

    /** Moves at every close, so a load of an earlier opening is never taken. */
    private var session = 0L
    private var pendingAd: Pending? = null
    private var inFlight: Job? = null
    private val waiters = ArrayDeque<Waiter>()

    val isLoading: Boolean get() = inFlight != null

    /** The ad loaded for the next claim, if any. */
    val pending: LoadedNativeAd? get() = pendingAd?.ad

    /** A slot of this source came on screen. */
    fun attach() {
        slots++
    }

    /** A slot of this source left the screen; the last one closes the source. */
    fun detach() {
        if (slots == 0) return
        slots--
        if (slots == 0) close()
    }

    /** The placement was switched off or consent withdrawn: close, and tell the slots refreshing to stop. */
    fun shutDown() {
        close()
        closedCount.value += 1
    }

    /** One ad for one showing; see the class rules. */
    suspend fun claim(use: NativeLoadUse): NativeClaim {
        if (!allowed()) return NativeClaim.Blocked
        takeFresh()?.let { ad ->
            logger.info(TAG, "Native $placementKey claimed")
            startNextLoad()
            return NativeClaim.Ad(ad)
        }
        return suspendCancellableCoroutine { continuation ->
            val waiter = Waiter(use, continuation)
            waiters.addLast(waiter)
            continuation.invokeOnCancellation { waiters.remove(waiter) }
            if (inFlight == null) startLoad(use)
        }
    }

    private fun takeFresh(): LoadedNativeAd? {
        val kept = pendingAd ?: return null
        pendingAd = null
        if (clock.nowMillis() - kept.loadedAtMillis < NativeAdPolicy.MAX_AGE_MILLIS) return kept.ad
        kept.ad.destroy()
        logger.info(TAG, "Native $placementKey expired")
        return null
    }

    private fun startNextLoad() {
        if (inFlight != null || slots == 0 || waiters.isNotEmpty()) return
        logger.info(TAG, "Native $placementKey next load")
        startLoad(NativeLoadUse.REFRESH)
    }

    private fun startLoad(use: NativeLoadUse) {
        if (inFlight != null) return
        val opening = session
        // Started after it is kept in inFlight: a load that ends without suspending still clears it.
        val job = scope.launch(start = CoroutineStart.LAZY) {
            val result = sendAndLoad(use)
            if (session != opening) {
                (result as? NativeAdState.Loaded)?.ad?.let(::destroyLate)
                return@launch
            }
            inFlight = null
            when (result) {
                null -> blockAll()
                is NativeAdState.Loaded -> deliver(result.ad, clock.nowMillis())
                else -> failed()
            }
        }
        inFlight = job
        job.start()
    }

    /** The load of [use] through the gate; null when the gate is closed. Never throws but for cancellation. */
    private suspend fun sendAndLoad(use: NativeLoadUse): NativeAdState? = try {
        if (sendGate()) load(use) else null
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Never the message of the error: it may hold anything (O-09).
        logger.info(TAG, "Native $placementKey load failed: ${e.javaClass.simpleName}")
        NativeAdState.Unavailable
    }

    private fun deliver(ad: LoadedNativeAd, loadedAtMillis: Long) {
        val head = waiters.removeFirstOrNull()
        if (head == null) {
            keep(ad, loadedAtMillis)
            return
        }
        // Handed over and removed in one step; a claimer cancelled before it resumes gives the ad back.
        head.continuation.resume(NativeClaim.Ad(ad)) { _, _, _ -> giveBack(ad, loadedAtMillis) }
        logger.info(TAG, "Native $placementKey claimed")
        if (waiters.isEmpty()) {
            startNextLoad()
            return
        }
        waiters.forEach { it.lost = true }
        logger.info(TAG, "Native $placementKey claim lost, retry once")
        startLoad(waiters.first().use)
    }

    private fun failed() {
        // The first waiter, and every one that has not lost yet, waited on this very load.
        val head = waiters.firstOrNull() ?: return
        val done = waiters.filter { it === head || !it.lost }
        waiters.removeAll(done)
        done.forEach { it.continuation.resume(NativeClaim.NoAd) }
        waiters.firstOrNull()?.let { startLoad(it.use) }
    }

    private fun blockAll() {
        val all = waiters.toList()
        waiters.clear()
        all.forEach { it.continuation.resume(NativeClaim.Blocked) }
    }

    private fun keep(ad: LoadedNativeAd, loadedAtMillis: Long) {
        if (slots == 0) {
            destroyLate(ad)
            return
        }
        pendingAd?.ad?.destroy()
        pendingAd = Pending(ad, loadedAtMillis)
    }

    /** An ad handed to a claimer that was cancelled before taking it. */
    private fun giveBack(ad: LoadedNativeAd, loadedAtMillis: Long) {
        if (slots == 0) destroyLate(ad) else deliver(ad, loadedAtMillis)
    }

    private fun destroyLate(ad: LoadedNativeAd) {
        ad.destroy()
        logger.info(TAG, "Native $placementKey late ad destroyed")
    }

    private fun close() {
        session++
        inFlight?.cancel()
        inFlight = null
        pendingAd?.ad?.destroy()
        pendingAd = null
        val all = waiters.toList()
        waiters.clear()
        all.forEach { it.continuation.resume(NativeClaim.NoAd) }
        logger.info(TAG, "Native $placementKey source closed")
    }
}
