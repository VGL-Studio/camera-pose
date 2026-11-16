package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Native ads at the font scales a device offers (1.0 to 2.0; plan T-045, section 4): the call-to-action text is
 * never cut, the call to action keeps at least its design height (exactly at 1.0 and 1.3), the media of medium, the
 * step card and the collapsible layer never goes below 120 dp, and the "Ad" label of the full-screen ad keeps the
 * height of its text (O-08). 0.5 dp tolerance; each message names the form, the font scale and the texts.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class NativeAdFontScaleTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val badge: String
        get() = ApplicationProvider.getApplicationContext<android.content.Context>()
            .getString(R.string.ads_native_badge)

    private fun bounds(tag: String): DpRect =
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun textBounds(text: String): DpRect =
        composeRule.onNodeWithText(text, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun textLayout(text: String): TextLayoutResult {
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult].action!!(layouts)
        return layouts.single()
    }

    /**
     * Whether the one line of [layout] is cut in height: the laid-out box is lower than the line. Not
     * `didOverflowHeight`, which is also true when a long text is cut with "…" across (`didExceedMaxLines`).
     */
    private fun cutInHeight(layout: TextLayoutResult): Boolean = layout.size.height < layout.multiParagraph.height

    private fun DpRect.h(): Float = (bottom - top).value

    private fun textsName(assets: NativeAdAssets): String = when (assets) {
        sampleNativeAdAssets, FakeNativeAdAssets.Design -> "short texts"
        else -> "long texts"
    }

    /**
     * The call to action of [ctaTag] holds its text [ctaText] whole: (a) the text is not cut in height, (b)
     * the text lies inside the call to action, (c) the call to action is at least [designHeight] high, exactly that
     * at font scales 1.0 and 1.3, (d) the frame of [frameTag] is [frameHeight] high (any height when null) and holds
     * the call to action.
     */
    private fun assertCtaFits(
        case: String,
        scale: Float,
        ctaTag: String,
        ctaText: String,
        frameTag: String,
        designHeight: Dp,
        frameHeight: Dp?,
    ) {
        val cta = bounds(ctaTag)
        val text = textBounds(ctaText)
        val frame = bounds(frameTag)
        assertFalse("CTA text cut in height, $case", cutInHeight(textLayout(ctaText)))
        assertTrue(
            "CTA text $text inside the call to action $cta, $case",
            text.top.value >= cta.top.value - TOLERANCE && text.bottom.value <= cta.bottom.value + TOLERANCE,
        )
        if (scale <= 1.3f) {
            assertEquals("call to action height, $case", designHeight.value, cta.h(), TOLERANCE)
        } else {
            assertTrue(
                "call to action is ${cta.h()} high, under $designHeight, $case",
                cta.h() >= designHeight.value - TOLERANCE,
            )
        }
        if (frameHeight != null) assertEquals("frame height, $case", frameHeight.value, frame.h(), TOLERANCE)
        assertTrue(
            "call to action $cta inside the frame $frame, $case",
            cta.top.value >= frame.top.value - TOLERANCE && cta.bottom.value <= frame.bottom.value + TOLERANCE,
        )
    }

    private class CardCase(
        val name: String,
        val size: NativeAdSize,
        val ctaLarge: Boolean,
        val ctaHeight: Dp,
        /** The card's height at a font scale. */
        val cardHeight: (Float) -> Dp,
        val modifier: Modifier,
    )

    @Test
    fun cardSizes_ctaTextFits_fontScales() {
        val cases = listOf(
            CardCase("small", NativeAdSize.SMALL, false, 44.dp, { 126.dp }, Modifier.width(CARD_WIDTH)),
            CardCase(
                "medium",
                NativeAdSize.MEDIUM,
                false,
                44.dp,
                { if (it < 2f) 400.dp else 406.dp },
                Modifier.width(CARD_WIDTH),
            ),
            CardCase(
                "medium ctaLarge",
                NativeAdSize.MEDIUM,
                true,
                56.dp,
                { if (it < 2f) 400.dp else 418.dp },
                Modifier.width(CARD_WIDTH),
            ),
            CardCase("full", NativeAdSize.FULL, false, 36.dp, { 418.dp }, Modifier.width(CARD_WIDTH)),
            CardCase("card", NativeAdSize.CARD, false, 36.dp, { 300.dp }, Modifier.size(180.dp, 300.dp)),
        )
        var fontScale by mutableStateOf(1f)
        var assets by mutableStateOf(sampleNativeAdAssets)
        var case by mutableStateOf(cases.first())
        composeRule.setContent {
            WithFontScale(fontScale) {
                AdsTestTheme { NativeAdCard(assets, case.size, case.modifier, ctaLarge = case.ctaLarge) }
            }
        }
        cases.forEach { current ->
            SCALES.forEach { scale ->
                listOf(sampleNativeAdAssets, longFullNativeAdAssets).forEach { texts ->
                    case = current
                    fontScale = scale
                    assets = texts
                    composeRule.waitForIdle()
                    assertCtaFits(
                        case = "${current.name}, font scale $scale, ${textsName(texts)}",
                        scale = scale,
                        ctaTag = NativeAdTestTags.CTA,
                        ctaText = texts.callToAction!!,
                        frameTag = NativeAdTestTags.CARD,
                        designHeight = current.ctaHeight,
                        frameHeight = current.cardHeight(scale),
                    )
                }
            }
        }
    }

    private class TextCase(val name: String, val size: NativeAdSize, val modifier: Modifier, val frameHeight: Dp)

    private fun DpRect.w(): Float = (right - left).value

    private fun DpRect.inside(frame: DpRect): Boolean = left.value >= frame.left.value - TOLERANCE &&
        right.value <= frame.right.value + TOLERANCE &&
        top.value >= frame.top.value - TOLERANCE &&
        bottom.value <= frame.bottom.value + TOLERANCE

    /** Characters of the one line of [layout] that show, before the "…" when the text is cut across. */
    private fun visibleChars(layout: TextLayoutResult): Int = layout.getLineEnd(0, visibleEnd = true)

    /**
     * KB-1 (plan T-060): for each case of [cases] (on the screen with its 16 dp margin), each font scale and each
     * set of texts: (a) the frame keeps its height; (b) the headline, the "Ad" text and the call-to-action text are
     * not cut in height and lie inside the frame; (c) "Ad" shows both letters and its label is as wide as its line;
     * (d) the headline shows at least 4 characters; (e) the call to action shows at least 1 character.
     */
    private fun assertTextsNotCut(cases: List<TextCase>) {
        var fontScale by mutableStateOf(1f)
        var assets by mutableStateOf(sampleNativeAdAssets)
        var case by mutableStateOf(cases.first())
        composeRule.setContent {
            WithFontScale(fontScale) {
                AdsTestTheme {
                    Box(Modifier.fillMaxWidth().padding(16.dp)) { NativeAdCard(assets, case.size, case.modifier) }
                }
            }
        }
        cases.forEach { current ->
            SCALES.forEach { scale ->
                listOf(sampleNativeAdAssets, longFullNativeAdAssets).forEach { texts ->
                    case = current
                    fontScale = scale
                    assets = texts
                    composeRule.waitForIdle()
                    val name = "${current.name}, font scale $scale, ${textsName(texts)}"
                    val frame = bounds(NativeAdTestTags.CARD)
                    assertEquals("frame height, $name", current.frameHeight.value, frame.h(), TOLERANCE)
                    val headline = textLayout(texts.headline)
                    val label = textLayout(badge)
                    val cta = textLayout(texts.callToAction!!)
                    listOf(
                        Triple("headline", texts.headline, headline),
                        Triple("Ad", badge, label),
                        Triple("CTA", texts.callToAction!!, cta),
                    ).forEach { (part, text, layout) ->
                        assertFalse("$part text cut in height, $name", cutInHeight(layout))
                        val textRect = textBounds(text)
                        assertTrue("$part text $textRect inside the frame $frame, $name", textRect.inside(frame))
                    }
                    val labelBox = bounds(NativeAdTestTags.BADGE)
                    val labelLine = with(composeRule.density) { label.size.width.toDp().value }
                    assertEquals(
                        "Ad letters shown, label ${labelBox.w()} dp wide, $name",
                        badge.length,
                        visibleChars(label),
                    )
                    assertTrue(
                        "Ad label ${labelBox.w()} dp, narrower than its line $labelLine dp, $name",
                        labelBox.w() >= labelLine - TOLERANCE,
                    )
                    assertTrue(
                        "headline shows ${visibleChars(headline)} of ${texts.headline.length} characters, " +
                            "${bounds(NativeAdTestTags.HEADLINE).w()} dp wide, $name",
                        visibleChars(headline) >= 4,
                    )
                    assertTrue(
                        "call to action shows ${visibleChars(cta)} characters, " +
                            "${bounds(NativeAdTestTags.CTA).w()} dp wide, $name",
                        visibleChars(cta) >= 1,
                    )
                }
            }
        }
    }

    /** KB-1 (plan T-060): small (379 dp) and the card (180 x 300 dp) keep their texts at font scales 1.0 to 2.0. */
    @Test
    fun smallAndCard_headlineBadgeCta_notCut_fontScales() {
        assertTextsNotCut(
            listOf(
                TextCase("small 379 dp", NativeAdSize.SMALL, Modifier, NativeAdDimens.bottomSlotHeight(411.dp)),
                TextCase("card 180 x 300 dp", NativeAdSize.CARD, Modifier.size(180.dp, 300.dp), 300.dp),
            ),
        )
    }

    /** KB-1 (plan T-060), the low screen: small (328 dp), the card at 180 x 300 dp and 156 x 300 dp. */
    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun smallAndCard_headlineBadgeCta_notCut_fontScales_small() {
        assertTextsNotCut(
            listOf(
                TextCase("small 328 dp", NativeAdSize.SMALL, Modifier, NativeAdDimens.bottomSlotHeight(360.dp)),
                TextCase("card 180 x 300 dp", NativeAdSize.CARD, Modifier.size(180.dp, 300.dp), 300.dp),
                TextCase("card 156 x 300 dp", NativeAdSize.CARD, Modifier.size(156.dp, 300.dp), 300.dp),
            ),
        )
    }

    /**
     * Medium on the 891 dp screen: the card keeps 400 dp (also with `cta_large`) while its media stays above 120 dp
     * (font scales 1.0 to 1.5); at 2.0 the media reaches 120 dp and the card grows by just the media it lacks, to
     * 406 dp (418 dp), as plan T-045 lets it (plan T-065, section 3.4, question 12).
     */
    @Test
    fun medium_partsKeepSize_mediaShrinksFirst_fontScales() {
        var fontScale by mutableStateOf(1f)
        var assets by mutableStateOf(sampleNativeAdAssets)
        var ctaLarge by mutableStateOf(false)
        composeRule.setContent {
            WithFontScale(fontScale) {
                AdsTestTheme {
                    NativeAdCard(assets, NativeAdSize.MEDIUM, Modifier.width(CARD_WIDTH), ctaLarge = ctaLarge)
                }
            }
        }
        listOf(false, true).forEach { large ->
            listOf(1f, 1.3f, 1.5f, 2f).forEach { scale ->
                listOf(sampleNativeAdAssets, longFullNativeAdAssets).forEach { texts ->
                    ctaLarge = large
                    fontScale = scale
                    assets = texts.copy(starRating = 4.0)
                    composeRule.waitForIdle()
                    val case = "medium${if (large) " ctaLarge" else ""}, font scale $scale, ${textsName(texts)}"
                    val card = bounds(NativeAdTestTags.CARD)
                    val cta = bounds(NativeAdTestTags.CTA)
                    val stars = bounds(NativeAdTestTags.STAR_ROW)
                    val media = bounds(NativeAdTestTags.MEDIA)
                    if (scale < 2f) {
                        assertEquals("card, $case", 400f, card.h(), TOLERANCE)
                    } else {
                        assertEquals("media at its floor, $case", 120f, media.h(), TOLERANCE)
                        assertEquals("card, $case", if (large) 418f else 406f, card.h(), TOLERANCE)
                    }
                    assertEquals("call to action, $case", if (large) 56f else 44f, cta.h(), TOLERANCE)
                    assertEquals("star row, $case", 16f, stars.h(), TOLERANCE)
                    assertEquals(
                        "call to action to card bottom, $case",
                        13f,
                        (card.bottom - cta.bottom).value,
                        TOLERANCE,
                    )
                    assertEquals("body lines, $case", 2, textLayout(texts.body!!).lineCount)
                    assertTrue("media is ${media.h()} high, $case", media.h() >= 120f - TOLERANCE)
                    assertTrue(
                        "media top ${media.top}, star row bottom ${stars.bottom}, $case",
                        media.top.value >= stars.bottom.value - TOLERANCE,
                    )
                    if (scale == 1f) {
                        assertEquals("media at the design size, $case", if (large) 170f else 182f, media.h(), TOLERANCE)
                    }
                    if (scale == 1.5f) assertEquals("media, $case", if (large) 127f else 139f, media.h(), TOLERANCE)
                }
            }
        }
    }

    /**
     * KB-2 at large text on the low frame (question 7 of plan T-065): at font scale 1.0 the card keeps 320 dp
     * (media 134 dp, 122 dp with `cta_large`); with larger text the media shrinks to 120 dp first, then the card
     * grows (337 / 349 dp at 1.5, 357 / 369 dp at 2.0, measured on a stand-in layout: 1 dp tolerance). The body
     * keeps one line.
     */
    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun medium_compact_lowScreen_fontScales() {
        var fontScale by mutableStateOf(1f)
        var ctaLarge by mutableStateOf(false)
        composeRule.setContent {
            WithFontScale(fontScale) {
                AdsTestTheme {
                    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        NativeAdCard(longFullNativeAdAssets, NativeAdSize.MEDIUM, ctaLarge = ctaLarge)
                    }
                }
            }
        }
        listOf(false, true).forEach { large ->
            listOf(1f, 1.5f, 2f).forEach { scale ->
                ctaLarge = large
                fontScale = scale
                composeRule.waitForIdle()
                val case = "medium${if (large) " ctaLarge" else ""} low frame, font scale $scale"
                val card = bounds(NativeAdTestTags.CARD)
                val media = bounds(NativeAdTestTags.MEDIA)
                when (scale) {
                    1f -> {
                        assertEquals("card, $case", 320f, card.h(), TOLERANCE)
                        assertEquals("media, $case", if (large) 122f else 134f, media.h(), TOLERANCE)
                    }
                    1.5f -> {
                        assertEquals("media, $case", 120f, media.h(), TOLERANCE)
                        assertEquals("card, $case", if (large) 349f else 337f, card.h(), 1f)
                    }
                    else -> {
                        assertEquals("media, $case", 120f, media.h(), TOLERANCE)
                        assertEquals("card, $case", if (large) 369f else 357f, card.h(), 1f)
                    }
                }
                assertEquals("body lines, $case", 1, textLayout(longFullNativeAdAssets.body!!).lineCount)
                assertTrue("media is ${media.h()} high, $case", media.h() >= 120f - TOLERANCE)
                if (card.h() > 320f + TOLERANCE) {
                    assertEquals("card grows only with the media at its floor, $case", 120f, media.h(), TOLERANCE)
                }
            }
        }
    }

    @Test
    fun medium_mediaFloor120dp_cardGrows_atFontScale3() {
        composeRule.setContent {
            WithFontScale(3f) {
                AdsTestTheme { NativeAdCard(longFullNativeAdAssets, NativeAdSize.MEDIUM, Modifier.width(CARD_WIDTH)) }
            }
        }
        val card = bounds(NativeAdTestTags.CARD)
        val cta = bounds(NativeAdTestTags.CTA)
        val media = bounds(NativeAdTestTags.MEDIA)
        assertEquals("media", 120f, media.h(), TOLERANCE)
        assertTrue("call to action is ${cta.h()} high", cta.h() >= 44f - TOLERANCE)
        assertTrue("card is ${card.h()} high", card.h() > 406f + TOLERANCE)
    }

    @Test
    fun step_mediaFloor120dp_cardGrows_fontScales() {
        var fontScale by mutableStateOf(1f)
        var assets by mutableStateOf(sampleNativeAdAssets)
        var stepHeight = 0.dp
        composeRule.setContent {
            WithFontScale(fontScale) {
                AdsTestTheme {
                    stepHeight = NativeStepAdDefaults.height()
                    Column {
                        NativeAdCard(assets, NativeAdSize.STEP, Modifier.width(CARD_WIDTH))
                        NativeAdPlaceholder(NativeAdSize.STEP, Modifier.width(CARD_WIDTH), animated = false)
                    }
                }
            }
        }
        listOf(1f, 1.3f, 1.5f, 2f).forEach { scale ->
            listOf(sampleNativeAdAssets, longFullNativeAdAssets).forEach { texts ->
                fontScale = scale
                assets = texts
                composeRule.waitForIdle()
                val case = "step, font scale $scale, ${textsName(texts)}"
                val media = bounds(NativeAdTestTags.MEDIA)
                assertTrue("media is ${media.h()} high, $case", media.h() >= 120f - TOLERANCE)
                assertCtaFits(
                    case = case,
                    scale = scale,
                    ctaTag = NativeAdTestTags.CTA,
                    ctaText = texts.callToAction!!,
                    frameTag = NativeAdTestTags.CARD,
                    designHeight = 36.dp,
                    frameHeight = stepHeight,
                )
                val card = bounds(NativeAdTestTags.CARD)
                val placeholder = bounds(NativeAdTestTags.PLACEHOLDER)
                assertEquals("placeholder height, $case", card.h(), placeholder.h(), TOLERANCE)
                when (scale) {
                    1f, 1.3f -> assertEquals("card height, $case", 250f, card.h(), TOLERANCE)
                    2f -> assertEquals("card height, $case", 270f, card.h(), TOLERANCE)
                }
                val expectedBadge = BADGE_HEIGHTS[scale]
                if (expectedBadge != null) {
                    assertEquals("Ad label height, $case", expectedBadge, bounds(NativeAdTestTags.BADGE).h(), TOLERANCE)
                }
            }
        }
    }

    /**
     * The collapsible layer keeps 299 dp at 411 dp at every font scale (plan T-065, section 3.1 step 4): its content
     * follows the small native, whose text column never goes past the slot, so the MediaView keeps 161 dp.
     */
    @Test
    fun collapsibleLayer_keepsHeight_fontScales() {
        var fontScale by mutableStateOf(1f)
        var assets by mutableStateOf(sampleNativeAdAssets)
        composeRule.setContent {
            WithFontScale(fontScale) {
                AdsTestTheme { CollapsibleNativeAdCard(assets, onCollapse = {}, Modifier.width(CARD_WIDTH)) }
            }
        }
        listOf(1f, 1.3f, 1.5f, 2f).forEach { scale ->
            listOf(sampleNativeAdAssets, longFullNativeAdAssets).forEach { texts ->
                fontScale = scale
                assets = texts
                composeRule.waitForIdle()
                val case = "collapsible layer, font scale $scale, ${textsName(texts)}"
                val media = bounds(CollapsibleNativeAdTestTags.MEDIA)
                assertTrue("media is ${media.h()} high, $case", media.h() >= 120f - TOLERANCE)
                assertEquals("MediaView, $case", 161f, media.h(), TOLERANCE)
                assertCtaFits(
                    case = case,
                    scale = scale,
                    ctaTag = NativeAdTestTags.CTA,
                    ctaText = texts.callToAction!!,
                    frameTag = CollapsibleNativeAdTestTags.AD,
                    designHeight = 44.dp,
                    frameHeight = 299.dp,
                )
                val layer = bounds(CollapsibleNativeAdTestTags.AD)
                val collapse = bounds(CollapsibleNativeAdTestTags.COLLAPSE)
                assertEquals("collapse button to the top, $case", 4f, (collapse.top - layer.top).value, TOLERANCE)
                assertEquals("collapse button to the start, $case", 4f, (collapse.left - layer.left).value, TOLERANCE)
            }
        }
    }

    /**
     * KB-1 at 200% (plan T-065, question 8), on the screens of 411 (this class), 320 and 360 dp, at font scales 1.0
     * to 2.0, long and short texts: the card keeps the slot height of its screen; headline, "Ad" and call-to-action
     * texts are not cut in height and lie inside the card; "Ad" shows both letters; the headline shows at least
     * [minHeadlineChars] characters before "…"; "Install" (18 sp) shows whole, 83.33 / 100.33 / 117.33 dp wide at
     * 1.0 / 1.5 / 2.0 and 44 dp high; a long call to action stops at 120 dp.
     */
    @Test
    fun small_fontScales_headlineBadgeCta_notCut_allWidths() = assertSmallTexts(slotHeight = 126f, minHeadlineChars = 4)

    @Test
    @Config(qualifiers = "w320dp-h640dp-xxhdpi")
    fun small_fontScales_headlineBadgeCta_notCut_allWidths_320() =
        assertSmallTexts(slotHeight = 90f, minHeadlineChars = 1)

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun small_fontScales_headlineBadgeCta_notCut_allWidths_360() =
        assertSmallTexts(slotHeight = 106f, minHeadlineChars = 4)

    private fun assertSmallTexts(slotHeight: Float, minHeadlineChars: Int) {
        var fontScale by mutableStateOf(1f)
        var assets by mutableStateOf(sampleNativeAdAssets)
        composeRule.setContent {
            WithFontScale(fontScale) {
                AdsTestTheme {
                    Box(Modifier.fillMaxWidth().padding(16.dp)) { NativeAdCard(assets, NativeAdSize.SMALL) }
                }
            }
        }
        SMALL_SCALES.forEach { scale ->
            listOf(sampleNativeAdAssets, longFullNativeAdAssets).forEach { texts ->
                fontScale = scale
                assets = texts
                composeRule.waitForIdle()
                val frame = bounds(NativeAdTestTags.CARD)
                val name = "small ${frame.w()} dp, font scale $scale, ${textsName(texts)}"
                assertEquals("card height, $name", slotHeight, frame.h(), TOLERANCE)
                val headline = textLayout(texts.headline)
                val label = textLayout(badge)
                val cta = textLayout(texts.callToAction!!)
                listOf(
                    Triple("headline", texts.headline, headline),
                    Triple("Ad", badge, label),
                    Triple("CTA", texts.callToAction!!, cta),
                ).forEach { (part, text, layout) ->
                    assertFalse("$part text cut in height, $name", cutInHeight(layout))
                    val textRect = textBounds(text)
                    assertTrue("$part text $textRect inside the card $frame, $name", textRect.inside(frame))
                }
                assertEquals("Ad letters shown, $name", badge.length, visibleChars(label))
                assertTrue(
                    "headline shows ${visibleChars(headline)} of ${texts.headline.length} characters, " +
                        "${bounds(NativeAdTestTags.HEADLINE).w()} dp wide, $name",
                    visibleChars(headline) >= minHeadlineChars,
                )
                val ctaBox = bounds(NativeAdTestTags.CTA)
                if (texts === sampleNativeAdAssets) {
                    assertEquals("Install shown whole, $name", texts.callToAction!!.length, visibleChars(cta))
                    assertFalse("Install cut with an ellipsis, $name", cta.isLineEllipsized(0))
                    assertEquals("Install height, $name", 44f, ctaBox.h(), TOLERANCE)
                    INSTALL_WIDTHS[scale]?.let { width ->
                        assertEquals("Install width, $name", width, ctaBox.w(), TOLERANCE)
                    }
                } else {
                    assertEquals("long call to action width, $name", 120f, ctaBox.w(), TOLERANCE)
                }
            }
        }
    }

    /**
     * Question 8 of plan T-065: below font scale 1.5 the "Ad" label stands before the headline on its row and the
     * body has up to two lines; from 1.5 the label starts the body line, the body keeps one line and the headline
     * takes the whole text column. Without any text below the headline, the label still goes to the line below.
     */
    @Test
    fun small_badgeMovesToBodyLine_fromFontScale1_5() {
        var fontScale by mutableStateOf(1f)
        var assets by mutableStateOf(longFullNativeAdAssets.copy(advertiser = null))
        composeRule.setContent {
            WithFontScale(fontScale) {
                AdsTestTheme { NativeAdCard(assets, NativeAdSize.SMALL, Modifier.width(CARD_WIDTH)) }
            }
        }
        val bodyText = longFullNativeAdAssets.body!!
        SMALL_SCALES.forEach { scale ->
            fontScale = scale
            composeRule.waitForIdle()
            val case = "font scale $scale"
            val label = bounds(NativeAdTestTags.BADGE)
            val headline = bounds(NativeAdTestTags.HEADLINE)
            val body = textBounds(bodyText)
            val lines = textLayout(bodyText).lineCount
            if (scale < 1.5f) {
                assertTrue(
                    "label right ${label.right}, headline left ${headline.left}, $case",
                    label.right <= headline.left,
                )
                val centers =
                    abs((label.top.value + label.bottom.value) - (headline.top.value + headline.bottom.value)) / 2
                assertTrue("label and headline centers $centers dp apart, $case", centers < 2f)
                assertTrue("body lines $lines, $case", lines in 1..2)
            } else {
                assertTrue(
                    "label top ${label.top}, headline bottom ${headline.bottom}, $case",
                    label.top.value >= headline.bottom.value - TOLERANCE,
                )
                assertEquals(
                    "label start at the headline start, $case",
                    headline.left.value,
                    label.left.value,
                    TOLERANCE,
                )
                assertEquals("body lines, $case", 1, lines)
                assertTrue("body after the label, $case", body.left.value >= label.right.value - TOLERANCE)
                assertTrue("body on the label's line, $case", body.top < label.bottom && body.bottom > label.top)
                assertEquals(
                    "headline as wide as the text column, $case",
                    (body.right - label.left).value,
                    headline.w(),
                    TOLERANCE,
                )
            }
        }

        fontScale = 1.5f
        assets = longFullNativeAdAssets.copy(advertiser = null, body = null)
        composeRule.waitForIdle()
        val label = bounds(NativeAdTestTags.BADGE)
        val headline = bounds(NativeAdTestTags.HEADLINE)
        assertTrue(
            "no text below: label top ${label.top}, headline bottom ${headline.bottom}",
            label.top.value >= headline.bottom.value - TOLERANCE,
        )
    }

    @Test
    fun fullScreenAfterInter_adBadgeKeepsTextHeight_fontScales() {
        var fontScale by mutableStateOf(1f)
        var assets by mutableStateOf(FakeNativeAdAssets.Design)
        composeRule.setContent {
            WithFontScale(fontScale) {
                AdsTestTheme {
                    NativeFullScreenAdScreen(
                        assets = assets,
                        onClose = {},
                        media = { FakeMedia() },
                        adChoices = { FakeAdChoices() },
                    )
                }
            }
        }
        SCALES.forEach { scale ->
            listOf(FakeNativeAdAssets.Design, FakeNativeAdAssets.Longest).forEach { texts ->
                fontScale = scale
                assets = texts
                composeRule.waitForIdle()
                val case = "after inter, font scale $scale, ${textsName(texts)}"
                val label = bounds(NativeAdTestTags.BADGE)
                val labelText = textLayout(badge)
                assertFalse("Ad text cut in height, $case", cutInHeight(labelText))
                assertEquals(
                    "Ad label as high as its line, $case",
                    with(composeRule.density) { labelText.size.height.toDp().value },
                    label.h(),
                    TOLERANCE,
                )
                assertEquals("Ad label height, $case", BADGE_HEIGHTS.getValue(scale), label.h(), TOLERANCE)
                assertFalse("headline cut in height, $case", cutInHeight(textLayout(texts.headline)))
                val media = bounds(NativeFullScreenAdTestTags.MEDIA)
                assertTrue("media is ${media.h()} high, $case", media.h() >= 120f - TOLERANCE)
                val close = bounds(NativeFullScreenAdTestTags.CLOSE)
                assertEquals("close button width, $case", 48f, (close.right - close.left).value, TOLERANCE)
                assertEquals("close button height, $case", 48f, close.h(), TOLERANCE)
                assertEquals("close button left, $case", 16f, close.left.value, TOLERANCE)
                assertEquals("close button top, $case", 24f, close.top.value, TOLERANCE)
                if (scale == 1f && texts === FakeNativeAdAssets.Design) {
                    val icon = bounds(NativeFullScreenAdTestTags.ICON)
                    assertEquals("icon (head) at the design size, $case", 48f, icon.h(), TOLERANCE)
                    assertEquals("media at the design size, $case", 583f, media.h(), TOLERANCE)
                }
            }
        }
    }

    @Test
    fun fullScreen_ctaTextFits_fontScales() {
        // 0: the screen after an interstitial; 1 and 2: the page of a flow step in 484 dp (column) and 255 dp (row).
        var form by mutableStateOf(0)
        var fontScale by mutableStateOf(1f)
        var assets by mutableStateOf(FakeNativeAdAssets.Design)
        composeRule.setContent {
            WithFontScale(fontScale) {
                AdsTestTheme {
                    when (form) {
                        0 -> Box(Modifier.testTag(FRAME)) {
                            NativeFullScreenAdScreen(
                                assets = assets,
                                onClose = {},
                                media = { FakeMedia() },
                                adChoices = { FakeAdChoices() },
                            )
                        }
                        else -> {
                            val height = if (form == 1) 484.dp else 255.dp
                            Box(Modifier.fillMaxWidth().height(height).testTag(FRAME)) {
                                NativeFullScreenStepContent(
                                    assets,
                                    media = { FakeMedia() },
                                    adChoices = { FakeAdChoices() },
                                )
                            }
                        }
                    }
                }
            }
        }
        listOf(0, 1, 2).forEach { current ->
            SCALES.forEach { scale ->
                listOf(FakeNativeAdAssets.Design, FakeNativeAdAssets.Longest).forEach { texts ->
                    form = current
                    fontScale = scale
                    assets = texts
                    composeRule.waitForIdle()
                    val name = when (current) {
                        0 -> "after inter"
                        1 -> "flow step page 484 dp"
                        else -> "flow step page 255 dp"
                    }
                    assertCtaFits(
                        case = "$name, font scale $scale, ${textsName(texts)}",
                        scale = scale,
                        ctaTag = NativeFullScreenAdTestTags.CTA,
                        ctaText = texts.callToAction!!,
                        frameTag = FRAME,
                        designHeight = 52.dp,
                        frameHeight = null,
                    )
                }
            }
        }
    }

    private companion object {
        val CARD_WIDTH = 379.dp
        val SCALES = listOf(1f, 1.3f, 2f)

        /** Small at the font scales of question 8 of plan T-065, with 1.5 where the "Ad" label moves. */
        val SMALL_SCALES = listOf(1f, 1.3f, 1.5f, 2f)

        /** "Install" at 18 sp, as wide as its text (plan T-065, section 4.1). */
        val INSTALL_WIDTHS = mapOf(1f to 83.33f, 1.5f to 100.33f, 2f to 117.33f)

        /** The "Ad" label is as high as one line of `labelSmall`: 16, 21 and 32 dp at font scales 1.0, 1.3, 2.0. */
        val BADGE_HEIGHTS = mapOf(1f to 16f, 1.3f to 21f, 2f to 32f)
        const val FRAME = "font_scale_test_frame"
        const val TOLERANCE = 0.5f
    }
}
