package com.vglstudio.camera.pose.line.guide.feature.camera

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.CameraIconButton
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.GridOverlay
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.StateMessage
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.ZoomControl
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Radius
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Stroke
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.appColors
import com.vglstudio.camera.pose.line.guide.feature.camera.model.FlashMode
import com.vglstudio.camera.pose.line.guide.feature.camera.model.TimerSetting

// Sizes from design/context/.
internal val POSE_AREA_HEIGHT = 132.dp
private val TOP_BAR_HEIGHT = 48.dp
private val CAPTURE_ROW_HEIGHT = 76.dp
private val SHUTTER_SIZE = 76.dp
private val SHUTTER_RING = 4.dp
private val SHUTTER_INNER_SIZE = 62.dp
private val SHUTTER_ICON_SIZE = 28.dp
private val LAST_PHOTO_SIZE = 48.dp
private val COUNTDOWN_SIZE = 128.dp
private val SPINNER_SIZE = 40.dp
private const val COUNTDOWN_SCRIM_ALPHA = 0.35f
private const val DISABLED_ALPHA = 0.38f

/** Library on the left; on the right flash, timer, grid (camera ready only) and settings. */
@Composable
internal fun CameraTopBar(
    ready: CameraUiState.Ready?,
    onFlashClick: () -> Unit,
    onTimerClick: () -> Unit,
    onGridClick: () -> Unit,
    onOpenPoses: (() -> Unit)?,
    onOpenSettings: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md)
            .padding(top = Spacing.xl)
            .height(TOP_BAR_HEIGHT),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onOpenPoses != null) {
            CameraIconButton(
                icon = painterResource(R.drawable.ic_camera_grid_view),
                onClick = onOpenPoses,
                contentDescription = stringResource(R.string.camera_cd_poses),
                modifier = Modifier.testTag(CameraTestTags.POSES),
                toggleable = false,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            if (ready != null) {
                CameraIconButton(
                    icon = painterResource(flashIcon(ready.flash)),
                    onClick = onFlashClick,
                    contentDescription = stringResource(flashDescription(ready.flash)),
                    modifier = Modifier.testTag(CameraTestTags.FLASH),
                    enabled = ready.flashAvailable,
                    toggleable = false,
                )
                TimerButton(timer = ready.timer, onClick = onTimerClick)
                CameraIconButton(
                    icon = painterResource(R.drawable.ic_camera_grid_on),
                    onClick = onGridClick,
                    contentDescription = stringResource(R.string.camera_cd_grid),
                    modifier = Modifier.testTag(CameraTestTags.GRID_BUTTON),
                    active = ready.gridVisible,
                )
            }
            if (onOpenSettings != null) {
                CameraIconButton(
                    icon = painterResource(R.drawable.ic_camera_settings),
                    onClick = onOpenSettings,
                    contentDescription = stringResource(R.string.camera_cd_settings),
                    modifier = Modifier.testTag(CameraTestTags.SETTINGS),
                    toggleable = false,
                )
            }
        }
    }
}

/** Off: the timer icon. On: yellow with "3s" or "10s". */
@Composable
private fun TimerButton(timer: TimerSetting, onClick: () -> Unit) {
    val description = stringResource(
        when (timer) {
            TimerSetting.OFF -> R.string.camera_cd_timer_off
            TimerSetting.THREE -> R.string.camera_cd_timer_3
            TimerSetting.TEN -> R.string.camera_cd_timer_10
        },
    )
    val modifier = Modifier.testTag(CameraTestTags.TIMER)
    if (timer == TimerSetting.OFF) {
        CameraIconButton(
            icon = painterResource(R.drawable.ic_camera_timer),
            onClick = onClick,
            contentDescription = description,
            modifier = modifier,
        )
    } else {
        CameraIconButton(
            text = stringResource(R.string.camera_timer_label, timer.seconds),
            onClick = onClick,
            contentDescription = description,
            modifier = modifier,
            active = true,
        )
    }
}

