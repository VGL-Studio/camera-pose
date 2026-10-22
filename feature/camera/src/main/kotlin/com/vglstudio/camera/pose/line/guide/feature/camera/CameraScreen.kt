package com.vglstudio.camera.pose.line.guide.feature.camera

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@Composable
internal fun CameraRoute(onPhotoCaptured: () -> Unit, onOpenPoses: (() -> Unit)?, onOpenSettings: (() -> Unit)?) = Unit

@Composable
internal fun CameraScreen(
    uiState: CameraUiState,
    onFlashClick: () -> Unit,
    onTimerClick: () -> Unit,
    onGridClick: () -> Unit,
    onFlipClick: () -> Unit,
    onZoomSelect: (Float) -> Unit,
    onShutterClick: () -> Unit,
    onCancelCountdown: () -> Unit,
    onRetry: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenPoses: (() -> Unit)?,
    onOpenSettings: (() -> Unit)?,
    modifier: Modifier = Modifier,
    preview: @Composable (Modifier) -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize().testTag(CameraTestTags.SCREEN))
}

internal object CameraTestTags {
    const val SCREEN = "camera_screen"
    const val PREVIEW = "camera_preview"
    const val POSES = "camera_poses"
    const val SETTINGS = "camera_settings"
    const val FLASH = "camera_flash"
    const val TIMER = "camera_timer"
    const val GRID_BUTTON = "camera_grid_button"
    const val GRID = "camera_grid"
    const val ZOOM = "camera_zoom"
    const val COUNTDOWN = "camera_countdown"
    const val SHUTTER = "camera_shutter"
    const val LAST_PHOTO = "camera_last_photo"
    const val FLIP = "camera_flip"
}
