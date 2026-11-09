package com.vglstudio.camera.pose.line.guide.feature.onboarding

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.ads.NativeAdAssets
import com.vglstudio.camera.pose.line.guide.core.ads.NativeAdCard
import com.vglstudio.camera.pose.line.guide.core.ads.NativeAdPlaceholder
import com.vglstudio.camera.pose.line.guide.core.ads.NativeAdSize
import com.vglstudio.camera.pose.line.guide.core.ads.NativeStepAdDefaults
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PrimaryButtonTestTags
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.feature.onboarding.OnboardingPage.CAMERA
import com.vglstudio.camera.pose.line.guide.feature.onboarding.OnboardingPage.GUIDE
import com.vglstudio.camera.pose.line.guide.feature.onboarding.OnboardingPage.INTRO
import com.vglstudio.camera.pose.line.guide.feature.onboarding.OnboardingPage.POSES
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * UI behaviour of the stateless onboarding flow, run on the JVM with Robolectric (plan T-012, section 4; plan T-082,
 * section 4; plan T-091, section 4). Native graphics give real text sizes. Layout numbers come from design/context/ (411 x 891, Vietnamese
 * text of the design); the short frame 360 x 640 is checked in the seven release languages (L-028).
 * setContent may run once per test, so [setScreen] keeps the state, the theme and the ad slot in mutable state that a
 * test changes afterwards.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = DESIGN_FRAME_VI)
class OnboardingScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private class ScreenState(
        uiState: OnboardingUiState,
        darkTheme: Boolean,
        adSlot: @Composable (OnboardingPage) -> Unit,
        nextEnabled: Boolean,
    ) {
        var uiState by mutableStateOf(uiState)
        var darkTheme by mutableStateOf(darkTheme)
        var adSlot by mutableStateOf(adSlot)
        var nextEnabled by mutableStateOf(nextEnabled)
    }

    private fun setScreen(
        uiState: OnboardingUiState = OnboardingUiState.Browsing(PAGES_4, 0),
        darkTheme: Boolean = false,
        onNext: () -> Unit = {},
        onBack: () -> Unit = {},
        onEnableCamera: () -> Unit = {},
        adSlot: @Composable (OnboardingPage) -> Unit = {},
        nextEnabled: Boolean = true,
    ): ScreenState {
        val screen = ScreenState(uiState, darkTheme, adSlot, nextEnabled)
        composeRule.setContent {
            AppTheme(darkTheme = screen.darkTheme) {
                OnboardingScreen(
                    uiState = screen.uiState,
                    onNext = onNext,
                    onBack = onBack,
                    onEnableCamera = onEnableCamera,
                    adSlot = screen.adSlot,
                    nextEnabled = screen.nextEnabled,
                )
            }
        }
        composeRule.waitForIdle()
        return screen
    }

    private fun ScreenState.show(state: OnboardingUiState) {
        uiState = state
        composeRule.waitForIdle()
    }

    // ---- Layout against design/context/ (4 dp tolerance; the code button is 56 high, the design one 60) ----

    @Test
    fun intro_layoutMatchesDesign() {
        setScreen(OnboardingUiState.Browsing(PAGES_4, 0))
        assertPageLayout(INTRO)
        assertDp("page area top", 64f, bounds(OnboardingTestTags.PAGE_AREA).top)
    }

    @Test
    fun poses_layoutMatchesDesign() {
        setScreen(OnboardingUiState.Browsing(PAGES_4, 1))
        assertPageLayout(POSES)
    }

    @Test
    fun guide_layoutMatchesDesign() {
        setScreen(OnboardingUiState.Browsing(PAGES_4, 2))
        assertPageLayout(GUIDE)
        // The bubble text is centered on 76.8% and 15.2% of the art (art 317 wide, centered: left 47).
        val bubble = bounds(OnboardingTestTags.BUBBLE)
        assertDp("bubble center x", 290f, (bubble.left + bubble.right) / 2)
        assertDp("bubble center y", 142.5f, (bubble.top + bubble.bottom) / 2)
    }

    @Test
    fun camera_layoutMatchesDesign() {
        setScreen(OnboardingUiState.Browsing(PAGES_4, 3))
        assertPageLayout(CAMERA)
        val note = bounds(OnboardingTestTags.NOTE)
        assertDp("note bottom", 787f, note.bottom)
        assertDp("note width", 363f, note.width)
        val button = bounds(OnboardingTestTags.ENABLE_CAMERA)
        assertEquals("note to button", 24f, (button.top - note.bottom).value, 1f)
    }

    /**
     * Page without a card (design/context/, 411 x 891): illustration 88, 360 high; title 472; body 24 below the title,
     * whether the title takes one line or two (plan T-091, version 2); the button at 811 on every page.
     */
    private fun assertPageLayout(page: OnboardingPage) {
        val illustration = bounds(OnboardingTestTags.ILLUSTRATION)
        assertDp("$page: illustration left", 24f, illustration.left)
        assertDp("$page: illustration top", 88f, illustration.top)
        assertDp("$page: illustration width", 363f, illustration.width)
        // Also the "full height when there is room" check (plan T-012, section 4).
        assertDp("$page: illustration height", 360f, illustration.height)
        assertArtDrawn(page)
        val title = bounds(OnboardingTestTags.TITLE)
        val lines = titleLines(page, title)
        assertDp("$page: title top", 472f, title.top)
        val body = bounds(OnboardingTestTags.BODY)
        assertDp("$page: body top, title bottom + 24 ($lines-line title)", (title.bottom + 24.dp).value, body.top)
        // Control with the numbers of the design when the title takes one line.
        if (lines == 1) assertDp("$page: body top, one-line title", 532f, body.top)
        val button = bounds(if (page == CAMERA) OnboardingTestTags.ENABLE_CAMERA else OnboardingTestTags.NEXT)
        assertDp("$page: button width", 363f, button.width)
        assertTrue("$page: button height ${button.height}", button.height >= 56.dp)
        assertDp("$page: button top", 811f, button.top)
        assertDp("$page: button bottom", 867f, button.bottom)
    }

    /** The illustration of [page] draws its art (the WebP of res/drawable-nodpi), not an empty frame. */
    private fun assertArtDrawn(page: OnboardingPage) {
        val illustration = bounds(OnboardingTestTags.ILLUSTRATION)
        val count = artPixels(OnboardingTestTags.ILLUSTRATION, 0.dp, illustration.width)
        assertTrue("$page: $count orange points of the art", count > MIN_ART_PIXELS)
    }

    /** The title takes one line (36) or two (72) of headlineMedium; any other height fails, with the count. */
    private fun titleLines(page: OnboardingPage, title: DpRect): Int {
        val lines = when {
            kotlin.math.abs(title.height.value - 36f) <= 1f -> 1
            kotlin.math.abs(title.height.value - 72f) <= 1f -> 2
            else -> 0
        }
        assertTrue("$page: title height ${title.height} is neither one line (36) nor two (72)", lines > 0)
        return lines
    }

    @Test
    @Config(qualifiers = UNFOLDED_VI)
    fun unfolded_layoutMatchesDesign() {
        val screen = setScreen(OnboardingUiState.Browsing(PAGES_4, 0))
        val illustration = bounds(OnboardingTestTags.ILLUSTRATION)
        assertDp("illustration left", 24f, illustration.left)
        assertDp("illustration top", 88f, illustration.top)
        assertDp("illustration width", 625f, illustration.width)
        assertDp("illustration height", 360f, illustration.height)
        // The art (about 352 wide) is centered in the 625 frame: nothing of it in the 120 dp at either side.
        assertTrue("art in the middle", artPixels(OnboardingTestTags.ILLUSTRATION, 120.dp, 505.dp) > MIN_ART_PIXELS)
        assertEquals("art on the left", 0, artPixels(OnboardingTestTags.ILLUSTRATION, 0.dp, 120.dp))
        assertEquals("art on the right", 0, artPixels(OnboardingTestTags.ILLUSTRATION, 505.dp, 625.dp))
        val title = bounds(OnboardingTestTags.TITLE)
        assertDp("title top", 472f, title.top)
        assertDp("body top", (title.bottom + 24.dp).value, bounds(OnboardingTestTags.BODY).top)
        if (titleLines(INTRO, title) ==
            1
        ) {
            assertDp("body top, one-line title", 532f, bounds(OnboardingTestTags.BODY).top)
        }
        val next = bounds(OnboardingTestTags.NEXT)
        assertDp("next width", 625f, next.width)
        assertDp("next top", 761f, next.top)
        assertDp("next bottom", 817f, next.bottom)
        screen.show(OnboardingUiState.Browsing(PAGES_4, 3))
        assertDp("note bottom", 737f, bounds(OnboardingTestTags.NOTE).bottom)
        assertDp("enable camera top", 761f, bounds(OnboardingTestTags.ENABLE_CAMERA).top)
    }

    // ---- Colors of the design system ----

    @Test
    fun eachPage_drawsItsArt() {
        val screen = setScreen(OnboardingUiState.Browsing(PAGES_4, 0), darkTheme = false)
        assertColor("background", 0xFFFFFDF8, pixelAt(OnboardingTestTags.SCREEN, 2.dp, 2.dp))
        val masks = PAGES_4.indices.map { index ->
            screen.show(OnboardingUiState.Browsing(PAGES_4, index))
            val illustration = bounds(OnboardingTestTags.ILLUSTRATION)
            val count = artPixels(OnboardingTestTags.ILLUSTRATION, 0.dp, illustration.width)
            assertTrue("page ${index + 1}: $count orange points of the art", count > MIN_ART_PIXELS)
            artMask(OnboardingTestTags.ILLUSTRATION)
        }
        // Control: each page draws its own art, not one image for all.
        assertEquals("distinct arts", PAGES_4.size, masks.toSet().size)
    }

    @Test
    fun guideBubble_text_dark() {
        setScreen(OnboardingUiState.Browsing(PAGES_4, 2), darkTheme = true)
        val (darkest, brightest) = darkestAndBrightest(OnboardingTestTags.BUBBLE)
        // Control: the bubble of the art behind the text stays white in the dark theme.
        assertColor("bubble behind the text", 0xFFFFFFFF, brightest)
        // The text keeps the dark category/onCategory, not the light onBackground of the dark theme.
        assertColor("bubble text, dark", 0xFF111111, darkest)
    }

    @Test
    @Config(qualifiers = "en-$DESIGN_FRAME")
    fun guideBubble_fits_en() = assertBubbleFits("en")

    @Test
    fun guideBubble_fits_vi() = assertBubbleFits("vi")

    @Test
    @Config(qualifiers = "hi-$DESIGN_FRAME")
    fun guideBubble_fits_hi() = assertBubbleFits("hi")

    @Test
    @Config(qualifiers = "pt-rBR-$DESIGN_FRAME")
    fun guideBubble_fits_ptBR() = assertBubbleFits("pt-BR")

    @Test
    @Config(qualifiers = "b+es+419-$DESIGN_FRAME")
    fun guideBubble_fits_es419() = assertBubbleFits("es-419")

    @Test
    @Config(qualifiers = "in-$DESIGN_FRAME")
    fun guideBubble_fits_in() = assertBubbleFits("in")

    @Test
    @Config(qualifiers = "th-$DESIGN_FRAME")
    fun guideBubble_fits_th() = assertBubbleFits("th")

    // The bubble text follows the art, not the font size of the device.
    @Test
    @Config(qualifiers = DESIGN_FRAME_VI, fontScale = 2f)
    fun guideBubble_fits_fontScale2() = assertBubbleFits("vi, font scale 2")

    /** One line, at most 41% of the width of the art (design README, section 3). */
    private fun assertBubbleFits(language: String) {
        setScreen(OnboardingUiState.Browsing(PAGES_4, 2))
        // Control: the bubble shows the text of the language.
        composeRule.onNodeWithTag(OnboardingTestTags.BUBBLE)
            .assertTextEquals(composeRule.activity.getString(R.string.onboarding_guide_bubble))
        val illustration = bounds(OnboardingTestTags.ILLUSTRATION)
        val artWidth = minOf(illustration.width, illustration.height * GUIDE_ART_RATIO)
        val bubble = bounds(OnboardingTestTags.BUBBLE)
        assertTrue("$language: bubble drawn", bubble.width > 0.dp)
        assertTrue(
            "$language: bubble ${bubble.width} wider than 41% of the art $artWidth",
            bubble.width <= artWidth * 0.41f,
        )
        val lineHeight = bubble.height / illustration.height
        assertTrue("$language: one line, bubble height ${bubble.height}", lineHeight <= 16f / 289f * 1.5f)
    }

    @Test
    fun camera_colors_dark() {
        val screen = setScreen(OnboardingUiState.Browsing(PAGES_4, 3), darkTheme = true)
        assertColor("background", 0xFF121212, pixelAt(OnboardingTestTags.SCREEN, 2.dp, 2.dp))
        assertColor("button", 0xFFFFFFFF, pixelAt(OnboardingTestTags.ENABLE_CAMERA, 8.dp, 30.dp))
        screen.show(OnboardingUiState.Browsing(PAGES_4, 0))
        assertColor("next button", 0xFFFFFFFF, pixelAt(OnboardingTestTags.NEXT, 8.dp, 30.dp))
    }

    // ---- Pages and texts ----

    @Test
    fun eachPage_showsItsTexts() {
        val screen = setScreen()
        val union = mutableSetOf<String>()
        PAGES_4.forEachIndexed { index, page ->
            screen.show(OnboardingUiState.Browsing(PAGES_4, index))
            val own = PAGE_TEXTS.getValue(page)
            own.forEach { text -> composeRule.onNodeWithText(text).assertExists("$page: $text") }
            (DESIGN_TEXTS - own.toSet()).forEach { text ->
                composeRule.onNodeWithText(text).assertDoesNotExist()
            }
            union += own
        }
        assertEquals(DESIGN_TEXTS.toSet(), union)
    }

    @Test
    fun noPage_mentionsPremium() {
        val screen = setScreen()
        PAGES_4.indices.forEach { index ->
            screen.show(OnboardingUiState.Browsing(PAGES_4, index))
            composeRule.onAllNodes(hasText("premium", substring = true, ignoreCase = true)).assertCountEquals(0)
            composeRule.onNodeWithTag(OnboardingTestTags.pageDot(4)).assertExists()
        }
    }

    @Test
    fun lastPage_showsEnableCamera_notNext() {
        val screen = setScreen()
        PAGES_4.indices.forEach { index ->
            screen.show(OnboardingUiState.Browsing(PAGES_4, index))
            if (index == PAGES_4.lastIndex) {
                composeRule.onNodeWithTag(OnboardingTestTags.ENABLE_CAMERA).assertExists()
                composeRule.onNodeWithTag(OnboardingTestTags.NEXT).assertDoesNotExist()
            } else {
                composeRule.onNodeWithTag(OnboardingTestTags.NEXT).assertExists()
                composeRule.onNodeWithTag(OnboardingTestTags.ENABLE_CAMERA).assertDoesNotExist()
            }
        }
    }

    @Test
    fun pageIndicator_describesCurrentPage() {
        val screen = setScreen(OnboardingUiState.Browsing(PAGES_4, 1))
        composeRule.onNodeWithTag(OnboardingTestTags.PAGE_INDICATOR).assertContentDescriptionEquals("Trang 2 trên 4")
        screen.show(OnboardingUiState.Browsing(THREE_PAGES, 0))
        composeRule.onNodeWithTag(OnboardingTestTags.PAGE_INDICATOR).assertContentDescriptionEquals("Trang 1 trên 3")
    }

    @Test
    fun pageIndicator_layoutAndColors() {
        val screen = setScreen(OnboardingUiState.Browsing(PAGES_4, 1), darkTheme = false)
        val row = bounds(OnboardingTestTags.PAGE_INDICATOR)
        assertDp("row left", 24f, row.left)
        assertDp("row top", 56f, row.top)
        assertDp("row height", 8f, row.height)
        assertDp("row width", 72f, row.width)
        val current = bounds(OnboardingTestTags.pageDot(2))
        assertDp("current dot left", 40f, current.left)
        assertDp("current dot width", 24f, current.width)
        assertDp("current dot height", 8f, current.height)
        listOf(1 to 24f, 3 to 72f, 4 to 88f).forEach { (number, left) ->
            val dot = bounds(OnboardingTestTags.pageDot(number))
            assertDp("dot $number left", left, dot.left)
            assertDp("dot $number width", 8f, dot.width)
            assertDp("dot $number height", 8f, dot.height)
        }
        assertColor("current dot, light", 0xFF111111, pixelAt(OnboardingTestTags.pageDot(2), 12.dp, 4.dp))
        assertColor("other dot, light", 0xFFD9D4C8, pixelAt(OnboardingTestTags.pageDot(1), 4.dp, 4.dp))
        screen.darkTheme = true
        composeRule.waitForIdle()
        assertColor("current dot, dark", 0xFFFFFFFF, pixelAt(OnboardingTestTags.pageDot(2), 12.dp, 4.dp))
        assertColor("other dot, dark", 0xFF4A4744, pixelAt(OnboardingTestTags.pageDot(1), 4.dp, 4.dp))
        // Control: three pages, three dots.
        screen.show(OnboardingUiState.Browsing(THREE_PAGES, 0))
        assertDp("row width, three pages", 56f, bounds(OnboardingTestTags.PAGE_INDICATOR).width)
        composeRule.onNodeWithTag(OnboardingTestTags.pageDot(4)).assertDoesNotExist()
    }

    // ---- Pinned button and the ad slot of T-083 ----

    @Test
    fun bottomButton_samePlace_onEveryPage_withAndWithoutAdSlot() {
        val screen = setScreen()
        val places = mutableListOf<Pair<String, DpRect>>()
        val contentHeights = mutableMapOf<Pair<Boolean, Int>, Dp>()
        listOf(false, true).forEach { withAd ->
            screen.adSlot = if (withAd) AD_BLOCK else NO_AD
            PAGES_4.indices.forEach { index ->
                val state = OnboardingUiState.Browsing(PAGES_4, index)
                screen.show(state)
                places += "ad $withAd, page ${index + 1}" to bounds(primaryButtonTag(state))
                contentHeights[withAd to index] = bounds(OnboardingTestTags.CONTENT).height
            }
        }
        val first = places.first().second
        places.forEach { (what, place) ->
            assertEquals("$what top", first.top, place.top)
            assertEquals("$what bottom", first.bottom, place.bottom)
            assertEquals("$what left", first.left, place.left)
            assertEquals("$what width", first.width, place.width)
        }
        // Control: the block really takes 250 dp from the page area.
        PAGES_4.indices.forEach { index ->
            val shrink = contentHeights.getValue(false to index) - contentHeights.getValue(true to index)
            assertEquals("page ${index + 1}: content shorter by the block", 250f, shrink.value, 0.5f)
        }
    }

    @Test
    fun adSlot_betweenPageAndButton_belowNoteNever() {
        setScreen(OnboardingUiState.Browsing(PAGES_4, 3), adSlot = AD_BLOCK)
        val pageArea = bounds(OnboardingTestTags.PAGE_AREA)
        val slot = bounds(OnboardingTestTags.AD_SLOT)
        val note = bounds(OnboardingTestTags.NOTE)
        val button = bounds(OnboardingTestTags.ENABLE_CAMERA)
        assertEquals("slot height", 250f, slot.height.value, 0.5f)
        assertTrue("slot top ${slot.top} above page area bottom ${pageArea.bottom}", slot.top >= pageArea.bottom)
        assertTrue("slot bottom ${slot.bottom} below note top ${note.top}", slot.bottom <= note.top)
        assertTrue("note bottom ${note.bottom} below button top ${button.top}", note.bottom <= button.top)
    }

    @Test
    fun adSlot_receivesCurrentPage() {
        var received: OnboardingPage? = null
        val screen = setScreen(adSlot = { page -> received = page })
        val states = PAGES_4.indices.map { OnboardingUiState.Browsing(PAGES_4, it) } +
            OnboardingUiState.Browsing(PAGES_4, 1) +
            OnboardingUiState.RequestingPermission(PAGES_4)
        states.forEach { state ->
            screen.show(state)
            assertEquals("$state", state.page, received)
        }
    }

    // ---- The card of T-083 (spec onboarding, KB-6; plan T-083, section 4) ----

    @Test
    fun adSlot_card_onEveryPage_heightOfStepCard() {
        val screen = setScreen(adSlot = cardSlot)
        PAGES_4.indices.forEach { index ->
            val state = OnboardingUiState.Browsing(PAGES_4, index)
            screen.show(state)
            val slot = bounds(OnboardingTestTags.AD_SLOT)
            assertEquals("page ${index + 1}: slot height", stepHeight.value, slot.height.value, 1f)
            val below = if (state.isLastPage) OnboardingTestTags.NOTE else OnboardingTestTags.NEXT
            assertTrue("page ${index + 1}: slot bottom ${slot.bottom} over $below", slot.bottom <= bounds(below).top)
        }
        // Control: the card is the step card of the framework, 250 dp at the default font scale.
        assertEquals(250f, stepHeight.value, 0.5f)
    }

    @Test
    fun introAd_layoutMatchesDesign() {
        setScreen(OnboardingUiState.Browsing(PAGES_4, 0), adSlot = cardSlot)
        val pageArea = bounds(OnboardingTestTags.PAGE_AREA)
        assertDp("page area top", 64f, pageArea.top)
        assertDp("page area height", 473f, pageArea.height)
        assertAdPageLayout(INTRO, cardTop = 537f, oneLine = OneLineNumbers(293f, 405f, 465f))
        // Control: the card takes its room from the illustration (360 without the card).
        val illustration = bounds(OnboardingTestTags.ILLUSTRATION)
        assertTrue("illustration height ${illustration.height}", illustration.height < 360.dp)
        val next = bounds(OnboardingTestTags.NEXT)
        assertDp("next top", 811f, next.top)
        assertDp("next bottom", 867f, next.bottom)
    }

    @Test
    fun posesAd_layoutMatchesDesign() {
        setScreen(OnboardingUiState.Browsing(PAGES_4, 1), adSlot = cardSlot)
        assertAdPageLayout(POSES, cardTop = 537f, oneLine = OneLineNumbers(293f, 405f, 465f))
        assertDp("next top", 811f, bounds(OnboardingTestTags.NEXT).top)
    }

    @Test
    fun guideAd_layoutMatchesDesign() {
        setScreen(OnboardingUiState.Browsing(PAGES_4, 2), adSlot = cardSlot)
        assertAdPageLayout(GUIDE, cardTop = 537f, oneLine = OneLineNumbers(293f, 405f, 465f))
        assertDp("next top", 811f, bounds(OnboardingTestTags.NEXT).top)
    }

    @Test
    fun cameraAd_layoutMatchesDesign() {
        setScreen(OnboardingUiState.Browsing(PAGES_4, 3), adSlot = cardSlot)
        assertDp("page area height", 417f, bounds(OnboardingTestTags.PAGE_AREA).height)
        assertAdPageLayout(CAMERA, cardTop = 481f, oneLine = OneLineNumbers(237f, 349f, 409f))
        val card = assertCard(top = 481f)
        assertDp("card bottom", 731f, card.bottom)
        val note = bounds(OnboardingTestTags.NOTE)
        assertDp("note top", 755f, note.top)
        assertDp("note bottom", 787f, note.bottom)
        val button = bounds(OnboardingTestTags.ENABLE_CAMERA)
        assertDp("button top", 811f, button.top)
        assertDp("button bottom", 867f, button.bottom)
        assertEquals("card to note", 24f, (note.top - card.bottom).value, 1f)
        assertEquals("note to button", 24f, (button.top - note.bottom).value, 1f)
    }

    @Test
    fun introAdWait_layoutMatchesDesign() {
        setScreen(
            OnboardingUiState.Browsing(PAGES_4, 0),
            adSlot = { NativeAdPlaceholder(NativeAdSize.STEP, Modifier.fillMaxWidth(), animated = false) },
            nextEnabled = false,
        )
        val slot = bounds(OnboardingTestTags.AD_SLOT)
        assertDp("placeholder top", 537f, slot.top)
        assertDp("placeholder height", 250f, slot.height)
        val next = bounds(OnboardingTestTags.NEXT)
        assertDp("next top", 811f, next.top)
        assertDp("next width", 363f, next.width)
        composeRule.onNodeWithTag(OnboardingTestTags.NEXT).assertIsNotEnabled()
    }

    @Test
    fun camera_withCard_noteAndButtonBelowCard() {
        setScreen(OnboardingUiState.Browsing(PAGES_4, 3), adSlot = cardSlot)
        val card = bounds(OnboardingTestTags.AD_CARD)
        val note = bounds(OnboardingTestTags.NOTE)
        val button = bounds(OnboardingTestTags.ENABLE_CAMERA)
        assertTrue("card ${card.height} drawn", card.height > 0.dp)
        assertTrue("card bottom ${card.bottom} over note top ${note.top}", card.bottom <= note.top)
        assertTrue("note bottom ${note.bottom} over button top ${button.top}", note.bottom <= button.top)
    }

    @Test
    @Config(qualifiers = "vi-$SHORT_SCREEN")
    fun small_withCard_buttonInWindow_pageScrolls() {
        val screen = setScreen(OnboardingUiState.Browsing(PAGES_4, 0), adSlot = cardSlot)
        assertShortWithCard(INTRO, pageArea = 222f, cardTop = 286f, cardBottom = 536f)
        screen.show(OnboardingUiState.Browsing(PAGES_4, 3))
        assertShortWithCard(CAMERA, pageArea = 166f, cardTop = 230f, cardBottom = 480f)
        val note = bounds(OnboardingTestTags.NOTE)
        assertDp("note top", 504f, note.top)
        assertDp("note bottom", 536f, note.bottom)
        assertTrue("note bottom ${note.bottom} inside the window", note.bottom <= SHORT_SCREEN_HEIGHT)
        val cardToNote = note.top - bounds(OnboardingTestTags.AD_CARD).bottom
        assertTrue("note $cardToNote below the card, at least 24", cardToNote.value >= 24f - 0.5f)
    }

    private fun assertShortWithCard(page: OnboardingPage, pageArea: Float, cardTop: Float, cardBottom: Float) {
        val button = bounds(if (page == CAMERA) OnboardingTestTags.ENABLE_CAMERA else OnboardingTestTags.NEXT)
        assertDp("$page: button top", 560f, button.top)
        assertDp("$page: button bottom", 616f, button.bottom)
        assertTrue("$page: button bottom ${button.bottom} inside the window", button.bottom <= SHORT_SCREEN_HEIGHT)
        val area = bounds(OnboardingTestTags.PAGE_AREA)
        assertDp("$page: page area height", pageArea, area.height)
        val card = bounds(OnboardingTestTags.AD_CARD)
        assertDp("$page: card top", cardTop, card.top)
        assertDp("$page: card bottom", cardBottom, card.bottom)
        val last = LAST_ELEMENT.getValue(page)
        // Control: before scrolling, the last element is below the page area.
        assertTrue("$page: $last before scrolling", bounds(last).bottom > area.bottom)
        composeRule.onNodeWithTag(last).performScrollTo()
        val scrolled = bounds(last)
        assertTrue("$page: $last top ${scrolled.top} in the page area", scrolled.top >= area.top)
        assertTrue("$page: $last bottom ${scrolled.bottom} in the page area", scrolled.bottom <= area.bottom)
    }

    @Test
    @Config(qualifiers = "vi-$SHORT_SCREEN", fontScale = 2f)
    fun small_withCard_fontScale2_buttonAndNoteInWindow() {
        setScreen(OnboardingUiState.Browsing(PAGES_4, 3), adSlot = cardSlot)
        // Control: the card grows with large text.
        assertTrue("step card height $stepHeight", stepHeight > 250.dp)
        val card = bounds(OnboardingTestTags.AD_CARD)
        assertEquals("card height", stepHeight.value, card.height.value, 1f)
        val note = bounds(OnboardingTestTags.NOTE)
        val button = bounds(OnboardingTestTags.ENABLE_CAMERA)
        assertTrue("button bottom ${button.bottom} inside the window", button.bottom <= SHORT_SCREEN_HEIGHT)
        assertTrue("note bottom ${note.bottom} inside the window", note.bottom <= SHORT_SCREEN_HEIGHT)
        assertTrue("note top ${note.top} below the card ${card.bottom}", note.top >= card.bottom)
        val area = bounds(OnboardingTestTags.PAGE_AREA)
        assertTrue("page area height ${area.height}", area.height > 0.dp)
        composeRule.onNodeWithTag(OnboardingTestTags.BODY).performScrollTo()
        val body = bounds(OnboardingTestTags.BODY)
        assertTrue("body top ${body.top} in the page area", body.top >= area.top)
    }

    @Test
    fun nextButton_touchArea_notOverCard_w411() = assertTouchAreaNotOverCard()

    @Test
    @Config(qualifiers = "vi-$SHORT_SCREEN")
    fun nextButton_touchArea_notOverCard_small() = assertTouchAreaNotOverCard()

    private fun assertTouchAreaNotOverCard() {
        setScreen(OnboardingUiState.Browsing(PAGES_4, 0), adSlot = cardSlot)
        val touch = composeRule.onNodeWithTag(OnboardingTestTags.NEXT).fetchSemanticsNode().touchBoundsInRoot
        val card = composeRule.onNodeWithTag(OnboardingTestTags.AD_CARD).fetchSemanticsNode().boundsInRoot
        assertTrue("card drawn: $card", card.height > 0f)
        assertFalse("touch area $touch over the card $card", touch.overlaps(card))
    }

    @Test
    fun nextDisabled_whenNextEnabledFalse() {
        var clicks = 0
        val screen = setScreen(OnboardingUiState.Browsing(PAGES_4, 0), onNext = { clicks++ }, nextEnabled = false)
        composeRule.onNodeWithTag(OnboardingTestTags.NEXT).assertIsNotEnabled().performClick()
        assertEquals("held Next does nothing", 0, clicks)
        // Control: enabled again, one tap calls onNext once.
        screen.nextEnabled = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(OnboardingTestTags.NEXT).assertIsEnabled().performClick()
        assertEquals(1, clicks)
        // "Turn on camera" on the last page does not follow nextEnabled.
        screen.nextEnabled = false
        screen.show(OnboardingUiState.Browsing(PAGES_4, 3))
        composeRule.onNodeWithTag(OnboardingTestTags.ENABLE_CAMERA).assertIsEnabled()
    }

    @Test
    fun realCard_noAdsController_takesNoSpace() {
        val screen = setScreen(OnboardingUiState.Browsing(PAGES_4, 0), adSlot = { OnboardingAdCard(it) })
        assertEquals("slot height", 0f, bounds(OnboardingTestTags.AD_SLOT).height.value, 0.5f)
        val withRealCard = bounds(OnboardingTestTags.NEXT)
        screen.adSlot = NO_AD
        composeRule.waitForIdle()
        assertEquals("next top", bounds(OnboardingTestTags.NEXT).top, withRealCard.top)
        // Control: a slot with content takes its height.
        screen.adSlot = AD_BLOCK
        composeRule.waitForIdle()
        assertEquals("slot height with a block", 250f, bounds(OnboardingTestTags.AD_SLOT).height.value, 0.5f)
    }

    /** Illustration height, title top and body top of the design when the title takes one line. */
    private data class OneLineNumbers(val illustrationHeight: Float, val titleTop: Float, val bodyTop: Float)

    /**
     * Page with a card: the illustration at 88 fills what the texts leave above the card (ShrinkFirstLayout), the
     * title 24 below it, the body 24 below the title, the body ends at least 24 above the card (plan T-091, version 2).
     */
    private fun assertAdPageLayout(page: OnboardingPage, cardTop: Float, oneLine: OneLineNumbers) {
        val area = bounds(OnboardingTestTags.PAGE_AREA)
        val card = assertCard(top = cardTop)
        val illustration = bounds(OnboardingTestTags.ILLUSTRATION)
        val title = bounds(OnboardingTestTags.TITLE)
        val body = bounds(OnboardingTestTags.BODY)
        val lines = titleLines(page, title)
        assertDp("$page: illustration left", 24f, illustration.left)
        assertDp("$page: illustration top", 88f, illustration.top)
        assertDp("$page: illustration width", 363f, illustration.width)
        assertArtDrawn(page)
        val texts = body.bottom - title.top
        val expected = (area.height - 48.dp - texts - 24.dp).coerceIn(160.dp, 360.dp)
        assertDp("$page: illustration height ($lines-line title)", expected.value, illustration.height)
        assertDp("$page: title top, illustration bottom + 24", (illustration.bottom + 24.dp).value, title.top)
        assertDp("$page: body top, title bottom + 24", (title.bottom + 24.dp).value, body.top)
        // Half a dp for pixel rounding at xxhdpi.
        assertTrue(
            "$page: body bottom ${body.bottom} over card top ${card.top} - 24",
            body.bottom.value <= (card.top - 24.dp).value + 0.5f,
        )
        // Control with the numbers of the design when the title takes one line.
        if (lines == 1) {
            assertDp("$page: illustration height, one-line title", oneLine.illustrationHeight, illustration.height)
            assertDp("$page: title top, one-line title", oneLine.titleTop, title.top)
            assertDp("$page: body top, one-line title", oneLine.bodyTop, body.top)
        }
    }

    /** The card, left 24, 363 wide, as high as the step card of the framework; its bounds. */
    private fun assertCard(top: Float): DpRect {
        val card = bounds(OnboardingTestTags.AD_CARD)
        assertDp("card left", 24f, card.left)
        assertDp("card top", top, card.top)
        assertDp("card width", 363f, card.width)
        assertEquals("card height", stepHeight.value, card.height.value, 1f)
        return card
    }

    /** Height of the step card read in composition, at the font scale of the test. */
    private var stepHeight: Dp = 0.dp

    /** A fake step card with the texts of the reference images of the framework (NativeAdCard_step_*). */
    private val cardSlot: @Composable (OnboardingPage) -> Unit = {
        stepHeight = NativeStepAdDefaults.height()
        NativeAdCard(FAKE_AD, NativeAdSize.STEP, Modifier.fillMaxWidth().testTag(OnboardingTestTags.AD_CARD))
    }

    // ---- Clicks and Back ----

    @Test
    fun enableCamera_click_callsCallbackOnce() {
        var clicks = 0
        setScreen(OnboardingUiState.Browsing(PAGES_4, 3), onEnableCamera = { clicks++ })
        // Control: the button is on the last page.
        composeRule.onNodeWithTag(OnboardingTestTags.PAGE_CAMERA).assertExists()
        composeRule.onNodeWithTag(OnboardingTestTags.ENABLE_CAMERA).performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun next_click_callsOnNextOnce() {
        var clicks = 0
        setScreen(OnboardingUiState.Browsing(PAGES_4, 0), onNext = { clicks++ })
        composeRule.onNodeWithTag(OnboardingTestTags.NEXT).performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun requesting_showsSpinner_andIgnoresClicks() {
        var clicks = 0
        setScreen(OnboardingUiState.RequestingPermission(PAGES_4), onEnableCamera = { clicks++ })
        composeRule.onNodeWithTag(PrimaryButtonTestTags.SPINNER, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(OnboardingTestTags.ENABLE_CAMERA).performClick()
        assertEquals(0, clicks)
    }

    // Passes on the skeleton too (no BackHandler there either); kept as a guard (plan T-012, section 4).
    @Test
    fun firstPage_doesNotInterceptBack() {
        setScreen(OnboardingUiState.Browsing(PAGES_4, 0))
        composeRule.waitForIdle()
        assertFalse(composeRule.activity.onBackPressedDispatcher.hasEnabledCallbacks())
    }

    @Test
    fun secondPage_backCallsOnBack() {
        var backs = 0
        setScreen(OnboardingUiState.Browsing(PAGES_4, 1), onBack = { backs++ })
        composeRule.waitForIdle()
        assertTrue(composeRule.activity.onBackPressedDispatcher.hasEnabledCallbacks())
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        assertEquals(1, backs)
    }

    @Test
    fun requesting_doesNotInterceptBack() {
        val screen = setScreen(OnboardingUiState.Browsing(PAGES_4, 3))
        // Control: the last page while browsing does take Back.
        assertTrue(composeRule.activity.onBackPressedDispatcher.hasEnabledCallbacks())
        screen.show(OnboardingUiState.RequestingPermission(PAGES_4))
        assertFalse(composeRule.activity.onBackPressedDispatcher.hasEnabledCallbacks())
    }

    // ---- Short frame in every release language (L-028): the button stays on screen, every page scrolls ----

    @Test
    @Config(qualifiers = SHORT_SCREEN)
    fun small_everyPage_buttonVisible_contentScrolls_en() = assertShortScreen()

    @Test
    @Config(qualifiers = "vi-$SHORT_SCREEN")
    fun small_everyPage_buttonVisible_contentScrolls_vi() = assertShortScreen(illustrationShrinks = true)

    @Test
    @Config(qualifiers = "hi-$SHORT_SCREEN")
    fun small_everyPage_buttonVisible_contentScrolls_hi() = assertShortScreen()

    @Test
    @Config(qualifiers = "pt-rBR-$SHORT_SCREEN")
    fun small_everyPage_buttonVisible_contentScrolls_ptBR() = assertShortScreen()

    @Test
    @Config(qualifiers = "b+es+419-$SHORT_SCREEN")
    fun small_everyPage_buttonVisible_contentScrolls_es419() = assertShortScreen()

    @Test
    @Config(qualifiers = "in-$SHORT_SCREEN")
    fun small_everyPage_buttonVisible_contentScrolls_in() = assertShortScreen()

    @Test
    @Config(qualifiers = "th-$SHORT_SCREEN")
    fun small_everyPage_buttonVisible_contentScrolls_th() = assertShortScreen()

    /**
     * [illustrationShrinks]: the design measures about 288 dp at 360 x 640 in Vietnamese (README, section 6); other
     * languages may keep 360 when their two texts are short.
     */
    private fun assertShortScreen(illustrationShrinks: Boolean = false) {
        var clicks = 0
        val screen = setScreen(onEnableCamera = { clicks++ })
        PAGES_4.indices.forEach { index ->
            val state = OnboardingUiState.Browsing(PAGES_4, index)
            screen.show(state)
            val tag = primaryButtonTag(state)
            // Before any scrolling: the pinned button is inside the screen, below the scroll area.
            val button = bounds(tag)
            val content = composeRule.onNodeWithTag(OnboardingTestTags.CONTENT).getBoundsInRoot()
            assertTrue("page ${index + 1}: button bottom ${button.bottom}", button.bottom <= SHORT_SCREEN_HEIGHT)
            assertTrue("page ${index + 1}: button over the scroll area", button.top >= content.bottom)
            composeRule.onNodeWithTag(tag).assertHeightIsAtLeast(48.dp)
            assertIllustrationRule(illustrationShrinks)
            if (state.isLastPage) {
                val note = bounds(OnboardingTestTags.NOTE)
                assertTrue("note bottom ${note.bottom} is below 640", note.bottom <= SHORT_SCREEN_HEIGHT)
                assertTrue("note top ${note.top} overlaps scroll area ${content.bottom}", note.top >= content.bottom)
            }
            // The last element of the page is reached by scrolling.
            composeRule.onNodeWithTag(LAST_ELEMENT.getValue(state.page)).performScrollTo().assertIsDisplayed()
        }
        composeRule.onNodeWithTag(OnboardingTestTags.ENABLE_CAMERA).performClick()
        assertEquals(1, clicks)
    }

    /** The illustration of every page takes what the texts leave of the page area (ShrinkFirstLayout). */
    private fun assertIllustrationRule(shrinks: Boolean) {
        val height = bounds(OnboardingTestTags.ILLUSTRATION).height
        val viewport = bounds(OnboardingTestTags.PAGE_AREA).height - 48.dp
        val texts = bounds(OnboardingTestTags.BODY).bottom - bounds(OnboardingTestTags.TITLE).top
        val expected = (viewport - texts - 24.dp).coerceIn(160.dp, 360.dp)
        assertEquals("illustration height", expected.value, height.value, 1f)
        assertTrue("illustration height $height", height >= 160.dp && height <= 360.dp)
        if (shrinks) assertTrue("illustration height $height shrinks", height < 360.dp)
    }

    // A large screen in landscape follows the device (spec camera, KB-11; plan T-056): button and note stay inside the
    // window, the content scrolls. The second frame is the first minus the system bars.
    @Test
    @Config(qualifiers = "vi-w960dp-h600dp-land-xxhdpi")
    fun landscape_buttonAndNoteVisible_w960h600() = assertLandscape()

    @Test
    @Config(qualifiers = "vi-w960dp-h520dp-land-xxhdpi")
    fun landscape_buttonAndNoteVisible_w960h520() = assertLandscape()

    private fun assertLandscape() {
        var clicks = 0
        val screen = setScreen(onEnableCamera = { clicks++ })
        val root = composeRule.onRoot().getUnclippedBoundsInRoot()
        PAGES_4.indices.forEach { index ->
            val state = OnboardingUiState.Browsing(PAGES_4, index)
            screen.show(state)
            val button = bounds(primaryButtonTag(state))
            val content = composeRule.onNodeWithTag(OnboardingTestTags.CONTENT).getBoundsInRoot()
            assertTrue("page ${index + 1}: button bottom inside the window", button.bottom <= root.bottom)
            assertTrue("page ${index + 1}: button over the scroll area", button.top >= content.bottom)
            if (state.isLastPage) {
                val note = bounds(OnboardingTestTags.NOTE)
                assertTrue("note bottom ${note.bottom} inside the window ${root.bottom}", note.bottom <= root.bottom)
                assertTrue("note top ${note.top} overlaps scroll area ${content.bottom}", note.top >= content.bottom)
            }
            composeRule.onNodeWithTag(LAST_ELEMENT.getValue(state.page)).performScrollTo().assertIsDisplayed()
        }
        composeRule.onNodeWithTag(OnboardingTestTags.ENABLE_CAMERA).performClick()
        assertEquals(1, clicks)
    }

    private fun primaryButtonTag(state: OnboardingUiState) =
        if (state.isLastPage) OnboardingTestTags.ENABLE_CAMERA else OnboardingTestTags.NEXT

    private fun bounds(tag: String) = composeRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()

    // captureToImage times out on Robolectric; draw the root view into a bitmap instead (L-006).
    private fun pixelAt(tag: String, x: Dp, y: Dp): Color {
        composeRule.waitForIdle()
        val root = composeRule.activity.window.decorView
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        val bounds = composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInWindow
        with(composeRule.density) {
            return Color(bitmap.getPixel(bounds.left.toInt() + x.roundToPx(), bounds.top.toInt() + y.roundToPx()))
        }
    }

    /** The window drawn into a bitmap, and the bounds of [tag] in it (L-006). */
    private fun drawWindow(tag: String): Pair<Bitmap, android.graphics.Rect> {
        composeRule.waitForIdle()
        val root = composeRule.activity.window.decorView
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        val bounds = composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInWindow
        val rect = android.graphics.Rect(
            bounds.left.toInt(),
            bounds.top.toInt(),
            bounds.right.toInt().coerceAtMost(bitmap.width),
            bounds.bottom.toInt().coerceAtMost(bitmap.height),
        )
        return bitmap to rect
    }

    /** Orange points of the art (the clothes of every illustration) between [from] and [to] of [tag], every 4 px. */
    private fun artPixels(tag: String, from: Dp, to: Dp): Int {
        val (bitmap, rect) = drawWindow(tag)
        val left = rect.left + with(composeRule.density) { from.roundToPx() }
        val right = minOf(rect.right, rect.left + with(composeRule.density) { to.roundToPx() })
        var count = 0
        for (y in rect.top until rect.bottom step SAMPLE_STEP) {
            for (x in left until right step SAMPLE_STEP) {
                if (isArtOrange(bitmap.getPixel(x, y))) count++
            }
        }
        return count
    }

    /** Where the orange points of [tag] are, on a coarse grid: two different arts give two different masks. */
    private fun artMask(tag: String): List<Boolean> {
        val (bitmap, rect) = drawWindow(tag)
        val mask = mutableListOf<Boolean>()
        for (y in rect.top until rect.bottom step MASK_STEP) {
            for (x in rect.left until rect.right step MASK_STEP) mask += isArtOrange(bitmap.getPixel(x, y))
        }
        return mask
    }

    private fun isArtOrange(argb: Int): Boolean {
        val color = Color(argb)
        val red = color.red * 255
        val green = color.green * 255
        val blue = color.blue * 255
        return red >= 200 && green in 80f..175f && blue <= 80
    }

    /** Darkest and brightest points inside [tag], by the sum of the channels. */
    private fun darkestAndBrightest(tag: String): Pair<Color, Color> {
        val (bitmap, rect) = drawWindow(tag)
        var darkest = Color.White
        var brightest = Color.Black
        fun sum(c: Color) = c.red + c.green + c.blue
        for (y in rect.top until rect.bottom) {
            for (x in rect.left until rect.right) {
                val color = Color(bitmap.getPixel(x, y))
                if (sum(color) < sum(darkest)) darkest = color
                if (sum(color) > sum(brightest)) brightest = color
            }
        }
        return darkest to brightest
    }

    private fun assertColor(what: String, expected: Long, actual: Color) {
        assertEquals(what, Color(expected), actual)
    }

    private fun assertDp(what: String, expected: Float, actual: Dp) {
        assertEquals(what, expected, actual.value, DESIGN_TOLERANCE_DP)
    }

    private companion object {
        val SHORT_SCREEN_HEIGHT = 640.dp

        // One step of the 4 dp grid (design README, sections 6 and 7).
        const val DESIGN_TOLERANCE_DP = 4f

        val PAGES_4 = listOf(INTRO, POSES, GUIDE, CAMERA)

        // The screen takes any page list: the dots follow its size.
        val THREE_PAGES = listOf(INTRO, POSES, CAMERA)

        // Width to height of the art of page 3 (951 x 1080).
        const val GUIDE_ART_RATIO = 951f / 1080f

        // Grid of the pixel counts, in px.
        const val SAMPLE_STEP = 4
        const val MASK_STEP = 16

        // Fewer orange points than this means no art drawn (each art has thousands on the 4 px grid).
        const val MIN_ART_PIXELS = 500

        val FAKE_AD = NativeAdAssets(
            headline = "Headline of the ad",
            advertiser = "Advertiser",
            body = "Body text of the ad. Up to three lines; the ad decides the words, the app decides the style.",
            callToAction = "Install",
            icon = null,
            hasMedia = true,
        )

        val NO_AD: @Composable (OnboardingPage) -> Unit = {}
        val AD_BLOCK: @Composable (OnboardingPage) -> Unit = {
            Box(Modifier.fillMaxWidth().height(250.dp))
        }

        const val NEXT = "Tiếp"
        const val BUBBLE_TEXT = "Đứng vào đây!"

        // Text table of the design (README, section 2), by page. The description of the dots is not drawn as text:
        // pageIndicator_describesCurrentPage checks it.
        val PAGE_TEXTS = mapOf(
            INTRO to listOf(
                "Dáng đẹp, không cần đoán",
                "Chưa biết tạo dáng thế nào? App gợi ý dáng và chỉ bạn đứng ra sao.",
                NEXT,
            ),
            POSES to listOf(
                "Hơn 200 tư thế có sẵn",
                "Một người, cặp đôi hay nhóm bạn đều có. Lưu dáng yêu thích để chụp lại sau.",
                NEXT,
            ),
            GUIDE to listOf(
                "Khớp nét viền, bấm chụp",
                "Chỉnh độ mờ, cỡ và vị trí nét viền cho vừa khung hình. Ảnh lưu ra không kèm nét viền.",
                BUBBLE_TEXT,
                NEXT,
            ),
            CAMERA to listOf(
                "Sẵn sàng chụp rồi!",
                "Chọn một dáng, đứng vào nét viền và chụp tấm ảnh đầu tiên của bạn.",
                "App cần quyền camera để hiện khung xem trước. Ảnh chỉ lưu trên máy của bạn.",
                "Bật camera",
            ),
        )

        // The twelve texts drawn on screen (README, section 2).
        val DESIGN_TEXTS = listOf(
            "Dáng đẹp, không cần đoán",
            "Chưa biết tạo dáng thế nào? App gợi ý dáng và chỉ bạn đứng ra sao.",
            "Hơn 200 tư thế có sẵn",
            "Một người, cặp đôi hay nhóm bạn đều có. Lưu dáng yêu thích để chụp lại sau.",
            "Khớp nét viền, bấm chụp",
            "Chỉnh độ mờ, cỡ và vị trí nét viền cho vừa khung hình. Ảnh lưu ra không kèm nét viền.",
            BUBBLE_TEXT,
            "Sẵn sàng chụp rồi!",
            "Chọn một dáng, đứng vào nét viền và chụp tấm ảnh đầu tiên của bạn.",
            "App cần quyền camera để hiện khung xem trước. Ảnh chỉ lưu trên máy của bạn.",
            "Bật camera",
            NEXT,
        )

        // The last element of each page, reached by scrolling on a short screen.
        val LAST_ELEMENT = mapOf(
            INTRO to OnboardingTestTags.BODY,
            POSES to OnboardingTestTags.BODY,
            GUIDE to OnboardingTestTags.BODY,
            CAMERA to OnboardingTestTags.BODY,
        )
    }
}

// Short phone frame (L-028), standard design frame and unfolded frame with the language of the design.
private const val SHORT_SCREEN = "w360dp-h640dp-xxhdpi"
private const val DESIGN_FRAME = "w411dp-h891dp-xxhdpi"
private const val DESIGN_FRAME_VI = "vi-$DESIGN_FRAME"
private const val UNFOLDED_VI = "vi-w673dp-h841dp-xxhdpi"
