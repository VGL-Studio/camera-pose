package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Held, dropped and refill preloads of the plan on a virtual clock (S-09, S-21, S-23, S-24, S-25, HD-02). */
@OptIn(ExperimentalCoroutinesApi::class)
class AdPreloaderTest {
    private val logger = RecordingAppLogger()
    private val events = mutableListOf<String>()
    private var gate = OPEN

    private fun TestScope.now(): Long = testScheduler.currentTime

    private fun TestScope.slot(format: AdFormat, settleAt: Long? = 0, order: MutableList<AdFormat>? = null) =
        FakePreloadSlot(format, { now() }, settleAt, order)

    private fun TestScope.preloader(slots: List<PreloadSlot>) = AdPreloader(
        scope = backgroundScope,
        runner = PreloadPlanRunner(MonotonicClock { now() }, logger),
        slotsOf = { format -> slots.filter { it.format == format } },
        gate = {
            events += "gate"
            gate
        },
        askConsentAgain = { events += "ask" },
        logger = logger,
    )

    /** Moves the virtual clock to [millis] after the start and runs what is due. */
    private fun TestScope.at(millis: Long) {
        advanceTimeBy(millis - testScheduler.currentTime)
        runCurrent()
    }

    private fun logged(message: String) = logger.entries.any { it.tag == "Ads" && it.message == message }

    @Test
    fun planSetBeforeFirstRun_isUsed() = runTest {
        val interstitial = slot(AdFormat.INTERSTITIAL)
        val nativeFullScreen = slot(AdFormat.NATIVE_FULL_SCREEN)
        val preloader = preloader(listOf(interstitial, nativeFullScreen))

        preloader.setPlan(AdPreloadPlan(listOf(setOf(AdFormat.INTERSTITIAL))))
        preloader.request(PreloadReason.SDK_READY)
        runCurrent()

        assertEquals(1, interstitial.loadCalls.size)
        assertEquals(0, nativeFullScreen.loadCalls.size)
        assertTrue(logged("Preload SDK_READY: RUN"))

        // Control: the default plan loads both.
        val otherInterstitial = slot(AdFormat.INTERSTITIAL)
        val otherNative = slot(AdFormat.NATIVE_FULL_SCREEN)
        preloader(listOf(otherInterstitial, otherNative)).request(PreloadReason.SDK_READY)
        runCurrent()
        assertEquals(1, otherInterstitial.loadCalls.size)
        assertEquals(1, otherNative.loadCalls.size)
    }

    @Test
    fun planSetWhileRunning_appliesNextRun() = runTest {
        val interstitial = slot(AdFormat.INTERSTITIAL, settleAt = null)
        val nativeFullScreen = slot(AdFormat.NATIVE_FULL_SCREEN)
        val preloader = preloader(listOf(interstitial, nativeFullScreen))
        preloader.request(PreloadReason.SDK_READY)
        runCurrent()

        at(100)
        preloader.setPlan(AdPreloadPlan(listOf(setOf(AdFormat.NATIVE_FULL_SCREEN))))
        assertTrue(logger.entries.any { it.tag == "Ads" && it.level == LogLevel.WARN })

        at(7_000)
        preloader.request(PreloadReason.NETWORK_BACK)
        runCurrent()
        assertEquals(listOf(0L), interstitial.loadCalls)
        assertEquals(listOf(0L, 7_000L), nativeFullScreen.loadCalls)
    }

    @Test
    fun heldUntilSdkReady() = runTest {
        val interstitial = slot(AdFormat.INTERSTITIAL)
        val preloader = preloader(listOf(interstitial))
        gate = OPEN.copy(sdkReady = false)

        preloader.request(PreloadReason.SDK_READY)
        runCurrent()
        assertEquals(0, interstitial.loadCalls.size)

        gate = OPEN
        preloader.onGateChanged()
        runCurrent()
        assertEquals(1, interstitial.loadCalls.size)
    }

