package com.vglstudio.camera.pose.line.guide.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.vglstudio.camera.pose.line.guide.core.designsystem.R
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Radius
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Stroke
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.appColors
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.stickerShadow

private val TileSize = 112.dp
private val MarkWidth = 56.dp
private val MarkHeight = 52.dp

/**
 * Brand logo: the brand mark on a yellow sticker tile with an outline and a hard shadow.
 * No content description: the app name is always shown right below it.
 * Each app replaces res/drawable/brand_mark.xml with its own mark (ADR-0008).
 */
@Composable
fun BrandLogo(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(Radius.sheet)
    Box(
        modifier = modifier
            .size(TileSize)
            .stickerShadow(shape, MaterialTheme.appColors.shadowSticker)
            .background(colors.secondaryContainer, shape)
            .border(Stroke.sticker, colors.outline, shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.brand_mark),
            contentDescription = null,
            tint = colors.onSecondaryContainer,
            modifier = Modifier.size(width = MarkWidth, height = MarkHeight),
        )
    }
}
