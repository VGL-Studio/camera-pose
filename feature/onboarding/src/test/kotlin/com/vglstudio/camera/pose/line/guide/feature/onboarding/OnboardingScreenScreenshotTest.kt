package com.vglstudio.camera.pose.line.guide.feature.onboarding

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.vglstudio.camera.pose.line.guide.core.ads.NativeAdAssets
import com.vglstudio.camera.pose.line.guide.core.ads.NativeAdCard
import com.vglstudio.camera.pose.line.guide.core.ads.NativeAdPlaceholder
import com.vglstudio.camera.pose.line.guide.core.ads.NativeAdSize
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

    // ---- The card of T-083 (plan T-083, section 4): a fake step card, static ----

    @Test
    fun introAd_light() {
        captureRoboImage("src/test/screenshots/OnboardingScreen_intro-ad_light.png") {
            Page(index = 0, darkTheme = false, adSlot = FAKE_CARD)
        }
    }

    @Test
    fun introAd_dark() {
        captureRoboImage("src/test/screenshots/OnboardingScreen_intro-ad_dark.png") {
            Page(index = 0, darkTheme = true, adSlot = FAKE_CARD)
        }
    }

    @Test
    fun posesAd_light() {
        captureRoboImage("src/test/screenshots/OnboardingScreen_poses-ad_light.png") {
            Page(index = 1, darkTheme = false, adSlot = FAKE_CARD)
        }
    }

    @Test
    fun posesAd_dark() {
        captureRoboImage("src/test/screenshots/OnboardingScreen_poses-ad_dark.png") {
            Page(index = 1, darkTheme = true, adSlot = FAKE_CARD)
        }
    }

    @Test
    fun premiumAd_light() {
        captureRoboImage("src/test/screenshots/OnboardingScreen_premium-ad_light.png") {
            Page(index = 2, darkTheme = false, adSlot = FAKE_CARD)
        }
    }

    @Test
    fun premiumAd_dark() {
        captureRoboImage("src/test/screenshots/OnboardingScreen_premium-ad_dark.png") {
            Page(index = 2, darkTheme = true, adSlot = FAKE_CARD)
        }
    }

    @Test
    fun cameraAd_light() {
        captureRoboImage("src/test/screenshots/OnboardingScreen_camera-ad_light.png") {
            Page(index = 3, darkTheme = false, adSlot = FAKE_CARD)
        }
    }

    @Test
    fun cameraAd_dark() {
        captureRoboImage("src/test/screenshots/OnboardingScreen_camera-ad_dark.png") {
            Page(index = 3, darkTheme = true, adSlot = FAKE_CARD)
        }
    }

    // Page one while the card waits (L-029): the placeholder pulses forever, so the clock is stopped and the shot
    // is taken after one pulse, when the placeholder is fully opaque like State=Loading of the design.
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun introAdWait_light() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { Page(index = 0, darkTheme = false, adSlot = WAITING_CARD, nextEnabled = false) }
        mainClock.advanceTimeBy(WAIT_PULSE_CYCLE_MS)
        onRoot().captureRoboImage("src/test/screenshots/OnboardingScreen_intro-ad-wait_light.png")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun introAdWait_dark() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { Page(index = 0, darkTheme = true, adSlot = WAITING_CARD, nextEnabled = false) }
        mainClock.advanceTimeBy(WAIT_PULSE_CYCLE_MS)
        onRoot().captureRoboImage("src/test/screenshots/OnboardingScreen_intro-ad-wait_dark.png")
    }

    // Short phone frame (L-028), page one with the card: the illustration shrinks to 160, the page scrolls.
    @Test
    @Config(qualifiers = "vi-w360dp-h640dp-xxhdpi")
    fun introAd_small() {
        captureRoboImage("src/test/screenshots/OnboardingScreen_intro-ad_small.png") {
            Page(index = 0, darkTheme = false, adSlot = FAKE_CARD)
        }
    }

    // Short phone frame (L-028), last page with the card: the most pinned parts (card, note and button).
    @Test
    @Config(qualifiers = "vi-w360dp-h640dp-xxhdpi")
    fun cameraAd_small() {
        captureRoboImage("src/test/screenshots/OnboardingScreen_camera-ad_small.png") {
            Page(index = 3, darkTheme = false, adSlot = FAKE_CARD)
        }
    }

    /** Page [index] of the four-page flow, browsing (no spinner on the button), empty ad slot by default. */
    @Composable
    private fun Page(
        index: Int,
        darkTheme: Boolean,
        adSlot: @Composable (OnboardingPage) -> Unit = {},
        nextEnabled: Boolean = true,
    ) {
        AppTheme(darkTheme = darkTheme) {
            OnboardingScreen(
                uiState = OnboardingUiState.Browsing(PAGES_4, index),
                onNext = {},
                onBack = {},
                onEnableCamera = {},
                adSlot = adSlot,
                nextEnabled = nextEnabled,
            )
        }
    }

    private companion object {
        val PAGES_4 = listOf(OnboardingPage.INTRO, OnboardingPage.POSES, OnboardingPage.PREMIUM, OnboardingPage.CAMERA)

        /** One pulse of the placeholder (800 ms up, 800 ms down): fully opaque again. */
        const val WAIT_PULSE_CYCLE_MS = 1_600L

        /** The texts of the reference images NativeAdCard_step_* of the framework, as in the design. */
        val FAKE_AD = NativeAdAssets(
            headline = "Headline of the ad",
            advertiser = "Advertiser",
            body = "Body text of the ad. Up to three lines; the ad decides the words, the app decides the style.",
            callToAction = "Install",
            icon = null,
            hasMedia = true,
        )

        val FAKE_CARD: @Composable (OnboardingPage) -> Unit = {
            NativeAdCard(FAKE_AD, NativeAdSize.STEP, Modifier.fillMaxWidth())
        }

        val WAITING_CARD: @Composable (OnboardingPage) -> Unit = {
            NativeAdPlaceholder(NativeAdSize.STEP, Modifier.fillMaxWidth())
        }
    }
}
