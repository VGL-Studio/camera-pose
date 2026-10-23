package com.vglstudio.camera.pose.line.guide.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter

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

/** Skeleton for the tests written first (plan T-025, section 4). */
@Composable
fun SettingsRow(
    icon: Painter,
    title: String,
    trailing: SettingsRowTrailing,
    modifier: Modifier = Modifier,
    value: String? = null,
) {
    Box(modifier)
}

/** Skeleton for the tests written first (plan T-025, section 4). */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Box(modifier)
}

object SettingsRowTestTags {
    const val CHEVRON = "settings_row_chevron"
    const val LINK = "settings_row_link"
    const val SWITCH = "settings_row_switch"
}
