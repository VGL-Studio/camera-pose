package com.vglstudio.camera.pose.line.guide.core.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A return of the whole process to foreground (plan T-099, step 6): the started activities go from 0 to 1, but not
 * the first time of the process; a rotation or a second activity is not a return.
 */
class ForegroundCounterTest {
    private val counter = ForegroundCounter()

    @Test
    fun firstActivityOfProcess_isNotAReturn() {
        assertFalse(counter.onActivityStarted())
        assertEquals(1, counter.started)

        // Control: the process leaves and comes back: a return.
        counter.onActivityStopped()
        assertTrue(counter.onActivityStarted())
    }

    @Test
    fun backgroundThenForeground_countsOnce() {
        counter.onActivityStarted()
        counter.onActivityStopped()
        assertEquals(0, counter.started)

        assertTrue(counter.onActivityStarted())
        // A second activity on top is not another return.
        assertFalse(counter.onActivityStarted())
        assertEquals(2, counter.started)
    }

    @Test
    fun overlappingActivities_areNotAReturn() {
        counter.onActivityStarted()
        counter.onActivityStopped()
        assertTrue(counter.onActivityStarted())
        // A starts B: B starts before A stops.
        assertFalse(counter.onActivityStarted())
        counter.onActivityStopped()
        assertEquals(1, counter.started)

        // Control: B stops, then an activity starts: a return.
        counter.onActivityStopped()
        assertTrue(counter.onActivityStarted())
    }

    @Test
    fun extraStop_neverNegative() {
        counter.onActivityStarted()
        counter.onActivityStopped()
        counter.onActivityStopped()
        assertEquals(0, counter.started)

        assertTrue(counter.onActivityStarted())
        assertEquals(1, counter.started)
    }
}
