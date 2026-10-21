package com.vglstudio.camera.pose.line.guide.core.ads

/**
 * The ad unit of each placement for the current build, bound in :app per build flavor (ADR-0007):
 * [GoogleTestAdUnitIds] in staging, [ConfiguredAdUnitIds] in production. `null` means the placement shows no ad.
 */
fun interface AdUnitIds {
    fun of(placement: AdPlacement): String?
}

/** Google's public test ad units. They never produce real impressions, so tapping them is safe. */
object GoogleTestAdUnitIds : AdUnitIds {
    const val APP_ID = "ca-app-pub-3940256099942544~3347511713"

    override fun of(placement: AdPlacement): String = when (placement.format) {
        AdFormat.BANNER -> "ca-app-pub-3940256099942544/9214589741"
        AdFormat.INTERSTITIAL -> "ca-app-pub-3940256099942544/1033173712"
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

    /** Placements that have no ad unit in this build. A release must have none. */
    val missing: List<AdPlacement> = AdPlacement.entries.filter { it.key !in units }

    override fun of(placement: AdPlacement): String? = units[placement.key]
}