    @Test
    fun heldWhileGateClosed_runsOnceWhenOpen() = runTest {
        val interstitial = slot(AdFormat.INTERSTITIAL)
        val preloader = preloader(listOf(interstitial))
        preloader.request(PreloadReason.SDK_READY)
        runCurrent()
        assertEquals(1, interstitial.loadCalls.size)

        gate = OPEN.copy(mayRequest = false)
        preloader.request(PreloadReason.NETWORK_BACK)
        runCurrent()
        preloader.request(PreloadReason.NETWORK_BACK)
        runCurrent()
        assertEquals(1, interstitial.loadCalls.size)

        gate = OPEN
        preloader.onGateChanged()
        runCurrent()
        preloader.onGateChanged()
        runCurrent()
        assertEquals(2, interstitial.loadCalls.size)
    }

    @Test
    fun consentDenied_dropsHeldRequest() = runTest {
        val interstitial = slot(AdFormat.INTERSTITIAL)
        val preloader = preloader(listOf(interstitial))
        preloader.request(PreloadReason.SDK_READY)
        runCurrent()
        gate = OPEN.copy(mayRequest = false)
        preloader.request(PreloadReason.NETWORK_BACK)
        runCurrent()

        gate = OPEN.copy(mayRequest = false, consentDenied = true)
        preloader.onGateChanged()
        gate = OPEN
        preloader.onGateChanged()
        runCurrent()
        assertEquals(1, interstitial.loadCalls.size)

        // S-14: the gate opened again by a new consent asks for its own preload.
        preloader.request(PreloadReason.GATE_REOPENED)
        runCurrent()
        assertEquals(2, interstitial.loadCalls.size)
    }

    @Test
    fun noHold_beforeSdkStarted() = runTest {
        val interstitial = slot(AdFormat.INTERSTITIAL)
        val preloader = preloader(listOf(interstitial))
        gate = OPEN.copy(sdkStarted = false, mayRequest = false)

        preloader.request(PreloadReason.SDK_READY)
        runCurrent()
        gate = OPEN
        preloader.onGateChanged()
        runCurrent()
        assertEquals(0, interstitial.loadCalls.size)

        // Control (L-055): the same request with the SDK started is held, then runs.
        val other = slot(AdFormat.INTERSTITIAL)
        val otherPreloader = preloader(listOf(other))
        gate = OPEN.copy(mayRequest = false)
        otherPreloader.request(PreloadReason.SDK_READY)
        runCurrent()
        gate = OPEN
        otherPreloader.onGateChanged()
        runCurrent()
        assertEquals(1, other.loadCalls.size)
    }

    @Test
    fun noHold_beforeFirstPreload() = runTest {
        val interstitial = slot(AdFormat.INTERSTITIAL)
        val preloader = preloader(listOf(interstitial))
        gate = OPEN.copy(mayRequest = false)

        preloader.request(PreloadReason.NETWORK_BACK)
        runCurrent()
        gate = OPEN
        preloader.onGateChanged()
        runCurrent()
        assertEquals(0, interstitial.loadCalls.size)

        // Control (L-055): after the first preload, the same request is held, then runs.
        val other = slot(AdFormat.INTERSTITIAL)
        val otherPreloader = preloader(listOf(other))
        otherPreloader.request(PreloadReason.SDK_READY)
        runCurrent()
        gate = OPEN.copy(mayRequest = false)
        otherPreloader.request(PreloadReason.NETWORK_BACK)
        runCurrent()
        gate = OPEN
        otherPreloader.onGateChanged()
        runCurrent()
        assertEquals(2, other.loadCalls.size)
    }

    @Test
    fun noOverlap_whileRunning() = runTest {
        val interstitial = slot(AdFormat.INTERSTITIAL, settleAt = null)
        val preloader = preloader(listOf(interstitial))
        preloader.request(PreloadReason.SDK_READY)
        runCurrent()

        at(1_000)
        preloader.request(PreloadReason.NETWORK_BACK)
        runCurrent()

        assertEquals(listOf(0L), interstitial.loadCalls)
        assertTrue(logged("Preload NETWORK_BACK: HOLD"))

        // T-037: the held preload runs once the running one ends, never on top of it.
        at(5_999)
        assertEquals(listOf(0L), interstitial.loadCalls)
        at(6_000)
        assertEquals(listOf(0L, 6_000L), interstitial.loadCalls)
    }

