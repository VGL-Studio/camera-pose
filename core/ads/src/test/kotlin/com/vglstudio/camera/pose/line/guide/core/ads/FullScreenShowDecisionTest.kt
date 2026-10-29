package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.ads.FullScreenShowDecision.BUSY
import com.vglstudio.camera.pose.line.guide.core.ads.FullScreenShowDecision.CONSENT_CLOSED
import com.vglstudio.camera.pose.line.guide.core.ads.FullScreenShowDecision.DISABLED
import com.vglstudio.camera.pose.line.guide.core.ads.FullScreenShowDecision.NOT_READY
import com.vglstudio.camera.pose.line.guide.core.ads.FullScreenShowDecision.SHOW
import com.vglstudio.camera.pose.line.guide.core.ads.FullScreenShowDecision.THROTTLED
import org.junit.Assert.assertEquals
import org.junit.Test

/** The order of the full-screen gate: consent, placement switch, busy, not ready, interval (plan T-022). */
class FullScreenShowDecisionTest {
    @Test
    fun order() {
        // consentOpen, enabled, busy, ready, intervalPassed -> expected.
        val rows = listOf(
            Row(false, false, true, false, false, CONSENT_CLOSED),
            Row(true, false, true, false, false, DISABLED),
            Row(true, true, true, false, false, BUSY),
            Row(true, true, false, false, false, NOT_READY),
            Row(true, true, false, true, false, THROTTLED),
            Row(true, true, false, true, true, SHOW),
        )
        rows.forEach { row ->
            assertEquals(
                "$row",
                row.expected,
                decideFullScreenShow(row.consentOpen, row.enabled, row.busy, row.ready, row.intervalPassed),
            )
        }
    }

    @Test
    fun busy_beatsNotReadyAndThrottled() {
        assertEquals(
            BUSY,
            decideFullScreenShow(
                consentOpen = true,
                enabled = true,
                busy = true,
                ready = false,
                intervalPassed = false,
            ),
        )
    }

    private data class Row(
        val consentOpen: Boolean,
        val enabled: Boolean,
        val busy: Boolean,
        val ready: Boolean,
        val intervalPassed: Boolean,
        val expected: FullScreenShowDecision,
    )
}
