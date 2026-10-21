package com.vglstudio.camera.pose.line.guide.feature.loading

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.designsystem.R as DesignSystemR
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
        composeRule.onNodeWithText(string(DesignSystemR.string.brand_tagline)).assertIsDisplayed()
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

    private companion object {
        const val APP_NAME = "Todo App"

        // Short phone frame (L-028, FB-20261017-01).
        const val SHORT_SCREEN = "w360dp-h640dp-xxhdpi"
    }
}
