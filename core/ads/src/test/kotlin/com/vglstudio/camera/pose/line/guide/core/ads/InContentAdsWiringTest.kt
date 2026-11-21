package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Application
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.ads.AdView
import com.vglstudio.camera.pose.line.guide.core.analytics.AdFunnelEvent
import com.vglstudio.camera.pose.line.guide.core.analytics.RecordingAdFunnel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * In-content ads win over the floating ads of their screen (MC-13, KB-35 criteria 4 to 6, plan T-092) through the
 * production wiring of [InContentAds], [BottomAdBarScaffold] and [AdSlot] under a real [AdsController]: with an
 * in-content placement of the screen switched on, the bar has its slot only and the collapsible banner builds no ad
 * view, takes no space and does not hold the anchored turn of T-091; switched on while the screen is open, the
 * floating ad goes at once; switched off, it comes back at the next entry. A fake native loader and one test unit per
 * format tell the format of a request; banners are counted as [AdView]s in the window. The clock of Compose is
 * stopped and moves with the main looper ([frames], [advanceBy]).
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class InContentAdsWiringTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val bar = AdPlacement.POSES_BOTTOM
    private val medium = AdPlacement.GALLERY_NATIVE_MEDIUM
    private val mrec = AdPlacement.GALLERY_BANNER_MREC
    private val fixed = AdPlacement.GALLERY_BANNER
    private val collapsible = AdPlacement.GALLERY_BANNER_COLLAPSIBLE
    private val logger = RecordingAppLogger()
    private val funnel = RecordingAdFunnel()
    private val loader = FakeNativeAdLoader()
    private val settings = FakeRemoteSettings().apply {
        set(AdsSettings.IS_SHOW_NATIVE_BOTTOM, true)
        set(AdsSettings.placementEnabled(bar), true)
        // The layer never opens again by itself unless a test says so.
        set(AdsSettings.TIME_EXPAND_COLLAP_AD, 0L)
        set(AdsSettings.TIME_RELOAD_NATIVE_SMALL, 60L)
        set(AdsSettings.placementEnabled(medium), false)
        set(AdsSettings.placementEnabled(mrec), false)
        set(AdsSettings.placementEnabled(fixed), false)
        set(AdsSettings.placementEnabled(collapsible), false)
    }

    /** Requests of [loader] already answered by the test. */
    private val answered = mutableSetOf<Int>()

    private val controller by lazy {
        testAdsController(
            context = application,
            unitIds = UnitPerFormat,
            flags = DefaultAdsFlags(settings),
            funnel = funnel,
            logger = logger,
        ).also { it.nativeLoaderForTest = loader }
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun switch(placement: AdPlacement, on: Boolean) = settings.set(AdsSettings.placementEnabled(placement), on)

    /** Started, consent open, AdMob ready, in foreground; no preload asked, so every stock is empty. */
    private fun start() {
        controller.start(application, AppActivityFilter { true })
        idle()
        controller.openConsentGateForTest()
        controller.admobReadyForTest()
        controller.setAppForegroundForTest(true)
        idle()
    }

    /** [content] on screen under the controller, with the clock of Compose stopped. */
    private fun show(content: @Composable () -> Unit) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { CompositionLocalProvider(LocalAdsController provides controller) { content() } }
        frames()
    }

    /** One frame, the main looper idle after it. */
    private fun frame() {
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        idle()
    }

    /** A few frames: a change, its effects, and what the screen draws after them. */
    private fun frames() {
        repeat(3) { frame() }
        composeRule.waitForIdle()
    }

    /** Moves both clocks by [millis] in steps of 250 ms; requests are answered only by the test. */
    private fun advanceBy(millis: Long) {
        var left = millis
        while (left > 0) {
            val step = minOf(left, STEP_MILLIS)
            composeRule.mainClock.advanceTimeBy(step, ignoreFrameDuration = true)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(step))
            left -= step
        }
        frames()
    }

    private fun requestsOf(format: AdFormat): Int = loader.requests.count { it.unitId == UnitPerFormat.unitFor(format) }

    /** Answers the oldest open request of [format] with an ad of [headline]. */
    private fun deliver(format: AdFormat, headline: String): FakeLoadedNativeAd {
        val index = loader.requests.indices.firstOrNull {
            it !in answered && loader.requests[it].unitId == UnitPerFormat.unitFor(format)
        }
        checkNotNull(index) { "no open request of $format" }
        answered += index
        val ad = FakeLoadedNativeAd(sampleNativeAdAssets.copy(headline = headline))
        loader.deliverAt(index, ad)
        idle()
        frames()
        return ad
    }

    /** Answers every open request, each with an ad of its own. */
    private fun deliverAll() {
        loader.requests.indices.filter { it !in answered }.forEach { index ->
            answered += index
            loader.deliverAt(index, FakeLoadedNativeAd(sampleNativeAdAssets.copy(headline = "Ad $index")))
            idle()
        }
        frames()
    }

    private fun slotShows(headline: String): Boolean = composeRule.onAllNodes(
        hasText(headline) and hasAnyAncestor(hasTestTag(BottomAdBarTestTags.SLOT)),
        useUnmergedTree = true,
    ).fetchSemanticsNodes().isNotEmpty()

    private fun layerShows(headline: String): Boolean = composeRule.onAllNodes(
        hasText(headline) and hasAnyAncestor(hasTestTag(CollapsibleNativeAdTestTags.AD)),
        useUnmergedTree = true,
    ).fetchSemanticsNodes().isNotEmpty()

    private fun layerShown(): Boolean =
        composeRule.onAllNodesWithTag(CollapsibleNativeAdTestTags.AD, useUnmergedTree = true)
            .fetchSemanticsNodes().isNotEmpty()

    private fun eligibleCount(): Int = funnel.events.count { it is AdFunnelEvent.Eligible }

    private fun logged(line: String): Int = logger.entries.count { it.message == line }

    private fun adViews(view: View = composeRule.activity.window.decorView): List<AdView> = when (view) {
        is AdView -> listOf(view)
        is ViewGroup -> (0 until view.childCount).flatMap { adViews(view.getChildAt(it)) }
        else -> emptyList()
    }

    private fun height(tag: String): Dp = composeRule.onNodeWithTag(tag).getBoundsInRoot().height

    private fun fixedHeight(): Dp = bannerAdSize(composeRule.activity, AdFormat.BANNER, 411).height.dp

    private fun collapsibleHeight(): Dp = bannerAdSize(composeRule.activity, AdFormat.BANNER_COLLAPSIBLE, 411).height.dp

    private fun assertHeight(message: String, expected: Dp, actual: Dp) {
        assertEquals(message, expected.value, actual.value, TOLERANCE_DP)
    }

    private fun waitLogs(): Int = logger.entries.count {
        it.message == anchoredAdWaitLog(collapsible, fixed) ||
            it.message == anchoredAdWaitLog(fixed, collapsible) ||
            it.message == anchoredAdWaitLog(collapsible, bar) ||
            it.message == anchoredAdWaitLog(bar, collapsible)
    }

    @Composable
    private fun Bar(content: @Composable () -> Unit = {}) = BottomAdBarScaffold(bar) { content() }

    @Composable
    private fun FixedBanner() = AdSlot(fixed, Modifier.fillMaxWidth().testTag(FIXED))

    @Composable
    private fun CollapsibleBanner() = AdSlot(collapsible, Modifier.fillMaxWidth().testTag(COLLAPSIBLE))

    /**
     * The frames, counted from the first composition (0), at which the bar sends its first request of the layer and
     * of the slot, with [medium] off and declared or not; null when not within [FIRST_FRAMES] frames.
     */
    private fun firstRequestFrames(declared: Boolean): Pair<Int?, Int?> {
        start()
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CompositionLocalProvider(LocalAdsController provides controller) {
                if (declared) {
                    InContentAds(listOf(medium)) { Bar { NativeAdSlot(medium) } }
                } else {
                    Bar { NativeAdSlot(medium) }
                }
            }
        }
        var layerFrame: Int? = null
        var slotFrame: Int? = null
        for (frame in 0..FIRST_FRAMES) {
            if (frame > 0) frame()
            if (layerFrame == null && requestsOf(AdFormat.NATIVE_COLLAPSIBLE) > 0) layerFrame = frame
            if (slotFrame == null && requestsOf(AdFormat.NATIVE_SMALL) > 0) slotFrame = frame
        }
        return layerFrame to slotFrame
    }

    @Test
    fun declaredOn_barHasNoLayer() {
        switch(medium, true)
        start()
        show { InContentAds(listOf(medium)) { Bar { NativeAdSlot(medium) } } }

        assertEquals("no request of the layer (one before plan T-092)", 0, requestsOf(AdFormat.NATIVE_COLLAPSIBLE))
        assertEquals("the slot sends its first load", 1, requestsOf(AdFormat.NATIVE_SMALL))
        assertEquals(1, logged("Bottom bar ${bar.key}: slot=NATIVE collapsible=false"))
        assertEquals(1, logged(inContentBlockLog(listOf(medium))))

        deliver(AdFormat.NATIVE_SMALL, SLOT_AD)
        assertTrue("the slot shows its ad", slotShows(SLOT_AD))
        assertFalse("no layer", layerShown())
    }

    /** Control of [declaredOff_layerAsUsual_sameFrames]: passes before plan T-092, it measures the bar as it is. */
    @Test
    fun noDeclaration_layerAsUsual() {
        assertEquals(CONTROL_FRAMES, firstRequestFrames(declared = false))
        deliver(AdFormat.NATIVE_COLLAPSIBLE, LAYER_AD)
        assertTrue("the layer shows", layerShown())
    }

    @Test
    fun declaredOff_layerAsUsual_sameFrames() {
        assertEquals("the same frames as without a declaration", CONTROL_FRAMES, firstRequestFrames(declared = true))
        deliver(AdFormat.NATIVE_COLLAPSIBLE, LAYER_AD)
        assertTrue("the layer shows", layerShown())
        assertEquals("no block logged", 0, logged(inContentBlockLog(listOf(medium))))
    }

    @Test
    fun declaredOn_stockOfLayerKept() {
        switch(medium, true)
        start()
        var screen by mutableStateOf(Screen.NONE)
        show {
            when (screen) {
                Screen.NONE -> Unit
                Screen.DECLARED -> InContentAds(listOf(medium)) { Bar() }
                Screen.PLAIN -> Bar()
            }
        }
        controller.requestPreloadForTest()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(PRELOAD_MILLIS))
        frames()
        val stockAd = deliver(AdFormat.NATIVE_COLLAPSIBLE, LAYER_AD)
        val layerRequests = requestsOf(AdFormat.NATIVE_COLLAPSIBLE)

        screen = Screen.DECLARED
        frames()
        frames()
        assertFalse("no layer", layerShown())
        assertEquals("the ad of the stock is kept", 0, stockAd.destroyCount)
        assertEquals("no new request of the layer", layerRequests, requestsOf(AdFormat.NATIVE_COLLAPSIBLE))

        // Control: leave, then enter a screen without a declaration: the layer shows that very ad.
        screen = Screen.NONE
        frames()
        screen = Screen.PLAIN
        // The keys have their first value one frame after the bar entered; the layer takes the ad of the stock at frame
        // 3 and draws it at frame 4, the same cadence as before the turn of the anchored ad, so wait two batches.
        frames()
        frames()
        assertTrue("the layer shows the ad of the stock", layerShows(LAYER_AD))
    }

    @Test
    fun turnedOnWhileOpen_layerDropped_noEvents() {
        settings.set(AdsSettings.TIME_EXPAND_COLLAP_AD, 30L)
        start()
        show { InContentAds(listOf(medium)) { Bar() } }
        val layerAd = deliver(AdFormat.NATIVE_COLLAPSIBLE, LAYER_AD)
        assertTrue("the layer shows", layerShown())
        assertEquals("the slot gives way", 1, logged("Bottom bar ${bar.key}: slot yields to the collapsible layer"))
        val eligibleBefore = eligibleCount()
        val layerRequests = requestsOf(AdFormat.NATIVE_COLLAPSIBLE)

        switch(medium, true)
        frames()

        assertFalse("the layer is dropped", layerShown())
        assertEquals("the ad of the layer is destroyed", 1, layerAd.destroyCount)
        assertEquals("the slot comes back", 1, logged("Bottom bar ${bar.key}: slot back after the collapsible layer"))
        assertEquals(1, logged(inContentBlockLog(listOf(medium))))

        // Past the reload of the layer (60 s) and its opening again (30 s): nothing for the layer.
        advanceBy(61_000)
        assertEquals("no new request of the layer", layerRequests, requestsOf(AdFormat.NATIVE_COLLAPSIBLE))
        assertEquals("one display opportunity, the slot back (D-07)", eligibleBefore + 1, eligibleCount())
        assertFalse(layerShown())
    }

    @Test
    fun turnedOffWhileOpen_layerBackNextEntry() {
        switch(medium, true)
        start()
        var shown by mutableStateOf(true)
        show {
            if (shown) InContentAds(listOf(medium)) { Bar() }
        }
        assertEquals(0, requestsOf(AdFormat.NATIVE_COLLAPSIBLE))

        switch(medium, false)
        frames()
        advanceBy(1_000)
        assertEquals("still no request of the layer", 0, requestsOf(AdFormat.NATIVE_COLLAPSIBLE))
        assertEquals(0, logged("Bottom bar ${bar.key}: slot=NATIVE collapsible=true"))

        // The next entry into the screen.
        shown = false
        frames()
        shown = true
        frames()
        assertEquals("the layer loads again", 1, requestsOf(AdFormat.NATIVE_COLLAPSIBLE))
    }

    @Test
    fun declaredOn_collapsibleBannerNoViewNoSpace() {
        switch(mrec, true)
        switch(collapsible, true)
        start()
        var shown by mutableStateOf(true)
        show {
            if (shown) {
                InContentAds(listOf(mrec)) {
                    Column {
                        AdSlot(mrec, Modifier.fillMaxWidth().testTag(MREC))
                        CollapsibleBanner()
                    }
                }
            }
        }

        assertEquals("the ad view of the MREC only (two before plan T-092)", 1, adViews().size)
        assertHeight("collapsible banner takes no space", 0.dp, height(COLLAPSIBLE))

        // Control: the MREC off at the next entry, the collapsible banner runs.
        switch(mrec, false)
        shown = false
        frames()
        shown = true
        frames()
        assertEquals("the ad view of the collapsible banner", 1, adViews().size)
        assertHeight("collapsible banner runs", collapsibleHeight(), height(COLLAPSIBLE))
    }

    @Test
    fun turnedOnWhileOpen_collapsibleBannerGone() {
        switch(collapsible, true)
        start()
        var shown by mutableStateOf(true)
        show {
            if (shown) InContentAds(listOf(medium)) { CollapsibleBanner() }
        }
        assertEquals(1, adViews().size)
        assertHeight("collapsible banner runs", collapsibleHeight(), height(COLLAPSIBLE))

        switch(medium, true)
        frames()
        assertEquals("the ad view is gone", 0, adViews().size)
        assertHeight("no space", 0.dp, height(COLLAPSIBLE))

        switch(medium, false)
        frames()
        assertEquals("not back on this entry", 0, adViews().size)
        assertHeight("still no space", 0.dp, height(COLLAPSIBLE))

        // The next entry into the screen, the in-content placement off.
        shown = false
        frames()
        shown = true
        frames()
        assertEquals(1, adViews().size)
        assertHeight("collapsible banner runs again", collapsibleHeight(), height(COLLAPSIBLE))
    }

    @Test
    fun blockedCollapsibleFirst_fixedBannerRuns() {
        switch(medium, true)
        switch(collapsible, true)
        switch(fixed, true)
        start()
        var shown by mutableStateOf(true)
        show {
            if (shown) {
                InContentAds(listOf(medium)) {
                    Column {
                        CollapsibleBanner()
                        FixedBanner()
                    }
                }
            }
        }

        assertEquals(1, adViews().size)
        assertHeight("fixed banner runs", fixedHeight(), height(FIXED))
        assertHeight("collapsible banner takes no space", 0.dp, height(COLLAPSIBLE))
        assertEquals("no wait warning", 0, waitLogs())

        // Control: the in-content placement off at the next entry, the collapsible banner holds the turn (T-091).
        switch(medium, false)
        shown = false
        frames()
        shown = true
        frames()
        assertEquals(1, adViews().size)
        assertHeight("collapsible banner runs", collapsibleHeight(), height(COLLAPSIBLE))
        assertHeight("fixed banner waits", 0.dp, height(FIXED))
    }

    @Test
    fun blockedCollapsibleFirst_barRuns() {
        switch(medium, true)
        switch(collapsible, true)
        start()
        var shown by mutableStateOf(true)
        show {
            if (shown) {
                InContentAds(listOf(medium)) {
                    Column {
                        CollapsibleBanner()
                        Bar()
                    }
                }
            }
        }

        assertEquals("the bar sends its slot", 1, requestsOf(AdFormat.NATIVE_SMALL))
        assertEquals("the bar has no layer either", 0, requestsOf(AdFormat.NATIVE_COLLAPSIBLE))
        assertEquals(0, adViews().size)
        assertEquals("no wait warning", 0, waitLogs())

        // Control: the in-content placement off at the next entry, the collapsible banner holds the turn (T-091).
        switch(medium, false)
        shown = false
        frames()
        shown = true
        frames()
        assertEquals(1, adViews().size)
        assertHeight("collapsible banner runs", collapsibleHeight(), height(COLLAPSIBLE))
    }

    @Test
    fun holderBlockedWhileOpen_waitingStillWaits() {
        switch(collapsible, true)
        switch(fixed, true)
        start()
        show {
            InContentAds(listOf(medium)) {
                Column {
                    CollapsibleBanner()
                    FixedBanner()
                }
            }
        }
        // Control: the collapsible banner holds the turn before the switch.
        assertEquals(1, adViews().size)
        assertHeight("collapsible banner runs", collapsibleHeight(), height(COLLAPSIBLE))

        switch(medium, true)
        frames()

        assertEquals("no ad view", 0, adViews().size)
        assertHeight("collapsible banner takes no space", 0.dp, height(COLLAPSIBLE))
        assertHeight("the fixed banner still waits (MC-12)", 0.dp, height(FIXED))
    }

    @Test
    fun declaredOn_eligibleOnlyOfSlotAndContent() {
        switch(medium, true)
        start()
        show { InContentAds(listOf(medium)) { Bar { NativeAdSlot(medium) } } }

        deliverAll()
        advanceBy(1_000)

        assertEquals("the slot and the medium only", 2, eligibleCount())
    }

    /** Control of [declaredOn_eligibleOnlyOfSlotAndContent]: the same screen without a declaration. */
    @Test
    fun noDeclaration_eligibleThree() {
        switch(medium, true)
        start()
        show { Bar { NativeAdSlot(medium) } }

        deliverAll()
        advanceBy(1_000)

        assertEquals("the layer, the slot and the medium", 3, eligibleCount())
    }

    private enum class Screen { NONE, DECLARED, PLAIN }

    /** One test unit per format, so a request tells its format. */
    private object UnitPerFormat : AdUnitIds {
        fun unitFor(format: AdFormat): String = "unit-${format.code}"

        override fun of(placement: AdPlacement): String = of(placement, placement.format)

        override fun of(placement: AdPlacement, format: AdFormat): String = unitFor(format)
    }

    private companion object {
        const val STEP_MILLIS = 250L

        /** Long enough for both preload groups (S-22), shorter than the 10 s a load waits. */
        const val PRELOAD_MILLIS = 7_000L
        const val FIRST_FRAMES = 6
        const val TOLERANCE_DP = 0.5f
        const val FIXED = "fixed"
        const val COLLAPSIBLE = "collapsible"
        const val MREC = "mrec"
        const val LAYER_AD = "Ad of the layer"
        const val SLOT_AD = "Ad of the slot"

        /**
         * The frames of the first request of the layer and of the slot without a declaration, measured on the main
         * branch with plan T-091 (527e33d, section 3.4 of plan T-092): frame 1 and frame 2, as before plan T-091.
         */
        val CONTROL_FRAMES: Pair<Int?, Int?> = 1 to 2
    }
}
