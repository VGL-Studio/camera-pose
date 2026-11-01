package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Activity
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
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
 * The real [AdsController] with the combo key on (plan T-026, L-072): every way an interstitial turn that passed the
 * gate can end reaches the combo, with no gap in the showing flag or the resume block; turns blocked at the gate, the
 * startup interstitial and the standalone native full-screen turn never open the combo. No SDK ad is needed: the
 * turns are opened with [AdsController.beginInterstitialTurn] and [AdsController.beginStandaloneNativeTurn]. The
 * standalone native full-screen turn is also played through [AdsController.showFullScreenAd], the path of
 * [rememberShowFullScreenAd], with an ad put in its slot by [AdsController.putPreloadedNativeFullScreenForTest] (plan
 * T-044): no ad skips the turn as `NOT_READY`, a turn still open past the cap of F-02 skips the next one as `BUSY`.
 */
@RunWith(AndroidJUnit4::class)
class AdsControllerNativeComboTest {
    private val settings = FakeRemoteSettings().apply { set(AdsSettings.IS_SHOW_NATIVE_AFTER_INTER, true) }
    private val logger = RecordingAppLogger()
    private val activity: Activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
    private val hostId get() = System.identityHashCode(activity)
    private var finishes = 0
    private val onFinished: () -> Unit = { finishes++ }

    private val controller = testAdsController(
        // No ad unit: a skipped turn that loads sends nothing.
        unitIds = AdUnitIds { null },
        flags = DefaultAdsFlags(settings),
        logger = logger,
    )

