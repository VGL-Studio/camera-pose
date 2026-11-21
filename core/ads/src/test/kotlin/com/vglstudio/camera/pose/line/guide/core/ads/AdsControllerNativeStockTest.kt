package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Application
import android.content.res.Configuration
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.analytics.AdFunnelEvent
import com.vglstudio.camera.pose.line.guide.core.analytics.AdFunnelKind
import com.vglstudio.camera.pose.line.guide.core.analytics.RecordingAdFunnel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import java.time.Duration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The native small and collapsible stocks of KB-33 in the real [AdsController] (plan T-086): filled by the preload
 * plan before any screen, shown at once by the bottom slot, the layer and a standalone slot, kept across screens,
 * never filled while consent is closed, ad-free, a key is off or the app is in background. A fake native loader
 * ([AdsController.nativeLoaderForTest]) and one test unit per format, so the format of a request is read from its
 * unit; the metrics of the loads are checked in [NativeAdSourcesTest] and [AdTelemetryTest].
 */
@RunWith(AndroidJUnit4::class)
class AdsControllerNativeStockTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val logger = RecordingAppLogger()
    private val funnel = RecordingAdFunnel()
    private val loader = FakeNativeAdLoader()
    private val adFree = MutableStateFlow(false)
    private val settings = FakeRemoteSettings().apply {
        set(AdsSettings.IS_SHOW_NATIVE_BOTTOM, true)
        set(AdsSettings.placementEnabled(AdPlacement.POSES_BOTTOM), true)
        set(AdsSettings.placementEnabled(AdPlacement.GALLERY_NATIVE_SMALL), true)
    }

    /** Requests of [loader] already answered by the test. */
    private val answered = mutableSetOf<Int>()

    private val controller by lazy {
        testAdsController(
            context = application,
            unitIds = StockUnitIds,
            flags = DefaultAdsFlags(
                settings,
                object : AdFreeStatus {
                    override val adFree: StateFlow<Boolean> = this@AdsControllerNativeStockTest.adFree
                },
            ),
            funnel = funnel,
            logger = logger,
        ).also { it.nativeLoaderForTest = loader }
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun idleFor(millis: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))

    /** Started, consent open and AdMob ready (S-21), still in background. */
    private fun startToSdkReady() {
        controller.start(application, AppActivityFilter { true })
        idle()
        controller.openConsentGateForTest()
        controller.admobReadyForTest()
        idle()
    }

    /**
     * Long enough for both preload groups (each moves on once settled, or after 6 s, S-22), and shorter than the 10 s
     * a native load waits for its ad, so the requests sent are still open for the test to answer.
     */
    private fun runPreload() = idleFor(7_000)

    /** The setup of section 4 of the plan: in foreground, then the preload of S-21. */
    private fun startAndPreload() {
        startToSdkReady()
        controller.setAppForegroundForTest(true)
        idle()
        controller.requestPreloadForTest()
        runPreload()
    }

    private fun requestsOf(format: AdFormat): Int = loader.requests.count { it.unitId == StockUnitIds.unitFor(format) }

    /** Answers the oldest request of [format] not answered yet with an ad of [headline]. */
    private fun deliver(format: AdFormat, headline: String): FakeLoadedNativeAd {
        val index = loader.requests.indices.first {
            it !in answered && loader.requests[it].unitId == StockUnitIds.unitFor(format)
        }
        answered += index
        val ad = FakeLoadedNativeAd(sampleNativeAdAssets.copy(headline = headline))
        loader.deliverAt(index, ad)
        idle()
        return ad
    }

    private fun show(content: @Composable () -> Unit) {
        composeRule.setContent { CompositionLocalProvider(LocalAdsController provides controller) { content() } }
        composeRule.waitForIdle()
    }

    private fun shownIn(tag: String, headline: String): Boolean = composeRule.onAllNodes(
        hasText(headline) and hasAnyAncestor(hasTestTag(tag)),
        useUnmergedTree = true,
    ).fetchSemanticsNodes().isNotEmpty()

    private fun eligibleEvents(): List<AdFunnelEvent.Eligible> =
        funnel.events.filterIsInstance<AdFunnelEvent.Eligible>()

    private fun layerDrawn(): Boolean = composeRule.onAllNodes(
        hasTestTag(CollapsibleNativeAdTestTags.AD),
        useUnmergedTree = true,
    ).fetchSemanticsNodes().isNotEmpty()

    private fun loggedNoRoom(window: String): Boolean = logger.entries.any {
        it.message == "Collapsible poses_bottom not shown: window $window dp has no room"
    }

    private fun shown(headline: String): Boolean =
        composeRule.onAllNodes(hasText(headline), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun sdkReady_preloadFillsSmallAndCollapStocks_beforeAnyScreen() {
        startAndPreload()

        assertEquals(
            listOf(StockUnitIds.unitFor(AdFormat.NATIVE_COLLAPSIBLE), StockUnitIds.unitFor(AdFormat.NATIVE_SMALL)),
            loader.requests.map { it.unitId }.sorted(),
        )
    }

    @Test
    fun bottomSlot_collapOff_entersAfterPreload_showsStockAd() {
        settings.set(AdsSettings.IS_SHOW_NATIVE_COLLAP, false)
        startAndPreload()
        assertEquals(1, requestsOf(AdFormat.NATIVE_SMALL))
        assertEquals(0, requestsOf(AdFormat.NATIVE_COLLAPSIBLE))
        deliver(AdFormat.NATIVE_SMALL, STOCK_AD)

        show { BottomAdBarScaffold(AdPlacement.POSES_BOTTOM) {} }

        assertTrue(shownIn(BottomAdBarTestTags.SLOT, STOCK_AD))
        // Only the next load of the stock, after the slot took its ad: none was sent for the slot itself.
        assertEquals(2, requestsOf(AdFormat.NATIVE_SMALL))
        assertEquals(1, loader.requests.size - answered.size)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun layer_entersAfterPreload_showsStockAd() {
        startAndPreload()
        val stockAd = deliver(AdFormat.NATIVE_SMALL, STOCK_AD)
        deliver(AdFormat.NATIVE_COLLAPSIBLE, LAYER_AD)
        assertEquals(1, requestsOf(AdFormat.NATIVE_COLLAPSIBLE))

        show { BottomAdBarScaffold(AdPlacement.POSES_BOTTOM) {} }

        assertTrue(shownIn(CollapsibleNativeAdTestTags.AD, LAYER_AD))
        // The ad of the layer came from the stock: one more collapsible request, the next load after the claim.
        assertEquals(2, requestsOf(AdFormat.NATIVE_COLLAPSIBLE))
        // One display opportunity for the slot and one for the layer (control of the landscape test below).
        assertEquals(List(2) { AdFunnelEvent.Eligible(AdFunnelKind.NATIVE) }, eligibleEvents())
        // D-07 (plan T-090, question 1: option A): the slot took its stock ad on entry, then gave way to the open
        // layer, which destroyed that ad.
        assertEquals("the slot's stock ad is destroyed", 1, stockAd.destroyCount)
        assertFalse(shownIn(BottomAdBarTestTags.SLOT, STOCK_AD))
    }

    /**
     * KB-4 added 2026-11-16, KB-33, section 8.3 of the spec: on a 891 x 411 dp window the layer has no room, so it
     * never takes the ad of the `native_collap` stock (no next load), reports no display opportunity, and logs it;
     * the slot shows its stock ad and reports its own opportunity.
     */
    @Test
    @Config(qualifiers = "w891dp-h411dp-xxhdpi")
    fun landscape_layerNoRoom_stockKept_noEligible_logged() {
        startAndPreload()
        deliver(AdFormat.NATIVE_SMALL, STOCK_AD)
        val layerAd = deliver(AdFormat.NATIVE_COLLAPSIBLE, LAYER_AD)

        show { BottomAdBarScaffold(AdPlacement.POSES_BOTTOM) {} }

        assertTrue(shownIn(BottomAdBarTestTags.SLOT, STOCK_AD))
        assertFalse("no layer without room", layerDrawn())
        assertEquals("the stock was not claimed: no next load", 1, requestsOf(AdFormat.NATIVE_COLLAPSIBLE))
        assertEquals("the stock keeps its ad", 0, layerAd.destroyCount)
        assertEquals(listOf(AdFunnelEvent.Eligible(AdFunnelKind.NATIVE)), eligibleEvents())
        assertTrue(loggedNoRoom("891x411"))
    }

    /**
     * KB-4 added 2026-11-16: the window changes without the activity being rebuilt (from 411 x 891 to 891 x 411 dp):
     * the open layer goes and its ad is destroyed (L-061), the line is logged; the slot, which gave way to the open
     * layer (D-07, plan T-090), comes back at 106 dp with the ad its stock kept meanwhile, and reports one new display
     * opportunity (question 2 of plan T-090); the layer reports none.
     */
    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun configurationChange_layerGoes_adDestroyed_logged() {
        val base = application.resources.configuration
        fun window(width: Int, height: Int) = Configuration(base).apply {
            screenWidthDp = width
            screenHeightDp = height
        }
        var config by mutableStateOf(window(411, 891))
        startAndPreload()
        val stockAd = deliver(AdFormat.NATIVE_SMALL, STOCK_AD)
        val layerAd = deliver(AdFormat.NATIVE_COLLAPSIBLE, LAYER_AD)
        composeRule.setContent {
            CompositionLocalProvider(LocalAdsController provides controller, LocalConfiguration provides config) {
                BottomAdBarScaffold(AdPlacement.POSES_BOTTOM) {}
            }
        }
        composeRule.waitForIdle()
        // Control: with room the layer shows the ad of the stock.
        assertTrue(shownIn(CollapsibleNativeAdTestTags.AD, LAYER_AD))
        // D-07: the slot gave way to the open layer, its stock ad destroyed.
        assertEquals("the slot's stock ad is destroyed", 1, stockAd.destroyCount)
        // The next load of the small stock answers while the slot gives way: the stock keeps it.
        deliver(AdFormat.NATIVE_SMALL, NEXT_AD)
        val eligibleBefore = eligibleEvents().size

        config = window(891, 411)
        composeRule.waitForIdle()

        assertFalse("the layer goes", layerDrawn())
        assertEquals("the held ad is destroyed", 1, layerAd.destroyCount)
        assertTrue(shownIn(BottomAdBarTestTags.SLOT, NEXT_AD))
        val slot = composeRule.onNodeWithTag(BottomAdBarTestTags.SLOT, useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertEquals("slot height", 106f, (slot.bottom - slot.top).value, 1f)
        assertTrue(loggedNoRoom("891x411"))
        assertEquals(
            "one new display opportunity, the slot's; none for the layer",
            eligibleBefore + 1,
            eligibleEvents().size,
        )
    }

    /**
     * KB-35 criterion 3, KB-33, O-01 (plan T-090): both stocks hold an ad at entry; the slot gives way to the open
     * layer while its stock loads the next ad and keeps it; collapsed, the slot shows that ad at once, sends no request
     * of its own (only the next load of the stock), and reports one new display opportunity.
     */
    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun collapse_slotTakesStock_eligibleOnce() {
        startAndPreload()
        deliver(AdFormat.NATIVE_SMALL, STOCK_AD)
        deliver(AdFormat.NATIVE_COLLAPSIBLE, LAYER_AD)
        show { BottomAdBarScaffold(AdPlacement.POSES_BOTTOM) {} }
        assertTrue(shownIn(CollapsibleNativeAdTestTags.AD, LAYER_AD))
        val kept = deliver(AdFormat.NATIVE_SMALL, NEXT_AD)
        val smallBefore = requestsOf(AdFormat.NATIVE_SMALL)
        val eligibleBefore = eligibleEvents().size

        composeRule.onNodeWithTag(CollapsibleNativeAdTestTags.COLLAPSE, useUnmergedTree = true).performClick()
        composeRule.waitForIdle()
        idle()
        composeRule.waitForIdle()

        assertFalse("the layer is collapsed", layerDrawn())
        assertTrue(shownIn(BottomAdBarTestTags.SLOT, NEXT_AD))
        assertEquals(0, kept.destroyCount)
        assertEquals("only the next load of the stock", smallBefore + 1, requestsOf(AdFormat.NATIVE_SMALL))
        assertEquals("one new display opportunity", eligibleBefore + 1, eligibleEvents().size)
    }

    private fun noRoomLines(window: String): Int = logger.entries.count {
        it.message == "Collapsible poses_bottom not shown: window $window dp has no room"
    }

    /**
     * KB-4 criterion 4, CL-05, MC-14 (plan T-109): on a 411 x 520 dp window at font scale 2.0 the small native leaves
     * the layer no room, so the layer never takes the ad of the `native_collap` stock, reports no display opportunity
     * and logs one line; the slot shows its stock ad and reports its own opportunity.
     */
    @Test
    @Config(qualifiers = "w411dp-h520dp-xxhdpi")
    fun largeText_layerNoRoom_stockKept_noEligible_logged() {
        startAndPreload()
        deliver(AdFormat.NATIVE_SMALL, STOCK_AD)
        val layerAd = deliver(AdFormat.NATIVE_COLLAPSIBLE, LAYER_AD)

        var roomAtDefault: Dp? = null
        show {
            // Control: the same window has room at font scale 1.0, so the missing layer is the text's doing.
            WithFontScale(1f) { roomAtDefault = BottomAdBarDefaults.collapsibleLayerHeight() }
            WithFontScale(2f) { BottomAdBarScaffold(AdPlacement.POSES_BOTTOM) {} }
        }

        assertNotNull("room at font scale 1.0", roomAtDefault)
        assertTrue(shownIn(BottomAdBarTestTags.SLOT, STOCK_AD))
        assertFalse("no layer without room", layerDrawn())
        assertEquals("the stock was not claimed: no next load", 1, requestsOf(AdFormat.NATIVE_COLLAPSIBLE))
        assertEquals("the stock keeps its ad", 0, layerAd.destroyCount)
        assertEquals(listOf(AdFunnelEvent.Eligible(AdFunnelKind.NATIVE)), eligibleEvents())
        assertEquals("one no-room line", 1, noRoomLines("411x520"))
    }

    /** Control of the test above: the same window at font scale 1.0 has room, the layer shows and reports. */
    @Test
    @Config(qualifiers = "w411dp-h520dp-xxhdpi")
    fun largeText_layerNoRoom_fontScale1_layerShown() {
        startAndPreload()
        deliver(AdFormat.NATIVE_SMALL, STOCK_AD)
        deliver(AdFormat.NATIVE_COLLAPSIBLE, LAYER_AD)

        show { WithFontScale(1f) { BottomAdBarScaffold(AdPlacement.POSES_BOTTOM) {} } }

        assertTrue("the layer shows", layerDrawn())
        assertTrue(shownIn(CollapsibleNativeAdTestTags.AD, LAYER_AD))
        assertEquals(
            "one display opportunity for the slot and one for the layer",
            List(2) { AdFunnelEvent.Eligible(AdFunnelKind.NATIVE) },
            eligibleEvents(),
        )
        assertEquals("no no-room line", 0, noRoomLines("411x520"))
    }

    @Test
    fun standaloneSlot_entersAfterPreload_showsStockAd() {
        startAndPreload()
        deliver(AdFormat.NATIVE_SMALL, STOCK_AD)

        show { NativeAdSlot(AdPlacement.GALLERY_NATIVE_SMALL) }

        assertTrue(shown(STOCK_AD))
        assertEquals(2, requestsOf(AdFormat.NATIVE_SMALL))
    }

    @Test
    fun leaveScreen_stockKept_nextScreenGetsIt() {
        settings.set(AdsSettings.IS_SHOW_NATIVE_COLLAP, false)
        startAndPreload()
        deliver(AdFormat.NATIVE_SMALL, STOCK_AD)
        var screen by mutableStateOf(SCREEN_A)
        show {
            when (screen) {
                SCREEN_A -> BottomAdBarScaffold(AdPlacement.POSES_BOTTOM) {}
                SCREEN_B -> BottomAdBarScaffold(AdPlacement.GALLERY_NATIVE_SMALL) {}
                else -> Unit
            }
        }
        assertTrue(shownIn(BottomAdBarTestTags.SLOT, STOCK_AD))
        // The stock is loading its next ad when screen A leaves.
        assertEquals(2, requestsOf(AdFormat.NATIVE_SMALL))

        screen = NO_SCREEN
        composeRule.waitForIdle()
        val kept = deliver(AdFormat.NATIVE_SMALL, NEXT_AD)
        assertEquals(0, kept.destroyCount)
        val sentBefore = loader.requests.size

        screen = SCREEN_B
        composeRule.waitForIdle()

        assertTrue(shownIn(BottomAdBarTestTags.SLOT, NEXT_AD))
        // One request more: the next load after screen B took the kept ad.
        assertEquals(sentBefore + 1, loader.requests.size)
        assertEquals(0, kept.destroyCount)
    }

    @Test
    fun consentClosed_noStockLoad_heldUntilOpen() {
        startToSdkReady()
        controller.setAppForegroundForTest(true)
        controller.closeConsentGateForTest()
        idle()

        controller.requestPreloadForTest()
        runPreload()
        assertTrue(loader.requests.isEmpty())

        controller.reopenConsentGateForTest()
        runPreload()
        assertEquals(1, requestsOf(AdFormat.NATIVE_SMALL))
        assertEquals(1, requestsOf(AdFormat.NATIVE_COLLAPSIBLE))
    }

    @Test
    fun adFree_noStockLoad_existingStockDestroyed() {
        startAndPreload()
        val small = deliver(AdFormat.NATIVE_SMALL, STOCK_AD)
        val layer = deliver(AdFormat.NATIVE_COLLAPSIBLE, LAYER_AD)
        val sent = loader.requests.size

        adFree.value = true
        idle()

        assertEquals(1, small.destroyCount)
        assertEquals(1, layer.destroyCount)
        controller.requestPreloadForTest()
        runPreload()
        assertEquals(sent, loader.requests.size)
    }

    @Test
    fun collapSwitchedOff_stockDestroyed_noMoreFill() {
        startAndPreload()
        deliver(AdFormat.NATIVE_SMALL, STOCK_AD)
        val layer = deliver(AdFormat.NATIVE_COLLAPSIBLE, LAYER_AD)

        settings.set(AdsSettings.IS_SHOW_NATIVE_COLLAP, false)
        idle()
        assertEquals(1, layer.destroyCount)
        controller.requestPreloadForTest()
        runPreload()
        assertEquals(1, requestsOf(AdFormat.NATIVE_COLLAPSIBLE))

        // Back on: a preload is asked for (S-20) and fills the stock again.
        settings.set(AdsSettings.IS_SHOW_NATIVE_COLLAP, true)
        runPreload()
        assertEquals(2, requestsOf(AdFormat.NATIVE_COLLAPSIBLE))
        assertTrue(logger.entries.any { it.message == "Preload PLACEMENT_ENABLED: RUN" })
    }

    @Test
    fun nativeBottomOff_noSlot_smallStockStopsFilling_keepsAd() {
        startAndPreload()
        val held = deliver(AdFormat.NATIVE_SMALL, STOCK_AD)

        settings.set(AdsSettings.IS_SHOW_NATIVE_BOTTOM, false)
        idle()
        controller.requestPreloadForTest()
        runPreload()
        assertEquals(1, requestsOf(AdFormat.NATIVE_SMALL))
        assertEquals(0, held.destroyCount)

        // A standalone slot comes: it takes the ad held, and the stock loads the next one while it is on screen.
        var onScreen by mutableStateOf(true)
        show { if (onScreen) NativeAdSlot(AdPlacement.GALLERY_NATIVE_SMALL) }
        assertTrue(shown(STOCK_AD))
        assertEquals(2, requestsOf(AdFormat.NATIVE_SMALL))

        onScreen = false
        composeRule.waitForIdle()
        val next = deliver(AdFormat.NATIVE_SMALL, NEXT_AD)
        idleFor(NativeAdPolicy.MAX_AGE_MILLIS)
        // Expired with no slot on screen and the flag off: dropped, not loaded again.
        assertEquals(1, next.destroyCount)
        assertEquals(2, requestsOf(AdFormat.NATIVE_SMALL))
    }

    @Test
    fun preloadHeldInBackground_sentOnceOnResume() {
        startToSdkReady()
        controller.setAppForegroundForTest(false)
        controller.requestPreloadForTest()
        runPreload()
        assertEquals(0, requestsOf(AdFormat.NATIVE_SMALL))
        assertEquals(0, requestsOf(AdFormat.NATIVE_COLLAPSIBLE))

        controller.setAppForegroundForTest(true)
        runPreload()

        assertEquals(1, requestsOf(AdFormat.NATIVE_SMALL))
        assertEquals(1, requestsOf(AdFormat.NATIVE_COLLAPSIBLE))
    }

    // A bottom bar declared without the floating layer (plan T-094; spec ads, question 19 point e; spec camera, KB-9
    // criterion 6): with is_show_native_collap on, the real controller neither loads nor shows the layer for it.

    /** Only the bar of [placement] on, with is_show_native_collap [collapOn]; the other bars of the test off. */
    private fun onlyBar(placement: AdPlacement, collapOn: Boolean) {
        settings.set(AdsSettings.placementEnabled(AdPlacement.POSES_BOTTOM), false)
        settings.set(AdsSettings.placementEnabled(AdPlacement.GALLERY_NATIVE_SMALL), false)
        settings.set(AdsSettings.placementEnabled(placement), true)
        settings.set(AdsSettings.IS_SHOW_NATIVE_COLLAP, collapOn)
    }

    private fun layerShown(): Boolean = composeRule.onAllNodes(
        hasTestTag(BottomAdBarTestTags.LAYER) or hasTestTag(CollapsibleNativeAdTestTags.AD),
        useUnmergedTree = true,
    ).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun reviewBottom_collapOn_noLayer_noCollapsibleRequest() {
        onlyBar(AdPlacement.REVIEW_BOTTOM, collapOn = true)
        startAndPreload()
        assertEquals(0, requestsOf(AdFormat.NATIVE_COLLAPSIBLE))
        assertEquals(1, requestsOf(AdFormat.NATIVE_SMALL))
        deliver(AdFormat.NATIVE_SMALL, STOCK_AD)

        show { BottomAdBarScaffold(AdPlacement.REVIEW_BOTTOM) {} }

        assertTrue(shownIn(BottomAdBarTestTags.SLOT, STOCK_AD))
        assertFalse(layerShown())
        assertEquals(0, requestsOf(AdFormat.NATIVE_COLLAPSIBLE))
    }

    @Test
    fun posesBottom_collapOn_layerAndCollapsibleRequest() {
        // Control of the test above: the same setup with the library bar loads and shows the layer.
        onlyBar(AdPlacement.POSES_BOTTOM, collapOn = true)
        startAndPreload()
        assertEquals(1, requestsOf(AdFormat.NATIVE_COLLAPSIBLE))
        assertEquals(1, requestsOf(AdFormat.NATIVE_SMALL))
        deliver(AdFormat.NATIVE_SMALL, STOCK_AD)

        show { BottomAdBarScaffold(AdPlacement.POSES_BOTTOM) {} }

        assertTrue(shownIn(BottomAdBarTestTags.SLOT, STOCK_AD))
        assertTrue(layerShown())
    }

    @Test
    fun reviewBottom_collapSwitchedOnWhileShown_noLayer() {
        showThenSwitchCollapOn(AdPlacement.REVIEW_BOTTOM)
        assertFalse(layerShown())
        assertEquals(0, requestsOf(AdFormat.NATIVE_COLLAPSIBLE))
    }

    @Test
    fun posesBottom_collapSwitchedOnWhileShown_layerLoads() {
        // Control of the test above: the library bar gets the layer and a collapsible request once the key is on.
        showThenSwitchCollapOn(AdPlacement.POSES_BOTTOM)
        assertTrue(layerShown())
        assertTrue(requestsOf(AdFormat.NATIVE_COLLAPSIBLE) >= 1)
    }

    /** The bar of [placement] shows its slot with is_show_native_collap off, then the key is switched on. */
    private fun showThenSwitchCollapOn(placement: AdPlacement) {
        onlyBar(placement, collapOn = false)
        startAndPreload()
        deliver(AdFormat.NATIVE_SMALL, STOCK_AD)
        show { BottomAdBarScaffold(placement) {} }
        assertTrue(shownIn(BottomAdBarTestTags.SLOT, STOCK_AD))
        assertFalse(layerShown())
        assertEquals(0, requestsOf(AdFormat.NATIVE_COLLAPSIBLE))

        settings.set(AdsSettings.IS_SHOW_NATIVE_COLLAP, true)
        idle()
        runPreload()
        composeRule.waitForIdle()
    }

    /**
     * One test unit per format (section 4 of plan T-086), so a request tells its format: Google's test units give
     * the collapsible and medium formats the same one.
     */
    private object StockUnitIds : AdUnitIds {
        fun unitFor(format: AdFormat): String = "unit-${format.code}"

        override fun of(placement: AdPlacement): String = of(placement, placement.format)

        override fun of(placement: AdPlacement, format: AdFormat): String = unitFor(format)
    }

    private companion object {
        const val STOCK_AD = "Ad of the small stock"
        const val LAYER_AD = "Ad of the collapsible stock"
        const val NEXT_AD = "Next ad of the small stock"
        const val SCREEN_A = 1
        const val SCREEN_B = 2
        const val NO_SCREEN = 0
    }
}
