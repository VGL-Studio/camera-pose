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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vglstudio.camera.pose.line.guide.core.designsystem.R
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Stroke

/** Test tag of the ring of a [PremiumBadge]; with testTagsAsResourceId, also its resource id for device scenarios. */
const val PREMIUM_BADGE_TAG = "pose_premium_badge"

/** Sizes of [PremiumBadge] (design: component PremiumBadge, Size=Card, Thumb, Inline). */
enum class PremiumBadgeSize(internal val ring: Dp, internal val icon: Dp) {
    /** Top left of the image of a PoseCard. */
    Card(32.dp, 20.dp),

    /** Over the top left corner of a 64 dp PoseThumb, mirroring SelectedBadge. */
    Thumb(22.dp, 14.dp),

    /** Before the meta line of the pose detail. */
    Inline(20.dp, 14.dp),
}

/**
 * The premium mark of a pose not unlocked yet (spec poses, KB-8): never blurs or covers the pose image.
 * [contentDescription] is null where the parent already says "premium" (a camera cell), so TalkBack does not
 * read it twice. It takes no tap: a tap on it lands on the card or cell under it.
 */
@Composable
fun PremiumBadge(size: PremiumBadgeSize, contentDescription: String?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(size.ring)
            .testTag(PREMIUM_BADGE_TAG)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                },
            )
            .background(colors.tertiaryContainer, CircleShape)
            .border(Stroke.sticker, colors.outline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_premium),
            contentDescription = null,
            tint = colors.onTertiaryContainer,
            modifier = Modifier.size(size.icon),
        )
    }
}