    private fun idleFor(millis: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun beginComboTurn(): FullScreenCompletion {
        val turn = controller.beginInterstitialTurn(AdPlacement.GALLERY_INTERSTITIAL, hostId, combo = true, onFinished)
        assertNotNull(turn)
        assertTrue(controller.fullScreenAdShowing.value)
        return turn!!
    }

    /** Checks the end of a turn that passed the gate: no gap, then the combo, then [onFinished] only on close. */
    private fun assertReachesCombo() {
        // No gap: the combo holds the flag and the resume block before anything is dispatched.
        assertTrue(controller.fullScreenAdShowing.value)
        assertTrue(controller.resumeAdsSuppressed.value)
        assertEquals(0, finishes)
        // No resumed activity of the app (no tracker), so the combo waits the full 2 s for the interstitial to leave.
        idleFor(2_100)
        assertNotNull(controller.nativeFullScreenView.value)
        assertEquals(0, finishes)
        assertTrue(controller.fullScreenAdShowing.value)
        // The consent gate is closed on the JVM: no ad is requested and the close button works at once.
        assertTrue(controller.nativeFullScreenView.value!!.closeEnabled)
        controller.closeNativeFullScreen()
        assertEquals(1, finishes)
        assertNull(controller.nativeFullScreenView.value)
        assertFalse(controller.fullScreenAdShowing.value)
        idleFor(5_000)
        assertEquals(1, finishes)
    }

    @Test
    fun closed_opensCombo() {
        val turn = beginComboTurn()
        controller.onInterstitialClosed(AdPlacement.GALLERY_INTERSTITIAL, turn)
        assertReachesCombo()
    }

    @Test
    fun showFailed_opensCombo() {
        val turn = beginComboTurn()
        controller.onInterstitialShowFailed(AdPlacement.GALLERY_INTERSTITIAL, turn, code = 1)
        assertReachesCombo()
    }

    @Test
    fun adLost_opensCombo() {
        val turn = beginComboTurn()
        controller.onInterstitialLost(AdPlacement.GALLERY_INTERSTITIAL, turn, loadState = "idle")
        assertReachesCombo()
    }

    @Test
    fun stuckRecovered_opensCombo() {
        beginComboTurn()
        controller.onAppActivityPaused(activity)
        controller.onAppActivityResumed(activity)
        assertReachesCombo()
    }

    @Test
    fun hostDestroyed_opensCombo() {
        beginComboTurn()
        controller.onAppActivityDestroyed(activity)
        assertReachesCombo()
    }

    @Test
    fun capExpired_reachesCombo_busyFinishesAtOnce() {
        beginComboTurn()
        idleFor(FullScreenAdPresence.SHOWING_CAP_MILLIS + 1)
        var secondFinishes = 0
        val second = controller.beginInterstitialTurn(AdPlacement.GALLERY_INTERSTITIAL, hostId, combo = true) {
            secondFinishes++
        }
        assertNotNull(second)
        // The first turn went through the combo, which found the second turn on screen (F-01).
        assertEquals(1, finishes)
        assertTrue(logger.entries.any { it.message == "Native combo skipped: busy" })
        idleFor(5_000)
        assertNull(controller.nativeFullScreenView.value)
        assertEquals(0, secondFinishes)
        assertTrue(controller.fullScreenAdShowing.value)
    }

    @Test
    fun onFinished_runsOnMainThread() {
        var onMain: Boolean? = null
        val turn = controller.beginInterstitialTurn(AdPlacement.GALLERY_INTERSTITIAL, hostId, combo = true) {
            onMain = Looper.myLooper() == Looper.getMainLooper()
        }!!
        controller.onInterstitialClosed(AdPlacement.GALLERY_INTERSTITIAL, turn)
        idleFor(2_100)
        controller.closeNativeFullScreen()
        assertEquals(true, onMain)
    }

    @Test
    fun startupTurn_neverOpensCombo() {
        val turn = controller.beginInterstitialTurn(AdPlacement.OPEN_APP_INTER, hostId, combo = false, onFinished)
        assertNotNull(turn)
        controller.onAppActivityDestroyed(activity)
        assertEquals(1, finishes)
        assertFalse(controller.fullScreenAdShowing.value)
        idleFor(5_000)
        assertNull(controller.nativeFullScreenView.value)
        assertEquals(1, finishes)
    }

    @Test
    fun showStartupAd_blocked_neverOpensCombo() {
        val results = mutableListOf<Boolean>()
        controller.showStartupAd(activity, StartupAdTurn(AdPlacement.OPEN_APP_INTER)) { results += it }
        // At once: a combo that fails to load would also finish, but only when its countdown ends.
        idle()
        assertEquals(listOf(false), results)
        idleFor(5_000)
        assertEquals(listOf(false), results)
        assertNull(controller.nativeFullScreenView.value)
    }

    @Test
    fun blockedAtGate_consentClosed_finishesOnce_noView() {
        settings.set(AdsSettings.placementEnabled(AdPlacement.GALLERY_INTERSTITIAL), true)
        CoroutineScope(Dispatchers.Main.immediate).launch {
            controller.showInterstitial(activity, AdPlacement.GALLERY_INTERSTITIAL, onFinished)
        }
        idle()
        assertEquals(1, finishes)
        idleFor(5_000)
        assertNull(controller.nativeFullScreenView.value)
        assertFalse(controller.fullScreenAdShowing.value)
        assertEquals(1, finishes)
    }

    @Test
    fun blockedAtGate_noAd_finishesOnce_noView() {
        settings.set(AdsSettings.placementEnabled(AdPlacement.GALLERY_INTERSTITIAL), true)
        controller.openConsentGateForTest()
        idle()
        assertTrue(controller.canRequestAds.value)
        CoroutineScope(Dispatchers.Main.immediate).launch {
            controller.showInterstitial(activity, AdPlacement.GALLERY_INTERSTITIAL, onFinished)
        }
        idle()
        assertEquals(1, finishes)
        assertTrue(logger.entries.any { it.message.contains("gallery_interstitial skipped: NOT_READY") })
        idleFor(5_000)
        assertNull(controller.nativeFullScreenView.value)
        assertFalse(controller.fullScreenAdShowing.value)
        assertEquals(1, finishes)
    }

    @Test
    fun standalone_noCurrentTurn_presenceUntilClose() {
        val shown = controller.beginStandaloneNativeTurn(
            AdPlacement.GALLERY_NATIVE_FULL_SCREEN,
            { FakeLoadedNativeAd() },
            onFinished,
        )
        assertTrue(shown)
        assertEquals(5, controller.nativeFullScreenView.value!!.secondsLeft)
        controller.onAppActivityDestroyed(activity)
        controller.onAppActivityPaused(activity)
        controller.onAppActivityResumed(activity)
        idleFor(5_000)
        assertTrue(controller.fullScreenAdShowing.value)
        assertNotNull(controller.nativeFullScreenView.value)
        assertEquals(0, finishes)
        controller.closeNativeFullScreen()
        assertFalse(controller.fullScreenAdShowing.value)
        assertEquals(1, finishes)
    }

    @Test
    fun standalone_timeCloseNative_fromRemote() {
        val placement = AdPlacement.GALLERY_NATIVE_FULL_SCREEN
        // Remote values reach the controller once it is started, as the app does in onCreate.
        controller.start(activity.application, AppActivityFilter { true })
        settings.set(AdsSettings.TIME_CLOSE_NATIVE, 12L)
        idle()
        assertTrue(controller.beginStandaloneNativeTurn(placement, { FakeLoadedNativeAd() }, onFinished))
        assertEquals(12, controller.nativeFullScreenView.value!!.secondsLeft)
        idleFor(12_000)
        controller.closeNativeFullScreen()
        assertEquals(1, finishes)

        // Control: a value outside 1..30 opens the next turn with 5.
        settings.set(AdsSettings.TIME_CLOSE_NATIVE, 0L)
        idle()
        assertTrue(controller.beginStandaloneNativeTurn(placement, { FakeLoadedNativeAd() }, onFinished))
        assertEquals(5, controller.nativeFullScreenView.value!!.secondsLeft)
        idleFor(5_000)
        controller.closeNativeFullScreen()
        assertEquals(2, finishes)
    }

    @Test
    fun standalone_whileTurnOpenPastCap_isRejected_takesNoAd() {
        val first = FakeLoadedNativeAd()
        assertTrue(controller.beginStandaloneNativeTurn(AdPlacement.GALLERY_NATIVE_FULL_SCREEN, { first }, onFinished))
        // F-02: past the cap of 10 minutes the showing flag no longer reads busy, but the first turn is still open.
        idleFor(FullScreenAdPresence.SHOWING_CAP_MILLIS + 1)
        var taken = 0
        var secondFinishes = 0
        val second = controller.beginStandaloneNativeTurn(
            AdPlacement.GALLERY_NATIVE_FULL_SCREEN,
            {
                taken++
                FakeLoadedNativeAd()
            },
        ) { secondFinishes++ }
        // Rejected before the gate takes an ad, so no `shown` opportunity; the caller skips the turn (BUSY).
        assertFalse(second)
        assertEquals(0, taken)
        assertEquals(0, secondFinishes)
        assertTrue(controller.fullScreenAdShowing.value)
        assertTrue(controller.nativeFullScreenView.value!!.ad === first)
        controller.closeNativeFullScreen()
        assertEquals(1, finishes)
        assertNull(controller.nativeFullScreenView.value)
        assertFalse(controller.fullScreenAdShowing.value)
    }

    @Test
    fun standalone_noAd_showsNothing_finishesAtOnce() {
        val shown = controller.beginStandaloneNativeTurn(AdPlacement.GALLERY_NATIVE_FULL_SCREEN, { null }, onFinished)
        assertFalse(shown)
        assertFalse(controller.fullScreenAdShowing.value)
        assertNull(controller.nativeFullScreenView.value)
        // The caller skips the turn (NOT_READY), which runs onFinished once.
        assertEquals(0, finishes)
    }

    /** Turns the standalone native full-screen placement on and opens the consent gate, as on a device. */
    private fun enableStandaloneThroughShow() {
        settings.set(AdsSettings.placementEnabled(AdPlacement.GALLERY_NATIVE_FULL_SCREEN), true)
        controller.openConsentGateForTest()
        idle()
        assertTrue(controller.canRequestAds.value)
    }

    /** Plays [AdPlacement.GALLERY_NATIVE_FULL_SCREEN] through [AdsController.showFullScreenAd]; returns its result. */
    private fun showStandalone(onDone: () -> Unit): Boolean? {
        var result: Boolean? = null
        CoroutineScope(Dispatchers.Main.immediate).launch {
            result = controller.showFullScreenAd(activity, AdPlacement.GALLERY_NATIVE_FULL_SCREEN, onDone)
        }
        idle()
        return result
    }

    @Test
    fun standalone_noAd_throughShow_skipsNotReady_finishesOnce() {
        enableStandaloneThroughShow()
        val result = showStandalone(onFinished)
        assertEquals(false, result)
        assertEquals(1, finishes)
        assertTrue(
            logger.entries.any { it.message == "Full-screen gallery_native_full_screen skipped: NOT_READY" },
        )
        assertFalse(controller.fullScreenAdShowing.value)
        assertNull(controller.nativeFullScreenView.value)
        idleFor(5_000)
        assertEquals(1, finishes)
    }

    @Test
    fun standalone_withAd_throughShow_opensTurn() {
        enableStandaloneThroughShow()
        val ad = FakeLoadedNativeAd()
        controller.putPreloadedNativeFullScreenForTest(AdPlacement.GALLERY_NATIVE_FULL_SCREEN, ad)
        val result = showStandalone(onFinished)
        assertEquals(true, result)
        assertEquals(0, finishes)
        assertTrue(controller.nativeFullScreenView.value!!.ad === ad)
        assertFalse(logger.entries.any { it.message.contains("skipped") })
        // The consent gate is open, so the close button waits for its countdown of 5 s by default (K-03,
        // `time_close_native`).
        idleFor(4_999)
        assertFalse(controller.nativeFullScreenView.value!!.closeEnabled)
        idleFor(1)
        assertTrue(controller.nativeFullScreenView.value!!.closeEnabled)
        controller.closeNativeFullScreen()
        assertEquals(1, finishes)
    }

    @Test
    fun standalone_turnOpenPastCap_throughShow_skipsBusy_keepsAdInSlot() {
        enableStandaloneThroughShow()
        val first = FakeLoadedNativeAd()
        controller.putPreloadedNativeFullScreenForTest(AdPlacement.GALLERY_NATIVE_FULL_SCREEN, first)
        assertEquals(true, showStandalone(onFinished))
        // F-02: past the cap of 10 minutes the showing flag no longer reads busy and the gate says SHOW, but the
        // first turn is still open.
        idleFor(FullScreenAdPresence.SHOWING_CAP_MILLIS + 1)
        val second = FakeLoadedNativeAd()
        controller.putPreloadedNativeFullScreenForTest(AdPlacement.GALLERY_NATIVE_FULL_SCREEN, second)
        var secondFinishes = 0
        val result = showStandalone { secondFinishes++ }
        assertEquals(false, result)
        assertEquals(1, secondFinishes)
        assertTrue(logger.entries.any { it.message == "Full-screen gallery_native_full_screen skipped: BUSY" })
        assertFalse(logger.entries.any { it.message.contains("skipped: NOT_READY") })
        // The second ad is not taken out of its slot.
        assertTrue(controller.isFullScreenAdReady(AdPlacement.GALLERY_NATIVE_FULL_SCREEN))
        assertEquals(0, second.destroyCount)
        assertTrue(controller.nativeFullScreenView.value!!.ad === first)
        assertEquals(0, finishes)
        controller.closeNativeFullScreen()
        assertEquals(1, finishes)
    }
}
