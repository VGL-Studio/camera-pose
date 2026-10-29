package com.vglstudio.camera.pose.line.guide.core.ads

/**
 * The ad unit of each placement for the current build, bound in :app per build flavor (ADR-0007):
 * [GoogleTestAdUnitIds] in staging, [ConfiguredAdUnitIds] in production. `null` means the placement shows no ad.
 */
fun interface AdUnitIds {
    fun of(placement: AdPlacement): String?

    /**
     * The unit of [placement] drawn as [format]: a layer of a bottom bar (banner, collapsible native) is another
     * format than the bar's own (plan T-030, question 2). By default only the placement's own format has a unit, so a
     * layer of another format sends nothing (B-04) until units per slot exist (spec, question 14).
     */
    fun of(placement: AdPlacement, format: AdFormat): String? = if (format == placement.format) of(placement) else null
}

/** Google's public test ad units. They never produce real impressions, so tapping them is safe. */
object GoogleTestAdUnitIds : AdUnitIds {
    const val APP_ID = "ca-app-pub-3940256099942544~3347511713"

    override fun of(placement: AdPlacement): String = unitFor(placement.format)

    /** Staging shows every layer of a bottom bar: the test unit of the layer's format. */
    override fun of(placement: AdPlacement, format: AdFormat): String = unitFor(format)

    /** The test unit of each format; also checks formats that have no placement yet. */
    internal fun unitFor(format: AdFormat): String = when (format) {
        AdFormat.BANNER -> "ca-app-pub-3940256099942544/9214589741"
        AdFormat.INTERSTITIAL -> "ca-app-pub-3940256099942544/1033173712"
        AdFormat.NATIVE_SMALL -> "ca-app-pub-3940256099942544/2247696110"
        // Native video, so the media view can be tried with a video.
        AdFormat.NATIVE_MEDIUM -> "ca-app-pub-3940256099942544/1044960115"
        // Opens as medium, so it uses the same native video unit.
        AdFormat.NATIVE_COLLAPSIBLE -> "ca-app-pub-3940256099942544/1044960115"
        // Native video, to try a video on the large media area.
        AdFormat.NATIVE_FULL_SCREEN -> "ca-app-pub-3940256099942544/1044960115"
        AdFormat.APP_OPEN -> "ca-app-pub-3940256099942544/9257395921"
        AdFormat.REWARDED -> "ca-app-pub-3940256099942544/5224354917"
        // Google has no collapsible test unit of its own: the adaptive banner unit.
        AdFormat.BANNER_COLLAPSIBLE -> "ca-app-pub-3940256099942544/9214589741"
        // Native video, like the other native cards with media.
        AdFormat.NATIVE_STEP -> "ca-app-pub-3940256099942544/1044960115"
        AdFormat.NATIVE_IN_COLLECTION -> "ca-app-pub-3940256099942544/1044960115"
    }
}

/**
 * Real ad units, given when the release is built and never stored in the repo (ADR-0009).
 * [config] has the form `<placement key>=<ad unit>;<placement key>=<ad unit>`.
 */
class ConfiguredAdUnitIds(config: String) : AdUnitIds {
    private val units: Map<String, String> = config.split(';')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .associate { entry ->
            val key = entry.substringBefore('=').trim()
            val unit = entry.substringAfter('=', missingDelimiterValue = "").trim()
            require(key.isNotEmpty() && unit.isNotEmpty()) { "Ad unit entry must be <placement key>=<ad unit>" }
            key to unit
        }

    /**
     * Placements that have no ad unit in this build. A release must have none. Staging-only placements
     * ([AdPlacement.stagingOnly]) never ship, so they are never missing.
     */
    val missing: List<AdPlacement> = AdPlacement.entries.filter { !it.stagingOnly && it.key !in units }

    /** Staging-only placements get no unit even when the configuration names them: production never shows them. */
    override fun of(placement: AdPlacement): String? = if (placement.stagingOnly) null else units[placement.key]
}
