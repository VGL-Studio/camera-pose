package com.vglstudio.camera.pose.line.guide.core.ads

/**
 * KB-26, criterion 3 (F-08, plan T-055): the activity a full-screen ad of the SDK is about to be shown on is usable
 * only when it is not finishing, not destroyed and is the resumed activity of the app. "Resumed", not "started": an
 * activity still visible under a permission dialog or a share sheet does not take input. JBase checks the first two
 * only (`AdmobHelper.getSafeCurrentActivity`, ADR-0038); the third is the spec's.
 */
internal fun fullScreenHostUsable(finishing: Boolean, destroyed: Boolean, resumed: Boolean): Boolean =
    !finishing && !destroyed && resumed

/**
 * `FullScreenContentCallback` code "app not foreground" ([showFailReasonOf] gives `app_not_foreground`): the code of
 * the `ad_show_fail` that :core:ads sends itself when the activity is not usable (section 8.2). Kept here, beside
 * [fullScreenHostUsable], so the controller and [RewardedTurns] send the same code.
 */
internal const val SHOW_FAILED_APP_NOT_FOREGROUND = 3

/** What the destruction of an app activity does to the full-screen turn on screen (N-04, HD-03, plan T-055). */
internal enum class HostDestroyedAction {
    /** No turn with a host, or not its host. */
    IGNORE,

    /** The host is really destroyed: forced completion (N-04, HD-03). */
    FORCE_COMPLETE,

    /** The host is recreated for a configuration change (rotation): the turn is kept for the new activity. */
    HAND_OVER,
}

/**
 * KB-26, criterion 4: the activity [activityId] is destroyed while the turn on screen is shown on [hostId] (null when
 * no turn has a host). A destruction with `isChangingConfigurations` ([changingConfigurations]) is no loss of the host.
 */
internal fun hostDestroyedAction(activityId: Int, hostId: Int?, changingConfigurations: Boolean): HostDestroyedAction =
    when {
        hostId == null || hostId != activityId -> HostDestroyedAction.IGNORE
        changingConfigurations -> HostDestroyedAction.HAND_OVER
        else -> HostDestroyedAction.FORCE_COMPLETE
    }
