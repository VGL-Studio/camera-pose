package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.datastore.preferences.core.preferencesOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.analytics.AdFunnelEvent
import com.vglstudio.camera.pose.line.guide.core.analytics.RecordingAdFunnel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Whether the card of a flow step may take an ad now ([rememberNativeStepAdAllowed], OB-04, KB-20 criterion 9)
 * through the real [AdsController]: the same gate as [NativeStepAd] (consent, `is_show_ads` and ad-free,
 * `is_show_<placement>`, ad unit, first-open mark for a first-open-only placement, OB-03), each gate opened then closed
 * again; and the page of a flow step through the same gate ([rememberNativeFullScreenStepReady]). The template declares
 * no first-open-only placement, so the tests set [AdsController.firstOpenOnlyOf].
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class NativeStepAdAllowedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val settings = FakeRemoteSettings().apply {
        set(AdsSettings.placementEnabled(STEP), true)
        set(AdsSettings.placementEnabled(STEP_FULL), true)
    }
    private val adFree = MutableStateFlow(false)
    private val flags = DefaultAdsFlags(settings, adFreeStatus(adFree))
    private var allowed: Boolean? = null
    private var ready: Boolean? = null

    private fun adFreeStatus(flow: StateFlow<Boolean>): AdFreeStatus = object : AdFreeStatus {
        override val adFree = flow
    }

    private fun newController(
        funnel: RecordingAdFunnel = RecordingAdFunnel(),
        laterOpen: Boolean = false,
        firstOpenOnly: AdPlacement? = null,
    ): AdsController {
        val store = CountingPreferencesStore(if (laterOpen) preferencesOf(FIRST_OPEN_KEY to true) else preferencesOf())
        return testAdsController(
            flags = flags,
            funnel = funnel,
            firstOpenMark = FirstOpenMark(store, RecordingAppLogger()),
        ).also { controller -> if (firstOpenOnly != null) controller.firstOpenOnlyOf = { it == firstOpenOnly } }
    }

    @Composable
    private fun StepContent(controller: AdsController?, withCard: Boolean) {
        val content: @Composable () -> Unit = {
            Column {
                if (withCard) NativeStepAd(STEP, step = 0, modifier = Modifier.testTag(SLOT))
                allowed = rememberNativeStepAdAllowed(STEP)
            }
        }
        AdsTestTheme {
            if (controller == null) {
                content()
            } else {
                CompositionLocalProvider(LocalAdsController provides controller, content = content)
            }
        }
    }

    private fun step(action: () -> Unit) {
        composeRule.runOnIdle(action)
        composeRule.waitForIdle()
    }

    private fun eligible(funnel: RecordingAdFunnel): Int = funnel.events.count { it is AdFunnelEvent.Eligible }

    private fun slotHeight() = composeRule.onNodeWithTag(SLOT, useUnmergedTree = true).fetchSemanticsNode().size.height

    private fun hasCard() =
        composeRule.onAllNodesWithTag(NativeAdTestTags.CARD, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun noController_false() {
        var controller: AdsController? by mutableStateOf(newController())
        composeRule.setContent { StepContent(controller, withCard = false) }
        step { controller!!.openConsentGateForTest() }
        // Control: with an ads controller and every gate open, the card may take an ad.
        assertEquals(true, allowed)
        step { controller = null }
        assertEquals(false, allowed)
    }

    @Test
    fun otherFormat_throws() {
        val error = runCatching {
            composeRule.setContent { AdsTestTheme { rememberNativeStepAdAllowed(AdPlacement.GALLERY_NATIVE_MEDIUM) } }
            composeRule.waitForIdle()
        }.exceptionOrNull()
        assertTrue("expected IllegalArgumentException, got $error", error is IllegalArgumentException)
    }

    @Test
    fun followsEveryGate_openThenClosed() {
        val controller = newController()
        composeRule.setContent { StepContent(controller, withCard = false) }
        composeRule.waitForIdle()
        assertEquals("no consent yet", false, allowed)
        step { controller.openConsentGateForTest() }
        assertEquals("consent given", true, allowed)
        step { controller.closeConsentGateForTest() }
        assertEquals("consent withdrawn", false, allowed)
        step { controller.reopenConsentGateForTest() }
        assertEquals("consent given again", true, allowed)
        step { settings.set(AdsSettings.IS_SHOW_ADS, false) }
        assertEquals("master switch off", false, allowed)
        step { settings.set(AdsSettings.IS_SHOW_ADS, true) }
        assertEquals("master switch on", true, allowed)
        step { settings.set(AdsSettings.placementEnabled(STEP), false) }
        assertEquals("placement off", false, allowed)
        step { settings.set(AdsSettings.placementEnabled(STEP), true) }
        assertEquals("placement on", true, allowed)
        step { adFree.value = true }
        assertEquals("ad-free", false, allowed)
        step { adFree.value = false }
        assertEquals("not ad-free", true, allowed)
    }

    @Test
    fun stepAdAndAllowed_sameGate() {
        val funnel = RecordingAdFunnel()
        val controller = newController(funnel)
        composeRule.setContent { StepContent(controller, withCard = true) }
        composeRule.waitForIdle()
        assertEquals(0 to false, eligible(funnel) to allowed)
        step { controller.openConsentGateForTest() }
        assertEquals(1 to true, eligible(funnel) to allowed)
        step { controller.closeConsentGateForTest() }
        assertEquals(1 to false, eligible(funnel) to allowed)
        step { controller.reopenConsentGateForTest() }
        assertEquals(2 to true, eligible(funnel) to allowed)
        step { adFree.value = true }
        assertEquals(2 to false, eligible(funnel) to allowed)
        step { adFree.value = false }
        assertEquals(3 to true, eligible(funnel) to allowed)
    }

    @Test
    fun stepAd_gateClosed_takesNoSpace() {
        val controller = newController()
        composeRule.setContent { StepContent(controller, withCard = true) }
        step { controller.openConsentGateForTest() }
        // Control: the gate open, the card may take an ad.
        assertEquals(true, allowed)
        step { controller.closeConsentGateForTest() }
        assertEquals(0, slotHeight())
        assertFalse(hasCard())
        assertEquals(false, allowed)
    }

    @Test
    fun firstOpenOnly_laterOpen_falseAndNoEligible() {
        val laterFunnel = RecordingAdFunnel()
        val firstFunnel = RecordingAdFunnel()
        val later = newController(laterFunnel, laterOpen = true, firstOpenOnly = STEP)
        val first = newController(firstFunnel, laterOpen = false, firstOpenOnly = STEP)
        var controller: AdsController by mutableStateOf(later)
        composeRule.setContent { key(controller) { StepContent(controller, withCard = true) } }
        step { later.openConsentGateForTest() }
        assertEquals(false, allowed)
        assertEquals(0, eligible(laterFunnel))
        assertEquals(0, slotHeight())

        // Control: the first open, the same placement may take an ad.
        step {
            first.openConsentGateForTest()
            controller = first
        }
        assertEquals(true, allowed)
        assertEquals(1, eligible(firstFunnel))
        // The first open still closes with consent.
        step { first.closeConsentGateForTest() }
        assertEquals(false, allowed)
    }

    @Composable
    private fun PageReady(controller: AdsController) {
        CompositionLocalProvider(LocalAdsController provides controller) {
            ready = rememberNativeFullScreenStepReady(STEP_FULL)
        }
    }

    private fun AdsController.withPreloadedPage(): AdsController = apply {
        openConsentGateForTest()
        putPreloadedNativeFullScreenForTest(STEP_FULL, FakeLoadedNativeAd())
    }

    /** Waits [millis] of real time, so the retry of the page every 500 ms has run a few times. */
    private fun waitReal(millis: Long) {
        val until = System.currentTimeMillis() + millis
        composeRule.waitUntil(millis + WAIT_MILLIS) { System.currentTimeMillis() >= until }
        composeRule.waitForIdle()
    }

    @Test
    fun fullScreenStepReady_followsGate_realController() {
        val controller = newController()
        composeRule.runOnIdle { controller.withPreloadedPage() }
        composeRule.setContent { PageReady(controller) }
        composeRule.waitUntil(WAIT_MILLIS) { ready == true }
        step { controller.closeConsentGateForTest() }
        assertEquals(false, ready)
        step { controller.reopenConsentGateForTest() }
        composeRule.waitUntil(WAIT_MILLIS) { ready == true }
    }

    @Test
    fun fullScreenStepReady_firstOpenOnly_laterOpen_false() {
        // Control: the first open, the page of the same placement is ready.
        val first = newController(laterOpen = false, firstOpenOnly = STEP_FULL)
        val later = newController(laterOpen = true, firstOpenOnly = STEP_FULL)
        var controller: AdsController by mutableStateOf(first)
        composeRule.runOnIdle {
            first.withPreloadedPage()
            later.withPreloadedPage()
        }
        composeRule.setContent { key(controller) { PageReady(controller) } }
        composeRule.waitUntil(WAIT_MILLIS) { ready == true }

        step { controller = later }
        waitReal(LATER_OPEN_WAIT_MILLIS)
        assertEquals(false, ready)
    }

    private companion object {
        val STEP = AdPlacement.GALLERY_NATIVE_STEP
        val STEP_FULL = AdPlacement.GALLERY_NATIVE_STEP_FULL
        const val SLOT = "step_slot"
        const val WAIT_MILLIS = 3_000L

        /** Three retries of the page (every 500 ms). */
        const val LATER_OPEN_WAIT_MILLIS = 1_500L
    }
}
