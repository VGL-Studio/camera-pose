package com.vglstudio.camera.pose.line.guide.feature.camera

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.PoseActions
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.PoseUiState

/** The unfolded layout of the camera (plan T-020). */
@Composable
internal fun CameraUnfoldedLayout(
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
    preview: @Composable (Modifier) -> Unit,
    poseState: PoseUiState,
    poseActions: PoseActions,
    lastPhoto: ImageBitmap?,
    onLastPhotoClick: (() -> Unit)?,
    chipsState: LazyListState,
    stripState: LazyListState,
) {
    CameraPhoneLayout(
        uiState = uiState,
        onFlashClick = onFlashClick,
        onTimerClick = onTimerClick,
        onGridClick = onGridClick,
        onFlipClick = onFlipClick,
        onZoomSelect = onZoomSelect,
        onShutterClick = onShutterClick,
        onCancelCountdown = onCancelCountdown,
        onRetry = onRetry,
        onOpenAppSettings = onOpenAppSettings,
        onOpenPoses = onOpenPoses,
        onOpenSettings = onOpenSettings,
        preview = preview,
        poseState = poseState,
        poseActions = poseActions,
        lastPhoto = lastPhoto,
        onLastPhotoClick = onLastPhotoClick,
        chipsState = chipsState,
        stripState = stripState,
    )
}
