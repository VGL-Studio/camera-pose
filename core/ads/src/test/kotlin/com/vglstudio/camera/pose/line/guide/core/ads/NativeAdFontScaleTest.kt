package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
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
        val cardHeight: Dp,
        val modifier: Modifier,
    )

    @Test
    fun cardSizes_ctaTextFits_fontScales() {
        val cases = listOf(
            CardCase("small", NativeAdSize.SMALL, false, 34.dp, 80.dp, Modifier.width(CARD_WIDTH)),
            CardCase("medium", NativeAdSize.MEDIUM, false, 44.dp, 410.dp, Modifier.width(CARD_WIDTH)),
            CardCase("medium ctaLarge", NativeAdSize.MEDIUM, true, 56.dp, 422.dp, Modifier.width(CARD_WIDTH)),
            CardCase("full", NativeAdSize.FULL, false, 36.dp, 418.dp, Modifier.width(CARD_WIDTH)),
            CardCase("card", NativeAdSize.CARD, false, 36.dp, 300.dp, Modifier.size(180.dp, 300.dp)),
        )
        var fontScale by mutableStateOf(1f)
        var assets by mutableStateOf(sampleNativeAdAssets)
        var case by mutableStateOf(cases.first())
        composeRule.setContent {
            WithFontScale(fontScale) {
                AppTheme { NativeAdCard(assets, case.size, case.modifier, ctaLarge = case.ctaLarge) }
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
                        frameHeight = current.cardHeight,
                    )
                }
            }
        }
    }

    @Test
    fun medium_partsKeepSize_mediaShrinksFirst_fontScales() {
        var fontScale by mutableStateOf(1f)
        var assets by mutableStateOf(sampleNativeAdAssets)
        var ctaLarge by mutableStateOf(false)
        composeRule.setContent {
            WithFontScale(fontScale) {
                AppTheme { NativeAdCard(assets, NativeAdSize.MEDIUM, Modifier.width(CARD_WIDTH), ctaLarge = ctaLarge) }
            }
        }
        listOf(false, true).forEach { large ->
            SCALES.forEach { scale ->
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
                    assertEquals("card, $case", if (large) 422f else 410f, card.h(), TOLERANCE)
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
                    if (scale == 1f) assertEquals("media at the design size, $case", 196f, media.h(), TOLERANCE)
                }
            }
        }
    }

    @Test
    fun medium_mediaFloor120dp_cardGrows_atFontScale3() {
        composeRule.setContent {
            WithFontScale(3f) {
                AppTheme { NativeAdCard(longFullNativeAdAssets, NativeAdSize.MEDIUM, Modifier.width(CARD_WIDTH)) }
            }
        }
        val card = bounds(NativeAdTestTags.CARD)
        val cta = bounds(NativeAdTestTags.CTA)
        val media = bounds(NativeAdTestTags.MEDIA)
        assertEquals("media", 120f, media.h(), TOLERANCE)
        assertTrue("call to action is ${cta.h()} high", cta.h() >= 44f - TOLERANCE)
        assertTrue("card is ${card.h()} high", card.h() > 410f + TOLERANCE)
    }

    @Test
    fun step_mediaFloor120dp_cardGrows_fontScales() {
        var fontScale by mutableStateOf(1f)
        var assets by mutableStateOf(sampleNativeAdAssets)
        var stepHeight = 0.dp
        composeRule.setContent {
            WithFontScale(fontScale) {
                AppTheme {
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

    @Test
    fun collapsibleLayer_mediaFloor120dp_layerGrows_fontScales() {
        var fontScale by mutableStateOf(1f)
        var assets by mutableStateOf(sampleNativeAdAssets)
        composeRule.setContent {
            WithFontScale(fontScale) {
                AppTheme { CollapsibleNativeAdCard(assets, onCollapse = {}, Modifier.width(CARD_WIDTH)) }
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
                assertCtaFits(
                    case = case,
                    scale = scale,
                    ctaTag = NativeAdTestTags.CTA,
                    ctaText = texts.callToAction!!,
                    frameTag = CollapsibleNativeAdTestTags.AD,
                    designHeight = 36.dp,
                    frameHeight = when (scale) {
                        1f, 1.3f -> 250.dp
                        2f -> 272.dp
                        else -> null
                    },
                )
                val layer = bounds(CollapsibleNativeAdTestTags.AD)
                val collapse = bounds(CollapsibleNativeAdTestTags.COLLAPSE)
                assertEquals("collapse button to the top, $case", 4f, (collapse.top - layer.top).value, TOLERANCE)
                assertEquals("collapse button to the end, $case", 4f, (layer.right - collapse.right).value, TOLERANCE)
            }
        }
    }

    @Test
    fun fullScreenAfterInter_adBadgeKeepsTextHeight_fontScales() {
        var fontScale by mutableStateOf(1f)
        var assets by mutableStateOf(FakeNativeAdAssets.Design)
        composeRule.setContent {
            WithFontScale(fontScale) {
                AppTheme {
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
                assertEquals("close button width, $case", 36f, (close.right - close.left).value, TOLERANCE)
                assertEquals("close button height, $case", 36f, close.h(), TOLERANCE)
                assertEquals("close button left, $case", 16f, close.left.value, TOLERANCE)
                assertEquals("close button top, $case", 24f, close.top.value, TOLERANCE)
                if (scale == 1f && texts === FakeNativeAdAssets.Design) {
                    val icon = bounds(NativeFullScreenAdTestTags.ICON)
                    assertEquals("icon (head) at the design size, $case", 48f, icon.h(), TOLERANCE)
                    assertEquals("media at the design size, $case", 595f, media.h(), TOLERANCE)
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
                AppTheme {
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

        /** The "Ad" label is as high as one line of `labelSmall`: 16, 21 and 32 dp at font scales 1.0, 1.3, 2.0. */
        val BADGE_HEIGHTS = mapOf(1f to 16f, 1.3f to 21f, 2f to 32f)
        const val FRAME = "font_scale_test_frame"
        const val TOLERANCE = 0.5f
    }
}
