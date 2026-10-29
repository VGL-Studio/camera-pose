package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger

/**
 * The function that runs when one full-screen turn ends (N-02, N-04): exactly once, whether the end comes from the
 * SDK, stuck recovery, the forced completion of a destroyed host, or a turn that was blocked. Main thread only.
 */
internal class FullScreenCompletion(private val logger: AppLogger) {
    private var onFinished: (() -> Unit)? = null

    /** True while a function is set and has not run. */
    val pending: Boolean get() = onFinished != null

    /** Sets the function of a new turn. A function that has not run yet runs first, so none is ever lost. */
    fun set(onFinished: () -> Unit) {
        if (pending) complete("replaced")
        this.onFinished = onFinished
    }

    /** Runs the function once, with [reason] in the log; later calls do nothing. */
    fun complete(reason: String) {
        val finished = onFinished ?: return
        onFinished = null
        logger.info(TAG, "Full-screen turn finished: $reason")
        finished()
    }

    private companion object {
        const val TAG = "Ads"
    }
}
