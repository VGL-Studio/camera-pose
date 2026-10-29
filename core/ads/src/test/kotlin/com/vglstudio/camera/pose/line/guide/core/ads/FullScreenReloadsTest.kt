package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Retries and reloads of full-screen placements on a virtual clock (F-05, F-06, S-15, O-05, question 9). */
@OptIn(ExperimentalCoroutinesApi::class)
class FullScreenReloadsTest {
    private val logger = RecordingAppLogger()
    private val foreground = MutableStateFlow(true)

    /** Times of each loadIfNeeded call, per placement. */
    private val loads = mutableMapOf<AdPlacement, MutableList<Long>>()

    private fun TestScope.reloads(failuresInARow: (AdPlacement) -> Int, onLoad: (AdPlacement) -> Unit = {}) =
        FullScreenReloads(
            scope = backgroundScope,
            clock = MonotonicClock { testScheduler.currentTime },
            failuresInARow = failuresInARow,
            loadIfNeeded = {
                loads.getOrPut(it) { mutableListOf() } += testScheduler.currentTime
                onLoad(it)
            },
            foreground = foreground,
            logger = logger,
        )

    private fun loadsOf(placement: AdPlacement): List<Long> = loads[placement].orEmpty()

    /** Moves the virtual clock to [millis] after the start and runs what is due. */
    private fun TestScope.at(millis: Long) {
        advanceTimeBy(millis - testScheduler.currentTime)
        runCurrent()
    }

    @Test
    fun failures_retryAt2_4_6_8_10_thenEvery10s() = runTest {
        var failures = 0
        val states = mutableListOf<Pair<Long, FullScreenLoadState>>()
        lateinit var reloads: FullScreenReloads
        // Every load fails at once, as the first one did.
        reloads = reloads(failuresInARow = { failures }) {
            if (failures < 7) {
                failures++
                states += testScheduler.currentTime to reloads.onLoadFailed(INTER, 3)
            }
        }
        failures = 1
        states += 0L to reloads.onLoadFailed(INTER, 3)

        at(1_999)
        assertEquals(emptyList<Long>(), loadsOf(INTER))
        at(60_000)
        assertEquals(listOf(2_000L, 6_000L, 12_000L, 20_000L, 30_000L, 40_000L, 50_000L), loadsOf(INTER))
        assertEquals(FullScreenLoadState.WaitingRetry(dueAtMillis = 2_000), states[0].second)
        assertEquals(FullScreenLoadState.WaitingRetry(dueAtMillis = 6_000), states[1].second)
        assertEquals(FullScreenLoadState.WaitingRetry(dueAtMillis = 50_000), states[6].second)
        assertTrue(
            logger.entries.any {
                it.tag == "Ads" &&
                    it.message == "Retry gallery_interstitial in 4000 ms (failures 2, code 3)"
            },
        )
    }

    @Test
    fun successResetsDelay() = runTest {
        val counter = LoadAttemptCounter()
        val slot = preloadCounterKey(INTER.key)
        val reloads = reloads(failuresInARow = { counter.failuresInARow(preloadCounterKey(it.key)) })

        counter.onRequestSent(slot, 0)
        counter.onFailed(slot, 0)
        assertEquals(FullScreenLoadState.WaitingRetry(2_000), reloads.onLoadFailed(INTER, 3))
        counter.onRequestSent(slot, 0)
        counter.onFailed(slot, 0)
        assertEquals(FullScreenLoadState.WaitingRetry(4_000), reloads.onLoadFailed(INTER, 3))
        counter.onRequestSent(slot, 0)
        counter.onLoaded(slot, 0)
        counter.onRequestSent(slot, 0)
        counter.onFailed(slot, 0)
        assertEquals(FullScreenLoadState.WaitingRetry(2_000), reloads.onLoadFailed(INTER, 3))

        // Control (L-055): with no success in between, the third wait is 6 s.
        val other = LoadAttemptCounter()
        val otherReloads = reloads(failuresInARow = { other.failuresInARow(preloadCounterKey(it.key)) })
        repeat(3) {
            other.onRequestSent(slot, 0)
            other.onFailed(slot, 0)
        }
        assertEquals(FullScreenLoadState.WaitingRetry(6_000), otherReloads.onLoadFailed(INTER, 3))
    }

    @Test
    fun newFailure_replacesPendingRetry() = runTest {
        var failures = 1
        val reloads = reloads(failuresInARow = { failures })
        reloads.onLoadFailed(INTER, 3)

        at(1_000)
        failures = 2
        assertEquals(FullScreenLoadState.WaitingRetry(5_000), reloads.onLoadFailed(INTER, 3))

        at(60_000)
        assertEquals(listOf(5_000L), loadsOf(INTER))
    }

    @Test
    fun prepareFailure_isCode0() = runTest {
        val reloads = reloads(failuresInARow = { 1 })

        assertEquals(FullScreenLoadState.WaitingRetry(2_000), reloads.onLoadFailed(INTER, 0))
        at(2_000)
        assertEquals(listOf(2_000L), loadsOf(INTER))
    }

