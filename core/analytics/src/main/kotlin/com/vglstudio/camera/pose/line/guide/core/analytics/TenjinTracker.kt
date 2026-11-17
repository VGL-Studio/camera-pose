package com.vglstudio.camera.pose.line.guide.core.analytics

import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger

/** Tenjin behind the interfaces of this module (ADR-1010). */
internal class TenjinTracker(
    private val config: TenjinConfig,
    private val sink: TenjinSink,
    private val switches: TenjinSwitches,
    private val foreground: AppForeground,
    private val logger: AppLogger,
) : AppsFlyerStartup,
    AdMetrics,
    AttributionConsent {
    override fun prepare() = Unit

    override fun start() = Unit

    override fun onConsentKnown() = Unit

    override fun log(event: AdMetricEvent) = Unit
}
