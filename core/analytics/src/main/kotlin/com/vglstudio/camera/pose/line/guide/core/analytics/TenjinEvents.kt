package com.vglstudio.camera.pose.line.guide.core.analytics

import com.vglstudio.camera.pose.line.guide.core.config.RemoteKey

/**
 * The AdMob impression Tenjin gets for [event] (ADR-1010; spec ads, question 21 a), or null when it is not sent: an
 * impression of another platform (MAX is not on, spec ads question 16), the zero-value fallback, or no currency. The
 * fields of Tenjin's AdMob documentation the event has; only `value_micros` is required. `response_id` and
 * `precision_type` are not in the event of :core:ads, so they are not sent (plan T-099, question 8.1).
 */
internal fun tenjinAdMobPayload(event: AdMetricEvent.ImpressionRevenue): Map<String, Any>? {
    if (event.platform != AdMetricPlatform.ADMOB || event.value <= 0.0 || event.currency.isBlank()) return null
    return buildMap {
        put("ad_unit_id", event.adUnitName)
        put("currency_code", event.currency)
        put("value_micros", Math.round(event.value * MICROS))
        event.adapterClassName?.let { put("mediation_adapter_class_name", it) }
    }
}

private const val MICROS = 1_000_000

/**
 * The Tenjin event of [this] funnel step (ADR-1010, section 5): the AppsFlyer name without `af_`, with no param; null
 * when it is not sent (a display count outside 1 to 20, N-05).
 */
internal fun AdFunnelEvent.toTenjinEvent(): String? = toAfEvent()?.name?.removePrefix("af_")

/** The switch of [this] funnel step: one key of [TenjinSettings] per event (ADR-1010, section 6). No `else`. */
internal fun AdFunnelEvent.tenjinSwitch(): RemoteKey<Boolean> = when (this) {
    is AdFunnelEvent.Eligible -> when (kind) {
        AdFunnelKind.INTERS -> TenjinSettings.IS_LOG_TENJIN_INTERS_AD_ELIGIBLE
        AdFunnelKind.REWARDED -> TenjinSettings.IS_LOG_TENJIN_REWARDED_AD_ELIGIBLE
        AdFunnelKind.NATIVE -> TenjinSettings.IS_LOG_TENJIN_NATIVE_AD_ELIGIBLE
    }
    is AdFunnelEvent.ApiCalled -> when (kind) {
        AdFunnelKind.INTERS -> TenjinSettings.IS_LOG_TENJIN_INTERS_API_CALLED
        AdFunnelKind.REWARDED -> TenjinSettings.IS_LOG_TENJIN_REWARDED_API_CALLED
        AdFunnelKind.NATIVE -> TenjinSettings.IS_LOG_TENJIN_NATIVE_API_CALLED
    }
    is AdFunnelEvent.Displayed -> when (kind) {
        AdFunnelKind.INTERS -> TenjinSettings.IS_LOG_TENJIN_INTERS_DISPLAYED
        AdFunnelKind.REWARDED -> TenjinSettings.IS_LOG_TENJIN_REWARDED_AD_DISPLAYED
        AdFunnelKind.NATIVE -> TenjinSettings.IS_LOG_TENJIN_NATIVE_AD_DISPLAYED
    }
    is AdFunnelEvent.InterDisplayedTimes -> TenjinSettings.IS_LOG_TENJIN_INTERS_DISPLAYED_N_TIMES
}
