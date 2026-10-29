package com.vglstudio.camera.pose.line.guide.core.ads

/**
 * Counts the started activities, so switching between two activities of the app does not look like going to
 * background: the new activity's `onStart` runs before the old one's `onStop`. Main thread only.
 */
internal class StartedActivityCounter {
    private var started = 0

    /** Returns true only when this start brings the app to foreground (0 -> 1). */
    fun onStart(): Boolean {
        started++
        return started == 1
    }

    /** Returns true only when this stop sends the app to background (1 -> 0). Never goes below 0. */
    fun onStop(): Boolean {
        if (started == 0) return false
        started--
        return started == 0
    }
}
