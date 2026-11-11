package com.vglstudio.camera.pose.line.guide.core.ads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The reload interval of a slot (KB-34, plan T-087): the rule of a remote value ([reloadSeconds]) and the wait for one
 * tick ([awaitReloadTick]) on a virtual clock: a new value applies from the next count, 0 to positive counts from the
 * receipt, positive to 0 drops the count under way.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReloadIntervalTest {
    private val interval = MutableStateFlow(60_000L)

    /** Waits [count] ticks in a row, the first one with [extraMillis]; returns the times each tick was reached. */
    private fun TestScope.ticks(count: Int = 1, extraMillis: Long = 0): List<Long> {
        val done = mutableListOf<Long>()
        backgroundScope.launch {
            repeat(count) { index ->
                awaitReloadTick(interval, if (index == 0) extraMillis else 0)
                done += testScheduler.currentTime
            }
        }
        runCurrent()
        return done
    }

    private fun TestScope.at(millis: Long) {
        advanceTimeBy(millis - testScheduler.currentTime)
        runCurrent()
    }

    private fun TestScope.set(millis: Long) {
        interval.value = millis
        runCurrent()
    }

    @Test
    fun reloadSeconds_rules() {
        mapOf(
            (0L to 60L) to 0L,
            (-1L to 60L) to 60L,
            (-1L to 30L) to 30L,
            (1L to 60L) to 30L,
            (29L to 60L) to 30L,
            (30L to 60L) to 30L,
            (45L to 60L) to 45L,
            (600L to 30L) to 600L,
        ).forEach { (input, expected) ->
            val (raw, default) = input
            assertEquals("reloadSeconds($raw, $default)", expected, reloadSeconds(raw, default))
        }
        assertEquals(30L, MIN_RELOAD_SECONDS)
    }

    @Test
    fun positiveChangedMidCount_currentTickKept_nextUsesNew() = runTest {
        val done = ticks(count = 2)
        at(20_000)
        set(120_000)
        at(59_999)
        assertEquals(emptyList<Long>(), done)
        at(60_000)
        assertEquals(listOf(60_000L), done)
        at(179_999)
        assertEquals(listOf(60_000L), done)
        at(180_000)
        assertEquals(listOf(60_000L, 180_000L), done)
    }

    @Test
    fun zeroToPositive_countsFromReceipt() = runTest {
        interval.value = 0
        val done = ticks()
        at(300_000)
        assertEquals(emptyList<Long>(), done)
        set(60_000)
        at(359_999)
        assertEquals(emptyList<Long>(), done)
        at(360_000)
        assertEquals(listOf(360_000L), done)
    }

    @Test
    fun positiveToZero_dropsCurrentCount() = runTest {
        val done = ticks()
        at(30_000)
        set(0)
        at(60_000)
        assertEquals(emptyList<Long>(), done)
        at(200_000)
        assertEquals(emptyList<Long>(), done)
        set(60_000)
        at(259_999)
        assertEquals(emptyList<Long>(), done)
        at(260_000)
        assertEquals(listOf(260_000L), done)
    }

    @Test
    fun extraMillis_addsToCount() = runTest {
        val done = ticks(extraMillis = 500)
        at(60_499)
        assertEquals(emptyList<Long>(), done)
        at(60_500)
        assertEquals(listOf(60_500L), done)
    }
}
