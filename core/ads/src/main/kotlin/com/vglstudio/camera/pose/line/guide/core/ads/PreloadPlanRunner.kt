package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import kotlinx.coroutines.delay

/** One slot the plan can preload: full-screen placements and native pools. */
internal interface PreloadSlot {
    val format: AdFormat

    /** Starts a load when needed; never when loading, ready or waiting for a retry (F-04). */
    fun loadIfNeeded()

    /** Has an ad, failed or waits for a retry: the group may move on (S-22). */
    fun settled(): Boolean
}

/** Runs a preload plan (S-22). */
internal class PreloadPlanRunner(private val clock: MonotonicClock, private val logger: AppLogger) {
    /**
     * Runs the groups in order: the next one starts once every slot of the current one settled, or after 6 s; checks
     * at once, then every 200 ms. A format with no slot counts as settled; a slot of a format in no group never loads.
     */
    suspend fun run(plan: AdPreloadPlan, slotsOf: (AdFormat) -> List<PreloadSlot>) {
        val total = plan.groups.size
        plan.groups.forEachIndexed { index, group ->
            val name = "${index + 1}/$total"
            logger.info(TAG, "Preload group $name started: ${group.joinToString()}")
            val slots = group.flatMap(slotsOf)
            slots.forEach { it.loadIfNeeded() }
            val startedAt = clock.nowMillis()
            while (true) {
                if (slots.all { it.settled() }) {
                    logger.info(TAG, "Preload group $name moved on: settled")
                    break
                }
                val elapsed = clock.nowMillis() - startedAt
                if (elapsed >= GROUP_CAP_MILLIS) {
                    logger.info(TAG, "Preload group $name moved on: timed out")
                    break
                }
                delay(minOf(POLL_MILLIS, GROUP_CAP_MILLIS - elapsed))
            }
        }
        logger.info(TAG, "Preload plan finished")
    }

    private companion object {
        const val TAG = "Ads"
        const val GROUP_CAP_MILLIS = 6_000L
        const val POLL_MILLIS = 200L
    }
}
