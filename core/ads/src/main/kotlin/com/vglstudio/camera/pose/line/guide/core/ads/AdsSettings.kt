package com.vglstudio.camera.pose.line.guide.core.ads

/**
 * Remote Config keys of the ads module (ADR-0003, ADR-0004) and their values before any remote value arrives.
 * The defaults are business decisions (ADR-0014).
 */
internal object AdsSettings {
    fun placementEnabledKey(placement: AdPlacement) = "ads_${placement.key}_enabled"

    /** First break of the session that may show an ad. */
    fun placementFirstBreakKey(placement: AdPlacement) = "ads_${placement.key}_first_break"

    /** Minimum number of breaks between two ads of the placement. */
    fun placementBreaksBetweenKey(placement: AdPlacement) = "ads_${placement.key}_breaks_between"

    /** Minimum seconds between two ads of the placement. */
    fun placementMinIntervalKey(placement: AdPlacement) = "ads_${placement.key}_min_interval_seconds"

    /** Business decisions of the spec: a placement without limits of its own has none besides the shared guard. */
    fun breakCapDefaults(placement: AdPlacement): BreakCap = when (placement) {
        AdPlacement.REVIEW_INTERSTITIAL -> BreakCap(firstBreak = 2, breaksBetween = 3, minIntervalSeconds = 120)
        else -> BreakCap.NONE
    }

    /** Off by default: a placement shows ads only once the product owner switches it on remotely. */
    const val PLACEMENT_ENABLED_DEFAULT = false

    /**
     * Personalized ads where the law requires a consent form (EEA, UK, Switzerland and similar regions).
     * On by default: users who gave consent get personalized ads; users who declined never do, whatever this says.
     */
    const val PERSONALIZED_IN_REGULATED_REGIONS = "ads_personalized_regulated_regions_enabled"
    const val PERSONALIZED_IN_REGULATED_REGIONS_DEFAULT = true

    /** Minimum time between two full-screen ads, in seconds. */
    const val INTERSTITIAL_MIN_INTERVAL_SECONDS = "ads_interstitial_min_interval_seconds"
    const val INTERSTITIAL_MIN_INTERVAL_SECONDS_DEFAULT = 45L
}

/**
 * Whether ad requests must ask for non-personalized ads. Outside regulated regions the consent form is not
 * required and personalization follows Google's defaults; inside them it also needs the remote switch.
 */
internal fun requestsNonPersonalizedAds(inRegulatedRegion: Boolean, personalizedInRegulatedRegions: Boolean) =
    inRegulatedRegion && !personalizedInRegulatedRegions
