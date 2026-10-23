package com.vglstudio.camera.pose.line.guide.feature.camera.pose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.StepCard
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.StepCardArrows
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Radius
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Stroke
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.appColors
import com.vglstudio.camera.pose.line.guide.feature.camera.CameraTestTags
import com.vglstudio.camera.pose.line.guide.feature.camera.R
import com.vglstudio.camera.pose.line.guide.feature.camera.match.MatchHintOverlay
import com.vglstudio.camera.pose.line.guide.feature.camera.match.MatchUiState

// The outline box of the design: 200 x 400 in the 411 x 548 frame.
private const val OUTLINE_WIDTH_FRACTION = 200f / 411f
private const val OUTLINE_HEIGHT_FRACTION = 400f / 548f
private val HIDE_DRAG_DISTANCE = 24.dp
private val SHOW_STEPS_HEIGHT = 48.dp
private val SMALL_ICON_SIZE = 20.dp

/**
 * What the pose part draws on the 3:4 frame when the camera is ready (spec camera, KB-1 to KB-3), bottom to top: the
 * drag and pinch area, the outline (moved, scaled, mirrored and faded as adjusted), the step card (or the "Bước"
 * button when it is hidden), the tool column when [toolsOffset] puts it in the frame, the adjust panel and the lock
 * hint. Without a pose it draws the hint to pick one. [hintBottomPadding] is the space under the lock hint: above the
 * zoom control, which is why it is the caller's. [onStepBottomChange] tells where the step card ends, for the place of
 * the tool column. [matchState] (plan T-017) turns the outline green when the pose matches and shows the matching hint
 * in the place of the lock hint, which wins while it shows; the adjust panel covers that place, so it hides it too.
 */
@Composable
internal fun BoxScope.PoseOverlay(
    state: PoseUiState,
    actions: PoseActions,
    hintBottomPadding: Dp = Spacing.md,
    toolsOffset: DpOffset? = null,
    onStepBottomChange: (Dp) -> Unit = {},
    matchState: MatchUiState = MatchUiState.Off,
) {
    if (state !is PoseUiState.Ready) return
    val selected = state.selected
    if (selected == null) {
        PreviewHint(
            icon = painterResource(R.drawable.ic_camera_swipe),
            text = stringResource(R.string.camera_no_pose_hint),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = Spacing.md)
                .testTag(CameraTestTags.NO_POSE_HINT),
        )
        return
    }
    val overlay = state.overlay
    val outline = selected.outline
    Box(
        modifier = Modifier
            .matchParentSize()
            .testTag(CameraTestTags.OVERLAY_GESTURES)
            .overlayGestures(
                enabled = !overlay.settings.hidden,
                onTransform = actions.onOverlayTransform,
                onEnd = actions.onOverlayGestureEnd,
            ),
    )
    if (outline is PoseImage.Ready && !overlay.settings.hidden) {
        // The layer is the whole frame, so the offsets are parts of the frame and the scale is about its centre.
        Box(
            modifier = Modifier.matchParentSize().graphicsLayer {
                translationX = overlay.offsetX * size.width
                translationY = overlay.offsetY * size.height
                scaleX = overlay.settings.scale * if (overlay.settings.mirrored) -1f else 1f
                scaleY = overlay.settings.scale
                alpha = overlay.settings.opacity
            },
        ) {
            PoseOutline(
                bitmap = outline.bitmap,
                color = if (matchState is MatchUiState.Matched) {
                    MaterialTheme.appColors.success
                } else {
                    MaterialTheme.appColors.cameraOverlayStroke
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(OUTLINE_WIDTH_FRACTION)
                    .fillMaxHeight(OUTLINE_HEIGHT_FRACTION)
                    .testTag(CameraTestTags.OUTLINE),
            )
        }
    }
    val density = LocalDensity.current
    val topStart = Modifier
        .align(Alignment.TopStart)
        .padding(Spacing.md)
        .onSizeChanged { size ->
            onStepBottomChange(with(density) { size.height.toDp() } + Spacing.md)
        }
    when {
        state.stepsHidden -> ShowStepsButton(
            onClick = actions.onShowSteps,
            modifier = topStart.testTag(CameraTestTags.SHOW_STEPS),
        )
        outline == PoseImage.Failed -> HideableStepCard(
            text = stringResource(R.string.camera_outline_failed),
            badge = null,
            onHide = actions.onHideSteps,
            modifier = topStart,
        ) {
            TextButton(onClick = actions.onRetryOutline) {
                Text(text = stringResource(R.string.camera_outline_retry))
            }
        }
        selected.steps.isNotEmpty() -> {
            val index = selected.stepIndex.coerceIn(0, selected.steps.lastIndex)
            HideableStepCard(
                text = selected.steps[index],
                badge = stringResource(R.string.camera_step_count, index + 1, selected.steps.size),
                onHide = actions.onHideSteps,
                modifier = topStart,
            ) {
                StepCardArrows(
                    onPrevious = actions.onPreviousStep,
                    onNext = actions.onNextStep,
                    previousEnabled = index > 0,
                    nextEnabled = index < selected.steps.lastIndex,
                    previousDescription = stringResource(R.string.camera_cd_step_previous),
                    nextDescription = stringResource(R.string.camera_cd_step_next),
                )
            }
        }
    }
    if (toolsOffset != null) {
        OverlayToolColumn(
            overlay = overlay,
            actions = actions,
            modifier = Modifier.align(Alignment.TopStart).offset(x = toolsOffset.x, y = toolsOffset.y),
        )
    }
    if (overlay.panelOpen) {
        // The panel sits 16 dp from the bottom and the sides; on a short frame it is as high as the frame allows.
        Box(
            modifier = Modifier.matchParentSize().padding(Spacing.md),
            contentAlignment = Alignment.BottomCenter,
        ) {
            OverlayAdjustPanel(overlay = overlay, actions = actions, modifier = Modifier.fillMaxWidth())
        }
    }
    if (matchState !is MatchUiState.Off && !overlay.lockHintVisible && !overlay.panelOpen) {
        MatchHintOverlay(
            state = matchState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = Spacing.md, end = Spacing.md, bottom = hintBottomPadding),
        )
    }
    if (overlay.lockHintVisible) {
        PreviewHint(
            icon = painterResource(R.drawable.ic_camera_lock),
            text = stringResource(R.string.camera_locked_hint),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = hintBottomPadding)
                .testTag(CameraTestTags.LOCK_HINT),
        )
    }
}

