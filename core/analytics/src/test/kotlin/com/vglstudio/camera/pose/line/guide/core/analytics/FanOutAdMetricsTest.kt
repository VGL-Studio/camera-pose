package com.vglstudio.camera.pose.line.guide.core.analytics

import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every ad metric goes to Firebase and AppsFlyer; one failing target never stops the other (plan T-033). */
class FanOutAdMetricsTest {
    private val logger = RecordingAppLogger()

    @Test
    fun everyEvent_reachesEveryTarget_inOrder() {
        val order = mutableListOf<String>()
        val first = object : AdMetrics {
            override fun log(event: AdMetricEvent) {
                order += "first:${event.adFormat}"
            }
        }
        val second = object : AdMetrics {
            override fun log(event: AdMetricEvent) {
                order += "second:${event.adFormat}"
            }
        }
        val fanOut = FanOutAdMetrics(listOf(first, second), logger)

        fanOut.log(AdMetricEvent.Clicked("INTER", "p"))
        fanOut.log(AdMetricEvent.Clicked("NATIVE_SMALL", "p"))

        assertEquals(
            listOf("first:INTER", "second:INTER", "first:NATIVE_SMALL", "second:NATIVE_SMALL"),
            order,
        )
    }

    @Test
    fun firstTargetThrows_secondStillReceives() {
        val failing = object : AdMetrics {
            override fun log(event: AdMetricEvent): Unit = throw IllegalStateException("message of the sdk")
        }
        val firstRecorder = RecordingAdMetrics()
        val secondRecorder = RecordingAdMetrics()
        val event = AdMetricEvent.Clicked("INTER", "p")

        FanOutAdMetrics(listOf(failing, secondRecorder), logger).log(event)
        // And the other way round.
        FanOutAdMetrics(listOf(firstRecorder, failing), logger).log(event)

        assertEquals(listOf(event), secondRecorder.events)
        assertEquals(listOf(event), firstRecorder.events)
        val warnings = logger.entries.filter { it.level == LogLevel.WARN }
        assertEquals(2, warnings.size)
        warnings.forEach {
            assertTrue(it.message, "IllegalStateException" in it.message)
            assertFalse(it.message, "message of the sdk" in it.message)
        }
    }
}
