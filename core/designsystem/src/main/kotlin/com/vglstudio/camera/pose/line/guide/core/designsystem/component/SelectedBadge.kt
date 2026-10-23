package com.vglstudio.camera.pose.line.guide.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.vglstudio.camera.pose.line.guide.core.designsystem.R
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Stroke

/** Test tag of the badge, so tests can tell whether a chip or a thumbnail shows it. */
const val SELECTED_BADGE_TAG = "selected_badge"

private val BadgeSize = 22.dp
private val CheckSize = 14.dp

/**
 * Yellow round check on the corner of the selected chip or thumbnail (sticker.md, section 4). Decorative only: the
 * selected state is in the semantics of the parent, so screen readers do not hear the badge.
 */
@Composable
fun SelectedBadge(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(BadgeSize)
            .testTag(SELECTED_BADGE_TAG)
            .background(colors.secondaryContainer, CircleShape)
            .border(Stroke.sticker, colors.outline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_check),
            contentDescription = null,
            tint = colors.onSecondaryContainer,
            modifier = Modifier.size(CheckSize),
        )
    }
}
