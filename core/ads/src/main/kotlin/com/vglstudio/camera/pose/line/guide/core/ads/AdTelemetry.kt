package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.analytics.AdFunnel
import com.vglstudio.camera.pose.line.guide.core.analytics.AdFunnelEvent
import com.vglstudio.camera.pose.line.guide.core.analytics.AdMetricEvent
import com.vglstudio.camera.pose.line.guide.core.analytics.AdMetrics
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger

/**
 * Records the ad metrics of :core:ads (docs/ops/ads-business-rules.md, sections 8.1 and 8.2; plan T-023).
 *
 * - Revenue ([revenue], [revenueFallback]) is always logged, whatever the switches or the consent gate say; the
 *   placement is read when the event comes (PL-03), and a placement that cannot be read counts as none (PL-04).
 * - Lifecycle events are logged only when the switch of the format's group ([lifecycleSwitches], read at every
 *   event) is on and ads are being served ([serving]).
 * - The [counter] moves on every request and result, metrics on or off (T-021 reads it).
 * - The AppsFlyer funnel ([eligible], [apiCalled], [displayed], [interDisplayed], plan T-033) goes to [funnel] whatever
 *   the lifecycle switches or the consent gate say: its own switches are read by :core:analytics. App open and banners
 *   have no funnel ([funnelKind]).
 * - A failing [metrics] or [funnel] is swallowed and logged by class name only: the ads keep running. No message of the SDK is
 *   ever put in a param.
 */
