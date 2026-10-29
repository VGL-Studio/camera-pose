package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Native ad loading on a virtual clock: timeout, SDK errors, late ads and cancellation (plan T-010, section 4). */
@OptIn(ExperimentalCoroutinesApi::class)
class NativeAdLoadingTest {
    private val loader = FakeNativeAdLoader()
    private val logger = RecordingAppLogger()

    private fun TestScope.collectStates(timeoutMillis: Long = TIMEOUT): Pair<MutableList<NativeAdState>, Job> {
        val states = mutableListOf<NativeAdState>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            nativeAdStates(loader, UNIT_ID, NativeAdSize.SMALL, timeoutMillis, logger, KEY).collect { states += it }
        }
        return states to job
    }

    /** Moves the virtual clock to [millis] after the start and runs what is due. */
    private fun TestScope.at(millis: Long) {
        advanceTimeBy(millis - testScheduler.currentTime)
        runCurrent()
    }

    @Test
    fun noAdBeforeTimeout_becomesUnavailable() = runTest {
        val (states, _) = collectStates()
        assertEquals(listOf(NativeAdState.Loading), states)

        at(9_999)
        assertEquals(listOf(NativeAdState.Loading), states)

        at(10_000)
        assertEquals(listOf(NativeAdState.Loading, NativeAdState.Unavailable), states)
        assertTrue(logger.entries.any { it.tag == "Ads" && it.message.contains("timed out after 10000 ms") })
    }

    @Test
    fun loadFailure_becomesUnavailableAtOnce() = runTest {
        val (states, _) = collectStates()

        at(1_000)
        loader.fail(3)
        runCurrent()

        assertEquals(listOf(NativeAdState.Loading, NativeAdState.Unavailable), states)
        assertEquals(1_000L, testScheduler.currentTime)
        val failures = logger.entries.filter { it.tag == "Ads" && it.message.contains("failed: 3") }
        assertEquals(1, failures.size)
        assertEquals(LogLevel.INFO, failures.single().level)
    }

    @Test
    fun adBeforeTimeout_isLoaded() = runTest {
        val (states, _) = collectStates()
        val ad = FakeLoadedNativeAd()

        at(2_000)
        loader.deliver(ad)
        runCurrent()
        at(12_000)

        assertEquals(2, states.size)
        assertEquals(NativeAdState.Loading, states[0])
        assertSame(ad, (states[1] as NativeAdState.Loaded).ad)
        assertFalse(states.contains(NativeAdState.Unavailable))
        assertEquals(0, ad.destroyCount)
    }

    @Test
    fun adAfterTimeout_isDestroyedAndNotShown() = runTest {
        val (states, _) = collectStates()
        val ad = FakeLoadedNativeAd()

        at(12_000)
        loader.deliver(ad)
        runCurrent()

        assertEquals(listOf(NativeAdState.Loading, NativeAdState.Unavailable), states)
        assertEquals(1, ad.destroyCount)
    }

    @Test
    fun nonPositiveTimeout_isUnavailableWithoutWaiting() = runTest {
        val (states, _) = collectStates(timeoutMillis = -5)
        runCurrent()

        assertEquals(listOf(NativeAdState.Loading, NativeAdState.Unavailable), states)
        assertEquals(0L, testScheduler.currentTime)
    }

    @Test
    fun cancelCollection_destroysLoadedAd() = runTest {
        val (states, job) = collectStates()
        val ad = FakeLoadedNativeAd()
        at(2_000)
        loader.deliver(ad)
        runCurrent()
        assertTrue(states.last() is NativeAdState.Loaded)
        assertEquals(0, ad.destroyCount)

        job.cancel()
        runCurrent()

        assertEquals(1, ad.destroyCount)
        assertTrue(logger.entries.any { it.tag == "Ads" && it.message.contains("destroyed") })
    }

    @Test
    fun cancelWhileLoading_destroysLateAd() = runTest {
        val (_, job) = collectStates()
        val ad = FakeLoadedNativeAd()
        at(1_000)

        job.cancel()
        runCurrent()
        loader.deliver(ad)

        assertEquals(1, ad.destroyCount)
    }

    @Test
    fun loadOne_adBeforeTimeout_returnsLoadedAndKeepsAd() = runTest {
        val ad = FakeLoadedNativeAd()
        val result = async(UnconfinedTestDispatcher(testScheduler)) {
            loadOneNativeAd(loader, UNIT_ID, NativeAdSize.SMALL, TIMEOUT, logger, KEY)
        }

        at(2_000)
        loader.deliver(ad)
        runCurrent()

        assertTrue(result.isCompleted)
        assertSame(ad, (result.await() as NativeAdState.Loaded).ad)
        assertEquals(0, ad.destroyCount)
    }

    @Test
    fun loadOne_noAd_returnsUnavailableAtTimeout() = runTest {
        val ad = FakeLoadedNativeAd()
        val result = async(UnconfinedTestDispatcher(testScheduler)) {
            loadOneNativeAd(loader, UNIT_ID, NativeAdSize.SMALL, TIMEOUT, logger, KEY)
        }

        at(9_999)
        assertFalse(result.isCompleted)
        at(10_000)
        assertTrue(result.isCompleted)
        assertEquals(NativeAdState.Unavailable, result.await())

        at(12_000)
        loader.deliver(ad)
        assertEquals(1, ad.destroyCount)
    }

    @Test
    fun loadOne_cancelledWhileLoading_destroysLateAd() = runTest {
        val ad = FakeLoadedNativeAd()
        val result = async(UnconfinedTestDispatcher(testScheduler)) {
            loadOneNativeAd(loader, UNIT_ID, NativeAdSize.SMALL, TIMEOUT, logger, KEY)
        }
        at(1_000)

        result.cancel()
        runCurrent()
        loader.deliver(ad)

        assertEquals(1, ad.destroyCount)
    }

    @Test
    fun logsHoldNoAdContent() = runTest {
        // One ad shown then destroyed, one failure, one timeout with a late ad: every log line of the flow.
        val (_, job) = collectStates()
        at(1_000)
        loader.deliver(FakeLoadedNativeAd())
        runCurrent()
        job.cancel()
        runCurrent()
        collectStates()
        loader.fail(3)
        runCurrent()
        collectStates()
        at(20_000)
        loader.deliver(FakeLoadedNativeAd())

        // Control: the flow did log, and with the placement key, so the check below reads real lines.
        assertTrue(logger.entries.size >= 4)
        assertTrue(logger.entries.all { it.message.contains(KEY) })
        logger.entries.forEach { entry ->
            assertFalse(entry.message, entry.message.contains(sampleNativeAdAssets.headline))
            assertFalse(entry.message, entry.message.contains(sampleNativeAdAssets.advertiser!!))
            assertFalse(entry.message, entry.message.contains(UNIT_ID))
        }
    }

    private companion object {
        const val TIMEOUT = 10_000L
        const val KEY = "sample_native_small"
        const val UNIT_ID = "ca-app-pub-3940256099942544/2247696110"
    }
}