    @Test
    fun placementEnabled_duringFirstRun_runsOnceAfterIt() = runTest {
        val interstitial = slot(AdFormat.INTERSTITIAL, settleAt = null)
        val preloader = preloader(listOf(interstitial))
        preloader.request(PreloadReason.SDK_READY)
        runCurrent()

        at(2_000)
        preloader.request(PreloadReason.PLACEMENT_ENABLED)
        runCurrent()
        assertEquals(listOf(0L), interstitial.loadCalls)
        assertTrue(logged("Preload PLACEMENT_ENABLED: HOLD"))

        at(6_000)
        assertEquals(listOf(0L, 6_000L), interstitial.loadCalls)
        assertTrue(logged("Held preload PLACEMENT_ENABLED: run"))

        at(20_000)
        assertEquals(listOf(0L, 6_000L), interstitial.loadCalls)
    }

    @Test
    fun gateReopened_duringLaterRun_runsOnceAfterIt() = runTest {
        val interstitial = slot(AdFormat.INTERSTITIAL)
        val preloader = preloader(listOf(interstitial))
        preloader.request(PreloadReason.SDK_READY)
        runCurrent()
        interstitial.settleAt = null

        at(1_000)
        preloader.request(PreloadReason.NETWORK_BACK)
        runCurrent()
        at(3_000)
        preloader.request(PreloadReason.GATE_REOPENED)
        runCurrent()
        assertTrue(logged("Preload GATE_REOPENED: HOLD"))

        at(7_000)
        assertEquals(listOf(0L, 1_000L, 7_000L), interstitial.loadCalls)
        at(20_000)
        assertEquals(listOf(0L, 1_000L, 7_000L), interstitial.loadCalls)
    }

    @Test
    fun networkReturned_duringHeldRun_runsOnceAfterIt() = runTest {
        val interstitial = slot(AdFormat.INTERSTITIAL)
        val preloader = preloader(listOf(interstitial))
        preloader.request(PreloadReason.SDK_READY)
        runCurrent()
        interstitial.settleAt = null

        at(500)
        gate = OPEN.copy(mayRequest = false)
        preloader.request(PreloadReason.PLACEMENT_ENABLED)
        runCurrent()
        at(1_000)
        gate = OPEN
        preloader.onGateChanged()
        runCurrent()

        at(2_000)
        val resets = preloader.nativeRetryResets.value
        preloader.onNetworkReturned()
        assertEquals(resets + 1, preloader.nativeRetryResets.value)
        runCurrent()
        assertTrue(logged("Preload NETWORK_BACK: HOLD"))

        at(7_000)
        assertEquals(listOf(0L, 1_000L, 7_000L), interstitial.loadCalls)
    }

    @Test
    fun requestDuringNativeRefill_runsWholePlanAfterIt() = runTest {
        val interstitial = slot(AdFormat.INTERSTITIAL)
        val small = slot(AdFormat.NATIVE_SMALL)
        val preloader = preloader(listOf(interstitial, small))
        preloader.setPlan(AdPreloadPlan(listOf(setOf(AdFormat.INTERSTITIAL, AdFormat.NATIVE_SMALL))))
        preloader.request(PreloadReason.SDK_READY)
        runCurrent()
        small.settleAt = null

        at(100)
        preloader.onActivityResumed(fullScreenShowing = false)
        runCurrent()
        at(1_000)
        preloader.request(PreloadReason.PLACEMENT_ENABLED)
        runCurrent()
        assertTrue(logged("Preload PLACEMENT_ENABLED: HOLD"))

        at(6_100)
        assertEquals(listOf(0L, 6_100L), interstitial.loadCalls)
        assertEquals(listOf(0L, 100L, 6_100L), small.loadCalls)
    }

    private enum class RunKind { FIRST, LATER, HELD, NATIVE_REFILL }

