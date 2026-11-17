package com.vglstudio.camera.pose.line.guide.core.analytics

/**
 * Tells a return of the whole process to foreground from the started activities (plan T-099, step 6): the count goes
 * from 0 to 1, except the first time of the process. A rotation or a second activity on top is not a return. Called
 * on the main thread only.
 */
internal class ForegroundCounter {
    /** Activities started and not stopped. */
    var started: Int = 0
        private set

    private var firstDone = false

    /** True when this start brings the process back to foreground. */
    fun onActivityStarted(): Boolean {
        started++
        if (started != 1) return false
        val isReturn = firstDone
        firstDone = true
        return isReturn
    }

    /** One activity less; never below 0. */
    fun onActivityStopped() {
        if (started > 0) started--
    }
}
