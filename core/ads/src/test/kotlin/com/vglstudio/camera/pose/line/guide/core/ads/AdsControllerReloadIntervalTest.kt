package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Application
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.config.RemoteKey
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/**
 * The reload keys of KB-34 in the real [AdsController] (plan T-087): each layer reads its own key
 * ([AdsController.reloadIntervalMillis]), and a value changed while a slot is on screen applies from its next count.
 * A fake native loader ([AdsController.nativeLoaderForTest]) answers every request at once; refreshes are counted by
 * their log line, not by the requests sent (the stock of plan T-086 loads its next ad after each claim).
 */
@RunWith(AndroidJUnit4::class)
class AdsControllerReloadIntervalTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val logger = RecordingAppLogger()
    private val loader = FakeNativeAdLoader()
    private val settings = FakeRemoteSettings().apply {
        set(AdsSettings.placementEnabled(AdPlacement.POSES_BOTTOM), true)
        set(AdsSettings.placementEnabled(AdPlacement.GALLERY_NATIVE_SMALL), true)
    }

    /** Requests of [loader] already answered. */
    private val answered = mutableSetOf<Int>()

    private val controller by lazy {
        testAdsController(context = application, flags = DefaultAdsFlags(settings), logger = logger)
            .also { it.nativeLoaderForTest = loader }
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    /** The setup of section 4 of the plan: started, consent open, AdMob ready, in foreground. */
    private fun start() {
        controller.start(application, AppActivityFilter { true })
        idle()
        controller.openConsentGateForTest()
        controller.admobReadyForTest()
        controller.setAppForegroundForTest(true)
        idle()
    }

    /** Answers every open request of [loader] with a new ad. */
    private fun deliverAll() {
        loader.requests.indices.filter { it !in answered }.forEach { index ->
            answered += index
            loader.deliverAt(index, FakeLoadedNativeAd())
        }
        idle()
    }

    /**
     * Moves both clocks by [millis] in steps of 250 ms, the one of the composition (the refresh of a slot) and the
     * main looper (the controller and its stocks), answering every request at once.
     */
    private fun advanceBy(millis: Long) {
        var left = millis
        while (left > 0) {
            val step = minOf(left, STEP_MILLIS)
            composeRule.mainClock.advanceTimeBy(step, ignoreFrameDuration = true)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(step))
            deliverAll()
            left -= step
        }
        composeRule.waitForIdle()
    }

    private fun show(content: @Composable () -> Unit) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { CompositionLocalProvider(LocalAdsController provides controller) { content() } }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        deliverAll()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
    }

    private fun refreshes(slotKey: String): Int = logger.entries.count {
        it.message == "Native $slotKey refresh: loaded"
    }

    @Test
    fun reloadIntervalMillis_followsKeyOfEachLayer() {
        settings.set(TIME_RELOAD_NATIVE_SMALL, 40L)
        settings.set(TIME_RELOAD_COLLAP_AD, 100L)
        settings.set(TIME_RELOAD_BANNER_COLLAP, 50L)
        start()

        val small = controller.reloadIntervalMillis(AdFormat.NATIVE_SMALL)
        assertEquals(40_000L, small.value)
        assertEquals(100_000L, controller.reloadIntervalMillis(AdFormat.NATIVE_COLLAPSIBLE).value)
        assertEquals(50_000L, controller.reloadIntervalMillis(AdFormat.BANNER_COLLAPSIBLE).value)

        settings.set(TIME_RELOAD_NATIVE_SMALL, 0L)
        idle()
        assertEquals(0L, small.value)
        // Control (L-055): the other layers keep their own keys.
        assertEquals(100_000L, controller.reloadIntervalMillis(AdFormat.NATIVE_COLLAPSIBLE).value)
    }

    @Test
    fun bannerLayers_followTheirKeys() {
        // Plan T-088, KB-34 criterion 1: the fixed banner (also the banner layer of a bottom bar) and the MREC read
        // their own key.
        settings.set(TIME_RELOAD_BANNER, 45L)
        settings.set(TIME_RELOAD_BANNER_MREC, 90L)
        settings.set(TIME_RELOAD_BANNER_COLLAP, 50L)
        start()

        assertEquals(45_000L, controller.reloadIntervalMillis(AdFormat.BANNER).value)
        assertEquals(90_000L, controller.reloadIntervalMillis(AdFormat.BANNER_MREC).value)
        // Control (L-055): the collapsible banner keeps its own key.
        assertEquals(50_000L, controller.reloadIntervalMillis(AdFormat.BANNER_COLLAPSIBLE).value)

        settings.set(TIME_RELOAD_BANNER, 0L)
        idle()
        assertEquals(0L, controller.reloadIntervalMillis(AdFormat.BANNER).value)
        assertEquals(90_000L, controller.reloadIntervalMillis(AdFormat.BANNER_MREC).value)
    }

    @Test
    fun nativeSmallSlot_keyChangedOnScreen_nextTickUsesNew() {
        start()
        show { NativeAdSlot(AdPlacement.GALLERY_NATIVE_SMALL) }
        val key = AdPlacement.GALLERY_NATIVE_SMALL.key
        assertEquals(0, refreshes(key))

        advanceBy(30_000)
        settings.set(TIME_RELOAD_NATIVE_SMALL, 120L)
        idle()
        // The count under way keeps its 60 s.
        advanceBy(29_000)
        assertEquals(0, refreshes(key))
        advanceBy(2_000)
        assertEquals(1, refreshes(key))

        // The next count reads 120 s: nothing at 120 s, the swap at 180 s.
        advanceBy(117_000)
        assertEquals(1, refreshes(key))
        advanceBy(2_000)
        assertEquals(2, refreshes(key))
    }

    @Test
    fun collapsibleLayer_refreshesAtCollapKey_staysCollapsed() {
        settings.set(AdsSettings.IS_SHOW_NATIVE_BOTTOM, true)
        settings.set(AdsSettings.TIME_EXPAND_COLLAP_AD, 0L)
        settings.set(TIME_RELOAD_COLLAP_AD, 45L)
        settings.set(TIME_RELOAD_NATIVE_SMALL, 120L)
        start()
        show { BottomAdBarScaffold(AdPlacement.POSES_BOTTOM) {} }
        val layerKey = nativeSlotKey(AdPlacement.POSES_BOTTOM, AdFormat.NATIVE_COLLAPSIBLE)
        val slotKey = AdPlacement.POSES_BOTTOM.key
        assertTrue(hasNode(CollapsibleNativeAdTestTags.AD))

        advanceBy(10_000)
        composeRule.onNodeWithTag(CollapsibleNativeAdTestTags.COLLAPSE, useUnmergedTree = true).performClick()
        advanceBy(1_000)
        assertTrue("the layer did not collapse", !hasNode(CollapsibleNativeAdTestTags.AD))

        advanceBy(33_000)
        assertEquals(0, refreshes(layerKey))
        advanceBy(2_000)
        assertEquals(1, refreshes(layerKey))
        assertTrue("the refresh expanded the layer", !hasNode(CollapsibleNativeAdTestTags.AD))

        // The bottom slot reads its own key: no refresh before 120 s (and 500 ms after the layer's count).
        advanceBy(73_000)
        assertEquals(0, refreshes(slotKey))
        // Control (L-055): the bottom slot does refresh once its own count is over.
        advanceBy(2_000)
        assertEquals(1, refreshes(slotKey))
    }

    private fun hasNode(tag: String): Boolean =
        composeRule.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private companion object {
        const val STEP_MILLIS = 250L

        // Written by name, as the team writes them on Firebase; FakeRemoteSettings matches values by name.
        val TIME_RELOAD_NATIVE_SMALL = RemoteKey.long("time_reload_native_small", 0L, "Test key.")
        val TIME_RELOAD_COLLAP_AD = RemoteKey.long("time_reload_collap_ad", 0L, "Test key.")
        val TIME_RELOAD_BANNER_COLLAP = RemoteKey.long("time_reload_banner_collap", 0L, "Test key.")
        val TIME_RELOAD_BANNER = RemoteKey.long("time_reload_banner", 0L, "Test key.")
        val TIME_RELOAD_BANNER_MREC = RemoteKey.long("time_reload_banner_mrec", 0L, "Test key.")
    }
}
