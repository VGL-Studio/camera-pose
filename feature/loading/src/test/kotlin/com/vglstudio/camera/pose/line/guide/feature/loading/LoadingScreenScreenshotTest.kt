package com.vglstudio.camera.pose.line.guide.feature.loading

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot tests (Roborazzi). Reference images live in src/test/screenshots and are compared on every CI run.
 * A changed reference image must be listed in the plan and reviewed by a person.
 * Each test has the name of a design image in docs/features/loading/design/screens/, with the same sample
 * app name and progress, so scripts/design-compare.sh can compare them (ADR-0020).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class LoadingScreenScreenshotTest {
    @Test
    fun progress_light() {
        captureRoboImage("src/test/screenshots/LoadingScreen_progress_light.png") {
            AppTheme(darkTheme = false) {
                LoadingScreen(uiState = LoadingUiState.InProgress(PROGRESS), appName = APP_NAME, onRetry = {})
            }
        }
    }

    @Test
    fun progress_dark() {
        captureRoboImage("src/test/screenshots/LoadingScreen_progress_dark.png") {
            AppTheme(darkTheme = true) {
                LoadingScreen(uiState = LoadingUiState.InProgress(PROGRESS), appName = APP_NAME, onRetry = {})
            }
        }
    }

    @Test
    fun failed_light() {
        captureRoboImage("src/test/screenshots/LoadingScreen_failed_light.png") {
            AppTheme(darkTheme = false) {
                LoadingScreen(uiState = LoadingUiState.Failed(PROGRESS), appName = APP_NAME, onRetry = {})
            }
        }
    }

    @Test
    fun failed_dark() {
        captureRoboImage("src/test/screenshots/LoadingScreen_failed_dark.png") {
            AppTheme(darkTheme = true) {
                LoadingScreen(uiState = LoadingUiState.Failed(PROGRESS), appName = APP_NAME, onRetry = {})
            }
        }
    }

    // Short phone frame (L-028): the content must scroll and the retry button must not be cut or covered.
    @Test
    @Config(qualifiers = SHORT_SCREEN)
    fun failed_small() {
        captureRoboImage("src/test/screenshots/LoadingScreen_failed_small.png") {
            AppTheme(darkTheme = false) {
                LoadingScreen(uiState = LoadingUiState.Failed(PROGRESS), appName = APP_NAME, onRetry = {})
            }
        }
    }

    @Test
    @Config(qualifiers = SHORT_SCREEN)
    fun progress_small() {
        captureRoboImage("src/test/screenshots/LoadingScreen_progress_small.png") {
            AppTheme(darkTheme = false) {
                LoadingScreen(uiState = LoadingUiState.InProgress(PROGRESS), appName = APP_NAME, onRetry = {})
            }
        }
    }

    private companion object {
        const val APP_NAME = "Todo App"
        const val PROGRESS = 0.68f
        const val SHORT_SCREEN = "w360dp-h640dp-xxhdpi"
    }
}
