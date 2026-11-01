package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/**
 * The banner slot of this app with a separator (spec poses, KB-6; plan T-023, section 4; plan T-074, section 3.1): the
 * slot of the framework ([BannerSlotHost]) with a fake banner of the SDK, whose load results are given by hand.
 * Native graphics: sizes are measured.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AdSlotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val views = FakeBannerAdViews()
    private val reported = mutableListOf<Boolean>()

    private fun setBanner(separator: Boolean = true) {
        composeRule.setContent {
            Column {
                BannerSlotHost(
                    gateOpen = true,
                    enabled = true,
                    unitId = "u",
                    heightDp = BANNER_HEIGHT,
                    refresh = BannerRefresh.BY_AD_UNIT,
                    visible = MutableStateFlow(true),
                    covered = MutableStateFlow(false),
                    mayRequestNow = { true },
                    retryResets = MutableStateFlow(0),
                    logger = RecordingAppLogger(),
                    placementKey = AdPlacement.GALLERY_BANNER.key,
                    newView = views::newView,
                    modifier = Modifier.testTag(SLOT),
                    separator = if (separator) {
                        { Box(Modifier.fillMaxWidth().height(SEPARATOR_HEIGHT.dp).testTag(SEPARATOR)) }
                    } else {
                        null
                    },
                    onLoadResult = { reported += it },
                )
                Box(Modifier.fillMaxWidth().height(10.dp).testTag(BELOW))
            }
        }
        composeRule.waitForIdle()
    }

    private fun result(action: FakeBannerAdView.() -> Unit) {
        composeRule.runOnUiThread { views.last.action() }
        composeRule.waitForIdle()
    }

    private fun failLoad() = result { emitFailed(NO_FILL) }

    private fun slotHeight() = composeRule.onNodeWithTag(SLOT).getUnclippedBoundsInRoot().height.value

    private fun belowTop() = composeRule.onNodeWithTag(BELOW).getUnclippedBoundsInRoot().top.value

    private fun hasSeparator() = composeRule.onAllNodesWithTag(SEPARATOR).fetchSemanticsNodes().size == 1

    @Test
    fun beforeLoad_keepsBannerHeight_withSeparator() {
        setBanner()
        assertEquals((SEPARATOR_HEIGHT + BANNER_HEIGHT).toFloat(), slotHeight(), 0.5f)
        assertTrue(hasSeparator())
        assertEquals((SEPARATOR_HEIGHT + BANNER_HEIGHT).toFloat(), belowTop(), 0.5f)
    }

    @Test
    fun failedLoad_collapsesBannerToZero_andHidesSeparator() {
        setBanner()
        // Control: before the failure the banner takes its room, so the checks below can fail.
        assertEquals((SEPARATOR_HEIGHT + BANNER_HEIGHT).toFloat(), slotHeight(), 0.5f)
        assertTrue(hasSeparator())
        failLoad()
        assertEquals(0f, slotHeight(), 0.5f)
        assertFalse(hasSeparator())
        // What is under the slot moves up by the banner and the separator.
        assertEquals(0f, belowTop(), 0.5f)
    }

    @Test
    fun loadedAfterFailure_showsBannerAndSeparatorAgain() {
        setBanner()
        failLoad()
        assertEquals(0f, slotHeight(), 0.5f)
        assertFalse(hasSeparator())
        // The retry of B-02 is still sent from the collapsed slot (the view stays in the tree).
        composeRule.mainClock.advanceTimeBy(RETRY_WAIT_MILLIS)
        composeRule.waitForIdle()
        assertEquals(2, views.last.sendCount)
        result { emitLoaded() }
        assertEquals((SEPARATOR_HEIGHT + BANNER_HEIGHT).toFloat(), slotHeight(), 0.5f)
        assertTrue(hasSeparator())
        assertEquals((SEPARATOR_HEIGHT + BANNER_HEIGHT).toFloat(), belowTop(), 0.5f)
    }

    @Test
    fun withoutAdsController_takesNoSpace_andDrawsNoSeparator() {
        composeRule.setContent {
            Column {
                AdSlot(
                    placement = AdPlacement.GALLERY_BANNER,
                    modifier = Modifier.testTag(SLOT),
                    separator = { Box(Modifier.fillMaxWidth().height(SEPARATOR_HEIGHT.dp).testTag(SEPARATOR)) },
                )
                Box(Modifier.fillMaxWidth().height(10.dp).testTag(BELOW))
            }
        }
        composeRule.waitForIdle()
        assertEquals(0f, slotHeight(), 0.5f)
        assertFalse(hasSeparator())
        assertEquals(0f, belowTop(), 0.5f)
    }

    @Test
    fun bannerListener_reportsFailedThenLoaded() {
        setBanner()
        failLoad()
        result { emitLoaded() }
        // Each result of a load is reported: false for the failure, true for the load.
        assertEquals(listOf(false, true), reported)
    }

    private companion object {
        const val SLOT = "slot"
        const val SEPARATOR = "separator"
        const val BELOW = "below"
        const val BANNER_HEIGHT = 50
        const val SEPARATOR_HEIGHT = 2
        const val NO_FILL = 3

        /** The first retry of B-02 comes 2 s after the first failure. */
        const val RETRY_WAIT_MILLIS = 2_000L
    }
}
