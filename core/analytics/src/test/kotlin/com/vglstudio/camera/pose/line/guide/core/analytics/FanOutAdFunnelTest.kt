package com.vglstudio.camera.pose.line.guide.core.analytics

import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** Every ad funnel event goes to AppsFlyer, then Tenjin; one failing target never stops the other (plan T-100). */
class FanOutAdFunnelTest {
    private val logger = RecordingAppLogger()

    @Test
    fun eachTargetGetsEvent_inOrder() {
        val received = mutableListOf<Pair<String, AdFunnelEvent>>()
        val appsFlyer = NamedAdFunnel("appsFlyer", received)
        val tenjin = NamedAdFunnel("tenjin", received)
        val event = AdFunnelEvent.Eligible(AdFunnelKind.INTERS)

        FanOutAdFunnel(listOf(appsFlyer, tenjin), logger).log(event)

        assertEquals(listOf("appsFlyer" to event, "tenjin" to event), received)
    }

    @Test
    fun throwingTarget_doesNotStopOthers() {
        val received = mutableListOf<Pair<String, AdFunnelEvent>>()
        val event = AdFunnelEvent.InterDisplayedTimes(3)

        FanOutAdFunnel(listOf(ThrowingAdFunnel(), NamedAdFunnel("second", received)), logger).log(event)
        // And the other way round.
        FanOutAdFunnel(listOf(NamedAdFunnel("first", received), ThrowingAdFunnel()), logger).log(event)

        assertEquals(listOf("second" to event, "first" to event), received)
        val warnings = logger.entries.filter { it.level == LogLevel.WARN }
        assertEquals(
            List(2) { "Ad funnel event not logged by ThrowingAdFunnel: IllegalStateException" },
            warnings.map { it.message },
        )
        warnings.forEach {
            assertEquals("Analytics", it.tag)
            assertFalse(it.message, "message of the sdk" in it.message)
        }
    }

    private class NamedAdFunnel(
        private val name: String,
        private val received: MutableList<Pair<String, AdFunnelEvent>>,
    ) : AdFunnel {
        override fun log(event: AdFunnelEvent) {
            received += name to event
        }
    }

    private class ThrowingAdFunnel : AdFunnel {
        override fun log(event: AdFunnelEvent): Unit = throw IllegalStateException("message of the sdk")
    }
}
