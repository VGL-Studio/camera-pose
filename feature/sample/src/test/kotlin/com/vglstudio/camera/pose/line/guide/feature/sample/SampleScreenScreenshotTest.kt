package com.vglstudio.camera.pose.line.guide.feature.sample

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
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class SampleScreenScreenshotTest {
    @Test
    fun ready_light() {
        captureRoboImage("src/test/screenshots/SampleScreen_ready_light.png") {
            AppTheme(darkTheme = false) { SampleScreen(uiState = SampleUiState.Ready(count = 3), onIncrement = {}) }
        }
    }

    @Test
    fun ready_dark() {
        captureRoboImage("src/test/screenshots/SampleScreen_ready_dark.png") {
            AppTheme(darkTheme = true) { SampleScreen(uiState = SampleUiState.Ready(count = 3), onIncrement = {}) }
        }
    }

    @Test
    fun disabled_light() {
        captureRoboImage("src/test/screenshots/SampleScreen_disabled_light.png") {
            AppTheme(darkTheme = false) { SampleScreen(uiState = SampleUiState.Disabled, onIncrement = {}) }
        }
    }
}
