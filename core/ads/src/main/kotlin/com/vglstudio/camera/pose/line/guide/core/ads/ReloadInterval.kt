package com.vglstudio.camera.pose.line.guide.core.ads

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** The shortest reload interval of a slot, in seconds (KB-34, criterion 2). */
internal const val MIN_RELOAD_SECONDS = 30L

/**
 * The reload interval of a slot from its remote value [raw], in seconds (KB-34, criterion 2): 0 means no reload, a
 * negative value reads as [defaultSeconds], 1 to 29 as [MIN_RELOAD_SECONDS]. A value of the wrong type already reads
 * as the default of its key in :core:config.
 */
internal fun reloadSeconds(raw: Long, defaultSeconds: Long): Long = when {
    raw == 0L -> 0L
    raw < 0L -> defaultSeconds
    raw < MIN_RELOAD_SECONDS -> MIN_RELOAD_SECONDS
    else -> raw
}

/**
 * Waits for the next reload tick of a slot whose interval is [intervalMillis] (KB-34, criterion 3), the one wait of
 * every reload by key (native refresh of the small slot and the collapsible layer, reload by hand of the fixed,
 * collapsible and MREC banners):
 *
 * - The value read when a count starts sets it, plus [extraMillis]; a new positive value that comes meanwhile applies
 *   from the next count, the one under way is not set again.
 * - 0 means no reload: the wait goes on until a positive value comes, then counts from that moment.
 * - A value going to 0 during a count drops that count.
 *
 * Coroutine delays: a monotonic clock, not the wall clock (AR-01).
 */
internal suspend fun awaitReloadTick(intervalMillis: StateFlow<Long>, extraMillis: Long = 0) {
    while (true) {
        val interval = intervalMillis.value
        if (interval <= 0L) {
            intervalMillis.first { it > 0L }
            continue
        }
        val dropped = withTimeoutOrNull(interval + extraMillis) { intervalMillis.first { it <= 0L } } != null
        if (!dropped) return
    }
}
