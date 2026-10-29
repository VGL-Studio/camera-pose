package com.vglstudio.camera.pose.line.guide.core.network

import com.vglstudio.camera.pose.line.guide.core.config.RemoteKey

/** Remote Config key of the network module (ADR-0003) and its value before any remote value arrives. */
internal object NetworkSettings {
    /** How long the device must report no network before the dialog shows, in seconds (spec network, question 2). */
    const val LOST_DELAY_SECONDS = "network_lost_delay_seconds"
    const val LOST_DELAY_SECONDS_DEFAULT = 3L

    /** The key of [LOST_DELAY_SECONDS] (ADR-0023). */
    val LOST_DELAY: RemoteKey<Long> = RemoteKey.long(
        LOST_DELAY_SECONDS,
        LOST_DELAY_SECONDS_DEFAULT,
        "Seconds the device must report no network before the dialog that blocks the app shows.",
    )

    /** "Retry" checks the network every [RETRY_POLL_MILLIS] for at most [RETRY_MAX_MILLIS] (spec, question 3). */
    const val RETRY_MAX_MILLIS = 2_000L
    const val RETRY_POLL_MILLIS = 250L

    /** Every Remote Config key of the network module. */
    fun all(): List<RemoteKey<*>> = listOf(LOST_DELAY)
}
