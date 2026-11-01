package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Activity
import android.app.Application
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.analytics.AdFunnelEvent
import com.vglstudio.camera.pose.line.guide.core.analytics.AdFunnelKind
import com.vglstudio.camera.pose.line.guide.core.analytics.RecordingAdFunnel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode

/** The entry point of features for full-screen ads, in tests and previews that provide no ads controller. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FullScreenAdsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun bannerPlacement_isRejected() {
        val error = runCatching {
            composeRule.setContent { rememberShowFullScreenAd(AdPlacement.POSES_BOTTOM) }
            composeRule.waitForIdle()
        }.exceptionOrNull()
        assertTrue("expected IllegalArgumentException, got $error", error is IllegalArgumentException)
    }

    @Test
    fun withoutController_actionDoesNothing() {
        // No full-screen placement exists yet (T-014 adds one), so the action is built by the function that
        // rememberShowFullScreenAd wraps, with a recording show call in place of the controller's.
        val shown = mutableListOf<AdPlacement>()
        val show: suspend (AdsController, Activity, AdPlacement) -> Unit = { _, _, placement -> shown += placement }
        var withController by mutableStateOf(false)
        var action: () -> Unit = {}
        composeRule.setContent {
            CompositionLocalProvider(LocalAdsController provides if (withController) controller else null) {
                action = rememberFullScreenAdAction(AdPlacement.POSES_BOTTOM, show)
                if (withController) Box(modifier = Modifier.size(1.dp).testTag(CONTROL))
                FullScreenAdHost()
            }
        }
        composeRule.waitForIdle()
        action()
        composeRule.waitForIdle()
        assertTrue(shown.isEmpty())
        // FullScreenAdHost draws nothing without a controller.
        composeRule.onAllNodes(isRoot().not()).assertCountEquals(0)

        // Control: with a controller, the same action shows the ad, and the node query sees drawn nodes.
        withController = true
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag(CONTROL).assertCountEquals(1)
        action()
        composeRule.waitForIdle()
        assertEquals(listOf(AdPlacement.POSES_BOTTOM), shown)
    }

    @Test
    fun showStartupAd_withoutController_finishesOnceWithFalse() {
        val results = mutableListOf<Boolean>()
        var action: (StartupAdTurn, (Boolean) -> Unit) -> Unit = { _, _ -> }
        composeRule.setContent { action = rememberShowStartupAd() }
        composeRule.waitForIdle()
        action(StartupAdTurn(AdPlacement.OPEN_APP)) { results += it }
        composeRule.waitForIdle()
        assertEquals(listOf(false), results)
    }

    @Test
    fun showFullScreenAd_rejectsAppWidePlacement() {
        var placement by mutableStateOf(AdPlacement.GALLERY_INTERSTITIAL)
        composeRule.setContent { rememberShowFullScreenAd(placement) }
        // Control: a full-screen placement that is not app-wide is accepted.
        composeRule.waitForIdle()
        val error = runCatching {
            placement = AdPlacement.OPEN_APP_INTER
            composeRule.waitForIdle()
        }.exceptionOrNull()
        assertTrue("expected IllegalArgumentException, got $error", error is IllegalArgumentException)
    }

    @Test
    fun rememberShowFullScreenAd_rejectsNativeCombo() {
        var placement by mutableStateOf(AdPlacement.GALLERY_NATIVE_FULL_SCREEN)
        composeRule.setContent { rememberShowFullScreenAd(placement) }
        // Control: a native full-screen placement that is not app-wide is accepted.
        composeRule.waitForIdle()
        val error = runCatching {
            placement = AdPlacement.NATIVE_AFTER_INTER
            composeRule.waitForIdle()
        }.exceptionOrNull()
        assertTrue("expected IllegalArgumentException, got $error", error is IllegalArgumentException)
    }

    @Test
    fun showRewarded_recordsEligible_evenWithoutAd() {
        val recorder = RecordingAdFunnel()
        val controller = testAdsController(funnel = recorder)
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val outcomes = mutableListOf<RewardedOutcome>()

        // Two taps on "Watch ad": no consent, no ad loaded; the second one may come while the first is busy.
        controller.showRewarded(activity, AdPlacement.GALLERY_RW_ACTION) { outcomes += it }
        controller.showRewarded(activity, AdPlacement.GALLERY_RW_ACTION) { outcomes += it }
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(
            listOf(AdFunnelEvent.Eligible(AdFunnelKind.REWARDED), AdFunnelEvent.Eligible(AdFunnelKind.REWARDED)),
            recorder.events,
        )
    }

    @Test
    fun start_startsAppsFlyerAt10s_notBefore() {
        val attribution = FakeAppsFlyerStartup()
        val controller = testAdsController(attribution = attribution)
        val application = ApplicationProvider.getApplicationContext<Application>()

        // No activity: no consent form opens and the gate stays closed, so only the 10 s path starts AppsFlyer.
        controller.start(application, AppActivityFilter.inPackageOf(ComponentActivity::class.java))
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(9_999))
        assertEquals(0, attribution.starts)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1))
        assertEquals(1, attribution.starts)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(20))
        assertEquals(1, attribution.starts)
        // :core:ads never prepares AppsFlyer: :app does (ADR-0027, decision 1).
        assertEquals(0, attribution.prepares)
    }

    @Test
    fun showAppOpenAd_rejectsNonStagingPlacement() {
        var placement by mutableStateOf(AdPlacement.GALLERY_APP_OPEN)
        composeRule.setContent { rememberShowAppOpenAd(placement) }
        // Control: the app open placement of the gallery is accepted.
        composeRule.waitForIdle()
        val error = runCatching {
            placement = AdPlacement.OPEN_APP
            composeRule.waitForIdle()
        }.exceptionOrNull()
        assertTrue("expected IllegalArgumentException, got $error", error is IllegalArgumentException)
    }

    @Test
    fun showAppOpenAd_withoutController_finishesOnce() {
        val finished = mutableListOf<String>()
        var label by mutableStateOf("first")
        var action: () -> Unit = {}
        composeRule.setContent {
            val current = label
            action = rememberShowAppOpenAd(AdPlacement.GALLERY_APP_OPEN, onFinished = { finished += current })
        }
        composeRule.waitForIdle()
        action()
        composeRule.waitForIdle()
        assertEquals(listOf("first"), finished)

        // Rebuilt with another lambda: the latest one runs.
        label = "second"
        composeRule.waitForIdle()
        action()
        composeRule.waitForIdle()
        assertEquals(listOf("first", "second"), finished)
    }

    @Test
    fun showAppOpenAd_realController_goesThroughAppOpenGate() {
        // A real controller with no SDK: consent is not given, so the gate of app open skips the turn (A-05) and
        // onFinished runs once; the log names the gallery placement, so the action reached the controller.
        val logger = RecordingAppLogger()
        val real = testAdsController(logger = logger)
        var finished = 0
        var action: () -> Unit = {}
        composeRule.setContent {
            CompositionLocalProvider(LocalAdsController provides real) {
                action = rememberShowAppOpenAd(AdPlacement.GALLERY_APP_OPEN, onFinished = { finished++ })
            }
        }
        composeRule.waitForIdle()
        action()
        composeRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(1, finished)
        assertTrue(
            "${logger.entries}",
            logger.entries.any { it.tag == "Ads" && it.message.startsWith("Full-screen gallery_app_open skipped") },
        )
    }

    private val controller by lazy { testAdsController() }

    private companion object {
        const val CONTROL = "control"
    }
}
