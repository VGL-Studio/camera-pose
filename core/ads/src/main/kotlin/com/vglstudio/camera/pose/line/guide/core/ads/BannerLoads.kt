package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "Ads"

/** Who refreshes a banner once it has an ad (B-02, B-03). */
internal enum class BannerRefresh {
    /** The SDK, at the refresh rate set on the ad unit in AdMob; the app only retries after a failure. */
    BY_AD_UNIT,

    /** :core:ads, every [COLLAPSIBLE_BANNER_RELOAD_MILLIS], each time with a new collapsible request id. */
    MANUAL,
}

/**
 * [AdFormat.BANNER] and [AdFormat.BANNER_MREC] are refreshed by their ad unit, [AdFormat.BANNER_COLLAPSIBLE] by hand;
 * other formats throw.
 */
internal fun bannerRefreshOf(format: AdFormat): BannerRefresh = when (format) {
    AdFormat.BANNER, AdFormat.BANNER_MREC -> BannerRefresh.BY_AD_UNIT
    AdFormat.BANNER_COLLAPSIBLE -> BannerRefresh.MANUAL
    else -> throw IllegalArgumentException("$format is not a banner format")
}

/** One result of a banner load, sent by the app or refreshed by the SDK. */
internal sealed interface BannerLoadResult {
    data object Loaded : BannerLoadResult

    data class Failed(val code: Int) : BannerLoadResult

    /**
     * The platform asked had no ad, and the frame has another platform to ask in this same turn (plan T-071, KB-31):
     * sent again at once, not a failure.
     */
    data object TryNext : BannerLoadResult
}

/** B-03, section 11 of docs/ops/ads-business-rules.md: a collapsible banner loads again 30 s after it loaded. */
internal const val COLLAPSIBLE_BANNER_RELOAD_MILLIS = 30_000L

/**
 * The sends of one banner slot (B-02 to B-06), until cancelled or until the consent gate is closed at a send.
 *
 * - Every send (the first one, a retry, a reload by hand) goes through one step: wait for [visible], then ask
 *   [mayRequestNow]; false ends the loop with nothing sent, no failure and no retry (S-15). The slot starts a new
 *   loop when the gate opens again.
 * - [view] results are collected from before the first send; a result that came before this loop is dropped, and so
 *   is one still queued when a new request goes out.
 * - Loaded: the failure count goes back to 0 and [onLoaded] runs. [BannerRefresh.BY_AD_UNIT] sends nothing more (the
 *   SDK refreshes, its results come through [BannerAdView.results] too); [BannerRefresh.MANUAL] sends again
 *   [COLLAPSIBLE_BANNER_RELOAD_MILLIS] later.
 * - TryNext (a frame of two platforms whose first one had no ad, plan T-071): sent again at once through the same
 *   step, with no failure counted, no [onFailed] and no wait; only a failure of every platform is Failed.
 * - Failed: [onFailed] runs, then a retry after min(n x 2 s, 10 s) for the n-th failure in a row (B-02), cut short
 *   when [retryResets] changes (S-24). The count belongs to this loop, so two slots of one placement count apart
 *   (L-068).
 * - Waits are coroutine delays: a monotonic clock, not the wall clock (AR-01). No unit id is logged.
 */
internal suspend fun runBannerLoads(
    refresh: BannerRefresh,
    view: BannerAdView,
    visible: Flow<Boolean>,
    mayRequestNow: () -> Boolean,
    retryResets: StateFlow<Int>,
    onLoaded: () -> Unit,
    logger: AppLogger,
    placementKey: String,
    onFailed: () -> Unit = {},
): Unit = coroutineScope {
    val inbox = Channel<BannerLoadResult>(Channel.UNLIMITED)
    // Undispatched: the collector subscribes before the first send, so a result given during send() is not lost.
    val collector = launch(start = CoroutineStart.UNDISPATCHED) { view.results.collect { inbox.send(it) } }

    suspend fun sendWhenAllowed(): Boolean {
        visible.first { it }
        if (!mayRequestNow()) {
            logger.info(TAG, "Banner $placementKey request blocked")
            return false
        }
        // A result still queued belongs to an earlier request.
        while (inbox.tryReceive().isSuccess) Unit
        view.send()
        return true
    }

    try {
        var failures = 0
        var sending = sendWhenAllowed()
        while (sending) {
            when (val result = inbox.receive()) {
                BannerLoadResult.Loaded -> {
                    failures = 0
                    onLoaded()
                    if (refresh == BannerRefresh.MANUAL) {
                        logger.info(TAG, "Banner $placementKey reload in $COLLAPSIBLE_BANNER_RELOAD_MILLIS ms")
                        delay(COLLAPSIBLE_BANNER_RELOAD_MILLIS)
                        sending = sendWhenAllowed()
                    }
                }

                // The next platform of the frame, at once, through the same step (S-15, KB-27, L-072).
                BannerLoadResult.TryNext -> sending = sendWhenAllowed()

                is BannerLoadResult.Failed -> {
                    failures++
                    onFailed()
                    val generation = retryResets.value
                    val wait = AdRetryPolicy.fullScreenDelayMillis(failures)
                    logger.info(
                        TAG,
                        "Banner $placementKey retry in $wait ms (failures $failures, code ${result.code})",
                    )
                    withTimeoutOrNull(wait) { retryResets.first { it != generation } }
                    sending = sendWhenAllowed()
                }
            }
        }
    } finally {
        collector.cancel()
    }
}
