package com.vglstudio.camera.pose.line.guide.core.ads

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import java.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/**
 * The window of the in-screen ads in the real [AdsController] (KB-27, ADR-0036 decision 1, plan T-054): it follows
 * foreground and the showing flag of the full-screen gate, the send gate given to sources and pools waits for it, and
 * each line that wires it into a slot (native small with its refresh, the slot with no source, the pool) is checked
 * with a fake native loader ([AdsController.nativeLoaderForTest]). No SDK ad: the turns are opened with
 * [AdsController.beginStandaloneNativeTurn] and [AdsController.beginInterstitialTurn], full-screen formats have no
 * ad unit so nothing of theirs is sent.
 */
@RunWith(AndroidJUnit4::class)
class AdsControllerInScreenWindowTest {
    private val settings = FakeRemoteSettings()
    private val logger = RecordingAppLogger()
    private val loader = FakeNativeAdLoader()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val controller = testAdsController(
        unitIds = InScreenOnlyUnitIds,
        flags = DefaultAdsFlags(settings),
        logger = logger,
    ).also { it.nativeLoaderForTest = loader }

    @After
    fun tearDown() {
        scope.cancel()
        idle()
    }

    private fun idleFor(millis: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun count(message: String): Int = logger.entries.count { it.message == message }

    /** Consent open and the app in foreground: the window is open. */
    private fun openAndForeground() {
        controller.openConsentGateForTest()
        controller.setAppForegroundForTest(true)
        idle()
        assertTrue(controller.inScreenAdWindow.value)
    }

    /** A standalone native full-screen ad over the app. */
    private fun cover() {
        assertTrue(
            controller.beginStandaloneNativeTurn(AdPlacement.GALLERY_NATIVE_FULL_SCREEN, { FakeLoadedNativeAd() }) {},
        )
        assertTrue(controller.fullScreenAdShowing.value)
    }

    /** Closes the standalone turn of [cover], once its countdown is over (K-03). */
    private fun uncover() {
        if (controller.nativeFullScreenView.value?.closeEnabled != true) {
            idleFor(NATIVE_CLOSE_COUNTDOWN_DEFAULT_SECONDS * 1_000)
        }
        controller.closeNativeFullScreen()
        assertFalse(controller.fullScreenAdShowing.value)
    }

    private fun collect(states: Flow<NativeAdState>): MutableList<NativeAdState> {
        val received = mutableListOf<NativeAdState>()
        scope.launch { states.collect { received += it } }
        idle()
        return received
    }

    private fun List<NativeAdState>.lastAd(): LoadedNativeAd? = (lastOrNull() as? NativeAdState.Loaded)?.ad

    @Test
    fun standaloneNativeTurn_closesWindow_closeOpensIt() {
        controller.setAppForegroundForTest(true)
        assertTrue(controller.inScreenAdWindow.value)

        cover()
        assertFalse(controller.inScreenAdWindow.value)

        uncover()
        assertTrue(controller.inScreenAdWindow.value)
        assertEquals(
            listOf(PAUSED, RESUMED),
            logger.entries.map {
                it.message
            }.filter { it == PAUSED || it == RESUMED },
        )
    }

    @Test
    fun comboTurn_noOpenWindowAcrossInterClose() {
        settings.set(AdsSettings.IS_SHOW_NATIVE_AFTER_INTER, true)
        openAndForeground()
        val windowValues = mutableListOf<Boolean>()
        val flagValues = mutableListOf<Boolean>()
        scope.launch { controller.inScreenAdWindow.collect { windowValues += it } }
        scope.launch { controller.fullScreenAdShowing.collect { flagValues += it } }
        var finishes = 0
        val inter = controller.beginInterstitialTurn(AdPlacement.GALLERY_INTERSTITIAL, 1, combo = true) { finishes++ }
        assertNotNull(inter)
        val send = scope.async { controller.inScreenSendGate() }
        idle()

        controller.onInterstitialClosed(AdPlacement.GALLERY_INTERSTITIAL, inter!!)
        idleFor(2_100)
        assertNotNull("the combo is not on screen", controller.nativeFullScreenView.value)
        assertEquals(listOf(true, false), windowValues)
        assertEquals(listOf(false, true), flagValues)
        assertFalse("a send went under the cover", send.isCompleted)
        assertEquals(1, count(PAUSED))
        assertEquals(0, count(RESUMED))

        // No ad unit for the combo: the load fails at once, so the close button works now (K-04).
        controller.closeNativeFullScreen()
        idle()
        assertEquals(1, finishes)
        assertEquals(listOf(true, false, true), windowValues)
        assertTrue(send.isCompleted)
        assertTrue(send.getCompleted())
        assertEquals(1, count(RESUMED))

        // Control (L-055): an interstitial turn with no combo opens the window again when it closes.
        val plain = controller.beginInterstitialTurn(AdPlacement.GALLERY_INTERSTITIAL, 1, combo = false) { finishes++ }
        assertNotNull(plain)
        assertFalse(controller.inScreenAdWindow.value)
        controller.onInterstitialClosed(AdPlacement.GALLERY_INTERSTITIAL, plain!!)
        assertTrue(controller.inScreenAdWindow.value)
        assertEquals(2, finishes)
    }

    @Test
    fun realController_smallSlot_refreshTickWhileCovered_isSkipped() {
        settings.set(AdsSettings.placementEnabled(AdPlacement.SAMPLE_NATIVE_SMALL), true)
        openAndForeground()
        val states = collect(controller.nativeAd(AdPlacement.SAMPLE_NATIVE_SMALL))
        assertEquals(1, loader.requests.size)
        val first = FakeLoadedNativeAd()
        loader.deliver(first)
        idle()
        assertSame(first, states.lastAd())
        // The next load after the claim brings the ad of the next refresh.
        assertEquals(2, loader.requests.size)
        val second = FakeLoadedNativeAd()
        loader.deliver(second)
        idle()

        idleFor(50_000)
        cover()
        idleFor(20_000)
        assertEquals(2, loader.requests.size)
        assertSame(first, states.lastAd())
        assertEquals(0, first.destroyCount)

        uncover()
        idleFor(49_000)
        assertEquals(2, loader.requests.size)
        assertSame(first, states.lastAd())

        // Control (L-055): the tick of 120 s takes the new ad.
        idleFor(1_000)
        assertSame(second, states.lastAd())
        assertEquals(1, first.destroyCount)
    }

    @Test
    fun realController_smallSlot_nextLoadAfterClaimWhileCovered_waits() {
        settings.set(AdsSettings.placementEnabled(AdPlacement.SAMPLE_NATIVE_SMALL), true)
        openAndForeground()
        val states = collect(controller.nativeAd(AdPlacement.SAMPLE_NATIVE_SMALL))
        assertEquals(1, loader.requests.size)
        cover()
        val first = FakeLoadedNativeAd()
        loader.deliver(first)
        idle()
        assertSame(first, states.lastAd())
        // The next load after the claim goes through the send gate of the source: it waits under the cover.
        idleFor(30_000)
        assertEquals(1, loader.requests.size)

        // Control (L-055): the cover goes, the next load is sent once.
        uncover()
        idle()
        assertEquals(2, loader.requests.size)
    }

    @Test
    fun realController_retryingSlot_firstLoadWaitsWhileCovered() {
        settings.set(AdsSettings.placementEnabled(AdPlacement.SAMPLE_NATIVE_SMALL), true)
        openAndForeground()
        cover()
        // Another format than the placement's own, with no source and no pool: the slot with retries.
        collect(controller.nativeAd(AdPlacement.SAMPLE_NATIVE_SMALL, AdFormat.NATIVE_MEDIUM))
        idleFor(60_000)
        assertEquals(0, loader.requests.size)

        uncover()
        idle()
        assertEquals(1, loader.requests.size)
    }

    @Test
    fun realController_poolRefillWhileCovered_waits() {
        settings.set(AdsSettings.placementEnabled(AdPlacement.GALLERY_NATIVE_MEDIUM), true)
        openAndForeground()
        cover()
        collect(controller.pooledCell(AdPlacement.GALLERY_NATIVE_MEDIUM))
        idleFor(60_000)
        assertEquals(0, loader.requests.size)

        uncover()
        idle()
        assertEquals(1, loader.requests.size)
    }

    @Test
    fun poolCanLoadNow_followsWindow() {
        controller.markSdkReadyForTest()
        openAndForeground()
        assertTrue(controller.poolCanLoadNow())

        cover()
        assertFalse(controller.poolCanLoadNow())

        uncover()
        assertTrue(controller.poolCanLoadNow())
    }

    @Test
    fun flowStepFullPage_keepsWindowOpen() {
        openAndForeground()
        controller.putPreloadedNativeFullScreenForTest(AdPlacement.GALLERY_NATIVE_STEP_FULL, FakeLoadedNativeAd())
        val states = collect(controller.fullScreenStep(AdPlacement.GALLERY_NATIVE_STEP_FULL))
        assertTrue(states.last() is NativeAdState.Loaded)
        assertTrue(controller.inScreenAdWindow.value)
        assertFalse(controller.fullScreenAdShowing.value)

        // Control (L-055): a native full-screen ad over the same page closes the window.
        cover()
        assertFalse(controller.inScreenAdWindow.value)
    }

    @Test
    fun background_closesWindow_withoutCover() {
        controller.setAppForegroundForTest(true)
        assertTrue(controller.inScreenAdWindow.value)

        controller.setAppForegroundForTest(false)
        assertFalse(controller.inScreenAdWindow.value)
        assertEquals(0, count(PAUSED))

        // Control (L-055): a cover logs the pause.
        controller.setAppForegroundForTest(true)
        cover()
        assertEquals(1, count(PAUSED))
    }

    @Test
    fun inScreenSendGate_waitsWhileCovered_thenSends() {
        openAndForeground()
        // Control (L-055): not covered, the gate answers at once.
        val free = scope.async { controller.inScreenSendGate() }
        idle()
        assertTrue(free.isCompleted)
        assertTrue(free.getCompleted())

        cover()
        val covered = scope.async { controller.inScreenSendGate() }
        idleFor(60_000)
        assertFalse(covered.isCompleted)

        uncover()
        idle()
        assertTrue(covered.isCompleted)
        assertTrue(covered.getCompleted())
    }

    /** Test units for the in-screen native formats only: no full-screen ad or banner of this test sends anything. */
    private object InScreenOnlyUnitIds : AdUnitIds {
        override fun of(placement: AdPlacement): String? = of(placement, placement.format)

        override fun of(placement: AdPlacement, format: AdFormat): String? = when (format) {
            AdFormat.NATIVE_SMALL, AdFormat.NATIVE_MEDIUM, AdFormat.NATIVE_COLLAPSIBLE ->
                GoogleTestAdUnitIds.unitFor(format)
            else -> null
        }
    }

    private companion object {
        const val PAUSED = "In-screen ads paused (full-screen ad on screen)"
        const val RESUMED = "In-screen ads resumed (full-screen ad closed)"
    }
}
