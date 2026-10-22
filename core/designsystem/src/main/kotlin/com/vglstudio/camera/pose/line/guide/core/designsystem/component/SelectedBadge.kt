package com.vglstudio.camera.pose.line.guide.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Test tag of the badge, so tests can tell whether a chip or a thumbnail shows it. */
const val SELECTED_BADGE_TAG = "selected_badge"

@Composable
fun SelectedBadge(modifier: Modifier = Modifier) {
    Box(modifier)
}
