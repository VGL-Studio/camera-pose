package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.math.abs
import kotlin.math.roundToInt
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
        // The small card's height at the font scale on screen, read in the same composition (plan T-109).
        var slotHeight = 0.dp
        // Medium's height worked out with the text measuring of the same composition (plan T-110, section 3.5).
        var mediumHeight = 0.dp
        var mediumLargeHeight = 0.dp
        val cases = listOf(
            CardCase("small", NativeAdSize.SMALL, false, 44.dp, { slotHeight }, Modifier.width(CARD_WIDTH)),
            CardCase(
                "medium",
                NativeAdSize.MEDIUM,
                false,
                44.dp,
                { mediumHeight },
                Modifier.width(CARD_WIDTH),
            ),
            CardCase(
                "medium ctaLarge",
                NativeAdSize.MEDIUM,
                true,
                56.dp,
                { mediumLargeHeight },
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
                AdsTestTheme {
                    slotHeight = BottomAdBarDefaults.nativeSlotHeight()
                    val textMeasurer = rememberTextMeasurer()
                    val types = MaterialTheme.typography
                    val scaled = LocalDensity.current
                    mediumHeight = mediumNativeHeight(textMeasurer, types, scaled, ctaLarge = false)
                    mediumLargeHeight = mediumNativeHeight(textMeasurer, types, scaled, ctaLarge = true)
                    NativeAdCard(assets, case.size, case.modifier, ctaLarge = case.ctaLarge)
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
                    if (scale == 1f) assertEquals("small slot height at font scale 1.0", 106f, slotHeight.value, 0f)
                    if (scale == 1f) {
                        assertEquals("medium height at font scale 1.0", 250f, mediumHeight.value, 0f)
                        assertEquals("medium ctaLarge height at font scale 1.0", 250f, mediumLargeHeight.value, 0f)
                    }
                    if (scale == 2f) {
                        assertTrue("medium height at font scale 2.0: $mediumHeight", mediumHeight > 250.dp)
                        assertTrue("medium ctaLarge at font scale 2.0: $mediumLargeHeight", mediumLargeHeight > 250.dp)
                    }
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

    private class TextCase(
        val name: String,
        val size: NativeAdSize,
        val modifier: Modifier,
        /** The frame's height, read when asserting: the small one follows the font scale (plan T-109). */
        val frameHeight: () -> Dp,
    )

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
                    smallSlotHeight = BottomAdBarDefaults.nativeSlotHeight()
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
                    assertEquals("frame height, $name", current.frameHeight().value, frame.h(), TOLERANCE)
                    if (current.size == NativeAdSize.SMALL && scale == 1f) {
                        assertEquals("small frame at font scale 1.0, $name", 106f, frame.h(), TOLERANCE)
                    }
                    // Control: small grows with its text (plan T-109), so its height is not a fixed slot.
                    if (current.size == NativeAdSize.SMALL && scale == 2f) {
                        assertTrue("small frame at 2.0 is ${frame.h()}, $name", frame.h() > 106f + TOLERANCE)
                    }
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

    /** The small native's height at the font scale on screen, read in the composition of [assertTextsNotCut]. */
    private var smallSlotHeight = 0.dp

    /**
     * KB-1 (plan T-060): small (379 dp) and the card (180 x 300 dp) keep their texts at font scales 1.0 to 2.0; small
     * is as high as [BottomAdBarDefaults.nativeSlotHeight] at each font scale (plan T-109).
     */
    @Test
    fun smallAndCard_headlineBadgeCta_notCut_fontScales() {
        assertTextsNotCut(
            listOf(
                TextCase("small 379 dp", NativeAdSize.SMALL, Modifier) { smallSlotHeight },
                TextCase("card 180 x 300 dp", NativeAdSize.CARD, Modifier.size(180.dp, 300.dp)) { 300.dp },
            ),
        )
    }

    /** KB-1 (plan T-060), the low screen: small (328 dp), the card at 180 x 300 dp and 156 x 300 dp. */
    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun smallAndCard_headlineBadgeCta_notCut_fontScales_small() {
        assertTextsNotCut(
            listOf(
                TextCase("small 328 dp", NativeAdSize.SMALL, Modifier) { smallSlotHeight },
                TextCase("card 180 x 300 dp", NativeAdSize.CARD, Modifier.size(180.dp, 300.dp)) { 300.dp },
                TextCase("card 156 x 300 dp", NativeAdSize.CARD, Modifier.size(156.dp, 300.dp)) { 300.dp },
            ),
        )
    }

    // The text measuring of the composition on screen, at its font scale (plan T-110): set in `setContent`.
    private lateinit var measurer: TextMeasurer
    private lateinit var typography: Typography
    private lateinit var density: Density

    /** The content block of medium worked out with the measuring of the composition on screen. */
    private fun content(large: Boolean): Dp = nativeContentHeight(measurer, typography, density, large)

    /** One line of the call-to-action text of medium (18 sp, 17 sp when [large]), measured as `Text` does. */
    private fun ctaLine(large: Boolean): Float {
        val style = typography.labelLarge.merge(TextStyle(fontSize = if (large) 17.sp else 18.sp))
        val pixels = measurer.measure("Ad", style, maxLines = 1, density = density).size.height
        return pixels / density.density
    }

    /**
     * The parts of medium add up to the card the functions work out (plan T-110, section 3.5): the media is
     * [NativeAdDimens.mediumMediaHeight] of [nativeContentHeight], the content block that height, the top row 6 dp
     * below the media, the call to action 6 dp (less than 7 dp with the part left by rounding up) below the top row and
     * 6 dp above the card bottom. 0.5 dp tolerance.
     */
    private fun assertMediumAddsUp(case: String, large: Boolean) {
        val card = bounds(NativeAdTestTags.CARD)
        val media = bounds(NativeAdTestTags.MEDIA)
        val row = bounds(NativeAdTestTags.SMALL_TOP_ROW)
        val cta = bounds(NativeAdTestTags.CTA)
        val content = content(large)
        assertEquals(
            "media as worked out, $case",
            NativeAdDimens.mediumMediaHeight(content).value,
            media.h(),
            TOLERANCE,
        )
        assertEquals("top row below the media, $case", 6f, (row.top - media.bottom).value, TOLERANCE)
        val gap = (cta.top - row.bottom).value
        assertTrue("top row to CTA $gap dp, $case", gap >= 6f - TOLERANCE && gap < 7f + TOLERANCE)
        assertEquals("CTA to card bottom, $case", 6f, (card.bottom - cta.bottom).value, TOLERANCE)
    }

    /**
     * KB-2 criterion 1, KB-32 criterion 3 (question 66, plan T-110): medium on the 891 dp screen is
     * [mediumNativeHeight] high at every font scale (250 dp at 1.0, also with `cta_large`, more at 2.0); with larger
     * text the media shrinks first, never below 120 dp, and only then does the card grow. Every part keeps its size
     * and stays inside the card, every text shows uncut in height.
     */
    @Test
    fun medium_partsInsideCard_mediaAtLeast120_fontScales() {
        var fontScale by mutableStateOf(1f)
        var assets by mutableStateOf(sampleNativeAdAssets)
        var ctaLarge by mutableStateOf(false)
        composeRule.setContent {
            WithFontScale(fontScale) {
                AdsTestTheme {
                    measurer = rememberTextMeasurer()
                    typography = MaterialTheme.typography
                    density = LocalDensity.current
                    Column {
                        NativeAdCard(assets, NativeAdSize.MEDIUM, Modifier.width(CARD_WIDTH), ctaLarge = ctaLarge)
                        NativeAdPlaceholder(
                            NativeAdSize.MEDIUM,
                            Modifier.width(CARD_WIDTH),
                            animated = false,
                            ctaLarge = ctaLarge,
                        )
                    }
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
                    val block = bounds(NativeAdTestTags.CONTENT)
                    val expected = mediumNativeHeight(measurer, typography, density, large)
                    assertEquals("card as worked out, $case", expected.value, card.h(), TOLERANCE)
                    if (scale == 1f) assertEquals("card at 1.0, $case", 250f, card.h(), TOLERANCE)
                    if (scale == 2f) assertTrue("card at 2.0 is ${card.h()}, $case", card.h() > 250f + TOLERANCE)
                    if (card.h() > 250f + TOLERANCE) {
                        assertEquals("card grows only with the media at its floor, $case", 120f, media.h(), TOLERANCE)
                    } else {
                        assertEquals("card, $case", 250f, card.h(), TOLERANCE)
                    }
                    assertTrue("media is ${media.h()} high, $case", media.h() >= 120f - TOLERANCE)
                    assertEquals(
                        "call to action, $case",
                        maxOf(if (large) 56f else 44f, ctaLine(large)),
                        cta.h(),
                        TOLERANCE,
                    )
                    assertEquals("star row, $case", 16f, stars.h(), TOLERANCE)
                    assertEquals("content block as worked out, $case", content(large).value, block.h(), TOLERANCE)
                    assertEquals("media and content block, $case", card.h(), media.h() + block.h(), TOLERANCE)
                    assertMediumAddsUp(case, large)
                    assertTrue(
                        "call to action $cta inside the card $card, $case",
                        cta.top.value >= card.top.value - TOLERANCE &&
                            cta.bottom.value <= card.bottom.value + TOLERANCE,
                    )
                    assertEquals("call to action width, $case", card.w() - 24f, cta.w(), TOLERANCE)
                    assertEquals("line below lines, $case", 1, textLayout(texts.advertiser!!).lineCount)
                    assertTrue(
                        "star row top ${stars.top}, media bottom ${media.bottom}, $case",
                        stars.top.value >= media.bottom.value + 6f - TOLERANCE,
                    )
                    val headline = textLayout(texts.headline)
                    val label = textLayout(badge)
                    val ctaText = textLayout(texts.callToAction!!)
                    val second = textLayout(texts.advertiser!!)
                    listOf(
                        Triple("headline", texts.headline, headline),
                        Triple("Ad", badge, label),
                        Triple("CTA", texts.callToAction!!, ctaText),
                        Triple("second line", texts.advertiser!!, second),
                    ).forEach { (part, text, layout) ->
                        assertFalse("$part text cut in height, $case", cutInHeight(layout))
                        val textRect = textBounds(text)
                        assertTrue("$part text $textRect inside the card $card, $case", textRect.inside(card))
                    }
                    assertEquals("Ad letters shown, $case", badge.length, visibleChars(label))
                    assertTrue(
                        "headline shows ${visibleChars(headline)} of ${texts.headline.length} characters, $case",
                        visibleChars(headline) >= 4,
                    )
                    if (texts === sampleNativeAdAssets) {
                        assertEquals("Install shown whole, $case", texts.callToAction!!.length, visibleChars(ctaText))
                        assertFalse("Install cut with an ellipsis, $case", ctaText.isLineEllipsized(0))
                    } else {
                        assertTrue("call to action shows no character, $case", visibleChars(ctaText) >= 1)
                    }
                    assertEquals("placeholder, $case", card.h(), bounds(NativeAdTestTags.PLACEHOLDER).h(), TOLERANCE)
                    if (scale == 1f) {
                        assertEquals("media at the design size, $case", if (large) 132f else 144f, media.h(), TOLERANCE)
                    }
                    // Control: the media shrinks first; at 1.5 it is below its design size, the card not grown yet.
                    if (scale == 1.5f) {
                        assertTrue("media at 1.5 is ${media.h()}, $case", media.h() < if (large) 132f else 144f)
                        if (!large) assertEquals("card at 1.5, $case", 250f, card.h(), TOLERANCE)
                    }
                }
            }
        }
    }

    /**
     * KB-2 at large text on the low frame (plan T-110, section 3.5): medium is the same as on the 891 dp screen. At
     * font scale 1.0 the card is 250 dp (media 144 dp, 132 dp with `cta_large`); with larger text the media shrinks
     * first, never below 120 dp, then the card grows to [mediumNativeHeight] (1 dp tolerance at 1.5 and 2.0). The line
     * below the headline keeps one line; the placeholder is as high as the card.
     */
    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun medium_lowScreen_sameAsStandard_fontScales() {
        var fontScale by mutableStateOf(1f)
        var ctaLarge by mutableStateOf(false)
        composeRule.setContent {
            WithFontScale(fontScale) {
                AdsTestTheme {
                    measurer = rememberTextMeasurer()
                    typography = MaterialTheme.typography
                    density = LocalDensity.current
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        NativeAdCard(longFullNativeAdAssets, NativeAdSize.MEDIUM, ctaLarge = ctaLarge)
                        NativeAdPlaceholder(NativeAdSize.MEDIUM, animated = false, ctaLarge = ctaLarge)
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
                        assertEquals("card, $case", 250f, card.h(), TOLERANCE)
                        assertEquals("media, $case", if (large) 132f else 144f, media.h(), TOLERANCE)
                    }
                    else -> {
                        val expected = mediumNativeHeight(measurer, typography, density, large)
                        assertEquals("card, $case", expected.value, card.h(), 1f)
                    }
                }
                assertMediumAddsUp(case, large)
                assertEquals("line below lines, $case", 1, textLayout(longFullNativeAdAssets.advertiser!!).lineCount)
                assertTrue("media is ${media.h()} high, $case", media.h() >= 120f - TOLERANCE)
                if (card.h() > 250f + TOLERANCE) {
                    assertEquals("card grows only with the media at its floor, $case", 120f, media.h(), TOLERANCE)
                }
                assertEquals("placeholder, $case", card.h(), bounds(NativeAdTestTags.PLACEHOLDER).h(), TOLERANCE)
            }
        }
    }

    @Test
    fun medium_mediaFloor120dp_cardGrows_atFontScale3() {
        composeRule.setContent {
            WithFontScale(3f) {
                AdsTestTheme {
                    measurer = rememberTextMeasurer()
                    typography = MaterialTheme.typography
                    density = LocalDensity.current
                    NativeAdCard(longFullNativeAdAssets, NativeAdSize.MEDIUM, Modifier.width(CARD_WIDTH))
                }
            }
        }
        val card = bounds(NativeAdTestTags.CARD)
        val cta = bounds(NativeAdTestTags.CTA)
        val media = bounds(NativeAdTestTags.MEDIA)
        assertEquals("media", 120f, media.h(), TOLERANCE)
        assertTrue("call to action is ${cta.h()} high", cta.h() >= 44f - TOLERANCE)
        assertEquals("card is the content and 120 dp", content(false).value + 120f, card.h(), TOLERANCE)
        assertTrue("card is ${card.h()} high", card.h() > 250f + TOLERANCE)
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
     * KB-32 criterion 3 (question 66, plan T-109): the content of the collapsible layer is as high as the small native
     * at the font scale ([BottomAdBarDefaults.nativeSlotHeight]); the layer is max(299, content + 132) dp at 411 x 891
     * dp, its MediaView what is left (layer - content - 12 dp), never below 120 dp: 181 dp in 299 dp at 1.0.
     */
    @Test
    fun collapsibleLayer_contentFollowsFontScale() {
        var fontScale by mutableStateOf(1f)
        var assets by mutableStateOf(sampleNativeAdAssets)
        var slotHeight = 0.dp
        composeRule.setContent {
            WithFontScale(fontScale) {
                AdsTestTheme {
                    slotHeight = BottomAdBarDefaults.nativeSlotHeight()
                    CollapsibleNativeAdCard(assets, onCollapse = {}, Modifier.width(CARD_WIDTH))
                }
            }
        }
        listOf(1f, 1.3f, 1.5f, 2f).forEach { scale ->
            listOf(sampleNativeAdAssets, longFullNativeAdAssets).forEach { texts ->
                fontScale = scale
                assets = texts
                composeRule.waitForIdle()
                val case = "collapsible layer, font scale $scale, ${textsName(texts)}"
                val tolerance = if (scale == 1f) TOLERANCE else 1f
                val layer = bounds(CollapsibleNativeAdTestTags.AD)
                val content = bounds(CollapsibleNativeAdTestTags.CONTENT)
                val media = bounds(CollapsibleNativeAdTestTags.MEDIA)
                assertEquals("content as high as the small native, $case", slotHeight.value, content.h(), tolerance)
                assertEquals(
                    "layer, $case",
                    maxOf(299f, content.h() + 132f),
                    layer.h(),
                    tolerance,
                )
                assertEquals("MediaView, $case", layer.h() - content.h() - 12f, media.h(), tolerance)
                assertTrue("media is ${media.h()} high, $case", media.h() >= 120f - TOLERANCE)
                if (scale == 1f) {
                    assertEquals("MediaView at 1.0, $case", 181f, media.h(), TOLERANCE)
                    assertEquals("layer at 1.0, $case", 299f, layer.h(), TOLERANCE)
                }
                assertCtaFits(
                    case = case,
                    scale = scale,
                    ctaTag = NativeAdTestTags.CTA,
                    ctaText = texts.callToAction!!,
                    frameTag = CollapsibleNativeAdTestTags.AD,
                    designHeight = 44.dp,
                    frameHeight = layer.h().dp,
                )
                val collapse = bounds(CollapsibleNativeAdTestTags.COLLAPSE)
                assertEquals("collapse button to the top, $case", 4f, (collapse.top - layer.top).value, TOLERANCE)
                assertEquals("collapse button to the start, $case", 4f, (collapse.left - layer.left).value, TOLERANCE)
            }
        }
        // Control: the content grows with the text, so the layer is not a fixed 299 dp.
        fontScale = 2f
        assets = sampleNativeAdAssets
        composeRule.waitForIdle()
        assertTrue("small native at 2.0 is $slotHeight", slotHeight > 106.dp)
    }

    /**
     * KB-1 at 200% (plan T-065, question 8; plan T-109), on the screens of 411 (this class), 320 and 360 dp, at font
     * scales 1.0 to 2.0, long and short texts: the card is as high as [BottomAdBarDefaults.nativeSlotHeight] read in
     * the same composition (106 dp at 1.0); headline, "Ad", call-to-action and second-line texts are not cut in height
     * and lie inside the card; "Ad" shows both letters; the headline shows at least [minHeadlineChars] characters
     * before "…"; "Install" (18 sp) shows whole, 44 dp high; the call to action is as wide as the card less 24 dp.
     */
    @Test
    fun small_fontScales_headlineBadgeCta_notCut_allWidths() = assertSmallTexts(minHeadlineChars = 4)

    @Test
    @Config(qualifiers = "w320dp-h640dp-xxhdpi")
    fun small_fontScales_headlineBadgeCta_notCut_allWidths_320() = assertSmallTexts(minHeadlineChars = 1)

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun small_fontScales_headlineBadgeCta_notCut_allWidths_360() = assertSmallTexts(minHeadlineChars = 4)

    private fun assertSmallTexts(minHeadlineChars: Int) {
        var fontScale by mutableStateOf(1f)
        var assets by mutableStateOf(sampleNativeAdAssets)
        var slotHeight = 0.dp
        composeRule.setContent {
            WithFontScale(fontScale) {
                AdsTestTheme {
                    slotHeight = BottomAdBarDefaults.nativeSlotHeight()
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
                assertEquals("card height, $name", slotHeight.value, frame.h(), TOLERANCE)
                if (scale == 1f) assertEquals("card height at 1.0, $name", 106f, frame.h(), TOLERANCE)
                val headline = textLayout(texts.headline)
                val label = textLayout(badge)
                val cta = textLayout(texts.callToAction!!)
                val second = textLayout(texts.advertiser!!)
                listOf(
                    Triple("headline", texts.headline, headline),
                    Triple("Ad", badge, label),
                    Triple("CTA", texts.callToAction!!, cta),
                    Triple("second line", texts.advertiser!!, second),
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
                assertEquals("call to action width, $name", frame.w() - 24f, ctaBox.w(), TOLERANCE)
                if (texts === sampleNativeAdAssets) {
                    assertEquals("Install shown whole, $name", texts.callToAction!!.length, visibleChars(cta))
                    assertFalse("Install cut with an ellipsis, $name", cta.isLineEllipsized(0))
                    assertEquals("Install height, $name", 44f, ctaBox.h(), TOLERANCE)
                }
            }
        }
    }

    /**
     * KB-1 criterion 6, KB-32 (plan T-109, section 4): at every font scale and on the screens of 411, 360 and 320 dp,
     * the small native is as high as [BottomAdBarDefaults.nativeSlotHeight] read in the same composition (106 dp at
     * 1.0, more at 2.0, never less as the text grows), its placeholder next to it as high; the parts add up to that
     * height: the top row 6 dp below the card top, the call to action 6 dp below the top row, 6 dp above the card
     * bottom (1 dp tolerance: the height worked out before the ad against the real layout, plan section 3.4); the text
     * column ends within the top row; icon, text column and call to action never overlap.
     */
    @Test
    fun small_partsInsideCard_fontScales() = assertSmallParts()

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun small_partsInsideCard_fontScales_360() = assertSmallParts()

    @Test
    @Config(qualifiers = "w320dp-h640dp-xxhdpi")
    fun small_partsInsideCard_fontScales_320() = assertSmallParts()

    private fun DpRect.overlaps(other: DpRect): Boolean = left.value < other.right.value - TOLERANCE &&
        right.value > other.left.value + TOLERANCE &&
        top.value < other.bottom.value - TOLERANCE &&
        bottom.value > other.top.value + TOLERANCE

    private fun assertSmallParts() {
        var fontScale by mutableStateOf(1f)
        var assets by mutableStateOf(sampleNativeAdAssets)
        var slotHeight = 0.dp
        composeRule.setContent {
            WithFontScale(fontScale) {
                AdsTestTheme {
                    slotHeight = BottomAdBarDefaults.nativeSlotHeight()
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        NativeAdCard(assets, NativeAdSize.SMALL)
                        NativeAdPlaceholder(NativeAdSize.SMALL, animated = false)
                    }
                }
            }
        }
        listOf(sampleNativeAdAssets, longFullNativeAdAssets).forEach { texts ->
            var previous = 0f
            SMALL_SCALES.forEach { scale ->
                fontScale = scale
                assets = texts
                composeRule.waitForIdle()
                val card = bounds(NativeAdTestTags.CARD)
                val name = "small ${card.w()} dp, font scale $scale, ${textsName(texts)}"
                assertEquals("card as high as nativeSlotHeight(), $name", slotHeight.value, card.h(), TOLERANCE)
                when (scale) {
                    1f -> assertEquals("card at 1.0, $name", 106f, card.h(), TOLERANCE)
                    2f -> assertTrue("card at 2.0 is ${card.h()}, $name", card.h() > 106f + TOLERANCE)
                }
                assertTrue("card ${card.h()} lower than $previous at a smaller scale, $name", card.h() >= previous)
                previous = card.h()
                assertEquals(
                    "placeholder as high as the card, $name",
                    card.h(),
                    bounds(NativeAdTestTags.PLACEHOLDER).h(),
                    TOLERANCE,
                )
                val row = bounds(NativeAdTestTags.SMALL_TOP_ROW)
                val icon = bounds(NativeAdTestTags.ICON)
                val cta = bounds(NativeAdTestTags.CTA)
                val label = bounds(NativeAdTestTags.BADGE)
                val headline = bounds(NativeAdTestTags.HEADLINE)
                val second = bounds(NativeAdTestTags.SECOND_LINE)
                val column = DpRect(
                    minOf(label.left.value, headline.left.value, second.left.value).dp,
                    minOf(label.top.value, headline.top.value).dp,
                    maxOf(headline.right.value, second.right.value).dp,
                    second.bottom,
                )
                assertEquals("top row to card top, $name", 6f, (row.top - card.top).value, 1f)
                assertEquals("CTA to top row, $name", 6f, (cta.top - row.bottom).value, 1f)
                assertEquals("CTA to card bottom, $name", 6f, (card.bottom - cta.bottom).value, 1f)
                assertTrue(
                    "text column bottom ${column.bottom}, top row bottom ${row.bottom}, $name",
                    column.bottom.value <= row.bottom.value + TOLERANCE,
                )
                listOf(
                    Triple("icon", icon, "text column" to column),
                    Triple("icon", icon, "CTA" to cta),
                    Triple("text column", column, "CTA" to cta),
                ).forEach { (part, rect, other) ->
                    assertFalse(
                        "$part $rect overlaps ${other.first} ${other.second}, $name",
                        rect.overlaps(other.second),
                    )
                }
            }
        }
    }

    /**
     * Question 2 of plan T-109 (the rules of question 8 of plan T-065 dropped): at every font scale the "Ad" label
     * stands before the headline on its row and the line below keeps one line; without any text below the headline
     * there is no line below, and the label still stands before the headline.
     */
    @Test
    fun small_badgeBeforeHeadline_oneSecondLine_fontScales() {
        var fontScale by mutableStateOf(1f)
        var assets by mutableStateOf(longFullNativeAdAssets.copy(advertiser = null))
        composeRule.setContent {
            WithFontScale(fontScale) {
                AdsTestTheme { NativeAdCard(assets, NativeAdSize.SMALL, Modifier.width(CARD_WIDTH)) }
            }
        }
        val bodyText = longFullNativeAdAssets.body!!
        listOf(true, false).forEach { withSecondLine ->
            assets = if (withSecondLine) {
                longFullNativeAdAssets.copy(advertiser = null)
            } else {
                longFullNativeAdAssets.copy(advertiser = null, body = null)
            }
            SMALL_SCALES.forEach { scale ->
                fontScale = scale
                composeRule.waitForIdle()
                val case = "font scale $scale, ${if (withSecondLine) "body below" else "no text below"}"
                val label = bounds(NativeAdTestTags.BADGE)
                val headline = bounds(NativeAdTestTags.HEADLINE)
                assertTrue(
                    "label right ${label.right}, headline left ${headline.left}, $case",
                    label.right <= headline.left,
                )
                val centers =
                    abs((label.top.value + label.bottom.value) - (headline.top.value + headline.bottom.value)) / 2
                assertTrue("label and headline centers $centers dp apart, $case", centers < 2f)
                if (withSecondLine) {
                    assertEquals("body lines, $case", 1, textLayout(bodyText).lineCount)
                } else {
                    // Control: the headline is there, so the missing line is not a missing card.
                    composeRule.onNodeWithTag(NativeAdTestTags.HEADLINE, useUnmergedTree = true).assertExists()
                    composeRule.onNodeWithTag(NativeAdTestTags.SECOND_LINE, useUnmergedTree = true)
                        .assertDoesNotExist()
                }
            }
        }
    }

    /**
     * Plan T-109, section 3.5 (replaces the test of the former `bottomSlotHeight`): one function for the small native,
     * whole dp, the number of the design: 106 dp at font scale 1.0 at densities 2 and 3 (every line a whole number of
     * pixels); at 2.625 (420 dpi) each line is rounded up to a pixel, so 106 to 108 dp; more at font scale 2.0.
     */
    @Test
    fun smallNativeHeight_is106dp_atFontScale1_wholeDp() {
        lateinit var measurer: TextMeasurer
        lateinit var typography: Typography
        composeRule.setContent {
            AdsTestTheme {
                measurer = rememberTextMeasurer()
                typography = MaterialTheme.typography
            }
        }
        composeRule.waitForIdle()
        fun height(density: Float, fontScale: Float): Dp =
            smallNativeHeight(measurer, typography, Density(density = density, fontScale = fontScale))

        fun assertWholeDp(what: String, value: Dp) =
            assertEquals("$what whole dp", value.value.roundToInt().toFloat(), value.value, 0f)
        listOf(2f, 3f).forEach { density ->
            val value = height(density, 1f)
            assertEquals("density $density, font scale 1.0", 106f, value.value, 0f)
        }
        val at420 = height(2.625f, 1f)
        assertWholeDp("density 2.625, font scale 1.0: $at420", at420)
        assertTrue("density 2.625, font scale 1.0: $at420", at420.value in 106f..108f)
        // Control: larger text gives a taller card, still whole dp.
        val large = height(3f, 2f)
        assertWholeDp("density 3, font scale 2.0: $large", large)
        assertTrue("density 3, font scale 2.0: $large", large > 106.dp)
    }

    /**
     * Plan T-110, section 3.5 (KB-32 criterion 3, question 66): one function for medium, whole dp: 250 dp at font
     * scale 1.0 at densities 2, 3 and 2.625 (420 dpi, where the content block may be 107 to 108 dp), also with
     * `cta_large`; at font scale 2.0 [NativeAdDimens.mediumHeight] of [nativeContentHeight], more than 250 dp.
     */
    @Test
    fun mediumNativeHeight_is250dp_atFontScale1_wholeDp() {
        lateinit var textMeasurer: TextMeasurer
        lateinit var types: Typography
        composeRule.setContent {
            AdsTestTheme {
                textMeasurer = rememberTextMeasurer()
                types = MaterialTheme.typography
            }
        }
        composeRule.waitForIdle()
        fun assertWholeDp(what: String, value: Dp) =
            assertEquals("$what whole dp", value.value.roundToInt().toFloat(), value.value, 0f)
        listOf(false, true).forEach { large ->
            listOf(2f, 3f, 2.625f).forEach { scale ->
                val value = mediumNativeHeight(textMeasurer, types, Density(density = scale, fontScale = 1f), large)
                assertEquals("density $scale, font scale 1.0, ctaLarge $large", 250f, value.value, 0f)
            }
            val at2 = Density(density = 3f, fontScale = 2f)
            val grown = mediumNativeHeight(textMeasurer, types, at2, large)
            val content = nativeContentHeight(textMeasurer, types, at2, large)
            assertEquals(
                "density 3, font scale 2.0, ctaLarge $large",
                NativeAdDimens.mediumHeight(content).value,
                grown.value,
                0f,
            )
            assertWholeDp("density 3, font scale 2.0, ctaLarge $large: $grown", grown)
            assertTrue("density 3, font scale 2.0, ctaLarge $large: $grown", grown > 250.dp)
        }
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

        /** Small at the font scales of question 8 of plan T-065. */
        val SMALL_SCALES = listOf(1f, 1.3f, 1.5f, 2f)

        /** The "Ad" label is as high as one line of `labelSmall`: 16, 21 and 32 dp at font scales 1.0, 1.3, 2.0. */
        val BADGE_HEIGHTS = mapOf(1f to 16f, 1.3f to 21f, 2f to 32f)
        const val FRAME = "font_scale_test_frame"
        const val TOLERANCE = 0.5f
    }
}
