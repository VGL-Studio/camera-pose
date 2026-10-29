package com.vglstudio.camera.pose.line.guide.core.ads

/**
 * The `ads_interval` marks of full-screen ads (docs/ops/ads-business-rules.md, section 4), kept free of Android so it
 * runs in unit tests. One mark per interval group (MC-04): "the last time an interstitial left the screen" (I-01),
 * written when it closes (I-02, I-05), in the memory of this process only (I-03). Times are milliseconds of
 * [MonotonicClock] (AR-01). Main thread only.
 */
internal class FullScreenAdGuard {
    private val marks = mutableMapOf<AdIntervalGroup, Long>()

    /** I-01: no mark lets it through; at the boundary it lets it through; a negative gap counts as passed (AR-01). */
    fun allows(group: AdIntervalGroup, intervalMillis: Long, now: Long): Boolean {
        val mark = marks[group] ?: return true
        val gap = now - mark
        return gap < 0 || gap >= intervalMillis
    }

    /** I-02, I-05: called from the SDK's close callback (or stuck recovery, or the combo's close). */
    fun record(group: AdIntervalGroup, now: Long) {
        marks[group] = now
    }
}

/**
 * I-02: only interstitial-family placements that record (MC-04) move a mark; app open and rewarded never do. The
 * native full-screen placement counts as an interstitial (question 2 of plan T-022).
 */
internal fun AdPlacementDeclaration.pushesIntervalMark(): Boolean = format.isFullScreen && recordInterval

/** I-04, MC-04: the gate is read unless the placement declares ignoreInterval. */
internal fun AdPlacementDeclaration.readsInterval(): Boolean = !ignoreInterval
