package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Activity
import android.app.Application
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.ads.AdError
import com.vglstudio.camera.pose.line.guide.core.analytics.RecordingAdMetrics
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import java.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

/**
 * The real [AdsController] with the combo key on and the startup interstitial (KB-13, criterion 4, A-02, K-01; plan
 * T-089): the interstitial of `open_app_inter` passes the gate and shows, and whether it closes or fails to show the
 * user goes on at once, with no native full-screen ad. A fake interstitial is put in its slot; the controller is
 * started, so the activity tracker follows the activity built after it.
 */
@RunWith(AndroidJUnit4::class)
class AdsControllerStartupInterComboTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val settings = FakeRemoteSettings().apply {
        set(AdsSettings.IS_LOG_AD_FULLSCREEN_EVENTS, true)
        // The keys of this open choose `open_app_inter` (StartupAdChoiceTest).
        set(AdsSettings.SHOW_OPEN_ADS, true)
        set(AdsSettings.SHOW_OPEN_ADS_FIRST_OPEN, true)
        set(AdsSettings.OPEN_AD_TYPE, "inter")
        set(AdsSettings.IS_SHOW_NATIVE_AFTER_INTER, true)
        set(AdsSettings.placementEnabled(AdPlacement.GALLERY_INTERSTITIAL), true)
    }
    private val logger = RecordingAppLogger()
    private val results = mutableListOf<Boolean>()

    private val controller = testAdsController(
        context = application,
        // No ad unit: nothing is ever loaded; the ads are put in their slot by the tests.
        unitIds = AdUnitIds { null },
        flags = DefaultAdsFlags(settings),
        metrics = RecordingAdMetrics(),
        logger = logger,
    )

    // A FullScreenAdHost is in a composition, as with the shell of the app (HD-05, plan T-102).
    private val detachHost = controller.attachFullScreenHost()

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun idleFor(millis: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))

    /** The startup interstitial passed the gate and shown on a resumed activity of the app; returns it and the host. */
    private fun startupInterShown(): Pair<FakeInterstitialAd, Activity> {
        controller.start(application, AppActivityFilter { true })
        idle()
        controller.openConsentGateForTest()
        idle()
        val fake = FakeInterstitialAd()
        controller.putLoadedInterstitialForTest(AdPlacement.OPEN_APP_INTER, fake)
        val activity: Activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        controller.showStartupAd(activity, StartupAdTurn(AdPlacement.OPEN_APP_INTER)) { results += it }
        idle()
        assertEquals(1, fake.shows)
        assertTrue(results.isEmpty())
        return fake to activity
    }

    private fun assertNoCombo() {
        assertNull(controller.nativeFullScreenView.value)
        assertTrue(logger.entries.none { it.message.startsWith("Native combo") })
    }

    /**
     * Control: with the same controller and the same key on, the interstitial of an in-app placement still opens the
     * combo (KB-16, criterion 1), ended by [end] (closed or failed to show).
     */
    private fun assertInAppInterOpensCombo(activity: Activity, end: (FakeInterstitialAd) -> Unit) {
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
    fun startupInter_closed_comboOn_noNativeFullScreen() {
        val (fake, activity) = startupInterShown()
        fake.callback!!.onAdDismissedFullScreenContent()
        // KB-13, criterion 4, A-02: the user goes on at once (a combo would hold the flag and the completion).
        assertEquals(listOf(true), results)
        assertFalse(controller.fullScreenAdShowing.value)
        idleFor(2_100)
        assertNoCombo()
        idleFor(5_000)
        assertEquals(listOf(true), results)
        assertInAppInterOpensCombo(activity) { it.callback!!.onAdDismissedFullScreenContent() }
    }

    @Test
    fun startupInter_showFailed_comboOn_noNativeFullScreen() {
        val (fake, _) = startupInterShown()
        fake.callback!!.onAdFailedToShowFullScreenContent(SHOW_FAILED)
        // K-01: the interstitial was asked to show, and no native ad follows it.
        assertEquals(listOf(true), results)
        assertFalse(controller.fullScreenAdShowing.value)
        idleFor(2_100)
        assertNoCombo()
    }

    private companion object {
        val SHOW_FAILED = AdError(1, "Show failed.", "com.google.android.gms.ads")
    }
}
