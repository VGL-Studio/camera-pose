package com.vglstudio.camera.pose.line.guide.core.designsystem.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Corner radii of the design system. */
object Radius {
    val image: Dp = 10.dp
    val button: Dp = 14.dp
    val thumb: Dp = 14.dp
    val card: Dp = 16.dp
    val chip: Dp = 18.dp
    val sheet: Dp = 24.dp
    val full: Dp = 999.dp

    val scale: List<Pair<String, Dp>> = listOf(
        "image" to image,
        "button" to button,
        "thumb" to thumb,
        "card" to card,
        "chip" to chip,
        "sheet" to sheet,
        "full" to full,
    )
}

/** Border widths of the design system. */
object Stroke {
    val thin: Dp = 1.dp
    val sticker: Dp = 2.dp
    val selected: Dp = 3.dp

    val scale: List<Pair<String, Dp>> = listOf("thin" to thin, "sticker" to sticker, "selected" to selected)
}

/** Hard shadow of the sticker style: shifted copy of the shape, no blur. */
object StickerShadow {
    val offset: Dp = 4.dp
}

fun Modifier.stickerShadow(shape: Shape, color: Color): Modifier = drawBehind {
    val offsetPx = StickerShadow.offset.toPx()
    translate(offsetPx, offsetPx) {
        drawOutline(shape.createOutline(size, layoutDirection, this@drawBehind), color)
    }
}