    @Test
    fun wholePlanRequests_duringAnyRun_areHeldAndRunOnceAfter() = runTest {
        val reasons = listOf(PreloadReason.GATE_REOPENED, PreloadReason.NETWORK_BACK, PreloadReason.PLACEMENT_ENABLED)
        reasons.forEach { reason ->
            RunKind.entries.forEach { kind ->
                val name = "$reason during $kind"
                gate = OPEN
                val interstitial = slot(AdFormat.INTERSTITIAL)
                val small = slot(AdFormat.NATIVE_SMALL)
                val preloader = preloader(listOf(interstitial, small))
                preloader.setPlan(AdPreloadPlan(listOf(setOf(AdFormat.INTERSTITIAL, AdFormat.NATIVE_SMALL))))
                if (kind == RunKind.FIRST) {
                    interstitial.settleAt = null
                    small.settleAt = null
                }
                preloader.request(PreloadReason.SDK_READY)
                runCurrent()
                when (kind) {
                    RunKind.FIRST -> Unit
                    RunKind.LATER -> {
                        interstitial.settleAt = null
                        small.settleAt = null
                        preloader.request(PreloadReason.NETWORK_BACK)
                        runCurrent()
                    }
                    RunKind.HELD -> {
                        interstitial.settleAt = null
                        small.settleAt = null
                        gate = OPEN.copy(mayRequest = false)
                        preloader.request(PreloadReason.PLACEMENT_ENABLED)
                        runCurrent()
                        gate = OPEN
                        preloader.onGateChanged()
                        runCurrent()
                    }
                    RunKind.NATIVE_REFILL -> {
                        small.settleAt = null
                        preloader.onActivityResumed(fullScreenShowing = false)
                        runCurrent()
                    }
                }
                assertTrue(name, preloader.running)
                val start = now()
                val before = interstitial.loadCalls.size

                at(start + 1_000)
                if (reason == PreloadReason.NETWORK_BACK) preloader.onNetworkReturned() else preloader.request(reason)
                runCurrent()
                at(start + 5_999)
                assertEquals(name, emptyList<Long>(), interstitial.loadCalls.drop(before))
                at(start + 6_000)
                assertEquals(name, listOf(start + 6_000), interstitial.loadCalls.drop(before))
                at(start + 20_000)
                assertEquals(name, listOf(start + 6_000), interstitial.loadCalls.drop(before))
            }
        }
    }

    @Test
    fun twoRequestsDuringRun_runOnceAfterIt() = runTest {
        val interstitial = slot(AdFormat.INTERSTITIAL, settleAt = null)
        val preloader = preloader(listOf(interstitial))
        preloader.request(PreloadReason.SDK_READY)
        runCurrent()

        at(1_000)
        preloader.request(PreloadReason.PLACEMENT_ENABLED)
        runCurrent()
        at(2_000)
        preloader.onNetworkReturned()
        runCurrent()
        at(3_000)
        preloader.request(PreloadReason.GATE_REOPENED)
        runCurrent()

        at(6_000)
        assertEquals(listOf(0L, 6_000L), interstitial.loadCalls)
        at(30_000)
        assertEquals(listOf(0L, 6_000L), interstitial.loadCalls)
    }

    @Test
    fun heldDuringRun_gateClosedAtEnd_runsWhenGateOpens() = runTest {
        val interstitial = slot(AdFormat.INTERSTITIAL, settleAt = null)
        val preloader = preloader(listOf(interstitial))
        preloader.request(PreloadReason.SDK_READY)
        runCurrent()

        at(1_000)
        preloader.request(PreloadReason.NETWORK_BACK)
        runCurrent()
        at(3_000)
        gate = OPEN.copy(mayRequest = false)
        at(6_000)
        assertEquals(listOf(0L), interstitial.loadCalls)

        at(8_000)
        gate = OPEN
        preloader.onGateChanged()
        runCurrent()
        assertEquals(listOf(0L, 8_000L), interstitial.loadCalls)
    }

    @Test
    fun requestRun_whileHeldForClosedGate_clearsHeld() = runTest {
        val interstitial = slot(AdFormat.INTERSTITIAL)
        val preloader = preloader(listOf(interstitial))
        preloader.request(PreloadReason.SDK_READY)
        runCurrent()
        interstitial.settleAt = null

        at(1_000)
        gate = OPEN.copy(mayRequest = false)
        preloader.request(PreloadReason.NETWORK_BACK)
        runCurrent()

        // The real order of GATE_REOPENED: the request comes before the gate collector.
        at(2_000)
        gate = OPEN
        preloader.request(PreloadReason.GATE_REOPENED)
        preloader.onGateChanged()
        runCurrent()

        at(30_000)
        assertEquals(listOf(0L, 2_000L), interstitial.loadCalls)
        assertTrue(logged("Held preload NETWORK_BACK: covered by GATE_REOPENED run"))
        assertTrue(!logged("Held preload NETWORK_BACK: run"))
    }

