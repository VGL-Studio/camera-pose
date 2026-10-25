package com.vglstudio.camera.pose.line.guide.feature.camera

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.feature.camera.CameraUiState.Ready
import com.vglstudio.camera.pose.line.guide.feature.camera.model.FlashMode
import com.vglstudio.camera.pose.line.guide.feature.camera.model.Lens
import com.vglstudio.camera.pose.line.guide.feature.camera.model.TimerSetting
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The last photo slot of the camera (spec camera, KB-6; plan T-016). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "vi-w411dp-h891dp-xxhdpi")
class CameraLastPhotoTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var clicks = 0

    private fun setScreen(lastPhoto: ImageBitmap?, onLastPhotoClick: (() -> Unit)?) {
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                CameraScreen(
                    uiState = READY,
                    onFlashClick = {},
                    onTimerClick = {},
                    onGridClick = {},
                    onFlipClick = {},
                    onZoomSelect = {},
                    onShutterClick = {},
                    onCancelCountdown = {},
                    onRetry = {},
                    onOpenAppSettings = {},
                    onOpenPoses = null,
                    onOpenSettings = null,
                    lastPhoto = lastPhoto,
                    onLastPhotoClick = onLastPhotoClick,
                )
            }
        }
    }

    @Test
    fun withPhoto_isClickable_andOpens() {
        setScreen(ImageBitmap(48, 48)) { clicks++ }
        composeRule.onNodeWithTag(CameraTestTags.LAST_PHOTO).assertHasClickAction().performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun withoutPhoto_isNotClickable() {
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                CameraScreen(
                    uiState = READY,
                    onFlashClick = {},
                    onTimerClick = {},
                    onGridClick = {},
                    onFlipClick = {},
                    onZoomSelect = {},
                    onShutterClick = {},
                    onCancelCountdown = {},
                    onRetry = {},
                    onOpenAppSettings = {},
                    onOpenPoses = null,
                    onOpenSettings = null,
                )
            }
        }
        composeRule.onNodeWithTag(CameraTestTags.LAST_PHOTO).assertHasNoClickAction()
    }

    @Test
    fun uriWithoutImage_stillClickable() {
        setScreen(lastPhoto = null) { clicks++ }
        composeRule.onNodeWithTag(CameraTestTags.LAST_PHOTO).assertHasClickAction().performClick()
        assertEquals(1, clicks)
    }

    private companion object {
        val READY = Ready(
            flash = FlashMode.OFF,
            flashAvailable = true,
            timer = TimerSetting.OFF,
            gridVisible = false,
            lens = Lens.BACK,
            zoomLevels = listOf(1f, 2f),
            zoom = 1f,
        )
    }
}
