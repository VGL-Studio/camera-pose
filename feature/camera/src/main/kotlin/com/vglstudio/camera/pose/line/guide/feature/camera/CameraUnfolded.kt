package com.vglstudio.camera.pose.line.guide.feature.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Radius
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.appColors
import com.vglstudio.camera.pose.line.guide.feature.camera.match.MatchUiState
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.PoseActions
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.PoseOverlay
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.PosePicker
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.PoseUiState
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.toolColumnPlacement

// Sizes from design/context/CameraScreen_default_dark_unfolded.md (673 x 841): the frame is 450 x 600 at (16, 72); in
// the column on its right the controls sit, from the middle of the frame height, at these distances (frame 72 to 672:
// flip 226, shutter centre 372, last photo 500, zoom 620).
private val FRAME_HEIGHT_IN_DESIGN = 600.dp
private val FLIP_ABOVE_MIDDLE = 122.dp
private val LAST_PHOTO_BELOW_MIDDLE = 152.dp

// Room the right column needs at least: the zoom control (136) with a margin on both sides.
private val RIGHT_COLUMN_MIN_WIDTH = 136.dp + Spacing.md * 2

// Under the pose picker, as in the design (the strip ends at 804 of 841).
private val BOTTOM_MARGIN = Spacing.xl + Spacing.xs

/**
 * The unfolded layout of the camera (spec camera, KB-11; plan T-020): the 3:4 frame on the left under the top bar, as
 * high as the room allows; in the column on its right the switch camera button, the shutter, the last photo and the
 * zoom; the chips and the strip under both, across the screen.
 */
@Composable
internal fun CameraUnfoldedLayout(
    uiState: CameraUiState,
    onFlashClick: () -> Unit,
    onTimerClick: () -> Unit,
    onGridClick: () -> Unit,
    onFlipClick: () -> Unit,
    onZoomSelect: (Float) -> Unit,
    onShutterClick: () -> Unit,
    onCancelCountdown: (CountdownCancelReason) -> Unit,
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
    matchState: MatchUiState = MatchUiState.Off,
) {
    val appColors = MaterialTheme.appColors
    val ready = uiState as? CameraUiState.Ready
    val counting = ready?.countdown != null
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.cameraBackground)
            .testTag(CameraTestTags.SCREEN),
    ) {
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val aboveFrame = Spacing.md + TOP_BAR_HEIGHT + Spacing.sm
            val maxFrameWidth = (maxWidth - Spacing.md * 2 - RIGHT_COLUMN_MIN_WIDTH).coerceAtLeast(0.dp)
            val frameHeight = minOf(maxHeight - aboveFrame, maxFrameWidth / PREVIEW_ASPECT_RATIO).coerceAtLeast(0.dp)
            val frameWidth = frameHeight * PREVIEW_ASPECT_RATIO
            val overlay = (poseState as? PoseUiState.Ready)?.takeIf { it.selected != null }?.overlay
            val tools = if (ready != null) overlay else null
            var stepBottom by remember { mutableStateOf(DEFAULT_STEP_BOTTOM) }
            // The frame fills its box, so the tool column goes inside it (a margin of 0 on both sides).
            val placement = toolColumnPlacement(frameWidth.value, frameWidth.value, frameHeight.value, stepBottom.value)
            Row(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.width(frameWidth + Spacing.md * 2)) {
                    CameraTopBar(
                        ready = ready,
                        onFlashClick = onFlashClick,
                        onTimerClick = onTimerClick,
                        onGridClick = onGridClick,
                        onOpenPoses = onOpenPoses,
                        onOpenSettings = onOpenSettings,
                        topPadding = Spacing.md,
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Box(
                        modifier = Modifier
                            .padding(start = Spacing.md)
                            .size(frameWidth, frameHeight)
                            .clip(RoundedCornerShape(Radius.card))
                            .background(if (ready != null) appColors.cameraSurface else appColors.cameraBackground)
                            .testTag(CameraTestTags.PREVIEW),
                        contentAlignment = Alignment.Center,
                    ) {
                        PreviewContent(
                            uiState = uiState,
                            onZoomSelect = onZoomSelect,
                            onRetry = onRetry,
                            onOpenAppSettings = onOpenAppSettings,
                            preview = preview,
                            poseOverlay = {
                                PoseOverlay(
                                    state = poseState,
                                    actions = poseActions,
                                    // The zoom control is not in the frame here.
                                    hintBottomPadding = Spacing.md,
                                    toolsOffset = if (tools != null) DpOffset(placement.x.dp, placement.y.dp) else null,
                                    onStepBottomChange = { stepBottom = it },
                                    matchState = matchState,
                                    locked = counting,
                                )
                            },
                            zoomVisible = false,
                            onCountdownTap = { onCancelCountdown(CountdownCancelReason.PREVIEW) },
                        )
                    }
                }
                UnfoldedCaptureColumn(
                    ready = ready,
                    frameHeight = frameHeight,
                    onZoomSelect = onZoomSelect,
                    onShutterClick = onShutterClick,
                    onCancelCountdown = { onCancelCountdown(CountdownCancelReason.BUTTON) },
                    onFlipClick = onFlipClick,
                    lastPhoto = lastPhoto,
                    onLastPhotoClick = onLastPhotoClick,
                    matched = matchState is MatchUiState.Matched,
                    modifier = Modifier.weight(1f).padding(top = aboveFrame),
                )
            }
        }
        // Chip row and pose strip, across the screen, right under the frame.
        PosePicker(
            state = poseState,
            actions = poseActions,
            chipsState = chipsState,
            stripState = stripState,
            enabled = !counting,
        )
        Spacer(modifier = Modifier.height(BOTTOM_MARGIN))
    }
}

/**
 * The controls beside the frame, centred across the column and as high as the frame: the shutter in the middle of it,
 * the switch camera button above it, the last photo below it, the zoom at the foot. A frame lower than the 600 dp of
 * the design brings the distances closer in proportion.
 */
@Composable
internal fun UnfoldedCaptureColumn(
    ready: CameraUiState.Ready?,
    frameHeight: Dp,
    onZoomSelect: (Float) -> Unit,
    onShutterClick: () -> Unit,
    onCancelCountdown: () -> Unit,
    onFlipClick: () -> Unit,
    lastPhoto: ImageBitmap?,
    onLastPhotoClick: (() -> Unit)?,
    matched: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val scale = (frameHeight / FRAME_HEIGHT_IN_DESIGN).coerceAtMost(1f)
    Box(modifier = modifier.height(frameHeight)) {
        Box(modifier = Modifier.align(Alignment.Center).offset(y = -FLIP_ABOVE_MIDDLE * scale)) {
            FlipCameraButton(ready = ready, onFlipClick = onFlipClick)
        }
        Box(modifier = Modifier.align(Alignment.Center)) {
            ShutterButton(
                ringColor = if (matched) MaterialTheme.appColors.successContainer else MaterialTheme.appColors.onCamera,
                enabled = ready != null && !ready.capturing,
                countingDown = ready?.countdown != null,
                onShutterClick = onShutterClick,
                onCancelCountdown = onCancelCountdown,
            )
        }
        Box(modifier = Modifier.align(Alignment.Center).offset(y = LAST_PHOTO_BELOW_MIDDLE * scale)) {
            LastPhotoSlot(image = lastPhoto, onClick = onLastPhotoClick)
        }
        if (ready != null) {
            CameraZoom(
                ready = ready,
                onZoomSelect = onZoomSelect,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = Spacing.xs),
            )
        }
    }
}
