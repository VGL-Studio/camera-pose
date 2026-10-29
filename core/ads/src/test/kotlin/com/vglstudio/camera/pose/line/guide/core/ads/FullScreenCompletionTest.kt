package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** N-02, N-04: the completion of a full-screen turn runs exactly once, whatever ends the turn. */
class FullScreenCompletionTest {
    private val completion = FullScreenCompletion(RecordingAppLogger())
    private var runs = 0

    @Test
    fun runsOnce_fromAnySource() {
        completion.set { runs++ }
        assertTrue(completion.pending)
        completion.complete("stuck_recovered")
        // The real callback of the SDK comes late.
        completion.complete("closed")
        assertEquals(1, runs)
        assertFalse(completion.pending)
    }

    @Test
    fun hostDestroyed_forcesCompletion() {
        completion.set { runs++ }
        completion.complete("host_destroyed")
        assertEquals(1, runs)
    }

    @Test
    fun blockedTurn_completesAtOnce() {
        completion.set { runs++ }
        completion.complete("busy")
        assertEquals(1, runs)
        // Nothing waits: no queue.
        assertFalse(completion.pending)
        completion.complete("closed")
        assertEquals(1, runs)
    }
}
