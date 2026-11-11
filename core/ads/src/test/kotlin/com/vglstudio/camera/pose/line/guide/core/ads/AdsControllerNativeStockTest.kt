package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Application
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import java.time.Duration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

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
    fun layer_entersAfterPreload_showsStockAd() {
        startAndPreload()
        deliver(AdFormat.NATIVE_SMALL, STOCK_AD)
        deliver(AdFormat.NATIVE_COLLAPSIBLE, LAYER_AD)
        assertEquals(1, requestsOf(AdFormat.NATIVE_COLLAPSIBLE))

        show { BottomAdBarScaffold(AdPlacement.POSES_BOTTOM) {} }

        assertTrue(shownIn(CollapsibleNativeAdTestTags.AD, LAYER_AD))
        // The ad of the layer came from the stock: one more collapsible request, the next load after the claim.
        assertEquals(2, requestsOf(AdFormat.NATIVE_COLLAPSIBLE))
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
