package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Retries of a native slot on a virtual clock: 5 s, foreground only, gate at send, network back (O-05, O-09). */
@OptIn(ExperimentalCoroutinesApi::class)
class NativeAdRetryTest {
    private val loader = FakeNativeAdLoader()
    private val logger = RecordingAppLogger()
    private val foreground = MutableStateFlow(true)
    private val retryResets = MutableStateFlow(0)
    private var mayRequest = true

    /** The window of the in-screen ads as AdsController builds it, in foreground (KB-27, plan T-054). */
    private val window = InScreenAdWindow(logger).apply { onForeground(true) }

    /** Times at which a request reached the loader. */
    private val requestTimes = mutableListOf<Long>()

    private fun TestScope.loadOnce(): suspend () -> NativeAdState = {
        requestTimes += testScheduler.currentTime
        loadOneNativeAd(loader, UNIT_ID, NativeAdSize.SMALL, TIMEOUT, logger, KEY)
    }

    private fun TestScope.collectStates(
        load: suspend () -> NativeAdState? = loadOnce(),
        refresh: ((NativeAdState.Loaded) -> Flow<NativeAdState>)? = null,
        gate: StateFlow<Boolean> = foreground,
    ): Pair<MutableList<NativeAdState>, Job> {
        val states = mutableListOf<NativeAdState>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            retryingNativeAdStates(load, gate, { mayRequest }, retryResets, logger, KEY, refresh)
                .collect { states += it }
        }
        return states to job
    }

    /** Moves the virtual clock to [millis] after the start and runs what is due. */
    private fun TestScope.at(millis: Long) {
        advanceTimeBy(millis - testScheduler.currentTime)
        runCurrent()
    }

    @Test
    fun failureWithoutAd_retriesAfter5s() = runTest {
        val (states, _) = collectStates()
        loader.fail(3)
        runCurrent()
        assertEquals(listOf(NativeAdState.Loading, NativeAdState.Unavailable), states)

        at(4_999)
        assertEquals(1, loader.requests.size)
        at(5_000)
        assertEquals(2, loader.requests.size)

        val ad = FakeLoadedNativeAd()
        loader.deliver(ad)
        runCurrent()
        assertEquals(3, states.size)
        assertEquals(listOf(NativeAdState.Loading, NativeAdState.Unavailable), states.take(2))
        assertSame(ad, (states[2] as NativeAdState.Loaded).ad)
    }

    @Test
    fun keepsFailing_every5s_neverGivesUp() = runTest {
        collectStates()
        repeat(5) { index ->
            at(index * 5_000L)
            loader.fail(2)
            runCurrent()
        }
        at(25_000)

        assertEquals(listOf(0L, 5_000L, 10_000L, 15_000L, 20_000L, 25_000L), requestTimes)
    }

    @Test
    fun prepareFailure_countsAsCode0AndRetries() = runTest {
        var calls = 0
        val (states, _) = collectStates(
            load = {
                calls++
                requestTimes += testScheduler.currentTime
                if (calls == 1) throw IllegalStateException("secret sdk detail")
                NativeAdState.Unavailable
            },
        )
        runCurrent()

        assertEquals(listOf(NativeAdState.Loading, NativeAdState.Unavailable), states)
        val ads = logger.entries.filter { it.tag == "Ads" }
        assertTrue(ads.any { it.message.contains("failed: 0") })
        assertFalse(logger.entries.any { it.message.contains("secret sdk detail") })
        at(4_999)
        assertEquals(1, calls)
        at(5_000)
        assertEquals(listOf(0L, 5_000L), requestTimes)
    }

    @Test
    fun networkReturn_cutsWaitShort() = runTest {
        collectStates()
        loader.fail(2)
        runCurrent()

        at(1_000)
        retryResets.value += 1
        runCurrent()

        assertEquals(listOf(0L, 1_000L), requestTimes)
    }

    @Test
    fun background_sendsNothingUntilForeground() = runTest {
        collectStates()
        loader.fail(2)
        runCurrent()

        at(1_000)
        foreground.value = false
        at(5_000)
        assertEquals(1, loader.requests.size)
        at(60_000)
        assertEquals(1, loader.requests.size)
        at(70_000)
        foreground.value = true
        runCurrent()
        assertEquals(listOf(0L, 70_000L), requestTimes)
    }

    @Test
    fun background_control_foregroundRetriesAt5s() = runTest {
        // Control (L-055) of background_sendsNothingUntilForeground: in foreground the retry goes at 5 000.
        collectStates()
        loader.fail(2)
        runCurrent()

        at(5_000)
        assertEquals(listOf(0L, 5_000L), requestTimes)
    }

    @Test
    fun firstLoad_inBackground_waitsForForeground() = runTest {
        foreground.value = false
        val (states, _) = collectStates()

        at(30_000)
        assertEquals(0, loader.requests.size)
        assertEquals(listOf(NativeAdState.Loading), states)

        foreground.value = true
        runCurrent()
        assertEquals(listOf(30_000L), requestTimes)
    }

    @Test
    fun firstLoad_whileCovered_waitsUntilUncovered() = runTest {
        window.onCovered(true)
        val (states, _) = collectStates(gate = window.open)

        at(60_000)
        assertEquals(0, loader.requests.size)
        assertEquals(listOf(NativeAdState.Loading), states)

        // Control (L-055): the cover goes, the first load is sent once.
        window.onCovered(false)
        runCurrent()
        assertEquals(listOf(60_000L), requestTimes)
    }

    @Test
    fun retryDueWhileCovered_sendsOnceWhenUncovered() = runTest {
        collectStates(gate = window.open)
        loader.fail(2)
        runCurrent()

        at(1_000)
        window.onCovered(true)
        at(60_000)
        assertEquals(1, loader.requests.size)

        window.onCovered(false)
        runCurrent()
        assertEquals(listOf(0L, 60_000L), requestTimes)
    }

    @Test
    fun gateClosedAtSend_endsWithoutRetry() = runTest {
        val (states, job) = collectStates()
        loader.fail(2)
        runCurrent()

        at(1_000)
        mayRequest = false
        at(60_000)

        assertEquals(1, loader.requests.size)
        assertTrue(logger.entries.any { it.tag == "Ads" && it.message.contains("request blocked") })
        assertEquals(listOf(NativeAdState.Loading, NativeAdState.Unavailable), states)
        assertTrue(job.isCompleted)
    }

    @Test
    fun gateClosedAtSend_control_openGateRetriesAt5s() = runTest {
        // Control (L-055) of gateClosedAtSend_endsWithoutRetry: an open gate sends the retry at 5 000.
        collectStates()
        loader.fail(2)
        runCurrent()

        at(1_000)
        mayRequest = true
        at(5_000)

        assertEquals(listOf(0L, 5_000L), requestTimes)
    }

    @Test
    fun loadedAd_isDestroyedWhenCollectionEnds() = runTest {
        val (states, job) = collectStates()
        val ad = FakeLoadedNativeAd()
        loader.deliver(ad)
        runCurrent()
        assertEquals(2, states.size)
        assertEquals(0, ad.destroyCount)

        job.cancel()
        runCurrent()
        assertEquals(1, ad.destroyCount)
    }

    @Test
    fun cancelDuringWait_sendsNothing() = runTest {
        val (_, job) = collectStates()
        loader.fail(2)
        runCurrent()

        at(1_000)
        job.cancel()
        at(60_000)

        assertEquals(1, loader.requests.size)
        assertTrue(job.isCancelled)
    }

    @Test
    fun everyNativeLoad_attachesFreshTelemetryEvents() = runTest {
        val made = mutableListOf<NativeAdEvents>()
        val given = mutableListOf<NativeAdEvents?>()
        val load = nativeLoadOnce(
            newEvents = { mockk<NativeAdEvents>(relaxed = true).also { made += it } },
            newLoader = { events ->
                given += events
                loader
            },
            unitId = UNIT_ID,
            size = NativeAdSize.SMALL,
            timeoutMillis = 10_000,
            logger = logger,
            placementKey = KEY,
        )
        val (states, _) = collectStates(load)
        loader.fail(3)
        runCurrent()
        at(5_000)
        loader.deliver(FakeLoadedNativeAd())
        runCurrent()

        assertNotNull("the first load got no events", given.first())
        assertTrue(states.last() is NativeAdState.Loaded)
        assertEquals(2, made.size)
        assertEquals(2, given.size)
        given.forEachIndexed { index, events ->
            assertNotNull("load ${index + 1} got no events", events)
            assertSame(made[index], events)
        }
        assertNotSame(given[0], given[1])
    }

    @Test
    fun loaderNotBuilt_reportsCode0ToEventsAndRetriesAt5s() = runTest {
        val made = mutableListOf<NativeAdEvents>()
        var builds = 0
        val buildTimes = mutableListOf<Long>()
        val load = nativeLoadOnce(
            newEvents = { mockk<NativeAdEvents>(relaxed = true).also { made += it } },
            newLoader = {
                builds++
                buildTimes += testScheduler.currentTime
                if (builds == 1) throw IllegalStateException("sdk torn down")
                loader
            },
            unitId = UNIT_ID,
            size = NativeAdSize.SMALL,
            timeoutMillis = 10_000,
            logger = logger,
            placementKey = KEY,
        )
        val (states, _) = collectStates(load)
        runCurrent()

        assertEquals(listOf(NativeAdState.Loading, NativeAdState.Unavailable), states)
        verify(exactly = 1) { made.first().onFailed(0, null) }
        at(4_999)
        assertEquals(listOf(0L), buildTimes)
        at(5_000)
        assertEquals(listOf(0L, 5_000L), buildTimes)
        assertEquals(1, loader.requests.size)
    }

    @Test
    fun firstLoad_blockedAtSource_endsWithoutRetry() = runTest {
        var calls = 0
        val (states, job) = collectStates(
            load = {
                calls++
                null
            },
        )
        runCurrent()
        at(60_000)

        assertEquals(1, calls)
        assertEquals(listOf(NativeAdState.Loading, NativeAdState.Unavailable), states)
        assertTrue(job.isCompleted)
        assertTrue(logger.entries.any { it.message == "Native $KEY request blocked" })
        assertFalse(logger.entries.any { it.message.contains("retry in") })
    }

    @Test
    fun refreshGiven_handsOverToRefresh() = runTest {
        val ad1 = FakeLoadedNativeAd()
        val ad2 = FakeLoadedNativeAd()
        val (states, job) = collectStates(
            refresh = { first ->
                flow {
                    emit(first)
                    emit(NativeAdState.Loaded(ad2))
                    awaitCancellation()
                }
            },
        )
        loader.deliver(ad1)
        runCurrent()

        assertEquals(
            listOf(NativeAdState.Loading, NativeAdState.Loaded(ad1), NativeAdState.Loaded(ad2)),
            states,
        )
        job.cancel()
        runCurrent()
        // The refresh owns the ads now: the retry flow destroys none of them.
        assertEquals(0, ad1.destroyCount)
    }

    @Test
    fun refreshEnds_noFurtherLoad() = runTest {
        var refreshCalls = 0
        val (states, job) = collectStates(
            refresh = {
                refreshCalls++
                flowOf(NativeAdState.Unavailable)
            },
        )
        loader.deliver(FakeLoadedNativeAd())
        runCurrent()
        at(60_000)

        assertEquals(1, refreshCalls)
        assertEquals(1, loader.requests.size)
        assertEquals(listOf(NativeAdState.Loading, NativeAdState.Unavailable), states)
        assertTrue(job.isCompleted)
        assertFalse(logger.entries.any { it.message.contains("retry in 5000 ms") })
    }

    @Test
    fun refreshNull_neverRequestsAgainAfterLoaded() = runTest {
        // Medium and collapsible slots pass no refresh: one ad until the slot leaves the screen.
        val (states, _) = collectStates(refresh = null)
        val ad = FakeLoadedNativeAd()
        loader.deliver(ad)
        runCurrent()
        at(180_000)

        assertEquals(1, loader.requests.size)
        assertEquals(listOf(NativeAdState.Loading, NativeAdState.Loaded(ad)), states)
        assertEquals(0, ad.destroyCount)
    }

    private companion object {
        const val UNIT_ID = "test-native-unit"
        const val KEY = "poses_bottom"
        const val TIMEOUT = 10_000L
    }
}
