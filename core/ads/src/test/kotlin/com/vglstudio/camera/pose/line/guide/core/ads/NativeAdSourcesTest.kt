package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.analytics.AdMetricEvent
import com.vglstudio.camera.pose.line.guide.core.analytics.RecordingAdFunnel
import com.vglstudio.camera.pose.line.guide.core.analytics.RecordingAdMetrics
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
 * The sources of native single-ad slots: one per (placement, format), closed when the placement is switched off or
 * consent is withdrawn (L-061), every send through the gate (L-072), the use and format each load gets, and the
 * metrics of overlapping slots (L-068; plan T-040).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NativeAdSourcesTest {
    private val loader = FakeNativeAdLoader()
    private val logger = RecordingAppLogger()
    private val foreground = MutableStateFlow(true)
    private val retryResets = MutableStateFlow(0)
    private val canRequestAds = MutableStateFlow(true)
    private val enabledFlows = mutableMapOf<AdPlacement, MutableStateFlow<Boolean>>()
    private var mayRequest = true
    private var gateCalls = 0
    private lateinit var loads: FakeNativeLoads

    /** What each load was made for. */
    private val newLoads = mutableListOf<Triple<AdPlacement, AdFormat, NativeLoadUse>>()

    private fun enabledOf(placement: AdPlacement) = enabledFlows.getOrPut(placement) { MutableStateFlow(true) }

    private fun TestScope.newSources(
        newLoad: ((AdPlacement, AdFormat, NativeLoadUse) -> suspend () -> NativeAdState)? = null,
    ): NativeAdSources {
        loads = FakeNativeLoads(loader, { testScheduler.currentTime }, logger)
        return NativeAdSources(
            enabled = { enabledOf(it) },
            canRequestAds = canRequestAds,
            newLoad = newLoad ?: { placement, format, use ->
                newLoads += Triple(placement, format, use)
                suspend { loads.load(use) }
            },
            sendGate = {
                gateCalls++
                awaitSendAllowed(foreground) { mayRequest }
            },
            clock = { testScheduler.currentTime },
            scope = backgroundScope,
            logger = logger,
        ).also { runCurrent() }
    }

    /** One slot on screen, wired like AdsController.nativeAd. */
    private fun TestScope.collectSlot(
        sources: NativeAdSources,
        placement: AdPlacement = PLACEMENT,
    ): Pair<MutableList<NativeAdState>, Job> {
        val states = mutableListOf<NativeAdState>()
        val source = sources.source(placement)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            singleAdSlotStates(
                source,
                foreground,
                { mayRequest },
                retryResets,
                { testScheduler.currentTime },
                logger,
                placement.key,
            ).collect { states += it }
        }
        runCurrent()
        return states to job
    }

    private fun TestScope.claim(
        source: NativeAdSource,
        use: NativeLoadUse = NativeLoadUse.SINGLE_AD,
    ): Deferred<NativeClaim> = backgroundScope.async { source.claim(use) }.also { runCurrent() }

    private fun TestScope.deliver(ad: FakeLoadedNativeAd = FakeLoadedNativeAd()): FakeLoadedNativeAd {
        loader.deliver(ad)
        runCurrent()
        return ad
    }

    private fun TestScope.at(millis: Long) {
        advanceTimeBy(millis - testScheduler.currentTime)
        runCurrent()
    }

    private fun Deferred<NativeClaim>.result(): NativeClaim {
        assertTrue("the claim has not returned", isCompleted)
        return getCompleted()
    }

    @Test
    fun placementSwitchedOff_closesSource_destroysPending() = runTest {
        val sources = newSources()
        val source = sources.source(PLACEMENT)
        source.attach()
        claim(source)
        deliver()
        val pending = deliver()
        assertSame(pending, source.pending)

        enabledOf(PLACEMENT).value = false
        runCurrent()

        assertEquals(1, pending.destroyCount)
        assertNull(source.pending)
        assertEquals(1L, source.closed.value)
    }

    @Test
    fun consentWithdrawn_closesSource_waitersGetNoAd() = runTest {
        val sources = newSources()
        val source = sources.source(PLACEMENT)
        source.attach()
        val a = claim(source)
        assertEquals(1, loads.sent.size)

        canRequestAds.value = false
        runCurrent()

        assertEquals(NativeClaim.NoAd, a.result())
        assertEquals(1L, source.closed.value)
        val late = deliver()
        assertEquals(1, late.destroyCount)
        // A claim while consent is withdrawn sends nothing.
        assertEquals(NativeClaim.Blocked, claim(source).result())
        assertEquals(1, loads.sent.size)
    }

    @Test
    fun placementSwitchedOff_whileRefreshing_destroysShownAd_noMoreRefresh() = runTest {
        val sources = newSources()
        val (states, job) = collectSlot(sources)
        val ad1 = deliver()
        val ad2 = deliver()
        assertSame(ad1, (states.last() as NativeAdState.Loaded).ad)
        assertEquals(2, loads.sent.size)

        at(30_000)
        enabledOf(PLACEMENT).value = false
        runCurrent()

        assertEquals(1, ad1.destroyCount)
        assertEquals(1, ad2.destroyCount)
        assertEquals(NativeAdState.Unavailable, states.last())
        at(210_000)
        assertEquals(2, loads.sent.size)
        assertTrue(job.isCompleted)
    }

    @Test
    fun placementOnAgain_newSlotLoadsFromScratch() = runTest {
        val sources = newSources()
        val (_, first) = collectSlot(sources)
        deliver()
        deliver()
        enabledOf(PLACEMENT).value = false
        runCurrent()
        assertTrue(first.isCompleted)
        val sentBefore = loads.sent.size

        at(10_000)
        enabledOf(PLACEMENT).value = true
        runCurrent()
        val (states, _) = collectSlot(sources)

        assertEquals(sentBefore + 1, loads.sent.size)
        assertEquals(NativeLoadUse.SINGLE_AD, loads.uses.last())
        assertEquals(10_000L, loads.times.last())
        val ad = deliver()
        assertSame(ad, (states.last() as NativeAdState.Loaded).ad)
    }

    @Test
    fun placementOff_slotInFirstLoadRetry_sendsNothingFor60s() = runTest {
        val sources = newSources()
        val (states, job) = collectSlot(sources)
        loader.fail(3)
        runCurrent()
        assertEquals(listOf(NativeAdState.Loading, NativeAdState.Unavailable), states)
        assertEquals(1, loads.sent.size)

        at(2_000)
        enabledOf(PLACEMENT).value = false
        runCurrent()
        at(62_000)

        assertEquals(1, loads.sent.size)
        assertTrue(job.isCompleted)
        assertTrue(logger.entries.any { it.message == "Native ${PLACEMENT.key} request blocked" })
    }

    @Test
    fun everySend_callsSendGate() = runTest {
        val sources = newSources()
        // (a) first load of slot A, then (b) the next load after its claim.
        collectSlot(sources)
        deliver()
        // Slot B takes the ad of the next load, which starts another next load (b).
        collectSlot(sources)
        deliver()
        // Slots C and D wait on that load: C wins, D gets its one retry (c), then the next load (b).
        collectSlot(sources)
        collectSlot(sources)
        deliver()
        deliver()

        assertEquals(5, loads.sent.size)
        assertEquals(loads.sent.size, gateCalls)
    }

    @Test
    fun twoSlotsOverlap_loadAttemptAndLatencyPerRequest() = runTest {
        val metrics = RecordingAdMetrics()
        val counter = LoadAttemptCounter()
        val telemetry = AdTelemetry(
            metrics = metrics,
            funnel = RecordingAdFunnel(),
            lifecycleSwitches = { DEFAULT_FLAGS.copy(logSlotEvents = true) },
            serving = { true },
            offline = { false },
            counter = counter,
            logger = logger,
        )
        val uses = mutableListOf<NativeLoadUse>()
        val sources = newSources(
            newLoad = { placement, format, use ->
                uses += use
                val report = telemetry.nativeLoadReport(placement, use, format)
                val reporting = NativeAdLoader { unitId, size, onLoaded, onFailed ->
                    report.sent(testScheduler.currentTime)
                    loader.load(
                        unitId,
                        size,
                        {
                            report.succeeded(testScheduler.currentTime)
                            onLoaded(it)
                        },
                        {
                            report.failed(it, null, testScheduler.currentTime)
                            onFailed(it)
                        },
                    )
                }
                suspend { loadOneNativeAd(reporting, "unit", NativeAdSize.SMALL, 30_000, logger, placement.key) }
            },
        )
        val source = sources.source(PLACEMENT)
        repeat(2) { source.attach() }
        // The refresh ticks of slots A and B come at the same time; A wins the load.
        val a = claim(source, NativeLoadUse.REFRESH)
        val b = claim(source, NativeLoadUse.REFRESH)
        at(100)
        deliver()
        assertTrue(a.result() is NativeClaim.Ad)
        at(400)
        loader.fail(3)
        runCurrent()
        assertEquals(NativeClaim.NoAd, b.result())

        assertEquals(listOf(NativeLoadUse.REFRESH, NativeLoadUse.REFRESH), uses)
        val success = metrics.events.filterIsInstance<AdMetricEvent.LoadSuccess>().single()
        val fail = metrics.events.filterIsInstance<AdMetricEvent.LoadFail>().single()
        assertNull(success.loadAttempt)
        assertNull(fail.loadAttempt)
        assertEquals(100L, success.latencyMs)
        assertEquals(300L, fail.latencyMs)
        assertEquals(PLACEMENT.key, success.adPlacement)
        assertEquals(0, counter.failuresInARow(PLACEMENT.key))
    }

    @Test
    fun singleAdSourceFormats_areSmallAndCollapsible() {
        // CL-02: the collapsible layer of the bottom bar keeps one ad and one pending ad, like the slot (plan T-030).
        assertEquals(
            listOf(AdFormat.NATIVE_SMALL, AdFormat.NATIVE_COLLAPSIBLE),
            AdFormat.entries.filter { it.usesSingleAdSource() },
        )
    }

    @Test
    fun bottomRefreshDelay_is500ms() {
        // Section 5.7: the bottom slot refreshes 500 ms after the collapsible layer above it.
        assertEquals(500L, NativeAdPolicy.BOTTOM_REFRESH_DELAY_AFTER_COLLAPSIBLE_MILLIS)
        assertEquals(60_500L, bottomSlotFirstRefreshDelayMillis(collapsible = true))
        assertEquals(60_000L, bottomSlotFirstRefreshDelayMillis(collapsible = false))
    }

    @Test
    fun twoFormatsOfOnePlacement_getSeparateSources() = runTest {
        val sources = newSources()
        val small = sources.source(PLACEMENT, AdFormat.NATIVE_SMALL)
        val collapsible = sources.source(PLACEMENT, AdFormat.NATIVE_COLLAPSIBLE)
        // Control (L-055): the same pair gives the same source.
        assertSame(small, sources.source(PLACEMENT, AdFormat.NATIVE_SMALL))
        assertSame(small, sources.source(PLACEMENT))
        assertNotSame(small, collapsible)

        small.attach()
        claim(small)
        deliver()
        val pending = deliver()
        assertSame(pending, small.pending)
        collapsible.attach()
        val c = claim(collapsible)

        assertTrue("the collapsible source took the pending ad of the small one", !c.isCompleted)
        assertSame(pending, small.pending)
        assertEquals(
            listOf(AdFormat.NATIVE_SMALL, AdFormat.NATIVE_SMALL, AdFormat.NATIVE_COLLAPSIBLE),
            newLoads.map { it.second },
        )
        assertTrue(newLoads.all { it.first == PLACEMENT })
    }

    @Test
    fun loadUse_firstSingleAd_nextLoadRefresh_loserRetrySingleAd() = runTest {
        val sources = newSources()
        val source = sources.source(PLACEMENT)
        repeat(2) { source.attach() }
        claim(source)
        claim(source)
        deliver()
        deliver()

        assertEquals(
            listOf(NativeLoadUse.SINGLE_AD, NativeLoadUse.SINGLE_AD, NativeLoadUse.REFRESH),
            newLoads.map { it.third },
        )
    }

    private companion object {
        val PLACEMENT = AdPlacement.SAMPLE_NATIVE_SMALL

        /** The defaults of T-019, as read when Remote Config has no value. */
        val DEFAULT_FLAGS: AdsGlobalFlags = runBlocking { DefaultAdsFlags(FakeRemoteSettings()).global.first() }
    }
}
