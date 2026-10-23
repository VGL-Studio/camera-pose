package com.vglstudio.camera.pose.line.guide.feature.camera.pose

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.appColors

private val HALO_BLUR = 6.dp
private val HALO_SHIFT = 1.5.dp

/**
 * The outline of the pose, tinted [color] (white; T-017 passes the success colour), over a dark halo so it reads on
 * light and dark scenes (design README, section 3: halo `camera/scrim` blurred 6). Modifier.blur works from API 31;
 * below that the halo is four copies shifted by 1.5 dp.
 */
@Composable
internal fun PoseOutline(
    bitmap: ImageBitmap,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.appColors.cameraOverlayStroke,
) {
    val halo = MaterialTheme.appColors.cameraScrim
    Box(modifier = modifier) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            TintedOutline(bitmap, halo, Modifier.matchParentSize().blur(HALO_BLUR, BlurredEdgeTreatment.Unbounded))
        } else {
            HALO_OFFSETS.forEach { (x, y) -> TintedOutline(bitmap, halo, Modifier.matchParentSize().offset(x, y)) }
        }
        TintedOutline(bitmap, color, Modifier.matchParentSize())
    }
}

@Composable
private fun TintedOutline(bitmap: ImageBitmap, color: Color, modifier: Modifier) {
    Image(
        bitmap = bitmap,
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(color, BlendMode.SrcIn),
    )
}

private val HALO_OFFSETS: List<Pair<Dp, Dp>> = listOf(
    0.dp to -HALO_SHIFT,
    0.dp to HALO_SHIFT,
    -HALO_SHIFT to 0.dp,
    HALO_SHIFT to 0.dp,
)
