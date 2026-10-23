package com.vglstudio.camera.pose.line.guide.feature.camera.pose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.AdjustAction
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.AdjustPanel
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.AdjustSlider
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.CameraIconButton
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import com.vglstudio.camera.pose.line.guide.feature.camera.CameraTestTags
import com.vglstudio.camera.pose.line.guide.feature.camera.R

/**
 * The four tools beside the frame, top to bottom: adjust, mirror, lock, show or hide the outline (spec camera, KB-3).
 * While locked, adjust and mirror are dimmed and cannot be pressed.
 */
@Composable
internal fun OverlayToolColumn(overlay: OverlayState, actions: PoseActions, modifier: Modifier = Modifier) {
    val settings = overlay.settings
    Column(
        modifier = modifier.testTag(CameraTestTags.OVERLAY_TOOLS),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        CameraIconButton(
            icon = painterResource(R.drawable.ic_camera_tune),
            onClick = actions.onTogglePanel,
            contentDescription = stringResource(R.string.camera_cd_adjust),
            modifier = Modifier.testTag(CameraTestTags.TOOL_ADJUST),
            active = overlay.panelOpen,
            enabled = !settings.locked,
        )
        CameraIconButton(
            icon = painterResource(R.drawable.ic_camera_mirror),
            onClick = actions.onToggleMirror,
            contentDescription = stringResource(R.string.camera_cd_mirror),
            modifier = Modifier.testTag(CameraTestTags.TOOL_MIRROR),
            active = settings.mirrored,
            enabled = !settings.locked,
        )
        CameraIconButton(
            icon = painterResource(if (settings.locked) R.drawable.ic_camera_lock else R.drawable.ic_camera_lock_open),
            onClick = actions.onToggleLock,
            contentDescription = stringResource(
                if (settings.locked) R.string.camera_cd_unlock else R.string.camera_cd_lock,
            ),
            modifier = Modifier.testTag(CameraTestTags.TOOL_LOCK),
            active = settings.locked,
        )
        CameraIconButton(
            icon = painterResource(
                if (settings.hidden) R.drawable.ic_camera_visibility_off else R.drawable.ic_camera_visibility,
            ),
            onClick = actions.onToggleHidden,
            contentDescription = stringResource(
                if (settings.hidden) R.string.camera_cd_show_outline else R.string.camera_cd_hide_outline,
            ),
            modifier = Modifier.testTag(CameraTestTags.TOOL_VISIBILITY),
            active = !settings.hidden,
        )
    }
}

/**
 * The adjust panel: opacity and size sliders, then flip, lock, hide and reset. The caller bounds the height; what
 * does not fit below the title scrolls, and the four actions wrap to a second row when the frame is narrow.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun OverlayAdjustPanel(overlay: OverlayState, actions: PoseActions, modifier: Modifier = Modifier) {
    val settings = overlay.settings
    AdjustPanel(
        title = stringResource(R.string.camera_adjust_title),
        onClose = actions.onClosePanel,
        closeDescription = stringResource(R.string.camera_cd_close_adjust),
        modifier = modifier.testTag(CameraTestTags.ADJUST_PANEL),
    ) {
        Column(
            modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            AdjustSlider(
                label = stringResource(R.string.camera_adjust_opacity),
                valueText = stringResource(R.string.camera_adjust_percent, percent(settings.opacity)),
                value = settings.opacity,
                valueRange = MIN_OPACITY..MAX_OPACITY,
                onValueChange = actions.onOpacityChange,
                onValueChangeFinished = actions.onOpacityChangeFinished,
                modifier = Modifier.testTag(CameraTestTags.OPACITY_SLIDER),
            )
            AdjustSlider(
                label = stringResource(R.string.camera_adjust_size),
                valueText = stringResource(R.string.camera_adjust_percent, percent(settings.scale)),
                value = settings.scale,
                valueRange = MIN_SCALE..MAX_SCALE,
                onValueChange = actions.onScaleChange,
                onValueChangeFinished = actions.onScaleChangeFinished,
                modifier = Modifier.testTag(CameraTestTags.SIZE_SLIDER),
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                AdjustAction(
                    icon = painterResource(R.drawable.ic_camera_mirror),
                    label = stringResource(R.string.camera_adjust_flip),
                    onClick = actions.onToggleMirror,
                    active = settings.mirrored,
                    enabled = !settings.locked,
                )
                AdjustAction(
                    icon = painterResource(
                        if (settings.locked) R.drawable.ic_camera_lock else R.drawable.ic_camera_lock_open,
                    ),
                    label = stringResource(R.string.camera_adjust_lock),
                    onClick = actions.onToggleLock,
                    active = settings.locked,
                )
                AdjustAction(
                    icon = painterResource(R.drawable.ic_camera_visibility_off),
                    label = stringResource(R.string.camera_adjust_hide),
                    onClick = actions.onToggleHidden,
                    active = settings.hidden,
                )
                AdjustAction(
                    icon = painterResource(R.drawable.ic_camera_reset),
                    label = stringResource(R.string.camera_adjust_reset),
                    onClick = actions.onResetOverlay,
                    enabled = !settings.locked,
                )
            }
        }
    }
}
