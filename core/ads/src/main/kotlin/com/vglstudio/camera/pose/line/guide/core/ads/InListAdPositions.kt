package com.vglstudio.camera.pose.line.guide.core.ads

/**
 * Positions, in the combined list of items and ads, where an in-list ad goes (ADR-0022, ads-guide.md section 4).
 * The k-th ad follows the (k * every)-th item. An ad always sits between two items: never first, never last,
 * so a list of `every` items or fewer has none. A non-positive `every` means no ads.
 */
fun inListAdPositions(itemCount: Int, every: Int): List<Int> {
    if (every < 1 || itemCount < 1) return emptyList()
    val count = (itemCount - 1) / every
    // The k-th ad (from 1) comes after k * every items and the k - 1 ads before it.
    return (1..count).map { k -> k * every + (k - 1) }
}

/** `every` rounded up to whole grid rows, so an ad row never leaves a half-filled row above it. */
fun gridAdEvery(every: Int, columns: Int): Int {
    if (columns < 1 || every < 1) return every
    val rounded = (every.toLong() + columns - 1) / columns * columns
    return rounded.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}

/** N to use, or null when no ad row may be added (ads not allowed, placement off, bad remote value). */
internal fun inListEvery(adsAllowed: Boolean, everyCount: Long): Int? {
    if (!adsAllowed) return null
    return if (everyCount in 1L..Int.MAX_VALUE.toLong()) everyCount.toInt() else null
}
