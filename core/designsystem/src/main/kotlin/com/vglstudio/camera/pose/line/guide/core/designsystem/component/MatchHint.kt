package com.vglstudio.camera.pose.line.guide.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Radius
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Stroke
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.appColors

private val PillMinHeight = 56.dp
private val CircleSize = 40.dp
private val IconSize = 22.dp

/**
 * The pose matching hint on the camera (sticker.md, section 4): a pill at least 56 high with a 40 dp circle that holds
 * the [score] (or [icon] when there is no score) and a short [text] that wraps when the room is short. [matched] turns
 * the pill green. One merged node, a polite live region, so a screen reader says the new text when it changes; the
 * caller adds a content description when the score and the text should be read as one sentence.
 */
@Composable
fun MatchHint(
    text: String,
    modifier: Modifier = Modifier,
    score: Int? = null,
    matched: Boolean = false,
    icon: Painter? = null,
) {
    val colors = MaterialTheme.colorScheme
    val appColors = MaterialTheme.appColors
    val pill = RoundedCornerShape(Radius.full)
    val background = if (matched) appColors.successContainer else appColors.cameraControl
    val circle = if (!matched && score != null) colors.secondaryContainer else appColors.cameraControl
    Row(
        modifier = modifier
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
            .heightIn(min = PillMinHeight)
            .clip(pill)
            .background(background, pill)
            .border(Stroke.sticker, appColors.cameraOnControl, pill)
            .padding(start = Spacing.sm, end = Spacing.md, top = Spacing.sm, bottom = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(CircleSize)
                .background(circle, CircleShape)
                .border(Stroke.sticker, appColors.cameraOnControl, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (score != null) {
                Text(
                    text = score.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = appColors.cameraOnControl,
                )
            } else if (icon != null) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = appColors.cameraOnControl,
                    modifier = Modifier.size(IconSize),
                )
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = appColors.cameraOnControl,
        )
    }
}
