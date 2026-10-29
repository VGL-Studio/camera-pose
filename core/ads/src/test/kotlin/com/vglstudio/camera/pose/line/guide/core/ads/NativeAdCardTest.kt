package com.vglstudio.camera.pose.line.guide.core.ads

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The stateless native ad card and its loading placeholder: parts, sizes, AdChoices corner, colors (plans T-010,
 * T-027; sizes of ads-business-rules.md 5.7 and design/context/SampleScreen_ready_light.md).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class NativeAdCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val badge: String
        get() = ApplicationProvider.getApplicationContext<android.content.Context>()
            .getString(R.string.ads_native_badge)

    private fun setCard(size: NativeAdSize, assets: NativeAdAssets = sampleNativeAdAssets) {
        composeRule.setContent {
            AppTheme { NativeAdCard(assets = assets, size = size, modifier = Modifier.width(CARD_WIDTH)) }
        }
    }

    private fun bounds(tag: String) = composeRule.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun textStyle(text: String): TextStyle {
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult].action!!(layouts)
        return layouts.single().layoutInput.style
    }

    @Test
    fun small_showsHeadlineAdBadgeAdvertiserAndCta() {
        setCard(NativeAdSize.SMALL)
        composeRule.onNodeWithText("Headline of the ad", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText(badge, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Advertiser or short body", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Install", useUnmergedTree = true).assertExists()
    }

    @Test
    fun small_withoutAdvertiser_showsBody() {
        setCard(NativeAdSize.SMALL, sampleNativeAdAssets.copy(advertiser = null))
        composeRule.onNodeWithText(sampleNativeAdAssets.body!!, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText(badge, useUnmergedTree = true).assertExists()
    }

    @Test
    fun small_leavesAdChoicesCornerFree() {
        setCard(NativeAdSize.SMALL)
        val card = bounds(NativeAdTestTags.CARD)
        val cta = bounds(NativeAdTestTags.CTA)
        assertEquals(CARD_WIDTH.value, (card.right - card.left).value, 0.5f)
        assertTrue("CTA ends ${card.right - cta.right} from the card edge", card.right - cta.right >= 24.dp)
    }

    @Test
    fun medium_leavesAdChoicesCornerFree() {
        setCard(NativeAdSize.MEDIUM)
        val card = bounds(NativeAdTestTags.CARD)
        val icon = bounds(NativeAdTestTags.ICON)
        assertTrue("Icon starts ${icon.top - card.top} below the card top", icon.top - card.top >= 24.dp)
    }

    @Test
    fun small_iconIs36dp() {
        setCard(NativeAdSize.SMALL)
        composeRule.onNodeWithTag(NativeAdTestTags.ICON, useUnmergedTree = true)
            .assertWidthIsEqualTo(36.dp)
            .assertHeightIsEqualTo(36.dp)
    }

    @Test
    fun small_ctaIs34dpHighWith13spText() {
        setCard(NativeAdSize.SMALL)
        composeRule.onNodeWithTag(NativeAdTestTags.CTA, useUnmergedTree = true).assertHeightIsEqualTo(34.dp)
        assertEquals(13.sp, textStyle("Install").fontSize)
    }

    @Test
    fun small_badgeTextIs10sp_belowHeadline() {
        setCard(NativeAdSize.SMALL)
        assertEquals(10.sp, textStyle(badge).fontSize)
        val headline = composeRule.onNodeWithText("Headline of the ad", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val label = bounds(NativeAdTestTags.BADGE)
        val advertiser = composeRule.onNodeWithText("Advertiser or short body", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        // The label starts the description line, under the headline (design redrawn by T-035; plan T-027, Q6).
        assertTrue("label top ${label.top}, headline bottom ${headline.bottom}", label.top >= headline.bottom)
        assertEquals(headline.left.value, label.left.value, 0.5f)
        assertTrue("advertiser starts after the label", advertiser.left >= label.right)
        assertTrue("advertiser on the label's line", advertiser.top < label.bottom && advertiser.bottom > label.top)
    }

    @Test
    fun small_is80dpHigh() {
        setCard(NativeAdSize.SMALL)
        composeRule.onNodeWithTag(NativeAdTestTags.CARD).assertHeightIsEqualTo(80.dp)
    }

    @Test
    fun medium_is410dpHigh() {
        setCard(NativeAdSize.MEDIUM)
        composeRule.onNodeWithTag(NativeAdTestTags.CARD).assertHeightIsEqualTo(410.dp)
    }

    @Test
    fun medium_ctaIs44dp_labelLarge14sp() {
        var labelLarge: TextStyle? = null
        composeRule.setContent {
            AppTheme {
                labelLarge = MaterialTheme.typography.labelLarge
                NativeAdCard(sampleNativeAdAssets, NativeAdSize.MEDIUM, Modifier.width(CARD_WIDTH))
            }
        }
        composeRule.onNodeWithTag(NativeAdTestTags.CTA, useUnmergedTree = true).assertHeightIsEqualTo(44.dp)
        val style = textStyle("Install")
        assertEquals(14.sp, style.fontSize)
        assertEquals(labelLarge!!.fontWeight, style.fontWeight)
    }

    @Test
    fun medium_starRow16dp_belowBadgeLine_hiddenStarsKeepHeight() {
        var assets by mutableStateOf(sampleNativeAdAssets.copy(starRating = 4.0))
        composeRule.setContent {
            AppTheme { NativeAdCard(assets, NativeAdSize.MEDIUM, Modifier.width(CARD_WIDTH)) }
        }
        composeRule.onAllNodesWithTag(NativeAdTestTags.STAR, useUnmergedTree = true).assertCountEquals(4)
        val row = bounds(NativeAdTestTags.STAR_ROW)
        assertEquals(16f, (row.bottom - row.top).value, 0.5f)
        assertTrue("star row below the badge line", row.top >= bounds(NativeAdTestTags.BADGE).bottom)
        val mediaTop = bounds(NativeAdTestTags.MEDIA).top

        // L-055: a rating of 0 draws no star, yet the card and its parts keep their place.
        assets = sampleNativeAdAssets.copy(starRating = 0.0)
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag(NativeAdTestTags.STAR, useUnmergedTree = true).assertCountEquals(0)
        composeRule.onNodeWithTag(NativeAdTestTags.CARD).assertHeightIsEqualTo(410.dp)
        assertEquals(mediaTop.value, bounds(NativeAdTestTags.MEDIA).top.value, 0.5f)
    }

    @Test
    fun medium_ctaLarge_56dp_17sp_cardIs422dp() {
        var large by mutableStateOf(true)
        composeRule.setContent {
            AppTheme {
                NativeAdCard(sampleNativeAdAssets, NativeAdSize.MEDIUM, Modifier.width(CARD_WIDTH), ctaLarge = large)
            }
        }
        composeRule.onNodeWithTag(NativeAdTestTags.CTA, useUnmergedTree = true).assertHeightIsEqualTo(56.dp)
        assertEquals(17.sp, textStyle("Install").fontSize)
        composeRule.onNodeWithTag(NativeAdTestTags.CARD).assertHeightIsEqualTo(422.dp)
        // The text of the SDK is kept as it is, never upper-cased.
        composeRule.onNodeWithText("Install", useUnmergedTree = true).assertExists()

        // Control: the same ad without cta_large.
        large = false
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(NativeAdTestTags.CTA, useUnmergedTree = true).assertHeightIsEqualTo(44.dp)
        composeRule.onNodeWithTag(NativeAdTestTags.CARD).assertHeightIsEqualTo(410.dp)
    }

    @Test
    fun card_iconIs32dp_headline13sp_mediaFillsTop_ctaIs36dp() {
        composeRule.setContent {
            AppTheme { NativeAdCard(sampleNativeAdAssets, NativeAdSize.CARD, Modifier.size(180.dp, 300.dp)) }
        }
        val card = bounds(NativeAdTestTags.CARD)
        assertEquals(180f, (card.right - card.left).value, 0.5f)
        assertEquals(300f, (card.bottom - card.top).value, 0.5f)
        composeRule.onNodeWithTag(NativeAdTestTags.ICON, useUnmergedTree = true)
            .assertWidthIsEqualTo(32.dp)
            .assertHeightIsEqualTo(32.dp)
        assertEquals(13.sp, textStyle("Headline of the ad").fontSize)
        val media = bounds(NativeAdTestTags.MEDIA)
        val icon = bounds(NativeAdTestTags.ICON)
        val cta = bounds(NativeAdTestTags.CTA)
        assertEquals(8f, (media.top - card.top).value, 0.5f)
        assertEquals(164f, (media.right - media.left).value, 0.5f)
        assertTrue("media above the icon", media.bottom <= icon.top)
        assertTrue("media fills the top", (media.bottom - media.top) >= 120.dp)
        assertEquals(36f, (cta.bottom - cta.top).value, 0.5f)
        assertEquals(164f, (cta.right - cta.left).value, 0.5f)
        assertEquals(8f, (card.bottom - cta.bottom).value, 0.5f)
    }

    @Test
    fun card_showsAdBadge_leavesAdChoicesCornerFree() {
        composeRule.setContent {
            AppTheme { NativeAdCard(sampleNativeAdAssets, NativeAdSize.CARD, Modifier.size(180.dp, 300.dp)) }
        }
        composeRule.onNodeWithText(badge, useUnmergedTree = true).assertExists()
        val card = bounds(NativeAdTestTags.CARD)
        val corner = DpRect(card.right - 24.dp, card.top, card.right, card.top + 24.dp)
        // AdChoices sits on the media, top right; no part of the app's own layout is there.
        listOf(NativeAdTestTags.ICON, NativeAdTestTags.HEADLINE, NativeAdTestTags.BADGE, NativeAdTestTags.CTA)
            .forEach { tag ->
                val part = bounds(tag)
                val overlaps = part.left < corner.right &&
                    part.right > corner.left &&
                    part.top < corner.bottom &&
                    part.bottom > corner.top
                assertFalse("$tag $part overlaps the AdChoices corner $corner", overlaps)
            }
    }

    @Test
    fun card_inFixedCell_takesCellSize() {
        composeRule.setContent {
            AppTheme {
                Box(Modifier.size(160.dp, 240.dp)) { NativeAdCard(sampleNativeAdAssets, NativeAdSize.CARD) }
            }
        }
        val card = bounds(NativeAdTestTags.CARD)
        assertEquals(160f, (card.right - card.left).value, 0.5f)
        assertEquals(240f, (card.bottom - card.top).value, 0.5f)
    }

    @Test
    fun placeholder_matchesCardHeight_forEachSize() {
        var size by mutableStateOf(NativeAdSize.SMALL)
        composeRule.setContent {
            AppTheme {
                Column {
                    NativeAdCard(sampleNativeAdAssets, size, Modifier.width(CARD_WIDTH))
                    NativeAdPlaceholder(size, Modifier.width(CARD_WIDTH), animated = false)
                }
            }
        }
        NativeAdSize.entries.forEach { current ->
            size = current
            composeRule.waitForIdle()
            val card = bounds(NativeAdTestTags.CARD)
            val placeholder = bounds(NativeAdTestTags.PLACEHOLDER)
            val expected = when (current) {
                NativeAdSize.SMALL -> 80.dp
                NativeAdSize.MEDIUM -> 410.dp
                NativeAdSize.FULL -> FULL_HEIGHT
                NativeAdSize.CARD -> 300.dp
                NativeAdSize.STEP -> 250.dp
            }
            assertEquals("$current card height", expected.value, (card.bottom - card.top).value, 0.5f)
            assertEquals(
                "$current placeholder height",
                (card.bottom - card.top).value,
                (placeholder.bottom - placeholder.top).value,
                0.5f,
            )
        }
    }

    @Test
    fun step_is250dpHigh_icon32_cta36_headline13sp() {
        setCard(NativeAdSize.STEP)
        val card = bounds(NativeAdTestTags.CARD)
        assertEquals(250f, (card.bottom - card.top).value, 0.5f)
        composeRule.onNodeWithTag(NativeAdTestTags.ICON, useUnmergedTree = true)
            .assertWidthIsEqualTo(32.dp)
            .assertHeightIsEqualTo(32.dp)
        assertEquals(13.sp, textStyle("Headline of the ad").fontSize)
        val media = bounds(NativeAdTestTags.MEDIA)
        val icon = bounds(NativeAdTestTags.ICON)
        val cta = bounds(NativeAdTestTags.CTA)
        // Section 5.7 and the 250 dp layout of design/context/AdGalleryScreen_collapsible-expanded_light.md: 12 dp
        // inner margin, media on top, head, then a call to action of 36 dp as wide as the card.
        assertEquals(12f, (media.top - card.top).value, 0.5f)
        assertEquals(12f, (media.left - card.left).value, 0.5f)
        assertTrue("media at least 120 dp wide: $media", (media.right - media.left) >= 120.dp)
        assertTrue("media above the icon", media.bottom <= icon.top)
        assertEquals(36f, (cta.bottom - cta.top).value, 0.5f)
        assertEquals((card.right - card.left).value - 24f, (cta.right - cta.left).value, 0.5f)
        assertEquals(12f, (card.bottom - cta.bottom).value, 0.5f)
        assertTrue("head above the call to action", icon.bottom <= cta.top)
    }

    @Test
    fun step_showsAdBadge_leavesAdChoicesCornerFree() {
        setCard(NativeAdSize.STEP)
        composeRule.onNodeWithText(badge, useUnmergedTree = true).assertExists()
        val card = bounds(NativeAdTestTags.CARD)
        val corner = DpRect(card.right - 24.dp, card.top, card.right, card.top + 24.dp)
        // O-08: AdChoices sits on the media, top right, as on every native ad but the collapsible layer.
        listOf(NativeAdTestTags.ICON, NativeAdTestTags.HEADLINE, NativeAdTestTags.BADGE, NativeAdTestTags.CTA)
            .forEach { tag ->
                val part = bounds(tag)
                val overlaps = part.left < corner.right &&
                    part.right > corner.left &&
                    part.top < corner.bottom &&
                    part.bottom > corner.top
                assertFalse("$tag $part overlaps the AdChoices corner $corner", overlaps)
            }
    }

    @Test
    fun step_longTexts_keepHeight() {
        setCard(NativeAdSize.STEP, longFullNativeAdAssets)
        composeRule.onNodeWithTag(NativeAdTestTags.CARD).assertHeightIsEqualTo(250.dp)
        composeRule.onNodeWithTag(NativeAdTestTags.CTA, useUnmergedTree = true).assertHeightIsEqualTo(36.dp)
        val card = bounds(NativeAdTestTags.CARD)
        val cta = bounds(NativeAdTestTags.CTA)
        assertTrue("call to action inside the card: $cta, $card", cta.bottom <= card.bottom)
    }

    @Test
    fun stepPlaceholder_ctaIs36dp() {
        composeRule.setContent {
            AppTheme { NativeAdPlaceholder(NativeAdSize.STEP, Modifier.width(CARD_WIDTH), animated = false) }
        }
        composeRule.onNodeWithTag(NativeAdTestTags.PLACEHOLDER).assertHeightIsEqualTo(250.dp)
        composeRule.onNodeWithTag(NativeAdTestTags.PLACEHOLDER_CTA, useUnmergedTree = true)
            .assertHeightIsEqualTo(36.dp)
    }

    @Test
    fun longTexts_keepHeight() {
        val long = sampleNativeAdAssets.copy(headline = "H".repeat(40) + " " + "e".repeat(39), body = "b ".repeat(150))
        var size by mutableStateOf(NativeAdSize.SMALL)
        composeRule.setContent {
            AppTheme { NativeAdCard(long.copy(advertiser = null), size, Modifier.width(CARD_WIDTH)) }
        }
        composeRule.onNodeWithTag(NativeAdTestTags.CARD).assertHeightIsEqualTo(80.dp)
        composeRule.onNodeWithTag(NativeAdTestTags.HEADLINE, useUnmergedTree = true).assertHeightIsEqualTo(20.dp)

        size = NativeAdSize.MEDIUM
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(NativeAdTestTags.CARD).assertHeightIsEqualTo(410.dp)
        composeRule.onNodeWithTag(NativeAdTestTags.HEADLINE, useUnmergedTree = true).assertHeightIsEqualTo(20.dp)
    }

    @Test
    fun medium_hasMediaOfAtLeast120dp() {
        setCard(NativeAdSize.MEDIUM)
        val card = bounds(NativeAdTestTags.CARD)
        val media = bounds(NativeAdTestTags.MEDIA)
        assertEquals(CARD_WIDTH.value, (card.right - card.left).value, 0.5f)
        assertTrue("Media is ${media.right - media.left} wide", media.right - media.left >= 355.dp - 0.5.dp)
        assertEquals(196f, (media.bottom - media.top).value, 0.5f)
        assertTrue(media.right - media.left >= 120.dp && media.bottom - media.top >= 120.dp)
        // Below the 56 dp head with its star row: 1 + 24 + 56 + 12 dp from the top of the card.
        assertEquals(93f, (media.top - card.top).value, 0.5f)
    }

    @Test
    fun small_hasNoMedia() {
        setCard(NativeAdSize.SMALL)
        // Control: the card itself is there, so the missing media node is not a missing card.
        composeRule.onNodeWithTag(NativeAdTestTags.CARD).assertExists()
        composeRule.onNodeWithTag(NativeAdTestTags.MEDIA, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun badgeText_isAd() {
        var text = ""
        var labelSmall: TextStyle? = null
        composeRule.setContent {
            AppTheme {
                text = stringResource(R.string.ads_native_badge)
                labelSmall = MaterialTheme.typography.labelSmall
                NativeAdCard(sampleNativeAdAssets, NativeAdSize.SMALL, Modifier.width(CARD_WIDTH))
            }
        }
        assertEquals("Ad", text)
        val style = textStyle("Ad")
        // labelSmall at the 10 sp of the small ad (ads-business-rules.md 5.7, plan T-027).
        assertEquals(10.sp, style.fontSize)
        assertEquals(labelSmall!!.fontWeight, style.fontWeight)
        assertEquals(labelSmall!!.lineHeight, style.lineHeight)
        // Control: the headline next to it is not the label's style, so the check above reads the right node.
        assertTrue(textStyle("Headline of the ad").fontSize != style.fontSize)
    }

    @Test
    fun full_showsHeadlineAdBadgeCta_leavesAdChoicesCornerFree() {
        setCard(NativeAdSize.FULL)
        composeRule.onNodeWithText("Headline of the ad", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText(badge, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Install", useUnmergedTree = true).assertExists()
        val card = bounds(NativeAdTestTags.CARD)
        assertEquals(CARD_WIDTH.value, (card.right - card.left).value, 0.5f)
        val corner = DpRect(card.right - 24.dp, card.top, card.right, card.top + 24.dp)
        listOf(
            NativeAdTestTags.ICON,
            NativeAdTestTags.HEADLINE,
            NativeAdTestTags.BADGE,
            NativeAdTestTags.MEDIA,
            NativeAdTestTags.CTA,
        ).forEach { tag ->
            val part = bounds(tag)
            val overlapsX = part.left < corner.right && part.right > corner.left
            val overlapsY = part.top < corner.bottom && part.bottom > corner.top
            val overlaps = overlapsX && overlapsY
            assertFalse("$tag $part overlaps the AdChoices corner $corner", overlaps)
        }
    }

    @Test
    fun full_mediaIs200dpHigh_atLeast120dpWide() {
        setCard(NativeAdSize.FULL)
        val media = bounds(NativeAdTestTags.MEDIA)
        assertEquals(200f, (media.bottom - media.top).value, 0.5f)
        assertTrue("Media is ${media.right - media.left} wide", media.right - media.left >= 120.dp)
    }

    @Test
    fun full_iconIs40dp_headline14sp() {
        setCard(NativeAdSize.FULL)
        composeRule.onNodeWithTag(NativeAdTestTags.ICON, useUnmergedTree = true)
            .assertWidthIsEqualTo(40.dp)
            .assertHeightIsEqualTo(40.dp)
        assertEquals(14.sp, textStyle("Headline of the ad").fontSize)
    }

    @Test
    fun full_placeholderHeight_equalsCardHeight_shortAndLongTexts() {
        var assets by mutableStateOf(sampleNativeAdAssets)
        composeRule.setContent {
            AppTheme {
                Column {
                    NativeAdCard(assets, NativeAdSize.FULL, Modifier.width(CARD_WIDTH))
                    NativeAdPlaceholder(NativeAdSize.FULL, Modifier.width(CARD_WIDTH), animated = false)
                }
            }
        }
        listOf(sampleNativeAdAssets, longFullNativeAdAssets).forEach { current ->
            assets = current
            composeRule.waitForIdle()
            val card = bounds(NativeAdTestTags.CARD)
            val placeholder = bounds(NativeAdTestTags.PLACEHOLDER)
            assertEquals("card with ${current.headline}", FULL_HEIGHT.value, (card.bottom - card.top).value, 0.5f)
            assertEquals(FULL_HEIGHT.value, (placeholder.bottom - placeholder.top).value, 0.5f)
        }
    }

    /** Lines of the laid-out [text] (the semantics text is the whole text, even when the line cuts it off). */
    private fun lineCount(text: String): Int {
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult].action!!(layouts)
        return layouts.single().lineCount
    }

    /**
     * Whether the laid-out call-to-action [text] is cut in height (plan T-045): its box is lower than its line. Not
     * `didOverflowHeight`, which is also true for a long text cut with "…" across.
     */
    private fun ctaOverflows(text: String): Boolean {
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult].action!!(layouts)
        val layout = layouts.single()
        return layout.size.height < layout.multiParagraph.height
    }

    @Test
    fun full_partsKeepSize_insideCard_textsAndFontScales() {
        var fontScale by mutableStateOf(1f)
        var assets by mutableStateOf(sampleNativeAdAssets)
        composeRule.setContent {
            WithFontScale(fontScale) {
                AppTheme { NativeAdCard(assets, NativeAdSize.FULL, Modifier.width(CARD_WIDTH)) }
            }
        }
        listOf(1f, 1.3f, 2f).forEach { scale ->
            listOf(sampleNativeAdAssets, longFullNativeAdAssets).forEach { current ->
                fontScale = scale
                assets = current
                composeRule.waitForIdle()
                val case = "font scale $scale, ${if (current === sampleNativeAdAssets) "short" else "long"} texts"
                val card = bounds(NativeAdTestTags.CARD)
                val cta = bounds(NativeAdTestTags.CTA)
                val stars = bounds(NativeAdTestTags.STAR_ROW)
                val media = bounds(NativeAdTestTags.MEDIA)
                val badgeBounds = bounds(NativeAdTestTags.BADGE)
                assertEquals("card, $case", FULL_HEIGHT.value, (card.bottom - card.top).value, 0.5f)
                // Plan T-045: the call to action is 36 dp at least and grows with its one line of text.
                if (scale <= 1.3f) {
                    assertEquals("call to action, $case", 36f, (cta.bottom - cta.top).value, 0.5f)
                } else {
                    assertTrue("call to action, $case", (cta.bottom - cta.top).value >= 36f - 0.5f)
                    assertFalse("call to action text overflows, $case", ctaOverflows(current.callToAction!!))
                }
                assertEquals("star row, $case", 16f, (stars.bottom - stars.top).value, 0.5f)
                // 12 dp inner margin and 1 dp border below the star row.
                assertTrue(
                    "star row bottom ${stars.bottom}, card bottom ${card.bottom}, $case",
                    stars.bottom.value <= (card.bottom - 13.dp).value + 0.5f,
                )
                assertEquals("gap above the star row, $case", 12f, (stars.top - cta.bottom).value, 0.5f)
                assertTrue(
                    "media is ${media.bottom - media.top} high, $case",
                    (media.bottom - media.top).value >= 120f - 0.5f,
                )
                assertTrue(
                    "media top ${media.top}, label bottom ${badgeBounds.bottom}, $case",
                    media.top.value >= badgeBounds.bottom.value - 0.5f,
                )
                assertEquals("body lines, $case", 2, lineCount(current.body!!))
                if (scale == 1f) {
                    assertEquals("media at the design size, $case", 200f, (media.bottom - media.top).value, 0.5f)
                }
            }
        }
    }

    @Test
    fun full_mediaFloor120dp_cardGrows_atFontScale3() {
        composeRule.setContent {
            WithFontScale(3f) {
                AppTheme { NativeAdCard(longFullNativeAdAssets, NativeAdSize.FULL, Modifier.width(CARD_WIDTH)) }
            }
        }
        val card = bounds(NativeAdTestTags.CARD)
        val cta = bounds(NativeAdTestTags.CTA)
        val stars = bounds(NativeAdTestTags.STAR_ROW)
        val media = bounds(NativeAdTestTags.MEDIA)
        assertEquals("media", 120f, (media.bottom - media.top).value, 0.5f)
        // Plan T-045: the call to action is 36 dp at least and grows with its one line of text.
        assertTrue("call to action", (cta.bottom - cta.top).value >= 36f - 0.5f)
        assertFalse("call to action text overflows", ctaOverflows(longFullNativeAdAssets.callToAction!!))
        assertEquals("star row", 16f, (stars.bottom - stars.top).value, 0.5f)
        assertEquals("star row to card bottom", 13f, (card.bottom - stars.bottom).value, 0.5f)
        assertTrue("card is ${card.bottom - card.top} high", (card.bottom - card.top).value > FULL_HEIGHT.value + 0.5f)
    }

    @Test
    fun full_starRow_hiddenWhenZero_shownFilledWhenFour() {
        var assets by mutableStateOf(sampleNativeAdAssets.copy(starRating = 0.0))
        composeRule.setContent {
            AppTheme { NativeAdCard(assets, NativeAdSize.FULL, Modifier.width(CARD_WIDTH)) }
        }
        composeRule.onAllNodesWithTag(NativeAdTestTags.STAR_ROW, useUnmergedTree = true).assertCountEquals(0)
        composeRule.onAllNodesWithTag(NativeAdTestTags.STAR, useUnmergedTree = true).assertCountEquals(0)
        // Control: the card is drawn, so no star row is the rating, not a missing card.
        composeRule.onNodeWithTag(NativeAdTestTags.CARD).assertExists()

        assets = sampleNativeAdAssets.copy(starRating = 4.0)
        composeRule.waitForIdle()
        val row = composeRule.onNodeWithTag(NativeAdTestTags.STAR_ROW, useUnmergedTree = true).fetchSemanticsNode()
        assertEquals(listOf("4 of 5 stars"), row.config[SemanticsProperties.ContentDescription])
        composeRule.onAllNodesWithTag(NativeAdTestTags.STAR, useUnmergedTree = true).assertCountEquals(4)
    }

    @Test
    fun small_headlineAndCtaColors_lightAndDark() = assertColorsInBothModes(NativeAdSize.SMALL)

    @Test
    fun full_headlineAndCtaColors_lightAndDark() = assertColorsInBothModes(NativeAdSize.FULL)

    @Test
    fun medium_headlineAndCtaColors_lightAndDark() = assertColorsInBothModes(NativeAdSize.MEDIUM)

    @Test
    fun mediumPlaceholder_ctaIs44dp() {
        var ctaLarge by mutableStateOf(false)
        composeRule.setContent {
            AppTheme { NativeAdPlaceholder(NativeAdSize.MEDIUM, animated = false, ctaLarge = ctaLarge) }
        }
        composeRule.waitForIdle()
        // The CTA block of the medium placeholder has the 44 dp CTA of the medium ad (plan T-028; kept from the
        // control of CollapsibleNativeAdTest, plan T-030).
        composeRule.onNodeWithTag(NativeAdTestTags.PLACEHOLDER_CTA, useUnmergedTree = true).assertHeightIsEqualTo(44.dp)

        // With the large call to action (`cta_large`): 56 dp.
        ctaLarge = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(NativeAdTestTags.PLACEHOLDER_CTA, useUnmergedTree = true).assertHeightIsEqualTo(56.dp)
    }

    /**
     * In each mode, reads pixels of the drawn window: card and call-to-action backgrounds, then the text pixel
     * farthest from each background, against the tokens of that mode; text contrast at least 4.5:1 (KB-1).
     */
    private fun assertColorsInBothModes(size: NativeAdSize) {
        var dark by mutableStateOf(false)
        var scheme: ColorScheme? = null
        composeRule.setContent {
            AppTheme(darkTheme = dark) {
                scheme = MaterialTheme.colorScheme
                NativeAdCard(sampleNativeAdAssets, size, Modifier.width(CARD_WIDTH))
            }
        }
        listOf(false, true).forEach { mode ->
            dark = mode
            composeRule.waitForIdle()
            val colors = checkNotNull(scheme)
            val bitmap = windowBitmap()
            val card = windowBounds(NativeAdTestTags.CARD)
            val cta = windowBounds(NativeAdTestTags.CTA)
            val headline = windowBounds(NativeAdTestTags.HEADLINE)
            val cardBackground = bitmap.getPixel((card.left + px(6.dp)).toInt(), card.center.y.toInt())
            val ctaBackground = bitmap.getPixel((cta.left + px(6.dp)).toInt(), cta.center.y.toInt())
            assertColor("$size dark=$mode card", colors.surfaceContainerLow, cardBackground)
            assertColor("$size dark=$mode CTA", colors.tertiaryContainer, ctaBackground)

            val headlineText = farthestPixel(bitmap, headline, cardBackground)
            val ctaText = farthestPixel(bitmap, inset(cta, px(10.dp), px(4.dp)), ctaBackground)
            assertColor("$size dark=$mode headline text", colors.onSurface, headlineText)
            assertColor("$size dark=$mode CTA text", colors.onTertiaryContainer, ctaText)
            assertTrue("$size dark=$mode headline contrast", contrast(headlineText, cardBackground) >= 4.5)
            assertTrue("$size dark=$mode CTA contrast", contrast(ctaText, ctaBackground) >= 4.5)
        }
    }

    private fun windowBitmap(): Bitmap {
        val view = (composeRule as AndroidComposeTestRule<*, *>).activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        return bitmap
    }

    private fun windowBounds(tag: String): Rect =
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInWindow

    private fun px(dp: Dp): Float = with(composeRule.density) { dp.toPx() }

    private fun inset(rect: Rect, horizontal: Float, vertical: Float) =
        Rect(rect.left + horizontal, rect.top + vertical, rect.right - horizontal, rect.bottom - vertical)

    /** The pixel of [area] most unlike [background]: a pixel inside a letter stroke. */
    private fun farthestPixel(bitmap: Bitmap, area: Rect, background: Int): Int {
        var best = background
        var bestDistance = -1
        for (y in area.top.toInt() until area.bottom.toInt()) {
            for (x in area.left.toInt() until area.right.toInt()) {
                val pixel = bitmap.getPixel(x, y)
                val distance = channels(pixel).zip(channels(background)).sumOf { (a, b) -> abs(a - b) }
                if (distance > bestDistance) {
                    bestDistance = distance
                    best = pixel
                }
            }
        }
        return best
    }

    private fun assertColor(what: String, expected: Color, actual: Int) {
        val want = channels(expected.toArgb())
        val got = channels(actual)
        assertTrue(
            "$what: expected ${Integer.toHexString(expected.toArgb())}, was ${Integer.toHexString(actual)}",
            want.zip(got).all { (a, b) -> abs(a - b) <= COLOR_TOLERANCE },
        )
    }

    private fun channels(argb: Int) = listOf(argb shr 16 and 0xFF, argb shr 8 and 0xFF, argb and 0xFF)

    private fun contrast(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun luminance(argb: Int): Double {
        val (r, g, b) = channels(argb).map { value ->
            val c = value / 255.0
            if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private companion object {
        val CARD_WIDTH = 379.dp

        /**
         * The full native ad, fixed so the placeholder matches it: 1 + 24 top, head 40, media 200, body 40 (two
         * lines), call to action 36, star row 16, four 12 dp gaps between these five parts, 12 + 1 bottom.
         */
        val FULL_HEIGHT = 418.dp
        const val COLOR_TOLERANCE = 8
    }
}
