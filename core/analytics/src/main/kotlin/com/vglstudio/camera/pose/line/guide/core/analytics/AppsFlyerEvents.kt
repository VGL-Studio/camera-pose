package com.vglstudio.camera.pose.line.guide.core.analytics

import com.vglstudio.camera.pose.line.guide.core.config.RemoteKey
import java.util.Locale

/**
 * The pure mappings of the AppsFlyer events (docs/ops/ads-business-rules.md, section 8.3; ADR-0027): names, params
 * and the `is_log_af_*` switch of each event. No call to the SDK, so the names are tested on the JVM. No event is ever
 * named `af_revenue`: AppsFlyer would count the revenue twice (`af_revenue` is only the value param of
 * `af_ad_revenue`).
 */

/** One AppsFlyer event: [params] null when it has none. */
internal data class AfEvent(val name: String, val params: Map<String, Any>?)

/**
 * `af_ad_revenue` of one impression, the params in the order of section 8.3: `af_revenue` is a [Double], every other
 * one a [String]. Unlike Firebase, the network is the raw source of the SDK (empty when none) and a missing placement
 * is sent as an empty string. `af_currency` and `af_revenue` are the values of `AFInAppEventParameterName.CURRENCY`
 * and `REVENUE`, written out so this needs no class of the SDK.
 */
internal fun afRevenue(event: AdMetricEvent.ImpressionRevenue, country: String): AfEvent = AfEvent(
    name = "af_ad_revenue",
    params = linkedMapOf(
        "monetization_network" to event.monetizationNetwork,
        "mediation_network" to MEDIATION_NETWORK,
        "af_currency" to event.currency,
        "af_revenue" to event.value,
        "country" to country,
        "ad_placement" to (event.adPlacement ?: ""),
        "ad_unit" to event.adUnitName,
        "ad_type" to event.adFormat,
    ),
)

/** The AppsFlyer event of [this] funnel step; null when it is not sent (a display count outside 1 to 20, N-05). */
internal fun AdFunnelEvent.toAfEvent(): AfEvent? = when (this) {
    is AdFunnelEvent.Eligible -> AfEvent(
        name = when (kind) {
            AdFunnelKind.INTERS -> "af_inters_ad_eligible"
            AdFunnelKind.REWARDED -> "af_rewarded_ad_eligible"
            AdFunnelKind.NATIVE -> "af_native_ad_eligible"
        },
        params = null,
    )
    is AdFunnelEvent.ApiCalled -> AfEvent(
        name = when (kind) {
            AdFunnelKind.INTERS -> "af_inters_api_called"
            AdFunnelKind.REWARDED -> "af_rewarded_api_called"
            AdFunnelKind.NATIVE -> "af_native_api_called"
        },
        params = funnelParams(adFormat, adUnitId, adSource),
    )
    // The interstitial one has no `_ad_`, as in section 8.3.
    is AdFunnelEvent.Displayed -> AfEvent(
        name = when (kind) {
            AdFunnelKind.INTERS -> "af_inters_displayed"
            AdFunnelKind.REWARDED -> "af_rewarded_ad_displayed"
            AdFunnelKind.NATIVE -> "af_native_ad_displayed"
        },
        params = funnelParams(adFormat, adUnitId, adSource),
    )
    is AdFunnelEvent.InterDisplayedTimes ->
        if (count in INTER_COUNT_RANGE) AfEvent("af_inters_displayed_${count}_times", null) else null
}

/** The switch of [this] funnel step: one key of [AppsFlyerSettings] per event. Exhaustive, with no `else`. */
internal fun AdFunnelEvent.switch(): RemoteKey<Boolean> = when (this) {
    is AdFunnelEvent.Eligible -> when (kind) {
        AdFunnelKind.INTERS -> AppsFlyerSettings.IS_LOG_AF_INTERS_AD_ELIGIBLE
        AdFunnelKind.REWARDED -> AppsFlyerSettings.IS_LOG_AF_REWARDED_AD_ELIGIBLE
        AdFunnelKind.NATIVE -> AppsFlyerSettings.IS_LOG_AF_NATIVE_AD_ELIGIBLE
    }
    is AdFunnelEvent.ApiCalled -> when (kind) {
        AdFunnelKind.INTERS -> AppsFlyerSettings.IS_LOG_AF_INTERS_API_CALLED
        AdFunnelKind.REWARDED -> AppsFlyerSettings.IS_LOG_AF_REWARDED_API_CALLED
        AdFunnelKind.NATIVE -> AppsFlyerSettings.IS_LOG_AF_NATIVE_API_CALLED
    }
    is AdFunnelEvent.Displayed -> when (kind) {
        AdFunnelKind.INTERS -> AppsFlyerSettings.IS_LOG_AF_INTERS_DISPLAYED
        AdFunnelKind.REWARDED -> AppsFlyerSettings.IS_LOG_AF_REWARDED_AD_DISPLAYED
        AdFunnelKind.NATIVE -> AppsFlyerSettings.IS_LOG_AF_NATIVE_AD_DISPLAYED
    }
    is AdFunnelEvent.InterDisplayedTimes -> AppsFlyerSettings.IS_LOG_AF_INTERS_DISPLAYED_N_TIMES
}

/** The switch of `af_ad_revenue`. */
internal val REVENUE_SWITCH: RemoteKey<Boolean> = AppsFlyerSettings.IS_LOG_AF_AD_REVENUE

/**
 * `country` of `af_ad_revenue`: the country of the SIM, else of the mobile network, else of the device language,
 * upper case (AdsKit `AppsFlyerAdTracker.kt:155-160`). Empty when none is known.
 */
internal fun countryCode(sim: String?, network: String?, locale: Locale): String {
    val code = sim?.takeIf { it.isNotBlank() } ?: network?.takeIf { it.isNotBlank() } ?: locale.country
    return code.trim().uppercase(Locale.ROOT)
}

private fun funnelParams(adFormat: String, adUnitId: String, adSource: String): Map<String, Any> = linkedMapOf(
    "ad_format" to adFormat,
    "ad_unit_id" to adUnitId,
    "ad_source" to adSource,
)

/** Ads are served through AdMob, mediation included. */
private const val MEDIATION_NETWORK = "admob"

/** N-05: `af_inters_displayed_{N}_times` for N from 1 to 20 only. */
private val INTER_COUNT_RANGE = 1..20
