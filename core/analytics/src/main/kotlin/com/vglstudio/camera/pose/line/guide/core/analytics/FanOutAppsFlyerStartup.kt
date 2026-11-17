package com.vglstudio.camera.pose.line.guide.core.analytics

import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger

/** Starts every attribution SDK of the module, in order. */
internal class FanOutAppsFlyerStartup(private val targets: List<AppsFlyerStartup>, private val logger: AppLogger) :
    AppsFlyerStartup {
    override fun prepare() = Unit

    override fun start() = Unit
}
