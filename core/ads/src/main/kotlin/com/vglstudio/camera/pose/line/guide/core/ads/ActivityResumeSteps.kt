package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Activity

/** What :core:ads does when an app activity is resumed, in this order (G-06). */
internal class ActivityResumeSteps(
    private val recoverStuck: (Activity) -> Unit,
    private val regatherConsent: (Activity) -> Unit,
    private val showResumeAd: (Activity) -> Unit,
    private val releaseLeaveMark: () -> Unit,
) {
    /**
     * Releases a stuck full-screen flag (F-03) before gathering consent again (S-09), then shows the resume ad that
     * waits for this resume (T-025), then releases the leave mark (G-03), which the resume ad already read (A-14).
     */
    fun onResumed(activity: Activity) {
        recoverStuck(activity)
        regatherConsent(activity)
        showResumeAd(activity)
        releaseLeaveMark()
    }
}
