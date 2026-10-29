package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Activity
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.analytics.AdFunnelEvent
import com.vglstudio.camera.pose.line.guide.core.analytics.AdFunnelKind
import com.vglstudio.camera.pose.line.guide.core.analytics.RecordingAdFunnel
import java.time.Duration
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
 * The eligible events of the real [AdsController] (section 8.3; plan T-033): one per interstitial turn that opens,
 * one per native full-screen turn that opens, standalone or after an interstitial. No SDK ad is needed: the turns are
 * opened with the API of plan T-026.
 */
@RunWith(AndroidJUnit4::class)
class AdsControllerFunnelTest {
    private val recorder = RecordingAdFunnel()
    private val activity: Activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
    private val hostId get() = System.identityHashCode(activity)

    private fun controller(combo: Boolean = false) = testAdsController(
        unitIds = AdUnitIds { null },
        flags = DefaultAdsFlags(FakeRemoteSettings().apply { set(AdsSettings.IS_SHOW_NATIVE_AFTER_INTER, combo) }),
        funnel = recorder,
    )

    private fun idleFor(millis: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))

    @Test
    fun interTurn_recordsEligibleOnce() {
        val controller = controller()
        val turn = controller.beginInterstitialTurn(AdPlacement.GALLERY_INTERSTITIAL, hostId, combo = false) {}
        assertNotNull(turn)
        assertEquals(listOf<AdFunnelEvent>(AdFunnelEvent.Eligible(AdFunnelKind.INTERS)), recorder.events)

        // A second turn while the first is on screen is busy: no eligible.
        val busy = controller.beginInterstitialTurn(AdPlacement.GALLERY_INTERSTITIAL, hostId, combo = false) {}
        assertNull(busy)
        assertEquals(1, recorder.events.size)
    }

    @Test
    fun standalone_recordsEligible_onlyWithAd() {
        val controller = controller()
        val none = controller.beginStandaloneNativeTurn(AdPlacement.GALLERY_NATIVE_FULL_SCREEN, takeAd = { null }) {}
        assertFalse(none)
        assertTrue(recorder.events.isEmpty())

        val shown = controller.beginStandaloneNativeTurn(
            AdPlacement.GALLERY_NATIVE_FULL_SCREEN,
            takeAd = { FakeLoadedNativeAd() },
        ) {}
        assertTrue(shown)
        assertEquals(listOf<AdFunnelEvent>(AdFunnelEvent.Eligible(AdFunnelKind.NATIVE)), recorder.events)
    }

    @Test
    fun combo_recordsNativeEligibleAfterInter() {
        val controller = controller(combo = true)
        val turn = controller.beginInterstitialTurn(AdPlacement.GALLERY_INTERSTITIAL, hostId, combo = true) {}!!
        controller.onInterstitialClosed(AdPlacement.GALLERY_INTERSTITIAL, turn)
        // No resumed activity of the app (no tracker): the combo waits the full 2 s for the interstitial to leave.
        idleFor(2_100)
        assertNotNull(controller.nativeFullScreenView.value)
        assertEquals(
            listOf(AdFunnelEvent.Eligible(AdFunnelKind.INTERS), AdFunnelEvent.Eligible(AdFunnelKind.NATIVE)),
            recorder.events,
        )
    }

    @Test
    fun comboOff_recordsInterEligibleOnly() {
        val controller = controller(combo = false)
        val turn = controller.beginInterstitialTurn(AdPlacement.GALLERY_INTERSTITIAL, hostId, combo = true) {}!!
        controller.onInterstitialClosed(AdPlacement.GALLERY_INTERSTITIAL, turn)
        idleFor(2_100)
        assertNull(controller.nativeFullScreenView.value)
        assertEquals(listOf<AdFunnelEvent>(AdFunnelEvent.Eligible(AdFunnelKind.INTERS)), recorder.events)
    }
}
