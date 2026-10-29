package com.vglstudio.camera.pose.line.guide.core.ads

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** F-03: a full-screen ad is stuck when its host was paused, then resumed, while the ad still shows. */
class StuckFullScreenRecoveryTest {
    private val recovery = StuckFullScreenRecovery()

    @Test
    fun pausedThenResumedHost_isStuckOnce() {
        recovery.onActivityPaused(1, showingHostId = 1)
        assertTrue(recovery.onActivityResumed(1, showingHostId = 1))
        // The signal is used up.
        assertFalse(recovery.onActivityResumed(1, showingHostId = 1))
    }

    @Test
    fun resumeWithoutPause_isNotStuck() {
        assertFalse(recovery.onActivityResumed(1, showingHostId = 1))
        // Control: with a pause first, the same resume is stuck.
        recovery.onActivityPaused(1, showingHostId = 1)
        assertTrue(recovery.onActivityResumed(1, showingHostId = 1))
    }

    @Test
    fun otherActivity_isNotStuck() {
        recovery.onActivityPaused(2, showingHostId = 1)
        assertFalse(recovery.onActivityResumed(2, showingHostId = 1))
        // Control: the host itself is stuck.
        recovery.onActivityPaused(1, showingHostId = 1)
        assertTrue(recovery.onActivityResumed(1, showingHostId = 1))
    }

    @Test
    fun notShowing_isNotStuck() {
        recovery.onActivityPaused(1, showingHostId = null)
        assertFalse(recovery.onActivityResumed(1, showingHostId = null))
        // Control: while an ad shows, the same pause and resume are stuck.
        recovery.onActivityPaused(1, showingHostId = 1)
        assertTrue(recovery.onActivityResumed(1, showingHostId = 1))
    }
}
