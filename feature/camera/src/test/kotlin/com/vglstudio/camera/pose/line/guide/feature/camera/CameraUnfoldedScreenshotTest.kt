package com.vglstudio.camera.pose.line.guide.feature.camera

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.feature.camera.CameraScreenScreenshotTest.Companion.DEFAULT
import com.vglstudio.camera.pose.line.guide.feature.camera.CameraScreenScreenshotTest.Companion.samplePoses
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot test (Roborazzi) of the camera on an unfolded phone, 673 x 841 dp. Named like the design image in
 * docs/features/camera/design/screens/, so scripts/design-compare.sh compares them (ADR-0020). Same state as
 * CameraScreen_default_dark. No compose rule in this class: the rule opens one more window and Roborazzi would then
 * capture the whole screen.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "vi-w673dp-h841dp-xxhdpi")
class CameraUnfoldedScreenshotTest {
    @Test
    fun default_dark_unfolded() {
        captureRoboImage("src/test/screenshots/CameraScreen_default_dark_unfolded.png") {
            AppTheme(darkTheme = true) {
                CameraScreen(
                    uiState = DEFAULT,
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
                    poseState = samplePoses(),
                )
            }
        }
    }
}
