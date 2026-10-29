package com.vglstudio.camera.pose.line.guide.core.ads

/**
 * Finds a full-screen ad that is stuck (F-03): the activity that showed it was paused, then resumed, while the ad
 * still reads as showing. Activities are compared by identity hash. Main thread only.
 */
internal class StuckFullScreenRecovery {
    /** The host that was paused while its ad showed, until its next resume. */
    private var pausedHostId: Int? = null

    fun onActivityPaused(activityId: Int, showingHostId: Int?) {
        if (showingHostId != null && activityId == showingHostId) pausedHostId = activityId
    }

    /** True once when the host was paused and is resumed while an ad still shows; the signal is then used up. */
    fun onActivityResumed(activityId: Int, showingHostId: Int?): Boolean {
        if (pausedHostId != activityId) return false
        // Any resume of that activity uses the signal up, so a pause from an earlier ad never counts later.
        pausedHostId = null
        return showingHostId != null && activityId == showingHostId
    }
}
