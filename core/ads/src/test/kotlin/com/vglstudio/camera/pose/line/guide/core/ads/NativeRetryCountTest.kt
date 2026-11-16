package com.vglstudio.camera.pose.line.guide.core.ads

import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

/** The failures in a row of one native place with no ad (O-05, plan T-103). */
class NativeRetryCountTest {
    private val resets = MutableStateFlow(0)
    private val count = NativeRetryCount(resets)

    @Test
    fun counts_1_2_3() {
        assertEquals(listOf(1, 2, 3), List(3) { count.onFailure() })
    }

    @Test
    fun loaded_countsFrom1Again() {
        repeat(3) { count.onFailure() }
        count.onLoaded()

        assertEquals(listOf(1, 2), List(2) { count.onFailure() })
    }

    @Test
    fun clear_countsFrom1Again() {
        repeat(3) { count.onFailure() }
        count.clear()

        assertEquals(listOf(1, 2), List(2) { count.onFailure() })
    }

    @Test
    fun resetsMoved_countsFrom1Again_once() {
        // Control (L-055): with the signal still, the fourth failure is the fourth.
        val still = NativeRetryCount(MutableStateFlow(0))
        repeat(3) { still.onFailure() }
        assertEquals(4, still.onFailure())

        repeat(3) { count.onFailure() }
        resets.value += 1

        assertEquals(listOf(1, 2, 3), List(3) { count.onFailure() })
    }

    @Test
    fun resetsMovedTwice_stillOne() {
        repeat(3) { count.onFailure() }
        resets.value += 1
        resets.value += 1

        assertEquals(1, count.onFailure())
        assertEquals(2, count.onFailure())
    }
}
