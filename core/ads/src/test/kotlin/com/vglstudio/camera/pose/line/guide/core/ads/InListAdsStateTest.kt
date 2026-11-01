package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ads of the in-list slots of one screen: one state flow per slot, kept while scrolling, destroyed on leaving. A
 * native small row that failed tries again 5 s later while it is drawn, only while the in-screen window is open and
 * the gate is open right before the send (O-05, KB-27, plan T-059), on a virtual clock.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InListAdsStateTest {
    private val logger = RecordingAppLogger()

    /** The window of the in-screen ads as AdsController builds it, in foreground (KB-27, plan T-054). */
    private val window = InScreenAdWindow(logger).apply { onForeground(true) }
    private val retryResets = MutableStateFlow(0)
    private var mayRequest = true
    private val every = mutableStateOf<Int?>(3)

    /** (slot, virtual time) of every request that reached a loader. */
    private val sends = mutableListOf<Pair<Int, Long>>()

    /** The states each slot emitted, in order. */
    private val emitted = mutableMapOf<Int, MutableList<NativeAdState>>()

    /** The open loaders of each slot: requests stay open until the test answers them. */
    private val loaders = mutableMapOf<Int, FakeNativeAdLoader>()

    /** When true, every request fails at once with code 3 (no fill). */
    private var failAtOnce = false

    private fun loaderOf(slot: Int): NativeAdLoader = if (failAtOnce) {
        NativeAdLoader { _, _, _, onFailed -> onFailed(3) }
    } else {
        loaders.getOrPut(slot) { FakeNativeAdLoader() }
    }

    private fun openLoader(slot: Int): FakeNativeAdLoader = loaders.getValue(slot)

    /** Counts the calls of a row load and answers each with [answer]. */
    private class Counting(private val answer: suspend () -> NativeAdState) {
        var calls = 0
            private set

        val load: suspend () -> NativeAdState = {
            calls++
            answer()
        }
    }

    /**
     * Loads run on the test scheduler but outside the dispatcher of backgroundScope, whose work advanceUntilIdle does
     * not run. Their job is a child of backgroundScope, so the rows that keep retrying stop when the test ends.
     */
    private fun TestScope.loadScope() =
        CoroutineScope(StandardTestDispatcher(testScheduler) + Job(backgroundScope.coroutineContext[Job]))

    /** Rows through the real [inListRowStates], each with the load [loadOf] gives for its slot. */
    private fun rowsOf(loadOf: (slot: Int) -> suspend () -> NativeAdState) = InListRowSource { placement, slot, shown ->
        inListRowStates(
            load = loadOf(slot),
            window = window.open,
            rowShown = shown,
            mayRequestNow = { mayRequest },
            retryResets = retryResets,
            logger = logger,
            placementKey = "${placement.key}#$slot",
        ).onEach { emitted.getOrPut(slot) { mutableListOf() } += it }
    }

    /** Rows whose loads go through [loadOneNativeAd] and the loader of their slot, recording each send. */
    private fun TestScope.sdkRows() = rowsOf { slot ->
        {
            sends += slot to testScheduler.currentTime
            loadOneNativeAd(loaderOf(slot), UNIT_ID, NativeAdSize.SMALL, TIMEOUT, logger, "$KEY#$slot")
        }
    }

    private fun inListAds(rows: InListRowSource, scope: CoroutineScope) = InListAds(
        placement = AdPlacement.SAMPLE_NATIVE_SMALL,
        every = every,
        rowSource = rows,
        scope = scope,
        logger = logger,
    )

    private fun TestScope.sdkAds() = inListAds(sdkRows(), loadScope())

    /** Moves the virtual clock to [millis] after the start and runs what is due. */
    private fun TestScope.at(millis: Long) {
        advanceTimeBy(millis - testScheduler.currentTime)
        runCurrent()
    }

    private fun TestScope.setEvery(value: Int?) {
        every.value = value
        Snapshot.sendApplyNotifications()
        runCurrent()
    }

    private fun sendTimes(slot: Int = 0): List<Long> = sends.filter { it.first == slot }.map { it.second }

    private fun logs(text: String) = logger.entries.filter { it.message.contains(text) }

    @Test
    fun requireInListFormat_rejectsOtherFormats() {
        assertThrows(IllegalArgumentException::class.java) { requireInListFormat(AdFormat.BANNER) }
        assertThrows(IllegalArgumentException::class.java) { requireInListFormat(AdFormat.INTERSTITIAL) }
        requireInListFormat(AdFormat.NATIVE_SMALL)
        requireInListFormat(AdFormat.NATIVE_MEDIUM)
        requireInListFormat(AdFormat.NATIVE_IN_COLLECTION)
        assertThrows(IllegalArgumentException::class.java) { requireInListFormat(AdFormat.NATIVE_STEP) }
    }

    @Test
    fun slot_loadsOnce_reusedOnSecondCall() = runTest {
        val ad = FakeLoadedNativeAd(sampleNativeAdAssets)
        val loader = Counting { NativeAdState.Loaded(ad) }
        val ads = inListAds(rowsOf { loader.load }, loadScope())

        val first = ads.slot(0)
        assertEquals(NativeAdState.Loading, first.value)
        advanceUntilIdle()
        assertEquals(NativeAdState.Loaded(ad), first.value)

        val second = ads.slot(0)
        advanceUntilIdle()
        assertSame(first, second)
        assertEquals(NativeAdState.Loaded(ad), second.value)
        assertEquals(1, loader.calls)
    }

    @Test
    fun twoSlots_twoSeparateLoads() = runTest {
        val loader = Counting { NativeAdState.Loaded(FakeLoadedNativeAd(sampleNativeAdAssets)) }
        val ads = inListAds(rowsOf { loader.load }, loadScope())

        val first = ads.slot(0)
        val second = ads.slot(1)
        advanceUntilIdle()

        assertEquals(2, loader.calls)
        val firstAd = (first.value as NativeAdState.Loaded).ad
        val secondAd = (second.value as NativeAdState.Loaded).ad
        assertNotSame(firstAd, secondAd)
    }

    @Test
    fun unavailableSlot_retriedAfter5s_notOnScrollBack() = runTest {
        val loader = Counting { NativeAdState.Unavailable }
        val ads = inListAds(rowsOf { loader.load }, loadScope())

        val state = ads.slot(0)
        runCurrent()
        // Control: the first call did load, so the counts below are the retries.
        assertEquals(1, loader.calls)
        assertEquals(NativeAdState.Unavailable, state.value)

        at(4_999)
        assertEquals(1, loader.calls)
        at(5_000)
        assertEquals(2, loader.calls)

        // Scrolled away and back: the same row, no second flow and no extra load.
        assertSame(state, ads.slot(0))
        runCurrent()
        assertEquals(2, loader.calls)
    }

    @Test
    fun failedRow_keepsRetryingEvery5s_neverGivesUp() = runTest {
        failAtOnce = true
        val ads = sdkAds()

        ads.slot(0)
        runCurrent()
        at(5_000)
        at(10_000)
        at(15_000)

        assertEquals(listOf(0L, 5_000L, 10_000L, 15_000L), sendTimes())
        assertEquals(listOf(NativeAdState.Loading, NativeAdState.Unavailable), emitted[0])
    }

    @Test
    fun failedRow_thenAd_showsLoaded_noMoreSends() = runTest {
        val ads = sdkAds()
        val state = ads.slot(0)
        runCurrent()
        openLoader(0).fail(3)
        runCurrent()
        assertEquals(NativeAdState.Unavailable, state.value)

        at(5_000)
        assertEquals(listOf(0L, 5_000L), sendTimes())
        val ad = FakeLoadedNativeAd()
        openLoader(0).deliver(ad)
        runCurrent()
        assertEquals(NativeAdState.Loaded(ad), state.value)

        at(125_000)
        assertEquals(2, sends.size)
        assertEquals(0, ad.destroyCount)
    }

    @Test
    fun firstSend_inBackground_waitsForForeground() = runTest {
        window.onForeground(false)
        val ads = sdkAds()

        ads.slot(0)
        at(60_000)
        assertEquals(0, sends.size)

        window.onForeground(true)
        runCurrent()
        assertEquals(listOf(60_000L), sendTimes())
    }

    @Test
    fun firstSend_covered_waitsUntilUncovered() = runTest {
        window.onCovered(true)
        val ads = sdkAds()

        ads.slot(0)
        at(60_000)
        assertEquals(0, sends.size)

        window.onCovered(false)
        runCurrent()
        assertEquals(listOf(60_000L), sendTimes())
    }

    @Test
    fun retry_dueWhileCovered_waitsUntilUncovered() = runTest {
        failAtOnce = true
        val ads = sdkAds()
        ads.slot(0)
        runCurrent()

        at(1_000)
        window.onCovered(true)
        at(5_000)
        assertEquals(1, sends.size)
        at(30_000)
        assertEquals(1, sends.size)

        window.onCovered(false)
        runCurrent()
        assertEquals(listOf(0L, 30_000L), sendTimes())
    }

    @Test
    fun networkBack_whileCovered_sendsNothing() = runTest {
        failAtOnce = true
        val ads = sdkAds()
        ads.slot(0)
        runCurrent()

        at(1_000)
        window.onCovered(true)
        at(2_000)
        retryResets.value += 1
        runCurrent()
        at(20_000)
        assertEquals(1, sends.size)

        window.onCovered(false)
        runCurrent()
        assertEquals(listOf(0L, 20_000L), sendTimes())
    }

    @Test
    fun retry_inForeground_sendsAt5s() = runTest {
        failAtOnce = true
        val ads = sdkAds()
        ads.slot(0)
        runCurrent()

        at(4_999)
        assertEquals(1, sends.size)
        at(5_000)
        assertEquals(listOf(0L, 5_000L), sendTimes())
    }

    @Test
    fun firstSend_gateClosed_sendsNothing_noRetry() = runTest {
        mayRequest = false
        val ads = sdkAds()

        val state = ads.slot(0)
        runCurrent()
        assertEquals(NativeAdState.Unavailable, state.value)

        at(60_000)
        assertEquals(0, sends.size)
        assertTrue(logs("request blocked").isNotEmpty())
    }

    @Test
    fun firstSend_gateOpen_sends() = runTest {
        val ads = sdkAds()

        ads.slot(0)
        runCurrent()
        assertEquals(listOf(0L), sendTimes())
    }

    @Test
    fun retry_dueInBackground_waitsForForeground() = runTest {
        failAtOnce = true
        val ads = sdkAds()
        ads.slot(0)
        runCurrent()

        at(1_000)
        window.onForeground(false)
        at(5_000)
        assertEquals(1, sends.size)
        at(30_000)
        assertEquals(1, sends.size)

        window.onForeground(true)
        runCurrent()
        assertEquals(listOf(0L, 30_000L), sendTimes())
    }

    @Test
    fun retry_gateClosedAtSend_sendsNothing_endsRetries() = runTest {
        failAtOnce = true
        val ads = sdkAds()
        ads.slot(0)
        runCurrent()

        at(4_000)
        mayRequest = false
        at(5_000)
        assertEquals(1, sends.size)
        at(65_000)
        assertEquals(1, sends.size)
        assertEquals(1, logs("retry in").size)
    }

    @Test
    fun loadThrows_countsAsFailure_retriesAt5s() = runTest {
        var calls = 0
        val ads = inListAds(
            rowsOf {
                {
                    sends += 0 to testScheduler.currentTime
                    if (calls++ == 0) throw IllegalStateException("request not built")
                    NativeAdState.Unavailable
                }
            },
            loadScope(),
        )

        val state = ads.slot(0)
        runCurrent()
        assertEquals(NativeAdState.Unavailable, state.value)

        at(5_000)
        assertEquals(listOf(0L, 5_000L), sendTimes())
        assertTrue(logs("failed: 0 (request not built)").isNotEmpty())
    }

    @Test
    fun networkBack_cutsRetryWaitShort() = runTest {
        failAtOnce = true
        val ads = sdkAds()
        ads.slot(0)
        runCurrent()

        at(2_000)
        retryResets.value += 1
        runCurrent()
        assertEquals(listOf(0L, 2_000L), sendTimes())
    }

    @Test
    fun networkBack_inBackground_waitsForForeground() = runTest {
        failAtOnce = true
        val ads = sdkAds()
        ads.slot(0)
        runCurrent()

        at(1_000)
        window.onForeground(false)
        at(2_000)
        retryResets.value += 1
        runCurrent()
        at(10_000)
        assertEquals(1, sends.size)

        window.onForeground(true)
        runCurrent()
        assertEquals(listOf(0L, 10_000L), sendTimes())
    }

    @Test
    fun everyNull_duringRetryWait_sendsNothingMore() = runTest {
        failAtOnce = true
        val ads = sdkAds()
        ads.slot(0)
        runCurrent()

        at(3_000)
        setEvery(null)
        at(63_000)
        assertEquals(1, sends.size)
    }

    @Test
    fun everyBack_newRowLoads_throughGate() = runTest {
        failAtOnce = true
        val ads = sdkAds()
        val old = ads.slot(0)
        runCurrent()
        assertEquals(1, sends.size)

        at(1_000)
        setEvery(null)
        setEvery(3)
        val fresh = ads.slot(0)
        runCurrent()
        assertNotSame(old, fresh)
        assertEquals(listOf(0L, 1_000L), sendTimes())

        // The same in background: the new row waits for foreground.
        at(2_000)
        setEvery(null)
        window.onForeground(false)
        setEvery(3)
        ads.slot(0)
        at(62_000)
        assertEquals(2, sends.size)

        window.onForeground(true)
        runCurrent()
        assertEquals(listOf(0L, 1_000L, 62_000L), sendTimes())
    }

    @Test
    fun twentyRows_onlyShownRowsRetry() = runTest {
        failAtOnce = true
        val ads = sdkAds()
        (0 until 20).forEach { ads.slot(it) }
        runCurrent()
        assertEquals(20, sends.size)
        // The list was scrolled past 20 rows: only rows 0 and 1 are still drawn.
        (2 until 20).forEach { ads.onRowShown(it, false) }

        at(60_000)
        assertEquals(20 + 24, sends.size)
        assertEquals((0..12).map { it * 5_000L }, sendTimes(0))
        assertEquals((0..12).map { it * 5_000L }, sendTimes(1))

        at(62_000)
        retryResets.value += 1
        runCurrent()
        assertEquals(20 + 24 + 2, sends.size)
        assertEquals(62_000L, sendTimes(0).last())
        assertEquals(62_000L, sendTimes(1).last())

        at(70_000)
        ads.onRowShown(5, true)
        runCurrent()
        at(75_000)
        assertEquals(listOf(0L, 70_000L, 75_000L), sendTimes(5))
        (2 until 20).filter { it != 5 }.forEach { assertEquals("row $it", listOf(0L), sendTimes(it)) }
    }

    @Test
    fun twentyRows_allShown_allRetryAt5s() = runTest {
        // Control of twentyRows_onlyShownRowsRetry: with no row hidden, every row tries again.
        failAtOnce = true
        val ads = sdkAds()
        (0 until 20).forEach { ads.slot(it) }
        runCurrent()

        at(5_000)
        assertEquals(40, sends.size)
        assertEquals(20, sends.count { it.second == 5_000L })
    }

    @Test
    fun hiddenRow_inFlightAd_isKept() = runTest {
        val ads = sdkAds()
        val state = ads.slot(0)
        runCurrent()
        ads.onRowShown(0, false)

        val ad = FakeLoadedNativeAd()
        openLoader(0).deliver(ad)
        runCurrent()
        assertEquals(NativeAdState.Loaded(ad), state.value)
        assertEquals(0, ad.destroyCount)

        ads.onRowShown(0, true)
        assertSame(state, ads.slot(0))
        at(120_000)
        assertEquals(1, sends.size)
        assertEquals(0, ad.destroyCount)
    }

    @Test
    fun hiddenRow_gateClosedWhileHidden_endsRetries() = runTest {
        failAtOnce = true
        val ads = sdkAds()
        ads.slot(0)
        runCurrent()

        at(1_000)
        ads.onRowShown(0, false)
        at(10_000)
        assertEquals(1, sends.size)
        mayRequest = false
        ads.onRowShown(0, true)
        runCurrent()
        at(70_000)

        assertEquals(1, sends.size)
        assertTrue(logs("request blocked").isNotEmpty())
    }

    @Test
    fun loadedRow_scrollBack_sameAd_noSend() = runTest {
        val ads = sdkAds()
        val state = ads.slot(0)
        runCurrent()
        val ad = FakeLoadedNativeAd()
        openLoader(0).deliver(ad)
        runCurrent()

        at(120_000)
        val again = ads.slot(0)
        runCurrent()
        assertSame(state, again)
        assertSame(ad, (again.value as NativeAdState.Loaded).ad)
        assertEquals(1, sends.size)
    }

    @Test
    fun twoRowsFailTogether_retryIndependently() = runTest {
        val ads = sdkAds()
        val row0 = ads.slot(0)
        val row1 = ads.slot(1)
        runCurrent()
        openLoader(0).fail(3)
        openLoader(1).fail(3)
        runCurrent()

        at(5_000)
        assertEquals(4, sends.size)
        val ad = FakeLoadedNativeAd()
        openLoader(0).deliver(ad)
        openLoader(1).fail(3)
        runCurrent()

        at(10_000)
        assertEquals(5, sends.size)
        assertEquals(listOf(0L, 5_000L), sendTimes(0))
        assertEquals(listOf(0L, 5_000L, 10_000L), sendTimes(1))
        assertEquals(NativeAdState.Loaded(ad), row0.value)
        assertEquals(NativeAdState.Unavailable, row1.value)
    }

    @Test
    fun twoRows_networkBack_eachSendsOnce() = runTest {
        failAtOnce = true
        val ads = sdkAds()
        ads.slot(0)
        ads.slot(1)
        runCurrent()

        at(1_000)
        retryResets.value += 1
        runCurrent()
        assertEquals(listOf(0L, 1_000L), sendTimes(0))
        assertEquals(listOf(0L, 1_000L), sendTimes(1))
    }

    @Test
    fun twoRows_logKeysDiffer() = runTest {
        failAtOnce = true
        val ads = sdkAds()
        ads.slot(0)
        ads.slot(1)
        runCurrent()

        val retries = logs("retry in").map { it.message }
        assertTrue(retries.toString(), retries.any { it.contains("$KEY#0 retry in 5000 ms") })
        assertTrue(retries.toString(), retries.any { it.contains("$KEY#1 retry in 5000 ms") })
    }

    @Test
    fun destroyAll_whileLoading_lateAdDestroyed_noRetry() = runTest {
        val ads = sdkAds()
        val state = ads.slot(0)
        runCurrent()

        ads.destroyAll()
        runCurrent()
        val late = FakeLoadedNativeAd()
        openLoader(0).deliver(late)
        runCurrent()

        assertEquals(1, late.destroyCount)
        assertFalse(state.value is NativeAdState.Loaded)
        at(60_000)
        assertEquals(1, sends.size)
    }

    @Test
    fun destroyAll_whileLoading_lateFailure_noRetry() = runTest {
        val ads = sdkAds()
        ads.slot(0)
        runCurrent()

        ads.destroyAll()
        runCurrent()
        openLoader(0).fail(3)
        runCurrent()

        at(60_000)
        assertEquals(1, sends.size)
    }

    @Test
    fun destroyAll_duringRetryWait_sendsNothing() = runTest {
        failAtOnce = true
        val ads = sdkAds()
        ads.slot(0)
        runCurrent()

        at(2_000)
        ads.destroyAll()
        at(60_000)
        assertEquals(1, sends.size)
    }

    @Test
    fun destroyAll_destroysEveryLoadedAd_andCancelsPendingLoads() = runTest {
        val loadedAds = listOf(FakeLoadedNativeAd(sampleNativeAdAssets), FakeLoadedNativeAd(sampleNativeAdAssets))
        val pending = CompletableDeferred<NativeAdState>()
        var pendingCancelled = false
        var call = 0
        val load: suspend () -> NativeAdState = {
            when (val index = call++) {
                0, 1 -> NativeAdState.Loaded(loadedAds[index])
                else -> try {
                    pending.await()
                } catch (e: CancellationException) {
                    pendingCancelled = true
                    throw e
                }
            }
        }
        val ads = inListAds(rowsOf { load }, loadScope())

        ads.slot(0)
        ads.slot(1)
        val pendingSlot = ads.slot(2)
        advanceUntilIdle()
        // Control: nothing is destroyed while the screen is shown.
        assertEquals(listOf(0, 0), loadedAds.map { it.destroyCount })
        assertEquals(NativeAdState.Loading, pendingSlot.value)

        ads.destroyAll()
        advanceUntilIdle()
        assertEquals(listOf(1, 1), loadedAds.map { it.destroyCount })
        assertTrue(pendingCancelled)

        // An ad that would still arrive is not kept.
        val late = FakeLoadedNativeAd(sampleNativeAdAssets)
        pending.complete(NativeAdState.Loaded(late))
        advanceUntilIdle()
        assertFalse(pendingSlot.value is NativeAdState.Loaded)
    }

    @Test
    fun slotResult_isLoggedOnceWithoutAdContent() = runTest {
        val loader = Counting { NativeAdState.Loaded(FakeLoadedNativeAd(sampleNativeAdAssets)) }
        val ads = inListAds(rowsOf { loader.load }, loadScope())

        ads.slot(0)
        advanceUntilIdle()
        ads.slot(0)
        advanceUntilIdle()

        val infos = logger.entries.filter { it.level == LogLevel.INFO }
        assertEquals(1, infos.size)
        val entry = infos.single()
        assertEquals("Ads", entry.tag)
        assertTrue(entry.message, entry.message.contains("slot 0"))
        assertTrue(entry.message, entry.message.contains("LOADED"))
        assertFalse(entry.message, entry.message.contains(sampleNativeAdAssets.headline))
    }

    @Test
    fun retryLogs_holdNoUnitIdOrAdText() = runTest {
        val ads = sdkAds()
        ads.slot(0)
        runCurrent()
        openLoader(0).fail(3)
        runCurrent()
        at(5_000)
        openLoader(0).deliver(FakeLoadedNativeAd(sampleNativeAdAssets))
        runCurrent()

        val messages = logger.entries.map { it.message }
        assertTrue(messages.toString(), messages.any { it.contains("retry in 5000 ms") })
        assertTrue(messages.toString(), messages.any { it.contains("slot 0: UNAVAILABLE") })
        assertTrue(messages.toString(), messages.any { it.contains("slot 0: LOADED") })
        assertFalse(messages.any { it.contains(UNIT_ID) })
        assertFalse(messages.any { it.contains(sampleNativeAdAssets.headline) })
    }

    private companion object {
        const val UNIT_ID = "test-native-unit"
        const val KEY = "sample_native_small"
        const val TIMEOUT = 10_000L
    }
}
