package com.vglstudio.camera.pose.line.guide.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch as MaterialSwitch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vglstudio.camera.pose.line.guide.core.designsystem.R
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing

private val RowMinHeight = 64.dp
private val RowIconSize = 24.dp

/** What ends a [SettingsRow]: the four kinds of the design (Value, Switch, Link, Info). */
sealed interface SettingsRowTrailing {
    /** A chevron; the whole row opens a choice. */
    data class Chevron(val onClick: () -> Unit) : SettingsRowTrailing

    /** A switch; the whole row toggles it. */
    data class Switch(val checked: Boolean, val onCheckedChange: (Boolean) -> Unit) : SettingsRowTrailing

    /** An "open in new" icon; the whole row opens a page outside the screen. */
    data class Link(val onClick: () -> Unit) : SettingsRowTrailing

    /** Nothing: the row only shows information and cannot be pressed. */
    data object None : SettingsRowTrailing
}

/**
 * One row of a settings list (sticker.md, section 4; design settings, section 3): leading icon, title, an optional
 * [value] under the title, and the end given by [trailing]. The whole row is the touch target, at least 64 dp tall.
 */
@Composable
fun SettingsRow(
    icon: Painter,
    title: String,
    trailing: SettingsRowTrailing,
    modifier: Modifier = Modifier,
    value: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    val action = when (trailing) {
        is SettingsRowTrailing.Chevron -> Modifier.clickable(role = Role.Button, onClick = trailing.onClick)
        is SettingsRowTrailing.Link -> Modifier.clickable(role = Role.Button, onClick = trailing.onClick)
        is SettingsRowTrailing.Switch -> Modifier.toggleable(
            value = trailing.checked,
            role = Role.Switch,
            onValueChange = trailing.onCheckedChange,
        )
        // Not pressable: title and value are read together.
        SettingsRowTrailing.None -> Modifier.semantics(mergeDescendants = true) {}
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .then(action)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            modifier = Modifier.size(RowIconSize),
            tint = colors.onSurfaceVariant,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
            if (value != null) {
                Text(text = value, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
        when (trailing) {
            is SettingsRowTrailing.Chevron -> TrailingIcon(R.drawable.ic_chevron_right, SettingsRowTestTags.CHEVRON)
            is SettingsRowTrailing.Link -> TrailingIcon(R.drawable.ic_open_in_new, SettingsRowTestTags.LINK)
            is SettingsRowTrailing.Switch -> MaterialSwitch(
                checked = trailing.checked,
                // The row toggles; the switch only shows the state.
                onCheckedChange = null,
                modifier = Modifier.testTag(SettingsRowTestTags.SWITCH),
                colors = SwitchDefaults.colors(
                    checkedTrackColor = colors.primary,
                    checkedThumbColor = colors.onPrimary,
                    checkedBorderColor = colors.outline,
                    uncheckedTrackColor = colors.surface,
                    uncheckedThumbColor = colors.outline,
                    uncheckedBorderColor = colors.outline,
                ),
            )
            SettingsRowTrailing.None -> Unit
        }
    }
}

@Composable
private fun TrailingIcon(icon: Int, tag: String) {
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        modifier = Modifier.size(RowIconSize).testTag(tag),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Title of a group of settings rows, announced as a heading (design settings, section 3). */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { heading() }
            .padding(start = Spacing.md, top = Spacing.lg, end = Spacing.md, bottom = Spacing.sm),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

object SettingsRowTestTags {
    const val CHEVRON = "settings_row_chevron"
    const val LINK = "settings_row_link"
    const val SWITCH = "settings_row_switch"
}
