package com.vglstudio.camera.pose.line.guide.feature.camera.layout

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** From this width the camera and the review screen use the unfolded layout (plan T-020). */
internal val UNFOLDED_MIN_WIDTH = 600.dp

/** True when a screen of [width] is wide enough for the unfolded layout. */
internal fun isUnfolded(width: Dp): Boolean = width >= UNFOLDED_MIN_WIDTH
