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
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
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
import com.vglstudio.camera.pose.line.guide.feature.onboarding.OnboardingPage.INTRO
import com.vglstudio.camera.pose.line.guide.feature.onboarding.OnboardingPage.POSES
import com.vglstudio.camera.pose.line.guide.feature.onboarding.OnboardingPage.PREMIUM
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
 * section 4). Native graphics give real text sizes. Layout numbers come from design/context/ (411 x 891, Vietnamese
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
        val illustration = bounds(OnboardingTestTags.ILLUSTRATION)
        assertDp("illustration left", 24f, illustration.left)
        assertDp("illustration top", 88f, illustration.top)
        assertDp("illustration width", 363f, illustration.width)
        // Also the "full height when there is room" check (plan T-012, section 4).
        assertDp("illustration height", 280f, illustration.height)
        assertDp("title top", 392f, bounds(OnboardingTestTags.TITLE).top)
        assertDp("body top", 488f, bounds(OnboardingTestTags.BODY).top)
        val button = bounds(OnboardingTestTags.NEXT)
        assertDp("button width", 363f, button.width)
        assertTrue("button height ${button.height}", button.height >= 56.dp)
        assertDp("button top", 811f, button.top)
        assertDp("button bottom", 867f, button.bottom)
        assertDp("page area top", 64f, bounds(OnboardingTestTags.PAGE_AREA).top)
    }

    @Test
    fun poses_layoutMatchesDesign() {
        setScreen(OnboardingUiState.Browsing(PAGES_4, 1))
        val badge = bounds(OnboardingTestTags.BADGE)
        assertDp("badge left", 24f, badge.left)
        assertDp("badge top", 88f, badge.top)
        assertDp("badge width", 96f, badge.width)
        assertDp("badge height", 96f, badge.height)
        assertDp("title top", 208f, bounds(OnboardingTestTags.TITLE).top)
        val features = bounds(OnboardingTestTags.FEATURES)
        assertDp("features top", 268f, features.top)
        assertDp("features height", 96f, features.height)
        assertDp("poses point top", 268f, bounds(OnboardingTestTags.FEATURE_POSES).top)
        assertDp("saved point top", 324f, bounds(OnboardingTestTags.FEATURE_SAVED).top)
        val button = bounds(OnboardingTestTags.NEXT)
        assertDp("button top", 811f, button.top)
        assertDp("button bottom", 867f, button.bottom)
    }

    @Test
    fun premium_layoutMatchesDesign() {
        setScreen(OnboardingUiState.Browsing(PAGES_4, 2))
        val badge = bounds(OnboardingTestTags.BADGE)
        assertDp("badge top", 88f, badge.top)
        assertDp("badge width", 96f, badge.width)
        assertDp("badge height", 96f, badge.height)
        assertDp("title top", 208f, bounds(OnboardingTestTags.TITLE).top)
        val body = bounds(OnboardingTestTags.BODY)
        assertDp("body top", 268f, body.top)
        assertDp("body height", 48f, body.height)
        assertDp("button top", 811f, bounds(OnboardingTestTags.NEXT).top)
    }

    @Test
    fun camera_layoutMatchesDesign() {
        setScreen(OnboardingUiState.Browsing(PAGES_4, 3))
        assertDp("badge top", 88f, bounds(OnboardingTestTags.BADGE).top)
        assertDp("title top", 208f, bounds(OnboardingTestTags.TITLE).top)
        val features = bounds(OnboardingTestTags.FEATURES)
        assertDp("features top", 268f, features.top)
        assertDp("features height", 40f, features.height)
        val note = bounds(OnboardingTestTags.NOTE)
        assertDp("note bottom", 787f, note.bottom)
        assertDp("note width", 363f, note.width)
        val button = bounds(OnboardingTestTags.ENABLE_CAMERA)
        assertDp("button width", 363f, button.width)
        assertDp("button top", 811f, button.top)
        assertDp("button bottom", 867f, button.bottom)
        assertEquals("note to button", 24f, (button.top - note.bottom).value, 1f)
    }

    @Test
    @Config(qualifiers = UNFOLDED_VI)
    fun unfolded_layoutMatchesDesign() {
        val screen = setScreen(OnboardingUiState.Browsing(PAGES_4, 0))
        val illustration = bounds(OnboardingTestTags.ILLUSTRATION)
        assertDp("illustration left", 24f, illustration.left)
        assertDp("illustration top", 88f, illustration.top)
        assertDp("illustration width", 625f, illustration.width)
        assertDp("illustration height", 280f, illustration.height)
        assertDp("title top", 392f, bounds(OnboardingTestTags.TITLE).top)
        assertDp("body top", 452f, bounds(OnboardingTestTags.BODY).top)
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
    fun intro_illustrationColors_light() {
        setScreen(OnboardingUiState.Browsing(PAGES_4, 0), darkTheme = false)
        // Control: the first page is the one drawn.
        composeRule.onNodeWithTag(OnboardingTestTags.PAGE_INTRO).assertExists()
        val illustration = OnboardingTestTags.ILLUSTRATION
        assertColor("couple card", 0xFF3DDC97, pixelAt(illustration, 125.dp, 60.dp))
        assertColor("women card", 0xFFFF5C8A, pixelAt(illustration, 60.dp, 120.dp))
        assertColor("friends card", 0xFF4D8DFF, pixelAt(illustration, 310.dp, 150.dp))
        assertColor("badge", 0xFFFFC83D, pixelAt(illustration, 264.dp, 20.dp))
        assertColor("background", 0xFFFFFDF8, pixelAt(OnboardingTestTags.SCREEN, 2.dp, 2.dp))
    }

    @Test
    fun poses_iconCircleColor_light() {
        setScreen(OnboardingUiState.Browsing(PAGES_4, 1), darkTheme = false)
        // Control: the second page is the one drawn.
        composeRule.onNodeWithTag(OnboardingTestTags.PAGE_POSES).assertExists()
        assertColor("first icon circle", 0xFFFFC83D, pixelAt(OnboardingTestTags.FEATURE_POSES, 4.dp, 20.dp))
    }

    @Test
    fun premium_badgeColor_light() {
        setScreen(OnboardingUiState.Browsing(PAGES_4, 2), darkTheme = false)
        // Inside the circle, outside the 48 dp icon in the middle.
        assertColor("badge", 0xFFFFC83D, pixelAt(OnboardingTestTags.BADGE, 12.dp, 48.dp))
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
    fun threePages_noPremiumText_anywhere() {
        val screen = setScreen(OnboardingUiState.Browsing(PAGES_3, 0))
        PAGES_3.indices.forEach { index ->
            screen.show(OnboardingUiState.Browsing(PAGES_3, index))
            composeRule.onNodeWithText(PREMIUM_TITLE).assertDoesNotExist()
            composeRule.onNodeWithText(PREMIUM_BODY).assertDoesNotExist()
            composeRule.onNodeWithTag(OnboardingTestTags.pageDot(3)).assertExists()
            composeRule.onNodeWithTag(OnboardingTestTags.pageDot(4)).assertDoesNotExist()
        }
        // Control: the premium texts are there on the third of four pages.
        screen.show(OnboardingUiState.Browsing(PAGES_4, 2))
        composeRule.onNodeWithText(PREMIUM_TITLE).assertExists()
        composeRule.onNodeWithText(PREMIUM_BODY).assertExists()
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
        screen.show(OnboardingUiState.Browsing(PAGES_3, 0))
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
        screen.show(OnboardingUiState.Browsing(PAGES_3, 0))
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
        val illustration = bounds(OnboardingTestTags.ILLUSTRATION)
        assertDp("illustration top", 88f, illustration.top)
        assertDp("illustration width", 363f, illustration.width)
        assertDp("illustration height", 233f, illustration.height)
        // Control: the card takes its room from the illustration (280 without the card).
        assertTrue("illustration height ${illustration.height}", illustration.height < 280.dp)
        assertDp("title top", 345f, bounds(OnboardingTestTags.TITLE).top)
        assertDp("body top", 441f, bounds(OnboardingTestTags.BODY).top)
        assertCard(top = 537f)
        val next = bounds(OnboardingTestTags.NEXT)
        assertDp("next top", 811f, next.top)
        assertDp("next bottom", 867f, next.bottom)
    }

    @Test
    fun posesAd_layoutMatchesDesign() {
        setScreen(OnboardingUiState.Browsing(PAGES_4, 1), adSlot = cardSlot)
        assertDp("badge top", 88f, bounds(OnboardingTestTags.BADGE).top)
        assertDp("title top", 208f, bounds(OnboardingTestTags.TITLE).top)
        assertDp("features top", 268f, bounds(OnboardingTestTags.FEATURES).top)
        assertCard(top = 537f)
        assertDp("next top", 811f, bounds(OnboardingTestTags.NEXT).top)
    }

    @Test
    fun premiumAd_layoutMatchesDesign() {
        setScreen(OnboardingUiState.Browsing(PAGES_4, 2), adSlot = cardSlot)
        assertDp("badge top", 88f, bounds(OnboardingTestTags.BADGE).top)
        assertDp("title top", 208f, bounds(OnboardingTestTags.TITLE).top)
        assertDp("body top", 268f, bounds(OnboardingTestTags.BODY).top)
        assertCard(top = 537f)
        assertDp("next top", 811f, bounds(OnboardingTestTags.NEXT).top)
    }

    @Test
    fun cameraAd_layoutMatchesDesign() {
        setScreen(OnboardingUiState.Browsing(PAGES_4, 3), adSlot = cardSlot)
        assertDp("page area height", 417f, bounds(OnboardingTestTags.PAGE_AREA).height)
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
        composeRule.onNodeWithTag(OnboardingTestTags.FEATURE_ADJUST).performScrollTo()
        val adjust = bounds(OnboardingTestTags.FEATURE_ADJUST)
        assertTrue("adjust top ${adjust.top} in the page area", adjust.top >= area.top)
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
        setScreen(OnboardingUiState.Browsing(PAGES_3, 2), onEnableCamera = { clicks++ })
        // Control: the button is on the last page.
        composeRule.onNodeWithTag(OnboardingTestTags.PAGE_CAMERA).assertExists()
        composeRule.onNodeWithTag(OnboardingTestTags.ENABLE_CAMERA).performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun next_click_callsOnNextOnce() {
        var clicks = 0
        setScreen(OnboardingUiState.Browsing(PAGES_3, 0), onNext = { clicks++ })
        composeRule.onNodeWithTag(OnboardingTestTags.NEXT).performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun requesting_showsSpinner_andIgnoresClicks() {
        var clicks = 0
        setScreen(OnboardingUiState.RequestingPermission(PAGES_3), onEnableCamera = { clicks++ })
        composeRule.onNodeWithTag(PrimaryButtonTestTags.SPINNER, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(OnboardingTestTags.ENABLE_CAMERA).performClick()
        assertEquals(0, clicks)
    }

    // Passes on the skeleton too (no BackHandler there either); kept as a guard (plan T-012, section 4).
    @Test
    fun firstPage_doesNotInterceptBack() {
        setScreen(OnboardingUiState.Browsing(PAGES_3, 0))
        composeRule.waitForIdle()
        assertFalse(composeRule.activity.onBackPressedDispatcher.hasEnabledCallbacks())
    }

    @Test
    fun secondPage_backCallsOnBack() {
        var backs = 0
        setScreen(OnboardingUiState.Browsing(PAGES_3, 1), onBack = { backs++ })
        composeRule.waitForIdle()
        assertTrue(composeRule.activity.onBackPressedDispatcher.hasEnabledCallbacks())
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        assertEquals(1, backs)
    }

    @Test
    fun requesting_doesNotInterceptBack() {
        val screen = setScreen(OnboardingUiState.Browsing(PAGES_3, 2))
        // Control: the last page while browsing does take Back.
        assertTrue(composeRule.activity.onBackPressedDispatcher.hasEnabledCallbacks())
        screen.show(OnboardingUiState.RequestingPermission(PAGES_3))
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
     * [illustrationShrinks]: the design measures about 228 dp at 360 x 640 in Vietnamese (README, section 6); other
     * languages may keep 280 when their two texts are short.
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
            if (state.page == INTRO) assertIllustrationRule(illustrationShrinks)
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

    /** The illustration of page 1 takes what the texts leave of the page area (ShrinkFirstLayout). */
    private fun assertIllustrationRule(shrinks: Boolean) {
        val height = bounds(OnboardingTestTags.ILLUSTRATION).height
        val viewport = bounds(OnboardingTestTags.PAGE_AREA).height - 48.dp
        val texts = bounds(OnboardingTestTags.BODY).bottom - bounds(OnboardingTestTags.TITLE).top
        val expected = (viewport - texts - 24.dp).coerceIn(160.dp, 280.dp)
        assertEquals("illustration height", expected.value, height.value, 1f)
        assertTrue("illustration height $height", height >= 160.dp && height <= 280.dp)
        if (shrinks) assertTrue("illustration height $height shrinks", height < 280.dp)
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

        val PAGES_3 = listOf(INTRO, POSES, CAMERA)
        val PAGES_4 = listOf(INTRO, POSES, PREMIUM, CAMERA)

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
        const val PREMIUM_TITLE = "Tư thế premium"
        const val PREMIUM_BODY = "Xem một quảng cáo để mở một tư thế premium, rồi dùng mãi về sau."

        // Text table of the design (README, section 2), by page. The description of the dots is not drawn as text:
        // pageIndicator_describesCurrentPage checks it.
        val PAGE_TEXTS = mapOf(
            INTRO to listOf(
                "Chụp dáng đẹp, không cần đoán",
                "Chọn một tư thế, đứng vào nét viền rồi bấm chụp. Ảnh lưu vào thư viện máy, không kèm nét viền.",
                NEXT,
            ),
            POSES to listOf(
                "Tư thế cho mọi kiểu ảnh",
                "Hơn 200 tư thế: một người, cặp đôi, nhóm bạn",
                "Lưu tư thế yêu thích để chụp lại sau",
                NEXT,
            ),
            PREMIUM to listOf(PREMIUM_TITLE, PREMIUM_BODY, NEXT),
            CAMERA to listOf(
                "Sẵn sàng chụp",
                "Chỉnh độ mờ, cỡ và vị trí nét viền",
                "App cần quyền camera để hiện khung xem trước. Ảnh chỉ lưu trên máy của bạn.",
                "Bật camera",
            ),
        )

        // The twelve texts drawn on screen (README, section 2).
        val DESIGN_TEXTS = listOf(
            "Chụp dáng đẹp, không cần đoán",
            "Chọn một tư thế, đứng vào nét viền rồi bấm chụp. Ảnh lưu vào thư viện máy, không kèm nét viền.",
            "Tư thế cho mọi kiểu ảnh",
            "Hơn 200 tư thế: một người, cặp đôi, nhóm bạn",
            "Lưu tư thế yêu thích để chụp lại sau",
            PREMIUM_TITLE,
            PREMIUM_BODY,
            "Sẵn sàng chụp",
            "Chỉnh độ mờ, cỡ và vị trí nét viền",
            "App cần quyền camera để hiện khung xem trước. Ảnh chỉ lưu trên máy của bạn.",
            "Bật camera",
            NEXT,
        )

        // The last element of each page, reached by scrolling on a short screen.
        val LAST_ELEMENT = mapOf(
            INTRO to OnboardingTestTags.BODY,
            POSES to OnboardingTestTags.FEATURE_SAVED,
            PREMIUM to OnboardingTestTags.BODY,
            CAMERA to OnboardingTestTags.FEATURE_ADJUST,
        )
    }
}

// Short phone frame (L-028), standard design frame and unfolded frame with the language of the design.
private const val SHORT_SCREEN = "w360dp-h640dp-xxhdpi"
private const val DESIGN_FRAME_VI = "vi-w411dp-h891dp-xxhdpi"
private const val UNFOLDED_VI = "vi-w673dp-h841dp-xxhdpi"
