package com.vglstudio.camera.pose.line.guide.core.network

/** Remote Config key of the network module (ADR-0003) and its value before any remote value arrives. */
internal object NetworkSettings {
    /** How long the device must report no network before the dialog shows, in seconds (spec network, question 2). */
    const val LOST_DELAY_SECONDS = "network_lost_delay_seconds"
    const val LOST_DELAY_SECONDS_DEFAULT = 0L

    /** "Retry" checks the network every [RETRY_POLL_MILLIS] for at most [RETRY_MAX_MILLIS] (spec, question 3). */
    const val RETRY_MAX_MILLIS = 0L
    const val RETRY_POLL_MILLIS = 0L
}
