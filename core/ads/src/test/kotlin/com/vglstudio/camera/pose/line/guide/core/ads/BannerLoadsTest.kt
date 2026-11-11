package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** The sends of a banner slot on a virtual clock: refresh by ad unit or by hand, retries, gate, screen (B-02 to B-06). */
@OptIn(ExperimentalCoroutinesApi::class)
class BannerLoadsTest {
    private val logger = RecordingAppLogger()
    private val visible = MutableStateFlow(true)
    private val retryResets = MutableStateFlow(0)
    private var mayRequest = true
    private var loadedCalls = 0

    /** The reload interval by hand (time_reload_banner_collap, plan T-087): 30 s by default. */
    private val reload = MutableStateFlow(30_000L)

    private fun TestScope.fakeView() = FakeBannerAdView(clock = { testScheduler.currentTime })

    private fun TestScope.run(
        view: FakeBannerAdView,
        placementKey: String = KEY,
        reloadMillis: StateFlow<Long> = reload,
    ): Job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        runBannerLoads(
            view = view,
            visible = visible,
            mayRequestNow = { mayRequest },
            retryResets = retryResets,
            reloadMillis = reloadMillis,
            onLoaded = { loadedCalls++ },
            logger = logger,
            placementKey = placementKey,
        )
    }

    /** Moves the virtual clock to [millis] after the start and runs what is due. */
    private fun TestScope.at(millis: Long) {
        advanceTimeBy(millis - testScheduler.currentTime)
        runCurrent()
    }

    /** Answers every send of [view] with Loaded [afterMillis] later. */
    private fun TestScope.loadAfter(view: FakeBannerAdView, afterMillis: Long) {
        view.onSend = {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                delay(afterMillis)
                view.emitLoaded()
            }
        }
    }

    @Test
    fun requestIsCollapsible_onlyCollapsible_othersThrow() {
        // Plan T-088, section 3.5: every banner reloads by hand; only the collapsible banner asks to collapse.
        assertEquals(false, bannerRequestIsCollapsible(AdFormat.BANNER))
        assertEquals(false, bannerRequestIsCollapsible(AdFormat.BANNER_MREC))
        assertEquals(true, bannerRequestIsCollapsible(AdFormat.BANNER_COLLAPSIBLE))
        val others = AdFormat.entries - AdFormat.BANNER - AdFormat.BANNER_COLLAPSIBLE - AdFormat.BANNER_MREC
        assertTrue(others.isNotEmpty())
        others.forEach { format ->
            assertThrows(format.name, IllegalArgumentException::class.java) { bannerRequestIsCollapsible(format) }
        }
    }

    @Test
    fun afterLoaded_sendsOnlyOnReloadTick() = runTest {
        // Plan T-088, section 3.5: a result the SDK refreshes by itself never makes the app send; with the key at 0
        // the app never sends again.
        val off = sendTimesAfterSdkRefreshes(MutableStateFlow(0L))
        assertEquals(listOf(0L), off)

        // Control (L-055): with the key at 60 s the app sends on its tick only, 60 s after each load; the loads the SDK
        // refreshes at 60 000 and 120 000 fall inside a count, so they neither reset it nor send.
        val byKey = sendTimesAfterSdkRefreshes(MutableStateFlow(60_000L))
        assertEquals(
            listOf(0L, 60_500L, 120_500L, 180_500L, 240_500L, 300_500L, 360_500L, 420_500L, 480_500L, 540_500L),
            byKey,
        )
    }

    /**
     * Sends at 0, Loaded at 500, then the SDK refreshes (Loaded) at 60 000 and 120 000; sends up to 600 000. From the
     * second send on, the fake answers each send with Loaded at once.
     */
    private fun TestScope.sendTimesAfterSdkRefreshes(reloadMillis: StateFlow<Long>): List<Long> {
        val start = testScheduler.currentTime
        val view = fakeView()
        view.onSend = { if (view.sendCount >= 2) view.emitLoaded() }
        val job = run(view, reloadMillis = reloadMillis)
        at(start + 500)
        view.emitLoaded()
        runCurrent()
        at(start + 60_000)
        view.emitLoaded()
        runCurrent()
        at(start + 120_000)
        view.emitLoaded()
        runCurrent()
        at(start + 600_000)
        job.cancel()
        return view.sendTimes.map { it - start }
    }

    @Test
    fun fixed_reloadsByHandAfterKeySeconds() = runTest {
        // Plan T-088, KB-34 criterion 1, KB-18 criterion 1: a fixed banner reloads by hand at its key, 60 s.
        val view = fakeView()
        run(view, reloadMillis = MutableStateFlow(60_000L))
        view.emitLoaded()
        runCurrent()

        at(59_999)
        assertEquals(listOf(0L), view.sendTimes)
        at(60_000)
        assertEquals(listOf(0L, 60_000L), view.sendTimes)
        assertTrue(logger.entries.any { it.message == "Banner $KEY reload in 60000 ms" })
    }

    @Test
    fun tryNextWhileWaitingTick_sendsAtOnce_noFailure() = runTest {
        // Plan T-088, section 3.1 step 2: TryNext while the loop waits for its tick drops the count and sends at once.
        val view = fakeView()
        runCountingFailures(view, reloadMillis = MutableStateFlow(60_000L))
        view.emitLoaded()
        runCurrent()

        at(10_000)
        view.emitTryNext()
        runCurrent()
        assertEquals(listOf(0L, 10_000L), view.sendTimes)
        assertEquals(0, failedCalls)

        at(10_500)
        view.emitLoaded()
        runCurrent()
        at(60_000)
        assertEquals(listOf(0L, 10_000L), view.sendTimes)
        at(70_499)
        assertEquals(listOf(0L, 10_000L), view.sendTimes)
        at(70_500)
        assertEquals(listOf(0L, 10_000L, 70_500L), view.sendTimes)
        assertEquals(0, failedCalls)
    }

    @Test
    fun reloadZero_fixedBanner_retriesUntilLoaded_thenKeeps() = runTest {
        // Plan T-088, KB-34 criterion 4: key 0, no ad yet: retries until an ad comes (B-02), then keeps it.
        val off = failTwiceThenLoad(MutableStateFlow(0L))
        assertEquals(listOf(0L, 2_000L, 6_000L), off)

        // Control (L-055): the same story with the key at 60 s reloads 60 s after the load at 6 s.
        val byKey = failTwiceThenLoad(MutableStateFlow(60_000L))
        assertTrue("$byKey", 66_000L in byKey)
        assertEquals(listOf(0L, 2_000L, 6_000L), byKey.take(3))
    }

    /** Failed at 0 and at 2 000, Loaded at 6 000; the send times up to 600 000. */
    private fun TestScope.failTwiceThenLoad(reloadMillis: StateFlow<Long>): List<Long> {
        val start = testScheduler.currentTime
        val view = fakeView()
        val job = run(view, reloadMillis = reloadMillis)
        view.emitFailed(3)
        runCurrent()
        at(start + 2_000)
        view.emitFailed(3)
        runCurrent()
        at(start + 6_000)
        view.emitLoaded()
        runCurrent()
        at(start + 600_000)
        job.cancel()
        return view.sendTimes.map { it - start }
    }

    @Test
    fun fixedReload_gateClosedAtSend_sendsNothingAndStops() = runTest {
        // Plan T-088, KB-34 criterion 6, S-15: the reload by hand asks the consent gate right before it sends.
        val view = fakeView()
        val job = run(view, reloadMillis = MutableStateFlow(60_000L))
        view.emitLoaded()
        runCurrent()
        // Control: the loop waits for its tick.
        assertTrue(job.isActive)

        at(59_000)
        mayRequest = false
        at(60_000)
        assertEquals(listOf(0L), view.sendTimes)
        assertTrue("the loop has ended", job.isCompleted)
        assertTrue(logger.entries.any { it.message == "Banner $KEY request blocked" })

        mayRequest = true
        at(600_000)
        assertEquals(listOf(0L), view.sendTimes)
    }

    @Test
    fun retryDueWhileHidden_waitsForNextVisible() = runTest {
        val view = fakeView()
        run(view, reloadMillis = MutableStateFlow(0L))
        view.emitFailed(3)
        runCurrent()

        at(1_000)
        visible.value = false
        at(9_000)
        assertEquals(listOf(0L), view.sendTimes)

        visible.value = true
        runCurrent()
        assertEquals(listOf(0L, 9_000L), view.sendTimes)
    }

    @Test
    fun manualReloadDueWhileHidden_waitsForNextVisible() = runTest {
        val view = fakeView()
        run(view)
        view.emitLoaded()
        runCurrent()

        at(10_000)
        visible.value = false
        at(50_000)
        assertEquals(listOf(0L), view.sendTimes)

        visible.value = true
        runCurrent()
        assertEquals(listOf(0L, 50_000L), view.sendTimes)
    }

    @Test
    fun firstSend_waitsUntilVisible() = runTest {
        visible.value = false
        val view = fakeView()
        run(view, reloadMillis = MutableStateFlow(0L))

        at(60_000)
        assertEquals(emptyList<Long>(), view.sendTimes)

        visible.value = true
        runCurrent()
        assertEquals(listOf(60_000L), view.sendTimes)
    }

    @Test
    fun manual_reloadsEvery30sAfterLoaded() = runTest {
        val view = fakeView()
        loadAfter(view, 300)
        run(view)

        at(95_000)

        assertEquals(listOf(0L, 30_300L, 60_600L, 90_900L), view.sendTimes)
        assertTrue(logger.entries.any { it.message == "Banner $KEY reload in 30000 ms" })
    }

    @Test
    fun manual_reloadsAfterKeySeconds() = runTest {
        reload.value = 45_000
        val view = fakeView()
        run(view)
        view.emitLoaded()
        runCurrent()

        at(30_000)
        assertEquals(listOf(0L), view.sendTimes)
        at(44_999)
        assertEquals(listOf(0L), view.sendTimes)
        at(45_000)
        assertEquals(listOf(0L, 45_000L), view.sendTimes)
        assertTrue(logger.entries.any { it.message == "Banner $KEY reload in 45000 ms" })
    }

    @Test
    fun manual_reloadChangedMidCount_appliesNext() = runTest {
        val view = fakeView()
        run(view)
        view.emitLoaded()
        runCurrent()

        at(10_000)
        reload.value = 60_000
        runCurrent()
        // The count under way keeps its 30 s.
        at(30_000)
        assertEquals(listOf(0L, 30_000L), view.sendTimes)
        view.emitLoaded()
        runCurrent()

        // The next count reads 60 s.
        at(89_999)
        assertEquals(listOf(0L, 30_000L), view.sendTimes)
        at(90_000)
        assertEquals(listOf(0L, 30_000L, 90_000L), view.sendTimes)
    }

    @Test
    fun manual_reloadZeroToPositive_countsFromReceipt() = runTest {
        reload.value = 0
        val view = fakeView()
        run(view)
        view.emitLoaded()
        runCurrent()

        at(300_000)
        assertEquals(listOf(0L), view.sendTimes)
        assertTrue(logger.entries.any { it.message == "Banner $KEY reload off" })

        reload.value = 30_000
        runCurrent()
        at(329_999)
        assertEquals(listOf(0L), view.sendTimes)
        at(330_000)
        assertEquals(listOf(0L, 330_000L), view.sendTimes)
    }

    @Test
    fun manual_reloadZeroDuringRetryAfterLoaded_keepsBanner() = runTest {
        val view = fakeView()
        run(view)
        view.emitLoaded()
        runCurrent()
        at(30_000)
        assertEquals(listOf(0L, 30_000L), view.sendTimes)
        // The reload fails: a retry is due 2 s later, at 32 s.
        view.emitFailed(3)
        runCurrent()

        at(31_000)
        reload.value = 0
        runCurrent()
        at(600_000)
        assertEquals(listOf(0L, 30_000L), view.sendTimes)
        assertEquals(0, view.destroyCount)
        assertTrue(logger.entries.any { it.message == "Banner $KEY reload off, keeping ad" })

        reload.value = 30_000
        runCurrent()
        at(629_999)
        assertEquals(listOf(0L, 30_000L), view.sendTimes)
        at(630_000)
        assertEquals(listOf(0L, 30_000L, 630_000L), view.sendTimes)

        // Control (L-055): the same failure with the key kept sends its retry 2 s later.
        val start = testScheduler.currentTime
        val kept = fakeView()
        run(kept, reloadMillis = MutableStateFlow(30_000L))
        kept.emitLoaded()
        runCurrent()
        at(start + 30_000)
        kept.emitFailed(3)
        runCurrent()
        at(start + 32_000)
        assertEquals(listOf(0L, 30_000L, 32_000L), kept.sendTimes.map { it - start })
    }

    @Test
    fun manual_neverLoaded_twoFailures_thenZero_keepsRetrying() = runTest {
        val view = fakeView()
        run(view)
        view.emitFailed(3)
        runCurrent()
        at(2_000)
        assertEquals(listOf(0L, 2_000L), view.sendTimes)
        // Second failure: the next retry is due 4 s later, at 6 s.
        view.emitFailed(3)
        runCurrent()

        at(3_000)
        reload.value = 0
        runCurrent()
        // No ad in this loop yet: it still retries (KB-34, criterion 4).
        at(6_000)
        assertEquals(listOf(0L, 2_000L, 6_000L), view.sendTimes)
        view.emitLoaded()
        runCurrent()

        at(600_000)
        assertEquals(listOf(0L, 2_000L, 6_000L), view.sendTimes)
    }

    @Test
    fun manual_loadedThenTwoFailures_thenZero_keepsBanner() = runTest {
        val view = fakeView()
        run(view)
        view.emitLoaded()
        runCurrent()
        at(30_000)
        view.emitFailed(3)
        runCurrent()
        at(32_000)
        assertEquals(listOf(0L, 30_000L, 32_000L), view.sendTimes)
        // Second failure: the next retry is due 4 s later, at 36 s.
        view.emitFailed(3)
        runCurrent()

        at(33_000)
        reload.value = 0
        runCurrent()
        at(36_000)
        assertEquals(listOf(0L, 30_000L, 32_000L), view.sendTimes)
        at(600_000)
        assertEquals(listOf(0L, 30_000L, 32_000L), view.sendTimes)

        reload.value = 30_000
        runCurrent()
        at(630_000)
        assertEquals(listOf(0L, 30_000L, 32_000L, 630_000L), view.sendTimes)
        // The failure count went back to 0: the first failure in a row again, retried 2 s later.
        view.emitFailed(3)
        runCurrent()
        at(631_999)
        assertEquals(listOf(0L, 30_000L, 32_000L, 630_000L), view.sendTimes)
        at(632_000)
        assertEquals(listOf(0L, 30_000L, 32_000L, 630_000L, 632_000L), view.sendTimes)
    }

    @Test
    fun manual_reloadZero_retriesUntilLoaded_thenKeeps() = runTest {
        reload.value = 0
        val view = fakeView()
        run(view)
        view.emitFailed(3)
        runCurrent()
        at(2_000)
        view.emitFailed(3)
        runCurrent()
        at(6_000)
        assertEquals(listOf(0L, 2_000L, 6_000L), view.sendTimes)
        view.emitLoaded()
        runCurrent()

        at(600_000)
        assertEquals(listOf(0L, 2_000L, 6_000L), view.sendTimes)
    }

    @Test
    fun firstSend_gateClosed_sendsNothingAndNeverRetries() = runTest {
        mayRequest = false
        val blocked = fakeView()
        val blockedJob = run(blocked, reloadMillis = MutableStateFlow(0L))

        at(120_000)
        assertEquals(emptyList<Long>(), blocked.sendTimes)
        assertTrue(logger.entries.any { it.message == "Banner $KEY request blocked" })
        assertTrue("the loop has ended", blockedJob.isCompleted)

        // Control (L-055): with the gate open the same slot sends at once.
        mayRequest = true
        val start = testScheduler.currentTime
        val open = fakeView()
        run(open, reloadMillis = MutableStateFlow(0L))
        runCurrent()
        assertEquals(listOf(start), open.sendTimes)
    }

    @Test
    fun retry_gateClosedAtSend_sendsNothingAndStops() = runTest {
        val view = fakeView()
        val job = run(view, reloadMillis = MutableStateFlow(0L))
        view.emitFailed(3)
        runCurrent()

        at(1_000)
        mayRequest = false
        at(5_000)
        mayRequest = true
        at(60_000)

        assertEquals(listOf(0L), view.sendTimes)
        assertTrue(job.isCompleted)
        assertTrue(logger.entries.any { it.message == "Banner $KEY request blocked" })
    }

    @Test
    fun manualReload_gateClosedAtSend_sendsNothingAndStops() = runTest {
        val view = fakeView()
        val job = run(view)
        view.emitLoaded()
        runCurrent()
        assertEquals(1, loadedCalls)

        at(10_000)
        mayRequest = false
        at(120_000)

        assertEquals(listOf(0L), view.sendTimes)
        assertTrue(job.isCompleted)
    }

    @Test
    fun manual_failure_retriesThenBackTo30s() = runTest {
        val view = fakeView()
        run(view)
        view.emitFailed(3)
        runCurrent()

        at(2_000)
        assertEquals(listOf(0L, 2_000L), view.sendTimes)
        view.emitLoaded()
        runCurrent()

        at(31_999)
        assertEquals(listOf(0L, 2_000L), view.sendTimes)
        at(32_000)
        assertEquals(listOf(0L, 2_000L, 32_000L), view.sendTimes)
    }

    @Test
    fun failures_retryAt2_4_6_8_10_10s() = runTest {
        val view = fakeView()
        view.onSend = { view.emitFailed(3) }
        run(view, reloadMillis = MutableStateFlow(0L))

        at(40_000)

        assertEquals(listOf(0L, 2_000L, 6_000L, 12_000L, 20_000L, 30_000L, 40_000L), view.sendTimes)
        assertTrue(logger.entries.any { it.message == "Banner $KEY retry in 10000 ms (failures 5, code 3)" })
    }

    @Test
    fun failedWithMessage_retryLineHasCodeAndMessage() = runTest {
        // Plan T-093, KB-28: the masked SDK message of a failed banner is in its retry line, after the code.
        val view = fakeView()
        view.onSend = { if (view.sendCount == 1) view.emitFailed(3, "No fill.") else view.emitLoaded() }
        run(view, reloadMillis = MutableStateFlow(0L))

        at(3_000)

        val expected = "Banner $KEY retry in 2000 ms (failures 1, code 3 \"No fill.\")"
        assertEquals(logger.entries.toString(), 1, logger.entries.count { it.message == expected })
    }

    @Test
    fun failed_callsOnFailed_onceEach_beforeRetry() = runTest {
        // Plan T-062: the slot hears every failure (an MREC with no ad yet collapses), at the failure, before the retry.
        val failedAt = mutableListOf<Long>()
        val sendsAtFailure = mutableListOf<Int>()
        val view = fakeView()
        view.onSend = { if (view.sendCount <= 2) view.emitFailed(3) else view.emitLoaded() }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            runBannerLoads(
                view = view,
                visible = visible,
                mayRequestNow = { mayRequest },
                retryResets = retryResets,
                reloadMillis = MutableStateFlow(0L),
                onLoaded = { loadedCalls++ },
                logger = logger,
                placementKey = KEY,
                onFailed = {
                    failedAt += testScheduler.currentTime
                    sendsAtFailure += view.sendCount
                },
            )
        }
        at(10_000)

        assertEquals(listOf(0L, 2_000L), failedAt)
        assertEquals(listOf(1, 2), sendsAtFailure)
        assertEquals(1, loadedCalls)
        val withOnFailed = view.sendTimes.toList()
        assertEquals(listOf(0L, 2_000L, 6_000L), withOnFailed)

        // Control: the same story without onFailed sends at the same times.
        val start = testScheduler.currentTime
        val plain = fakeView()
        plain.onSend = { if (plain.sendCount <= 2) plain.emitFailed(3) else plain.emitLoaded() }
        run(plain, reloadMillis = MutableStateFlow(0L))
        at(start + 10_000)
        assertEquals(withOnFailed, plain.sendTimes.map { it - start })
    }

    @Test
    fun loaded_resetsFailureCount() = runTest {
        val view = fakeView()
        run(view, reloadMillis = MutableStateFlow(0L))
        view.emitFailed(3)
        runCurrent()
        at(2_000)
        view.emitLoaded()
        runCurrent()
        assertEquals(1, loadedCalls)

        // A refresh of the SDK fails: the first failure in a row again, so 2 s and not 4 s.
        at(50_000)
        view.emitFailed(3)
        runCurrent()
        at(52_000)

        assertEquals(listOf(0L, 2_000L, 52_000L), view.sendTimes)
    }

    @Test
    fun networkBack_cutsRetryWaitShort() = runTest {
        val view = fakeView()
        view.onSend = { view.emitFailed(2) }
        run(view, reloadMillis = MutableStateFlow(0L))
        at(20_000)
        assertEquals(listOf(0L, 2_000L, 6_000L, 12_000L, 20_000L), view.sendTimes)
        view.onSend = {}

        at(21_000)
        retryResets.value += 1
        runCurrent()

        assertEquals(listOf(0L, 2_000L, 6_000L, 12_000L, 20_000L, 21_000L), view.sendTimes)
    }

    @Test
    fun manual_networkBack_doesNotShortenReload() = runTest {
        val view = fakeView()
        run(view)
        view.emitLoaded()
        runCurrent()

        at(5_000)
        retryResets.value += 1
        at(29_999)
        assertEquals(listOf(0L), view.sendTimes)
        at(30_000)
        assertEquals(listOf(0L, 30_000L), view.sendTimes)
    }

    @Test
    fun twoSlotsSamePlacement_countFailuresApart() = runTest {
        val a = fakeView()
        a.onSend = { a.emitFailed(3) }
        run(a, reloadMillis = MutableStateFlow(0L))
        at(6_000)
        assertEquals(listOf(0L, 2_000L, 6_000L), a.sendTimes)

        val b = fakeView()
        b.onSend = { b.emitFailed(3) }
        run(b, reloadMillis = MutableStateFlow(0L))
        runCurrent()
        at(8_000)

        assertEquals(listOf(6_000L, 8_000L), b.sendTimes)
        at(14_000)
        assertEquals(listOf(6_000L, 8_000L, 12_000L), b.sendTimes)
    }

    @Test
    fun staleResultBeforeStart_isIgnored() = runTest {
        val view = fakeView()
        // A result of an earlier run (before the gate closed) comes before this run collects.
        view.emitFailed(3)
        run(view)
        at(100)
        view.emitLoaded()
        runCurrent()

        at(30_100)

        assertEquals(listOf(0L, 30_100L), view.sendTimes)
        assertFalse(logger.entries.any { it.message.startsWith("Banner $KEY retry") })
    }

    @Test
    fun cancelled_sendsNothingMore() = runTest {
        val view = fakeView()
        val job = run(view, reloadMillis = MutableStateFlow(0L))
        view.emitFailed(3)
        runCurrent()
        // Control: the loop is waiting for its retry.
        assertTrue(job.isActive)

        at(1_000)
        job.cancel()
        at(60_000)

        assertEquals(listOf(0L), view.sendTimes)
    }

    /** [run] with a count of [runBannerLoads]' onFailed calls (plan T-071). */
    private var failedCalls = 0

    private fun TestScope.runCountingFailures(
        view: FakeBannerAdView,
        reloadMillis: StateFlow<Long> = MutableStateFlow(0L),
    ): Job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        runBannerLoads(
            view = view,
            visible = visible,
            mayRequestNow = { mayRequest },
            retryResets = retryResets,
            reloadMillis = reloadMillis,
            onLoaded = { loadedCalls++ },
            logger = logger,
            placementKey = KEY,
            onFailed = { failedCalls++ },
        )
    }

    @Test
    fun tryNext_sendsAtOnce_noFailureCounted_noOnFailed() = runTest {
        // Plan T-071, KB-31: the first platform of a frame had no ad; the second one is asked at once.
        val view = fakeView()
        runCountingFailures(view)
        at(500)
        view.emitTryNext()
        runCurrent()
        assertEquals(listOf(0L, 500L), view.sendTimes)
        assertEquals(0, failedCalls)
        assertFalse(logger.entries.any { it.message.startsWith("Banner $KEY retry") })

        // The second platform fails too: the first failure in a row, retried 2 s later (not 4 s).
        view.emitFailed(3)
        runCurrent()
        assertEquals(1, failedCalls)
        at(2_499)
        assertEquals(listOf(0L, 500L), view.sendTimes)
        at(2_500)
        assertEquals(listOf(0L, 500L, 2_500L), view.sendTimes)
        assertTrue(logger.entries.any { it.message == "Banner $KEY retry in 2000 ms (failures 1, code 3)" })
    }

    @Test
    fun tryNext_waitsVisible_andAsksGate() = runTest {
        // S-15, KB-27, L-072: the send to the second platform waits for the screen and asks the consent gate.
        val view = fakeView()
        val job = runCountingFailures(view)
        at(1_000)
        visible.value = false
        view.emitTryNext()
        runCurrent()
        at(5_000)
        assertEquals(listOf(0L), view.sendTimes)
        visible.value = true
        runCurrent()
        assertEquals(listOf(0L, 5_000L), view.sendTimes)

        // The gate closed: nothing sent, the loop ends.
        mayRequest = false
        view.emitTryNext()
        runCurrent()
        at(60_000)
        assertEquals(listOf(0L, 5_000L), view.sendTimes)
        assertTrue(logger.entries.any { it.message == "Banner $KEY request blocked" })
        assertTrue(job.isCompleted)
        assertEquals(0, failedCalls)
    }

    private companion object {
        const val KEY = "test_banner"
    }
}
