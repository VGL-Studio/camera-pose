package com.vglstudio.camera.pose.line.guide.core.ads

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The started activities of the whole process (A-10, plan T-025): what is a trip to background and back. */
class ProcessForegroundCounterTest {
    private val counter = ProcessForegroundCounter()

    @Test
    fun firstStart_isForeground() {
        assertTrue(counter.onStart())
        // Control (L-055): a second start while one is started is not a new foreground.
        assertFalse(counter.onStart())
    }

    @Test
    fun handOverBetweenActivities_reportsNothing() {
        assertTrue(counter.onStart())
        // The new activity starts before the old one stops.
        assertFalse(counter.onStart())
        assertFalse(counter.onStop(changingConfigurations = false))
        // Control: the last stop is a trip to background.
        assertTrue(counter.onStop(changingConfigurations = false))
    }

    @Test
    fun lastStop_isBackground() {
        counter.onStart()
        assertTrue(counter.onStop(changingConfigurations = false))
        // Control: the next start is a foreground again.
        assertTrue(counter.onStart())
    }

    @Test
    fun configChangeStopThenStart_reportsNothing() {
        counter.onStart()
        // Rotation: the old activity stops before the recreated one starts.
        assertFalse(counter.onStop(changingConfigurations = true))
        assertFalse(counter.onStart())
        // Control (L-055): the recreated activity is counted, so its real stop is a trip to background.
        assertTrue(counter.onStop(changingConfigurations = false))
    }

    @Test
    fun configChangeThenRealStop_reportsBackground() {
        counter.onStart()
        assertFalse(counter.onStop(changingConfigurations = true))
        assertFalse(counter.onStart())
        // The recreated activity goes to background for real.
        assertTrue(counter.onStop(changingConfigurations = false))
        // Control: the restarting flag is cleared, so the next start is a foreground.
        assertTrue(counter.onStart())
    }

    @Test
    fun stopWithoutStart_neverNegative() {
        assertFalse(counter.onStop(changingConfigurations = false))
        assertFalse(counter.onStop(changingConfigurations = true))
        // Control: the count did not go below 0, so one start is a foreground and one stop a background.
        assertTrue(counter.onStart())
        assertTrue(counter.onStop(changingConfigurations = false))
    }
}
