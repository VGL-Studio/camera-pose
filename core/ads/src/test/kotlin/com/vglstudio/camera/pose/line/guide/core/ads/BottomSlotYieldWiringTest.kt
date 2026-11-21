package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Application
import android.os.Looper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.analytics.AdFunnelEvent
import com.vglstudio.camera.pose.line.guide.core.analytics.RecordingAdFunnel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import com.vglstudio.camera.pose.line.guide.core.network.InternetConnectivity
import java.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The native bottom slot giving way to the open collapsible layer (D-07, KB-35, MC-14; plan T-090) through the real
 * [BottomAdBarScaffold] and [AdsController], with a fake native loader ([AdsController.nativeLoaderForTest]) and one
 * test unit per format, so the format of a request is read from its unit. While the slot gives way it sends nothing
 * (no first load, refresh, retry, reload on foreground or on Internet back), takes nothing from its stock and reports
 * no display opportunity; a load sent before stays in the stock (the exception of O-03) and the slot takes it when it
 * comes back. Both clocks move together: the one of the composition (the slot and the layer) and the main looper (the
 * controller, its stocks and the 10 s wait of a load).
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class BottomSlotYieldWiringTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val placement = AdPlacement.SAMPLE_NATIVE_SMALL
    private val logger = RecordingAppLogger()
    private val funnel = RecordingAdFunnel()
    private val loader = FakeNativeAdLoader()
    private val online = MutableStateFlow(true)
    private val connectivity = object : InternetConnectivity {
        override val connected: Flow<Boolean> = online
    }
    private val settings = FakeRemoteSettings().apply {
        set(AdsSettings.IS_SHOW_NATIVE_BOTTOM, true)
        set(AdsSettings.placementEnabled(AdPlacement.SAMPLE_NATIVE_SMALL), true)
        // The layer never opens again by itself unless a test says so.
        set(AdsSettings.TIME_EXPAND_COLLAP_AD, 0L)
        set(AdsSettings.TIME_RELOAD_NATIVE_SMALL, 60L)
    }

    /** Requests of [loader] already answered by the test. */
    private val answered = mutableSetOf<Int>()

    private val controller by lazy {
        testAdsController(
            context = application,
            unitIds = YieldUnitIds,
            flags = DefaultAdsFlags(settings),
            funnel = funnel,
            logger = logger,
            connectivity = connectivity,
        ).also { it.nativeLoaderForTest = loader }
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    /** Started, consent open, AdMob ready, in foreground; no preload asked, so both stocks are empty. */
    private fun start() {
        controller.start(application, AppActivityFilter { true })
        idle()
        controller.openConsentGateForTest()
        controller.admobReadyForTest()
        controller.setAppForegroundForTest(true)
        idle()
    }

    /** The bar of [placement] on screen, with the clock of Compose stopped. */
    private fun show() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CompositionLocalProvider(LocalAdsController provides controller) { BottomAdBarScaffold(placement) {} }
        }
        frames()
    }

    /** A few frames, the main looper idle between them: a change, its effects, and what the bar draws after them. */
    private fun frames() {
        repeat(3) {
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.waitForIdle()
            idle()
        }
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

    private fun requestsOf(format: AdFormat): Int = loader.requests.count { it.unitId == YieldUnitIds.unitFor(format) }

    private fun openRequestOf(format: AdFormat, newest: Boolean = false): Int {
        val open = loader.requests.indices.filter {
            it !in answered && loader.requests[it].unitId == YieldUnitIds.unitFor(format)
        }
        check(open.isNotEmpty()) { "no open request of $format" }
        return if (newest) open.last() else open.first()
    }

    /** Answers the oldest open request of [format] (the newest with [newest]) with an ad of [headline]. */
    private fun deliver(format: AdFormat, headline: String, newest: Boolean = false): FakeLoadedNativeAd {
        val index = openRequestOf(format, newest)
        answered += index
        val ad = FakeLoadedNativeAd(sampleNativeAdAssets.copy(headline = headline))
        loader.deliverAt(index, ad)
        idle()
        frames()
        return ad
    }

    /** Fails the oldest open request of [format] with an SDK error (no fill). */
    private fun fail(format: AdFormat) {
        val index = openRequestOf(format)
        answered += index
        loader.requests[index].onFailed(3, null)
        idle()
        frames()
    }

    /** Taps the collapse button; the layer reports it is collapsed one frame later, so the slot comes back after it. */
    private fun collapse() {
        composeRule.onNodeWithTag(CollapsibleNativeAdTestTags.COLLAPSE, useUnmergedTree = true).performClick()
        frames()
        frames()
    }

    private fun slotShows(headline: String): Boolean = composeRule.onAllNodes(
        hasText(headline) and hasAnyAncestor(hasTestTag(BottomAdBarTestTags.SLOT)),
        useUnmergedTree = true,
    ).fetchSemanticsNodes().isNotEmpty()

    private fun layerShown(): Boolean =
        composeRule.onAllNodesWithTag(CollapsibleNativeAdTestTags.AD, useUnmergedTree = true)
            .fetchSemanticsNodes().isNotEmpty()

    private fun placeholders(): Int =
        composeRule.onAllNodesWithTag(NativeAdTestTags.PLACEHOLDER, useUnmergedTree = true).fetchSemanticsNodes().size

    private fun eligibleCount(): Int = funnel.events.count { it is AdFunnelEvent.Eligible }

    /** The result lines of the loads sent for the slot (`ad_load_*` of each request, logged under its key). */
    private fun slotLoadResults(): Int = logger.entries.count {
        it.message == "Native ${placement.key} loaded" ||
            it.message.startsWith("Native ${placement.key} failed") ||
            it.message.startsWith("Native ${placement.key} timed out")
    }

    private fun logged(line: String): Int = logger.entries.count { it.message == line }

    /**
     * KB-35 criterion 2, every path of a load: the first load in flight when the layer opens stays in the stock, and
     * the slot shows it once the layer is collapsed; while the layer is open again over a slot with an ad, past the
     * refresh count of the slot and with Internet back, nothing is sent and the ad of the stock is not taken.
     */
    @Test
    fun yielded_noRequest_noRefresh_noRetry() {
        settings.set(AdsSettings.TIME_EXPAND_COLLAP_AD, 30L)
        start()
        show()
        // The stocks are empty: the slot and the layer each wait for a load.
        assertEquals(1, requestsOf(AdFormat.NATIVE_SMALL))
        deliver(AdFormat.NATIVE_COLLAPSIBLE, LAYER_AD)
        assertTrue("the layer opens", layerShown())

        // The first load of the slot answers while it gives way: the ad goes to the stock, kept.
        val firstAd = deliver(AdFormat.NATIVE_SMALL, FIRST_AD)
        assertEquals("kept by the stock", 0, firstAd.destroyCount)
        assertFalse("not shown while the slot gives way", slotShows(FIRST_AD))
        assertEquals(1, requestsOf(AdFormat.NATIVE_SMALL))

        // Collapsed: the slot takes it, and the stock sends its next load (O-01), nothing else.
        collapse()
        assertTrue(slotShows(FIRST_AD))
        assertEquals("the next load of the stock only", 2, requestsOf(AdFormat.NATIVE_SMALL))
        val pendingAd = deliver(AdFormat.NATIVE_SMALL, PENDING_AD)

        // The layer opens again (CL-03) over the slot with its ad: the slot gives way, its ad destroyed.
        advanceBy(REEXPAND_MILLIS)
        assertTrue("the layer opens again", layerShown())
        assertEquals(1, firstAd.destroyCount)
        val sent = requestsOf(AdFormat.NATIVE_SMALL)

        // Past the refresh count of the slot (60 s + 500 ms) twice: nothing sent, the stock keeps its ad.
        advanceBy(130_000)
        assertEquals("no refresh while the slot gives way", sent, requestsOf(AdFormat.NATIVE_SMALL))

        // Internet goes and comes back (S-24): nothing sent for the slot.
        online.value = false
        frames()
        online.value = true
        frames()
        advanceBy(1_000)
        assertEquals("no reload on Internet back", sent, requestsOf(AdFormat.NATIVE_SMALL))
        assertEquals("the stock keeps its ad", 0, pendingAd.destroyCount)
        assertFalse(slotShows(PENDING_AD))

        collapse()
        assertTrue(slotShows(PENDING_AD))
        assertEquals("the next load of the stock only", sent + 1, requestsOf(AdFormat.NATIVE_SMALL))
    }

    /**
     * KB-35 criterion 2, the retries (O-05) and Internet back (S-24): the load of the slot failed right before the layer
     * opened (the slot shrank to 0); while the slot gives way, past its retry waits and with Internet back, it sends
     * nothing and does not take the ad its stock holds. Collapsed, it takes that ad.
     */
    @Test
    fun yielded_failedBeforeLayer_noRetry_networkBack_noRequest() {
        start()
        show()
        fail(AdFormat.NATIVE_SMALL)
        assertEquals("the slot shrank to 0", 0, placeholders())
        deliver(AdFormat.NATIVE_COLLAPSIBLE, LAYER_AD)
        assertTrue("the layer opens", layerShown())

        // The stock holds an ad and loads nothing (it loads only with none and none in flight): every new request of
        // the small unit would be the slot's.
        controller.requestPreloadForTest()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(PRELOAD_MILLIS))
        frames()
        val pendingAd = deliver(AdFormat.NATIVE_SMALL, PENDING_AD)
        val sent = requestsOf(AdFormat.NATIVE_SMALL)

        // Past the retry waits of the slot (5, then 10 s).
        advanceBy(30_000)
        assertEquals("no retry while the slot gives way", sent, requestsOf(AdFormat.NATIVE_SMALL))

        online.value = false
        frames()
        online.value = true
        frames()
        advanceBy(1_000)
        assertEquals("no reload on Internet back", sent, requestsOf(AdFormat.NATIVE_SMALL))
        assertEquals("the stock keeps its ad", 0, pendingAd.destroyCount)
        assertFalse(slotShows(PENDING_AD))

        collapse()
        assertTrue(slotShows(PENDING_AD))
        assertEquals("the next load of the stock only", sent + 1, requestsOf(AdFormat.NATIVE_SMALL))
    }

    /**
     * KB-35 criterion 2, CL-01: the first load of the layer waits past its 10 s and gives nothing; its retry (O-05)
     * gets an ad, the layer opens then and the slot, which had its ad, gives way.
     */
    @Test
    fun layerAdAfterTimeout_stillOpens_slotYields() {
        start()
        show()
        val slotAd = deliver(AdFormat.NATIVE_SMALL, SLOT_AD)
        assertTrue(slotShows(SLOT_AD))

        advanceBy(10_500)
        assertFalse("no layer after the wait of its first load", layerShown())
        assertTrue("the slot keeps its ad", slotShows(SLOT_AD))
        val layerRequests = requestsOf(AdFormat.NATIVE_COLLAPSIBLE)

        advanceBy(6_000)
        assertEquals("the retry of the layer", layerRequests + 1, requestsOf(AdFormat.NATIVE_COLLAPSIBLE))
        deliver(AdFormat.NATIVE_COLLAPSIBLE, LAYER_AD, newest = true)

        assertTrue("the layer opens", layerShown())
        assertEquals("the slot ad is destroyed", 1, slotAd.destroyCount)
        assertFalse(slotShows(SLOT_AD))
    }

    /**
     * KB-35 criterion 2: the app goes to background while the slot gives way and comes back: nothing is sent for the
     * slot, the ad of its stock is not taken. The slot is the one most eager to send on the way back: its first load
     * failed, and its retry (O-05) falls due in background, where a try is held until foreground.
     */
    @Test
    fun yielded_activityBackToForeground_noSlotRequest() {
        start()
        show()
        fail(AdFormat.NATIVE_SMALL)
        deliver(AdFormat.NATIVE_COLLAPSIBLE, LAYER_AD)
        assertTrue("the layer opens", layerShown())
        controller.requestPreloadForTest()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(PRELOAD_MILLIS))
        frames()
        val pendingAd = deliver(AdFormat.NATIVE_SMALL, PENDING_AD)
        val sent = requestsOf(AdFormat.NATIVE_SMALL)

        controller.setAppForegroundForTest(false)
        frames()
        advanceBy(30_000)
        controller.setAppForegroundForTest(true)
        frames()
        advanceBy(1_000)

        assertEquals(sent, requestsOf(AdFormat.NATIVE_SMALL))
        assertEquals("the stock keeps its ad", 0, pendingAd.destroyCount)
        assertFalse(slotShows(PENDING_AD))
    }

    /**
     * MC-14, KB-35 criterion 6: while the slot gives way (past its refresh count too) it reports no display
     * opportunity, sends nothing and logs no load result; collapsed, it reports one new opportunity (question 2).
     */
    @Test
    fun yielded_noEligible_noLoadEvents_backCountsOnce() {
        start()
        show()
        deliver(AdFormat.NATIVE_SMALL, SLOT_AD)
        deliver(AdFormat.NATIVE_SMALL, PENDING_AD)
        deliver(AdFormat.NATIVE_COLLAPSIBLE, LAYER_AD)
        assertTrue("the layer opens", layerShown())
        val eligibleBefore = eligibleCount()
        val sent = requestsOf(AdFormat.NATIVE_SMALL)
        val resultsBefore = slotLoadResults()

        advanceBy(130_000)

        assertEquals("no display opportunity", eligibleBefore, eligibleCount())
        assertEquals("no request", sent, requestsOf(AdFormat.NATIVE_SMALL))
        assertEquals("no load result", resultsBefore, slotLoadResults())

        collapse()
        assertEquals("one new display opportunity", eligibleBefore + 1, eligibleCount())
    }

    /**
     * Two requests of the slot never overlap: collapsed while the load it waited on before giving way is still in
     * flight, the slot waits for that very load (no new request), its placeholder shown, and shows its ad.
     */
    @Test
    fun collapseWhileOldRequestInFlight_slotGetsOldLoad() {
        start()
        show()
        assertEquals(1, requestsOf(AdFormat.NATIVE_SMALL))
        deliver(AdFormat.NATIVE_COLLAPSIBLE, LAYER_AD)
        assertTrue("the layer opens", layerShown())
        // Control: the slot gave way, its placeholder gone.
        assertEquals(0, placeholders())

        collapse()
        assertEquals("no new request", 1, requestsOf(AdFormat.NATIVE_SMALL))
        assertEquals("placeholder while it waits", 1, placeholders())

        val ad = deliver(AdFormat.NATIVE_SMALL, FIRST_AD)
        assertTrue(slotShows(FIRST_AD))
        assertEquals(0, ad.destroyCount)
        assertEquals("one load result for that load", 1, logged("Native ${placement.key} loaded"))
        assertEquals("then the next load of the stock", 2, requestsOf(AdFormat.NATIVE_SMALL))
    }

    /** ADR-0019: one line when the slot gives way, one when it comes back. */
    @Test
    fun yieldAndBack_logged() {
        start()
        show()
        deliver(AdFormat.NATIVE_COLLAPSIBLE, LAYER_AD)
        collapse()

        assertEquals(1, logged("Bottom bar ${placement.key}: slot yields to the collapsible layer"))
        assertEquals(1, logged("Bottom bar ${placement.key}: slot back after the collapsible layer"))
    }

    /** One test unit per format, so a request tells its format. */
    private object YieldUnitIds : AdUnitIds {
        fun unitFor(format: AdFormat): String = "unit-${format.code}"

        override fun of(placement: AdPlacement): String = of(placement, placement.format)

        override fun of(placement: AdPlacement, format: AdFormat): String = unitFor(format)
    }

    private companion object {
        const val STEP_MILLIS = 250L

        /** Past the 30 s after which the collapsed layer opens again (CL-03). */
        const val REEXPAND_MILLIS = 31_000L

        /** Long enough for both preload groups (S-22), shorter than the 10 s a load waits. */
        const val PRELOAD_MILLIS = 7_000L
        const val LAYER_AD = "Ad of the layer"
        const val FIRST_AD = "First ad of the slot"
        const val SLOT_AD = "Ad of the slot"
        const val PENDING_AD = "Ad kept by the small stock"
    }
}
