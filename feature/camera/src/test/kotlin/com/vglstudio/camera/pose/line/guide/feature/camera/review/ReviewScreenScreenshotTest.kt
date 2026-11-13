package com.vglstudio.camera.pose.line.guide.feature.camera.review

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.vglstudio.camera.pose.line.guide.core.ads.BottomAdBarDefaults
import com.vglstudio.camera.pose.line.guide.core.ads.BottomAdBarLayout
import com.vglstudio.camera.pose.line.guide.core.ads.NativeAdAssets
import com.vglstudio.camera.pose.line.guide.core.ads.NativeAdCard
import com.vglstudio.camera.pose.line.guide.core.ads.NativeAdSize
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.feature.camera.review.ReviewUiState.Ready
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot tests (Roborazzi) of the review screen. Tests named like a design image in
 * docs/features/camera/design/screens/ are compared by scripts/design-compare.sh (ADR-0020). The photo frame is empty
 * (no image) where the design shows a placeholder; the frame has no status bar, so everything sits 24 dp higher
 * than in the design. saving_dark is in ReviewScreenSavingScreenshotTest below.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "vi-w411dp-h891dp-xxhdpi")
class ReviewScreenScreenshotTest {
    @Test
    fun default_dark() {
        captureRoboImage("src/test/screenshots/ReviewScreen_default_dark.png") {
            Screen(Ready(PATH, SaveStatus.NOT_SAVED))
        }
    }

    @Test
    fun saveError_dark() {
        captureRoboImage("src/test/screenshots/ReviewScreen_save-error_dark.png") {
            Screen(Ready(PATH, SaveStatus.FAILED_STORAGE_FULL))
        }
    }

    @Test
    fun discard_dark() {
        captureRoboImage("src/test/screenshots/ReviewScreen_discard_dark.png") {
            Screen(Ready(PATH, SaveStatus.NOT_SAVED, pendingExit = ExitAction.RETAKE))
        }
    }

    // Short phone frame (L-028): the most content, with the error message, three actions and the main button.
    @Test
    @Config(qualifiers = "vi-w360dp-h640dp-xxhdpi")
    fun saveError_small() {
        captureRoboImage("src/test/screenshots/ReviewScreen_save-error_small.png") {
            Screen(Ready(PATH, SaveStatus.FAILED_STORAGE_FULL))
        }
    }

    // The same state in Portuguese, usually the longest of the release languages (plan T-050).
    @Test
    @Config(qualifiers = "pt-rBR-w360dp-h640dp-xxhdpi")
    fun saveError_portuguese_small() {
        captureRoboImage("src/test/screenshots/ReviewScreen_save-error-pt-br_small.png") {
            Screen(Ready(PATH, SaveStatus.FAILED_STORAGE_FULL))
        }
    }

    // The bottom ad bar (plan T-094), named like the design image: the photo kept, the main button above the slot.
    @Test
    fun ad_dark() {
        captureRoboImage("src/test/screenshots/ReviewScreen_ad_dark.png") {
            Screen(Ready(PATH, SaveStatus.NOT_SAVED), adBar = screenshotAdBar())
        }
    }

    // Short phone frame with the bar: the most content, the message with "Mở cài đặt", the actions and the button.
    @Test
    @Config(qualifiers = "vi-w360dp-h640dp-xxhdpi")
    fun ad_small() {
        captureRoboImage("src/test/screenshots/ReviewScreen_ad_small.png") {
            Screen(Ready(PATH, SaveStatus.PERMISSION_BLOCKED), adBar = screenshotAdBar())
        }
    }
}

/**
 * saving_dark, with the frame clock of Compose stopped: the spinner of the main button never ends, and capturing it
 * like the other tests waits about five minutes and can run the test JVM out of memory (L-029, plan T-014 step 4).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "vi-w411dp-h891dp-xxhdpi")
class ReviewScreenSavingScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun saving_dark() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { Screen(Ready(PATH, SaveStatus.SAVING)) }
        composeRule.mainClock.advanceTimeByFrame()
        // A fixed point of the spinner animation where its arc is clearly drawn, as in the design.
        composeRule.mainClock.advanceTimeBy(SPINNER_ARC_VISIBLE_MS)
        composeRule.onRoot().captureRoboImage("src/test/screenshots/ReviewScreen_saving_dark.png")
    }

    private companion object {
        const val SPINNER_ARC_VISIBLE_MS = 500L
    }
}

private const val PATH = "/cache/captures/capture_1.jpg"

@Composable
private fun Screen(uiState: ReviewUiState, adBar: ReviewAdBar? = null) {
    AppTheme(darkTheme = true) {
        if (adBar == null) {
            ReviewScreen(
                uiState = uiState,
                image = null,
                onCloseClick = {},
                onSaveClick = {},
                onRetrySave = {},
                onOpenAppSettings = {},
                onRetakeClick = {},
                onShareClick = {},
                onNextPoseClick = {},
                onDiscardConfirm = {},
                onDiscardSave = {},
                onDismissDialog = {},
            )
        } else {
            ReviewScreen(
                uiState = uiState,
                image = null,
                onCloseClick = {},
                onSaveClick = {},
                onRetrySave = {},
                onOpenAppSettings = {},
                onRetakeClick = {},
                onShareClick = {},
                onNextPoseClick = {},
                onDiscardConfirm = {},
                onDiscardSave = {},
                onDismissDialog = {},
                adBar = adBar,
            )
        }
    }
}

/** The bar of the framework with the fixed ad of the design (context/ReviewScreen_ad_dark.md), always with its ad. */
internal fun screenshotAdBar(): ReviewAdBar = { barModifier, content ->
    BottomAdBarLayout(
        reservation = BottomAdBarDefaults.nativeReservation,
        slot = { NativeAdCard(SCREENSHOT_AD, NativeAdSize.SMALL, it) },
        layer = null,
        modifier = barModifier,
        content = content,
    )
}

/** The fixed ad of the design: the same text as the other screens of the app, no icon, no media. */
private val SCREENSHOT_AD = NativeAdAssets(
    headline = "Headline of the ad",
    advertiser = "Advertiser or short body",
    body = null,
    callToAction = "Install",
    icon = null,
    hasMedia = false,
)
