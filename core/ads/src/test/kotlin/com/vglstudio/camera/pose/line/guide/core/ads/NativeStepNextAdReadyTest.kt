package com.vglstudio.camera.pose.line.guide.core.ads

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import java.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Whether the pool of a `native_step` placement already holds an ad for the next step of a flow (OB-05, KB-20
 * criterion 10): the rule that keeps one ad for the card of the current step ([nextStepAdReady]); the read in the
 * first frame of a step and every [NEXT_STEP_AD_READ_MILLIS] with an injected count on the test frame clock
 * ([rememberNextStepAdReady]); and the real [AdsController] with a fake loader ([rememberNativeStepNextAdReady]): the
 * pool of the placement, no pool made by the read, the gate of the card (consent, ad-free, placement key, KB-20
 * criterion 9) and one log line per step.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class NativeStepNextAdReadyTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val looper = shadowOf(Looper.getMainLooper())
    private val logger = RecordingAppLogger()
    private val loader = FakeNativeAdLoader()
    private val settings = FakeRemoteSettings().apply { set(AdsSettings.placementEnabled(STEP), true) }
    private val adFree = MutableStateFlow(false)

    private var ready: Boolean? = null
    private var allowed: Boolean? = null

    /** Each composition of the content with the controller: (allowed, ready). */
    private val pairs = mutableListOf<Pair<Boolean, Boolean>>()

    private fun adFreeStatus(flow: StateFlow<Boolean>): AdFreeStatus = object : AdFreeStatus {
        override val adFree = flow
    }

    private fun frame() {
        Snapshot.sendApplyNotifications()
        composeRule.mainClock.advanceTimeByFrame()
    }

    /** One read of the pool: the read interval and a frame. */
    private fun poll() {
        Snapshot.sendApplyNotifications()
        composeRule.mainClock.advanceTimeBy(NEXT_STEP_AD_READ_MILLIS + FRAME_MILLIS)
    }

    // ---- The rule ----

    @Test
    fun nextStepAdReady_keepsOneForCurrentCard() {
        assertFalse(nextStepAdReady(claimable = 0, currentStepHasAd = false))
        assertFalse(nextStepAdReady(claimable = 1, currentStepHasAd = false))
        assertTrue(nextStepAdReady(claimable = 2, currentStepHasAd = false))
        assertFalse(nextStepAdReady(claimable = 0, currentStepHasAd = true))
        assertTrue(nextStepAdReady(claimable = 1, currentStepHasAd = true))
        assertTrue(nextStepAdReady(claimable = 3, currentStepHasAd = true))
        // The current step has no card: the app passes currentStepHasAd = true, one ad is enough.
        assertTrue(nextStepAdReady(claimable = 1, currentStepHasAd = true))
    }

    // ---- The read with an injected count ----

    @Test
    fun noController_false() {
        var control: Boolean? = null
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            ready = rememberNativeStepNextAdReady(STEP, step = 1, currentStepHasAd = true)
            control = rememberNextStepAdReady(allowed = true, step = 1, currentStepHasAd = true) { 1 }
        }
        frame()
        assertEquals(false, ready)
        // Control: the same read with a gate open and an ad in the pool is true.
        assertEquals(true, control)
    }

    @Test
    fun wrongFormat_throws() {
        val error = runCatching {
            composeRule.setContent {
                rememberNativeStepNextAdReady(AdPlacement.GALLERY_NATIVE_MEDIUM, step = 1, currentStepHasAd = true)
            }
            composeRule.waitForIdle()
        }.exceptionOrNull()
        assertTrue("expected IllegalArgumentException, got $error", error is IllegalArgumentException)
    }

    @Test
    fun firstFrame_readsPool() {
        val values = mutableListOf<Boolean>()
        var calls = 0
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            values += rememberNextStepAdReady(allowed = true, step = 1, currentStepHasAd = false) {
                calls++
                2
            }
        }
        val callsBeforeFrame = calls
        frame()
        assertEquals(true, values.first())
        assertTrue("calls before the first frame: $callsBeforeFrame", callsBeforeFrame >= 1)
    }

    @Test
    fun followsPool() {
        var count = 0
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            ready = rememberNextStepAdReady(allowed = true, step = 1, currentStepHasAd = false) { count }
        }
        frame()
        assertEquals(false, ready)
        poll()
        assertEquals(false, ready)

        count = 2
        poll()
        assertEquals(true, ready)
    }

    @Test
    fun cardWaiting_oneAdArrives_staysFalse() {
        var count = 0
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            ready = rememberNextStepAdReady(allowed = true, step = 1, currentStepHasAd = false) { count }
        }
        frame()
        count = 1
        repeat(3) {
            poll()
            assertEquals("poll ${it + 1}", false, ready)
        }

        // Control: a second free ad, the next step has its own.
        count = 2
        poll()
        assertEquals(true, ready)
    }

    @Test
    fun cardClaims_countFallsBeforeReport_staysTrue() {
        var count = 2
        var hasAd by mutableStateOf(false)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            ready = rememberNextStepAdReady(allowed = true, step = 1, currentStepHasAd = hasAd) { count }
        }
        frame()
        assertEquals(true, ready)

        // The card took one, it has not reported it yet.
        count = 1
        poll()
        assertEquals(true, ready)
        count = 0
        poll()
        assertEquals(true, ready)
        hasAd = true
        poll()
        assertEquals(true, ready)
    }

    @Test
    fun cardReportsAd_latestFlagWithLatestCount() {
        val count = 1
        var hasAd by mutableStateOf(false)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            ready = rememberNextStepAdReady(allowed = true, step = 1, currentStepHasAd = hasAd) { count }
        }
        frame()
        assertEquals(false, ready)
        poll()
        assertEquals(false, ready)

        hasAd = true
        poll()
        assertEquals(true, ready)
    }

    @Test
    fun newStep_readsAgain() {
        var count = 2
        var step by mutableStateOf(1)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            ready = rememberNextStepAdReady(allowed = true, step = step, currentStepHasAd = false) { count }
        }
        frame()
        assertEquals(true, ready)

        count = 0
        step = 2
        frame()
        // The true of the step before is not carried over.
        assertEquals(false, ready)

        count = 2
        poll()
        assertEquals(true, ready)
    }

    @Test
    fun gateClosed_falseAndNoRead() {
        var calls = 0
        var gate by mutableStateOf(false)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            ready = rememberNextStepAdReady(allowed = gate, step = 1, currentStepHasAd = false) {
                calls++
                2
            }
        }
        frame()
        assertEquals(false, ready)
        repeat(3) {
            poll()
            assertEquals(false, ready)
        }
        assertEquals(0, calls)

        // Control: the gate opens, the pool is read and the next step has its ad.
        gate = true
        frame()
        assertEquals(true, ready)
        assertTrue("calls: $calls", calls >= 1)
    }

    @Test
    fun gateOpenThenClosed_falseAtOnce_thenReadsAgain() {
        var count = 2
        var gate by mutableStateOf(true)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            ready = rememberNextStepAdReady(allowed = gate, step = 1, currentStepHasAd = false) { count }
        }
        frame()
        assertEquals(true, ready)

        gate = false
        frame()
        assertEquals(false, ready)

        // Open again: read again, the true of the opening before is not kept.
        count = 0
        gate = true
        frame()
        assertEquals(false, ready)

        count = 2
        poll()
        assertEquals(true, ready)
    }

    @Test
    fun background_thenBack_followsLatestCount() {
        var count = 0
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            ready = rememberNextStepAdReady(allowed = true, step = 1, currentStepHasAd = false) { count }
        }
        frame()
        assertEquals(false, ready)

        val scenario = composeRule.activityRule.scenario
        scenario.moveToState(Lifecycle.State.CREATED)
        count = 2
        scenario.moveToState(Lifecycle.State.RESUMED)
        poll()
        // Nothing is asserted while in background: the read shows nothing, waiting is the app's.
        assertEquals(true, ready)
    }

    // ---- The real controller ----

    private fun newController(): AdsController = testAdsController(
        flags = DefaultAdsFlags(settings, adFreeStatus(adFree)),
        logger = logger,
    ).also {
        it.nativeLoaderForTest = loader
        it.openConsentGateForTest()
        it.setAppForegroundForTest(true)
        it.markSdkReadyForTest()
    }

    /** The content of the flow: the gate of the card in [allowed], the new read in [ready], both in [pairs]. */
    private fun setStepContent(controller: AdsController, step: Any, hasAd: () -> Boolean) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CompositionLocalProvider(LocalAdsController provides controller) {
                val gate = rememberNativeStepAdAllowed(STEP)
                val next = rememberNativeStepNextAdReady(STEP, step = step, currentStepHasAd = hasAd())
                allowed = gate
                ready = next
                pairs += gate to next
            }
        }
        settle()
    }

    /** Runs the main looper (controller, pool, card) and the frames that read what it changed. */
    private fun settle() {
        repeat(3) {
            looper.idle()
            frame()
        }
    }

    /** The card of the step collects the states of the pool while [block] runs, then gives its ads back. */
    private fun withCard(controller: AdsController, step: Any, block: (List<NativeAdState>) -> Unit) {
        val cardScope = CoroutineScope(Dispatchers.Main)
        val states = mutableListOf<NativeAdState>()
        try {
            cardScope.launch { controller.stepAd(STEP, MutableStateFlow(step)).collect { states += it } }
            looper.idle()
            block(states)
        } finally {
            cardScope.cancel()
            looper.idle()
        }
    }

    /** The card takes the first ad, the pool loads a second free one, the card reports its ad: the read is true. */
    private fun untilReady(step: Any, setHasAd: (Boolean) -> Unit) {
        assertEquals(1, loader.requests.size)
        loader.deliver(FakeLoadedNativeAd())
        looper.idle()
        looper.idleFor(Duration.ofMillis(PoolCellPolicy.RETRY_MILLIS))
        assertEquals(2, loader.requests.size)
        loader.deliver(FakeLoadedNativeAd())
        looper.idle()
        setHasAd(true)
        poll()
        assertEquals("ready for step $step", true, ready)
        assertEquals(true, allowed)
    }

    /**
     * Changes the gate with [action], then up to two frames until the gate of the card reads false: no composition
     * from the change on has the new read true while the gate is closed, and the read ends false.
     */
    private fun closeGate(action: () -> Unit) {
        val from = pairs.size
        action()
        looper.idle()
        repeat(2) { if (allowed != false) frame() }
        assertEquals(false, allowed)
        val after = pairs.drop(from)
        assertFalse("(allowed, ready) after the change: $after", after.any { (gate, next) -> next && !gate })
        assertEquals(false, ready)
    }

    @Test
    fun withController_readsPoolOfPlacement() {
        val controller = newController()
        var hasAd by mutableStateOf(false)
        setStepContent(controller, step = 1) { hasAd }
        assertEquals(true, allowed)
        assertEquals(false, ready)

        withCard(controller, step = 1) { states ->
            assertEquals(1, loader.requests.size)
            loader.deliver(FakeLoadedNativeAd())
            looper.idle()
            looper.idleFor(Duration.ofMillis(PoolCellPolicy.RETRY_MILLIS))
            assertTrue("card states: $states", states.last() is NativeAdState.Loaded)
            assertEquals(2, loader.requests.size)
            // One ad in use, none free.
            poll()
            assertEquals(false, ready)

            loader.deliver(FakeLoadedNativeAd())
            looper.idle()
            // One free ad, the card has not reported its own yet: the free one is kept for the card.
            poll()
            assertEquals(false, ready)

            hasAd = true
            poll()
            assertEquals(true, ready)
        }
    }

    @Test
    fun withController_noCard_makesNoPool() {
        val controller = newController()
        setStepContent(controller, step = 1) { false }
        assertEquals(true, allowed)
        frame()
        repeat(10) { poll() }
        assertEquals(false, ready)
        assertFalse(logger.entries.any { it.message == POOL_CREATED })
        assertTrue(loader.requests.isEmpty())

        // Control: the card makes the pool, and the line and the request show.
        withCard(controller, step = 1) {
            assertEquals(1, logger.entries.count { it.message == POOL_CREATED })
            assertEquals(1, loader.requests.size)

            // Control only: in background the send gate holds every request.
            controller.setAppForegroundForTest(false)
            looper.idle()
            val requests = loader.requests.size
            repeat(10) { poll() }
            assertEquals(requests, loader.requests.size)
        }
    }

    @Test
    fun withController_consentClosed_falseAtOnce_reopenReadsAgain() {
        val controller = newController()
        var hasAd by mutableStateOf(false)
        setStepContent(controller, step = 1) { hasAd }
        withCard(controller, step = 1) {
            untilReady(step = 1) { hasAd = it }

            closeGate { controller.closeConsentGateForTest() }

            controller.reopenConsentGateForTest()
            looper.idle()
            // The pool closed with consent and dropped its free ad; the one it opens again has none yet.
            poll()
            assertEquals(false, ready)
        }
    }

    @Test
    fun withController_logsOncePerStep() {
        val controller = newController()
        var hasAd by mutableStateOf(false)
        setStepContent(controller, step = STEP_KEY) { hasAd }
        withCard(controller, step = STEP_KEY) {
            untilReady(step = STEP_KEY) { hasAd = it }
            repeat(5) { poll() }
        }
        assertEquals(1, logger.entries.count { it.message == "Step gallery_native_step: next step ad ready" })
        assertFalse(logger.entries.any { it.message.contains(STEP_KEY) })
    }

    @Test
    fun withController_adFreeOn_falseAtOnce() {
        val controller = newController()
        var hasAd by mutableStateOf(false)
        setStepContent(controller, step = 1) { hasAd }
        withCard(controller, step = 1) {
            untilReady(step = 1) { hasAd = it }

            closeGate { adFree.value = true }

            adFree.value = false
            looper.idle()
            frame()
            assertEquals(true, allowed)
            // The pool closed with its placements and dropped its free ad; the fake loader has given none since.
            poll()
            assertEquals(false, ready)
        }
    }

    @Test
    fun withController_placementKeyOff_falseAtOnce() {
        val controller = newController()
        var hasAd by mutableStateOf(false)
        setStepContent(controller, step = 1) { hasAd }
        withCard(controller, step = 1) {
            untilReady(step = 1) { hasAd = it }

            closeGate { settings.set(AdsSettings.placementEnabled(STEP), false) }

            settings.set(AdsSettings.placementEnabled(STEP), true)
            looper.idle()
            poll()
            assertEquals(true, allowed)
            assertEquals(false, ready)
        }
    }

    private companion object {
        val STEP = AdPlacement.GALLERY_NATIVE_STEP
        const val FRAME_MILLIS = 32L
        const val STEP_KEY = "step-key-7"
        const val POOL_CREATED = "Pool native_step created for gallery_native_step"
    }
}
