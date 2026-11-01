package com.vglstudio.camera.pose.line.guide.core.ads

/**
 * Groups of ad slots preloaded one after the other (S-22). A format in no group is never preloaded. :app may set its
 * own plan with [AdsController.setPreloadPlan] in `Application.onCreate`, before consent has a result (HD-02).
 */
class AdPreloadPlan(groups: List<Set<AdFormat>>) {
    /** The groups in order; empty groups are dropped. */
    val groups: List<Set<AdFormat>> = groups.filter { it.isNotEmpty() }.map { it.toSet() }

    init {
        val all = this.groups.flatten()
        val twice = all.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        require(twice.isEmpty()) { "Formats in more than one preload group: $twice" }
    }

    companion object {
        /** HD-02 default: app open and the first step of a flow first, then every other format the placements use. */
        fun fromPlacements(placements: List<AdPlacement> = AdPlacement.entries): AdPreloadPlan =
            defaultPreloadPlan(placements.map { it.format })
    }
}

/** The slots the user sees first (S-22): app open and the native ad of the first step of a flow. */
private val FIRST_GROUP = setOf(AdFormat.APP_OPEN, AdFormat.NATIVE_STEP)

/** Two groups at most: [FIRST_GROUP] formats of [formats], then the others (question 2 of plan T-021). */
internal fun defaultPreloadPlan(formats: Collection<AdFormat>): AdPreloadPlan {
    val distinct = formats.toSet()
    val first = distinct.filter { it in FIRST_GROUP }.toSet()
    return AdPreloadPlan(listOf(first, distinct - first))
}

/** The native formats of the plan, in its group order, for the refill when an activity comes back (S-25). */
internal fun AdPreloadPlan.nativeOnly(): AdPreloadPlan = AdPreloadPlan(
    groups.map { group ->
        group.filter { it.isNative }.toSet()
    },
)

/** Every native format; exhaustive on purpose, so a new [AdFormat] decides here. */
private val AdFormat.isNative: Boolean
    get() = when (this) {
        AdFormat.NATIVE_SMALL,
        AdFormat.NATIVE_MEDIUM,
        AdFormat.NATIVE_COLLAPSIBLE,
        AdFormat.NATIVE_FULL_SCREEN,
        AdFormat.NATIVE_STEP,
        AdFormat.NATIVE_IN_COLLECTION,
        -> true
        AdFormat.BANNER,
        AdFormat.INTERSTITIAL,
        AdFormat.APP_OPEN,
        AdFormat.REWARDED,
        AdFormat.BANNER_COLLAPSIBLE,
        AdFormat.BANNER_MREC,
        -> false
    }
