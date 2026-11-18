package com.vglstudio.camera.pose.line.guide.core.analytics

import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger

internal class FanOutAdFunnel(private val targets: List<AdFunnel>, private val logger: AppLogger) : AdFunnel {
    override fun log(event: AdFunnelEvent) {
    }
}
