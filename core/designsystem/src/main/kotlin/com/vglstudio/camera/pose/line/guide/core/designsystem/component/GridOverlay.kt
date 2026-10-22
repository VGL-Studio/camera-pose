package com.vglstudio.camera.pose.line.guide.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Stroke
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.appColors

object GridOverlayDefaults {
    /** Opacity of the grid lines on the camera (camera design README, section 3). */
    val Alpha = 0.18f
}

/** Rule-of-thirds grid: two vertical and two horizontal lines, 1 dp, over the whole area it is given. */
@Composable
fun GridOverlay(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.appColors.onCamera,
    alpha: Float = GridOverlayDefaults.Alpha,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val lineColor = color.copy(alpha = alpha)
        val width = Stroke.thin.toPx()
        for (third in 1..2) {
            val x = size.width * third / 3
            val y = size.height * third / 3
            drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), width)
            drawLine(lineColor, Offset(0f, y), Offset(size.width, y), width)
        }
    }
}