internal class AdTelemetry(
    private val metrics: AdMetrics,
    /** The AppsFlyer funnel (plan T-033). */
    private val funnel: AdFunnel,
    /** Latest value of [AdsFlags.global], read at each event. */
    private val lifecycleSwitches: () -> AdsGlobalFlags,
    /** [AdsController.canRequestAds]. */
    private val serving: () -> Boolean,
    /** No usable Internet now. */
    private val offline: () -> Boolean,
    private val counter: LoadAttemptCounter,
    private val logger: AppLogger,
    /** Runs at every click of any format, before the metrics switches are read: the resume block (A-14, T-025). */
    private val onClicked: () -> Unit = {},
) {
    /** One paid event of the SDK (T-01). */
    fun revenue(
        format: AdFormat,
        unitId: String,
        placement: () -> AdPlacement?,
        valueMicros: Long,
        currency: String,
        adSource: String?,
        adapterClassName: String?,
    ) = logRevenue(format, unitId, placement, revenueValue(valueMicros), currency, adSource, adapterClassName, false)

    /** The zero-value impression of a native ad with no paid event 2 s after its impression (T-02). */
    fun revenueFallback(
        format: AdFormat,
        unitId: String,
        placement: () -> AdPlacement?,
        adSource: String?,
        adapterClassName: String?,
    ) = logRevenue(format, unitId, placement, 0.0, FALLBACK_CURRENCY, adSource, adapterClassName, true)

    /** The load of a slot with a failure counter: same as [requestSent], [loadSucceeded], [loadFailed]. */
    fun countedLoad(format: AdFormat, slot: String, placement: String?): LoadReport = object : LoadReport {
        override fun sent(now: Long) = requestSent(slot, now)

        override fun succeeded(now: Long) = loadSucceeded(format, slot, placement, now)

        override fun failed(code: Int, sdkMessage: String?, now: Long): String =
            loadFailed(format, slot, placement, code, sdkMessage, now)
    }

    /**
     * One request that does not go through the counter (a row of a list, like a pool: question 6, option B): no
     * `load_attempt`, latency from the send of this very request. Each call gives a new report.
     */
    fun perRequestLoad(format: AdFormat, placement: String?): LoadReport = object : LoadReport {
        @Volatile private var sentAt: Long? = null

        override fun sent(now: Long) {
            sentAt = now
        }

        override fun succeeded(now: Long) {
            if (!lifecycleOn(format)) return
            log(AdMetricEvent.LoadSuccess(format.metricCode(), placement, loadAttempt = null, latencyMs = latency(now)))
        }

        override fun failed(code: Int, sdkMessage: String?, now: Long): String {
            val label = loadStateAfterError(code, offline(), isSdkThrottledMessage(sdkMessage))
            if (lifecycleOn(format)) {
                log(
                    AdMetricEvent.LoadFail(
                        format.metricCode(),
                        placement,
                        loadAttempt = null,
                        latencyMs = latency(now),
                        loadErrorCode = code.toString(),
                        loadState = label,
                    ),
                )
            }
            return label
        }

        private fun latency(now: Long): Long? = sentAt?.let { now - it }
    }

    /** :core:ads sends a request for [slot] now. */
    fun requestSent(slot: String, now: Long) {
        counter.onRequestSent(slot, now)
    }

    fun loadSucceeded(format: AdFormat, slot: String, placement: String?, now: Long) {
        val outcome = counter.onLoaded(slot, now)
        if (!lifecycleOn(format)) return
        log(AdMetricEvent.LoadSuccess(format.metricCode(), placement, outcome.attempt, outcome.latencyMs))
    }

    /**
     * A load of [slot] failed with [code]. [sdkMessage] is only read here to tell the SDK's own throttling; it is
     * never logged or sent. Returns the `load_state` label of the error, kept by the caller for later opportunities.
     */
    fun loadFailed(
        format: AdFormat,
        slot: String,
        placement: String?,
        code: Int,
        sdkMessage: String?,
        now: Long,
    ): String {
        val outcome = counter.onFailed(slot, now)
        val label = loadStateAfterError(code, offline(), isSdkThrottledMessage(sdkMessage))
        if (lifecycleOn(format)) {
            log(
                AdMetricEvent.LoadFail(
                    format.metricCode(),
                    placement,
                    outcome.attempt,
                    outcome.latencyMs,
                    code.toString(),
                    label,
                ),
            )
        }
        return label
    }

    /** One call to show [placement]; no placement or no [result] is no opportunity (A 06:197). */
    fun opportunity(format: AdFormat, placement: AdPlacement?, result: String?, loadState: String) {
        if (placement == null || result == null || !lifecycleOn(format)) return
        log(AdMetricEvent.Opportunity(format.metricCode(), placement.key, result, loadState))
    }

    fun showFailed(format: AdFormat, placement: AdPlacement?, code: Int) {
        if (!lifecycleOn(format)) return
        log(AdMetricEvent.ShowFail(format.metricCode(), placement?.key, code.toString(), showFailReasonOf(code)))
    }

    /** A click on an ad of any format; every click callback of :core:ads comes here, so it always skips a resume. */
    fun clicked(format: AdFormat, placement: AdPlacement?) {
        try {
            onClicked()
        } catch (e: Exception) {
            // Class name only, as for the metrics: the SDK callback must not break.
            logger.warn(TAG, "Ad click not handled: ${e.javaClass.simpleName}")
        }
        if (!lifecycleOn(format)) return
        log(AdMetricEvent.Clicked(format.metricCode(), placement?.key))
    }

    /** The user earned the reward of a rewarded ad of [placement] (R-02, `ad_reward_earned`). */
    fun rewardEarned(placement: AdPlacement) {
        if (!lifecycleOn(AdFormat.REWARDED)) return
        log(AdMetricEvent.RewardEarned(AdFormat.REWARDED.metricCode(), placement.key))
    }

    /** The dialog before the rewarded ad of [placement] was shown or dismissed (`reward_dialog`, [action]). */
    fun rewardDialog(placement: AdPlacement, action: String) {
        if (!lifecycleOn(AdFormat.REWARDED)) return
        log(AdMetricEvent.RewardDialog(AdFormat.REWARDED.metricCode(), placement.key, action))
    }

    /**
     * A turn of [format] passed the gate of the app (interstitial, native) or the user tapped "Watch ad" (rewarded):
     * `af_*_ad_eligible`, with no param.
     */
    fun eligible(format: AdFormat) {
        val kind = format.funnelKind() ?: return
        logFunnel(AdFunnelEvent.Eligible(kind))
    }

    /** An ad of [format] was loaded (`af_*_api_called`); `ad_source` is the adapter class, empty when none. */
    fun apiCalled(format: AdFormat, unitId: String, adapterClassName: String?) {
        val kind = format.funnelKind() ?: return
        logFunnel(AdFunnelEvent.ApiCalled(kind, format.metricCode(), unitId, adapterClassName.orEmpty()))
    }

    /** An ad of [format] came on screen (`af_inters_displayed`, `af_*_ad_displayed`). */
    fun displayed(format: AdFormat, unitId: String, adapterClassName: String?) {
        val kind = format.funnelKind() ?: return
        logFunnel(AdFunnelEvent.Displayed(kind, format.metricCode(), unitId, adapterClassName.orEmpty()))
    }

    /** The [count]-th interstitial since install came on screen (N-05); null, a failed count, sends nothing. */
    fun interDisplayed(count: Int?) {
        if (count == null) return
        logFunnel(AdFunnelEvent.InterDisplayedTimes(count))
    }

    private fun logRevenue(
        format: AdFormat,
        unitId: String,
        placement: () -> AdPlacement?,
        value: Double,
        currency: String,
        adSource: String?,
        adapterClassName: String?,
        fallback: Boolean,
    ) {
        val placementKey = try {
            placement()?.key
        } catch (e: Exception) {
            logger.warn(TAG, "Ad revenue placement not read: ${e.javaClass.simpleName}")
            null
        }
        logger.debug(TAG, "Ad revenue ${format.metricCode()} placement=${placementKey ?: "none"} fallback=$fallback")
        log(
            AdMetricEvent.ImpressionRevenue(
                adFormat = format.metricCode(),
                adSource = adSourceOrAdmob(adSource),
                // The raw source for AppsFlyer, empty when the SDK reports none (section 8.3).
                monetizationNetwork = adSource.orEmpty(),
                adUnitName = unitId,
                currency = currency,
                value = value,
                adPlacement = placementKey,
                adapterClassName = adapterClassName,
            ),
        )
    }

    /** "Group on" (A 06:135-139): the group's switch is on and ads are being served, read again at every event. */
    private fun lifecycleOn(format: AdFormat): Boolean {
        val switches = lifecycleSwitches()
        val switchOn = when (format.metricGroup()) {
            MetricGroup.FULL_SCREEN -> switches.logFullScreenEvents
            MetricGroup.SLOT -> switches.logSlotEvents
        }
        return switchOn && serving()
    }

    private fun log(event: AdMetricEvent) {
        try {
            metrics.log(event)
        } catch (e: Exception) {
            // Class name only: the message of the error may hold anything.
            logger.warn(TAG, "Ad metric not logged: ${e.javaClass.simpleName}")
        }
    }

    private fun logFunnel(event: AdFunnelEvent) {
        try {
            funnel.log(event)
        } catch (e: Exception) {
            // Class name only, as for the metrics.
            logger.warn(TAG, "Ad funnel event not logged: ${e.javaClass.simpleName}")
        }
    }

    private companion object {
        const val TAG = "Ads"
        const val FALLBACK_CURRENCY = "USD"
    }
}

