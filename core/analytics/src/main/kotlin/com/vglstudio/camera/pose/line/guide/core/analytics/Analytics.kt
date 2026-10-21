package com.vglstudio.camera.pose.line.guide.core.analytics

/**
 * An analytics event. Every event must be listed in the spec of its feature and approved
 * before it is added (risk-scoring.md, blocking point 4). Do not put personal data in [params].
 * Firebase limits: [name] and param keys are letters, digits and underscores, start with a letter and are
 * at most 40 characters; a param value is at most 100 characters; at most 25 params. Firebase drops or cuts
 * what breaks these limits.
 */
data class AnalyticsEvent(val name: String, val params: Map<String, String> = emptyMap())

/** Logs analytics events. Feature modules use this interface, never an analytics SDK directly. */
interface Analytics {
    fun log(event: AnalyticsEvent)
}
