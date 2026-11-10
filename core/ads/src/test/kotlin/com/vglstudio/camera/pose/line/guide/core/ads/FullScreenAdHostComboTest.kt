package com.vglstudio.camera.pose.line.guide.core.ads

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.ComponentDialog
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

/**
 * The combo drawn the way the app draws it (plan T-090; HD-05): [AdsHost] with the real [AdsController] and the
 * public [FullScreenAdHost], the two composables MainActivity places. A combo turn opened after a closed
 * interstitial puts the overlay in the tree over the screen (K-02, K-03), the close button and the back button close
 * it (K-08) and the screen below is still there; an activity recreated or sent to the background while the countdown
 * runs keeps the turn and its deadline (L-061, L-091 of the template).
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class FullScreenAdHostComboTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val settings = FakeRemoteSettings().apply { set(AdsSettings.IS_SHOW_NATIVE_AFTER_INTER, true) }
    private val logger = RecordingAppLogger()
    private val controller = testAdsController(
        // No ad unit: the combo takes the ad put in its slot by the test.
        unitIds = AdUnitIds { null },
        flags = DefaultAdsFlags(settings),
        logger = logger,
    )
    private var finishes = 0

    /** What MainActivity sets: the ads root with a screen inside, and the overlay host after it. */
    @Composable
    private fun Shell() {
        AdsHost(controller) {
            Box(Modifier.fillMaxSize().testTag(SCREEN))
            FullScreenAdHost()
        }
    }

    private fun idleFor(millis: Long) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))
        composeRule.waitForIdle()
    }

    /** An interstitial turn of the review screen that passed the gate closes; the combo opens about 2 s later. */
    private fun openCombo() {
        controller.openConsentGateForTest()
        controller.putPreloadedNativeFullScreenForTest(
            AdPlacement.NATIVE_AFTER_INTER,
            FakeLoadedNativeAd(FakeNativeAdAssets.Design),
        )
        val hostId = System.identityHashCode(composeRule.activity)
        val turn = controller.beginInterstitialTurn(AdPlacement.REVIEW_INTERSTITIAL, hostId, combo = true) {
            finishes++
        }
        assertNotNull(turn)
        controller.onInterstitialClosed(AdPlacement.REVIEW_INTERSTITIAL, turn!!)
        // No resumed activity of the app (no tracker), so the combo waits the full 2 s for the interstitial to leave.
        idleFor(2_100)
        composeRule.onAllNodesWithTag(COUNTDOWN).assertCountEquals(1)
        composeRule.onAllNodesWithTag(SCREEN).assertCountEquals(1)
        assertEquals(0, finishes)
        assertTrue(controller.fullScreenAdShowing.value)
    }

    /** Back is sent to the dialog's own dispatcher: Robolectric does not route the activity's Back into it. */
    private fun pressBackOnDialog() {
        val dialog = ShadowDialog.getLatestDialog() as ComponentDialog
        composeRule.runOnUiThread { dialog.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }

    private fun comboClosedLogs(): List<String> =
        logger.entries.map { it.message }.filter { it.startsWith("Native combo closed") }

    @Test
    fun combo_overlayInTree_closeReturnsToScreen() {
        composeRule.setContent { Shell() }
        openCombo()

        idleFor(5_100)
        composeRule.onAllNodesWithTag(COUNTDOWN).assertCountEquals(0)
        composeRule.onNodeWithTag(CLOSE).performClick()
        composeRule.waitForIdle()

        assertEquals(1, finishes)
        composeRule.onAllNodesWithTag(CLOSE).assertCountEquals(0)
        composeRule.onAllNodesWithTag(SCREEN).assertCountEquals(1)
        assertFalse(controller.fullScreenAdShowing.value)
        assertEquals(listOf("Native combo closed (shown: true)"), comboClosedLogs())
    }

    @Test
    fun back_duringCountdown_ignored_afterCountdown_closes() {
        composeRule.setContent { Shell() }
        openCombo()

        pressBackOnDialog()
        composeRule.onAllNodesWithTag(COUNTDOWN).assertCountEquals(1)
        assertEquals(0, finishes)
        assertTrue(controller.fullScreenAdShowing.value)

        idleFor(5_100)
        composeRule.onAllNodesWithTag(CLOSE).assertCountEquals(1)
        pressBackOnDialog()
        assertEquals(1, finishes)
        composeRule.onAllNodesWithTag(CLOSE).assertCountEquals(0)
        composeRule.onAllNodesWithTag(SCREEN).assertCountEquals(1)
        assertEquals(listOf("Native combo closed (shown: true)"), comboClosedLogs())
    }

    @Test
    fun recreate_duringCountdown_overlayKept_deadlineKept() {
        composeRule.setContent { Shell() }
        openCombo()
        idleFor(1_000)

        composeRule.activityRule.scenario.recreate()
        // The new activity sets its content again, like MainActivity does in onCreate.
        composeRule.activityRule.scenario.onActivity { it.setContent { Shell() } }
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag(COUNTDOWN).assertCountEquals(1)
        composeRule.onAllNodesWithTag(SCREEN).assertCountEquals(1)
        assertEquals(0, finishes)

        // The overlay opened at about 2.0 s; this is about 5.6 s after it, past the deadline of 5 s. A deadline
        // restarted at the recreation (about 3.1 s) would still be counting until about 6.0 s.
        idleFor(4_500)
        composeRule.onAllNodesWithTag(CLOSE).assertCountEquals(1)
        composeRule.onAllNodesWithTag(COUNTDOWN).assertCountEquals(0)
        composeRule.onNodeWithTag(CLOSE).performClick()
        composeRule.waitForIdle()
        assertEquals(1, finishes)
        assertFalse(controller.fullScreenAdShowing.value)
    }

    @Test
    fun background_duringCountdown_resumedPastDeadline_closes() {
        composeRule.setContent { Shell() }
        openCombo()

        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        // The deadline is on the monotonic clock, so it passes while the app is in the background (L-091).
        idleFor(6_000)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag(CLOSE).assertCountEquals(1)
        composeRule.onAllNodesWithTag(COUNTDOWN).assertCountEquals(0)
        // With an ad the screen never closes by itself.
        assertEquals(0, finishes)
        composeRule.onNodeWithTag(CLOSE).performClick()
        composeRule.waitForIdle()
        assertEquals(1, finishes)
        composeRule.onAllNodesWithTag(CLOSE).assertCountEquals(0)
        assertFalse(controller.fullScreenAdShowing.value)
    }

    private companion object {
        const val SCREEN = "screenBelowTheCombo"
        const val CLOSE = NativeFullScreenAdTestTags.CLOSE
        const val COUNTDOWN = NativeFullScreenAdTestTags.COUNTDOWN
    }
}