/**
 * The step card across the top of the frame. Dragging it up by more than 24 dp hides it once per drag; screen reader
 * users get the same as the "Ẩn thẻ bước" action, since they cannot swipe (plan T-014, question 11).
 */
@Composable
private fun HideableStepCard(
    text: String,
    badge: String?,
    onHide: () -> Unit,
    modifier: Modifier,
    trailing: @Composable RowScope.() -> Unit,
) {
    val currentOnHide by rememberUpdatedState(onHide)
    val hideDistance = with(LocalDensity.current) { HIDE_DRAG_DISTANCE.toPx() }
    val hideLabel = stringResource(R.string.camera_cd_hide_steps)
    StepCard(
        text = text,
        badge = badge,
        modifier = modifier
            .fillMaxWidth()
            .testTag(CameraTestTags.STEP_CARD)
            .pointerInput(hideDistance) {
                var dragged = 0f
                var hidden = false
                detectVerticalDragGestures(
                    onDragStart = {
                        dragged = 0f
                        hidden = false
                    },
                    onVerticalDrag = { change, amount ->
                        change.consume()
                        dragged += amount
                        if (!hidden && dragged < -hideDistance) {
                            hidden = true
                            currentOnHide()
                        }
                    },
                )
            }
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(hideLabel) {
                        currentOnHide()
                        true
                    },
                )
            },
        trailing = trailing,
    )
}

/** Shown in place of the hidden step card: brings it back (spec camera, KB-2). */
@Composable
private fun ShowStepsButton(onClick: () -> Unit, modifier: Modifier) {
    val appColors = MaterialTheme.appColors
    val pill = RoundedCornerShape(Radius.full)
    Row(
        modifier = modifier
            .height(SHOW_STEPS_HEIGHT)
            .clip(pill)
            .background(appColors.cameraControl, pill)
            .border(Stroke.sticker, appColors.cameraOnControl, pill)
            .clickable(
                role = Role.Button,
                onClickLabel = stringResource(R.string.camera_cd_show_steps),
                onClick = onClick,
            )
            .padding(horizontal = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_camera_steps),
            contentDescription = null,
            tint = appColors.cameraOnControl,
            modifier = Modifier.size(SMALL_ICON_SIZE),
        )
        Text(
            text = stringResource(R.string.camera_show_steps),
            style = MaterialTheme.typography.labelLarge,
            color = appColors.cameraOnControl,
        )
    }
}

/**
 * A short hint on the frame: icon and text in a white pill (design README, section 3, "Lời nhắc trên khung"). The
 * text wraps when the frame is narrow. T-015 ("Đã khóa lớp phủ") and T-017 ("Lùi lại để cả người vào khung") use it too.
 */
@Composable
internal fun PreviewHint(icon: Painter, text: String, modifier: Modifier = Modifier) {
    val appColors = MaterialTheme.appColors
    val pill = RoundedCornerShape(Radius.full)
    Row(
        modifier = modifier
            .clip(pill)
            .background(appColors.cameraControl, pill)
            .border(Stroke.sticker, appColors.cameraOnControl, pill)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = appColors.cameraOnControl,
            modifier = Modifier.size(SMALL_ICON_SIZE),
        )
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = appColors.cameraOnControl)
    }
}