    @Test
    fun requestRun_whileHeldFromRunAndGateClosed_clearsHeld() = runTest {
        val interstitial = slot(AdFormat.INTERSTITIAL, settleAt = null)
        val preloader = preloader(listOf(interstitial))
        preloader.request(PreloadReason.SDK_READY)
        runCurrent()

        at(1_000)
        preloader.request(PreloadReason.PLACEMENT_ENABLED)
        runCurrent()
        at(3_000)
        gate = OPEN.copy(mayRequest = false)
        at(6_000)
        assertEquals(listOf(0L), interstitial.loadCalls)

        at(8_000)
        gate = OPEN
        preloader.request(PreloadReason.GATE_REOPENED)
        preloader.onGateChanged()
        runCurrent()

        at(30_000)
        assertEquals(listOf(0L, 8_000L), interstitial.loadCalls)
        assertTrue(logged("Held preload PLACEMENT_ENABLED: covered by GATE_REOPENED run"))
    }

    @Test
    fun everyRunReason_whileHeld_clearsHeld() = runTest {
        val reasons = listOf(PreloadReason.GATE_REOPENED, PreloadReason.NETWORK_BACK, PreloadReason.PLACEMENT_ENABLED)
        for (reason in reasons) {
            gate = OPEN
            val interstitial = slot(AdFormat.INTERSTITIAL)
            val preloader = preloader(listOf(interstitial))
            val start = now()
            preloader.request(PreloadReason.SDK_READY)
            runCurrent()
            interstitial.settleAt = null

            val retriesNetwork = reason == PreloadReason.NETWORK_BACK
            val heldReason = if (retriesNetwork) PreloadReason.PLACEMENT_ENABLED else PreloadReason.NETWORK_BACK
            at(start + 1_000)
            gate = OPEN.copy(mayRequest = false)
            preloader.request(heldReason)
            runCurrent()

            at(start + 2_000)
            gate = OPEN
            if (retriesNetwork) preloader.onNetworkReturned() else preloader.request(reason)
            preloader.onGateChanged()
            runCurrent()

            at(start + 22_000)
            assertEquals("$reason", listOf(start, start + 2_000), interstitial.loadCalls)
        }
    }

    @Test
    fun gateOpens_withoutRequest_heldRunsOnce() = runTest {
        val interstitial = slot(AdFormat.INTERSTITIAL)
        val preloader = preloader(listOf(interstitial))
        preloader.request(PreloadReason.SDK_READY)
        runCurrent()
        interstitial.settleAt = null

        at(1_000)
        gate = OPEN.copy(mayRequest = false)
        preloader.request(PreloadReason.NETWORK_BACK)
        runCurrent()

        at(2_000)
        gate = OPEN
        preloader.onGateChanged()
        runCurrent()

        at(30_000)
        assertEquals(listOf(0L, 2_000L), interstitial.loadCalls)
        assertTrue(logged("Held preload NETWORK_BACK: run"))
        assertTrue(logger.entries.none { it.tag == "Ads" && it.message.contains("covered by") })
    }

    @Test
    fun requestRun_clearsHeld_thenRequestDuringRunIsHeld() = runTest {
        val interstitial = slot(AdFormat.INTERSTITIAL)
        val preloader = preloader(listOf(interstitial))
        preloader.request(PreloadReason.SDK_READY)
        runCurrent()
        interstitial.settleAt = null

        at(1_000)
        gate = OPEN.copy(mayRequest = false)
        preloader.request(PreloadReason.NETWORK_BACK)
        runCurrent()

        at(2_000)
        gate = OPEN
        preloader.request(PreloadReason.GATE_REOPENED)
        preloader.onGateChanged()
        runCurrent()
        assertTrue(logged("Held preload NETWORK_BACK: covered by GATE_REOPENED run"))

        at(3_000)
        preloader.request(PreloadReason.PLACEMENT_ENABLED)
        runCurrent()
        assertTrue(logged("Preload PLACEMENT_ENABLED: HOLD"))

        at(8_000)
        assertEquals(listOf(0L, 2_000L, 8_000L), interstitial.loadCalls)
        assertTrue(logged("Held preload PLACEMENT_ENABLED: run"))
        at(30_000)
        assertEquals(listOf(0L, 2_000L, 8_000L), interstitial.loadCalls)
    }

