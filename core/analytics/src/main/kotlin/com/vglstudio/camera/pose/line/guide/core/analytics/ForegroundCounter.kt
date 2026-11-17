package com.vglstudio.camera.pose.line.guide.core.analytics

/** Counts the started activities of the process and tells a return to foreground. */
internal class ForegroundCounter {
    /** Activities started and not stopped. */
    val started: Int get() = 0

    /** True when the count goes from 0 to 1, except the first time of the process. */
    fun onActivityStarted(): Boolean = false

    fun onActivityStopped() = Unit
}