/**
 * What the 3:4 frame shows in each state. [poseOverlay] (outline, step card, tools) is drawn only when the camera is
 * ready. [zoomVisible] is false while the adjust panel covers the foot of the frame.
 */
@Composable
internal fun PreviewContent(
    uiState: CameraUiState,
    onZoomSelect: (Float) -> Unit,
    onRetry: () -> Unit,
    onOpenAppSettings: () -> Unit,
    preview: @Composable (Modifier) -> Unit,
    poseOverlay: @Composable BoxScope.() -> Unit = {},
    zoomVisible: Boolean = true,
) {
    when (uiState) {
        CameraUiState.Starting -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(SPINNER_SIZE),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = stringResource(R.string.camera_starting),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.appColors.onCamera,
            )
        }
        CameraUiState.PermissionDenied -> StateMessage(
            icon = painterResource(R.drawable.ic_camera_no_photography),
            title = stringResource(R.string.camera_permission_title),
            message = stringResource(R.string.camera_permission_message),
            actionLabel = stringResource(R.string.camera_permission_action),
            actionIcon = painterResource(R.drawable.ic_camera_settings),
            onAction = onOpenAppSettings,
        )
        CameraUiState.Error -> StateMessage(
            icon = painterResource(R.drawable.ic_camera_error),
            title = stringResource(R.string.camera_error_title),
            message = stringResource(R.string.camera_error_message),
            actionLabel = stringResource(R.string.camera_error_action),
            actionIcon = painterResource(R.drawable.ic_camera_refresh),
            onAction = onRetry,
        )
        is CameraUiState.Ready -> ReadyPreview(
            ready = uiState,
            onZoomSelect = onZoomSelect,
            preview = preview,
            poseOverlay = poseOverlay,
            zoomVisible = zoomVisible,
        )
    }
}

@Composable
private fun ReadyPreview(
    ready: CameraUiState.Ready,
    onZoomSelect: (Float) -> Unit,
    preview: @Composable (Modifier) -> Unit,
    poseOverlay: @Composable BoxScope.() -> Unit = {},
    zoomVisible: Boolean = true,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        preview(Modifier.fillMaxSize())
        if (ready.gridVisible) GridOverlay(modifier = Modifier.testTag(CameraTestTags.GRID))
        // Above the grid, below the zoom and the countdown.
        poseOverlay()
        if (zoomVisible && ready.zoomLevels.size > 1) {
            ZoomControl(
                levels = ready.zoomLevels.map { zoomLabel(it, selected = it == ready.zoom) },
                selectedIndex = ready.zoomLevels.indexOf(ready.zoom),
                onSelect = { onZoomSelect(ready.zoomLevels[it]) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = Spacing.md)
                    .testTag(CameraTestTags.ZOOM),
            )
        }
        val countdown = ready.countdown
        if (countdown != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.appColors.cameraScrim.copy(alpha = COUNTDOWN_SCRIM_ALPHA)),
                contentAlignment = Alignment.Center,
            ) {
                CountdownBadge(seconds = countdown)
            }
        }
    }
}

@Composable
private fun CountdownBadge(seconds: Int) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(COUNTDOWN_SIZE)
            .background(colors.secondaryContainer, CircleShape)
            .border(Stroke.sticker, colors.onSecondaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = seconds.toString(),
            style = MaterialTheme.typography.displayLarge,
            color = colors.onSecondaryContainer,
            modifier = Modifier.testTag(CameraTestTags.COUNTDOWN),
        )
    }
}

/**
 * Last photo on the left (the latest saved photo, T-016), shutter in the middle, switch camera on the right. Without
 * [onLastPhotoClick] the slot is empty and cannot be tapped. The shutter ring is green while the pose is [matched]
 * (T-017).
 */
