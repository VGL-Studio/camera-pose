package com.vglstudio.camera.pose.line.guide.core.analytics

/**
 * Records the ad funnel of AppsFlyer (docs/ops/ads-business-rules.md, section 8.3 and N-05; ADR-0027). Only :core:ads
 * calls it. Every value is a code (format, ad unit, adapter class), never personal data.
 */
interface AdFunnel {
    fun log(event: AdFunnelEvent)
}

/** The formats with a funnel: app open and banners have none (section 8.3). */
enum class AdFunnelKind { INTERS, REWARDED, NATIVE }

/** One step of the ad funnel. */
sealed interface AdFunnelEvent {
    /** A turn passed the gate of the app (interstitial, native) or the user tapped "Watch ad" (rewarded). */
    data class Eligible(val kind: AdFunnelKind) : AdFunnelEvent

    /** An ad was loaded. [adFormat] is the format code of T-04, [adSource] the adapter class, empty when none. */
    data class ApiCalled(val kind: AdFunnelKind, val adFormat: String, val adUnitId: String, val adSource: String) :
        AdFunnelEvent

    /** An ad came on screen; for a native ad, its first impression only. */
    data class Displayed(val kind: AdFunnelKind, val adFormat: String, val adUnitId: String, val adSource: String) :
        AdFunnelEvent

    /** The [count]-th interstitial since install came on screen (N-05). */
    data class InterDisplayedTimes(val count: Int) : AdFunnelEvent
}
