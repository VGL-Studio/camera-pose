package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "Ads"

/**
 * Whether a banner of [format] is requested as a collapsible banner (B-03): [AdFormat.BANNER_COLLAPSIBLE] only;
 * [AdFormat.BANNER] and [AdFormat.BANNER_MREC] are plain requests. Every banner is reloaded by :core:ads
 * ([runBannerLoads]). Other formats throw [IllegalArgumentException].
 */
internal fun bannerRequestIsCollapsible(format: AdFormat): Boolean = when (format) {
    AdFormat.BANNER, AdFormat.BANNER_MREC -> false
    AdFormat.BANNER_COLLAPSIBLE -> true
    else -> throw IllegalArgumentException("$format is not a banner format")
}

/** One result of a banner load, sent by the app or refreshed by the SDK. */
internal sealed interface BannerLoadResult {
    data object Loaded : BannerLoadResult

    /** [logMessage]: the SDK message, already safe to log ([LoadErrorLog], plan T-093), or null. */
    data class Failed(val code: Int, val logMessage: String? = null) : BannerLoadResult

    /**
     * The platform asked had no ad, and the frame has another platform to ask in this same turn (plan T-071, KB-31):
     * sent again at once, not a failure.
     */
    data object TryNext : BannerLoadResult
}

/**
 * The sends of one banner slot (B-02 to B-09, KB-34), until cancelled or until the consent gate is closed at a send.
 *
 * - Every send (the first one, a retry, a reload by hand, the next platform) goes through one step: wait for
 *   [visible], then ask [mayRequestNow]; false ends the loop with nothing sent, no failure and no retry (S-15). The
 *   slot starts a new loop when the gate opens again.
 * - [view] results are collected from before the first send; a result that came before this loop is dropped, and so
 *   is one still queued when a new request goes out.
 * - Loaded: the failure count goes back to 0, [onLoaded] runs, and the loop waits for its reload tick
 *   ([awaitReloadTick] on [reloadMillis], the interval of the key of the format, KB-34): a new value applies from the
 *   next count, 0 keeps the banner. Every banner is reloaded this way; none relies on the refresh of its ad unit.
 * - While it waits for the tick, the loop still reads the results of [view]: an ad unit that still refreshes by itself
 *   can send one. Loaded there runs [onLoaded] and keeps the count under way; Failed drops the count and goes the
 *   way of a failure below; TryNext drops the count and sends at once.
 * - TryNext (a frame of two platforms whose first one had no ad, plan T-071): sent again at once through the same
 *   step, with no failure counted, no [onFailed] and no wait; only a failure of every platform is Failed.
 * - Failed: [onFailed] runs, then a retry after min(n x 2 s, 10 s) for the n-th failure in a row (B-02), cut short
 *   when [retryResets] changes (S-24). The count belongs to this loop, so two slots of one placement count apart
 *   (L-068). Once this loop had an ad, a retry due while [reloadMillis] is 0 is not sent: the banner is kept, the
 *   count goes back to 0 and the loop waits for the next reload tick (KB-34, criteria 3, 4); before any ad it
 *   retries whatever the key. The retries of a failure that came while the loop waited for its tick (a refresh of
 *   the ad unit, not a send of :core:ads) are sent whatever the key, until the next Loaded.
 * - Waits are coroutine delays: a monotonic clock, not the wall clock (AR-01). No unit id is logged.
 */
internal suspend fun runBannerLoads(
    view: BannerAdView,
    visible: Flow<Boolean>,
    mayRequestNow: () -> Boolean,
    retryResets: StateFlow<Int>,
    reloadMillis: StateFlow<Long>,
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

    /** Starts the count to the next reload tick, from now. */
    fun startTick(): Deferred<Unit> {
        val millis = reloadMillis.value
        logger.info(
            TAG,
            if (millis > 0) "Banner $placementKey reload in $millis ms" else "Banner $placementKey reload off",
        )
        return async(start = CoroutineStart.UNDISPATCHED) { awaitReloadTick(reloadMillis) }
    }

    var tick: Deferred<Unit>? = null
    try {
        var failures = 0
        var hadAd = false
        // The failures in a row began with a result that came while the loop waited for its tick.
        var fromRefresh = false
        var sending = sendWhenAllowed()
        while (sending) {
            val waiting = tick
            // Null: the reload tick came first.
            val result: BannerLoadResult? = if (waiting == null) {
                inbox.receive()
            } else {
                select {
                    waiting.onAwait { null }
                    inbox.onReceive { it }
                }
            }
            if (result == null) {
                tick = null
                fromRefresh = false
                sending = sendWhenAllowed()
                continue
            }
            when (result) {
                BannerLoadResult.Loaded -> {
                    failures = 0
                    hadAd = true
                    fromRefresh = false
                    onLoaded()
                    // A load while the loop waits keeps the count under way (KB-34, criterion 3).
                    if (waiting == null) tick = startTick()
                }

                // The next platform of the frame, at once, through the same step (S-15, KB-27, L-072).
                BannerLoadResult.TryNext -> {
                    waiting?.cancel()
                    tick = null
                    sending = sendWhenAllowed()
                }

                is BannerLoadResult.Failed -> {
                    if (waiting != null) {
                        waiting.cancel()
                        tick = null
                        fromRefresh = true
                    }
                    failures++
                    onFailed()
                    val generation = retryResets.value
                    val wait = AdRetryPolicy.fullScreenDelayMillis(failures)
                    logger.info(
                        TAG,
                        "Banner $placementKey retry in $wait ms " +
                            "(failures $failures, code ${loadErrorText(result.code, result.logMessage)})",
                    )
                    withTimeoutOrNull(wait) { retryResets.first { it != generation } }
                    if (!fromRefresh && hadAd && reloadMillis.value == 0L) {
                        logger.info(TAG, "Banner $placementKey reload off, keeping ad")
                        failures = 0
                        tick = startTick()
                    } else {
                        sending = sendWhenAllowed()
                    }
                }
            }
        }
    } finally {
        tick?.cancel()
        collector.cancel()
    }
}
