package com.vglstudio.camera.pose.line.guide.feature.onboarding

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
 * The tests in the standard frame have the names of the design images in docs/features/onboarding/design/screens/,
 * with the language of the design (Vietnamese), so scripts/design-compare.sh compares them.
 * The _small test has no design image and is not compared (ADR-0020).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "vi-w411dp-h891dp-xxhdpi")
class OnboardingScreenScreenshotTest {
    @Test
    fun default_light() {
        captureRoboImage("src/test/screenshots/OnboardingScreen_default_light.png") {
            AppTheme(darkTheme = false) {
                OnboardingScreen(uiState = OnboardingUiState.Ready, onEnableCamera = {})
            }
        }
    }

    @Test
    fun default_dark() {
        captureRoboImage("src/test/screenshots/OnboardingScreen_default_dark.png") {
            AppTheme(darkTheme = true) {
                OnboardingScreen(uiState = OnboardingUiState.Ready, onEnableCamera = {})
            }
        }
    }

    // Short phone frame (L-028): the illustration shrinks, the button and note stay at the bottom.
    @Test
    @Config(qualifiers = "vi-w360dp-h640dp-xxhdpi")
    fun default_small() {
        captureRoboImage("src/test/screenshots/OnboardingScreen_default_small.png") {
            AppTheme(darkTheme = false) {
                OnboardingScreen(uiState = OnboardingUiState.Ready, onEnableCamera = {})
            }
        }
    }
}
