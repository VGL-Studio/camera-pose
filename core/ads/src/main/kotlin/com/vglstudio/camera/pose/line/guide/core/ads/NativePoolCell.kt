package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val TAG = "Ads"

/** P-06: how often a cell with no ad asks its pool again. */
internal object PoolCellPolicy {
    /** While an ad may still come (a load in flight, or one can be sent). */
    const val RETRY_MILLIS = 500L

    /** While the pool is exhausted (P-05). */
    const val EXHAUSTED_RETRY_MILLIS = 2_000L
}

/**
 * The states of one cell of [pool] on screen (P-06): Loading, then the ad claimed for [holder]. With no ad, asks
 * again every 500 ms, or every 2 s while the pool is exhausted, when it shows Unavailable (once); an ad that comes
 * later is still shown. When the collection ends (the cell left the screen) the ad goes back to the pool (P-01,
 * P-01a); the pool, not the cell, destroys it.
 */
internal fun pooledCellStates(
    pool: PooledNativeAds,
    holder: AdPlacement,
    logger: AppLogger,
    placementKey: String,
): Flow<NativeAdState> = flow {
    emit(NativeAdState.Loading)
    var unavailableShown = false
    while (true) {
        val ad = pool.claim(holder)
        if (ad != null) {
            try {
                emit(NativeAdState.Loaded(ad))
                awaitCancellation()
            } finally {
                pool.release(ad)
                logger.info(TAG, "Native $placementKey cell left: ad back to the pool")
            }
        }
        if (pool.exhausted) {
            if (!unavailableShown) {
                unavailableShown = true
                logger.info(TAG, "Native $placementKey cell: pool exhausted")
                emit(NativeAdState.Unavailable)
            }
            delay(PoolCellPolicy.EXHAUSTED_RETRY_MILLIS)
        } else {
            delay(PoolCellPolicy.RETRY_MILLIS)
        }
    }
}

/**
 * The `ad_opportunity` of one cell (P-07): `shown` the first time it has an ad while viewed; `not_ready` when it
 * leaves after it was viewed with no ad; nothing when it was never viewed. One result at most per cell (each time the
 * cell enters the composition is a new cell).
 */
internal class PoolCellOpportunity(private val record: (result: String) -> Unit) {
    private var viewedOnce = false
    private var recorded = false

    fun onState(hasAd: Boolean, viewed: Boolean) {
        if (!viewed) return
        viewedOnce = true
        if (hasAd && !recorded) {
            recorded = true
            record("shown")
        }
    }

    fun onLeave() {
        if (viewedOnce && !recorded) {
            recorded = true
            record("not_ready")
        }
    }
}
