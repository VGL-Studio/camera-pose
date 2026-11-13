package com.vglstudio.camera.pose.line.guide.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.vglstudio.camera.pose.line.guide.core.designsystem.R
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Radius

private val TileSize = 112.dp

/**
 * App logo: the launcher icon artwork, clipped to [Radius.sheet], no outline and no shadow.
 * No content description: the app name is always shown right below it.
 * Each app replaces res/drawable-nodpi/brand_logo.webp with its own artwork (ADR-0008).
 */
@Composable
fun BrandLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.brand_logo),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(TileSize)
            .clip(RoundedCornerShape(Radius.sheet)),
    )
}
