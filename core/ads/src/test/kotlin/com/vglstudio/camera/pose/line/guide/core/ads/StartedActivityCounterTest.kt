package com.vglstudio.camera.pose.line.guide.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartedActivityCounterTest {
    private val counter = StartedActivityCounter()

    @Test
    fun firstStart_isForeground() {
        assertTrue(counter.onStart())
    }

    @Test
    fun secondStart_isNotForeground() {
        // Control: the first start is the foreground one.
        assertTrue(counter.onStart())
        assertFalse(counter.onStart())
    }

    @Test
    fun lastStop_isBackground() {
        counter.onStart()
        assertTrue(counter.onStop())
    }

    @Test
    fun stopWithoutStart_staysAtZero() {
        assertFalse(counter.onStop())
        assertTrue(counter.onStart())
    }

    @Test
    fun switchingActivities_doesNotStartNewSession() {
        // A starts, B starts before A stops, A stops, then B stops: one foreground, one background, in that order.
        val events = listOf(
            counter.onStart(),
            counter.onStart(),
            counter.onStop(),
            counter.onStop(),
        )
        assertEquals(listOf(true, false, false, true), events)
    }
}
