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

    /**
     * Plan T-109, section 3.5: the top row (the headline) ends at least 28 dp from the card's end edge (12 dp margin
     * and the 16 dp of AdChoices), and the call to action starts below the AdChoices corner (16 + 4 dp).
     */
    @Test
    fun small_leavesAdChoicesCornerFree() {
        setCard(NativeAdSize.SMALL)
        val card = bounds(NativeAdTestTags.CARD)
        val cta = bounds(NativeAdTestTags.CTA)
        val headline = bounds(NativeAdTestTags.HEADLINE)
        assertEquals(CARD_WIDTH.value, (card.right - card.left).value, 0.5f)
        assertTrue(
            "headline ends ${card.right - headline.right} from the card edge",
            card.right - headline.right >= 28.dp,
        )
        assertTrue("CTA starts ${cta.top - card.top} below the card top", cta.top - card.top >= 20.dp)
        // Control: the headline reaches that margin, so the free corner is not only a narrow headline.
        assertEquals("headline end margin", 28f, (card.right - headline.right).value, 0.5f)
    }

    /**
     * Plan T-110, section 3.5 (design T-108, `Native/Medium`): the AdChoices corner (16 x 16 dp, 4 dp from the top and
     * the end of the card) lies on the media, over no text, star or call to action.
     */
    @Test
    fun medium_leavesAdChoicesCornerFree() {
        setCard(NativeAdSize.MEDIUM)
        val card = bounds(NativeAdTestTags.CARD)
        val media = bounds(NativeAdTestTags.MEDIA)
        val corner = DpRect(card.right - 20.dp, card.top + 4.dp, card.right - 4.dp, card.top + 20.dp)
        // Control: the marker of the fake ad is drawn in that corner, so the corner checked is where AdChoices is.
        val marker = bounds(NativeAdTestTags.AD_CHOICES_AREA)
        assertEquals("marker left", corner.left.value, marker.left.value, 0.5f)
        assertEquals("marker top", corner.top.value, marker.top.value, 0.5f)
        assertTrue(
            "AdChoices corner $corner on the media $media",
            corner.left.value >= media.left.value - 0.5f &&
                corner.right.value <= media.right.value + 0.5f &&
                corner.top.value >= media.top.value - 0.5f &&
                corner.bottom.value <= media.bottom.value + 0.5f,
        )
        listOf(
            NativeAdTestTags.ICON,
            NativeAdTestTags.HEADLINE,
            NativeAdTestTags.BADGE,
            NativeAdTestTags.SECOND_LINE,
            NativeAdTestTags.STAR_ROW,
            NativeAdTestTags.CTA,
        ).forEach { tag ->
            val part = bounds(tag)
            val overlaps = part.left < corner.right &&
                part.right > corner.left &&
                part.top < corner.bottom &&
                part.bottom > corner.top
            assertFalse("$tag $part overlaps the AdChoices corner $corner", overlaps)
        }
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
    fun small_is106dpHigh() {
        setCard(NativeAdSize.SMALL)
        composeRule.onNodeWithTag(NativeAdTestTags.CARD).assertHeightIsEqualTo(106.dp)
    }

    @Test
    fun medium_is250dpHigh() {
        setCard(NativeAdSize.MEDIUM)
        composeRule.onNodeWithTag(NativeAdTestTags.CARD).assertHeightIsEqualTo(250.dp)
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

    /**
     * Plan T-110, section 3.5 (design T-108): the four stars end the line below the headline, 16 dp high on that
     * line, 6 dp after its text, 12 dp from the card's end. A rating of 0 draws no star and nothing moves (L-055).
     */
    @Test
    fun medium_starsEndOfSecondLine_noRatingKeepsPlace() {
        var assets by mutableStateOf(sampleNativeAdAssets.copy(starRating = 4.0))
        composeRule.setContent {
            AdsTestTheme { NativeAdCard(assets, NativeAdSize.MEDIUM, Modifier.width(CARD_WIDTH)) }
        }
        composeRule.onAllNodesWithTag(NativeAdTestTags.STAR, useUnmergedTree = true).assertCountEquals(4)
        val card = bounds(NativeAdTestTags.CARD)
        val row = bounds(NativeAdTestTags.STAR_ROW)
        val second = bounds(NativeAdTestTags.SECOND_LINE)
        assertEquals(16f, (row.bottom - row.top).value, 0.5f)
        assertTrue("stars on the line below the headline", abs(row.centerY() - second.centerY()) < 1f)
        assertTrue("stars after the text: ${row.left}, ${second.right}", row.left >= second.right)
        assertEquals("text to stars", 6f, (row.left - second.right).value, 0.5f)
        assertEquals("stars to the card end", 12f, (card.right - row.right).value, 0.5f)
        val mediaTop = bounds(NativeAdTestTags.MEDIA).top
        val ctaTop = bounds(NativeAdTestTags.CTA).top

        // L-055: a rating of 0 draws no star, yet the card and its parts keep their place.
        assets = sampleNativeAdAssets.copy(starRating = 0.0)
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag(NativeAdTestTags.STAR, useUnmergedTree = true).assertCountEquals(0)
        composeRule.onNodeWithTag(NativeAdTestTags.CARD).assertHeightIsEqualTo(250.dp)
        assertEquals(mediaTop.value, bounds(NativeAdTestTags.MEDIA).top.value, 0.5f)
        assertEquals(ctaTop.value, bounds(NativeAdTestTags.CTA).top.value, 0.5f)
    }

    @Test
    fun medium_ctaLarge_56dp_17sp_cardIs250dp() {
        var large by mutableStateOf(true)
        composeRule.setContent {
            AdsTestTheme {
                NativeAdCard(sampleNativeAdAssets, NativeAdSize.MEDIUM, Modifier.width(CARD_WIDTH), ctaLarge = large)
            }
        }
        composeRule.onNodeWithTag(NativeAdTestTags.CTA, useUnmergedTree = true).assertHeightIsEqualTo(56.dp)
        assertEquals(17.sp, textStyle("Install").fontSize)
        composeRule.onNodeWithTag(NativeAdTestTags.CARD).assertHeightIsEqualTo(250.dp)
        assertEquals("media cta_large", 132f, bounds(NativeAdTestTags.MEDIA).h(), 0.5f)
        // The text of the SDK is kept as it is, never upper-cased.
        composeRule.onNodeWithText("Install", useUnmergedTree = true).assertExists()

        // Control: the same ad without cta_large.
        large = false
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(NativeAdTestTags.CTA, useUnmergedTree = true).assertHeightIsEqualTo(44.dp)
        composeRule.onNodeWithTag(NativeAdTestTags.CARD).assertHeightIsEqualTo(250.dp)
        assertEquals("media", 144f, bounds(NativeAdTestTags.MEDIA).h(), 0.5f)
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
        composeRule.onNodeWithTag(NativeAdTestTags.CARD).assertHeightIsEqualTo(250.dp)
        composeRule.onNodeWithTag(NativeAdTestTags.PLACEHOLDER).assertHeightIsEqualTo(250.dp)
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
                NativeAdSize.SMALL -> 106.dp
                NativeAdSize.MEDIUM -> 250.dp
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
        composeRule.onNodeWithTag(NativeAdTestTags.CARD).assertHeightIsEqualTo(106.dp)
        composeRule.onNodeWithTag(NativeAdTestTags.HEADLINE, useUnmergedTree = true).assertHeightIsEqualTo(22.dp)

        size = NativeAdSize.MEDIUM
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(NativeAdTestTags.CARD).assertHeightIsEqualTo(250.dp)
        composeRule.onNodeWithTag(NativeAdTestTags.HEADLINE, useUnmergedTree = true).assertHeightIsEqualTo(22.dp)
    }

    /**
     * KB-1, O-08 (plan T-109, section 3.5): the call to action of small is as wide as the card less its two 12 dp
     * margins, 355 dp in a 379 dp card, with a short and a long text; "Install" shows whole, a long text is cut with
     * "…" and shows at least one character; the headline keeps room.
     */
    @Test
    fun small_ctaFullWidth_longCtaEllipsized() {
        // Longer than the 331 dp a 355 dp call to action holds at 18 sp (the long fixture's text fits it whole).
        val longCta = "Install now and get the full version of the app with every feature for free"
        var assets by mutableStateOf(longFullNativeAdAssets.copy(callToAction = longCta))
        composeRule.setContent {
            AdsTestTheme { NativeAdCard(assets, NativeAdSize.SMALL, Modifier.width(CARD_WIDTH)) }
        }
        val cta = bounds(NativeAdTestTags.CTA)
        val headline = bounds(NativeAdTestTags.HEADLINE)
        val measured = "CTA ${(cta.right - cta.left).value} dp, headline ${(headline.right - headline.left).value} dp"
        assertEquals("long call to action width; $measured", 355f, (cta.right - cta.left).value, 0.5f)
        assertTrue("headline has no width; $measured", (headline.right - headline.left).value > 0f)
        val long = textLayout(longCta)
        assertTrue("long call to action not cut with an ellipsis", long.isLineEllipsized(0))
        assertTrue("long call to action shows no character", long.getLineEnd(0, visibleEnd = true) >= 1)

        assets = sampleNativeAdAssets
        composeRule.waitForIdle()
        val shortCta = bounds(NativeAdTestTags.CTA)
        assertEquals("short call to action width", 355f, (shortCta.right - shortCta.left).value, 0.5f)
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
        assertEquals(144f, (media.bottom - media.top).value, 0.5f)
        assertTrue(media.right - media.left >= 120.dp && media.bottom - media.top >= 120.dp)
        // Design T-108 ("Media" 379 x 144 dp at the top of "Native/Medium" in
        // design/context/AdGalleryScreen_default_light.md): the media starts at the card's top, as wide as the card.
        assertEquals(0f, (media.top - card.top).value, 0.5f)
        assertEquals("media width", CARD_WIDTH.value, media.w(), 0.5f)
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
     * KB-2 criterion 2 (plan T-110, section 4): the medium placeholder has the shape of the card, a media block of
     * 144 dp (132 dp with `cta_large`) from its top, then the two tiers of small (a 44 dp icon block, a call-to-action
     * block 355 dp wide, 44 dp or 56 dp high); at font scale 2.0 it is as high as the card.
     */
    @Test
    fun mediumPlaceholder_twoTier_matchesCard() {
        var large by mutableStateOf(false)
        var fontScale by mutableStateOf(1f)
        composeRule.setContent {
            WithFontScale(fontScale) {
                AdsTestTheme {
                    Column {
                        NativeAdCard(
                            sampleNativeAdAssets,
                            NativeAdSize.MEDIUM,
                            Modifier.width(CARD_WIDTH),
                            ctaLarge = large,
                        )
                        NativeAdPlaceholder(
                            NativeAdSize.MEDIUM,
                            Modifier.width(CARD_WIDTH),
                            animated = false,
                            ctaLarge = large,
                        )
                    }
                }
            }
        }
        listOf(false, true).forEach { current ->
            large = current
            fontScale = 1f
            composeRule.waitForIdle()
            val case = if (current) "cta_large" else "medium"
            val placeholder = bounds(NativeAdTestTags.PLACEHOLDER)
            val media = bounds(NativeAdTestTags.PLACEHOLDER_MEDIA)
            val icon = bounds(NativeAdTestTags.PLACEHOLDER_ICON)
            val cta = bounds(NativeAdTestTags.PLACEHOLDER_CTA)
            assertEquals("placeholder, $case", 250f, placeholder.h(), 0.5f)
            assertEquals("media block top, $case", 0f, (media.top - placeholder.top).value, 0.5f)
            assertEquals("media block width, $case", CARD_WIDTH.value, media.w(), 0.5f)
            assertEquals("media block, $case", if (current) 132f else 144f, media.h(), 0.5f)
            assertEquals("icon block width, $case", 44f, icon.w(), 0.5f)
            assertEquals("icon block height, $case", 44f, icon.h(), 0.5f)
            assertEquals("icon block below the media, $case", 6f, (icon.top - media.bottom).value, 0.5f)
            assertEquals("CTA block width, $case", 355f, cta.w(), 0.5f)
            assertEquals("CTA block height, $case", if (current) 56f else 44f, cta.h(), 0.5f)
            assertEquals("CTA block to the bottom, $case", 6f, (placeholder.bottom - cta.bottom).value, 0.5f)

            fontScale = 2f
            composeRule.waitForIdle()
            val card = bounds(NativeAdTestTags.CARD)
            assertTrue("card grows at font scale 2.0, $case: ${card.h()}", card.h() > 250f + 0.5f)
            assertEquals(
                "placeholder at font scale 2.0, $case",
                card.h(),
                bounds(NativeAdTestTags.PLACEHOLDER).h(),
                0.5f,
            )
        }
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
            // Medium (plan T-110, section 3.5): the middle of the card is on the media, so the background is read on
            // the content block, at the headline's height; the control checks that point is below the media.
            val backgroundY = if (size == NativeAdSize.MEDIUM) headline.center.y else card.center.y
            if (size == NativeAdSize.MEDIUM) {
                val mediaBottom = windowBounds(NativeAdTestTags.MEDIA).bottom
                assertTrue("background point $backgroundY below the media $mediaBottom", backgroundY > mediaBottom)
            }
            val cardBackground = bitmap.getPixel((card.left + px(6.dp)).toInt(), backgroundY.toInt())
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
     * KB-1 criterion 2, KB-32 criterion 3 (question 66, plan T-109): the small native and its placeholder are 106 dp
     * high at font scale 1.0 on every window, whatever its width: 411, 360 and 673 dp wide, and turned landscape.
     */
    @Test
    fun small_heightIs106dp_everyWindow() = assertSmallHeight(106f)

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun small_heightIs106dp_everyWindow_360() = assertSmallHeight(106f)

    @Test
    @Config(qualifiers = "w673dp-h841dp-xxhdpi")
    fun small_heightIs106dp_everyWindow_673() = assertSmallHeight(106f)

    /** As above at 891 x 411 dp: 106 dp, the card 859 dp wide. */
    @Test
    @Config(qualifiers = "w891dp-h411dp-xxhdpi")
    fun small_heightIs106dp_everyWindow_landscape() {
        assertSmallHeight(106f)
        assertEquals("card width", 859f, bounds(NativeAdTestTags.CARD).w(), 0.5f)
    }

    /** As above on a 640 x 360 dp window. */
    @Test
    @Config(qualifiers = "w640dp-h360dp-xxhdpi")
    fun small_heightIs106dp_everyWindow_landscape360() = assertSmallHeight(106f)

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

    /**
     * Design T-108 (design/context/SampleScreen_ready_light.md, "Native small"), KB-1 criterion 1, KB-32 criterion 3:
     * two tiers in a 106 dp card. The top row: the 44 dp icon 12 dp from the start and 6 dp from the top, the text
     * column 12 dp after it ("Ad" before a one-line 16/22 sp headline, 4 dp, one 13/18 sp line below), ending 28 dp or
     * more from the end. Below it, 6 dp lower, the call to action as wide as the card less 12 dp on each side, 44 dp
     * high with 18 sp text, 6 dp above the card bottom. No part overlaps another.
     */
    @Test
    fun small_twoTierLayout() {
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
        assertEquals("card height", 106f, card.h(), 0.5f)
        assertEquals("icon width", 44f, icon.w(), 0.5f)
        assertEquals("icon height", 44f, icon.h(), 0.5f)
        assertEquals("icon start margin", 12f, (icon.left - card.left).value, 0.5f)
        assertEquals("icon top margin", 6f, (icon.top - card.top).value, 0.5f)
        val columnLeft = min(label.left.value, headline.left.value)
        assertEquals("text column after the icon", 12f, columnLeft - icon.right.value, 0.5f)
        val titleRow = DpRect(
            columnLeft.dp,
            min(label.top.value, headline.top.value).dp,
            headline.right,
            max(label.bottom.value, headline.bottom.value).dp,
        )
        assertEquals("headline row height", 22f, titleRow.h(), 0.5f)
        assertTrue("label right ${label.right}, headline left ${headline.left}", label.right <= headline.left)
        assertTrue("label and headline centers", abs(label.centerY() - headline.centerY()) < 2f)
        assertEquals("headline text size", 16.sp, textStyle(sampleNativeAdAssets.headline).fontSize)
        assertEquals("headline lines", 1, textLayout(sampleNativeAdAssets.headline).lineCount)
        assertEquals("headline height", 22f, headline.h(), 0.5f)
        // Question 9 of plan T-065: the line below shows the advertiser, the body without one.
        val advertiserStyle = textStyle(sampleNativeAdAssets.advertiser!!)
        assertEquals("second line text size", 13.sp, advertiserStyle.fontSize)
        assertEquals("second line height of a line", 18.sp, advertiserStyle.lineHeight)
        assertEquals("one advertiser line", 18f, second.h(), 0.5f)
        assertEquals("second line below the headline row", 4f, (second.top - titleRow.bottom).value, 0.5f)
        assertTrue(
            "headline ends ${card.right - headline.right} from the card end",
            card.right - headline.right >= 28.dp,
        )
        assertEquals("CTA start margin", 12f, (cta.left - card.left).value, 0.5f)
        assertEquals("CTA end margin", 12f, (card.right - cta.right).value, 0.5f)
        assertEquals("CTA width", 355f, cta.w(), 0.5f)
        assertEquals("CTA height", 44f, cta.h(), 0.5f)
        assertEquals("CTA text size", 18.sp, textStyle("Install").fontSize)
        assertEquals("CTA below the top row (icon bottom)", 6f, (cta.top - icon.bottom).value, 0.5f)
        assertEquals("CTA to card bottom", 6f, (card.bottom - cta.bottom).value, 0.5f)
        val column =
            DpRect(columnLeft.dp, titleRow.top, max(headline.right.value, second.right.value).dp, second.bottom)
        listOf(
            Triple("icon", icon, "text column" to column),
            Triple("icon", icon, "CTA" to cta),
            Triple("text column", column, "CTA" to cta),
        ).forEach { (part, rect, other) ->
            val (otherName, otherRect) = other
            val overlaps = rect.left.value < otherRect.right.value - 0.5f &&
                rect.right.value > otherRect.left.value + 0.5f &&
                rect.top.value < otherRect.bottom.value - 0.5f &&
                rect.bottom.value > otherRect.top.value + 0.5f
            assertFalse("$part $rect overlaps $otherName $otherRect", overlaps)
        }

        assets = sampleNativeAdAssets.copy(advertiser = null, body = longFullNativeAdAssets.body)
        composeRule.waitForIdle()
        val body = textLayout(longFullNativeAdAssets.body!!)
        assertEquals("body one line", 1, body.lineCount)
        assertTrue("long body cut with an ellipsis", body.isLineEllipsized(0))
        assertEquals("one body line", 18f, bounds(NativeAdTestTags.SECOND_LINE).h(), 0.5f)
        assertEquals("card height with a long body", 106f, bounds(NativeAdTestTags.CARD).h(), 0.5f)
    }

    /**
     * Plan T-109, section 3.5 (design/context/SampleScreen_ad-loading_light.md, two tiers): the 44 x 44 dp icon
     * block 12 dp from the start and 6 dp from the top; the call-to-action block as wide as the card less 24 dp, 44 dp
     * high, 6 dp above the bottom; the two bars (14 dp, and 12 x 120 dp) between the icon block and the end less 28 dp.
     */
    @Test
    fun smallPlaceholder_twoTierBlocks() {
        composeRule.setContent {
            AdsTestTheme { NativeAdPlaceholder(NativeAdSize.SMALL, Modifier.width(CARD_WIDTH), animated = false) }
        }
        val card = bounds(NativeAdTestTags.PLACEHOLDER)
        val icon = bounds(NativeAdTestTags.PLACEHOLDER_ICON)
        val cta = bounds(NativeAdTestTags.PLACEHOLDER_CTA)
        assertEquals("placeholder height", 106f, card.h(), 0.5f)
        assertEquals("icon block width", 44f, icon.w(), 0.5f)
        assertEquals("icon block height", 44f, icon.h(), 0.5f)
        assertEquals("icon block start", 12f, (icon.left - card.left).value, 0.5f)
        assertEquals("icon block top", 6f, (icon.top - card.top).value, 0.5f)
        assertEquals("CTA block width", 355f, cta.w(), 0.5f)
        assertEquals("CTA block height", 44f, cta.h(), 0.5f)
        assertEquals("CTA block to the bottom", 6f, (card.bottom - cta.bottom).value, 0.5f)
        val bars = composeRule.onAllNodesWithTag(NativeAdTestTags.PLACEHOLDER_LINE, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .map { node ->
                val rect = node.boundsInRoot
                with(composeRule.density) {
                    DpRect(rect.left.toDp(), rect.top.toDp(), rect.right.toDp(), rect.bottom.toDp())
                }
            }
        assertEquals("two bars", 2, bars.size)
        val (top, bottom) = bars.sortedBy { it.top.value }
        assertEquals("top bar height", 14f, top.h(), 0.5f)
        assertEquals("bottom bar height", 12f, bottom.h(), 0.5f)
        assertEquals("bottom bar width", 120f, bottom.w(), 0.5f)
        assertEquals("bars 8 dp apart", 8f, (bottom.top - top.bottom).value, 0.5f)
        bars.forEach { bar ->
            assertTrue("bar $bar starts after the icon block $icon", bar.left >= icon.right)
            assertTrue("bar $bar ends within the end less 28 dp", bar.right <= card.right - 28.dp + 0.5.dp)
            assertTrue("bar $bar above the CTA block $cta", bar.bottom <= cta.top)
        }
        assertEquals("top bar to the end less 28 dp", (card.right - 28.dp).value, top.right.value, 0.5f)
    }

    /**
     * KB-2, KB-32 criterion 3 (question 66, plan T-110): medium is 250 dp at font scale 1.0 whatever the window, also
     * with `cta_large`; here at 891 dp, the other windows below.
     */
    @Test
    fun medium_is250dp_everyWindow() {
        var large by mutableStateOf(false)
        composeRule.setContent {
            AdsTestTheme {
                NativeAdCard(sampleNativeAdAssets, NativeAdSize.MEDIUM, Modifier.width(CARD_WIDTH), ctaLarge = large)
            }
        }
        assertEquals("medium", 250f, bounds(NativeAdTestTags.CARD).h(), 0.5f)
        large = true
        composeRule.waitForIdle()
        assertEquals("medium cta_large", 250f, bounds(NativeAdTestTags.CARD).h(), 0.5f)
    }

    /** The unfolded frame, 841 dp high: 250 dp, the layout of the design (one line below the headline). */
    @Test
    @Config(qualifiers = "w673dp-h841dp-xxhdpi")
    fun medium_is250dp_841() {
        composeRule.setContent {
            AdsTestTheme {
                Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    NativeAdCard(longFullNativeAdAssets, NativeAdSize.MEDIUM)
                }
            }
        }
        assertEquals("medium", 250f, bounds(NativeAdTestTags.CARD).h(), 0.5f)
        assertEquals("line below the headline", 18f, bounds(NativeAdTestTags.SECOND_LINE).h(), 0.5f)
        assertEquals("advertiser lines", 1, textLayout(longFullNativeAdAssets.advertiser!!).lineCount)
        composeRule.onNodeWithTag(NativeAdTestTags.BODY, useUnmergedTree = true).assertDoesNotExist()
    }

    /**
     * KB-2 criterion 1, KB-32 criterion 3 (question 66, plan T-110): on a 640 dp screen medium is 250 dp as on every
     * window, its media 144 dp (132 dp with `cta_large`) over the two-tier content block (6 + 44 + 6 + 44 (56) + 6 dp),
     * its placeholder as high. Without an advertiser and a body the line below keeps its 18 dp, so nothing moves.
     */
    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun medium_is250dp_lowScreen() {
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
            val second = bounds(NativeAdTestTags.SECOND_LINE)
            val cta = bounds(NativeAdTestTags.CTA)
            val stars = bounds(NativeAdTestTags.STAR_ROW)
            val headline = bounds(NativeAdTestTags.HEADLINE)
            val icon = bounds(NativeAdTestTags.ICON)
            assertEquals("card, $case", 250f, card.h(), 0.5f)
            assertEquals("media, $case", if (current) 132f else 144f, media.h(), 0.5f)
            composeRule.onNodeWithTag(NativeAdTestTags.BODY, useUnmergedTree = true).assertDoesNotExist()
            assertEquals("line below lines, $case", 1, textLayout(longFullNativeAdAssets.advertiser!!).lineCount)
            assertEquals("line below, $case", 18f, second.h(), 0.5f)
            assertEquals("star row below the media, $case", 33f, (stars.top - media.bottom).value, 0.5f)
            assertEquals("line below the media, $case", 32f, (second.top - media.bottom).value, 0.5f)
            assertEquals("line below to CTA, $case", 6f, (cta.top - second.bottom).value, 0.5f)
            assertEquals("star row, $case", 16f, stars.h(), 0.5f)
            assertEquals("head, $case", 43f, (stars.bottom - headline.top).value, 0.5f)
            assertEquals("icon below the media, $case", 6f, (icon.top - media.bottom).value, 0.5f)
            assertEquals("CTA below the icon, $case", 6f, (cta.top - icon.bottom).value, 0.5f)
            assertEquals("CTA to card bottom, $case", 6f, (card.bottom - cta.bottom).value, 0.5f)
            assertEquals("placeholder, $case", 250f, bounds(NativeAdTestTags.PLACEHOLDER).h(), 0.5f)

            // L-055: no body and no advertiser (still 4 stars): the line below keeps its place, nothing moves.
            assets = longFullNativeAdAssets.copy(body = null, advertiser = null)
            composeRule.waitForIdle()
            assertEquals("card without texts below, $case", 250f, bounds(NativeAdTestTags.CARD).h(), 0.5f)
            assertEquals("kept line below, $case", 18f, bounds(NativeAdTestTags.SECOND_LINE).h(), 0.5f)
            val keptMedia = bounds(NativeAdTestTags.MEDIA)
            assertEquals(
                "media bottom without texts below, $case",
                if (current) 132f else 144f,
                (keptMedia.bottom - bounds(NativeAdTestTags.CARD).top).value,
                0.5f,
            )
            assertEquals(
                "CTA top without texts below, $case",
                cta.top.value,
                bounds(NativeAdTestTags.CTA).top.value,
                0.5f,
            )
        }
    }

    /**
     * Design T-108 (design/context/AdGalleryScreen_default_light.md, "Native/Medium"), KB-2, KB-32 criterion 3 (plan
     * T-110, section 4): a 250 dp card, the 144 dp media at its top as wide as it, then the two tiers of small: the 44
     * dp icon 12 dp from the start and 6 dp below the media, the text column 12 dp after it ("Ad" before a one-line
     * 16/22 sp headline, 4 dp, one 13/18 sp line below) ending 12 dp from the card's end; the call to action 6 dp below
     * the top row, 355 x 44 dp with 18 sp text, 6 dp above the card bottom. No body paragraph.
     */
    @Test
    fun medium_twoTierLayout() {
        var large by mutableStateOf(false)
        var assets by mutableStateOf(sampleNativeAdAssets)
        composeRule.setContent {
            AdsTestTheme { NativeAdCard(assets, NativeAdSize.MEDIUM, Modifier.width(CARD_WIDTH), ctaLarge = large) }
        }
        val card = bounds(NativeAdTestTags.CARD)
        val icon = bounds(NativeAdTestTags.ICON)
        val label = bounds(NativeAdTestTags.BADGE)
        val headline = bounds(NativeAdTestTags.HEADLINE)
        val second = bounds(NativeAdTestTags.SECOND_LINE)
        val cta = bounds(NativeAdTestTags.CTA)
        val media = bounds(NativeAdTestTags.MEDIA)
        assertEquals("card height", 250f, card.h(), 0.5f)
        assertEquals("media top", 0f, (media.top - card.top).value, 0.5f)
        assertEquals("media width", CARD_WIDTH.value, media.w(), 0.5f)
        assertEquals("media", 144f, media.h(), 0.5f)
        assertEquals("icon width", 44f, icon.w(), 0.5f)
        assertEquals("icon height", 44f, icon.h(), 0.5f)
        assertEquals("icon start margin", 12f, (icon.left - card.left).value, 0.5f)
        assertEquals("icon below the media", 6f, (icon.top - media.bottom).value, 0.5f)
        val titleRow = DpRect(
            min(label.left.value, headline.left.value).dp,
            min(label.top.value, headline.top.value).dp,
            headline.right,
            max(label.bottom.value, headline.bottom.value).dp,
        )
        assertEquals("headline row height", 22f, titleRow.h(), 0.5f)
        assertTrue("label right ${label.right}, headline left ${headline.left}", label.right <= headline.left)
        assertTrue("label and headline centers", abs(label.centerY() - headline.centerY()) < 2f)
        assertEquals("headline height", 22f, headline.h(), 0.5f)
        assertEquals("advertiser line", 18f, second.h(), 0.5f)
        assertEquals("advertiser line below the headline row", 4f, (second.top - titleRow.bottom).value, 0.5f)
        assertEquals("advertiser text size", 13.sp, textStyle(sampleNativeAdAssets.advertiser!!).fontSize)
        assertEquals("advertiser lines", 1, textLayout(sampleNativeAdAssets.advertiser!!).lineCount)
        assertEquals("text column to the card end", 12f, (card.right - headline.right).value, 0.5f)
        assertEquals("CTA below the top row", 6f, (cta.top - icon.bottom).value, 0.5f)
        assertEquals("CTA start margin", 12f, (cta.left - card.left).value, 0.5f)
        assertEquals("CTA width", 355f, cta.w(), 0.5f)
        assertEquals("CTA height", 44f, cta.h(), 0.5f)
        assertEquals("CTA text size", 18.sp, textStyle("Install").fontSize)
        assertEquals("CTA to card bottom", 6f, (card.bottom - cta.bottom).value, 0.5f)
        composeRule.onNodeWithTag(NativeAdTestTags.BODY, useUnmergedTree = true).assertDoesNotExist()
        composeRule.onAllNodesWithTag(NativeAdTestTags.STAR, useUnmergedTree = true).assertCountEquals(4)

        large = true
        composeRule.waitForIdle()
        assertEquals("media cta_large", 132f, bounds(NativeAdTestTags.MEDIA).h(), 0.5f)
        assertEquals("CTA cta_large", 56f, bounds(NativeAdTestTags.CTA).h(), 0.5f)
        assertEquals("CTA text size cta_large", 17.sp, textStyle("Install").fontSize)
        assertEquals("card cta_large", 250f, bounds(NativeAdTestTags.CARD).h(), 0.5f)

        // Without an advertiser, the line below is the body, one line cut with "…".
        large = false
        assets = sampleNativeAdAssets.copy(advertiser = null, body = longFullNativeAdAssets.body)
        composeRule.waitForIdle()
        val body = textLayout(longFullNativeAdAssets.body!!)
        assertEquals("body one line", 1, body.lineCount)
        assertTrue("long body cut with an ellipsis", body.isLineEllipsized(0))
        assertEquals("body line", 18f, bounds(NativeAdTestTags.SECOND_LINE).h(), 0.5f)
        assertEquals("media without advertiser", 144f, bounds(NativeAdTestTags.MEDIA).h(), 0.5f)
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
