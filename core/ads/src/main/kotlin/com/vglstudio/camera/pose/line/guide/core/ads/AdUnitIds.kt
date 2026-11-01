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
     * layer of another format sends nothing (B-04); [ConfiguredAdUnitIds] reads layer units from slot keys (T-056).
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
        // Google's fixed-size banner unit, for every fixed size, 300 x 250 too (plan T-062, question 1).
        AdFormat.BANNER_MREC -> "ca-app-pub-3940256099942544/6300978111"
    }
}

/**
 * Real ad units, given when the release is built and never stored in the repo (ADR-0009).
 * [config] has the form `<key>=<ad unit>;<key>=<ad unit>`, with two kinds of key in one table (plan T-056):
 * - a placement key (`sample_native_small=...`): the unit of the placement's own format;
 * - a slot key, the code of a layer format of a bottom bar ([BOTTOM_BAR_LAYER_FORMATS]: `banner=...`,
 *   `native_collap=...`): one unit for that layer of every bottom bar of the app (M-05, spec question 14).
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
     * The keys of `adUnits` that this build lacks: placement keys first (in declaration order), then the slot keys of
     * the layers of those placements, each once. A release should have none; a missing slot key only leaves that
     * layer without an ad (B-04). Staging-only placements ([AdPlacement.stagingOnly]) never ship, so they and their
     * layers are never missing.
     */
    val missing: List<String> = missingUnitKeys(AdPlacement.entries.map { it.declaration }, units.keys)

    /** Staging-only placements get no unit even when the configuration names them: production never shows them. */
    override fun of(placement: AdPlacement): String? = if (placement.stagingOnly) null else units[placement.key]

    /**
     * The placement's own format reads only its placement key, never a slot key. A layer of another format reads
     * its slot key ([AdFormat.code]); without one the layer sends nothing (B-04).
     */
    override fun of(placement: AdPlacement, format: AdFormat): String? = when {
        placement.stagingOnly -> null
        format == placement.format -> of(placement)
        else -> units[format.code]
    }
}

/** The layer formats of a bottom bar (D-01) whose units come from slot keys of the same name (plan T-056). */
internal val BOTTOM_BAR_LAYER_FORMATS = listOf(AdFormat.BANNER, AdFormat.NATIVE_COLLAPSIBLE)

/**
 * The layers of this placement: a native small placement is a bottom bar (MC-08, [BottomAdBarScaffold]), so it has
 * the [BOTTOM_BAR_LAYER_FORMATS]; any other has none.
 */
internal val AdPlacementDeclaration.layerFormats: List<AdFormat>
    get() = if (format == AdFormat.NATIVE_SMALL) BOTTOM_BAR_LAYER_FORMATS else emptyList()

/**
 * The keys missing from `adUnits` for [placements], given the [configured] keys: placement keys of the placements
 * that ship, then the slot keys of their layers, each once, in the order of [BOTTOM_BAR_LAYER_FORMATS].
 */
internal fun missingUnitKeys(placements: List<AdPlacementDeclaration>, configured: Set<String>): List<String> {
    val shipped = placements.filterNot { it.stagingOnly }
    val placementKeys = shipped.map { it.code }.filter { it !in configured }
    val layers = shipped.flatMap { it.layerFormats }.toSet()
    val slotKeys = BOTTOM_BAR_LAYER_FORMATS.filter { it in layers }.map { it.code }.filter { it !in configured }
    return placementKeys + slotKeys
}

/**
 * The line logged once when :core:ads starts if production units are missing (plan T-056), or null: only
 * [ConfiguredAdUnitIds] with missing keys warns, so staging ([GoogleTestAdUnitIds]) never does.
 */
internal fun missingUnitsWarning(unitIds: AdUnitIds): String? {
    val missing = (unitIds as? ConfiguredAdUnitIds)?.missing.orEmpty()
    return if (missing.isEmpty()) null else "Production ad units missing, these show no ad: ${missing.joinToString()}"
}
