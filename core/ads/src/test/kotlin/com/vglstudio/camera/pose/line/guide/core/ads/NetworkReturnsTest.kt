package com.vglstudio.camera.pose.line.guide.core.ads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Usable Internet coming back, with changes within 3 s merged, on a virtual clock (S-24, L-A16). */
@OptIn(ExperimentalCoroutinesApi::class)
class NetworkReturnsTest {
    private val connected = MutableSharedFlow<Boolean>()

    /** The virtual times of every return. */
    private fun TestScope.collectReturns(): List<Long> {
        val returns = mutableListOf<Long>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            networkReturns(connected).collect { returns += testScheduler.currentTime }
        }
        return returns
    }

    /** Moves the virtual clock to [millis] after the start and runs what is due. */
    private fun TestScope.at(millis: Long) {
        advanceTimeBy(millis - testScheduler.currentTime)
        runCurrent()
    }

    private suspend fun TestScope.send(millis: Long, value: Boolean) {
        at(millis)
        connected.emit(value)
        runCurrent()
    }

    @Test
    fun offlineAtStart_thenOnline_returnsOnceAfter3s() = runTest {
        val returns = collectReturns()
        send(0, false)
        send(1_000, true)

        at(3_999)
        assertEquals(emptyList<Long>(), returns)
        at(4_000)
        assertEquals(listOf(4_000L), returns)
        at(20_000)
        assertEquals(listOf(4_000L), returns)
    }

    @Test
    fun flappingWithin3s_isOneReturn() = runTest {
        val returns = collectReturns()
        send(0, false)
        send(5_000, true)
        send(5_500, false)
        send(6_000, true)

        at(8_999)
        assertEquals(emptyList<Long>(), returns)
        at(30_000)
        assertEquals(listOf(9_000L), returns)
    }

    @Test
    fun shortDropUnder3s_isNoReturn() = runTest {
        val returns = collectReturns()
        send(0, true)
        send(5_000, false)
        send(6_000, true)

        at(20_000)
        assertEquals(emptyList<Long>(), returns)
    }

    @Test
    fun onlineAtStart_isNoReturn() = runTest {
        val returns = collectReturns()
        send(0, true)
        at(10_000)
        assertEquals(emptyList<Long>(), returns)

        // Control (L-055): a stable loss, then Internet back, is a return.
        send(10_000, false)
        send(20_000, true)
        at(22_999)
        assertEquals(emptyList<Long>(), returns)
        at(23_000)
        assertEquals(listOf(23_000L), returns)
    }
}
