package com.vglstudio.camera.pose.line.guide.core.analytics

/** Keeps the ad funnel in memory, for the unit tests of :core:ads (ADR-0027, decision 5). */
class RecordingAdFunnel : AdFunnel {
    private val recorded = mutableListOf<AdFunnelEvent>()

    val events: List<AdFunnelEvent> get() = synchronized(recorded) { recorded.toList() }

    override fun log(event: AdFunnelEvent) {
        synchronized(recorded) { recorded += event }
    }
}
