package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Retries and reloads of the full-screen placements (F-05, F-06, S-15; O-05 for native full screen, question 4 of
 * plan T-021). One pending retry or reload per placement: a new one replaces it. The due call goes to [loadIfNeeded]
 * (`loadFullScreenAdIfNeeded` of T-022), which checks the gate at that time (F-04). Main thread only.
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
) {
    private val pending = mutableMapOf<AdPlacement, Job>()

    /** F-05 (O-05 for native full screen): the state to keep, and a retry scheduled at its due time. */
    fun onLoadFailed(placement: AdPlacement, code: Int): FullScreenLoadState.WaitingRetry {
        val failures = failuresInARow(placement)
        val wait = when (placement.format) {
            AdFormat.NATIVE_FULL_SCREEN -> AdRetryPolicy.nativeDelayMillis(failures, holdsAd = false)
            else -> AdRetryPolicy.fullScreenDelayMillis(failures)
        }
        logger.info(TAG, "Retry ${placement.key} in $wait ms (failures $failures, code $code)")
        val dueAt = clock.nowMillis() + wait
        schedule(placement, wait)
        return FullScreenLoadState.WaitingRetry(dueAt)
    }

    /** S-15: nothing sent, not a failure, no retry. */
    fun onSendBlocked(placement: AdPlacement): FullScreenLoadState.Idle {
        pending.remove(placement)?.cancel()
        return FullScreenLoadState.Idle
    }

    /** F-06: reload after closing or failing to show. */
    fun onClosedOrShowFailed(placement: AdPlacement) {
        val wait = AdRetryPolicy.reloadAfterCloseMillis(placement.format)
        logger.info(TAG, "Reload ${placement.key} in $wait ms")
        schedule(placement, wait)
    }

    private fun schedule(placement: AdPlacement, waitMillis: Long) {
        pending.remove(placement)?.cancel()
        pending[placement] = scope.launch {
            delay(waitMillis)
            if (placement.format == AdFormat.NATIVE_FULL_SCREEN) foreground.first { it }
            pending -= placement
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
