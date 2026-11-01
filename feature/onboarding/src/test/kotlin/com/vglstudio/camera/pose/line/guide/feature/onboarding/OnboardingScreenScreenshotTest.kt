package com.vglstudio.camera.pose.line.guide.feature.onboarding

import androidx.compose.runtime.Composable
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
 * The tests in the standard and unfolded frames have the names of the design images in
 * docs/features/onboarding/design/screens/, with the language of the design (Vietnamese), so
 * scripts/design-compare.sh compares them. Every frame of the design draws the flow of four pages (README, section 1).
 * The _small tests have no design image and are not compared (ADR-0020).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "vi-w411dp-h891dp-xxhdpi")
class OnboardingScreenScreenshotTest {
    @Test
    fun intro_light() {
        captureRoboImage("src/test/screenshots/OnboardingScreen_intro_light.png") {
            Page(index = 0, darkTheme = false)
        }
    }

    @Test
    fun intro_dark() {
        captureRoboImage("src/test/screenshots/OnboardingScreen_intro_dark.png") {
            Page(index = 0, darkTheme = true)
        }
    }

    @Test
    fun poses_light() {
        captureRoboImage("src/test/screenshots/OnboardingScreen_poses_light.png") {
            Page(index = 1, darkTheme = false)
        }
    }

    @Test
    fun poses_dark() {
        captureRoboImage("src/test/screenshots/OnboardingScreen_poses_dark.png") {
            Page(index = 1, darkTheme = true)
        }
    }

    @Test
    fun premium_light() {
        captureRoboImage("src/test/screenshots/OnboardingScreen_premium_light.png") {
            Page(index = 2, darkTheme = false)
        }
    }

    @Test
    fun premium_dark() {
        captureRoboImage("src/test/screenshots/OnboardingScreen_premium_dark.png") {
            Page(index = 2, darkTheme = true)
        }
    }

    @Test
    fun camera_light() {
        captureRoboImage("src/test/screenshots/OnboardingScreen_camera_light.png") {
            Page(index = 3, darkTheme = false)
        }
    }

    @Test
    fun camera_dark() {
        captureRoboImage("src/test/screenshots/OnboardingScreen_camera_dark.png") {
            Page(index = 3, darkTheme = true)
        }
    }

    // Short phone frame (L-028): the illustration shrinks, the button stays at the bottom.
    @Test
    @Config(qualifiers = "vi-w360dp-h640dp-xxhdpi")
    fun intro_small() {
        captureRoboImage("src/test/screenshots/OnboardingScreen_intro_small.png") {
            Page(index = 0, darkTheme = false)
        }
    }

    // Short phone frame (L-028), last page: the most pinned parts (note and button).
    @Test
    @Config(qualifiers = "vi-w360dp-h640dp-xxhdpi")
    fun camera_small() {
        captureRoboImage("src/test/screenshots/OnboardingScreen_camera_small.png") {
            Page(index = 3, darkTheme = false)
        }
    }

    // Unfolded frame (ADR-0020).
    @Test
    @Config(qualifiers = "vi-w673dp-h841dp-xxhdpi")
    fun intro_light_unfolded() {
        captureRoboImage("src/test/screenshots/OnboardingScreen_intro_light_unfolded.png") {
            Page(index = 0, darkTheme = false)
        }
    }

    // Unfolded frame (ADR-0020).
    @Test
    @Config(qualifiers = "vi-w673dp-h841dp-xxhdpi")
    fun camera_light_unfolded() {
        captureRoboImage("src/test/screenshots/OnboardingScreen_camera_light_unfolded.png") {
            Page(index = 3, darkTheme = false)
        }
    }

    /** Page [index] of the four-page flow, browsing (no spinner on the button), empty ad slot. */
    @Composable
    private fun Page(index: Int, darkTheme: Boolean) {
        AppTheme(darkTheme = darkTheme) {
            OnboardingScreen(
                uiState = OnboardingUiState.Browsing(PAGES_4, index),
                onNext = {},
                onBack = {},
                onEnableCamera = {},
            )
        }
    }

    private companion object {
        val PAGES_4 = listOf(OnboardingPage.INTRO, OnboardingPage.POSES, OnboardingPage.PREMIUM, OnboardingPage.CAMERA)
    }
}
