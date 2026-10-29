package com.vglstudio.camera.pose.line.guide.core.analytics

/** Keeps ad metrics in memory, for the unit tests of :core:ads (ADR-0027, decision 5). */
class RecordingAdMetrics : AdMetrics {
    private val recorded = mutableListOf<AdMetricEvent>()

    val events: List<AdMetricEvent> get() = synchronized(recorded) { recorded.toList() }

    override fun log(event: AdMetricEvent) {
        synchronized(recorded) { recorded += event }
    }
}
