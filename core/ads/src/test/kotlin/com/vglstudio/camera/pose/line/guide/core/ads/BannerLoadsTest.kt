package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
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

    private fun TestScope.fakeView() = FakeBannerAdView(clock = { testScheduler.currentTime })

    private fun TestScope.run(refresh: BannerRefresh, view: FakeBannerAdView, placementKey: String = KEY): Job =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            runBannerLoads(
                refresh = refresh,
                view = view,
                visible = visible,
                mayRequestNow = { mayRequest },
                retryResets = retryResets,
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
    fun refreshOf_bannerByAdUnit_collapsibleManual_othersThrow() {
        assertEquals(BannerRefresh.BY_AD_UNIT, bannerRefreshOf(AdFormat.BANNER))
        assertEquals(BannerRefresh.MANUAL, bannerRefreshOf(AdFormat.BANNER_COLLAPSIBLE))
        val others = AdFormat.entries - AdFormat.BANNER - AdFormat.BANNER_COLLAPSIBLE
        assertTrue(others.isNotEmpty())
        others.forEach { format ->
            assertThrows(format.name, IllegalArgumentException::class.java) { bannerRefreshOf(format) }
        }
    }

    @Test
    fun byAdUnit_afterLoaded_appNeverSendsAgain() = runTest {
        val sdkRefreshed = sendTimesAfterSdkRefreshes(BannerRefresh.BY_AD_UNIT)
        assertEquals(listOf(0L), sdkRefreshed)

        // Control (L-055): the same story by hand sends again 30 s after the first load.
        val manual = sendTimesAfterSdkRefreshes(BannerRefresh.MANUAL)
        assertTrue("$manual", 30_500L in manual)
    }

    /** Sends at 0, Loaded at 500, then the SDK refreshes (Loaded) at 60 000 and 120 000; sends up to 600 000. */
    private fun TestScope.sendTimesAfterSdkRefreshes(refresh: BannerRefresh): List<Long> {
        val start = testScheduler.currentTime
        val view = fakeView()
        val job = run(refresh, view)
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
    fun retryDueWhileHidden_waitsForNextVisible() = runTest {
        val view = fakeView()
        run(BannerRefresh.BY_AD_UNIT, view)
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
        run(BannerRefresh.MANUAL, view)
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
        run(BannerRefresh.BY_AD_UNIT, view)

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
        run(BannerRefresh.MANUAL, view)

        at(95_000)

        assertEquals(listOf(0L, 30_300L, 60_600L, 90_900L), view.sendTimes)
        assertTrue(logger.entries.any { it.message == "Banner $KEY reload in 30000 ms" })
    }

    @Test
    fun firstSend_gateClosed_sendsNothingAndNeverRetries() = runTest {
        mayRequest = false
        val blocked = fakeView()
        val blockedJob = run(BannerRefresh.BY_AD_UNIT, blocked)

        at(120_000)
        assertEquals(emptyList<Long>(), blocked.sendTimes)
        assertTrue(logger.entries.any { it.message == "Banner $KEY request blocked" })
        assertTrue("the loop has ended", blockedJob.isCompleted)

        // Control (L-055): with the gate open the same slot sends at once.
        mayRequest = true
        val start = testScheduler.currentTime
        val open = fakeView()
        run(BannerRefresh.BY_AD_UNIT, open)
        runCurrent()
        assertEquals(listOf(start), open.sendTimes)
    }

    @Test
    fun retry_gateClosedAtSend_sendsNothingAndStops() = runTest {
        val view = fakeView()
        val job = run(BannerRefresh.BY_AD_UNIT, view)
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
        val job = run(BannerRefresh.MANUAL, view)
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
        run(BannerRefresh.MANUAL, view)
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
        run(BannerRefresh.BY_AD_UNIT, view)

        at(40_000)

        assertEquals(listOf(0L, 2_000L, 6_000L, 12_000L, 20_000L, 30_000L, 40_000L), view.sendTimes)
        assertTrue(logger.entries.any { it.message == "Banner $KEY retry in 10000 ms (failures 5, code 3)" })
    }

    @Test
    fun loaded_resetsFailureCount() = runTest {
        val view = fakeView()
        run(BannerRefresh.BY_AD_UNIT, view)
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
        run(BannerRefresh.BY_AD_UNIT, view)
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
        run(BannerRefresh.MANUAL, view)
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
        run(BannerRefresh.BY_AD_UNIT, a)
        at(6_000)
        assertEquals(listOf(0L, 2_000L, 6_000L), a.sendTimes)

        val b = fakeView()
        b.onSend = { b.emitFailed(3) }
        run(BannerRefresh.BY_AD_UNIT, b)
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
        run(BannerRefresh.MANUAL, view)
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
        val job = run(BannerRefresh.BY_AD_UNIT, view)
        view.emitFailed(3)
        runCurrent()
        // Control: the loop is waiting for its retry.
        assertTrue(job.isActive)

        at(1_000)
        job.cancel()
        at(60_000)

        assertEquals(listOf(0L), view.sendTimes)
    }

    private companion object {
        const val KEY = "test_banner"
    }
}
