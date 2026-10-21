package com.vglstudio.camera.pose.line.guide.core.network

import org.junit.Assert.assertEquals
import org.junit.Test

/** The values are decisions of the product owner (spec network, questions 2 and 3): change them only with a new one. */
class NetworkSettingsTest {
    @Test
    fun lostDelayIs3SecondsByDefault() {
        assertEquals("network_lost_delay_seconds", NetworkSettings.LOST_DELAY_SECONDS)
        assertEquals(3L, NetworkSettings.LOST_DELAY_SECONDS_DEFAULT)
    }

    @Test
    fun retryWaitsAtMost2Seconds() {
        assertEquals(2_000L, NetworkSettings.RETRY_MAX_MILLIS)
        assertEquals(250L, NetworkSettings.RETRY_POLL_MILLIS)
    }
}
