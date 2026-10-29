package com.vglstudio.camera.pose.line.guide.core.ads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The native full page of a flow step on a virtual clock (plan T-032): each entry takes its own preloaded ad from the
 * slot, a page with no ad asks for a load and shows an ad that comes later (P-06 pace), leaving gives the ad back, and
 * a closed consent gate takes nothing (S-11).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FullScreenStepStatesTest {
    /** The full-screen slot of the placement: [next] is taken once; records takes, loads asked and releases. */
    private class FakeSlot(private val now: () -> Long) {
        var next: LoadedNativeAd? = null
        val takeTimes = mutableListOf<Long>()
        var loadsAsked = 0
        val released = mutableListOf<LoadedNativeAd>()

        fun take(): LoadedNativeAd? {
            takeTimes += now()
            return next.also { next = null }
        }
    }

    private fun TestScope.collect(slot: FakeSlot): Pair<MutableList<NativeAdState>, Job> {
        val states = mutableListOf<NativeAdState>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            fullScreenStepStates(
                take = slot::take,
                ensureLoading = { slot.loadsAsked++ },
                release = { slot.released += it },
            ).collect { states += it }
        }
        return states to job
    }

    private fun TestScope.at(millis: Long) {
        advanceTimeBy(millis - testScheduler.currentTime)
        runCurrent()
    }

    @Test
    fun eachEntry_takesItsOwnAd() = runTest {
        val slot = FakeSlot { testScheduler.currentTime }
        val first = FakeLoadedNativeAd()
        slot.next = first
        val (firstStates, firstJob) = collect(slot)
        assertEquals(NativeAdState.Loaded(first), firstStates.last())

        // Leaving the page gives the ad back before the next page takes one.
        firstJob.cancel()
        runCurrent()
        assertEquals(listOf<LoadedNativeAd>(first), slot.released)

        val second = FakeLoadedNativeAd()
        slot.next = second
        val (secondStates, _) = collect(slot)

        assertEquals(NativeAdState.Loaded(second), secondStates.last())
        assertNotSame(first, second)
        assertEquals(2, slot.takeTimes.size)
        assertEquals(0, slot.loadsAsked)
    }

    @Test
    fun notReady_ensuresLoading_thenLateAdShows() = runTest {
        val slot = FakeSlot { testScheduler.currentTime }
        val (states, _) = collect(slot)
        assertEquals(1, slot.loadsAsked)
        assertEquals(listOf<NativeAdState>(NativeAdState.Unavailable), states.filter { it != NativeAdState.Loading })

        val ad = FakeLoadedNativeAd()
        slot.next = ad
        at(500)

        assertEquals(NativeAdState.Loaded(ad), states.last())
        assertEquals(listOf(0L, 500L), slot.takeTimes)
        assertEquals("a load is asked only while the page has no ad", 1, slot.loadsAsked)
        assertEquals("Unavailable once", 1, states.count { it == NativeAdState.Unavailable })
        at(10_000)
        assertEquals("no take once the page has its ad", 2, slot.takeTimes.size)
    }

    @Test
    fun notReady_retriesEvery500ms() = runTest {
        val slot = FakeSlot { testScheduler.currentTime }
        collect(slot)
        at(1_200)
        assertEquals(listOf(0L, 500L, 1_000L), slot.takeTimes)
    }

    @Test
    fun cancel_releasesAd() = runTest {
        val slot = FakeSlot { testScheduler.currentTime }
        val ad = FakeLoadedNativeAd()
        slot.next = ad
        val (_, job) = collect(slot)
        assertTrue(slot.released.isEmpty())

        job.cancel()
        runCurrent()

        assertEquals(listOf<LoadedNativeAd>(ad), slot.released)
    }

    @Test
    fun cancel_withoutAd_releasesNothing() = runTest {
        val slot = FakeSlot { testScheduler.currentTime }
        val (_, job) = collect(slot)
        at(700)
        job.cancel()
        runCurrent()
        assertTrue(slot.released.isEmpty())
    }

    @Test
    fun takeGate_closed_returnsNull() {
        val ad = FakeLoadedNativeAd()
        var takes = 0
        assertNull(stepFullScreenTake(canRequestAds = false) { takes++.let { ad } })
        assertEquals("a closed gate never takes the ad out of the slot", 0, takes)

        // Control: an open gate takes it.
        assertSame(ad, stepFullScreenTake(canRequestAds = true) { takes++.let { ad } })
        assertEquals(1, takes)
    }
}
