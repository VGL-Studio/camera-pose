package com.vglstudio.camera.pose.line.guide.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

object GridOverlayDefaults {
    val Alpha = 0.18f
}

@Composable
fun GridOverlay(modifier: Modifier = Modifier, color: Color = Color.Unspecified, alpha: Float = GridOverlayDefaults.Alpha) {
    Box(modifier)
}
