package com.vglstudio.camera.pose.line.guide.core.ads

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
            AdsTestTheme { NativeAdCard(assets = assets, size = size, modifier = Modifier.width(CARD_WIDTH)) }
        }
    }

    private fun bounds(tag: String) = composeRule.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun textStyle(text: String): TextStyle = textLayout(text).layoutInput.style

    private fun textLayout(text: String): TextLayoutResult {
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult].action!!(layouts)
        return layouts.single()
    }

    private fun DpRect.h(): Float = (bottom - top).value

    private fun DpRect.w(): Float = (right - left).value

    private fun DpRect.centerY(): Float = (top.value + bottom.value) / 2

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
    fun small_iconIs44dp() {
        setCard(NativeAdSize.SMALL)
        composeRule.onNodeWithTag(NativeAdTestTags.ICON, useUnmergedTree = true)
            .assertWidthIsEqualTo(44.dp)
            .assertHeightIsEqualTo(44.dp)
    }

    @Test
    fun small_ctaIs44dpHighWith18spText() {
        setCard(NativeAdSize.SMALL)
        composeRule.onNodeWithTag(NativeAdTestTags.CTA, useUnmergedTree = true).assertHeightIsEqualTo(44.dp)
        assertEquals(18.sp, textStyle("Install").fontSize)
    }

    @Test
    fun small_badgeTextIs13sp_beforeHeadline() {
        setCard(NativeAdSize.SMALL)
        assertEquals(13.sp, textStyle(badge).fontSize)
        val headline = composeRule.onNodeWithText("Headline of the ad", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val label = bounds(NativeAdTestTags.BADGE)
        val advertiser = composeRule.onNodeWithText("Advertiser or short body", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        // The label stands before the headline on its row (design T-064); the description line is below them.
        assertTrue("label right ${label.right}, headline left ${headline.left}", label.right <= headline.left)
        assertTrue("label on the headline's row", label.top < headline.bottom && label.bottom > headline.top)
        assertTrue(
            "description top ${advertiser.top}, headline bottom ${headline.bottom}",
            advertiser.top >= headline.bottom,
        )
        assertEquals("description starts at the label's start", label.left.value, advertiser.left.value, 0.5f)
    }

    @Test
    fun small_is126dpHigh() {
        setCard(NativeAdSize.SMALL)
        composeRule.onNodeWithTag(NativeAdTestTags.CARD).assertHeightIsEqualTo(126.dp)
    }

    @Test
    fun medium_is400dpHigh() {
        setCard(NativeAdSize.MEDIUM)
        composeRule.onNodeWithTag(NativeAdTestTags.CARD).assertHeightIsEqualTo(400.dp)
    }

    @Test
    fun medium_ctaIs44dp_18sp() {
        var labelLarge: TextStyle? = null
        composeRule.setContent {
            AdsTestTheme {
                labelLarge = MaterialTheme.typography.labelLarge
                NativeAdCard(sampleNativeAdAssets, NativeAdSize.MEDIUM, Modifier.width(CARD_WIDTH))
            }
        }
        composeRule.onNodeWithTag(NativeAdTestTags.CTA, useUnmergedTree = true).assertHeightIsEqualTo(44.dp)
        val style = textStyle("Install")
        assertEquals(18.sp, style.fontSize)
        assertEquals(labelLarge!!.fontWeight, style.fontWeight)
    }

    @Test
    fun medium_starRow16dp_belowBadgeLine_hiddenStarsKeepHeight() {
        var assets by mutableStateOf(sampleNativeAdAssets.copy(starRating = 4.0))
        composeRule.setContent {
            AdsTestTheme { NativeAdCard(assets, NativeAdSize.MEDIUM, Modifier.width(CARD_WIDTH)) }
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
        composeRule.onNodeWithTag(NativeAdTestTags.CARD).assertHeightIsEqualTo(400.dp)
        assertEquals(mediaTop.value, bounds(NativeAdTestTags.MEDIA).top.value, 0.5f)
    }

    @Test
    fun medium_ctaLarge_56dp_17sp_cardIs400dp() {
        var large by mutableStateOf(true)
        composeRule.setContent {
            AdsTestTheme {
                NativeAdCard(sampleNativeAdAssets, NativeAdSize.MEDIUM, Modifier.width(CARD_WIDTH), ctaLarge = large)
            }
        }
        composeRule.onNodeWithTag(NativeAdTestTags.CTA, useUnmergedTree = true).assertHeightIsEqualTo(56.dp)
        assertEquals(17.sp, textStyle("Install").fontSize)
        composeRule.onNodeWithTag(NativeAdTestTags.CARD).assertHeightIsEqualTo(400.dp)
        // The text of the SDK is kept as it is, never upper-cased.
        composeRule.onNodeWithText("Install", useUnmergedTree = true).assertExists()

        // Control: the same ad without cta_large.
        large = false
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(NativeAdTestTags.CTA, useUnmergedTree = true).assertHeightIsEqualTo(44.dp)
        composeRule.onNodeWithTag(NativeAdTestTags.CARD).assertHeightIsEqualTo(400.dp)
    }

    @Test
    fun card_iconIs32dp_headline13sp_mediaFillsTop_ctaIs36dp() {
        composeRule.setContent {
            AdsTestTheme { NativeAdCard(sampleNativeAdAssets, NativeAdSize.CARD, Modifier.size(180.dp, 300.dp)) }
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
            AdsTestTheme { NativeAdCard(sampleNativeAdAssets, NativeAdSize.CARD, Modifier.size(180.dp, 300.dp)) }
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
            AdsTestTheme {
                Box(Modifier.size(160.dp, 240.dp)) { NativeAdCard(sampleNativeAdAssets, NativeAdSize.CARD) }
            }
        }
        val card = bounds(NativeAdTestTags.CARD)
        assertEquals(160f, (card.right - card.left).value, 0.5f)
        assertEquals(240f, (card.bottom - card.top).value, 0.5f)
    }

    @Test
    fun card_cardHeight_setsCardAndPlaceholderHeight() {
        var size by mutableStateOf(NativeAdSize.CARD)
        composeRule.setContent {
            AdsTestTheme {
                Column {
                    NativeAdCard(sampleNativeAdAssets, size, Modifier.width(CARD_WIDTH), cardHeight = APP_CARD_HEIGHT)
                    NativeAdPlaceholder(
                        size,
                        Modifier.width(CARD_WIDTH),
                        animated = false,
                        cardHeight = APP_CARD_HEIGHT,
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(NativeAdTestTags.CARD).assertHeightIsEqualTo(APP_CARD_HEIGHT)
        composeRule.onNodeWithTag(NativeAdTestTags.PLACEHOLDER).assertHeightIsEqualTo(APP_CARD_HEIGHT)

        // Control: another size keeps its own height, whatever cardHeight says.
        size = NativeAdSize.MEDIUM
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(NativeAdTestTags.CARD).assertHeightIsEqualTo(400.dp)
        composeRule.onNodeWithTag(NativeAdTestTags.PLACEHOLDER).assertHeightIsEqualTo(400.dp)
    }

    @Test
    fun placeholder_matchesCardHeight_forEachSize() {
        var size by mutableStateOf(NativeAdSize.SMALL)
        composeRule.setContent {
            AdsTestTheme {
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
                NativeAdSize.SMALL -> 126.dp
                NativeAdSize.MEDIUM -> 400.dp
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
            AdsTestTheme { NativeAdPlaceholder(NativeAdSize.STEP, Modifier.width(CARD_WIDTH), animated = false) }
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
            AdsTestTheme { NativeAdCard(long.copy(advertiser = null), size, Modifier.width(CARD_WIDTH)) }
        }
        composeRule.onNodeWithTag(NativeAdTestTags.CARD).assertHeightIsEqualTo(126.dp)
        composeRule.onNodeWithTag(NativeAdTestTags.HEADLINE, useUnmergedTree = true).assertHeightIsEqualTo(22.dp)

        size = NativeAdSize.MEDIUM
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(NativeAdTestTags.CARD).assertHeightIsEqualTo(400.dp)
        composeRule.onNodeWithTag(NativeAdTestTags.HEADLINE, useUnmergedTree = true).assertHeightIsEqualTo(22.dp)
    }

    /**
     * KB-1 (plan T-060): a long call to action of small stops at 120 dp, so the headline keeps room; a short one
     * ("Install", 18 sp) is as wide as its text, 83.33 dp, between the 80 dp least width and the 120 dp ceiling, and
     * shows whole (plan T-065, question 4).
     */
    @Test
    fun small_ctaAtMost120dpWide_shortCtaKeepsWidth() {
        var assets by mutableStateOf(longFullNativeAdAssets)
        composeRule.setContent {
            AdsTestTheme { NativeAdCard(assets, NativeAdSize.SMALL, Modifier.width(CARD_WIDTH)) }
        }
        val cta = bounds(NativeAdTestTags.CTA)
        val headline = bounds(NativeAdTestTags.HEADLINE)
        val measured = "CTA ${(cta.right - cta.left).value} dp, headline ${(headline.right - headline.left).value} dp"
        assertEquals("long call to action width; $measured", 120f, (cta.right - cta.left).value, 0.5f)
        assertTrue("headline has no width; $measured", (headline.right - headline.left).value > 0f)

        assets = sampleNativeAdAssets
        composeRule.waitForIdle()
        val shortCta = bounds(NativeAdTestTags.CTA)
        assertEquals("short call to action width", 83.33f, (shortCta.right - shortCta.left).value, 0.5f)
        val install = textLayout("Install")
        assertEquals("Install shown whole", "Install".length, install.getLineEnd(0, visibleEnd = true))
        assertFalse("Install cut with an ellipsis", install.isLineEllipsized(0))
    }

    @Test
    fun medium_hasMediaOfAtLeast120dp() {
        setCard(NativeAdSize.MEDIUM)
        val card = bounds(NativeAdTestTags.CARD)
        val media = bounds(NativeAdTestTags.MEDIA)
        assertEquals(CARD_WIDTH.value, (card.right - card.left).value, 0.5f)
        assertTrue("Media is ${media.right - media.left} wide", media.right - media.left >= 355.dp - 0.5.dp)
        assertEquals(182f, (media.bottom - media.top).value, 0.5f)
        assertTrue(media.right - media.left >= 120.dp && media.bottom - media.top >= 120.dp)
        // Below the 60 dp head with its star row: 1 + 24 + 60 + 12 dp from the top of the card (the card at y 286,
        // the media at y 383 in design/context/AdGalleryScreen_default_light.md).
        assertEquals(97f, (media.top - card.top).value, 0.5f)
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
            AdsTestTheme {
                text = stringResource(R.string.ads_native_badge)
                labelSmall = MaterialTheme.typography.labelSmall
                NativeAdCard(sampleNativeAdAssets, NativeAdSize.SMALL, Modifier.width(CARD_WIDTH))
            }
        }
        assertEquals("Ad", text)
        val style = textStyle("Ad")
        // labelSmall at the 13 sp of the redrawn design (design T-064, section 1; plan T-065, question 10).
        assertEquals(13.sp, style.fontSize)
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
            AdsTestTheme {
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
                AdsTestTheme { NativeAdCard(assets, NativeAdSize.FULL, Modifier.width(CARD_WIDTH)) }
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
                AdsTestTheme { NativeAdCard(longFullNativeAdAssets, NativeAdSize.FULL, Modifier.width(CARD_WIDTH)) }
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
            AdsTestTheme { NativeAdCard(assets, NativeAdSize.FULL, Modifier.width(CARD_WIDTH)) }
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
            AdsTestTheme { NativeAdPlaceholder(NativeAdSize.MEDIUM, animated = false, ctaLarge = ctaLarge) }
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
            AdsTestTheme(darkTheme = dark) {
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

    // Sizes that follow the screen (KB-32, design T-064; plan T-065).

    /**
     * KB-1 criterion 2, KB-32: the small native and its placeholder are as high as the bottom slot of the screen
     * width, whatever the card's own width: 126 dp at 411 dp, also for a 379 dp card.
     */
    @Test
    fun small_heightFollowsScreenWidth() = assertSmallHeight(126f)

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun small_heightFollowsScreenWidth_360() = assertSmallHeight(106f)

    @Test
    @Config(qualifiers = "w673dp-h841dp-xxhdpi")
    fun small_heightFollowsScreenWidth_673() = assertSmallHeight(200f)

    /**
     * KB-1 criterion 2, KB-32 criterion 3 (changed 2026-11-16): a phone turned landscape keeps the portrait slot, the
     * height following the short edge of the window: 126 dp at 891 x 411 dp (200 dp by the width), the card 859 dp
     * wide.
     */
    @Test
    @Config(qualifiers = "w891dp-h411dp-xxhdpi")
    fun small_landscape_followsShortEdge() {
        assertSmallHeight(126f)
        assertEquals("card width", 859f, bounds(NativeAdTestTags.CARD).w(), 0.5f)
    }

    /** As above on a 640 x 360 dp window: 106 dp (200 dp by the width). */
    @Test
    @Config(qualifiers = "w640dp-h360dp-xxhdpi")
    fun small_landscape360_followsShortEdge() = assertSmallHeight(106f)

    private fun assertSmallHeight(expected: Float) {
        composeRule.setContent {
            AdsTestTheme {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    NativeAdCard(sampleNativeAdAssets, NativeAdSize.SMALL)
                    NativeAdPlaceholder(NativeAdSize.SMALL, animated = false)
                }
            }
        }
        composeRule.waitForIdle()
        val card = bounds(NativeAdTestTags.CARD)
        assertEquals("card height, card ${card.w()} dp wide", expected, card.h(), 0.5f)
        assertEquals("placeholder height", expected, bounds(NativeAdTestTags.PLACEHOLDER).h(), 0.5f)
    }

    /** Design T-064 (design/context/AdGalleryScreen_default_light.md, "Native/Small"), KB-1 criterion 1. */
    @Test
    fun small_jbaseLayout() {
        var assets by mutableStateOf(sampleNativeAdAssets)
        composeRule.setContent {
            AdsTestTheme { NativeAdCard(assets, NativeAdSize.SMALL, Modifier.width(CARD_WIDTH)) }
        }
        val card = bounds(NativeAdTestTags.CARD)
        val icon = bounds(NativeAdTestTags.ICON)
        val cta = bounds(NativeAdTestTags.CTA)
        val label = bounds(NativeAdTestTags.BADGE)
        val headline = bounds(NativeAdTestTags.HEADLINE)
        val second = bounds(NativeAdTestTags.SECOND_LINE)
        assertEquals("icon width", 44f, icon.w(), 0.5f)
        assertEquals("icon height", 44f, icon.h(), 0.5f)
        assertEquals("icon start margin", 12f, (icon.left - card.left).value, 0.5f)
        assertEquals("CTA height", 44f, cta.h(), 0.5f)
        assertEquals("CTA text size", 18.sp, textStyle("Install").fontSize)
        assertEquals("CTA end margin", 24f, (card.right - cta.right).value, 0.5f)
        assertTrue("label right ${label.right}, headline left ${headline.left}", label.right <= headline.left)
        assertTrue("label and headline centers", abs(label.centerY() - headline.centerY()) < 2f)
        assertEquals("headline text size", 16.sp, textStyle(sampleNativeAdAssets.headline).fontSize)
        assertEquals("headline lines", 1, textLayout(sampleNativeAdAssets.headline).lineCount)
        assertEquals("headline height", 22f, headline.h(), 0.5f)
        // Question 9: the line below shows the advertiser, the body without one.
        val advertiserStyle = textStyle(sampleNativeAdAssets.advertiser!!)
        assertEquals("second line text size", 13.sp, advertiserStyle.fontSize)
        assertEquals("second line height of a line", 18.sp, advertiserStyle.lineHeight)
        assertEquals("one advertiser line", 18f, second.h(), 0.5f)
        // Icon, text column and call to action centered in the card's height.
        val column = DpRect(headline.left, min(label.top.value, headline.top.value).dp, second.right, second.bottom)
        listOf("icon" to icon, "text column" to column, "CTA" to cta).forEach { (part, rect) ->
            assertEquals("$part centered", card.centerY(), rect.centerY(), 1f)
        }

        assets = sampleNativeAdAssets.copy(advertiser = null, body = longFullNativeAdAssets.body)
        composeRule.waitForIdle()
        val body = textLayout(longFullNativeAdAssets.body!!)
        assertEquals("body at most two lines", 2, body.lineCount)
        assertEquals("two body lines", 36f, bounds(NativeAdTestTags.SECOND_LINE).h(), 0.5f)
    }

    @Test
    fun smallPlaceholder_jbaseBlocks() {
        composeRule.setContent {
            AdsTestTheme { NativeAdPlaceholder(NativeAdSize.SMALL, Modifier.width(CARD_WIDTH), animated = false) }
        }
        val icon = bounds(NativeAdTestTags.PLACEHOLDER_ICON)
        val cta = bounds(NativeAdTestTags.PLACEHOLDER_CTA)
        assertEquals("icon block width", 44f, icon.w(), 0.5f)
        assertEquals("icon block height", 44f, icon.h(), 0.5f)
        assertEquals("CTA block width", 96f, cta.w(), 0.5f)
        assertEquals("CTA block height", 44f, cta.h(), 0.5f)
    }

    /** KB-2, KB-32: medium is as high as the screen height says: 400 dp at 891 dp, also with `cta_large`. */
    @Test
    fun medium_heightFollowsScreenHeight() {
        var large by mutableStateOf(false)
        composeRule.setContent {
            AdsTestTheme {
                NativeAdCard(sampleNativeAdAssets, NativeAdSize.MEDIUM, Modifier.width(CARD_WIDTH), ctaLarge = large)
            }
        }
        assertEquals("medium", 400f, bounds(NativeAdTestTags.CARD).h(), 0.5f)
        large = true
        composeRule.waitForIdle()
        assertEquals("medium cta_large", 400f, bounds(NativeAdTestTags.CARD).h(), 0.5f)
    }

    /** The unfolded frame, 841 dp high: 384 dp, the layout of the design (body of two lines). */
    @Test
    @Config(qualifiers = "w673dp-h841dp-xxhdpi")
    fun medium_heightFollowsScreenHeight_841() {
        composeRule.setContent {
            AdsTestTheme {
                Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    NativeAdCard(longFullNativeAdAssets, NativeAdSize.MEDIUM)
                }
            }
        }
        assertEquals("medium", 384f, bounds(NativeAdTestTags.CARD).h(), 0.5f)
        assertEquals("body lines", 2, textLayout(longFullNativeAdAssets.body!!).lineCount)
    }

    /**
     * KB-2 criterion 1, KB-32, question 7 (b) of the plan: on a 640 dp screen medium keeps 320 dp by a one-line body
     * and 8 dp gaps, so its media keeps more than 120 dp (134 dp, 122 dp with `cta_large`). Numbers from the design:
     * 25 + 60 + 20 + 44 (56) + 3 x 8 + 13 dp of fixed parts.
     */
    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun medium_compact_lowScreen() {
        var large by mutableStateOf(false)
        var assets by mutableStateOf(longFullNativeAdAssets)
        composeRule.setContent {
            AdsTestTheme {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    NativeAdCard(assets, NativeAdSize.MEDIUM, ctaLarge = large)
                    NativeAdPlaceholder(NativeAdSize.MEDIUM, animated = false, ctaLarge = large)
                }
            }
        }
        listOf(false, true).forEach { current ->
            large = current
            assets = longFullNativeAdAssets
            composeRule.waitForIdle()
            val case = if (current) "cta_large" else "medium"
            val card = bounds(NativeAdTestTags.CARD)
            val media = bounds(NativeAdTestTags.MEDIA)
            val body = bounds(NativeAdTestTags.BODY)
            val cta = bounds(NativeAdTestTags.CTA)
            val stars = bounds(NativeAdTestTags.STAR_ROW)
            val headline = bounds(NativeAdTestTags.HEADLINE)
            assertEquals("card, $case", 320f, card.h(), 0.5f)
            assertEquals("media, $case", if (current) 122f else 134f, media.h(), 0.5f)
            assertEquals("body lines, $case", 1, textLayout(longFullNativeAdAssets.body!!).lineCount)
            assertEquals("body frame, $case", 20f, body.h(), 0.5f)
            assertEquals("star row to media, $case", 8f, (media.top - stars.bottom).value, 0.5f)
            assertEquals("media to body, $case", 8f, (body.top - media.bottom).value, 0.5f)
            assertEquals("body to CTA, $case", 8f, (cta.top - body.bottom).value, 0.5f)
            assertEquals("star row, $case", 16f, stars.h(), 0.5f)
            assertEquals("head, $case", 60f, (stars.bottom - headline.top).value, 0.5f)
            assertEquals("CTA to card bottom, $case", 13f, (card.bottom - cta.bottom).value, 0.5f)
            assertEquals("placeholder, $case", 320f, bounds(NativeAdTestTags.PLACEHOLDER).h(), 0.5f)

            assets = longFullNativeAdAssets.copy(body = null)
            composeRule.waitForIdle()
            assertEquals("kept body space, $case", 20f, bounds(NativeAdTestTags.BODY).h(), 0.5f)
            assertEquals("card without body, $case", 320f, bounds(NativeAdTestTags.CARD).h(), 0.5f)
        }
    }

    /** Design T-064 (design/context/AdGalleryScreen_default_light.md, "Native/Medium"), KB-2. */
    @Test
    fun medium_jbaseLayout() {
        var large by mutableStateOf(false)
        var assets by mutableStateOf(sampleNativeAdAssets)
        composeRule.setContent {
            AdsTestTheme { NativeAdCard(assets, NativeAdSize.MEDIUM, Modifier.width(CARD_WIDTH), ctaLarge = large) }
        }
        val card = bounds(NativeAdTestTags.CARD)
        val icon = bounds(NativeAdTestTags.ICON)
        val label = bounds(NativeAdTestTags.BADGE)
        val headline = bounds(NativeAdTestTags.HEADLINE)
        assertEquals("icon width", 44f, icon.w(), 0.5f)
        assertEquals("icon height", 44f, icon.h(), 0.5f)
        assertTrue("label right ${label.right}, headline left ${headline.left}", label.right <= headline.left)
        assertTrue("label and headline centers", abs(label.centerY() - headline.centerY()) < 2f)
        assertEquals("headline height", 22f, headline.h(), 0.5f)
        assertEquals("advertiser line", 18f, bounds(NativeAdTestTags.SECOND_LINE).h(), 0.5f)
        assertEquals("CTA height", 44f, bounds(NativeAdTestTags.CTA).h(), 0.5f)
        assertEquals("CTA text size", 18.sp, textStyle("Install").fontSize)
        val media = bounds(NativeAdTestTags.MEDIA)
        assertEquals("media", 182f, media.h(), 0.5f)
        assertEquals("media top", 97f, (media.top - card.top).value, 0.5f)
        composeRule.onAllNodesWithTag(NativeAdTestTags.STAR, useUnmergedTree = true).assertCountEquals(4)

        large = true
        composeRule.waitForIdle()
        assertEquals("media cta_large", 170f, bounds(NativeAdTestTags.MEDIA).h(), 0.5f)

        large = false
        assets = sampleNativeAdAssets.copy(advertiser = null)
        composeRule.waitForIdle()
        val cardTop = bounds(NativeAdTestTags.CARD).top
        val kept = bounds(NativeAdTestTags.MEDIA)
        assertEquals("advertiser space kept", 18f, bounds(NativeAdTestTags.SECOND_LINE).h(), 0.5f)
        assertEquals("media top without advertiser", 97f, (kept.top - cardTop).value, 0.5f)
        assertEquals("media without advertiser", 182f, kept.h(), 0.5f)
    }

    private companion object {
        val CARD_WIDTH = 379.dp

        /** The height of the app's card next to an ad card, other than the 300 dp default. */
        val APP_CARD_HEIGHT = 420.dp

        /**
         * The full native ad, fixed so the placeholder matches it: 1 + 24 top, head 40, media 200, body 40 (two
         * lines), call to action 36, star row 16, four 12 dp gaps between these five parts, 12 + 1 bottom.
         */
        val FULL_HEIGHT = 418.dp
        const val COLOR_TOLERANCE = 8
    }
}
