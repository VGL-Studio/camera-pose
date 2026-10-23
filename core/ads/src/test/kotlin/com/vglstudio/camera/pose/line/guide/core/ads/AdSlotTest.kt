package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/**
 * The banner slot (spec poses, KB-6; plan T-023, section 4). A real [AdView] is drawn (it never loads: no request is
 * sent); the load callbacks are called by hand. Native graphics: sizes are measured.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AdSlotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var adView: AdView? = null

    private fun setBanner(separator: Boolean = true) {
        composeRule.setContent {
            val context = LocalContext.current
            val view = remember {
                AdView(context).apply {
                    adUnitId = GoogleTestAdUnitIds.of(AdPlacement.SAMPLE_BOTTOM_BANNER)
                    setAdSize(AdSize.BANNER)
                }
            }
            adView = view
            Column {
                BannerView(
                    adView = view,
                    heightDp = BANNER_HEIGHT,
                    separator = if (separator) {
                        { Box(Modifier.fillMaxWidth().height(SEPARATOR_HEIGHT.dp).testTag(SEPARATOR)) }
                    } else {
                        null
                    },
                    modifier = Modifier.testTag(SLOT),
                )
                Box(Modifier.fillMaxWidth().height(10.dp).testTag(BELOW))
            }
        }
        composeRule.waitForIdle()
    }

    private fun callListener(action: AdView.() -> Unit) {
        composeRule.runOnUiThread { adView!!.action() }
        composeRule.waitForIdle()
    }

    private fun failLoad() = callListener { adListener.onAdFailedToLoad(NO_FILL) }

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
        callListener { adListener.onAdLoaded() }
        assertEquals((SEPARATOR_HEIGHT + BANNER_HEIGHT).toFloat(), slotHeight(), 0.5f)
        assertTrue(hasSeparator())
        assertEquals((SEPARATOR_HEIGHT + BANNER_HEIGHT).toFloat(), belowTop(), 0.5f)
    }

    @Test
    fun withoutAdsController_takesNoSpace_andDrawsNoSeparator() {
        composeRule.setContent {
            Column {
                AdSlot(
                    placement = AdPlacement.SAMPLE_BOTTOM_BANNER,
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
        val reported = mutableListOf<Boolean>()
        val listener = bannerListener { reported += it }
        listener.onAdFailedToLoad(NO_FILL)
        listener.onAdLoaded()
        assertEquals(listOf(true, false), reported)
    }

    private companion object {
        const val SLOT = "slot"
        const val SEPARATOR = "separator"
        const val BELOW = "below"
        const val BANNER_HEIGHT = 50
        const val SEPARATOR_HEIGHT = 2
        val NO_FILL = LoadAdError(3, "No fill", "com.google.android.gms.ads", null, null)
    }
}