    @Test
    fun heldDuringRun_consentDeniedMidRun_dropsAtOnce() = runTest {
        val interstitial = slot(AdFormat.INTERSTITIAL, settleAt = null)
        val preloader = preloader(listOf(interstitial))
        preloader.request(PreloadReason.SDK_READY)
        runCurrent()

        at(1_000)
        preloader.request(PreloadReason.PLACEMENT_ENABLED)
        runCurrent()
        at(3_000)
        gate = OPEN.copy(consentDenied = true)
        preloader.onGateChanged()
        assertTrue(logged("Held preload PLACEMENT_ENABLED: dropped, IGNORE_CONSENT_DENIED"))
        at(6_000)
        assertEquals(listOf(0L), interstitial.loadCalls)
        at(8_000)
        gate = OPEN
        preloader.onGateChanged()
        runCurrent()
        assertEquals(listOf(0L), interstitial.loadCalls)

        // Control (L-055): the gate changing with consent still given keeps the held preload.
        val other = slot(AdFormat.INTERSTITIAL, settleAt = null)
        val otherPreloader = preloader(listOf(other))
        val start = now()
        otherPreloader.request(PreloadReason.SDK_READY)
        runCurrent()
        at(start + 1_000)
        otherPreloader.request(PreloadReason.PLACEMENT_ENABLED)
        runCurrent()
        at(start + 3_000)
        otherPreloader.onGateChanged()
        runCurrent()
        at(start + 6_000)
        assertEquals(listOf(start, start + 6_000), other.loadCalls)
    }

    @Test
    fun heldDuringRun_consentDeniedAtEnd_isDropped() = runTest {
        assertHeldDroppedByGateAtEnd(OPEN.copy(consentDenied = true), "IGNORE_CONSENT_DENIED")
    }

    @Test
    fun heldDuringRun_sdkNotStartedAtEnd_isDropped() = runTest {
        assertHeldDroppedByGateAtEnd(OPEN.copy(sdkStarted = false), "IGNORE_SDK_NOT_STARTED")
    }

    /** The held preload decides again when the running one ends: [atEnd] drops it, then an open gate never runs it. */
    private fun TestScope.assertHeldDroppedByGateAtEnd(atEnd: PreloadGate, decision: String) {
        val interstitial = slot(AdFormat.INTERSTITIAL, settleAt = null)
        val preloader = preloader(listOf(interstitial))
        preloader.request(PreloadReason.SDK_READY)
        runCurrent()

        at(1_000)
        preloader.request(PreloadReason.PLACEMENT_ENABLED)
        runCurrent()
        at(3_000)
        gate = atEnd
        at(6_000)
        assertEquals(listOf(0L), interstitial.loadCalls)
        assertTrue(logged("Held preload PLACEMENT_ENABLED: dropped, $decision"))

        at(8_000)
        gate = OPEN
        preloader.onGateChanged()
        runCurrent()
        assertEquals(listOf(0L), interstitial.loadCalls)

        // Control (L-055): the same steps with the gate unchanged run the held preload at the end.
        val other = slot(AdFormat.INTERSTITIAL, settleAt = null)
        val otherPreloader = preloader(listOf(other))
        val start = now()
        otherPreloader.request(PreloadReason.SDK_READY)
        runCurrent()
        at(start + 1_000)
        otherPreloader.request(PreloadReason.PLACEMENT_ENABLED)
        runCurrent()
        at(start + 6_000)
        assertEquals(listOf(start, start + 6_000), other.loadCalls)
    }

