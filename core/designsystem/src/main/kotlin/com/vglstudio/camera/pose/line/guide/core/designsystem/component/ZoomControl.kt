package com.vglstudio.camera.pose.line.guide.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Radius
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.appColors

private val LevelSize = 40.dp
private val PillHeight = 48.dp
private const val PILL_SCRIM_ALPHA = 0.45f

/**
 * Zoom levels in a pill on the camera (sticker.md, section 4). Takes formatted labels ("0.5", "1x", "2"), so the
 * component does not decide how a level is named. The selected level is yellow.
 */
@Composable
fun ZoomControl(levels: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val appColors = MaterialTheme.appColors
    val pill = RoundedCornerShape(Radius.full)
    Row(
        modifier = modifier
            .height(PillHeight)
            .clip(pill)
            .background(appColors.cameraScrim.copy(alpha = PILL_SCRIM_ALPHA), pill)
            .padding(Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        levels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .size(LevelSize)
                    .clip(CircleShape)
                    .background(if (selected) colors.secondaryContainer else Color.Transparent, CircleShape)
                    .selectable(selected = selected, role = Role.Button, onClick = { onSelect(index) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) colors.onSecondaryContainer else appColors.onCamera,
                )
            }
        }
    }
}
