package com.vglstudio.camera.pose.line.guide.core.ads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The one send gate of every native small request: foreground first, then the consent gate (L-072, S-15). */
@OptIn(ExperimentalCoroutinesApi::class)
class NativeSendGateTest {
    @Test
    fun foreground_openGate_returnsTrue() = runTest {
        var reads = 0
        assertTrue(awaitSendAllowed(MutableStateFlow(true)) { reads++ == 0 })
        assertEquals(1, reads)
    }

    @Test
    fun waitsForForeground_thenReadsGate() = runTest {
        val foreground = MutableStateFlow(false)
        var gate = true
        var reads = 0
        val result = async {
            awaitSendAllowed(foreground) {
                reads++
                gate
            }
        }
        runCurrent()
        assertFalse("returned while in background", result.isCompleted)
        assertEquals(0, reads)

        // The gate closes while in background: what counts is the gate when foreground comes.
        gate = false
        advanceTimeBy(60_000)
        runCurrent()
        assertFalse(result.isCompleted)

        foreground.value = true
        runCurrent()
        assertTrue(result.isCompleted)
        assertFalse(result.await())
        assertEquals(1, reads)
    }

    @Test
    fun gateClosed_returnsFalse() = runTest {
        // Control (L-055): the same call with an open gate says yes, so false below is the closed gate.
        assertTrue(awaitSendAllowed(MutableStateFlow(true)) { true })
        assertFalse(awaitSendAllowed(MutableStateFlow(true)) { false })
    }
}
