package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The in-place refresh of a native small slot on a virtual clock: every 60 s in foreground, a tick in background is
 * skipped (CL-02), the claim waits at most 10 s, failures keep the old ad and retry by O-05 with an ad held, the
 * send gate of paths (e) and (f) (L-072), and who destroys which ad (plan T-040).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NativeAdRefreshTest {
    private val loader = FakeNativeAdLoader()
    private val logger = RecordingAppLogger()
    private val foreground = MutableStateFlow(true)
    private val retryResets = MutableStateFlow(0)
    private var mayRequest = true
    private lateinit var loads: FakeNativeLoads
    private lateinit var source: NativeAdSource
    private val ad0 = FakeLoadedNativeAd()

    /** The destroy count of [ad0] when each state was received, to check the new ad is shown first. */
    private val ad0DestroyedAtEmit = mutableListOf<Int>()

    private fun TestScope.newSource(load: (suspend (NativeLoadUse) -> NativeAdState)? = null): NativeAdSource {
        loads = FakeNativeLoads(loader, { testScheduler.currentTime }, logger)
        source = NativeAdSource(
            load = load ?: { use -> loads.load(use) },
            sendGate = { awaitSendAllowed(foreground) { mayRequest } },
            clock = { testScheduler.currentTime },
            scope = backgroundScope,
            logger = logger,
            placementKey = KEY,
        )
        return source
    }

    /** The slot as AdsController wires it: attached while collected, detached when the collection ends. */
    private fun TestScope.collectRefresh(
        firstTickDelayMillis: Long = NativeAdPolicy.REFRESH_INTERVAL_MILLIS,
        load: (suspend (NativeLoadUse) -> NativeAdState)? = null,
    ): Pair<MutableList<NativeAdState>, Job> {
        if (!::source.isInitialized) newSource(load)
        val states = mutableListOf<NativeAdState>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            flow {
                source.attach()
                try {
                    emitAll(
                        refreshingNativeAdStates(
                            NativeAdState.Loaded(ad0),
                            source,
                            foreground,
                            { mayRequest },
                            retryResets,
                            { testScheduler.currentTime },
                            logger,
                            KEY,
                            firstTickDelayMillis,
                        ),
                    )
                } finally {
                    source.detach()
                }
            }.collect {
                ad0DestroyedAtEmit += ad0.destroyCount
                states += it
            }
        }
        runCurrent()
        return states to job
    }

    private fun TestScope.at(millis: Long) {
        advanceTimeBy(millis - testScheduler.currentTime)
        runCurrent()
    }

    private fun TestScope.deliver(ad: FakeLoadedNativeAd = FakeLoadedNativeAd()): FakeLoadedNativeAd {
        loader.deliver(ad)
        runCurrent()
        return ad
    }

    private fun TestScope.fail(code: Int = 3) {
        loader.fail(code)
        runCurrent()
    }

    private fun List<NativeAdState>.lastAd(): LoadedNativeAd = (last() as NativeAdState.Loaded).ad

    private fun failureLogs() = logger.entries.map { it.message }.filter { it.contains("refresh: failed") }

    @Test
    fun tickAt60s_claimsNewAd_emitsIt_thenDestroysOld() = runTest {
        val (states, _) = collectRefresh()
        assertSame(ad0, states.single().let { (it as NativeAdState.Loaded).ad })

        at(60_000)
        assertEquals(listOf(60_000L), loads.times)
        assertEquals(NativeLoadUse.REFRESH, loads.uses.single())
        val ad1 = deliver()

        assertSame(ad1, states.lastAd())
        assertEquals(2, states.size)
        // The new ad was emitted while the old one was still alive, then the old one was destroyed once.
        assertEquals(listOf(0, 0), ad0DestroyedAtEmit)
        assertEquals(1, ad0.destroyCount)
        assertEquals(0, ad1.destroyCount)
        assertTrue(logger.entries.any { it.message == "Native $KEY refresh: loaded" })
    }

    @Test
    fun at59s_noClaim() = runTest {
        collectRefresh()
        at(59_999)
        assertTrue(loads.sent.isEmpty())

        // Control (L-055): one millisecond later the tick claims.
        at(60_000)
        assertEquals(listOf(60_000L), loads.times)
    }

    @Test
    fun nextTick_countsFromLastSwap() = runTest {
        val (states, _) = collectRefresh()
        at(60_000)
        at(65_000)
        val ad1 = deliver()
        // The next load after the claim brings the pending ad of the next tick.
        val ad2 = deliver()
        assertSame(ad2, source.pending)

        at(124_999)
        assertSame(ad1, states.lastAd())
        at(125_000)
        assertSame(ad2, states.lastAd())
    }

    @Test
    fun firstTickDelay_appliesToFirstTickOnly() = runTest {
        val (states, _) = collectRefresh(firstTickDelayMillis = 30_000)
        at(29_999)
        assertTrue(loads.sent.isEmpty())
        at(30_000)
        assertEquals(listOf(30_000L), loads.times)
        val ad1 = deliver()
        val ad2 = deliver()

        at(60_000)
        assertSame(ad1, states.lastAd())
        at(89_999)
        assertSame(ad1, states.lastAd())
        at(90_000)
        assertSame(ad2, states.lastAd())
    }

    @Test
    fun tickInBackground_isSkipped_nextTickAt120s() = runTest {
        collectRefresh()
        at(30_000)
        foreground.value = false
        at(60_000)
        assertTrue(loads.sent.isEmpty())
        assertTrue(logger.entries.any { it.message == "Native $KEY refresh: skipped (background)" })

        at(90_000)
        foreground.value = true
        runCurrent()
        assertTrue("a refresh ran when back in foreground", loads.sent.isEmpty())
        at(119_999)
        assertTrue(loads.sent.isEmpty())
        at(120_000)
        assertEquals(listOf(120_000L), loads.times)
    }

    @Test
    fun backToForeground_noImmediateRefresh() = runTest {
        collectRefresh()
        at(30_000)
        foreground.value = false
        at(61_000)
        foreground.value = true
        runCurrent()
        at(62_000)
        assertTrue(loads.sent.isEmpty())

        // Control (L-055): the tick after the skipped one sends.
        at(120_000)
        assertEquals(listOf(120_000L), loads.times)
    }

    @Test
    fun claimWaitsAtMost10s_thenKeepsOld() = runTest {
        val (states, _) = collectRefresh()
        at(60_000)
        at(69_999)
        assertTrue(failureLogs().isEmpty())

        at(70_000)

        assertEquals(listOf("Native $KEY refresh: failed, retry in 2000 ms (failures 1)"), failureLogs())
        assertEquals(1, states.size)
        assertEquals(0, ad0.destroyCount)
    }

    @Test
    fun refreshFails_keepsOldAd_retriesAt2s4s8s() = runTest {
        val (states, _) = collectRefresh()
        at(60_000)
        fail()
        at(62_000)
        fail()
        at(66_000)
        fail()
        at(73_999)
        assertEquals(listOf(60_000L, 62_000L, 66_000L), loads.times)
        at(74_000)

        assertEquals(listOf(60_000L, 62_000L, 66_000L, 74_000L), loads.times)
        assertEquals(1, states.size)
        assertEquals(0, ad0.destroyCount)
        assertEquals(
            listOf(
                "Native $KEY refresh: failed, retry in 2000 ms (failures 1)",
                "Native $KEY refresh: failed, retry in 4000 ms (failures 2)",
                "Native $KEY refresh: failed, retry in 8000 ms (failures 3)",
            ),
            failureLogs(),
        )
    }

    @Test
    fun refreshRetry_cutShortByRetryReset() = runTest {
        collectRefresh()
        at(60_000)
        fail()
        at(62_000)
        fail()
        at(66_000)
        fail()
        at(68_000)
        assertEquals(3, loads.sent.size)

        retryResets.value += 1
        runCurrent()

        assertEquals(listOf(60_000L, 62_000L, 66_000L, 68_000L), loads.times)
    }

    @Test
    fun refreshSucceeds_afterFailures_resetsCounter() = runTest {
        val (states, _) = collectRefresh()
        at(60_000)
        fail()
        at(62_000)
        fail()
        at(66_000)
        val ad1 = deliver()
        assertSame(ad1, states.lastAd())
        // The next load after the claim fails: no pending ad for the next tick.
        fail()

        at(126_000)
        assertEquals(126_000L, loads.times.last())
        fail()
        at(128_000)

        assertEquals(128_000L, loads.times.last())
        assertEquals("Native $KEY refresh: failed, retry in 2000 ms (failures 1)", failureLogs().last())
    }

    @Test
    fun loaderThrows_countsAsCode0_keepsAd() = runTest {
        val events = mutableListOf<NativeAdEvents>()
        var builds = 0
        val buildTimes = mutableListOf<Long>()
        val (states, _) = collectRefresh(
            load = { _ ->
                nativeLoadOnce(
                    newEvents = { mockk<NativeAdEvents>(relaxed = true).also { events += it } },
                    newLoader = {
                        builds++
                        buildTimes += testScheduler.currentTime
                        if (builds == 1) throw IllegalStateException("sdk torn down")
                        loader
                    },
                    unitId = FakeNativeLoads.FAKE_UNIT_ID,
                    size = NativeAdSize.SMALL,
                    timeoutMillis = 30_000,
                    logger = logger,
                    placementKey = KEY,
                )()
            },
        )
        at(60_000)

        verify(exactly = 1) { events.first().onFailed(0, null) }
        assertEquals(1, states.size)
        assertEquals(0, ad0.destroyCount)
        assertEquals(listOf("Native $KEY refresh: failed, retry in 2000 ms (failures 1)"), failureLogs())
        at(62_000)
        assertEquals(listOf(60_000L, 62_000L), buildTimes)
        assertEquals(1, loader.requests.size)
    }

    @Test
    fun lateAdAfter10s_becomesPending_takenByRetry() = runTest {
        val (states, _) = collectRefresh()
        at(60_000)
        at(70_000)
        assertEquals(1, failureLogs().size)

        at(71_000)
        val late = deliver()
        assertSame(late, source.pending)
        assertEquals(1, states.size)

        at(72_000)
        assertSame(late, states.lastAd())
        assertEquals(0, late.destroyCount)
        assertEquals(1, ad0.destroyCount)
    }

    @Test
    fun adArrivesAtTimeoutInstant_notLeaked() = runTest {
        val (states, _) = collectRefresh()
        at(60_000)
        advanceTimeBy(10_000)
        assertEquals(70_000L, testScheduler.currentTime)
        val ad = FakeLoadedNativeAd()
        loader.deliver(ad)
        runCurrent()

        val shown = states.lastOrNull().let { it is NativeAdState.Loaded && it.ad === ad }
        val pending = source.pending === ad
        val destroyed = ad.destroyCount == 1
        assertTrue("ad lost: not shown, not pending, not destroyed", shown || pending || destroyed)
        assertFalse("shown and destroyed", shown && ad.destroyCount > 0)
        assertTrue(ad.destroyCount <= 1)
        // Whichever way it went, the old ad is still accounted for: shown, or destroyed once after a swap.
        assertEquals(if (shown) 1 else 0, ad0.destroyCount)
    }

    @Test
    fun cancelledBetweenClaimAndEmit_destroysNewAd() = runTest {
        lateinit var job: Job
        val ad1 = FakeLoadedNativeAd()
        newSource(
            load = { _ ->
                // The claim returns ad1 right as the slot leaves the screen.
                job.cancel()
                NativeAdState.Loaded(ad1)
            },
        )
        val (states, collection) = collectRefresh()
        job = collection

        at(60_000)

        assertTrue(collection.isCancelled)
        assertEquals(1, states.size)
        assertEquals(1, ad1.destroyCount)
        assertEquals(1, ad0.destroyCount)
    }

    @Test
    fun cancelled_destroysShownAd() = runTest {
        val (_, job) = collectRefresh()
        at(60_000)
        val ad1 = deliver()
        val ad2 = deliver()
        assertEquals(0, ad1.destroyCount)

        job.cancel()
        runCurrent()

        assertEquals(1, ad1.destroyCount)
        assertEquals(1, ad0.destroyCount)
        // The pending ad goes with the source once the last slot is gone.
        assertEquals(1, ad2.destroyCount)
    }

    @Test
    fun tick_gateClosed_sendsNothing_keepsAd_noFailureCounted() = runTest {
        val (states, _) = collectRefresh()
        at(59_000)
        mayRequest = false
        at(60_000)
        assertTrue(loads.sent.isEmpty())
        assertTrue(logger.entries.any { it.message == "Native $KEY refresh: blocked" })

        at(61_000)
        mayRequest = true
        at(62_000)
        assertTrue("a failure retry was planned", loads.sent.isEmpty())
        assertTrue(failureLogs().isEmpty())
        assertEquals(1, states.size)

        // Control (L-055): with the gate open the next tick sends.
        at(120_000)
        assertEquals(listOf(120_000L), loads.times)
    }

    @Test
    fun refreshRetry_inBackground_waitsForForeground() = runTest {
        collectRefresh()
        at(60_000)
        fail()
        at(61_000)
        foreground.value = false
        at(62_000)
        at(89_000)
        assertEquals(1, loads.sent.size)

        foreground.value = true
        runCurrent()

        assertEquals(listOf(60_000L, 89_000L), loads.times)
    }

    @Test
    fun refreshRetry_gateClosed_sendsNothing_keepsAd_backTo60sTick() = runTest {
        val (states, _) = collectRefresh()
        at(60_000)
        fail()
        at(61_000)
        mayRequest = false
        at(62_000)
        assertEquals(1, loads.sent.size)
        assertTrue(logger.entries.any { it.message == "Native $KEY refresh: blocked" })

        at(63_000)
        mayRequest = true
        at(121_999)
        assertEquals("no failure retry after a blocked one", 1, loads.sent.size)
        assertEquals(1, states.size)
        assertEquals(0, ad0.destroyCount)

        // The next tick, 60 s after the blocked retry, sends.
        at(122_000)
        assertEquals(listOf(60_000L, 122_000L), loads.times)
    }

    @Test
    fun everyRefresh_attachesFreshEvents() = runTest {
        val made = mutableListOf<NativeAdEvents>()
        val given = mutableListOf<NativeAdEvents?>()
        val (states, _) = collectRefresh(
            load = { _ ->
                nativeLoadOnce(
                    newEvents = { mockk<NativeAdEvents>(relaxed = true).also { made += it } },
                    newLoader = { events ->
                        given += events
                        loader
                    },
                    unitId = FakeNativeLoads.FAKE_UNIT_ID,
                    size = NativeAdSize.SMALL,
                    timeoutMillis = 30_000,
                    logger = logger,
                    placementKey = KEY,
                )()
            },
        )
        at(60_000)
        deliver()
        deliver()
        at(120_000)
        assertEquals(3, states.size)

        assertTrue("fewer loads than expected: ${given.size}", given.size >= 3)
        assertEquals(made.size, given.size)
        given.forEachIndexed { index, events ->
            assertNotNull("load ${index + 1} got no events", events)
            assertSame(made[index], events)
        }
        assertEquals(given.size, given.toSet().size)
    }

    private companion object {
        const val KEY = "sample_native_small"
    }
}
