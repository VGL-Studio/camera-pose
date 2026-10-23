package com.vglstudio.camera.pose.line.guide.core.ads

/** How an ad is shown. A placement has exactly one format. */
enum class AdFormat {
    /** Shown inside a screen with [AdSlot]. */
    BANNER,

    /** Covers the whole screen, shown at a natural break with [rememberShowInterstitial]. */
    INTERSTITIAL,
}

/**
 * Every ad placement of the app, declared in one place (ADR-0004).
 * A placement is a business decision: it must come from an approved spec.
 * Adding, removing or moving a placement is a high-risk change (risk-scoring.md, blocking point 1).
 * Each placement shows ads only once the Remote Config key `ads_<key>_enabled` is switched on (off by default).
 * Production builds need a real ad unit for every placement (see [ConfiguredAdUnitIds]).
 */
enum class AdPlacement(val key: String, val format: AdFormat) {
    SAMPLE_BOTTOM_BANNER("sample_bottom_banner", AdFormat.BANNER),
    REVIEW_INTERSTITIAL("review_interstitial", AdFormat.INTERSTITIAL),
}
