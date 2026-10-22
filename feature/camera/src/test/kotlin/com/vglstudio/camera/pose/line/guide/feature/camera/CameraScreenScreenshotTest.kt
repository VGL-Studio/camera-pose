package com.vglstudio.camera.pose.line.guide.feature.camera

import androidx.compose.runtime.Composable
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.feature.camera.CameraUiState.Ready
import com.vglstudio.camera.pose.line.guide.feature.camera.model.FlashMode
import com.vglstudio.camera.pose.line.guide.feature.camera.model.Lens
import com.vglstudio.camera.pose.line.guide.feature.camera.model.TimerSetting
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot tests (Roborazzi). The four tests in the standard frame named like a design image in
 * docs/features/camera/design/screens/ are compared by scripts/design-compare.sh (ADR-0020); the chip row and the
 * pose strip are left empty until T-014, so those areas differ from the design. ready_dark and the _small tests
 * have no design image and are not compared. Both top callbacks are set, so the top bar looks like the design.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class CameraScreenScreenshotTest {
    @Test
    fun starting_dark() {
        captureRoboImage("src/test/screenshots/CameraScreen_starting_dark.png") { Screen(CameraUiState.Starting) }
    }

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

    @Composable
    private fun Screen(uiState: CameraUiState) {
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
            )
        }
    }

    private companion object {
        const val SHORT_SCREEN = "w360dp-h640dp-xxhdpi"

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
