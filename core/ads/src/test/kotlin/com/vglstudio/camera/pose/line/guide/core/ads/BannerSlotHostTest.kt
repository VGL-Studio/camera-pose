package com.vglstudio.camera.pose.line.guide.core.ads

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.ads.AdPlatform.ADMOB
import com.vglstudio.camera.pose.line.guide.core.ads.AdPlatform.MAX
import com.vglstudio.camera.pose.line.guide.core.analytics.RecordingAdFunnel
import com.vglstudio.camera.pose.line.guide.core.analytics.RecordingAdMetrics
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The banner slot: frame, pause and resume, destroy, consent withdrawn and given again, placement off (B-01 to B-06). */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class BannerSlotHostTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val views = FakeBannerAdViews()
    private val visible = MutableStateFlow(true)

    /** A full-screen ad drawn over the app (KB-27, plan T-054). */
    private val covered = MutableStateFlow(false)
    private val retryResets = MutableStateFlow(0)

    /**
     * The reload interval of the slot (plan T-088): 0 (no reload) by default, as every test before T-088 of a banner
     * refreshed by its ad unit; a collapsible banner sets 30 s (time_reload_banner_collap, plan T-087).
     */
    private val reloadMillis = MutableStateFlow(0L)
    private val logger = RecordingAppLogger()

    private var gateOpen by mutableStateOf(true)
    private var enabled by mutableStateOf(true)
    private var unitId by mutableStateOf<String?>("u")
    private var shown by mutableStateOf(true)
    private var placementKey by mutableStateOf(KEY)
    private val availability = mutableListOf<Boolean>()

    // The size of the slot under test (plan T-062): a fixed banner by default, as every test before MREC.
    private var kind by mutableStateOf(BannerSizeKind.ANCHORED)
    private var fits by mutableStateOf(true)
    private var heightDp by mutableStateOf(HEIGHT_DP)

    /** A second slot of the same placement under [SLOT_2], drawn below the first one when [setHost] asks for it. */
    private val secondViews = FakeBannerAdViews()
    private var secondKind by mutableStateOf(BannerSizeKind.ANCHORED)

    /** Whether the second slot of [setHost] is in the composition (plan T-088: a frame built later). */
    private var secondShown by mutableStateOf(true)

    private val totalSends: Int get() = views.created.sumOf { it.sendCount }

    /** The views of the slots when a test builds frames of two platforms (plan T-071); null: [views], [secondViews]. */
    private var newViewOverride: (() -> BannerAdView)? = null
    private var secondNewViewOverride: (() -> BannerAdView)? = null

    private fun setHost(withSecond: Boolean = false) {
        composeRule.setContent {
            if (shown) {
                Column {
                    BannerSlotHost(
                        gateOpen = gateOpen,
                        enabled = enabled,
                        unitId = unitId,
                        heightDp = heightDp,
                        visible = visible,
                        covered = covered,
                        mayRequestNow = { gateOpen },
                        retryResets = retryResets,
                        reloadMillis = reloadMillis,
                        logger = logger,
                        placementKey = placementKey,
                        newView = newViewOverride ?: views::newView,
                        modifier = Modifier.fillMaxWidth().testTag(SLOT),
                        onAdAvailabilityChange = { availability += it },
                        kind = kind,
                        fits = fits,
                    )
                    if (withSecond && secondShown) {
                        BannerSlotHost(
                            gateOpen = gateOpen,
                            enabled = enabled,
                            unitId = unitId,
                            heightDp = heightDp,
                            visible = visible,
                            covered = covered,
                            mayRequestNow = { gateOpen },
                            retryResets = retryResets,
                            reloadMillis = reloadMillis,
                            logger = logger,
                            placementKey = placementKey,
                            newView = secondNewViewOverride ?: secondViews::newView,
                            modifier = Modifier.fillMaxWidth().testTag(SLOT_2),
                            kind = secondKind,
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    /** The fake ads drawn inside the slot tagged [slot]. */
    private fun adsIn(slot: String): SemanticsNodeInteractionCollection =
        composeRule.onAllNodes(hasTestTag(FAKE_BANNER_TAG) and hasAnyAncestor(hasTestTag(slot)), useUnmergedTree = true)

    private fun assertSlotHeight(slot: String, heightDp: Int) {
        composeRule.onNodeWithTag(slot).assertHeightIsEqualTo(heightDp.dp)
    }

    private fun assertSlotHeight(heightDp: Int) {
        assertSlotHeight(SLOT, heightDp)
    }

    private fun assertAdShown(shown: Boolean) {
        composeRule.onAllNodesWithTag(FAKE_BANNER_TAG, useUnmergedTree = true).assertCountEquals(if (shown) 1 else 0)
    }

    private fun update(change: () -> Unit) {
        composeRule.runOnIdle(change)
        composeRule.waitForIdle()
    }

    /** [setHost] with the clock of Compose stopped: the test moves it ([advanceTo]). */
    private fun setTimedHost(withSecond: Boolean = false) {
        composeRule.mainClock.autoAdvance = false
        setHost(withSecond)
        frame()
    }

    private fun frame() {
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
    }

    private fun advanceTo(timeMillis: Long) {
        val step = timeMillis - composeRule.mainClock.currentTime
        if (step > 0) composeRule.mainClock.advanceTimeBy(step, ignoreFrameDuration = true)
        composeRule.waitForIdle()
    }

    /** Changes a value with the clock stopped, then lets one frame run. */
    private fun timedUpdate(change: () -> Unit) {
        composeRule.runOnIdle(change)
        frame()
    }

    @Test
    fun otherPlacementSameUnit_newView() {
        setHost()
        assertEquals(1, views.createdCount)
        val first = views.last
        // Control: a change that keeps the placement keeps the view.
        update { retryResets.value += 1 }
        assertEquals(1, views.createdCount)

        update { placementKey = "other_banner" }
        assertEquals(2, views.createdCount)
        assertEquals(1, first.destroyCount)
        val second = views.last

        // Plan T-088, section 3.5: the same placement drawn as another layer (its format is in the key) is another banner.
        update { placementKey = "other_banner/banner_collapsible" }
        assertEquals(3, views.createdCount)
        assertEquals(1, second.destroyCount)
        val third = views.last

        // The other half of the key: another ad unit.
        update { unitId = "other-unit" }
        assertEquals(4, views.createdCount)
        assertEquals(1, third.destroyCount)
    }

    @Test
    fun hiddenScreen_pausesView_visibleAgain_resumes() {
        setHost()
        assertEquals(1, views.createdCount)
        val view = views.last
        // Control: built while visible, the view is neither paused nor resumed.
        assertEquals(0, view.pauseCount)
        assertEquals(0, view.resumeCount)
        assertEquals(1, view.sendCount)

        update { visible.value = false }
        assertEquals(1, view.pauseCount)
        assertEquals(0, view.resumeCount)

        update { visible.value = true }
        assertEquals(1, view.pauseCount)
        assertEquals(1, view.resumeCount)
    }

    @Test
    fun resumedFlow_followsActivityPauseAndResume() = runTest {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val values = mutableListOf<Boolean>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            controller.get().lifecycle.resumedFlow().collect { values += it }
        }

        controller.pause()
        controller.resume()
        job.cancel()

        assertEquals(listOf(true, false, true), values)
    }

    @Test
    fun revokedAfterAd_keepsHeight_hidesAd_pauses_reloadsWhenGranted() {
        setHost()
        assertEquals(1, views.createdCount)
        update { views.last.emitLoaded() }
        val view = views.last
        // Control: the slot shows the ad at its height after one send.
        assertSlotHeight(HEIGHT_DP)
        assertAdShown(true)
        assertEquals(1, view.sendCount)

        update { gateOpen = false }
        assertSlotHeight(HEIGHT_DP)
        assertAdShown(false)
        assertEquals(1, view.pauseCount)
        assertEquals(0, view.destroyCount)
        assertEquals(1, view.sendCount)

        update { gateOpen = true }
        assertAdShown(true)
        assertEquals(1, view.resumeCount)
        assertEquals(2, view.sendCount)
        assertEquals(1, views.createdCount)
        assertSame(view, views.last)
    }

    @Test
    fun gateClosed_noFrame_noViewCreated() {
        gateOpen = false
        setHost()
        assertSlotHeight(0)
        assertEquals(0, views.createdCount)

        // Control: the same slot with the gate open builds its frame and sends.
        update { gateOpen = true }
        assertSlotHeight(HEIGHT_DP)
        assertEquals(1, views.createdCount)
        assertEquals(1, views.last.sendCount)
    }

    @Test
    fun revokedBeforeAnyAd_givesSpaceBack_andDestroys() {
        setHost()
        // Control: the frame is kept while the first load runs.
        assertSlotHeight(HEIGHT_DP)
        assertEquals(1, views.createdCount)

        update { gateOpen = false }
        assertSlotHeight(0)
        assertEquals(1, views.last.destroyCount)
    }

    @Test
    fun blankUnit_noFrame_nothingSent() {
        unitId = ""
        setHost()
        assertSlotHeight(0)
        assertEquals(0, views.createdCount)

        update { unitId = "  " }
        assertSlotHeight(0)
        assertEquals(0, views.createdCount)

        // Control: a real unit id builds the frame.
        update { unitId = "u" }
        assertSlotHeight(HEIGHT_DP)
        assertEquals(1, views.createdCount)
    }

    @Test
    fun leavingComposition_destroysView() {
        setHost()
        assertEquals(1, views.createdCount)
        assertEquals(0, views.last.destroyCount)

        update { shown = false }

        assertEquals(1, views.last.destroyCount)
    }

    @Test
    fun placementSwitchedOffMidway_givesSpaceBack_destroysOnce_sendsNoMore() {
        setHost()
        assertEquals(1, views.createdCount)
        update { views.last.emitLoaded() }
        val first = views.last
        // Control: the slot shows the ad at its height.
        assertSlotHeight(HEIGHT_DP)
        assertAdShown(true)

        update { enabled = false }
        assertSlotHeight(0)
        assertEquals(1, first.destroyCount)
        update { retryResets.value += 1 }
        assertEquals(1, first.sendCount)
        assertEquals(1, first.destroyCount)

        update { enabled = true }
        assertEquals(2, views.createdCount)
        assertEquals(1, views.last.sendCount)
        assertEquals(2, totalSends)
        assertSlotHeight(HEIGHT_DP)
    }

    @Test
    fun availability_trueOnFirstLoad_falseWhenFrameGone_notOnKeep() {
        setHost()
        // Nothing while the first load runs.
        assertEquals(emptyList<Boolean>(), availability)

        update { views.last.emitLoaded() }
        update { views.last.emitLoaded() }
        assertEquals("true once, not on each refresh of the SDK", listOf(true), availability)

        // B-06: consent withdrawn after an ad keeps the frame, so the bar keeps its space: no false.
        update { gateOpen = false }
        assertSlotHeight(HEIGHT_DP)
        assertEquals(listOf(true), availability)
        update { gateOpen = true }
        update { views.last.emitLoaded() }
        assertEquals(listOf(true), availability)

        // The placement switched off: the frame goes, false is reported once.
        update { enabled = false }
        assertSlotHeight(0)
        assertEquals(listOf(true, false), availability)

        // On again: a new frame, true again on its first ad.
        update { enabled = true }
        update { views.last.emitLoaded() }
        assertEquals(listOf(true, false, true), availability)
    }

    @Test
    fun coveredAtStart_sendsNothing_untilUncovered() {
        covered.value = true
        setTimedHost()
        val start = composeRule.mainClock.currentTime
        assertEquals(1, views.createdCount)
        val view = views.last

        advanceTo(start + 60_000)
        assertEquals(0, view.sendCount)
        assertEquals(1, view.pauseCount)
        assertEquals(0, view.resumeCount)

        // Control (L-055): the cover goes, the first request is sent once.
        timedUpdate { covered.value = false }
        assertEquals(1, view.sendCount)
        assertEquals(1, view.resumeCount)
    }

    @Test
    fun retryDueWhileCovered_sendsOnceWhenUncovered() {
        setTimedHost()
        val start = composeRule.mainClock.currentTime
        val view = views.last
        assertEquals(1, view.sendCount)
        // The first load fails: a retry is due 2 s later (B-02).
        timedUpdate { view.emitFailed(3) }

        advanceTo(start + 1_000)
        timedUpdate { covered.value = true }
        advanceTo(start + 30_000)
        assertEquals(1, view.sendCount)

        timedUpdate { covered.value = false }
        assertEquals(2, view.sendCount)
        advanceTo(start + 31_000)
        assertEquals(2, view.sendCount)
    }

    @Test
    fun manualReloadDueWhileCovered_sendsOnceWhenUncovered() {
        reloadMillis.value = 30_000
        setTimedHost()
        val start = composeRule.mainClock.currentTime
        val view = views.last
        assertEquals(1, view.sendCount)
        // Loaded: the reload by hand is due 30 s later (B-03).
        timedUpdate { view.emitLoaded() }

        advanceTo(start + 10_000)
        timedUpdate { covered.value = true }
        // Three reloads of 30 s would have been due by now.
        advanceTo(start + 100_000)
        assertEquals(1, view.sendCount)

        // One send when the cover goes, not three.
        timedUpdate { covered.value = false }
        assertEquals(2, view.sendCount)
        advanceTo(start + 101_000)
        assertEquals(2, view.sendCount)
    }

    @Test
    fun collapsible_reloadMillisChangedOnScreen_reachesLoop() {
        reloadMillis.value = 30_000
        setTimedHost()
        val start = composeRule.mainClock.currentTime
        val view = views.last
        assertEquals(1, view.sendCount)
        timedUpdate { view.emitLoaded() }
        // Control (L-055): the first reload by hand is 30 s after the load.
        advanceTo(start + 29_000)
        assertEquals(1, view.sendCount)
        advanceTo(start + 30_000)
        assertEquals(2, view.sendCount)

        // The key changes on screen after the first reload: the loop of the frame reads it from its next count.
        timedUpdate { reloadMillis.value = 90_000 }
        val reloaded = composeRule.mainClock.currentTime
        timedUpdate { view.emitLoaded() }
        advanceTo(reloaded + 60_000)
        assertEquals(2, view.sendCount)
        advanceTo(reloaded + 89_000)
        assertEquals(2, view.sendCount)
        advanceTo(reloaded + 91_000)
        assertEquals(3, view.sendCount)
    }

    @Test
    fun covered_pausesView_keepsAd_uncovered_resumes() {
        setHost()
        update { views.last.emitLoaded() }
        val view = views.last
        // Control: built while visible and not covered, the view runs and shows its ad.
        assertEquals(0, view.pauseCount)
        assertAdShown(true)

        update { covered.value = true }
        assertEquals(1, view.pauseCount)
        assertEquals(0, view.destroyCount)
        assertSlotHeight(HEIGHT_DP)
        assertAdShown(true)
        assertEquals(1, views.createdCount)

        update { covered.value = false }
        assertEquals(1, view.resumeCount)
        assertEquals(0, view.destroyCount)
        assertEquals(1, view.sendCount)
    }

    @Test
    fun mrec_loading_keeps250_width300_centered() {
        kind = BannerSizeKind.MEDIUM_RECTANGLE
        heightDp = MREC_HEIGHT_DP
        // Control: a fixed banner of the same placement below it, in the same width.
        setHost(withSecond = true)

        // Before any result: the frame keeps 250 dp, the ad is 300 dp wide, centred in the 411 dp slot.
        assertSlotHeight(MREC_HEIGHT_DP)
        assertEquals(1, views.last.sendCount)
        val ad = adsIn(SLOT).also { it.assertCountEquals(1) }[0]
        ad.assertWidthIsEqualTo(300.dp)
        val left = ad.getBoundsInRoot().left
        assertEquals((411f - 300f) / 2f, left.value, 1f)
        adsIn(SLOT_2)[0].assertWidthIsEqualTo(411.dp)
    }

    @Test
    fun mrec_failedBeforeAd_collapsesTo0_retries_loadedBackTo250_laterFailureKeeps250() {
        kind = BannerSizeKind.MEDIUM_RECTANGLE
        heightDp = MREC_HEIGHT_DP
        setTimedHost(withSecond = true)
        val start = composeRule.mainClock.currentTime
        val view = views.last

        timedUpdate {
            view.emitFailed(3)
            secondViews.last.emitFailed(3)
        }
        assertSlotHeight(0)
        assertEquals(0, view.destroyCount)
        // Control (L-055): a fixed banner that fails keeps its height.
        assertSlotHeight(SLOT_2, MREC_HEIGHT_DP)

        // B-02: the view lives on in the collapsed frame and retries 2 s later.
        advanceTo(start + 2_000)
        assertEquals(2, view.sendCount)

        timedUpdate { view.emitLoaded() }
        assertSlotHeight(MREC_HEIGHT_DP)
        adsIn(SLOT).assertCountEquals(1)

        // A failure after an ad keeps the frame: the view still shows the ad it had.
        timedUpdate { view.emitFailed(3) }
        assertSlotHeight(MREC_HEIGHT_DP)
        adsIn(SLOT).assertCountEquals(1)
    }

    @Test
    fun mrec_revokedAfterAd_keeps250_reloadsWhenGranted() {
        kind = BannerSizeKind.MEDIUM_RECTANGLE
        heightDp = MREC_HEIGHT_DP
        setHost()
        update { views.last.emitLoaded() }
        val view = views.last
        assertSlotHeight(MREC_HEIGHT_DP)
        assertAdShown(true)

        update { gateOpen = false }
        assertSlotHeight(MREC_HEIGHT_DP)
        assertAdShown(false)
        assertEquals(1, view.pauseCount)
        assertEquals(0, view.destroyCount)

        update { gateOpen = true }
        assertAdShown(true)
        assertEquals(2, view.sendCount)
        assertEquals(1, views.createdCount)
        assertSame(view, views.last)
    }

    @Test
    fun mrec_gateClosedOrOffOrBlank_noSpace_noView() {
        kind = BannerSizeKind.MEDIUM_RECTANGLE
        heightDp = MREC_HEIGHT_DP
        gateOpen = false
        setHost()
        assertSlotHeight(0)
        assertEquals(0, views.createdCount)

        update {
            gateOpen = true
            enabled = false
        }
        assertSlotHeight(0)
        assertEquals(0, views.createdCount)

        update {
            enabled = true
            unitId = " "
        }
        assertSlotHeight(0)
        assertEquals(0, views.createdCount)

        // Control: gate open, placement on, a unit: the 250 dp frame and one send.
        update { unitId = "u" }
        assertSlotHeight(MREC_HEIGHT_DP)
        assertEquals(1, views.createdCount)
        assertEquals(1, views.last.sendCount)
    }

    @Test
    fun mrec_failedThenRevoked_givesSpaceBack_freshFrameAt250() {
        kind = BannerSizeKind.MEDIUM_RECTANGLE
        heightDp = MREC_HEIGHT_DP
        setTimedHost()
        val first = views.last
        timedUpdate { first.emitFailed(3) }
        assertSlotHeight(0)

        timedUpdate { gateOpen = false }
        // With the clock stopped, a value written from the test is composed one frame later.
        frame()
        assertSlotHeight(0)
        assertEquals(1, first.destroyCount)

        // A new frame starts with no failure: 250 dp while its first load runs.
        timedUpdate { gateOpen = true }
        frame()
        assertEquals(2, views.createdCount)
        assertEquals(1, views.last.sendCount)
        assertSlotHeight(MREC_HEIGHT_DP)
    }

    @Test
    fun mrec_twoSlotsSamePlacement_collapseApart() {
        kind = BannerSizeKind.MEDIUM_RECTANGLE
        secondKind = BannerSizeKind.MEDIUM_RECTANGLE
        heightDp = MREC_HEIGHT_DP
        setTimedHost(withSecond = true)
        // Control: both frames keep 250 dp while their first loads run.
        assertSlotHeight(MREC_HEIGHT_DP)
        assertSlotHeight(SLOT_2, MREC_HEIGHT_DP)

        timedUpdate { views.last.emitFailed(3) }
        assertSlotHeight(0)
        assertSlotHeight(SLOT_2, MREC_HEIGHT_DP)

        timedUpdate { secondViews.last.emitLoaded() }
        assertSlotHeight(0)
        assertSlotHeight(SLOT_2, MREC_HEIGHT_DP)
    }

    @Test
    fun mrec_doesNotFit_noFrame_noView_noSend() {
        kind = BannerSizeKind.MEDIUM_RECTANGLE
        heightDp = MREC_HEIGHT_DP
        unitId = UNIT
        fits = false
        setHost()
        assertSlotHeight(0)
        assertEquals(0, views.createdCount)
        assertEquals(0, totalSends)
        update { visible.value = false }
        update { visible.value = true }
        val narrow = logger.entries.filter { "too narrow" in it.message }
        assertEquals(1, narrow.size)
        assertEquals("Banner $KEY too narrow", narrow.single().message)
        assertTrue(logger.entries.none { UNIT in it.message })

        // Control: the same slot where 300 dp fit builds its frame and sends.
        update { fits = true }
        assertSlotHeight(MREC_HEIGHT_DP)
        assertEquals(1, views.createdCount)
        assertEquals(1, views.last.sendCount)
    }

    @Test
    fun mrec_hiddenScreen_pauses_noRetryUntilVisible() {
        kind = BannerSizeKind.MEDIUM_RECTANGLE
        heightDp = MREC_HEIGHT_DP
        setTimedHost()
        val start = composeRule.mainClock.currentTime
        val view = views.last
        assertEquals(1, view.sendCount)

        timedUpdate { visible.value = false }
        assertEquals(1, view.pauseCount)
        timedUpdate { view.emitFailed(3) }
        advanceTo(start + 10_000)
        assertEquals(1, view.sendCount)

        // B-05: the retry due meanwhile is sent once when the screen shows again.
        timedUpdate { visible.value = true }
        assertEquals(1, view.resumeCount)
        assertEquals(2, view.sendCount)
    }

    @Test
    fun mrec_covered_pausesView_keepsAd_noRetry_uncoveredResumes() {
        kind = BannerSizeKind.MEDIUM_RECTANGLE
        heightDp = MREC_HEIGHT_DP
        setTimedHost()
        val view = views.last
        timedUpdate { view.emitLoaded() }
        assertAdShown(true)

        timedUpdate { covered.value = true }
        assertEquals(1, view.pauseCount)
        assertEquals(0, view.destroyCount)
        assertSlotHeight(MREC_HEIGHT_DP)
        assertAdShown(true)

        timedUpdate { covered.value = false }
        assertEquals(1, view.resumeCount)

        // Control: not covered, a failure is retried 2 s later.
        val controlStart = composeRule.mainClock.currentTime
        timedUpdate { view.emitFailed(3) }
        advanceTo(controlStart + 2_000)
        assertEquals(2, view.sendCount)

        // Covered while a retry is due: nothing is sent until the cover goes, then once.
        val start = composeRule.mainClock.currentTime
        timedUpdate { view.emitFailed(3) }
        advanceTo(start + 1_000)
        timedUpdate { covered.value = true }
        advanceTo(start + 30_000)
        assertEquals(2, view.sendCount)

        timedUpdate { covered.value = false }
        assertEquals(3, view.sendCount)
        advanceTo(start + 31_000)
        assertEquals(3, view.sendCount)
    }

    // Frames of two platforms (plan T-071, KB-31): an OrderedBannerAdView of fakes, or of the MAX wrapper on a fake SDK.

    private val maxFakes = mutableListOf<FakeBannerAdView>()
    private val admobFakes = mutableListOf<FakeBannerAdView>()
    private val secondMaxFakes = mutableListOf<FakeBannerAdView>()
    private val secondAdmobFakes = mutableListOf<FakeBannerAdView>()
    private val sdkViews = mutableListOf<FakeMaxBannerSdkView>()
    private val secondSdkViews = mutableListOf<FakeMaxBannerSdkView>()
    private var maxReady = true

    private val telemetry = AdTelemetry(
        metrics = RecordingAdMetrics(),
        funnel = RecordingAdFunnel(),
        lifecycleSwitches = { DEFAULT_FLAGS },
        serving = { true },
        offline = { false },
        counter = LoadAttemptCounter(),
        logger = logger,
    )

    /** Frames of fakes tagged [MAX_TAG] and [ADMOB_TAG], asked in [order]. */
    private fun ordered(
        order: List<AdPlatform> = listOf(MAX, ADMOB),
        maxs: MutableList<FakeBannerAdView> = maxFakes,
        admobs: MutableList<FakeBannerAdView> = admobFakes,
    ): () -> BannerAdView = {
        val max = FakeBannerAdView(tag = MAX_TAG).also { maxs += it }
        val admob = FakeBannerAdView(tag = ADMOB_TAG).also { admobs += it }
        OrderedBannerAdView(order.map { it to if (it == MAX) max else admob }, { maxReady }, logger, KEY)
    }

    /** Frames of [MAX, ADMOB] whose MAX is the real wrapper on a fake SDK view, kept in [sdks] once built. */
    private fun withMaxSdk(
        sdks: MutableList<FakeMaxBannerSdkView> = sdkViews,
        admobs: MutableList<FakeBannerAdView> = admobFakes,
        ready: () -> Boolean = { maxReady },
    ): () -> BannerAdView = {
        val admob = FakeBannerAdView(tag = ADMOB_TAG).also { admobs += it }
        val max = MaxBannerAdView(
            newSdkView = { FakeMaxBannerSdkView().also { sdks += it } },
            placement = AdPlacement.GALLERY_BANNER,
            format = AdFormat.BANNER,
            telemetry = telemetry,
            now = { 0L },
            logger = logger,
        )
        OrderedBannerAdView(listOf(MAX to max, ADMOB to admob), ready, logger, KEY)
    }

    private fun assertTagCount(tag: String, count: Int) {
        composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).assertCountEquals(count)
    }

    @Test
    fun mrec_twoPlatforms_firstFails_keeps250_bothFail_collapses() {
        kind = BannerSizeKind.MEDIUM_RECTANGLE
        heightDp = MREC_HEIGHT_DP
        newViewOverride = ordered()
        setTimedHost()
        val max = maxFakes.single()
        val admob = admobFakes.single()
        assertEquals(1, max.sendCount)

        timedUpdate { max.emitFailed(204) }
        assertSlotHeight(MREC_HEIGHT_DP)
        assertEquals(1, admob.sendCount)

        val failedAt = composeRule.mainClock.currentTime
        timedUpdate { admob.emitFailed(3) }
        assertSlotHeight(0)

        // B-02: the retry asks MAX first again; AdMob has an ad this time.
        advanceTo(failedAt + 2_000)
        assertEquals(2, max.sendCount)
        timedUpdate { max.emitFailed(204) }
        assertEquals(2, admob.sendCount)
        timedUpdate { admob.emitLoaded() }
        assertSlotHeight(MREC_HEIGHT_DP)
        assertTagCount(ADMOB_TAG, 1)
    }

    @Test
    fun banner_twoPlatforms_firstFails_heightUnchanged_secondShown() {
        newViewOverride = ordered()
        setHost()
        val max = maxFakes.single()
        val admob = admobFakes.single()
        assertSlotHeight(HEIGHT_DP)

        update { max.emitFailed(204) }
        assertSlotHeight(HEIGHT_DP)
        assertEquals(1, admob.sendCount)

        update { admob.emitLoaded() }
        assertSlotHeight(HEIGHT_DP)
        assertTagCount(ADMOB_TAG, 1)
        assertTagCount(MAX_TAG, 0)
    }

    @Test
    fun unitKeyChangesMidway_destroysBothChildren_newFrameAsksFirst() {
        newViewOverride = ordered()
        setHost()
        update { maxFakes.single().emitLoaded() }
        assertTagCount(MAX_TAG, 1)

        // ads_mediation_order changes to admob_max: the key of the frame changes (L-061).
        update {
            newViewOverride = ordered(listOf(ADMOB, MAX))
            unitId = "admob_max"
        }
        assertEquals(1, maxFakes[0].destroyCount)
        assertEquals(1, admobFakes[0].destroyCount)
        assertEquals(1, maxFakes[0].sendCount)
        assertEquals(0, admobFakes[0].sendCount)
        assertEquals(2, admobFakes.size)
        assertEquals(1, admobFakes[1].sendCount)
        assertEquals(0, maxFakes[1].sendCount)
    }

    @Test
    fun covered_maxShown_noRefresh_uncovered_noAutoRefresh() {
        newViewOverride = withMaxSdk()
        setHost()
        val sdk = sdkViews.single()
        assertEquals(1, sdk.loadCount)
        update { sdk.emitLoaded() }
        val stops = sdk.stopCount

        update { covered.value = true }
        assertEquals(stops + 1, sdk.stopCount)
        assertEquals(1, sdk.loadCount)

        // Plan T-088, section 3.5: the cover goes, MAX does not start refreshing by itself.
        update { covered.value = false }
        assertEquals(0, sdk.startCount)
        assertEquals(1, sdk.loadCount)
    }

    @Test
    fun maxShown_revoked_keepsHeight_hides_reloadsFromFirstWhenGranted() {
        newViewOverride = withMaxSdk()
        setHost()
        val sdk = sdkViews.single()
        update { sdk.emitLoaded() }
        assertEquals(View.VISIBLE, sdk.view.visibility)
        val stops = sdk.stopCount

        update { gateOpen = false }
        assertSlotHeight(HEIGHT_DP)
        assertEquals(View.INVISIBLE, sdk.view.visibility)
        assertEquals(stops + 1, sdk.stopCount)
        assertEquals(1, sdk.loadCount)

        update { gateOpen = true }
        assertEquals(2, sdk.loadCount)
        assertEquals(0, admobFakes.single().sendCount)
        assertEquals(View.VISIBLE, sdk.view.visibility)
    }

    @Test
    fun hiddenScreen_maxFirst_noLoadUntilVisible() {
        visible.value = false
        newViewOverride = withMaxSdk()
        setHost()
        assertTrue(sdkViews.isEmpty())
        assertEquals(0, admobFakes.single().sendCount)

        update { visible.value = true }
        val sdk = sdkViews.single()
        assertEquals(1, sdk.loadCount)
        // Plan T-088, section 3.5: the new MAX view stops its auto refresh around its first load.
        assertEquals(2, sdk.stopCount)
        assertEquals(0, sdk.startCount)
    }

    @Test
    fun shownPlatformChangesAfterFirstAd_redraws() {
        newViewOverride = ordered()
        setTimedHost()
        val max = maxFakes.single()
        val admob = admobFakes.single()
        timedUpdate { max.emitFailed(204) }
        timedUpdate { admob.emitLoaded() }
        assertTagCount(ADMOB_TAG, 1)
        assertTagCount(MAX_TAG, 0)

        // A refresh of AdMob fails: the retry of B-02 asks MAX, which loads.
        val failedAt = composeRule.mainClock.currentTime
        timedUpdate { admob.emitFailed(3) }
        advanceTo(failedAt + 2_000)
        assertEquals(2, max.sendCount)
        timedUpdate { max.emitLoaded() }
        assertTagCount(MAX_TAG, 1)
        assertTagCount(ADMOB_TAG, 0)
        assertSlotHeight(HEIGHT_DP)
    }

    @Test
    fun maxNotReady_noMaxViewBuilt_admobShown_readyLater_buildsOnNextMaxTurn() {
        maxReady = false
        newViewOverride = withMaxSdk()
        // Control: a second slot whose MAX is ready from the start.
        secondNewViewOverride = withMaxSdk(secondSdkViews, secondAdmobFakes) { true }
        setTimedHost(withSecond = true)
        val admob = admobFakes.single()
        assertEquals(1, admob.sendCount)
        assertTrue(sdkViews.isEmpty())
        assertEquals(1, secondSdkViews.size)
        assertEquals(1, secondSdkViews.single().loadCount)
        assertEquals(0, secondAdmobFakes.single().sendCount)

        timedUpdate { admob.emitLoaded() }
        assertTrue(sdkViews.isEmpty())

        maxReady = true
        val failedAt = composeRule.mainClock.currentTime
        timedUpdate { admob.emitFailed(3) }
        advanceTo(failedAt + 2_000)
        assertEquals(1, sdkViews.size)
        assertEquals(1, sdkViews.single().loadCount)
    }

    @Test
    fun mrec_admobOnly_pausedThenFails_collapses() {
        kind = BannerSizeKind.MEDIUM_RECTANGLE
        secondKind = BannerSizeKind.MEDIUM_RECTANGLE
        heightDp = MREC_HEIGHT_DP
        newViewOverride = ordered(listOf(ADMOB))
        // Control: a second MREC of [MAX, ADMOB].
        secondNewViewOverride = ordered(listOf(MAX, ADMOB), secondMaxFakes, secondAdmobFakes)
        setTimedHost(withSecond = true)
        assertEquals(1, admobFakes.single().sendCount)
        assertEquals(1, secondMaxFakes.single().sendCount)

        timedUpdate { covered.value = true }
        timedUpdate {
            admobFakes.single().emitFailed(3)
            secondMaxFakes.single().emitFailed(204)
        }
        assertSlotHeight(0)
        assertSlotHeight(SLOT_2, MREC_HEIGHT_DP)
    }

    @Test
    fun shorterPlatform_drawnAtFrameTop_atItsOwnHeight() {
        // AdMob 50 dp, MAX 62 dp: the frame is 62 dp and the ad of AdMob sits at its top (question 4 of the plan, B-09).
        newViewOverride = {
            val admob = FakeBannerAdView(tag = ADMOB_TAG).also { admobFakes += it }
            val max = FakeBannerAdView(tag = MAX_TAG).also { maxFakes += it }
            OrderedBannerAdView(
                children = listOf(ADMOB to admob, MAX to max),
                maxReady = { maxReady },
                logger = logger,
                placementKey = KEY,
                heightsDp = mapOf(ADMOB to ADMOB_HEIGHT_DP, MAX to HEIGHT_DP),
            )
        }
        setHost()
        update { admobFakes.single().emitLoaded() }
        assertSlotHeight(HEIGHT_DP)
        val slot = composeRule.onNodeWithTag(SLOT).getBoundsInRoot()
        val ad = composeRule.onNodeWithTag(ADMOB_TAG, useUnmergedTree = true).getBoundsInRoot()
        assertEquals(slot.top, ad.top)
        assertEquals(ADMOB_HEIGHT_DP.dp, ad.bottom - ad.top)
        assertEquals(slot.left, ad.left)
        assertEquals(slot.right, ad.right)
    }

    // Reloads by hand of the fixed banner and the MREC (plan T-088, KB-34, KB-18, KB-29).

    @Test
    fun mrec_reloadsAtMrecKey() {
        kind = BannerSizeKind.MEDIUM_RECTANGLE
        heightDp = MREC_HEIGHT_DP
        reloadMillis.value = 90_000
        setTimedHost()
        val start = composeRule.mainClock.currentTime
        val view = views.last
        assertEquals(1, view.sendCount)
        timedUpdate { view.emitLoaded() }

        advanceTo(start + 89_000)
        assertEquals(1, view.sendCount)
        advanceTo(start + 91_000)
        assertEquals(2, view.sendCount)
        // The frame keeps 250 dp and the ad it had while the reload runs.
        assertSlotHeight(MREC_HEIGHT_DP)
        adsIn(SLOT).assertCountEquals(1)
    }

    @Test
    fun fixed_keyChangedOnScreen_nextReloadUsesNew() {
        reloadMillis.value = 60_000
        setTimedHost()
        val start = composeRule.mainClock.currentTime
        val view = views.last
        timedUpdate { view.emitLoaded() }

        advanceTo(start + 20_000)
        timedUpdate { reloadMillis.value = 120_000 }
        // The count under way keeps its 60 s.
        advanceTo(start + 59_000)
        assertEquals(1, view.sendCount)
        advanceTo(start + 61_000)
        assertEquals(2, view.sendCount)
        timedUpdate { view.emitLoaded() }
        val reloaded = composeRule.mainClock.currentTime

        // The next count reads 120 s: the reload at 180 s, not at 120 s.
        advanceTo(reloaded + 119_000)
        assertEquals(2, view.sendCount)
        advanceTo(reloaded + 121_000)
        assertEquals(3, view.sendCount)
    }

    @Test
    fun fixed_keyPositiveToZero_dropsCount_zeroToPositive_countsFromReceipt() {
        reloadMillis.value = 60_000
        setTimedHost()
        val start = composeRule.mainClock.currentTime
        val view = views.last
        timedUpdate { view.emitLoaded() }

        advanceTo(start + 30_000)
        timedUpdate { reloadMillis.value = 0 }
        advanceTo(start + 200_000)
        assertEquals(1, view.sendCount)

        timedUpdate { reloadMillis.value = 60_000 }
        val received = composeRule.mainClock.currentTime
        advanceTo(received + 59_000)
        assertEquals(1, view.sendCount)
        advanceTo(received + 61_000)
        assertEquals(2, view.sendCount)
    }

    @Test
    fun fixed_reloadDueWhileHidden_sentOnceWhenVisible() {
        reloadMillis.value = 60_000
        setTimedHost()
        val start = composeRule.mainClock.currentTime
        val view = views.last
        timedUpdate { view.emitLoaded() }

        advanceTo(start + 30_000)
        timedUpdate { visible.value = false }
        // The reload due at 60 s (and the ones after) is not sent while the app is in background (B-05).
        advanceTo(start + 200_000)
        assertEquals(1, view.sendCount)

        timedUpdate { visible.value = true }
        assertEquals(2, view.sendCount)
        advanceTo(start + 201_000)
        assertEquals(2, view.sendCount)
    }

    @Test
    fun fixed_reloadDueWhileCovered_sendsOnceWhenUncovered() {
        reloadMillis.value = 60_000
        assertReloadDueWhileCoveredSentOnce()
    }

    @Test
    fun mrec_reloadDueWhileCovered_sendsOnceWhenUncovered() {
        kind = BannerSizeKind.MEDIUM_RECTANGLE
        heightDp = MREC_HEIGHT_DP
        reloadMillis.value = 60_000
        assertReloadDueWhileCoveredSentOnce()
        assertSlotHeight(MREC_HEIGHT_DP)
    }

    /** Loaded, covered from 10 s to 200 s (three reloads of 60 s due): one send when the cover goes (B-07). */
    private fun assertReloadDueWhileCoveredSentOnce() {
        setTimedHost()
        val start = composeRule.mainClock.currentTime
        val view = views.last
        assertEquals(1, view.sendCount)
        timedUpdate { view.emitLoaded() }

        advanceTo(start + 10_000)
        timedUpdate { covered.value = true }
        advanceTo(start + 200_000)
        assertEquals(1, view.sendCount)

        timedUpdate { covered.value = false }
        assertEquals(2, view.sendCount)
        advanceTo(start + 201_000)
        assertEquals(2, view.sendCount)
    }

    /** Frames of MAX only (order `max`) whose MAX is the real wrapper on a fake SDK view, kept in [sdkViews]. */
    private fun maxOnly(): () -> BannerAdView = {
        val max = MaxBannerAdView(
            newSdkView = { FakeMaxBannerSdkView().also { sdkViews += it } },
            placement = AdPlacement.GALLERY_BANNER,
            format = AdFormat.BANNER,
            telemetry = telemetry,
            now = { 0L },
            logger = logger,
        )
        OrderedBannerAdView(listOf(MAX to max), { maxReady }, logger, KEY)
    }

    @Test
    fun maxShown_reloadsByHand_neverStartsAutoRefresh() {
        reloadMillis.value = 60_000
        newViewOverride = maxOnly()
        setTimedHost()
        val start = composeRule.mainClock.currentTime
        val sdk = sdkViews.single()
        assertEquals(1, sdk.loadCount)
        var loadedAt = 0L
        fun loadNow() {
            timedUpdate { sdk.emitLoaded() }
            loadedAt = composeRule.mainClock.currentTime
        }
        loadNow()

        // Reloads by hand at 60, 120 and 180 s, each answered at once.
        repeat(3) { round ->
            advanceTo(loadedAt + 59_000)
            assertEquals(round + 1, sdk.loadCount)
            advanceTo(loadedAt + 60_000)
            assertEquals(round + 2, sdk.loadCount)
            loadNow()
        }

        // Covered from 190 s to 250 s: the reload due at 240 s goes once, when the cover goes.
        advanceTo(start + 190_000)
        timedUpdate { covered.value = true }
        advanceTo(start + 250_000)
        assertEquals(4, sdk.loadCount)
        timedUpdate { covered.value = false }
        assertEquals(5, sdk.loadCount)
        loadNow()

        // Hidden from 260 s to 400 s: the reload due at 310 s goes once, when the screen shows again.
        advanceTo(start + 260_000)
        timedUpdate { visible.value = false }
        advanceTo(start + 400_000)
        assertEquals(5, sdk.loadCount)
        timedUpdate { visible.value = true }
        assertEquals(6, sdk.loadCount)
        advanceTo(start + 401_000)

        assertEquals(6, sdk.loadCount)
        assertEquals(0, sdk.startCount)
    }

    @Test
    fun covered_maxShown_dueReloadSentOnceWhenUncovered() {
        reloadMillis.value = 60_000
        newViewOverride = maxOnly()
        setTimedHost()
        val start = composeRule.mainClock.currentTime
        val sdk = sdkViews.single()
        timedUpdate { sdk.emitLoaded() }
        val stops = sdk.stopCount

        advanceTo(start + 10_000)
        timedUpdate { covered.value = true }
        assertEquals(stops + 1, sdk.stopCount)
        advanceTo(start + 200_000)
        assertEquals(1, sdk.loadCount)

        timedUpdate { covered.value = false }
        assertEquals(2, sdk.loadCount)
        advanceTo(start + 201_000)
        assertEquals(2, sdk.loadCount)
        assertEquals(0, sdk.startCount)
    }

    @Test
    fun twoFixedFramesSamePlacement_reloadsApart_countsApart() {
        reloadMillis.value = 60_000
        secondShown = false
        setTimedHost(withSecond = true)
        val start = composeRule.mainClock.currentTime
        val first = views.last
        timedUpdate { first.emitLoaded() }
        // Frame 1 answers its third send with a failure, every other one with an ad.
        first.onSend = { if (first.sendCount == 3) first.emitFailed(3) else first.emitLoaded() }

        advanceTo(start + 10_000)
        timedUpdate { secondShown = true }
        // With the clock stopped, a value written from the test is composed one frame later.
        frame()
        val second = secondViews.last
        assertEquals(1, second.sendCount)
        timedUpdate { second.emitLoaded() }
        second.onSend = { second.emitLoaded() }

        advanceTo(start + 59_000)
        assertEquals(1, first.sendCount)
        advanceTo(start + 61_000)
        assertEquals(2, first.sendCount)
        assertEquals(1, second.sendCount)
        advanceTo(start + 69_000)
        assertEquals(1, second.sendCount)
        advanceTo(start + 71_000)
        assertEquals(2, second.sendCount)

        // Frame 1 fails at 120 s and retries 2 s later (B-02); frame 2 keeps its own tick.
        advanceTo(start + 119_000)
        assertEquals(2, first.sendCount)
        advanceTo(start + 121_000)
        assertEquals(3, first.sendCount)
        advanceTo(start + 121_900)
        assertEquals(3, first.sendCount)
        advanceTo(start + 123_000)
        assertEquals(4, first.sendCount)
        assertEquals(2, second.sendCount)
        advanceTo(start + 129_000)
        assertEquals(2, second.sendCount)
        advanceTo(start + 131_000)
        assertEquals(3, second.sendCount)
        advanceTo(start + 181_000)
        assertEquals(4, first.sendCount)
        assertEquals(3, second.sendCount)
    }

    private companion object {
        const val SLOT = "banner_slot"
        const val SLOT_2 = "banner_slot_2"
        const val KEY = "test_banner"
        const val HEIGHT_DP = 62
        const val ADMOB_HEIGHT_DP = 50
        const val MREC_HEIGHT_DP = 250
        const val UNIT = "unit-of-the-mrec-slot"
        const val MAX_TAG = "max_banner"
        const val ADMOB_TAG = "admob_banner"

        val DEFAULT_FLAGS: AdsGlobalFlags = runBlocking { DefaultAdsFlags(FakeRemoteSettings()).global.first() }
    }
}
