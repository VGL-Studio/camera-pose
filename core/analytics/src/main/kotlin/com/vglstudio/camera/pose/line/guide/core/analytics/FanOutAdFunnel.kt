package com.vglstudio.camera.pose.line.guide.core.analytics

import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger

/**
 * Hands every ad funnel event to each of [targets], in order: AppsFlyer, then Tenjin (plan T-100). Each call is caught
 * on its own, so a target that throws never keeps the others from the event; the error is logged by class name only.
 */
internal class FanOutAdFunnel(private val targets: List<AdFunnel>, private val logger: AppLogger) : AdFunnel {
    override fun log(event: AdFunnelEvent) {
        targets.forEach { target ->
            try {
                target.log(event)
            } catch (e: Exception) {
                val name = target.javaClass.simpleName
                logger.warn(TAG, "Ad funnel event not logged by $name: ${e.javaClass.simpleName}")
            }
        }
    }

    private companion object {
        const val TAG = "Analytics"
    }
}
