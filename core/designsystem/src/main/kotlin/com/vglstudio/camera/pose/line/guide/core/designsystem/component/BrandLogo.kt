package com.vglstudio.camera.pose.line.guide.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.vglstudio.camera.pose.line.guide.core.designsystem.R

private val CircleSize = 132.dp
private val TileSize = 88.dp
private val TileCornerRadius = 26.dp
private val MarkWidth = 56.dp
private val MarkHeight = 52.dp

/**
 * Brand logo: the brand mark on a primary tile, inside a primaryContainer circle.
 * No content description: the app name is always shown right below it.
 * Each app replaces res/drawable/brand_mark.xml with its own mark (ADR-0008).
 */
@Composable
fun BrandLogo(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier.size(CircleSize).background(colors.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.size(TileSize).background(colors.primary, RoundedCornerShape(TileCornerRadius)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.brand_mark),
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(width = MarkWidth, height = MarkHeight),
            )
        }
    }
}