/** One load and its result, reported once each (plan T-023, question 6). */
internal interface LoadReport {
    fun sent(now: Long)
    fun succeeded(now: Long)

    /** Returns the `load_state` label of the error, like [AdTelemetry.loadFailed]. */
    fun failed(code: Int, sdkMessage: String?, now: Long): String
}

/**
 * How a native placement is loaded: one ad in its slot, one row of a list (plan T-023, question 6), a refresh of a
 * single-ad slot, which also covers the next load after a claim (plan T-040, section 8.2), or one load of a pool
 * (plan T-028, P-04, PL-02).
 */
internal enum class NativeLoadUse { SINGLE_AD, LIST_ROW, REFRESH, POOL }

/**
 * The load report of a native [placement] drawn as [format] and used as [use]; the choice of [AdsController],
 * testable without the SDK. A single ad counts `load_attempt` per placement and format ([nativeSlotKey]), so two
 * formats of one placement never share a counter (L-068). A list row, a refresh and a load of a pool get a new report
 * per request: no `load_attempt`, latency from the send of that request, and the counter is not touched.
 */
internal fun AdTelemetry.nativeLoadReport(
    placement: AdPlacement,
    use: NativeLoadUse,
    format: AdFormat = placement.format,
): LoadReport {
    // A list row keeps the placement key of its screen (question 7); a load of a pool logs `pool` (PL-02).
    val loadPlacement = loadPlacementOf(format, placement)
    return when (use) {
        NativeLoadUse.SINGLE_AD -> countedLoad(format, nativeSlotKey(placement, format), loadPlacement)
        NativeLoadUse.LIST_ROW, NativeLoadUse.REFRESH, NativeLoadUse.POOL -> perRequestLoad(format, loadPlacement)
    }
}

/**
 * The `ad_opportunity` of one cell of a pool (P-07, section 8.2): `shown` is always `ready`; `not_ready` is `idle`,
 * since a pool has no single-ad slot whose state it could report.
 */
internal fun AdTelemetry.poolCellOpportunity(placement: AdPlacement, result: String) =
    opportunity(placement.format, placement, result, loadState = if (result == "shown") "ready" else "idle")
