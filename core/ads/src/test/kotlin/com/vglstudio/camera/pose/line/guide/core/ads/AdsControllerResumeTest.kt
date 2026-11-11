package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Activity
import android.app.Application
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.ads.AdError
import com.vglstudio.camera.pose.line.guide.core.analytics.AdMetricEvent
import com.vglstudio.camera.pose.line.guide.core.analytics.RecordingAdMetrics
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import java.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

/**
 * The real [AdsController] tied to the resume ad (plan T-025, L-072): an ad click raises the resume block, the gate
 * reads the controller's own state, and a switched-off resume records nothing. No SDK ad is needed.
 */
@RunWith(AndroidJUnit4::class)
class AdsControllerResumeTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val settings = FakeRemoteSettings()
    private val metrics = RecordingAdMetrics()
    private val logger = RecordingAppLogger()

    // A FullScreenAdHost is in a composition, as with the shell of the app (HD-05, plan T-102).
    private var detachHost: () -> Unit = {}

    private fun controller() = testAdsController(
        context = application,
        unitIds = AdUnitIds { null },
        flags = DefaultAdsFlags(settings),
        metrics = metrics,
        logger = logger,
    ).also { detachHost = it.attachFullScreenHost() }

    private fun adFreeStatus(flow: StateFlow<Boolean>): AdFreeStatus = object : AdFreeStatus {
        override val adFree = flow
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun idleFor(millis: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))

    private fun startedController(): AdsController = controller().also {
        it.start(application, AppActivityFilter { true })
        idle()
    }

    @Test
    fun adClick_raisesResumeSuppression() {
        val controller = startedController()
        assertFalse(controller.resumeAdsSuppressed.value)
        // Any format, even with the lifecycle metrics off and nothing served.
        controller.telemetry.clicked(AdFormat.BANNER, null)
        assertTrue(controller.resumeAdsSuppressed.value)
        // Control: the next resume of an app activity releases it.
        controller.resumeSuppression.onActivityResumed()
        assertFalse(controller.resumeAdsSuppressed.value)
    }

    @Test
    fun resumeOff_gateHasNoPlacement_foregroundRecordsNothing() {
        // The lifecycle metrics on, else no opportunity is ever recorded and the check proves nothing.
        settings.set(AdsSettings.IS_LOG_AD_FULLSCREEN_EVENTS, true)
        val controller = startedController()
        controller.openConsentGateForTest()
        // The 45 s cap of the startup turn over, so a return reaches the OFF gate, not STARTUP.
        idleFor(45_100)
        val gate = controller.readResumeGate()
        assertTrue(gate.toString(), gate.serving)
        assertTrue(gate.toString(), gate.startupFinished)
        assertNull(gate.placement)
        controller.onProcessForegroundChanged(true)
        controller.onProcessForegroundChanged(false)
        controller.onProcessForegroundChanged(true)
        idle()
        assertTrue(metrics.events.none { it is AdMetricEvent.Opportunity })
        val resumeLogs = logger.entries.map { it.message }.filter { it.startsWith("Resume ad ") }
        assertEquals(listOf("Resume ad FIRST_FOREGROUND: none", "Resume ad OFF: none"), resumeLogs)
        // Control (L-055): switched on, the next return is an opportunity of the resume placement.
        settings.set(AdsSettings.SHOW_RESUM_ADS, true)
        idle()
        assertEquals(AdPlacement.RESUME, controller.readResumeGate().placement)
        controller.onProcessForegroundChanged(false)
        controller.onProcessForegroundChanged(true)
        idle()
        val opportunities = metrics.events.filterIsInstance<AdMetricEvent.Opportunity>()
        assertEquals(1, opportunities.size)
        assertEquals("resume", opportunities.single().adPlacement)
        assertEquals("not_ready", opportunities.single().result)
        assertEquals("idle", opportunities.single().loadState)
        assertEquals("APP_OPEN", opportunities.single().adFormat)
        settings.set(AdsSettings.RESUME_AD_TYPE, "inter")
        idle()
        assertEquals(AdPlacement.RESUME_INTER, controller.readResumeGate().placement)
    }

    @Test
    fun readResumeGate_mapsControllerState() {
        val controller = controller()
        // Before the keys are read.
        val before = controller.readResumeGate()
        assertNull(before.placement)
        assertFalse(before.serving)
        assertTrue(before.appOpenLockPassed)
        assertFalse(before.ready)
        assertFalse(before.busy)
        assertFalse(before.startupFinished)
        assertFalse(before.suppressed)
        controller.resumeSuppression.markAdClicked()
        assertTrue(controller.readResumeGate().suppressed)

        // The startup key is off: awaitStartupAd returns null and the startup turn is over.
        controller.start(application, AppActivityFilter { true })
        idle()
        assertFalse(controller.readResumeGate().startupFinished)
        var returned = false
        var turn: StartupAdTurn? = null
        CoroutineScope(Dispatchers.Main).launch {
            turn = controller.awaitStartupAd()
            returned = true
        }
        idle()
        assertTrue(returned)
        assertNull(turn)
        assertTrue(controller.readResumeGate().startupFinished)

        // Control: with no loading screen, the turn is over 45 s after start, not before.
        val other = startedController()
        idleFor(44_000)
        assertFalse(other.readResumeGate().startupFinished)
        idleFor(1_100)
        assertTrue(other.readResumeGate().startupFinished)
    }

    @Test
    fun adFreeMidSession_resumeOff_noOpportunity() {
        // KB-30, criteria 1 and 3 (L-061): the signal turns on mid session, the next return shows and records nothing.
        settings.set(AdsSettings.IS_LOG_AD_FULLSCREEN_EVENTS, true)
        settings.set(AdsSettings.SHOW_RESUM_ADS, true)
        val adFree = MutableStateFlow(false)
        val controller = testAdsController(
            context = application,
            unitIds = AdUnitIds { null },
            flags = DefaultAdsFlags(settings, adFreeStatus(adFree)),
            metrics = metrics,
            logger = logger,
        )
        controller.start(application, AppActivityFilter { true })
        idle()
        controller.openConsentGateForTest()
        idleFor(45_100)
        assertEquals(AdPlacement.RESUME, controller.readResumeGate().placement)
        adFree.value = true
        idle()
        assertNull(controller.readResumeGate().placement)
        controller.onProcessForegroundChanged(true)
        controller.onProcessForegroundChanged(false)
        controller.onProcessForegroundChanged(true)
        idle()
        assertTrue(metrics.events.none { it is AdMetricEvent.Opportunity })
        val resumeLogs = logger.entries.map { it.message }.filter { it.startsWith("Resume ad ") }
        assertEquals(listOf("Resume ad FIRST_FOREGROUND: none", "Resume ad OFF: none"), resumeLogs)
        // Control (L-055): the signal back off, the next return is an opportunity of the resume placement.
        adFree.value = false
        idle()
        assertEquals(AdPlacement.RESUME, controller.readResumeGate().placement)
        controller.onProcessForegroundChanged(false)
        controller.onProcessForegroundChanged(true)
        idle()
        val opportunities = metrics.events.filterIsInstance<AdMetricEvent.Opportunity>()
        assertEquals(1, opportunities.size)
        assertEquals("resume", opportunities.single().adPlacement)
        assertEquals("not_ready", opportunities.single().result)
    }

    /**
     * The resume interstitial shown on a return with the combo key on (plan T-089): the controller started, the
     * consent gate open, the startup turn over, a fake interstitial in the `resume_inter` slot, and an activity built
     * after start (the tracker sees it) sent to background and back.
     */
    private fun resumeInterShown(): ResumeInterTurn {
        settings.set(AdsSettings.IS_LOG_AD_FULLSCREEN_EVENTS, true)
        settings.set(AdsSettings.SHOW_RESUM_ADS, true)
        settings.set(AdsSettings.IS_SHOW_NATIVE_AFTER_INTER, true)
        settings.set(AdsSettings.placementEnabled(AdPlacement.GALLERY_INTERSTITIAL), true)
        settings.set(AdsSettings.RESUME_AD_TYPE, "inter")
        val controller = startedController()
        controller.openConsentGateForTest()
        idle()
        // The 45 s cap of the startup turn over, so a return reaches the resume ad.
        idleFor(45_100)
        val fake = FakeInterstitialAd()
        controller.putLoadedInterstitialForTest(AdPlacement.RESUME_INTER, fake)
        val host: ActivityController<ComponentActivity> = Robolectric.buildActivity(
            ComponentActivity::class.java,
        ).setup()
        host.pause().stop()
        host.start().resume()
        idle()
        assertEquals(1, fake.shows)
        assertSame(host.get(), fake.shownOn.single())
        assertTrue(logger.entries.any { it.message == "Interstitial shown: resume_inter" })
        return ResumeInterTurn(controller, fake, host)
    }

    private data class ResumeInterTurn(
        val controller: AdsController,
        val fake: FakeInterstitialAd,
        val host: ActivityController<ComponentActivity>,
    )

    private fun assertNoCombo(controller: AdsController) {
        assertNull(controller.nativeFullScreenView.value)
        assertTrue(logger.entries.none { it.message.startsWith("Native combo") })
    }

    /**
     * Control: with the same controller and the same key on, the interstitial of an in-app placement still opens the
     * combo (KB-16, criterion 1), ended by [end] (closed or failed to show).
     */
    private fun assertInAppInterOpensCombo(
        controller: AdsController,
        activity: Activity,
        end: (FakeInterstitialAd) -> Unit,
    ) {
        // The mark of `ads_interval` moved by the interstitial before.
        idleFor(45_100)
        val gallery = FakeInterstitialAd()
        controller.putLoadedInterstitialForTest(AdPlacement.GALLERY_INTERSTITIAL, gallery)
        CoroutineScope(Dispatchers.Main).launch {
            controller.showInterstitial(activity, AdPlacement.GALLERY_INTERSTITIAL) {}
        }
        idle()
        assertEquals(1, gallery.shows)
        end(gallery)
        assertTrue(controller.fullScreenAdShowing.value)
        idleFor(2_100)
        assertNotNull(controller.nativeFullScreenView.value)
        assertTrue(logger.entries.any { it.message.startsWith("Native combo opened") })
    }

    @Test
    fun resumeInter_closed_comboOn_noNativeFullScreen() {
        val (controller, fake, host) = resumeInterShown()
        fake.callback!!.onAdDismissedFullScreenContent()
        // KB-14, criterion 1, KB-16, criterion 3: the user goes on at once (a combo would hold the flag).
        assertFalse(controller.fullScreenAdShowing.value)
        idleFor(2_100)
        assertNoCombo(controller)
        idleFor(5_000)
        assertNull(controller.nativeFullScreenView.value)
        assertInAppInterOpensCombo(controller, host.get()) { it.callback!!.onAdDismissedFullScreenContent() }
    }

    @Test
    fun resumeInter_showFailed_comboOn_noNativeFullScreen() {
        val (controller, fake, host) = resumeInterShown()
        fake.callback!!.onAdFailedToShowFullScreenContent(SHOW_FAILED)
        // K-01, KB-16, criterion 8 (in-app placements only): no native after a resume interstitial that failed.
        assertFalse(controller.fullScreenAdShowing.value)
        idleFor(2_100)
        assertNoCombo(controller)
        // Control (K-01): an in-app interstitial that fails to show still opens the combo.
        assertInAppInterOpensCombo(controller, host.get()) {
            it.callback!!.onAdFailedToShowFullScreenContent(SHOW_FAILED)
        }
    }

    private companion object {
        val SHOW_FAILED = AdError(1, "Show failed.", "com.google.android.gms.ads")
    }
}
