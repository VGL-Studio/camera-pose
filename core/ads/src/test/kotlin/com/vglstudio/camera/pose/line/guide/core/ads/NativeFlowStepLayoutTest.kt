package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The layout of the page of a flow step at every height and font scale it takes an ad (plan T-041, KB-20, O-08): the
 * required parts of the ad inside the page, media never below 120 dp, media shrinking before the body, the compact
 * row below the column height, the column with no height bound, and the head of the ad growing with the text.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-xxhdpi")
class NativeFlowStepLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var height by mutableStateOf(484.dp)
    private var assets by mutableStateOf(FakeNativeAdAssets.Design)
    private var fontScale by mutableFloatStateOf(1f)
    private var throughGate by mutableStateOf(false)

    private var metrics: FlowStepMetrics? = null
    private var badgeLine = 0.dp
    private var titleLine = 0.dp
    private val takes = mutableListOf<LoadedNativeAd?>()

    /** The page in a box of [height], or the gate of the page with an ad to take when [throughGate]. */
    private fun setPage() {
        composeRule.setContent {
            WithFontScale(fontScale) {
                AppTheme {
                    val density = LocalDensity.current
                    val typography = MaterialTheme.typography
                    metrics = flowStepMetrics(density, typography)
                    badgeLine = density.lineOf(typography.labelSmall)
                    titleLine = density.lineOf(typography.titleMedium)
                    Box(Modifier.fillMaxWidth().height(height).testTag(PAGE)) {
                        if (throughGate) {
                            key(height, fontScale) {
                                val states = remember { gateStates() }
                                NativeFullScreenStepGate(allowed = true, states = states)
                            }
                        } else {
                            NativeFullScreenStepContent(
                                assets = assets,
                                media = { FakeMedia() },
                                adChoices = { FakeAdChoices() },
                            )
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    /**
     * One line of [style] as Compose draws it: with the non-linear font scale of Android 14 the line height follows
     * the converted font size (lineHeight / fontSize of the style).
     */
    private fun Density.lineOf(style: TextStyle): Dp =
        style.fontSize.toDp() * (style.lineHeight.value / style.fontSize.value)

    private fun gateStates(): Flow<NativeAdState> = fullScreenStepStates(
        take = { FakeLoadedNativeAd(FakeNativeAdAssets.Longest).also { takes += it } },
        ensureLoading = {},
        release = {},
    )

    private fun show(h: Dp, ad: NativeAdAssets = assets, scale: Float = fontScale) {
        height = h
        assets = ad
        fontScale = scale
        composeRule.waitForIdle()
    }

    private fun bounds(tag: String): DpRect =
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun textBounds(text: String): DpRect =
        composeRule.onNodeWithText(text, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun exists(tag: String): Boolean =
        composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun assertInside(name: String, part: DpRect, page: DpRect) {
        assertTrue("$name $part starts above the page $page", part.top.value >= page.top.value - TOLERANCE)
        assertTrue("$name $part ends below the page $page", part.bottom.value <= page.bottom.value + TOLERANCE)
        assertTrue("$name $part starts left of the page $page", part.left.value >= page.left.value - TOLERANCE)
        assertTrue("$name $part ends right of the page $page", part.right.value <= page.right.value + TOLERANCE)
    }

    private fun overlaps(a: DpRect, b: DpRect) =
        a.left < b.right && a.right > b.left && a.top < b.bottom && a.bottom > b.top

    private fun assertDp(message: String, expected: Float, actual: Dp) {
        assertEquals(message, expected, actual.value, TOLERANCE)
    }

    @Test
    fun requiredParts_insidePage_atEveryHeight() {
        setPage()
        listOf(FakeNativeAdAssets.Design, FakeNativeAdAssets.Longest).forEach { ad ->
            HEIGHTS.forEach { h ->
                show(h, ad)
                val name = "${ad.callToAction} at $h"
                val page = bounds(PAGE)
                val cta = bounds(NativeFullScreenAdTestTags.CTA)
                val headline = textBounds(ad.headline)
                val badge = bounds(NativeAdTestTags.BADGE)
                val adChoices = bounds(FAKE_AD_CHOICES_TAG)
                assertDp("CTA height, $name: $cta", 52f, cta.height)
                assertInside("CTA, $name", cta, page)
                assertInside("headline, $name", headline, page)
                assertInside("Ad label, $name", badge, page)
                assertInside("AdChoices, $name", adChoices, page)
                assertTrue("headline of one line, $name: $headline", headline.height.value > 1f)
                assertFalse("AdChoices $adChoices covers CTA $cta, $name", overlaps(adChoices, cta))
                assertFalse("AdChoices $adChoices covers headline $headline, $name", overlaps(adChoices, headline))
            }
        }
    }

    @Test
    fun media_neverBelow120dp() {
        setPage()
        listOf(FakeNativeAdAssets.Design, FakeNativeAdAssets.Longest).forEach { ad ->
            HEIGHTS.forEach { h ->
                show(h, ad)
                val media = bounds(NativeFullScreenAdTestTags.MEDIA)
                val name = "${ad.callToAction} at $h: $media"
                assertTrue("media height, $name", media.height.value >= 120f - TOLERANCE)
                assertTrue("media width, $name", media.width.value >= 120f - TOLERANCE)
            }
        }
    }

    @Test
    fun shrinkOrder_mediaFirst_thenBody() {
        setPage()
        val longest = FakeNativeAdAssets.Longest

        show(484.dp, longest)
        assertDp("body of 3 lines at 484 dp", 60f, bounds(NativeFullScreenAdTestTags.BODY).height)
        assertTrue("media above its floor at 484 dp", bounds(NativeFullScreenAdTestTags.MEDIA).height.value > 120.5f)

        show(436.dp, longest)
        assertDp("body of 3 lines at 436 dp", 60f, bounds(NativeFullScreenAdTestTags.BODY).height)
        assertDp("media at its floor at 436 dp", 120f, bounds(NativeFullScreenAdTestTags.MEDIA).height)

        show(400.dp, longest)
        val body = bounds(NativeFullScreenAdTestTags.BODY)
        assertTrue("body of 1 line at most at 400 dp: $body", body.height.value <= 20.5f)
        val media = bounds(NativeFullScreenAdTestTags.MEDIA)
        assertTrue("media at 400 dp: $media", media.height.value >= 120f - TOLERANCE)

        show(360.dp, longest)
        assertFalse("no body at 360 dp", exists(NativeFullScreenAdTestTags.BODY))
        assertDp("media at its floor at 360 dp", 120f, bounds(NativeFullScreenAdTestTags.MEDIA).height)
    }

    @Test
    fun compactRow_belowColumnMin() {
        setPage()
        show(255.dp, FakeNativeAdAssets.Longest)
        val page = bounds(PAGE)
        val media = bounds(NativeFullScreenAdTestTags.MEDIA)
        val cta = bounds(NativeFullScreenAdTestTags.CTA)
        assertTrue("media $media left of CTA $cta", media.right.value <= cta.left.value + TOLERANCE)
        assertDp("CTA at the bottom: $cta in $page", 16f, page.bottom - cta.bottom)

        // Control: the column at 360 dp, media above the call to action, where it starts after an interstitial.
        show(360.dp)
        val columnPage = bounds(PAGE)
        val columnMedia = bounds(NativeFullScreenAdTestTags.MEDIA)
        val columnCta = bounds(NativeFullScreenAdTestTags.CTA)
        assertTrue(
            "media $columnMedia above CTA $columnCta",
            columnMedia.bottom.value <= columnCta.top.value + TOLERANCE,
        )
        assertDp("media starts at 76 dp", 76f, columnMedia.top - columnPage.top)
    }

    @Test
    fun unboundedHeight_media120() {
        composeRule.setContent {
            AppTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    NativeFullScreenStepContent(
                        assets = FakeNativeAdAssets.Design,
                        modifier = Modifier.testTag(PAGE),
                        media = { FakeMedia() },
                        adChoices = { FakeAdChoices() },
                    )
                }
            }
        }
        composeRule.waitForIdle()
        val media = bounds(NativeFullScreenAdTestTags.MEDIA)
        assertTrue("media of at least 120 dp: $media", media.height.value >= 120f - TOLERANCE)
        assertDp("media of 200 dp: $media", 200f, media.height)
        assertDp("CTA height", 52f, bounds(NativeFullScreenAdTestTags.CTA).height)
        assertTrue("the body is drawn", exists(NativeFullScreenAdTestTags.BODY))
    }

    @Test
    fun fontScale_requiredPartsKeepSize() {
        setPage()
        show(360.dp, FakeNativeAdAssets.Longest, 1f)
        val atOne = metrics!!
        assertDp("least height at 1.0", 152f, atOne.minHeight)
        assertDp("least column height at 1.0", 360f, atOne.columnMinHeight)
        assertDp("head of the ad at 1.0", 48f, atOne.headerHeight)

        // 154 dp is above the least height at 1.0 (152 dp) and below it at 1.3 and 2.0 (plan T-049, section 3.5).
        var laidOut = 0
        var gated = 0
        listOf(1f, 1.3f, 2f).forEach { scale ->
            listOf(360.dp, 154.dp).forEach { h ->
                throughGate = false
                show(h, FakeNativeAdAssets.Longest, scale)
                val least = metrics!!.minHeight
                val name = "font scale $scale at $h (least $least)"
                if (h >= least) {
                    laidOut++
                    val page = bounds(PAGE)
                    val badge = bounds(NativeAdTestTags.BADGE)
                    assertDp("Ad label of one line of $badgeLine, $name: $badge", badgeLine.value, badge.height)
                    assertInside("Ad label, $name", badge, page)
                    val headline = textBounds(FakeNativeAdAssets.Longest.headline)
                    assertDp("headline of one line of $titleLine, $name: $headline", titleLine.value, headline.height)
                    assertInside("headline, $name", headline, page)
                    val media = bounds(NativeFullScreenAdTestTags.MEDIA)
                    assertTrue("media of 120 dp, $name: $media", media.height.value >= 120f - TOLERANCE)
                    val cta = bounds(NativeFullScreenAdTestTags.CTA)
                    assertDp("CTA height, $name", 52f, cta.height)
                    assertInside("CTA, $name", cta, page)
                } else {
                    gated++
                    takes.clear()
                    throughGate = true
                    composeRule.waitForIdle()
                    assertTrue("no ad taken, $name: $takes", takes.isEmpty())
                    assertFalse("no CTA, $name", exists(NativeFullScreenAdTestTags.CTA))
                }
            }
        }
        // Both branches ran: 360 dp at the three scales and 154 dp at 1.0 laid out, 154 dp at 1.3 and 2.0 gated.
        assertEquals("pages laid out", 4, laidOut)
        assertEquals("pages gated", 2, gated)
        // Control (L-055): the page through the gate at 360 dp and font scale 1.0 takes its ad.
        takes.clear()
        throughGate = true
        show(360.dp, FakeNativeAdAssets.Longest, 1f)
        assertEquals(1, takes.size)
    }

    /** The top right corner of [page] where the SDK pins the real AdChoices (O-08): 24 x 24 dp. */
    private fun adChoicesCorner(page: DpRect): DpRect =
        DpRect(page.right - AD_CHOICES_CORNER, page.top, page.right, page.top + AD_CHOICES_CORNER)

    /** No part of the ad on the page cuts into the corner of the real AdChoices (O-08, plan T-049). */
    private fun assertCornerFree(name: String, ad: NativeAdAssets) {
        val page = bounds(PAGE)
        val corner = adChoicesCorner(page)
        val parts = buildList {
            add("media" to bounds(NativeFullScreenAdTestTags.MEDIA))
            add("headline" to textBounds(ad.headline))
            add("Ad label" to bounds(NativeAdTestTags.BADGE))
            if (exists(NativeFullScreenAdTestTags.BODY)) add("body" to bounds(NativeFullScreenAdTestTags.BODY))
            add("CTA" to bounds(NativeFullScreenAdTestTags.CTA))
        }
        parts.forEach { (part, rect) ->
            assertFalse("$part $rect cuts into the AdChoices corner $corner, $name", overlaps(rect, corner))
        }
    }

    /**
     * KB-20 (plan T-049): the page area of a phone in landscape with its system bars and camera cutout, 186.5 dp
     * high, holds every required part of the ad in the compact row, and the corner of the real AdChoices stays free.
     */
    @Test
    @Config(qualifiers = "w873dp-h393dp-440dpi")
    fun compactRow_phoneLandscape_requiredPartsInside() {
        val ad = FakeNativeAdAssets.Longest
        setPage()
        show(186.5.dp, ad)
        val page = bounds(PAGE)
        val cta = bounds(NativeFullScreenAdTestTags.CTA)
        val media = bounds(NativeFullScreenAdTestTags.MEDIA)
        val headline = textBounds(ad.headline)
        val badge = bounds(NativeAdTestTags.BADGE)
        val adChoices = bounds(FAKE_AD_CHOICES_TAG)
        assertDp("CTA height: $cta", 52f, cta.height)
        assertDp("CTA 16 dp above the bottom: $cta in $page", 16f, page.bottom - cta.bottom)
        assertInside("CTA", cta, page)
        assertDp("media height: $media", 154.5f, media.height)
        assertDp("media starts at 16 dp: $media in $page", 16f, media.top - page.top)
        assertInside("headline", headline, page)
        assertInside("Ad label", badge, page)
        assertTrue("AdChoices $adChoices right of the headline $headline", adChoices.left >= headline.right)
        assertFalse("AdChoices $adChoices covers CTA $cta", overlaps(adChoices, cta))
        assertFalse("AdChoices $adChoices covers headline $headline", overlaps(adChoices, headline))
        assertDp("body of 1 line", 20f, bounds(NativeFullScreenAdTestTags.BODY).height)
        assertCornerFree("compact row at 186.5 dp", ad)

        show(152.dp, ad)
        assertDp("CTA height at 152 dp", 52f, bounds(NativeFullScreenAdTestTags.CTA).height)
        assertCornerFree("compact row at 152 dp", ad)

        // Control: the column at 360 dp keeps the corner free with its top bar.
        show(360.dp, ad)
        assertCornerFree("column at 360 dp", ad)
    }

    /** The placeholder of the page in the compact row at 186.5 dp (plan T-049): the head left of the 36 dp cell. */
    @Test
    @Config(qualifiers = "w873dp-h393dp-440dpi")
    fun compactRow_placeholder_phoneLandscape() {
        composeRule.setContent {
            AppTheme {
                Box(Modifier.fillMaxWidth().height(186.5.dp)) {
                    FlowStepPagePlaceholder(Modifier.testTag(NativeFlowStepTestTags.FULL_PAGE_PLACEHOLDER))
                }
            }
        }
        composeRule.waitForIdle()
        val page = bounds(NativeFlowStepTestTags.FULL_PAGE_PLACEHOLDER)
        val header = bounds(NativeFlowStepTestTags.PLACEHOLDER_HEADER)
        val cta = bounds(NativeFlowStepTestTags.PLACEHOLDER_CTA)
        assertTrue("head block has a width: $header", header.width.value > 1f)
        assertTrue(
            "head block $header left of the 36 dp cell in $page",
            header.right.value <= (page.right - 16.dp - 36.dp).value + TOLERANCE,
        )
        assertDp("CTA block height: $cta", 52f, cta.height)
        assertInside("CTA block", cta, page)
    }

    /** The app reads the least height at its font scale (plan T-049): the number the gate uses. */
    @Test
    fun defaults_minHeight_followsFontScale() {
        var scale by mutableFloatStateOf(1f)
        var fromDefaults = 0.dp
        var fromMetrics = 0.dp
        composeRule.setContent {
            WithFontScale(scale) {
                AppTheme {
                    fromDefaults = NativeFullScreenStepDefaults.minHeight()
                    fromMetrics = flowStepMetrics(LocalDensity.current, MaterialTheme.typography).minHeight
                }
            }
        }
        val read = listOf(1f, 1.3f, 2f).map { s ->
            scale = s
            composeRule.waitForIdle()
            assertEquals("minHeight() at $s", fromMetrics.value, fromDefaults.value, 0.01f)
            fromDefaults
        }
        assertDp("minHeight() at 1.0", 152f, read[0])
        assertTrue("grows with the font scale: $read", read[0] < read[1] && read[1] < read[2])
        // Control: the constant is the number at font scale 1.0.
        assertEquals(152.dp, NativeFullScreenStepDefaults.MinHeight)
    }

    private companion object {
        const val PAGE = "page"
        const val TOLERANCE = 0.5f
        val HEIGHTS = listOf(484.dp, 435.dp, 400.dp, 360.dp, 359.dp, 255.dp, 204.dp, 200.dp, 186.5.dp, 152.dp)
        val AD_CHOICES_CORNER = 24.dp
    }
}
