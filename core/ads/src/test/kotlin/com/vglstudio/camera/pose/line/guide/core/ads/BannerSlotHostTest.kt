package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
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
    private val retryResets = MutableStateFlow(0)
    private val logger = RecordingAppLogger()

    private var gateOpen by mutableStateOf(true)
    private var enabled by mutableStateOf(true)
    private var unitId by mutableStateOf<String?>("u")
    private var shown by mutableStateOf(true)
    private var placementKey by mutableStateOf(KEY)
    private var refresh by mutableStateOf(BannerRefresh.BY_AD_UNIT)
    private val availability = mutableListOf<Boolean>()

    private val totalSends: Int get() = views.created.sumOf { it.sendCount }

    private fun setHost() {
        composeRule.setContent {
            if (shown) {
                BannerSlotHost(
                    gateOpen = gateOpen,
                    enabled = enabled,
                    unitId = unitId,
                    heightDp = HEIGHT_DP,
                    refresh = refresh,
                    visible = visible,
                    mayRequestNow = { gateOpen },
                    retryResets = retryResets,
                    logger = logger,
                    placementKey = placementKey,
                    newView = views::newView,
                    modifier = Modifier.fillMaxWidth().testTag(SLOT),
                    onAdAvailabilityChange = { availability += it },
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun assertSlotHeight(heightDp: Int) {
        composeRule.onNodeWithTag(SLOT).assertHeightIsEqualTo(heightDp.dp)
    }

    private fun assertAdShown(shown: Boolean) {
        composeRule.onAllNodesWithTag(FAKE_BANNER_TAG, useUnmergedTree = true).assertCountEquals(if (shown) 1 else 0)
    }

    private fun update(change: () -> Unit) {
        composeRule.runOnIdle(change)
        composeRule.waitForIdle()
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

        update { refresh = BannerRefresh.MANUAL }
        assertEquals(3, views.createdCount)
        assertEquals(1, second.destroyCount)
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

    private companion object {
        const val SLOT = "banner_slot"
        const val KEY = "test_banner"
        const val HEIGHT_DP = 62
    }
}
