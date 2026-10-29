package com.vglstudio.camera.pose.line.guide.core.ads

/**
 * Counts the started activities of the whole process, the ads SDK's included (A-10, plan T-025), so a full-screen ad
 * covering the app is not a trip to background. A stop for a configuration change (rotation, dark mode) and the start
 * of the recreated activity report nothing. Main thread only.
 */
internal class ProcessForegroundCounter {
    private var started = 0

    /** Set when the last started activity stopped for a configuration change: its recreated self starts next. */
    private var restarting = false

    /** True only when this start brings the process to foreground (0 -> 1), not when it restarts a recreated activity. */
    fun onStart(): Boolean {
        started++
        if (started != 1) return false
        if (restarting) {
            restarting = false
            return false
        }
        return true
    }

    /** True only when this stop sends the process to background (1 -> 0) and is not for a configuration change. */
    fun onStop(changingConfigurations: Boolean): Boolean {
        if (started == 0) return false
        started--
        if (started != 0) return false
        if (changingConfigurations) {
            restarting = true
            return false
        }
        return true
    }
}