    @Test
    fun nativeFullScreen_retryWaitsForForeground() = runTest {
        val reloads = reloads(failuresInARow = { 1 })
        assertEquals(FullScreenLoadState.WaitingRetry(5_000), reloads.onLoadFailed(NATIVE_FULL_SCREEN, 3))

        at(1_000)
        foreground.value = false
        at(5_000)
        assertEquals(emptyList<Long>(), loadsOf(NATIVE_FULL_SCREEN))

        at(8_000)
        foreground.value = true
        runCurrent()
        assertEquals(listOf(8_000L), loadsOf(NATIVE_FULL_SCREEN))
    }

    @Test
    fun sendBlocked_isIdleAndSchedulesNothing() = runTest {
        var read = 0
        val reloads = reloads(failuresInARow = {
            read++
            1
        })

        assertEquals(FullScreenLoadState.Idle, reloads.onSendBlocked(INTER))
        at(60_000)

        assertEquals(emptyList<Long>(), loadsOf(INTER))
        assertEquals(0, read)
    }

    @Test
    fun closedInterstitial_reloadsAfter2s() = runTest {
        val reloads = reloads(failuresInARow = { 1 })

        reloads.onClosedOrShowFailed(INTER)
        reloads.onClosedOrShowFailed(NATIVE_FULL_SCREEN)
        runCurrent()
        assertEquals(listOf(0L), loadsOf(NATIVE_FULL_SCREEN))

        at(1_999)
        assertEquals(emptyList<Long>(), loadsOf(INTER))
        at(2_000)
        assertEquals(listOf(2_000L), loadsOf(INTER))
    }

    @Test
    fun perPlacementKeys_keepFailuresAndLatencyApart() {
        val a = preloadCounterKey("a_interstitial")
        val b = preloadCounterKey("b_interstitial")
        assertNotEquals(a, b)
        assertNotEquals("", a)
        assertNotEquals("", b)

        val counter = LoadAttemptCounter()
        counter.onRequestSent(a, 0)
        counter.onRequestSent(b, 100)
        assertEquals(LoadOutcome(1, 250), counter.onFailed(a, 250))
        assertEquals(LoadOutcome(1, 300), counter.onLoaded(b, 400))
        assertEquals(1, counter.failuresInARow(a))
        assertEquals(0, counter.failuresInARow(b))
        assertEquals(2_000L, AdRetryPolicy.fullScreenDelayMillis(counter.failuresInARow(a)))

        // Control (L-055): one key shared by the format (T-023 version 2) mixes both placements.
        val shared = LoadAttemptCounter()
        val key = "preload:INTER"
        shared.onRequestSent(key, 0)
        shared.onRequestSent(key, 100)
        assertEquals(LoadOutcome(1, 150), shared.onFailed(key, 250))
        val second = shared.onLoaded(key, 400)
        assertNull(second.latencyMs)
        assertEquals(0, shared.failuresInARow(key))
    }

    @Test
    fun twoPlacements_failingTogether_retryOnTheirOwnSchedule() = runTest {
        val counter = LoadAttemptCounter()
        val reloads = reloads(failuresInARow = { counter.failuresInARow(preloadCounterKey(it.key)) })
        val inter = preloadCounterKey(INTER.key)
        val native = preloadCounterKey(NATIVE_FULL_SCREEN.key)

        counter.onRequestSent(inter, 0)
        counter.onFailed(inter, 0)
        reloads.onLoadFailed(INTER, 3)
        counter.onRequestSent(native, 0)
        counter.onFailed(native, 0)
        reloads.onLoadFailed(NATIVE_FULL_SCREEN, 3)
        counter.onRequestSent(inter, 0)
        counter.onFailed(inter, 0)
        assertEquals(FullScreenLoadState.WaitingRetry(4_000), reloads.onLoadFailed(INTER, 3))

        at(60_000)
        assertEquals(listOf(4_000L), loadsOf(INTER))
        assertEquals(listOf(5_000L), loadsOf(NATIVE_FULL_SCREEN))
    }

    @Test
    fun rewardedFailure_retriesThroughLoadIfNeeded() = runTest {
        var failures = 1
        val reloads = reloads(failuresInARow = { failures })

        assertEquals(FullScreenLoadState.WaitingRetry(2_000), reloads.onLoadFailed(REWARDED, 3))
        at(1_999)
        assertEquals(emptyList<Long>(), loadsOf(REWARDED))
        at(2_000)
        assertEquals(listOf(2_000L), loadsOf(REWARDED))

        // Five failures in a row: the cap of 10 s (F-05).
        failures = 5
        assertEquals(FullScreenLoadState.WaitingRetry(12_000), reloads.onLoadFailed(REWARDED, 3))
        at(12_000)
        assertEquals(listOf(2_000L, 12_000L), loadsOf(REWARDED))
    }

    @Test
    fun closedRewarded_reloadsAfter2s() = runTest {
        val reloads = reloads(failuresInARow = { 1 })

        reloads.onClosedOrShowFailed(REWARDED)

        at(1_999)
        assertEquals(emptyList<Long>(), loadsOf(REWARDED))
        at(2_000)
        assertEquals(listOf(2_000L), loadsOf(REWARDED))
    }

    private companion object {
        val REWARDED = AdPlacement.GALLERY_RW_ACTION
        val INTER = AdPlacement.GALLERY_INTERSTITIAL
        val NATIVE_FULL_SCREEN = AdPlacement.GALLERY_NATIVE_FULL_SCREEN
    }
}
