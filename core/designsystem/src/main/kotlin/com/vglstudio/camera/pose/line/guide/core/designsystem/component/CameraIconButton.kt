package com.vglstudio.camera.pose.line.guide.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Stroke
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.appColors

private val ButtonSize = 48.dp
private val IconSize = 24.dp
private const val DISABLED_ALPHA = 0.38f

/**
 * Round 48 dp camera control (sticker.md, section 4): white with a dark sticker border, yellow while [active]
 * (a tool that is on). Disabled: faded and not clickable. Screen readers hear [contentDescription] and, for a switch
 * ([toggleable], the default), whether it is selected. A button that only acts (library, settings, flash, switch
 * camera) passes `toggleable = false`, so it is not read as "not selected".
 */
@Composable
fun CameraIconButton(
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    enabled: Boolean = true,
    toggleable: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val appColors = MaterialTheme.appColors
    val container = if (active) colors.secondaryContainer else appColors.cameraControl
    val contentColor = if (active) colors.onSecondaryContainer else appColors.cameraOnControl
    Box(
        modifier = modifier
            .size(ButtonSize)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clip(CircleShape)
            .background(container, CircleShape)
            .border(Stroke.sticker, appColors.cameraOnControl, CircleShape)
            .semantics {
                if (contentDescription != null) this.contentDescription = contentDescription
                if (toggleable) selected = active
            }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor, content = content)
    }
}

@Composable
fun CameraIconButton(
    icon: Painter,
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    enabled: Boolean = true,
    toggleable: Boolean = true,
) {
    CameraIconButton(onClick, contentDescription, modifier, active, enabled, toggleable) {
        Icon(painter = icon, contentDescription = null, modifier = Modifier.size(IconSize))
    }
}

/** A text label instead of an icon, for example "3s" on the timer while it is on. */
@Composable
fun CameraIconButton(
    text: String,
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    enabled: Boolean = true,
    toggleable: Boolean = true,
) {
    CameraIconButton(onClick, contentDescription, modifier, active, enabled, toggleable) {
        Text(text = text, style = MaterialTheme.typography.titleMedium, color = LocalContentColor.current)
    }
}
