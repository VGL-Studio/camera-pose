package com.vglstudio.camera.pose.line.guide.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vglstudio.camera.pose.line.guide.core.designsystem.R
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Radius
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Stroke

/** Test tag of the retry icon of a thumbnail that failed to load. */
const val POSE_THUMB_RETRY_TAG = "pose_thumb_retry"

private val ThumbSize = 64.dp
private val RetryIconSize = 24.dp

// Design: badge 137,701 on a thumbnail 88,708.
private val BadgeOffset = 7.dp

/**
 * Pose thumbnail of the strip (sticker.md, section 4): 64 dp square, [image] cropped to the rounded corners. Selected:
 * a 3 dp yellow border and a [SelectedBadge] outside the top right corner. No image yet: only the fill. [failed]: a
 * retry icon in the middle. Screen readers hear [contentDescription] and whether it is selected. [enabled] false: not
 * selectable, read as disabled; the caller fades it (plan T-061).
 */
@Composable
fun PoseThumb(
    image: ImageBitmap?,
    selected: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    failed: Boolean = false,
    enabled: Boolean = true,
    premium: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(Radius.thumb)
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(ThumbSize)
            .semantics { this.contentDescription = contentDescription }
            .selectable(
                selected = selected,
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(colors.surfaceVariant, shape)
                .indication(interactionSource, ripple()),
        ) {
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }
        if (failed) {
            Icon(
                painter = painterResource(R.drawable.ic_refresh),
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(RetryIconSize).testTag(POSE_THUMB_RETRY_TAG),
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(
                    width = if (selected) Stroke.selected else Stroke.sticker,
                    color = if (selected) colors.secondaryContainer else colors.outline,
                    shape = shape,
                ),
        )
        if (selected) {
            SelectedBadge(modifier = Modifier.align(Alignment.TopEnd).offset(x = BadgeOffset, y = -BadgeOffset))
        }
    }
}
