package com.vglstudio.camera.pose.line.guide.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vglstudio.camera.pose.line.guide.core.designsystem.R
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Radius
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Stroke
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.appColors

// Narrower than this, the trailing part goes to a second row (plan T-014, question 10).
private val OneRowMinWidth = 300.dp
private val ArrowWidth = 40.dp
private val ArrowHeight = 48.dp
private val ArrowIconSize = 24.dp
private const val DISABLED_ALPHA = 0.38f

/**
 * Step card on the camera (sticker.md, section 4): an optional step [badge] ("2/4"), the step [text] and a [trailing]
 * part, usually [StepCardArrows]. From 300 dp wide everything is on one row; narrower, [trailing] goes to a second row,
 * aligned to the end, so the text keeps room on a short phone.
 */
@Composable
fun StepCard(
    text: String,
    modifier: Modifier = Modifier,
    badge: String? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val appColors = MaterialTheme.appColors
    val shape = RoundedCornerShape(Radius.card)
    BoxWithConstraints(
        modifier = modifier
            .background(appColors.cameraControl, shape)
            .border(Stroke.sticker, appColors.cameraOnControl, shape)
            .padding(start = Spacing.md, top = Spacing.sm, end = Spacing.xs, bottom = Spacing.sm),
    ) {
        val oneRow = maxWidth + Spacing.md + Spacing.xs >= OneRowMinWidth
        if (oneRow) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                StepContent(text = text, badge = badge)
                trailing()
            }
        } else {
            Column {
                Row(
                    modifier = Modifier.padding(end = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    StepContent(text = text, badge = badge)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.End),
                    content = trailing,
                )
            }
        }
    }
}

@Composable
private fun RowScope.StepContent(text: String, badge: String?) {
    val colors = MaterialTheme.colorScheme
    val appColors = MaterialTheme.appColors
    if (badge != null) {
        val pill = RoundedCornerShape(Radius.full)
        Box(
            modifier = Modifier
                .background(colors.secondaryContainer, pill)
                .border(Stroke.sticker, appColors.cameraOnControl, pill)
                .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        ) {
            Text(text = badge, style = MaterialTheme.typography.labelLarge, color = colors.onSecondaryContainer)
        }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = appColors.cameraOnControl,
        modifier = Modifier.weight(1f),
    )
}

/** Previous and next arrows of a [StepCard], each with a 40 x 48 touch area. Disabled: faded and not clickable. */
@Composable
fun StepCardArrows(
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    previousEnabled: Boolean,
    nextEnabled: Boolean,
    previousDescription: String,
    nextDescription: String,
) {
    StepArrow(R.drawable.ic_chevron_left, previousDescription, previousEnabled, onPrevious)
    StepArrow(R.drawable.ic_chevron_right, nextDescription, nextEnabled, onNext)
}

@Composable
private fun StepArrow(icon: Int, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(ArrowWidth, ArrowHeight)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .semantics { contentDescription = description }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.appColors.cameraOnControl,
            modifier = Modifier.size(ArrowIconSize),
        )
    }
}