@Composable
internal fun CaptureRow(
    ready: CameraUiState.Ready?,
    onShutterClick: () -> Unit,
    onCancelCountdown: () -> Unit,
    onFlipClick: () -> Unit,
    lastPhoto: ImageBitmap? = null,
    onLastPhotoClick: (() -> Unit)? = null,
    matched: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(CAPTURE_ROW_HEIGHT),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            LastPhotoSlot(image = lastPhoto, onClick = onLastPhotoClick)
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            ShutterButton(
                ringColor = if (matched) MaterialTheme.appColors.successContainer else MaterialTheme.appColors.onCamera,
                enabled = ready != null && !ready.capturing,
                countingDown = ready?.countdown != null,
                onShutterClick = onShutterClick,
                onCancelCountdown = onCancelCountdown,
            )
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            CameraIconButton(
                icon = painterResource(R.drawable.ic_camera_flip),
                onClick = onFlipClick,
                contentDescription = stringResource(R.string.camera_cd_flip),
                modifier = Modifier.testTag(CameraTestTags.FLIP),
                enabled = ready != null,
                toggleable = false,
            )
        }
    }
}

/** The thumbnail is cropped to the slot; a photo that cannot be read leaves the slot empty but still tappable. */
@Composable
private fun LastPhotoSlot(image: ImageBitmap?, onClick: (() -> Unit)?) {
    val appColors = MaterialTheme.appColors
    val shape = RoundedCornerShape(Radius.thumb)
    val description = stringResource(R.string.camera_cd_last_photo)
    Box(
        modifier = Modifier
            .size(LAST_PHOTO_SIZE)
            .background(appColors.cameraSurface, shape)
            .border(Stroke.sticker, appColors.onCamera, shape)
            .semantics { contentDescription = description }
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .testTag(CameraTestTags.LAST_PHOTO),
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize().clip(shape),
            )
        }
    }
}

/** Ring 76 with a 62 disc; while counting down it shows a close icon and cancels instead. */
@Composable
private fun ShutterButton(
    ringColor: Color,
    enabled: Boolean,
    countingDown: Boolean,
    onShutterClick: () -> Unit,
    onCancelCountdown: () -> Unit,
) {
    val appColors = MaterialTheme.appColors
    val description = stringResource(
        if (countingDown) R.string.camera_cd_cancel_countdown else R.string.camera_cd_shutter,
    )
    Box(
        modifier = Modifier
            .size(SHUTTER_SIZE)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .border(SHUTTER_RING, ringColor, CircleShape)
            .semantics { contentDescription = description }
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = if (countingDown) onCancelCountdown else onShutterClick,
            )
            .testTag(CameraTestTags.SHUTTER),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.size(SHUTTER_INNER_SIZE).background(appColors.onCamera, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (countingDown) {
                Icon(
                    painter = painterResource(R.drawable.ic_camera_close),
                    contentDescription = null,
                    tint = appColors.cameraOnControl,
                    modifier = Modifier.size(SHUTTER_ICON_SIZE),
                )
            }
        }
    }
}

private fun flashIcon(mode: FlashMode): Int = when (mode) {
    FlashMode.OFF -> R.drawable.ic_camera_flash_off
    FlashMode.ON -> R.drawable.ic_camera_flash_on
    FlashMode.AUTO -> R.drawable.ic_camera_flash_auto
}

private fun flashDescription(mode: FlashMode): Int = when (mode) {
    FlashMode.OFF -> R.string.camera_cd_flash_off
    FlashMode.ON -> R.string.camera_cd_flash_on
    FlashMode.AUTO -> R.string.camera_cd_flash_auto
}

/** Selected level with an "x" ("1x"), others bare ("0.5", "2"); always a dot, whatever the locale. */
@Composable
private fun zoomLabel(ratio: Float, selected: Boolean): String {
    val number = if (ratio % 1f == 0f) ratio.toInt().toString() else ratio.toString()
    return if (selected) stringResource(R.string.camera_zoom_selected, number) else number
}
