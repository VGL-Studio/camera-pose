package com.vglstudio.camera.pose.line.guide.feature.loading

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * UI behaviour of the stateless screen, run on the JVM with Robolectric (no emulator needed).
 * Text follows the text table of the design (docs/features/loading/design/README.md, section 2).
 * Native graphics give real text sizes; the frame is the standard design frame.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class LoadingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun string(id: Int): String =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(id)

    @Test
    fun inProgress_showsIdentityProgressAndGuidance() {
        composeRule.setContent {
            AppTheme {
                LoadingScreen(uiState = LoadingUiState.InProgress(0.68f), appName = APP_NAME, onRetry = {})
            }
        }
        composeRule.onNodeWithTag(LoadingTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithTag(LoadingTestTags.APP_NAME).assertTextEquals(APP_NAME)
        composeRule.onNodeWithText(string(R.string.loading_tagline)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.loading_status)).assertIsDisplayed()
        composeRule.onNodeWithTag(LoadingTestTags.PERCENT).assertTextEquals("68%")
        composeRule.onNodeWithTag(LoadingTestTags.PROGRESS).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.loading_guidance)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.loading_footer)).assertIsDisplayed()
        composeRule.onNodeWithTag(LoadingTestTags.ERROR).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.loading_status_failed)).assertDoesNotExist()
    }

    @Test
    fun failed_showsErrorTitleGuidanceAndRetry_andRetryCallsOnRetry() {
        var retries = 0
        composeRule.setContent {
            AppTheme {
                LoadingScreen(uiState = LoadingUiState.Failed(0.68f), appName = APP_NAME, onRetry = { retries++ })
            }
        }
        composeRule.onNodeWithText(string(R.string.loading_status_failed)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.loading_error_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.loading_error)).assertIsDisplayed()
        composeRule.onNodeWithTag(LoadingTestTags.PERCENT).assertTextEquals("68%")
        composeRule.onNodeWithText(string(R.string.loading_guidance)).assertDoesNotExist()
        composeRule.onNodeWithTag(LoadingTestTags.RETRY).performClick()
        assertEquals(1, retries)
    }

    @Test
    fun appName_staysInPlace_whenFailed() {
        var uiState: LoadingUiState by mutableStateOf(LoadingUiState.InProgress(0.68f))
        composeRule.setContent {
            AppTheme { LoadingScreen(uiState = uiState, appName = APP_NAME, onRetry = {}) }
        }
        val topInProgress = composeRule.onNodeWithTag(LoadingTestTags.APP_NAME).getBoundsInRoot().top

        uiState = LoadingUiState.Failed(0.68f)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(LoadingTestTags.ERROR).assertIsDisplayed()
        val topFailed = composeRule.onNodeWithTag(LoadingTestTags.APP_NAME).getBoundsInRoot().top

        assertEquals(topInProgress, topFailed)
    }

    @Test
    fun retryButton_isAtLeast48dpHigh() {
        composeRule.setContent {
            AppTheme {
                LoadingScreen(uiState = LoadingUiState.Failed(0.68f), appName = APP_NAME, onRetry = {})
            }
        }
        composeRule.onNodeWithTag(LoadingTestTags.RETRY).assertHeightIsAtLeast(48.dp)
    }

    @Test
    @Config(qualifiers = SHORT_SCREEN)
    fun failed_onShortScreen_retryButtonFitsOnScreen() {
        var retries = 0
        composeRule.setContent {
            AppTheme {
                LoadingScreen(uiState = LoadingUiState.Failed(0.68f), appName = APP_NAME, onRetry = { retries++ })
            }
        }
        val screenBottom = composeRule.onNodeWithTag(LoadingTestTags.SCREEN).getBoundsInRoot().bottom
        val retryBottom = composeRule.onNodeWithTag(LoadingTestTags.RETRY).getBoundsInRoot().bottom
        assertTrue("retry bottom $retryBottom is below screen bottom $screenBottom", retryBottom <= screenBottom)
        // At 360 x 640 the button can end inside the screen yet sit under the pinned footer, so it must also
        // end above the footer to be seen.
        val footerTop = composeRule.onNodeWithText(string(R.string.loading_footer)).getBoundsInRoot().top
        assertTrue("retry bottom $retryBottom is under footer top $footerTop", retryBottom <= footerTop)

        composeRule.onNodeWithTag(LoadingTestTags.RETRY).performClick()
        assertEquals(1, retries)
    }

    @Test
    @Config(qualifiers = SHORT_SCREEN)
    fun failed_onShortScreen_footerBelowError() {
        composeRule.setContent {
            AppTheme {
                LoadingScreen(uiState = LoadingUiState.Failed(0.68f), appName = APP_NAME, onRetry = {})
            }
        }
        val errorBottom = composeRule.onNodeWithTag(LoadingTestTags.ERROR).getBoundsInRoot().bottom
        val footerTop = composeRule.onNodeWithText(string(R.string.loading_footer)).getBoundsInRoot().top
        assertTrue("footer top $footerTop overlaps error bottom $errorBottom", footerTop >= errorBottom)
    }

    @Test
    @Config(qualifiers = DESIGN_FRAME_VI)
    fun inProgress_layoutMatchesDesign() {
        composeRule.setContent {
            AppTheme {
                LoadingScreen(uiState = LoadingUiState.InProgress(0.68f), appName = APP_NAME, onRetry = {})
            }
        }
        val logo = composeRule.onNodeWithTag(LoadingTestTags.LOGO).getUnclippedBoundsInRoot()
        assertDp("logo top", 200f, logo.top)
        assertDp("logo height", 112f, logo.height)
        val appName = composeRule.onNodeWithTag(LoadingTestTags.APP_NAME).getUnclippedBoundsInRoot()
        assertDp("app name top", 336f, appName.top)
        assertDp("app name height (two lines)", 80f, appName.height)
        assertDp("app name left", 46.5f, appName.left)
        assertDp("app name width", 318f, appName.width)
        val guidance = composeRule.onNodeWithText(string(R.string.loading_guidance)).getUnclippedBoundsInRoot()
        assertDp("guidance top", 552f, guidance.top)
        val footer = composeRule.onNodeWithText(string(R.string.loading_footer)).getUnclippedBoundsInRoot()
        assertDp("footer bottom", 859f, footer.bottom)
    }

    @Test
    @Config(qualifiers = DESIGN_FRAME_VI)
    fun failed_layoutMatchesDesign() {
        composeRule.setContent {
            AppTheme {
                LoadingScreen(uiState = LoadingUiState.Failed(0.68f), appName = APP_NAME, onRetry = {})
            }
        }
        val error = composeRule.onNodeWithTag(LoadingTestTags.ERROR).getUnclippedBoundsInRoot()
        assertDp("error block top", 552f, error.top)
        val retry = composeRule.onNodeWithTag(LoadingTestTags.RETRY).getUnclippedBoundsInRoot()
        assertDp("retry top", 648f, retry.top)
        assertDp("retry height", 56f, retry.height)
        val footer = composeRule.onNodeWithText(string(R.string.loading_footer)).getUnclippedBoundsInRoot()
        assertDp("footer bottom", 859f, footer.bottom)
    }

    @Test
    fun background_isBackgroundRole_light() = assertBackgroundRole(darkTheme = false)

    @Test
    fun background_isBackgroundRole_dark() = assertBackgroundRole(darkTheme = true)

    private fun assertBackgroundRole(darkTheme: Boolean) {
        var background = Color.Unspecified
        composeRule.setContent {
            AppTheme(darkTheme = darkTheme) {
                background = MaterialTheme.colorScheme.background
                LoadingScreen(uiState = LoadingUiState.InProgress(0.68f), appName = APP_NAME, onRetry = {})
            }
        }
        // captureToImage times out on Robolectric; draw the root view into a bitmap instead (as ComponentsTest, T-007).
        composeRule.waitForIdle()
        val root = (composeRule as AndroidComposeTestRule<*, *>).activity.window.decorView
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        val bounds = composeRule.onNodeWithTag(LoadingTestTags.SCREEN).fetchSemanticsNode().boundsInWindow
        val offset = with(composeRule.density) { 4.dp.roundToPx() }
        val pixel = bitmap.getPixel(bounds.left.toInt() + offset, bounds.top.toInt() + offset)
        assertEquals(background.toArgb(), pixel)
    }

    // On a short screen, every release language must let the user scroll to "Try again" and press it, with the
    // footer below it (L-028). Long translations must not push the button under the footer or off the screen.
    @Test
    @Config(qualifiers = SHORT_SCREEN)
    fun failed_onShortScreen_retryReachable_en() = assertRetryReachableOnShortScreen()

    @Test
    @Config(qualifiers = "vi-$SHORT_SCREEN")
    fun failed_onShortScreen_retryReachable_vi() = assertRetryReachableOnShortScreen()

    @Test
    @Config(qualifiers = "hi-$SHORT_SCREEN")
    fun failed_onShortScreen_retryReachable_hi() = assertRetryReachableOnShortScreen()

    @Test
    @Config(qualifiers = "pt-rBR-$SHORT_SCREEN")
    fun failed_onShortScreen_retryReachable_ptBR() = assertRetryReachableOnShortScreen()

    @Test
    @Config(qualifiers = "b+es+419-$SHORT_SCREEN")
    fun failed_onShortScreen_retryReachable_es419() = assertRetryReachableOnShortScreen()

    @Test
    @Config(qualifiers = "in-$SHORT_SCREEN")
    fun failed_onShortScreen_retryReachable_in() = assertRetryReachableOnShortScreen()

    @Test
    @Config(qualifiers = "th-$SHORT_SCREEN")
    fun failed_onShortScreen_retryReachable_th() = assertRetryReachableOnShortScreen()

    private fun assertRetryReachableOnShortScreen() {
        var retries = 0
        composeRule.setContent {
            AppTheme {
                LoadingScreen(uiState = LoadingUiState.Failed(0.68f), appName = APP_NAME, onRetry = { retries++ })
            }
        }
        val retry = composeRule.onNodeWithTag(LoadingTestTags.RETRY)
        retry.performScrollTo().assertIsDisplayed()
        val screenBottom = composeRule.onNodeWithTag(LoadingTestTags.SCREEN).getBoundsInRoot().bottom
        assertTrue("retry is cut by the screen bottom", retry.getBoundsInRoot().bottom <= screenBottom)

        val footer = composeRule.onNodeWithText(string(R.string.loading_footer))
        footer.performScrollTo()
        val retryBottom = retry.getBoundsInRoot().bottom
        val footerTop = footer.getBoundsInRoot().top
        assertTrue("footer top $footerTop overlaps retry bottom $retryBottom", footerTop >= retryBottom)

        retry.performScrollTo().performClick()
        assertEquals(1, retries)
    }

    private fun assertDp(what: String, expected: Float, actual: Dp) {
        assertEquals(what, expected, actual.value, DESIGN_TOLERANCE_DP)
    }

    private companion object {
        const val APP_NAME = "Camera Pose: Lines and Guides"

        // Short phone frame (L-028, FB-20261017-01).
        const val SHORT_SCREEN = "w360dp-h640dp-xxhdpi"

        // Standard design frame with the language of the design.
        const val DESIGN_FRAME_VI = "vi-w411dp-h891dp-xxhdpi"

        // One step of the 4 dp grid (design README, section 7).
        const val DESIGN_TOLERANCE_DP = 4f
    }
}
