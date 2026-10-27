package com.vglstudio.camera.pose.line.guide.feature.onboarding

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PrimaryButtonTestTags
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
 * UI behaviour of the stateless onboarding screen, run on the JVM with Robolectric (plan T-012, section 4).
 * Native graphics give real text sizes. Layout numbers come from design/context/ (411 x 891, Vietnamese text of the
 * design); the short frame 360 x 640 is checked in the seven release languages (L-028).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = DESIGN_FRAME_VI)
class OnboardingScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun setScreen(
        uiState: OnboardingUiState = OnboardingUiState.Ready,
        darkTheme: Boolean = false,
        onEnableCamera: () -> Unit = {},
    ) {
        composeRule.setContent {
            AppTheme(darkTheme = darkTheme) {
                OnboardingScreen(uiState = uiState, onEnableCamera = onEnableCamera)
            }
        }
    }

    @Test
    fun default_layoutMatchesDesign() {
        setScreen()
        val illustration = bounds(OnboardingTestTags.ILLUSTRATION)
        assertDp("illustration top", 56f, illustration.top)
        // Also the "full height when there is room" check (plan T-012, section 4).
        assertDp("illustration height", 280f, illustration.height)
        assertDp("illustration left", 24f, illustration.left)
        assertDp("illustration width", 363f, illustration.width)
        assertDp("title top", 360f, bounds(OnboardingTestTags.TITLE).top)
        assertDp("body top", 456f, bounds(OnboardingTestTags.BODY).top)
        val features = bounds(OnboardingTestTags.FEATURES)
        assertDp("features top", 552f, features.top)
        assertDp("features height", 152f, features.height)
        val button = bounds(OnboardingTestTags.ENABLE_CAMERA)
        assertDp("button width", 363f, button.width)
        assertTrue("button height ${button.height}", button.height >= 56.dp)
        assertDp("button top", 753f, button.top)
        assertDp("note bottom", 869f, bounds(OnboardingTestTags.NOTE).bottom)
    }

    @Test
    fun default_illustrationColors_light() {
        setScreen(darkTheme = false)
        val illustration = OnboardingTestTags.ILLUSTRATION
        assertColor("couple card", 0xFF3DDC97, pixelAt(illustration, 125.dp, 60.dp))
        assertColor("women card", 0xFFFF5C8A, pixelAt(illustration, 60.dp, 120.dp))
        assertColor("friends card", 0xFF4D8DFF, pixelAt(illustration, 310.dp, 150.dp))
        assertColor("badge", 0xFFFFC83D, pixelAt(illustration, 264.dp, 20.dp))
        assertColor("first icon circle", 0xFFFFC83D, pixelAt(OnboardingTestTags.FEATURE_POSES, 4.dp, 20.dp))
        assertColor("background", 0xFFFFFDF8, pixelAt(OnboardingTestTags.SCREEN, 2.dp, 2.dp))
    }

    @Test
    fun default_colors_dark() {
        setScreen(darkTheme = true)
        assertColor("background", 0xFF121212, pixelAt(OnboardingTestTags.SCREEN, 2.dp, 2.dp))
        assertColor("button", 0xFFFFFFFF, pixelAt(OnboardingTestTags.ENABLE_CAMERA, 8.dp, 30.dp))
    }

    @Test
    fun default_showsDesignTexts() {
        setScreen()
        DESIGN_TEXTS.forEach { text -> composeRule.onNodeWithText(text).assertExists() }
    }

    @Test
    fun enableCamera_click_callsCallbackOnce() {
        var clicks = 0
        setScreen(onEnableCamera = { clicks++ })
        composeRule.onNodeWithTag(OnboardingTestTags.ENABLE_CAMERA).performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun requesting_showsSpinner_andIgnoresClicks() {
        var clicks = 0
        setScreen(uiState = OnboardingUiState.RequestingPermission, onEnableCamera = { clicks++ })
        composeRule.onNodeWithTag(PrimaryButtonTestTags.SPINNER, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(OnboardingTestTags.ENABLE_CAMERA).performClick()
        assertEquals(0, clicks)
    }

    // Passes on the skeleton too (no BackHandler there either); kept as a guard (plan T-012, section 4).
    @Test
    fun screen_doesNotInterceptBack() {
        setScreen()
        composeRule.waitForIdle()
        assertFalse(composeRule.activity.onBackPressedDispatcher.hasEnabledCallbacks())
    }

    // Short frame in every release language (L-028): button and note stay on screen, the content scrolls.
    @Test
    @Config(qualifiers = SHORT_SCREEN)
    fun small_buttonAndNoteVisible_contentScrolls_en() = assertShortScreen()

    @Test
    @Config(qualifiers = "vi-$SHORT_SCREEN")
    fun small_buttonAndNoteVisible_contentScrolls_vi() = assertShortScreen()

    @Test
    @Config(qualifiers = "hi-$SHORT_SCREEN")
    fun small_buttonAndNoteVisible_contentScrolls_hi() = assertShortScreen()

    @Test
    @Config(qualifiers = "pt-rBR-$SHORT_SCREEN")
    fun small_buttonAndNoteVisible_contentScrolls_ptBR() = assertShortScreen()

    @Test
    @Config(qualifiers = "b+es+419-$SHORT_SCREEN")
    fun small_buttonAndNoteVisible_contentScrolls_es419() = assertShortScreen()

    @Test
    @Config(qualifiers = "in-$SHORT_SCREEN")
    fun small_buttonAndNoteVisible_contentScrolls_in() = assertShortScreen()

    @Test
    @Config(qualifiers = "th-$SHORT_SCREEN")
    fun small_buttonAndNoteVisible_contentScrolls_th() = assertShortScreen()

    private fun assertShortScreen() {
        var clicks = 0
        setScreen(onEnableCamera = { clicks++ })
        // Before any scrolling: the pinned button and note are inside the screen, below the scroll area.
        val button = bounds(OnboardingTestTags.ENABLE_CAMERA)
        val note = bounds(OnboardingTestTags.NOTE)
        val content = composeRule.onNodeWithTag(OnboardingTestTags.CONTENT).getBoundsInRoot()
        assertTrue("button bottom ${button.bottom} is below 640", button.bottom <= SHORT_SCREEN_HEIGHT)
        assertTrue("note bottom ${note.bottom} is below 640", note.bottom <= SHORT_SCREEN_HEIGHT)
        assertTrue("button top ${button.top} overlaps scroll area ${content.bottom}", button.top >= content.bottom)
        composeRule.onNodeWithTag(OnboardingTestTags.ENABLE_CAMERA).assertHeightIsAtLeast(48.dp)
        // The illustration shrinks first, down to its minimum.
        val illustration = bounds(OnboardingTestTags.ILLUSTRATION)
        assertTrue("illustration height ${illustration.height}", illustration.height >= 160.dp)
        assertTrue("illustration height ${illustration.height}", illustration.height < 280.dp)
        // The last point is reached by scrolling.
        composeRule.onNodeWithTag(OnboardingTestTags.FEATURE_SAVED).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(OnboardingTestTags.ENABLE_CAMERA).performClick()
        assertEquals(1, clicks)
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
        setScreen(onEnableCamera = { clicks++ })
        val root = composeRule.onRoot().getUnclippedBoundsInRoot()
        val button = bounds(OnboardingTestTags.ENABLE_CAMERA)
        val note = bounds(OnboardingTestTags.NOTE)
        val content = composeRule.onNodeWithTag(OnboardingTestTags.CONTENT).getBoundsInRoot()
        assertTrue("button bottom ${button.bottom} inside the window ${root.bottom}", button.bottom <= root.bottom)
        assertTrue("note bottom ${note.bottom} inside the window ${root.bottom}", note.bottom <= root.bottom)
        assertTrue("button top ${button.top} overlaps scroll area ${content.bottom}", button.top >= content.bottom)
        composeRule.onNodeWithTag(OnboardingTestTags.FEATURE_SAVED).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(OnboardingTestTags.ENABLE_CAMERA).performClick()
        assertEquals(1, clicks)
    }

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

        // Text table of the design (README, section 2).
        val DESIGN_TEXTS = listOf(
            "Chụp dáng đẹp, không cần đoán",
            "Chọn một tư thế, đứng vào nét viền rồi bấm chụp. Ảnh lưu vào thư viện máy, không kèm nét viền.",
            "Hơn 200 tư thế: một người, cặp đôi, nhóm bạn",
            "Chỉnh độ mờ, cỡ và vị trí nét viền",
            "Lưu tư thế yêu thích để chụp lại sau",
            "Bật camera",
            "App cần quyền camera để hiện khung xem trước. Ảnh chỉ lưu trên máy của bạn.",
        )
    }
}

// Short phone frame (L-028) and standard design frame with the language of the design.
private const val SHORT_SCREEN = "w360dp-h640dp-xxhdpi"
private const val DESIGN_FRAME_VI = "vi-w411dp-h891dp-xxhdpi"
