package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Application
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.ads.AdView
import com.vglstudio.camera.pose.line.guide.core.analytics.AdFunnelEvent
import com.vglstudio.camera.pose.line.guide.core.analytics.AdFunnelKind
import com.vglstudio.camera.pose.line.guide.core.analytics.RecordingAdFunnel
import com.vglstudio.camera.pose.line.guide.core.analytics.RecordingAdMetrics
import com.vglstudio.camera.pose.line.guide.core.logging.LogEntry
import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * At most one anchored ad per screen (MC-12, MC-14, KB-35 criteria 5 and 6, plan T-091) through the production
 * wiring of [AdSlot] and [BottomAdBarScaffold] under a real [AdsController]: the first allowed one in the order of
 * the composition runs; a later one builds no ad view, sends no native request, takes no space, reports nothing and
 * logs one warning. Banners are counted as [AdView]s in the window; native requests on a fake loader.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class AnchoredAdWiringTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val logger = RecordingAppLogger()
    private val metrics = RecordingAdMetrics()
    private val funnel = RecordingAdFunnel()
    private val loader = FakeNativeAdLoader()
    private val banner = AdPlacement.GALLERY_BANNER
    private val collapsible = AdPlacement.GALLERY_BANNER_COLLAPSIBLE
    private val bar = AdPlacement.POSES_BOTTOM
    private val settings = FakeRemoteSettings().apply {
        set(AdsSettings.placementEnabled(banner), true)
        set(AdsSettings.placementEnabled(collapsible), true)
        set(AdsSettings.placementEnabled(bar), true)
        // The bar's slot is native (no ad view of its own), with no floating layer.
        set(AdsSettings.IS_SHOW_NATIVE_BOTTOM, true)
        set(AdsSettings.IS_SHOW_NATIVE_COLLAP, false)
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun controller(unitIds: AdUnitIds = GoogleTestAdUnitIds): AdsController = testAdsController(
        context = application,
        unitIds = unitIds,
        flags = DefaultAdsFlags(settings),
        logger = logger,
        metrics = metrics,
        funnel = funnel,
    ).also { it.nativeLoaderForTest = loader }

    /** Consent open, for the banners. */
    private fun bannerController(unitIds: AdUnitIds = GoogleTestAdUnitIds): AdsController =
        controller(unitIds).also { it.openConsentGateForTest() }

    /** Started, consent open, AdMob ready and in foreground, so the native slot of a bar sends (plan T-091, 4.1). */
    private fun barController(): AdsController = controller().also {
        it.start(application, AppActivityFilter { true })
        idle()
        it.openConsentGateForTest()
        it.admobReadyForTest()
        it.setAppForegroundForTest(true)
        idle()
    }

    private fun show(controller: AdsController, content: @Composable () -> Unit) {
        composeRule.setContent { CompositionLocalProvider(LocalAdsController provides controller) { content() } }
        settle()
    }

    private fun settle() {
        composeRule.waitForIdle()
        idle()
        composeRule.waitForIdle()
    }

    private fun adViews(view: View = composeRule.activity.window.decorView): List<AdView> = when (view) {
        is AdView -> listOf(view)
        is ViewGroup -> (0 until view.childCount).flatMap { adViews(view.getChildAt(it)) }
        else -> emptyList()
    }

    private fun height(tag: String): Dp = composeRule.onNodeWithTag(tag).getBoundsInRoot().height

    private fun fixedHeight(): Dp = bannerAdSize(composeRule.activity, AdFormat.BANNER, 411).height.dp

    private fun collapsibleHeight(): Dp = bannerAdSize(composeRule.activity, AdFormat.BANNER_COLLAPSIBLE, 411).height.dp

    private fun placeholders(): Int =
        composeRule.onAllNodesWithTag(NativeAdTestTags.PLACEHOLDER, useUnmergedTree = true).fetchSemanticsNodes().size

    private fun warnings(): List<LogEntry> = logger.entries.filter { it.level == LogLevel.WARN && it.tag == "Ads" }

    private fun turns(placement: AdPlacement): Int = logger.entries.count {
        it.level == LogLevel.INFO && it.tag == "Ads" && it.message == anchoredAdTurnLog(placement)
    }

    private fun eligible(): Int = funnel.events.count { it == AdFunnelEvent.Eligible(AdFunnelKind.NATIVE) }

    @Composable
    private fun Fixed() = AdSlot(banner, Modifier.fillMaxWidth().testTag(FIXED))

    @Composable
    private fun Collapsible() = AdSlot(collapsible, Modifier.fillMaxWidth().testTag(COLLAPSIBLE))

    private fun assertHeight(message: String, expected: Dp, actual: Dp) {
        assertEquals(message, expected.value, actual.value, TOLERANCE_DP)
    }

    @Test
    fun twoBanners_secondNoViewNoSpace_warns() {
        val controller = bannerController()
        var reversed by mutableStateOf(false)
        show(controller) {
            Column {
                if (reversed) {
                    Collapsible()
                    Fixed()
                } else {
                    Fixed()
                    Collapsible()
                }
            }
        }

        assertNotEquals(0.dp, fixedHeight())
        assertEquals("one ad view (two before plan T-091)", 1, adViews().size)
        assertHeight("fixed banner runs", fixedHeight(), height(FIXED))
        assertHeight("collapsible banner takes no space", 0.dp, height(COLLAPSIBLE))
        assertEquals(listOf(anchoredAdWaitLog(collapsible, banner)), warnings().map { it.message })

        // Control of the order: the collapsible banner first, it runs and the fixed one waits.
        reversed = true
        settle()
        assertEquals(1, adViews().size)
        assertHeight("collapsible banner runs", collapsibleHeight(), height(COLLAPSIBLE))
        assertHeight("fixed banner takes no space", 0.dp, height(FIXED))
    }

    @Test
    fun bar_thenBannerInContent_barRuns() {
        val controller = barController()
        show(controller) {
            BottomAdBarScaffold(bar) { Collapsible() }
        }

        assertTrue("the bar sends its native request", loader.requests.isNotEmpty())
        assertEquals("the bar shows its placeholder", 1, placeholders())
        assertEquals(0, adViews().size)
        assertHeight("banner takes no space", 0.dp, height(COLLAPSIBLE))
        assertEquals(listOf(anchoredAdWaitLog(collapsible, bar)), warnings().map { it.message })
    }

    @Test
    fun bannerAboveBar_bannerRuns() {
        val controller = barController()
        show(controller) {
            Column {
                Fixed()
                BottomAdBarScaffold(bar) {}
            }
        }

        assertEquals(1, adViews().size)
        assertHeight("banner runs", fixedHeight(), height(FIXED))
        assertTrue("the bar sends nothing: ${loader.requests.size}", loader.requests.isEmpty())
        assertEquals("no placeholder", 0, placeholders())
        assertEquals(listOf(anchoredAdWaitLog(bar, banner)), warnings().map { it.message })
    }

    @Test
    fun holderLeaves_waitingRuns() {
        val controller = bannerController()
        var showFixed by mutableStateOf(true)
        show(controller) {
            Column {
                if (showFixed) Fixed()
                Collapsible()
            }
        }
        assertEquals(1, adViews().size)
        assertHeight("collapsible waits", 0.dp, height(COLLAPSIBLE))

        showFixed = false
        settle()

        assertEquals(1, adViews().size)
        assertHeight("collapsible runs", collapsibleHeight(), height(COLLAPSIBLE))
        assertEquals(1, turns(collapsible))
    }

    @Test
    fun barLeaves_bannerRuns() {
        val controller = barController()
        var showBar by mutableStateOf(true)
        show(controller) {
            if (showBar) {
                BottomAdBarScaffold(bar) { Collapsible() }
            } else {
                Collapsible()
            }
        }
        assertEquals(0, adViews().size)

        showBar = false
        settle()

        assertEquals(1, adViews().size)
        assertHeight("banner runs", collapsibleHeight(), height(COLLAPSIBLE))
    }

    @Test
    fun barLeaves_waitingBannerStaysInTree_takesTurn() {
        val controller = barController()
        var showBar by mutableStateOf(true)
        show(controller) {
            Column {
                if (showBar) BottomAdBarScaffold(bar) {}
                Collapsible()
            }
        }
        assertEquals(0, adViews().size)
        assertHeight("banner waits", 0.dp, height(COLLAPSIBLE))
        assertEquals(0, turns(collapsible))

        showBar = false
        settle()

        assertEquals(1, adViews().size)
        assertHeight("waiting banner runs", collapsibleHeight(), height(COLLAPSIBLE))
        assertEquals(1, turns(collapsible))
    }

    @Test
    fun firstOff_secondRuns() {
        settings.set(AdsSettings.placementEnabled(banner), false)
        val controller = bannerController()
        show(controller) {
            Column {
                Fixed()
                Collapsible()
            }
        }

        assertEquals(1, adViews().size)
        assertHeight("collapsible runs", collapsibleHeight(), height(COLLAPSIBLE))
        assertTrue("no warning: ${warnings()}", warnings().isEmpty())
        assertEquals("the second one holds the turn", 1, turns(collapsible))
    }

    @Test
    fun firstNoUnit_secondRuns() {
        val noFixedUnit = object : AdUnitIds {
            override fun of(placement: AdPlacement): String? =
                if (placement == banner) null else GoogleTestAdUnitIds.of(placement)

            override fun of(placement: AdPlacement, format: AdFormat): String? =
                if (placement == banner) null else GoogleTestAdUnitIds.of(placement, format)
        }
        val controller = bannerController(noFixedUnit)
        show(controller) {
            Column {
                Fixed()
                Collapsible()
            }
        }

        assertEquals(1, adViews().size)
        assertHeight("fixed banner has no unit (B-04)", 0.dp, height(FIXED))
        assertHeight("collapsible runs", collapsibleHeight(), height(COLLAPSIBLE))
        assertTrue("no warning: ${warnings()}", warnings().isEmpty())
        assertEquals("the second one holds the turn", 1, turns(collapsible))
    }

    @Test
    fun barOff_bannerRuns() {
        settings.set(AdsSettings.placementEnabled(bar), false)
        val controller = barController()
        show(controller) {
            BottomAdBarScaffold(bar) { Collapsible() }
        }

        assertEquals(1, adViews().size)
        assertHeight("banner runs", collapsibleHeight(), height(COLLAPSIBLE))
        assertTrue("no warning: ${warnings()}", warnings().isEmpty())
        assertEquals("the banner holds the turn", 1, turns(collapsible))
    }

    @Test
    fun holderTurnedOff_staysHolder_waitingStillWaits() {
        val controller = bannerController()
        show(controller) {
            Column {
                Fixed()
                Collapsible()
            }
        }
        // Control: the fixed banner runs before the switch.
        assertEquals(1, adViews().size)
        assertHeight("fixed runs", fixedHeight(), height(FIXED))

        settings.set(AdsSettings.placementEnabled(banner), false)
        settle()

        assertEquals("the fixed banner stops, the collapsible one still waits", 0, adViews().size)
        assertHeight("collapsible still takes no space", 0.dp, height(COLLAPSIBLE))
    }

    @Test
    fun firstTurnedOnLater_noTurnChange() {
        settings.set(AdsSettings.placementEnabled(banner), false)
        val controller = bannerController()
        var shown by mutableStateOf(true)
        show(controller) {
            if (shown) {
                Column {
                    Fixed()
                    Collapsible()
                }
            }
        }
        val collapsibleView = adViews().single()
        assertHeight("collapsible runs", collapsibleHeight(), height(COLLAPSIBLE))

        settings.set(AdsSettings.placementEnabled(banner), true)
        settle()

        assertHeight("fixed banner still takes no space", 0.dp, height(FIXED))
        assertSame("no new ad view", collapsibleView, adViews().single())

        // The next entry into the screen: the order counts again.
        shown = false
        settle()
        assertEquals(0, adViews().size)
        shown = true
        settle()
        assertEquals(1, adViews().size)
        assertHeight("fixed banner runs", fixedHeight(), height(FIXED))
        assertHeight("collapsible waits", 0.dp, height(COLLAPSIBLE))
    }

    @Test
    fun consentLater_firstInOrderRuns() {
        val controller = controller()
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CompositionLocalProvider(LocalAdsController provides controller) {
                Column {
                    Fixed()
                    Collapsible()
                }
            }
        }
        repeat(FRAMES) {
            composeRule.mainClock.advanceTimeByFrame()
            idle()
        }
        assertEquals("no ad view before consent", 0, adViews().size)

        controller.openConsentGateForTest()
        repeat(FRAMES) { frame ->
            idle()
            composeRule.mainClock.advanceTimeByFrame()
            idle()
            assertTrue("frame $frame has ${adViews().size} ad views", adViews().size <= 1)
            assertHeight("collapsible takes no space at frame $frame", 0.dp, height(COLLAPSIBLE))
        }

        assertEquals(1, adViews().size)
        assertHeight("fixed banner runs (question 1 of the plan)", fixedHeight(), height(FIXED))
    }

    @Test
    fun twoOwners_eachOwnTurn() {
        val controller = bannerController()
        val first = ResumedOwner()
        val second = ResumedOwner()
        var separate by mutableStateOf(false)
        show(controller) {
            Column {
                Box {
                    CompositionLocalProvider(LocalLifecycleOwner provides first) { Fixed() }
                }
                Box {
                    CompositionLocalProvider(LocalLifecycleOwner provides if (separate) second else first) {
                        Collapsible()
                    }
                }
            }
        }
        // Control: both under one owner are one screen, one ad view.
        assertEquals(1, adViews().size)
        logger.entries.size.let { before ->
            separate = true
            settle()
            assertTrue("no new warning", logger.entries.drop(before).none { it.level == LogLevel.WARN })
        }

        assertEquals(2, adViews().size)
    }

    @Test
    fun anchoredAdScreen_ownScope() {
        val controller = bannerController()
        var screens by mutableStateOf(false)
        show(controller) {
            Column {
                if (screens) {
                    AnchoredAdScreen { Fixed() }
                    AnchoredAdScreen { Collapsible() }
                } else {
                    Fixed()
                    Collapsible()
                }
            }
        }
        // Control: one screen without AnchoredAdScreen, one ad view.
        assertEquals(1, adViews().size)

        screens = true
        settle()

        assertEquals(2, adViews().size)
    }

    @Test
    fun waitingBar_noEligibleNoRequest() {
        val controller = barController()
        var showBanner by mutableStateOf(true)
        show(controller) {
            Column {
                if (showBanner) Fixed()
                BottomAdBarScaffold(bar) {}
            }
        }

        assertTrue("no native request: ${loader.requests.size}", loader.requests.isEmpty())
        assertEquals(0, eligible())

        // Control: the banner leaves, the bar takes the turn, sends and reports its display opportunity once.
        showBanner = false
        settle()
        assertEquals(1, loader.requests.size)
        composeRule.runOnIdle { loader.deliver(FakeLoadedNativeAd()) }
        settle()
        // The slot took the ad, and its stock loads the next one (O-01).
        assertEquals(2, loader.requests.size)
        assertEquals(1, eligible())
    }

    @Test
    fun switchInSameScope_noWaitLog() {
        val controller = bannerController()
        var screenB by mutableStateOf(false)
        show(controller) {
            if (screenB) Collapsible() else Fixed()
        }
        assertEquals(1, adViews().size)

        screenB = true
        settle()

        val viewOfB = adViews().single()
        assertHeight("B runs", collapsibleHeight(), height(COLLAPSIBLE))
        assertTrue("no warning: ${warnings()}", warnings().isEmpty())
        assertEquals("B enters the scope once", 1, turns(collapsible))
        repeat(2) {
            composeRule.mainClock.advanceTimeByFrame()
            idle()
        }
        assertSame("the ad view of B is kept", viewOfB, adViews().single())
        assertEquals(1, turns(collapsible))
    }

    @Test
    fun recreate_newEntry_firstRunsAgain() {
        val controller = bannerController()
        var collapsibleHeightPx = -1
        val content: ComponentActivity.() -> Unit = {
            setContent {
                CompositionLocalProvider(LocalAdsController provides controller) {
                    Column {
                        AdSlot(banner, Modifier.fillMaxWidth())
                        AdSlot(
                            collapsible,
                            Modifier.fillMaxWidth().onGloballyPositioned { collapsibleHeightPx = it.size.height },
                        )
                    }
                }
            }
        }
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            var oldActivity: ComponentActivity? = null
            scenario.onActivity {
                oldActivity = it
                it.content()
            }
            settle()
            scenario.onActivity { assertEquals(1, adViews(it.window.decorView).size) }

            scenario.recreate()
            var newActivity: ComponentActivity? = null
            scenario.onActivity {
                newActivity = it
                it.content()
            }
            settle()

            scenario.onActivity {
                assertEquals("the first one runs again", 1, adViews(it.window.decorView).size)
            }
            assertEquals("collapsible takes no space", 0, collapsibleHeightPx)
            val old = checkNotNull(oldActivity)
            assertTrue("a new activity", old !== newActivity)
            assertEquals(Lifecycle.State.DESTROYED, old.lifecycle.currentState)
            assertFalse("the destroyed owner left", controller.anchoredScopes.containsForTest(old))
            assertTrue(controller.anchoredScopes.containsForTest(checkNotNull(newActivity)))
            assertEquals(1, controller.anchoredScopes.sizeForTest)
        }
    }

    /** A lifecycle owner of its own, resumed, as `NavHost` gives each destination ([LocalLifecycleOwner]). */
    private class ResumedOwner : LifecycleOwner {
        private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
    }

    private companion object {
        const val FIXED = "fixed"
        const val COLLAPSIBLE = "collapsible"
        const val TOLERANCE_DP = 0.5f
        const val FRAMES = 8
    }
}
