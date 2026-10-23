package com.vglstudio.camera.pose.line.guide.feature.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.feature.camera.CameraUiState.Ready
import com.vglstudio.camera.pose.line.guide.feature.camera.model.FlashMode
import com.vglstudio.camera.pose.line.guide.feature.camera.model.Lens
import com.vglstudio.camera.pose.line.guide.feature.camera.model.TimerSetting
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.CategoryChip
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.PoseImage
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.PoseThumbItem
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.PoseUiState
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.SelectedPose
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.TestPoseImages
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot tests (Roborazzi). The tests in the standard frame named like a design image in
 * docs/features/camera/design/screens/ are compared by scripts/design-compare.sh (ADR-0020). Every test shows the
 * sample pose state of the design (chips, strip, outline and step card) with placeholder images; the overlay tool
 * column of T-015 is not drawn yet. ready_dark and the _small tests have no design image and are not compared.
 * starting_dark is in CameraScreenStartingScreenshotTest below.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class CameraScreenScreenshotTest {
    @Test
    fun permissionDenied_dark() {
        captureRoboImage("src/test/screenshots/CameraScreen_permission-denied_dark.png") {
            Screen(CameraUiState.PermissionDenied)
        }
    }

    @Test
    fun error_dark() {
        captureRoboImage("src/test/screenshots/CameraScreen_error_dark.png") { Screen(CameraUiState.Error) }
    }

    @Test
    fun countdown_dark() {
        captureRoboImage("src/test/screenshots/CameraScreen_countdown_dark.png") { Screen(COUNTDOWN) }
    }

    @Test
    fun ready_dark() {
        captureRoboImage("src/test/screenshots/CameraScreen_ready_dark.png") { Screen(ALL_TOOLS) }
    }

    @Test
    fun default_dark() {
        captureRoboImage("src/test/screenshots/CameraScreen_default_dark.png") { Screen(DEFAULT) }
    }

    @Test
    fun noPose_dark() {
        captureRoboImage("src/test/screenshots/CameraScreen_no-pose_dark.png") {
            Screen(DEFAULT, samplePoses(selected = false))
        }
    }

    @Test
    fun stepsHidden_dark() {
        captureRoboImage("src/test/screenshots/CameraScreen_steps-hidden_dark.png") {
            Screen(DEFAULT, samplePoses(stepsHidden = true))
        }
    }

    // Short phone frame (L-028): the most text, and the most buttons.
    @Test
    @Config(qualifiers = SHORT_SCREEN)
    fun permissionDenied_small() {
        captureRoboImage("src/test/screenshots/CameraScreen_permission-denied_small.png") {
            Screen(CameraUiState.PermissionDenied)
        }
    }

    @Test
    @Config(qualifiers = SHORT_SCREEN)
    fun countdown_small() {
        captureRoboImage("src/test/screenshots/CameraScreen_countdown_small.png") { Screen(COUNTDOWN) }
    }

    // The most content: two-row step card, outline, chips and strip.
    @Test
    @Config(qualifiers = SHORT_SCREEN)
    fun default_small() {
        captureRoboImage("src/test/screenshots/CameraScreen_default_small.png") { Screen(DEFAULT) }
    }

    companion object {
        const val SHORT_SCREEN = "w360dp-h640dp-xxhdpi"

        // Chips and step text of the design (context/CameraScreen_default_dark.md).
        val CHIPS = listOf(
            "women" to "Nữ",
            "men" to "Nam",
            "couple" to "Cặp đôi",
            "friends" to "Bạn bè",
            "travel" to "Du lịch",
            "cafe" to "Cà phê",
        )
        const val STEP_TEXT = "Xoay vai 45° về phía máy, cằm hơi hạ."

        /** Eight cells with placeholder images, the second selected, step 2 of 4 (design `default`). */
        fun samplePoses(selected: Boolean = true, stepsHidden: Boolean = false): PoseUiState.Ready {
            val ids = (1..8).map { "p$it" }
            val thumb = PoseImage.Ready(TestPoseImages.thumbnail())
            val selectedThumb = PoseImage.Ready(TestPoseImages.thumbnail(Color.White))
            return PoseUiState.Ready(
                categories = CHIPS.map { (id, label) -> CategoryChip(id, label) },
                selectedCategoryId = null,
                strip = ids.map { id ->
                    PoseThumbItem(id, "Pose $id", if (selected && id == "p2") selectedThumb else thumb)
                },
                selected = if (selected) {
                    SelectedPose(
                        id = "p2",
                        steps = listOf("Step 1", STEP_TEXT, "Step 3", "Step 4"),
                        stepIndex = 1,
                        outline = PoseImage.Ready(TestPoseImages.outline()),
                    )
                } else {
                    null
                },
                stepsHidden = stepsHidden,
            )
        }

        // Design `default`: flash off, timer 3 s, grid on, zoom 0.5, 1x, 2.
        val DEFAULT = Ready(
            flash = FlashMode.OFF,
            flashAvailable = true,
            timer = TimerSetting.THREE,
            gridVisible = true,
            lens = Lens.BACK,
            zoomLevels = listOf(0.5f, 1f, 2f),
            zoom = 1f,
        )

        val COUNTDOWN = Ready(
            flash = FlashMode.OFF,
            flashAvailable = true,
            timer = TimerSetting.THREE,
            gridVisible = true,
            lens = Lens.BACK,
            zoomLevels = listOf(0.5f, 1f, 2f),
            zoom = 1f,
            countdown = 3,
        )

        // Flash auto, timer 10 s, grid off, two zoom levels.
        val ALL_TOOLS = Ready(
            flash = FlashMode.AUTO,
            flashAvailable = true,
            timer = TimerSetting.TEN,
            gridVisible = false,
            lens = Lens.BACK,
            zoomLevels = listOf(1f, 2f),
            zoom = 1f,
        )
    }
}

/**
 * starting_dark, with the frame clock of Compose stopped. Its spinner never ends: captured like the other tests, the
 * capture waited about five minutes while Robolectric kept every trace section of every frame, and the test JVM ran
 * out of memory once the pose strip was on screen (L-029; plan T-014, step 4).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class CameraScreenStartingScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun starting_dark() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { Screen(CameraUiState.Starting) }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/CameraScreen_starting_dark.png")
    }
}

/** The camera screen with both top callbacks, so the top bar looks like the design. */
@Composable
private fun Screen(uiState: CameraUiState, poseState: PoseUiState = CameraScreenScreenshotTest.samplePoses()) {
    AppTheme(darkTheme = true) {
        CameraScreen(
            uiState = uiState,
            onFlashClick = {},
            onTimerClick = {},
            onGridClick = {},
            onFlipClick = {},
            onZoomSelect = {},
            onShutterClick = {},
            onCancelCountdown = {},
            onRetry = {},
            onOpenAppSettings = {},
            onOpenPoses = {},
            onOpenSettings = {},
            poseState = poseState,
        )
    }
}
