package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * One cell of a pool on a virtual clock (plan T-028, P-06): claims every 500 ms, every 2 s once the pool is exhausted
 * (Unavailable), shows a late ad, gives its ad back when it leaves; and the opportunity of the cell (P-07).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NativePoolCellTest {
    private val logger = RecordingAppLogger()

    /** A pool for one cell: hands [next] once when set, records every claim time and every release. */
    private class FakePool(private val now: () -> Long) : PooledNativeAds {
        var next: LoadedNativeAd? = null
        override var exhausted = false
        val claimTimes = mutableListOf<Long>()
        val released = mutableListOf<LoadedNativeAd>()

        override fun claim(holder: AdPlacement): LoadedNativeAd? {
            claimTimes += now()
            return next.also { next = null }
        }

        override fun release(ad: LoadedNativeAd) {
            released += ad
        }
    }

    private fun TestScope.collect(pool: FakePool): Pair<MutableList<NativeAdState>, Job> {
        val states = mutableListOf<NativeAdState>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            pooledCellStates(pool, AdPlacement.GALLERY_NATIVE_MEDIUM, logger, "gallery_native_medium").collect {
                states += it
            }
        }
        return states to job
    }

    private fun TestScope.at(millis: Long) {
        advanceTimeBy(millis - testScheduler.currentTime)
        runCurrent()
    }

    @Test
    fun polls500ms_whileNotExhausted() = runTest {
        val pool = FakePool { testScheduler.currentTime }
        val (states, _) = collect(pool)
        at(1_200)

        assertEquals(listOf(0L, 500L, 1_000L), pool.claimTimes)
        assertEquals(listOf<NativeAdState>(NativeAdState.Loading), states)
    }

    @Test
    fun exhausted_emitsUnavailable_polls2s() = runTest {
        val pool = FakePool { testScheduler.currentTime }
        pool.exhausted = true
        val (states, _) = collect(pool)
        at(4_500)

        assertEquals(listOf(0L, 2_000L, 4_000L), pool.claimTimes)
        assertEquals(listOf(NativeAdState.Loading, NativeAdState.Unavailable), states)
    }

    @Test
    fun exhausted_thenLateAd_emitsLoaded() = runTest {
        val pool = FakePool { testScheduler.currentTime }
        pool.exhausted = true
        val (states, _) = collect(pool)
        at(1_000)
        val ad = FakeLoadedNativeAd()
        pool.next = ad
        pool.exhausted = false
        at(2_000)

        assertEquals(listOf(NativeAdState.Loading, NativeAdState.Unavailable, NativeAdState.Loaded(ad)), states)
        at(10_000)
        assertEquals(listOf(0L, 2_000L), pool.claimTimes)
    }

    @Test
    fun cancel_releasesAd_once() = runTest {
        val pool = FakePool { testScheduler.currentTime }
        val ad = FakeLoadedNativeAd()
        pool.next = ad
        val (states, job) = collect(pool)
        assertEquals(NativeAdState.Loaded(ad), states.last())
        assertTrue(pool.released.isEmpty())

        job.cancel()
        runCurrent()

        assertEquals(listOf<LoadedNativeAd>(ad), pool.released)
        assertEquals(0, ad.destroyCount)
    }

    @Test
    fun opportunity_shownOnceWhenAdAndViewed() {
        val results = mutableListOf<String>()
        val cell = PoolCellOpportunity { results += it }
        cell.onState(hasAd = true, viewed = false)
        assertEquals(emptyList<String>(), results)

        cell.onState(hasAd = true, viewed = true)
        cell.onState(hasAd = true, viewed = true)
        cell.onLeave()

        assertEquals(listOf("shown"), results)
    }

    @Test
    fun opportunity_notReady_whenLeftViewedWithoutAd() {
        val results = mutableListOf<String>()
        val cell = PoolCellOpportunity { results += it }
        cell.onState(hasAd = false, viewed = true)
        assertEquals(emptyList<String>(), results)

        cell.onLeave()

        assertEquals(listOf("not_ready"), results)
    }

    @Test
    fun opportunity_nothing_whenNeverViewed() {
        val results = mutableListOf<String>()
        val cell = PoolCellOpportunity { results += it }
        // A page built next to the one on screen, with its ad, then left without being viewed.
        cell.onState(hasAd = false, viewed = false)
        cell.onState(hasAd = true, viewed = false)
        cell.onLeave()

        assertEquals(emptyList<String>(), results)
    }

    @Test
    fun opportunity_shownNotNotReady_whenAdCameWhileViewed() {
        val results = mutableListOf<String>()
        val cell = PoolCellOpportunity { results += it }
        cell.onState(hasAd = false, viewed = true)
        cell.onState(hasAd = true, viewed = true)
        cell.onLeave()

        assertEquals(listOf("shown"), results)
    }
}
