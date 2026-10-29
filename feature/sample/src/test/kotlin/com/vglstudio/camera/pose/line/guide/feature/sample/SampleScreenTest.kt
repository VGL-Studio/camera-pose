package com.vglstudio.camera.pose.line.guide.feature.sample

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** UI behaviour of the stateless screen, run on the JVM with Robolectric (no emulator needed). */
@RunWith(AndroidJUnit4::class)
class SampleScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun string(id: Int): String =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(id)

    @Test
    fun whenReady_thenShowsCount() {
        composeRule.setContent {
            AppTheme { SampleScreen(uiState = SampleUiState.Ready(count = 7), onIncrement = {}) }
        }
        composeRule.onNodeWithTag(SampleTestTags.COUNT).assertTextEquals("7")
    }

    @Test
    fun whenTapButton_thenCallsOnIncrement() {
        var taps = 0
        composeRule.setContent {
            AppTheme { SampleScreen(uiState = SampleUiState.Ready(count = 0), onIncrement = { taps++ }) }
        }
        composeRule.onNodeWithText(string(R.string.sample_increment)).performClick()
        assertEquals(1, taps)
    }

    @Test
    fun whenSaveFailed_thenShowsErrorMessage() {
        composeRule.setContent {
            AppTheme { SampleScreen(uiState = SampleUiState.Ready(count = 2, saveFailed = true), onIncrement = {}) }
        }
        composeRule.onNodeWithTag(SampleTestTags.SAVE_ERROR).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.sample_save_failed)).assertIsDisplayed()
    }

    @Test
    fun whenSaveSucceeded_thenNoErrorMessage() {
        composeRule.setContent {
            AppTheme { SampleScreen(uiState = SampleUiState.Ready(count = 2), onIncrement = {}) }
        }
        composeRule.onNodeWithTag(SampleTestTags.SAVE_ERROR).assertDoesNotExist()
    }

    @Test
    fun whenDisabled_thenShowsNotAvailableMessage() {
        composeRule.setContent {
            AppTheme { SampleScreen(uiState = SampleUiState.Disabled, onIncrement = {}) }
        }
        composeRule.onNodeWithText(string(R.string.sample_disabled)).assertIsDisplayed()
    }

    @Test
    fun noSampleBottomBanner() {
        // Spec ads, question 4: the sample screen has no banner placement. Read by symbol from the source (L-062);
        // the working directory of a unit test is the module directory.
        val source = java.io.File("src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/sample/SampleScreen.kt")
        // Control: the file is the screen, so a missing symbol means something.
        assertTrue("Missing ${source.absolutePath}", source.readText().contains("fun SampleScreen("))
        assertFalse(source.readText().contains("AdPlacement.SAMPLE_BOTTOM_BANNER"))
    }
}
