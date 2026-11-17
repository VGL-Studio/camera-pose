package com.vglstudio.camera.pose.line.guide.core.analytics

import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger

/**
 * Starts every attribution SDK of the module, in order: AppsFlyer, then Tenjin (plan T-099, step 8; ADR-1010). Each
 * call is caught on its own, so a target that throws never keeps the others from starting; the error is logged by
 * class name only.
 */
internal class FanOutAppsFlyerStartup(private val targets: List<AppsFlyerStartup>, private val logger: AppLogger) :
    AppsFlyerStartup {
    override fun prepare() = each("prepare") { it.prepare() }

    override fun start() = each("start") { it.start() }

    private fun each(step: String, call: (AppsFlyerStartup) -> Unit) {
        targets.forEach { target ->
            try {
                call(target)
            } catch (e: Exception) {
                val name = target.javaClass.simpleName
                logger.warn(TAG, "Attribution $step failed in $name: ${e.javaClass.simpleName}")
            }
        }
    }

    private companion object {
        const val TAG = "Analytics"
    }
}
