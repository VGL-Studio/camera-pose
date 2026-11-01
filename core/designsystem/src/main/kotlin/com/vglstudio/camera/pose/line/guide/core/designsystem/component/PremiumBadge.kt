package com.vglstudio.camera.pose.line.guide.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Test tag of the ring of a [PremiumBadge]. */
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
 * read it twice.
 */
@Composable
fun PremiumBadge(size: PremiumBadgeSize, contentDescription: String?, modifier: Modifier = Modifier) {
    // Empty frame of step 3 (plan T-086, section 4).
}
