package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Retries and reloads of the full-screen placements (F-05, F-06, S-15; O-05 for native full screen, question 4 of
 * plan T-021). One pending retry and one pending reload per placement, apart (plan T-070): a new retry replaces the
 * retry, a new reload replaces the reload, and a reload never cancels a retry, since with two platforms the ad of one
 * may close while the other waits for its retry. The due call goes to [loadIfNeeded] (`loadFullScreenAdIfNeeded` of
 * T-022), which checks the gate and the state of each platform at that time (F-04). Main thread only.
 *
 * Native full screen (plan T-103): its failures in a row are its own ([NativeRetryCount] per placement, not the
 * counter of the metrics), back to 1 when it loads ([onLoaded]) or when [failureResets] moves; Internet back
 * ([retryResets]) cuts its retry wait short, then [onRetryCutShort] runs once in foreground, since the slot still
 * waits for a retry that is not due.
 */
internal class FullScreenReloads(
    private val scope: CoroutineScope,
    private val clock: MonotonicClock,
    /** Read after the loading path counted the failure. */
    private val failuresInARow: (AdPlacement) -> Int,
    private val loadIfNeeded: (AdPlacement) -> Unit,
    /** Native full screen only (O-05). */
    private val foreground: StateFlow<Boolean>,
    private val logger: AppLogger,
    /** Native full screen only: moves when its failures in a row count from 1 again (O-05, plan T-103). */
    private val failureResets: StateFlow<Int> = MutableStateFlow(0),
    /** Native full screen only: read at the start of each retry wait; a new value cuts the wait short (S-24). */
    private val retryResets: () -> StateFlow<Int> = { MutableStateFlow(0) },
    /** Native full screen only: a retry wait cut short by [retryResets], once in foreground. */
    private val onRetryCutShort: (AdPlacement) -> Unit = {},
) {
    /** The retry after a failed load (F-05, O-05) waiting, per placement. */
    private val retries = mutableMapOf<AdPlacement, Job>()

    /** The reload after a close or a failed show (F-06) waiting, per placement. */
    private val reloads = mutableMapOf<AdPlacement, Job>()

    /** The failures in a row of each native full-screen placement (O-05). */
    private val nativeCounts = mutableMapOf<AdPlacement, NativeRetryCount>()

    private fun nativeCount(placement: AdPlacement): NativeRetryCount =
        nativeCounts.getOrPut(placement) { NativeRetryCount(failureResets) }

    /** F-05 (O-05 for native full screen): the state to keep, and a retry scheduled at its due time. */
    fun onLoadFailed(placement: AdPlacement, code: Int): FullScreenLoadState.WaitingRetry {
        val native = placement.format == AdFormat.NATIVE_FULL_SCREEN
        val failures = if (native) nativeCount(placement).onFailure() else failuresInARow(placement)
        val wait = if (native) {
            AdRetryPolicy.nativeDelayMillis(failures, holdsAd = false)
        } else {
            AdRetryPolicy.fullScreenDelayMillis(failures)
        }
        logger.info(TAG, "Retry ${placement.key} in $wait ms (failures $failures, code $code)")
        val dueAt = clock.nowMillis() + wait
        if (native) scheduleNativeRetry(placement, wait) else schedule(retries, placement, wait)
        return FullScreenLoadState.WaitingRetry(dueAt)
    }

    /** A native full-screen ad of [placement] loaded: its next failure waits from 5 s again (O-05). */
    fun onLoaded(placement: AdPlacement) {
        nativeCounts[placement]?.onLoaded()
    }

    /** S-15: nothing sent, not a failure, no retry and no reload. */
    fun onSendBlocked(placement: AdPlacement): FullScreenLoadState.Idle {
        retries.remove(placement)?.cancel()
        reloads.remove(placement)?.cancel()
        return FullScreenLoadState.Idle
    }

    /** F-06: reload after closing or failing to show; a retry waiting for the placement is kept (plan T-070). */
    fun onClosedOrShowFailed(placement: AdPlacement) {
        val wait = AdRetryPolicy.reloadAfterCloseMillis(placement.format)
        logger.info(TAG, "Reload ${placement.key} in $wait ms")
        schedule(reloads, placement, wait)
    }

    /** The retry of a native full-screen placement: due at [waitMillis], or sooner when Internet is back (S-24). */
    private fun scheduleNativeRetry(placement: AdPlacement, waitMillis: Long) {
        retries.remove(placement)?.cancel()
        retries[placement] = scope.launch {
            val resets = retryResets()
            val generation = resets.value
            val cutShort = withTimeoutOrNull(waitMillis) { resets.first { it != generation } } != null
            foreground.first { it }
            retries -= placement
            if (cutShort) onRetryCutShort(placement) else loadIfNeeded(placement)
        }
    }

    private fun schedule(jobs: MutableMap<AdPlacement, Job>, placement: AdPlacement, waitMillis: Long) {
        jobs.remove(placement)?.cancel()
        jobs[placement] = scope.launch {
            delay(waitMillis)
            if (placement.format == AdFormat.NATIVE_FULL_SCREEN) foreground.first { it }
            jobs -= placement
            loadIfNeeded(placement)
        }
    }

    private companion object {
        const val TAG = "Ads"
    }
}

/**
 * The failure counter slot of the preload of one full-screen placement (question 9 of plan T-021): one per placement,
 * so placements of one format preloaded together keep their failures and latency apart (L-068).
 */
internal fun preloadCounterKey(placementKey: String): String = "$PRELOAD:$placementKey"
