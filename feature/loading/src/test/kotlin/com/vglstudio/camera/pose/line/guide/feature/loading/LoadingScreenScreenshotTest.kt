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
 * The tests in the standard frame have the names of the design images in docs/features/loading/design/screens/,
 * with the language (Vietnamese), app name and progress of the design, so scripts/design-compare.sh compares them.
 * The _small tests have no design image and are not compared (ADR-0020).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "vi-w411dp-h891dp-xxhdpi")
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

    // The server guidance (plan T-057) has no design frame of its own: same layout as failed, other guidance line.
    @Test
    fun failedServer_light() {
        captureRoboImage("src/test/screenshots/LoadingScreen_failed-server_light.png") {
            AppTheme(darkTheme = false) {
                LoadingScreen(
                    uiState = LoadingUiState.Failed(PROGRESS, LoadingFailure.SERVER),
                    appName = APP_NAME,
                    onRetry = {},
                )
            }
        }
    }

    @Test
    @Config(qualifiers = SHORT_SCREEN)
    fun failedServer_small() {
        captureRoboImage("src/test/screenshots/LoadingScreen_failed-server_small.png") {
            AppTheme(darkTheme = false) {
                LoadingScreen(
                    uiState = LoadingUiState.Failed(PROGRESS, LoadingFailure.SERVER),
                    appName = APP_NAME,
                    onRetry = {},
                )
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
        const val APP_NAME = "Camera Pose: Lines and Guides"
        const val PROGRESS = 0.68f
        const val SHORT_SCREEN = "vi-w360dp-h640dp-xxhdpi"
    }
}
