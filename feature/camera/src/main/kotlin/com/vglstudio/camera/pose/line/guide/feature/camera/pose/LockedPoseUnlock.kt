package com.vglstudio.camera.pose.line.guide.feature.camera.pose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import com.vglstudio.camera.pose.line.guide.core.ads.AdPlacement
import com.vglstudio.camera.pose.line.guide.core.ads.RewardedAction
import com.vglstudio.camera.pose.line.guide.core.ads.RewardedAdDialog
import com.vglstudio.camera.pose.line.guide.core.ads.rememberRewardedAction
import com.vglstudio.camera.pose.line.guide.core.designsystem.R as DesignR
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PremiumUnlockContent

/** The unlock of a locked cell (spec poses, KB-8; camera KB-1, criterion 8): the rewarded action and the tap. */
@Stable
internal class LockedPoseUnlock(private val action: State<RewardedAction>, private val hold: (String) -> Unit) {
    val rewarded: RewardedAction get() = action.value

    /** Holds [id] in the view model first, so an action that runs at once (key off, consent closed) reads it. */
    fun select(id: String) {
        hold(id)
        action.value.start()
    }
}

/**
 * The unlock of the locked cells of [viewModel]. [rememberUpdatedState] keeps the newest [RewardedAction] for the
 * remembered PoseActions of the screen.
 */
@Composable
internal fun rememberLockedPoseUnlock(viewModel: PoseViewModel): LockedPoseUnlock {
    val action = rememberRewardedAction(AdPlacement.RW_PREMIUM_POSE, onGranted = viewModel::onUnlockGranted)
    val current = rememberUpdatedState(action)
    return remember(viewModel) { LockedPoseUnlock(current, viewModel::onLockedPoseSelect) }
}

/** The dialog of [unlock]; inside the dark theme of the camera. */
@Composable
internal fun LockedPoseUnlockDialog(unlock: LockedPoseUnlock, poseName: String?) {
    RewardedAdDialog(unlock.rewarded, title = stringResource(DesignR.string.premium_unlock_title)) {
        PremiumUnlockContent(poseName = poseName.orEmpty())
    }
}
