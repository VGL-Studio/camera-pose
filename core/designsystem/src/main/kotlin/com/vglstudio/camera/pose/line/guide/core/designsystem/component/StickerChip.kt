package com.vglstudio.camera.pose.line.guide.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Radius
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Stroke

private val ChipHeight = 36.dp

// The badge sticks out of the top right corner (design: badge 78,647 on a chip 16,656 that is 76 wide).
private val BadgeOffsetX = 8.dp
private val BadgeOffsetY = (-9).dp

/**
 * Category chip of the sticker style (sticker.md, section 4): 36 high, sticker border. Selected: yellow with a
 * [SelectedBadge]; the badge is drawn outside the chip, so leave 9 dp above and 8 dp right of it. Screen readers hear
 * the label as a selected or unselected tab; the touch target is 48 dp high. [enabled] false: not selectable, read as
 * disabled; the caller fades it (plan T-061).
 */
@Composable
fun StickerChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(Radius.chip)
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .height(ChipHeight)
            .selectable(
                selected = selected,
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Only the fill is clipped, so the badge can stick out.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(if (selected) colors.secondaryContainer else colors.surface, shape)
                .indication(interactionSource, ripple())
                .border(Stroke.sticker, colors.outline, shape),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) colors.onSecondaryContainer else colors.onSurface,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = Spacing.md),
        )
        if (selected) {
            SelectedBadge(modifier = Modifier.align(Alignment.TopEnd).offset(x = BadgeOffsetX, y = BadgeOffsetY))
        }
    }
}
