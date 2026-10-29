package com.vglstudio.camera.pose.line.guide.core.analytics

import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger

/**
 * Hands every ad metric to each of [targets], in order: Firebase, then AppsFlyer (plan T-033). Each call is caught on
 * its own, so a target that throws never keeps the others from the event; the error is logged by class name only.
 * Public for the tests of :core:ads; the app gets it as its [AdMetrics] through Hilt.
 */
class FanOutAdMetrics(private val targets: List<AdMetrics>, private val logger: AppLogger) : AdMetrics {
    override fun log(event: AdMetricEvent) {
        targets.forEach { target ->
            try {
                target.log(event)
            } catch (e: Exception) {
                logger.warn(TAG, "Ad metric not logged by ${target.javaClass.simpleName}: ${e.javaClass.simpleName}")
            }
        }
    }

    private companion object {
        const val TAG = "Analytics"
    }
}
