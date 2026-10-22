package com.vglstudio.camera.pose.line.guide.feature.camera.pose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin

/**
 * Placeholder pose images for UI and screenshot tests: a figure drawn with lines, like the placeholders of the design.
 * Real images come from R2 (ADR-0015).
 */
internal object TestPoseImages {
    /** An outline.webp: white lines on a transparent background, 1:2. */
    fun outline(): ImageBitmap = figure(width = 400, height = 800, color = Color.White, strokePx = 9f, scale = 1f)

    /** A thumbnail: a small figure in the middle of a transparent square, as on the design. */
    fun thumbnail(color: Color = Color.White.copy(alpha = 0.7f)): ImageBitmap =
        figure(width = 192, height = 192, color = color, strokePx = 4.5f, scale = 0.8f)

    private fun figure(width: Int, height: Int, color: Color, strokePx: Float, scale: Float): ImageBitmap {
        val image = ImageBitmap(width, height)
        val canvas = Canvas(image)
        val paint = Paint().apply {
            this.color = color
            style = PaintingStyle.Stroke
            strokeWidth = strokePx
            strokeCap = StrokeCap.Round
            strokeJoin = StrokeJoin.Round
            isAntiAlias = true
        }
        // The figure box is 1:2, centred, [scale] of the image height.
        val boxHeight = height * scale
        val boxWidth = boxHeight / 2
        val left = (width - boxWidth) / 2
        val top = (height - boxHeight) / 2
        fun point(x: Float, y: Float) = Offset(left + x * boxWidth, top + y * boxHeight)

        canvas.drawCircle(point(0.5f, 0.12f), boxWidth * 0.13f, paint)
        val lines = listOf(
            listOf(0.5f to 0.25f, 0.5f to 0.31f),
            listOf(0.18f to 0.56f, 0.24f to 0.33f, 0.76f to 0.33f, 0.84f to 0.56f),
            listOf(0.3f to 0.33f, 0.33f to 0.6f, 0.67f to 0.6f, 0.7f to 0.33f),
            listOf(0.38f to 0.6f, 0.34f to 0.98f),
            listOf(0.62f to 0.6f, 0.68f to 0.98f),
        )
        lines.forEach { line ->
            val path = Path()
            line.forEachIndexed { index, (x, y) ->
                val p = point(x, y)
                if (index == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            canvas.drawPath(path, paint)
        }
        return image
    }
}