    @Test
    fun nativeRefill_duringRun_isNotHeld() = runTest {
        val small = slot(AdFormat.NATIVE_SMALL)
        val preloader = preloader(listOf(small))
        preloader.setPlan(AdPreloadPlan(listOf(setOf(AdFormat.NATIVE_SMALL))))
        preloader.request(PreloadReason.SDK_READY)
        runCurrent()
        small.settleAt = null

        at(1_000)
        preloader.request(PreloadReason.NETWORK_BACK)
        runCurrent()
        at(2_000)
        preloader.onActivityResumed(fullScreenShowing = false)
        runCurrent()
        at(20_000)
        assertEquals(listOf(0L, 1_000L), small.loadCalls)

        // Control (L-055): a resume with no plan running refills, so the test sees a refill.
        at(21_000)
        preloader.onActivityResumed(fullScreenShowing = false)
        runCurrent()
        assertEquals(listOf(0L, 1_000L, 21_000L), small.loadCalls)
    }

    @Test
    fun everyRequest_asksConsentAgain() = runTest {
        val preloader = preloader(listOf(slot(AdFormat.INTERSTITIAL)))

        preloader.request(PreloadReason.SDK_READY)
        preloader.request(PreloadReason.NETWORK_BACK)
        preloader.request(PreloadReason.PLACEMENT_ENABLED)

        assertEquals(listOf("ask", "gate", "ask", "gate", "ask", "gate"), events)
    }

    @Test
    fun networkReturn_preloadsAndResetsNativeWait() = runTest {
        val interstitial = slot(AdFormat.INTERSTITIAL)
        val nativeFullScreen = slot(AdFormat.NATIVE_FULL_SCREEN)
        val preloader = preloader(listOf(interstitial, nativeFullScreen))
        preloader.request(PreloadReason.SDK_READY)
        runCurrent()
        val resets = preloader.nativeRetryResets.value

        at(5_000)
        preloader.onNetworkReturned()
        runCurrent()

        assertEquals(listOf(0L, 5_000L), interstitial.loadCalls)
        assertEquals(listOf(0L, 5_000L), nativeFullScreen.loadCalls)
        assertEquals(resets + 1, preloader.nativeRetryResets.value)
    }

    @Test
    fun activityResumed_refillsNativeInPlanOrder() = runTest {
        val order = mutableListOf<AdFormat>()
        val step = slot(AdFormat.NATIVE_STEP, order = order)
        val interstitial = slot(AdFormat.INTERSTITIAL, order = order)
        val small = slot(AdFormat.NATIVE_SMALL, order = order)
        val preloader = preloader(listOf(step, interstitial, small))
        preloader.setPlan(
            AdPreloadPlan(listOf(setOf(AdFormat.NATIVE_STEP), setOf(AdFormat.INTERSTITIAL, AdFormat.NATIVE_SMALL))),
        )
        preloader.request(PreloadReason.SDK_READY)
        runCurrent()
        order.clear()

        preloader.onActivityResumed(fullScreenShowing = false)
        runCurrent()

        assertEquals(listOf(AdFormat.NATIVE_STEP, AdFormat.NATIVE_SMALL), order)
        assertEquals(1, interstitial.loadCalls.size)
    }

    @Test
    fun activityResumed_skipsWhenNeverFinishedRunningOrFullScreenShowing() = runTest {
        val small = slot(AdFormat.NATIVE_SMALL)
        val preloader = preloader(listOf(small))
        preloader.setPlan(AdPreloadPlan(listOf(setOf(AdFormat.NATIVE_SMALL))))

        // The plan never finished.
        preloader.onActivityResumed(fullScreenShowing = false)
        runCurrent()
        assertEquals(0, small.loadCalls.size)

        preloader.request(PreloadReason.SDK_READY)
        runCurrent()
        assertEquals(1, small.loadCalls.size)

        // A full-screen ad is on screen.
        preloader.onActivityResumed(fullScreenShowing = true)
        runCurrent()
        assertEquals(1, small.loadCalls.size)

        // Control (L-055): the same resume with nothing in the way refills.
        preloader.onActivityResumed(fullScreenShowing = false)
        runCurrent()
        assertEquals(2, small.loadCalls.size)

        // A plan is running.
        small.settleAt = null
        at(1_000)
        preloader.request(PreloadReason.NETWORK_BACK)
        runCurrent()
        assertEquals(3, small.loadCalls.size)
        preloader.onActivityResumed(fullScreenShowing = false)
        runCurrent()
        assertEquals(3, small.loadCalls.size)
    }

    private companion object {
        val OPEN = PreloadGate(sdkStarted = true, sdkReady = true, mayRequest = true, consentDenied = false)
    }
}
