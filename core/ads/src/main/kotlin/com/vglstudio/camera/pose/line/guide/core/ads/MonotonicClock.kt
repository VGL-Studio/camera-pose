package com.vglstudio.camera.pose.line.guide.core.ads

import android.os.SystemClock

/**
 * Milliseconds of a clock that only moves forward: the time since the device started, never the wall clock, so a
 * user who changes the time of the device does not change any gap of the ad rules (AR-01).
 */
internal fun interface MonotonicClock {
    fun nowMillis(): Long
}

/** The two time sources of the device, passed in so that tests can move each one on its own. */
internal interface TimeSources {
    /** Time since the device started. */
    fun elapsedRealtimeMillis(): Long

    /** Wall clock, which the user can change. Not read by [monotonicClockOf]. */
    fun wallClockMillis(): Long
}

/** The time sources of the device. */
internal object AndroidTimeSources : TimeSources {
    override fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()

    override fun wallClockMillis(): Long = System.currentTimeMillis()
}

/** A monotonic clock that reads the time since the device started of [sources] (AR-01). */
internal fun monotonicClockOf(sources: TimeSources): MonotonicClock = MonotonicClock { sources.elapsedRealtimeMillis() }

/** The monotonic clock of the device, used by every ad rule. */
internal object SystemMonotonicClock : MonotonicClock by monotonicClockOf(AndroidTimeSources)
