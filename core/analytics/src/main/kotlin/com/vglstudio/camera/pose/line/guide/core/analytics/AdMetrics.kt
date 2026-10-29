package com.vglstudio.camera.pose.line.guide.core.analytics

/**
 * Records the ad metrics of :core:ads (docs/ops/ads-business-rules.md, sections 8.1 and 8.2; ADR-0027, decision 1).
 * Only :core:ads calls it; features log their own events with [Analytics].
 */
interface AdMetrics {
    fun log(event: AdMetricEvent)
}

/**
 * One ad metric. Every value is a code (format, placement, network, currency, numeric error code): never a message
 * of the SDK, never personal data (risk-scoring.md, blocking point 4).
 */
sealed interface AdMetricEvent {
    /** Format code of section 8.1, T-04 (`INTER`, `NATIVE_SMALL`, ...). */
    val adFormat: String

    /** One paid impression (T-01), or the zero-value fallback of a native ad (T-02). Always sent. */
    data class ImpressionRevenue(
        override val adFormat: String,
        /** Network name for Firebase: the source the SDK reports, `admob` when it reports none (T-03). */
        val adSource: String,
        /** The source the SDK reports, as is: empty when it reports none; `monetization_network` of AppsFlyer. */
        val monetizationNetwork: String,
        val adUnitName: String,
        val currency: String,
        val value: Double,
        val adPlacement: String?,
        /**
         * Adapter class of the network that served the ad. Sent nowhere (section 8.3 has no such param): kept for
         * the logs and diagnosis.
         */
        val adapterClassName: String?,
    ) : AdMetricEvent

    data class LoadSuccess(
        override val adFormat: String,
        val adPlacement: String?,
        val loadAttempt: Int?,
        val latencyMs: Long?,
    ) : AdMetricEvent

    data class LoadFail(
        override val adFormat: String,
        val adPlacement: String?,
        val loadAttempt: Int?,
        val latencyMs: Long?,
        /** Numeric code of the SDK, as a string. */
        val loadErrorCode: String,
        val loadState: String,
    ) : AdMetricEvent

    data class Opportunity(
        override val adFormat: String,
        val adPlacement: String,
        val result: String,
        val loadState: String,
    ) : AdMetricEvent

    data class ShowFail(
        override val adFormat: String,
        val adPlacement: String?,
        /** Numeric code of the SDK, as a string. */
        val showErrorCode: String,
        val showFailReason: String,
    ) : AdMetricEvent

    data class Clicked(override val adFormat: String, val adPlacement: String?) : AdMetricEvent

    /** The user earned the reward of a rewarded ad (R-02); Firebase gets no `ad_format` for it. */
    data class RewardEarned(override val adFormat: String, val adPlacement: String) : AdMetricEvent

    /** The dialog before a rewarded ad was shown or dismissed ([dialogAction] `show`, `dismiss`); no `ad_format`. */
    data class RewardDialog(override val adFormat: String, val adPlacement: String, val dialogAction: String) :
        AdMetricEvent
}
