package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The page of a flow step through the real [AdsController] when the activity is built again (rotation, fold, unfold;
 * plan T-041, O-01, T-040): the old ad is destroyed once and never drawn again, the slot loads again, the page built
 * again shows the static placeholder, then a new ad. The retry of the page every 500 ms runs on the real clock in
 * Robolectric (plan T-041, section 4.1), so the waits are bounded real waits.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class NativeFullScreenStepRecreateTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val logger = RecordingAppLogger()
    private val settings = FakeRemoteSettings().apply {
        set(AdsSettings.placementEnabled(AdPlacement.GALLERY_NATIVE_STEP_FULL), true)
    }
    private val controller = testAdsController(
        logger = logger,
        flags = DefaultAdsFlags(settings),
        consentDebug = ConsentDebugOptions.EEA_TEST_DEVICE,
    )
    private val suppressor = ForegroundAdSuppressor()
    private val suppression = ResumeSuppression(suppressor, logger)
    private val reports = mutableListOf<Boolean>()

    private val first = FakeLoadedNativeAd(NativeAdAssets(FIRST, null, null, "Install", null, hasMedia = true))
    private val second = FakeLoadedNativeAd(NativeAdAssets(SECOND, null, null, "Install", null, hasMedia = true))
    private val third = FakeLoadedNativeAd(NativeAdAssets(THIRD, null, null, "Install", null, hasMedia = true))

    @Composable
    private fun Page() {
        CompositionLocalProvider(LocalAdsController provides controller) {
            AppTheme {
                AdsHostContent(suppression, suppressor) {
                    NativeFullScreenStep(
                        AdPlacement.GALLERY_NATIVE_STEP_FULL,
                        Modifier.fillMaxSize(),
                        shown = true,
                    ) { reports += it }
                }
            }
        }
    }

    /** Opens consent, puts [first] in the slot and shows the page with it. */
    private fun showFirst() {
        composeRule.runOnUiThread {
            controller.openConsentGateForTest()
            controller.putPreloadedNativeFullScreenForTest(AdPlacement.GALLERY_NATIVE_STEP_FULL, first)
        }
        composeRule.setContent { Page() }
        waitForText(FIRST)
        assertEquals(listOf(true), reports)
    }

    /** Builds the activity again, then sets the content again, as the app does in `onCreate`. */
    private fun recreate() {
        composeRule.activityRule.scenario.recreate()
        composeRule.activityRule.scenario.onActivity { it.setContent { Page() } }
        composeRule.waitForIdle()
    }

    private fun put(ad: LoadedNativeAd) = composeRule.runOnUiThread {
        controller.putPreloadedNativeFullScreenForTest(AdPlacement.GALLERY_NATIVE_STEP_FULL, ad)
    }

    private fun hasText(text: String): Boolean =
        composeRule.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun hasTag(tag: String): Boolean =
        composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun waitForText(text: String) = composeRule.waitUntil(WAIT_MILLIS) { hasText(text) }

    private fun logged(message: String): Int = logger.entries.count { it.message == message }

    /** Waits [millis] of real time, so the retry of the page every 500 ms has run a few times. */
    private fun waitReal(millis: Long) {
        val until = System.currentTimeMillis() + millis
        composeRule.waitUntil(millis + WAIT_MILLIS) { System.currentTimeMillis() >= until }
    }

    @Test
    fun recreate_destroysOldAd_requestsReload() {
        showFirst()
        assertEquals(0, first.destroyCount)

        recreate()

        assertEquals(1, first.destroyCount)
        assertEquals(1, logged("Flow step full page left: gallery_native_step_full"))
        assertTrue(
            logger.entries.map { it.message }.toString(),
            logged("Reload gallery_native_step_full in 0 ms") >= 1,
        )
        waitReal(RETRY_WAIT_MILLIS)
        assertFalse("the destroyed ad is not drawn again", hasText(FIRST))
        assertEquals(1, first.destroyCount)
    }

    @Test
    fun recreate_thenNewAdArrives_showsNewAd() {
        showFirst()
        assertEquals("the first ad taken once", 1, logged("Flow step full page shown: gallery_native_step_full"))

        recreate()

        composeRule.waitUntil(WAIT_MILLIS) { hasTag(NativeFlowStepTestTags.FULL_PAGE_PLACEHOLDER) }
        assertFalse(hasText(FIRST))

        put(second)
        waitForText(SECOND)

        assertEquals(listOf(true, false, true), reports)
        assertEquals(0, second.destroyCount)
        assertEquals("one line per ad", 2, logged("Flow step full page shown: gallery_native_step_full"))
        // The first ad: taken once, destroyed once, never drawn after the rebuild (one impression, O-01).
        assertEquals(1, first.destroyCount)
        assertFalse(hasText(FIRST))
    }

    @Test
    fun recreateTwice_eachAdDestroyedOnce() {
        showFirst()
        recreate()
        put(second)
        waitForText(SECOND)

        recreate()
        put(third)
        waitForText(THIRD)

        assertEquals(1, first.destroyCount)
        assertEquals(1, second.destroyCount)
        assertEquals("the last ad stays on screen", 0, third.destroyCount)
        assertFalse(hasText(FIRST))
        assertFalse(hasText(SECOND))
    }

    @Test
    fun recreate_newAd_suppressesAgain() {
        showFirst()
        assertTrue(suppressor.suppressed.value)

        recreate()
        composeRule.waitUntil(WAIT_MILLIS) { hasTag(NativeFlowStepTestTags.FULL_PAGE_PLACEHOLDER) }
        assertFalse("the placeholder does not block resume ads", suppressor.suppressed.value)

        put(second)
        waitForText(SECOND)
        assertTrue(suppressor.suppressed.value)
    }

    @Test
    fun recreate_placementOff_noPlaceholder_noTake() {
        showFirst()
        settings.set(AdsSettings.placementEnabled(AdPlacement.GALLERY_NATIVE_STEP_FULL), false)
        composeRule.waitForIdle()

        recreate()
        put(second)
        waitReal(RETRY_WAIT_MILLIS)

        assertFalse("no placeholder while the placement is off", hasTag(NativeFlowStepTestTags.FULL_PAGE_PLACEHOLDER))
        assertFalse(hasText(SECOND))
        assertEquals(0, second.destroyCount)

        // Control (L-055): the placement on again, the same page takes the ad waiting in the slot.
        settings.set(AdsSettings.placementEnabled(AdPlacement.GALLERY_NATIVE_STEP_FULL), true)
        waitForText(SECOND)
    }

    @Test
    fun recreate_consentClosed_notTaken() {
        showFirst()
        // S-18, the staging reset of the stored consent: the gate goes back to its first state, closed (S-11).
        composeRule.runOnUiThread { controller.resetConsent() }
        composeRule.waitForIdle()
        assertFalse(controller.canRequestAds.value)

        recreate()
        put(second)
        waitReal(RETRY_WAIT_MILLIS)

        assertFalse(hasText(SECOND))
        assertEquals(0, second.destroyCount)
        assertFalse(hasTag(NativeFlowStepTestTags.FULL_PAGE_PLACEHOLDER))

        // Control (L-055): consent given again, the ad is taken.
        composeRule.runOnUiThread { controller.openConsentGateForTest() }
        waitForText(SECOND)
    }

    private companion object {
        const val FIRST = "First ad"
        const val SECOND = "Second ad"
        const val THIRD = "Third ad"
        const val WAIT_MILLIS = 3_000L
        const val RETRY_WAIT_MILLIS = 1_200L
    }
}
