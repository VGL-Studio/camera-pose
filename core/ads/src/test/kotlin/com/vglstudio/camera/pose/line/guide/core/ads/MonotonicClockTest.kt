package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.ads.AdIntervalGroup.Companion.DEFAULT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** AR-01: the clock of the ad rules follows the time since the device started, never the wall clock. */
class MonotonicClockTest {
    /** Two numbers set by the test. */
    private class FakeTimeSources(var elapsed: Long, var wall: Long) : TimeSources {
        override fun elapsedRealtimeMillis(): Long = elapsed

        override fun wallClockMillis(): Long = wall
    }

    private val sources = FakeTimeSources(elapsed = 10_000, wall = 1_700_000_000_000)
    private val clock = monotonicClockOf(sources)

    @Test
    fun followsElapsedTime_notWallClock() {
        assertEquals(10_000, clock.nowMillis())
        sources.wall -= HOUR
        sources.elapsed += 5_000
        assertEquals(15_000, clock.nowMillis())
    }

    @Test
    fun intervalDecision_ignoresWallClockJump() {
        val guard = FullScreenAdGuard()
        guard.record(DEFAULT, clock.nowMillis())
        sources.wall -= HOUR
        sources.elapsed += 10_000
        assertFalse(guard.allows(DEFAULT, 45_000, clock.nowMillis()))
        sources.elapsed = 55_000
        assertTrue(guard.allows(DEFAULT, 45_000, clock.nowMillis()))
    }

    private companion object {
        const val HOUR = 3_600_000L
    }
}
