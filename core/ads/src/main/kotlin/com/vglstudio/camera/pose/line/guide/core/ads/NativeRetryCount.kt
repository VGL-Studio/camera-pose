package com.vglstudio.camera.pose.line.guide.core.ads

import kotlinx.coroutines.flow.StateFlow

/**
 * The failures in a row of one native place with no ad (a slot, a list row, a pool, the native full-screen ad of a
 * placement), for the retry waits of O-05 (plan T-103): back to 0 when an ad loads ([onLoaded]), on [clear], and when
 * [resets] moved since the last count (the whole process back in foreground, Internet back). Never cancels a wait in
 * progress: it only sets the wait of the next failure. Not thread-safe: one place, one thread.
 */
internal class NativeRetryCount(private val resets: StateFlow<Int>) {
    private var failures = 0
    private var since = resets.value

    /** The failures in a row, this one counted; 1 again when [resets] moved since the last count. */
    fun onFailure(): Int {
        val now = resets.value
        if (now != since) {
            since = now
            failures = 0
        }
        failures++
        return failures
    }

    /** An ad loaded: the next failure counts from 1. */
    fun onLoaded() = clear()

    /** The next failure counts from 1. */
    fun clear() {
        failures = 0
        since = resets.value
    }
}
