package com.vglstudio.camera.pose.line.guide.core.ads

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/**
 * The real [FullScreenAdHost] registers with the real [AdsController] while it is in a composition (plan T-102, HD-05,
 * K-09): a shell that drops it leaves no host; an activity built again (rotation) keeps the open combo; an activity
 * destroyed and built again while the app is in background keeps it too, and closes it 2 s after the app comes back
 * with no host. The controller runs on the main looper of Robolectric, moved with `idleFor`.
 */
@RunWith(AndroidJUnit4::class)
class FullScreenAdHostAttachTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val logger = RecordingAppLogger()
    private val settings = FakeRemoteSettings().apply { set(AdsSettings.IS_SHOW_NATIVE_AFTER_INTER, true) }
    private val controller = testAdsController(
        // No ad unit: nothing is loaded; the ad is put in its slot by the tests.
        unitIds = AdUnitIds { null },
        flags = DefaultAdsFlags(settings),
        logger = logger,
    )
    private var finishes = 0

    private fun idleFor(millis: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))

    /** The shell of the app: [AdsHost] around the screens, then [FullScreenAdHost]. */
    @Composable
    private fun Shell() {
        AdsHost(controller) {
            AdsTestTheme {
                FullScreenAdHost()
            }
        }
    }

    /** Shows [Shell], then opens a combo with an ad in the slot; returns once the combo is on screen. */
    private fun openComboWithShell() {
        composeRule.setContent { Shell() }
        composeRule.waitForIdle()
        // Not `start`: the rule opened the activity before the body of the test, so a tracker would never see it.
        controller.setAppForegroundForTest(true)
        controller.openConsentGateForTest()
        val ad = FakeLoadedNativeAd()
        controller.putPreloadedNativeFullScreenForTest(AdPlacement.NATIVE_AFTER_INTER, ad)
        val hostId = System.identityHashCode(composeRule.activity)
        val turn = controller.beginInterstitialTurn(AdPlacement.GALLERY_INTERSTITIAL, hostId, combo = true) {
            finishes++
        }
        assertNotNull(turn)
        controller.onInterstitialClosed(AdPlacement.GALLERY_INTERSTITIAL, turn!!)
        // No resumed activity seen by a tracker: the combo waits the full 2 s for the interstitial to leave.
        idleFor(2_100)
        assertTrue(controller.nativeFullScreenView.value!!.ad === ad)
    }

    /** The app goes to background, and its activity is destroyed and built again there with no content yet. */
    private fun backgroundAndRebuildWithoutContent() {
        controller.setAppForegroundForTest(false)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        composeRule.activityRule.scenario.recreate()
        // The real host left the composition: the case exists (not a pass by default).
        assertFalse(controller.fullScreenHostAttached.value)
        idleFor(5_000)
        assertNotNull(controller.nativeFullScreenView.value)
        assertEquals(0, finishes)
    }

    @Test
    fun hostInComposition_attached_removed_detached() {
        var provide by mutableStateOf(false)
        var showHost by mutableStateOf(true)
        composeRule.setContent {
            CompositionLocalProvider(LocalAdsController provides if (provide) controller else null) {
                AdsTestTheme {
                    if (showHost) FullScreenAdHost()
                }
            }
        }
        composeRule.waitForIdle()
        // Control: with no LocalAdsController the host registers nowhere and does not fail.
        assertFalse(controller.fullScreenHostAttached.value)
        provide = true
        composeRule.waitForIdle()
        assertTrue(controller.fullScreenHostAttached.value)
        // A shell that drops the host.
        showHost = false
        composeRule.waitForIdle()
        assertFalse(controller.fullScreenHostAttached.value)
    }

    @Test
    fun recreate_whileComboOpen_turnKept_finishesOnceOnClose() {
        openComboWithShell()
        val before = logger.entries.size

        composeRule.activityRule.scenario.recreate()
        // The new activity sets its content again, like MainActivity does in onCreate.
        composeRule.activityRule.scenario.onActivity { it.setContent { Shell() } }
        idleFor(3_000)

        assertNotNull(controller.nativeFullScreenView.value)
        assertEquals(0, finishes)
        assertTrue(controller.fullScreenHostAttached.value)
        // The real host left the composition, then came back with the new activity.
        val lines = logger.entries.drop(before).map { it.message }
        val detached = lines.indexOf("Full-screen host detached (count: 0)")
        val attached = lines.indexOf("Full-screen host attached (count: 1)")
        assertTrue(lines.toString(), detached >= 0)
        assertTrue(lines.toString(), attached > detached)
        // The countdown of 5 s is over: the close button closes, once.
        idleFor(2_000)
        controller.closeNativeFullScreen()
        assertEquals(1, finishes)
        assertNull(controller.nativeFullScreenView.value)
    }

    @Test
    fun background_hostLeavesWhileStopped_returns_turnKept() {
        openComboWithShell()
        backgroundAndRebuildWithoutContent()

        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.activityRule.scenario.onActivity { it.setContent { Shell() } }
        controller.setAppForegroundForTest(true)
        idleFor(3_000)

        assertNotNull(controller.nativeFullScreenView.value)
        assertEquals(0, finishes)
        assertTrue(controller.fullScreenHostAttached.value)
    }

    @Test
    fun background_returnWithoutHost_closesAfterGrace() {
        openComboWithShell()
        backgroundAndRebuildWithoutContent()

        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        controller.setAppForegroundForTest(true)
        idleFor(1_999)
        assertNotNull(controller.nativeFullScreenView.value)
        assertEquals(0, finishes)
        idleFor(1)
        assertNull(controller.nativeFullScreenView.value)
        assertEquals(1, finishes)
        assertFalse(controller.fullScreenAdShowing.value)
    }
}
