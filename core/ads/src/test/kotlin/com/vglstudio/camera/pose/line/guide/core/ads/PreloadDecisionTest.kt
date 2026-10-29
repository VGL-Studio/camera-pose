package com.vglstudio.camera.pose.line.guide.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The decision of one asked preload (S-21, S-23). */
class PreloadDecisionTest {
    private val open = PreloadGate(sdkStarted = true, sdkReady = true, mayRequest = true, consentDenied = false)

    private data class Row(
        val name: String,
        val reason: PreloadReason,
        val gate: PreloadGate,
        val everRequested: Boolean,
        val running: Boolean,
        val expected: PreloadDecision,
    )

    @Test
    fun order() {
        val rows = listOf(
            Row("open", PreloadReason.NETWORK_BACK, open, true, false, PreloadDecision.RUN),
            Row(
                "SDK not started, everything else wrong too",
                PreloadReason.NETWORK_BACK,
                PreloadGate(sdkStarted = false, sdkReady = false, mayRequest = false, consentDenied = true),
                false,
                true,
                PreloadDecision.IGNORE_SDK_NOT_STARTED,
            ),
            Row(
                "consent denied",
                PreloadReason.NETWORK_BACK,
                open.copy(consentDenied = true),
                true,
                false,
                PreloadDecision.IGNORE_CONSENT_DENIED,
            ),
            Row(
                "never requested",
                PreloadReason.NETWORK_BACK,
                open,
                false,
                false,
                PreloadDecision.IGNORE_NEVER_REQUESTED,
            ),
            Row("first preload", PreloadReason.SDK_READY, open, false, false, PreloadDecision.RUN),
            Row("running", PreloadReason.NETWORK_BACK, open, true, true, PreloadDecision.HOLD),
            Row(
                "SDK not ready",
                PreloadReason.NETWORK_BACK,
                open.copy(sdkReady = false),
                true,
                false,
                PreloadDecision.HOLD,
            ),
            Row(
                "gate closed",
                PreloadReason.NETWORK_BACK,
                open.copy(mayRequest = false),
                true,
                false,
                PreloadDecision.HOLD,
            ),
        )
        rows.forEach { row ->
            assertEquals(row.name, row.expected, decidePreload(row.reason, row.gate, row.everRequested, row.running))
        }
    }

    @Test
    fun running_holdsEveryWholePlanReason_ignoresNativeRefill() {
        PreloadReason.entries.forEach { reason ->
            val expected = if (reason == PreloadReason.NATIVE_REFILL) {
                PreloadDecision.IGNORE_RUNNING
            } else {
                PreloadDecision.HOLD
            }
            assertEquals(reason.name, expected, decidePreload(reason, open, everRequested = true, running = true))
        }
    }

    @Test
    fun running_losesToSdkConsentAndNeverRequested() {
        assertEquals(
            PreloadDecision.IGNORE_SDK_NOT_STARTED,
            decidePreload(PreloadReason.NETWORK_BACK, open.copy(sdkStarted = false), true, running = true),
        )
        assertEquals(
            PreloadDecision.IGNORE_CONSENT_DENIED,
            decidePreload(PreloadReason.NETWORK_BACK, open.copy(consentDenied = true), true, running = true),
        )
        assertEquals(
            PreloadDecision.IGNORE_NEVER_REQUESTED,
            decidePreload(PreloadReason.PLACEMENT_ENABLED, open, everRequested = false, running = true),
        )
        // Control (L-055): a closed gate while running is held like any other.
        assertEquals(
            PreloadDecision.HOLD,
            decidePreload(PreloadReason.NETWORK_BACK, open.copy(mayRequest = false), true, running = true),
        )
    }

    @Test
    fun sdkReady_atCallbackOr35s() {
        assertFalse(sdkReadyAt(initStartedAt = 1_000, callbackArrived = false, now = 35_999))
        assertTrue(sdkReadyAt(initStartedAt = 1_000, callbackArrived = false, now = 36_000))
        assertTrue(sdkReadyAt(initStartedAt = 1_000, callbackArrived = true, now = 2_000))
        // S-19: never before the SDK was started, even with a callback.
        assertFalse(sdkReadyAt(initStartedAt = null, callbackArrived = true, now = 10_000_000))
        assertFalse(sdkReadyAt(initStartedAt = null, callbackArrived = false, now = 10_000_000))
    }
}
