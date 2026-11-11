package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.analytics.AdMetricEvent
import com.vglstudio.camera.pose.line.guide.core.analytics.RecordingAdFunnel
import com.vglstudio.camera.pose.line.guide.core.analytics.RecordingAdMetrics
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    private val globalFlags = MutableStateFlow(DEFAULT_FLAGS)
    private val enabledFlows = mutableMapOf<AdPlacement, MutableStateFlow<Boolean>>()
    private var mayRequest = true
    private var gateCalls = 0
    private lateinit var loads: FakeNativeLoads

    /** What each load was made for: the slot waiting for it (null for a load of the stock), format, use. */
    private val newLoads = mutableListOf<Triple<AdPlacement?, AdFormat, NativeLoadUse>>()

    private fun enabledOf(placement: AdPlacement) = enabledFlows.getOrPut(placement) { MutableStateFlow(true) }

    private fun TestScope.newSources(
        placements: List<AdPlacement> = listOf(PLACEMENT),
        unitId: (AdPlacement, AdFormat) -> String? = { _, format -> "unit-${format.code}" },
        gate: StateFlow<Boolean> = foreground,
        newLoad: ((NativePoolKey, NativeLoadUse, AdPlacement?) -> suspend () -> NativeSourceLoad)? = null,
    ): NativeAdSources {
        loads = FakeNativeLoads(loader, { testScheduler.currentTime }, logger)
        return NativeAdSources(
            placements = placements,
            unitId = unitId,
            enabled = { enabledOf(it) },
            canRequestAds = canRequestAds,
            globalFlags = globalFlags,
            newLoad = newLoad ?: { key, use, placement ->
                newLoads += Triple(placement, key.format, use)
                suspend { loads.load(use, placement) }
            },
            sendGate = {
                gateCalls++
                awaitSendAllowed(gate) { mayRequest }
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
                placement,
                sources.stopped(placement),
                foreground,
                { mayRequest },
                retryResets,
                { testScheduler.currentTime },
                logger,
                placement.key,
                intervalMillis = MutableStateFlow(60_000L),
            ).collect { states += it }
        }
        runCurrent()
        return states to job
    }

    private fun TestScope.claim(
        source: NativeAdSource,
        use: NativeLoadUse = NativeLoadUse.SINGLE_AD,
        placement: AdPlacement = PLACEMENT,
    ): Deferred<NativeClaim> = backgroundScope.async { source.claim(use, placement) }.also { runCurrent() }

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
            newLoad = { key, use, placement ->
                uses += use
                val report = telemetry.nativeLoadReport(placement, use, key.format)
                val reporting = NativeAdLoader { unitId, size, onLoaded, onFailed ->
                    report.sent(testScheduler.currentTime)
                    loader.load(
                        unitId,
                        size,
                        {
                            report.succeeded(testScheduler.currentTime)
                            onLoaded(it)
                        },
                        { code, message ->
                            report.failed(code, null, testScheduler.currentTime)
                            onFailed(code, message)
                        },
                    )
                }
                suspend {
                    NativeSourceLoad(
                        loadOneNativeAd(reporting, "unit", NativeAdSize.SMALL, 30_000, logger, PLACEMENT.key),
                    )
                }
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
        // Section 5.7: the bottom slot refreshes 500 ms after the collapsible layer above it; the count itself is the
        // key's (plan T-087, section 3.5), so only the extra is set here.
        assertEquals(500L, NativeAdPolicy.BOTTOM_REFRESH_DELAY_AFTER_COLLAPSIBLE_MILLIS)
        assertEquals(500L, bottomSlotFirstRefreshExtraMillis(collapsible = true))
        assertEquals(0L, bottomSlotFirstRefreshExtraMillis(collapsible = false))
    }

    @Test
    fun twoFormatsOfOnePlacement_getSeparateSources() = runTest {
        // The collapsible layer is on, so its stock may load (KB-33, criterion 6).
        globalFlags.value = COLLAP_ON_FLAGS
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
        // The two loads a slot waited for carry its placement; the next load after the claim is the stock's.
        assertEquals(listOf(PLACEMENT, null, PLACEMENT), newLoads.map { it.first })
    }

    @Test
    fun loadUse_firstSingleAd_nextLoadPreload_loserRetrySingleAd() = runTest {
        val sources = newSources()
        val source = sources.source(PLACEMENT)
        repeat(2) { source.attach() }
        claim(source)
        claim(source)
        deliver()
        deliver()

        assertEquals(
            listOf(NativeLoadUse.SINGLE_AD, NativeLoadUse.SINGLE_AD, NativeLoadUse.PRELOAD),
            newLoads.map { it.third },
        )
    }

    @Test
    fun twoPlacementsSameUnit_shareOneStock() = runTest {
        val sources = newSources(placements = listOf(PLACEMENT, OTHER))

        assertSame(sources.source(PLACEMENT), sources.source(OTHER))

        // Control (L-055): another unit for each placement gives two stocks.
        val apart = newSources(placements = listOf(PLACEMENT, OTHER), unitId = { p, f -> "unit-${p.key}-${f.code}" })
        assertNotSame(apart.source(PLACEMENT), apart.source(OTHER))
    }

    @Test
    fun stagingTestUnits_oneStockPerFormat() = runTest {
        val sources = newSources(
            placements = listOf(PLACEMENT, OTHER),
            unitId = { p, f -> GoogleTestAdUnitIds.of(p, f) },
        )
        val small = sources.source(PLACEMENT, AdFormat.NATIVE_SMALL)
        val collap = sources.source(PLACEMENT, AdFormat.NATIVE_COLLAPSIBLE)

        assertSame(small, sources.source(OTHER, AdFormat.NATIVE_SMALL))
        assertSame(collap, sources.source(OTHER, AdFormat.NATIVE_COLLAPSIBLE))
        assertNotSame(small, collap)
    }

    @Test
    fun productionUnits_smallPerPlacement_collapShared() = runTest {
        // Gallery placements are staging only, so ConfiguredAdUnitIds gives them no unit: each placement reads the
        // production table of its own, as two shipped placements would (`<key>=u1` and `<key>=u2`, one native_collap).
        val sources = newSources(
            placements = listOf(PLACEMENT, OTHER),
            unitId = productionUnits(
                "poses_bottom=u1;native_collap=c",
                "poses_bottom=u2;native_collap=c",
            ),
        )

        assertNotSame(sources.source(PLACEMENT, AdFormat.NATIVE_SMALL), sources.source(OTHER, AdFormat.NATIVE_SMALL))
        assertSame(
            sources.source(PLACEMENT, AdFormat.NATIVE_COLLAPSIBLE),
            sources.source(OTHER, AdFormat.NATIVE_COLLAPSIBLE),
        )

        // Control (L-055): the same unit for both placements gives one stock.
        val shared = newSources(
            placements = listOf(PLACEMENT, OTHER),
            unitId = productionUnits(
                "poses_bottom=u1;native_collap=c",
                "poses_bottom=u1;native_collap=c",
            ),
        )
        assertSame(shared.source(PLACEMENT, AdFormat.NATIVE_SMALL), shared.source(OTHER, AdFormat.NATIVE_SMALL))
    }

    @Test
    fun preloadSlots_collapsible_isOneSlotForPlanner() = runTest {
        val slots = newSources().preloadSlots(AdFormat.NATIVE_COLLAPSIBLE)

        assertEquals(1, slots.size)
        assertEquals(AdFormat.NATIVE_COLLAPSIBLE, slots.single().format)
    }

    @Test
    fun preloadSlots_onlyForSmallAndCollapsible() = runTest {
        val sources = newSources()

        listOf(AdFormat.BANNER, AdFormat.BANNER_MREC, AdFormat.BANNER_COLLAPSIBLE).forEach {
            assertTrue("$it", sources.preloadSlots(it).isEmpty())
        }
        // Control (L-055): native small has its slot.
        assertEquals(listOf(AdFormat.NATIVE_SMALL), sources.preloadSlots(AdFormat.NATIVE_SMALL).map { it.format })
    }

    @Test
    fun preloadSlot_fillsWithPreloadUse_noPlacement() = runTest {
        globalFlags.value = COLLAP_ON_FLAGS
        val sources = newSources()
        val slot = sources.preloadSlots(AdFormat.NATIVE_SMALL).single()

        slot.loadIfNeeded()
        runCurrent()

        assertEquals(
            listOf(Triple<AdPlacement?, AdFormat, NativeLoadUse>(null, AdFormat.NATIVE_SMALL, PRELOAD)),
            newLoads,
        )
        assertEquals(1, loads.sent.size)
        assertFalse(slot.settled())
        val ad = deliver()
        assertTrue(slot.settled())
        assertSame(ad, sources.source(PLACEMENT).pending)
    }

    @Test
    fun slotWithStockAd_firstStateLoaded_noRequestBeforeIt() = runTest {
        globalFlags.value = COLLAP_ON_FLAGS
        val sources = newSources()
        sources.preloadSlots(AdFormat.NATIVE_SMALL).single().loadIfNeeded()
        runCurrent()
        assertEquals(1, loads.sent.size)
        val stockAd = deliver()

        // The number of sends when each state of the slot came.
        val sentAt = mutableListOf<Int>()
        val states = mutableListOf<NativeAdState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            singleAdSlotStates(
                sources.source(PLACEMENT),
                PLACEMENT,
                sources.stopped(PLACEMENT),
                foreground,
                { mayRequest },
                retryResets,
                { testScheduler.currentTime },
                logger,
                PLACEMENT.key,
                intervalMillis = MutableStateFlow(60_000L),
            ).collect {
                sentAt += loads.sent.size
                states += it
            }
        }
        runCurrent()

        assertEquals(listOf(NativeAdState.Loading, NativeAdState.Loaded(stockAd)), states)
        assertEquals(listOf(1, 1), sentAt)
        assertEquals(listOf(PRELOAD, PRELOAD), loads.uses)
    }

    @Test
    fun slotLeaves_destroysItsAd_notStockAd() = runTest {
        val sources = newSources()
        val (states, job) = collectSlot(sources)
        val shown = deliver()
        val pending = deliver()
        assertSame(shown, (states.last() as NativeAdState.Loaded).ad)
        assertSame(pending, sources.source(PLACEMENT).pending)

        job.cancel()
        runCurrent()

        assertEquals(1, shown.destroyCount)
        assertEquals(0, pending.destroyCount)
        assertSame(pending, sources.source(PLACEMENT).pending)
    }

    @Test
    fun stockExpiresInBackground_reloadWaitsForForeground() = runTest {
        globalFlags.value = COLLAP_ON_FLAGS
        val sources = newSources()
        sources.preloadSlots(AdFormat.NATIVE_SMALL).single().loadIfNeeded()
        runCurrent()
        assertEquals(1, loads.sent.size)
        val ad = deliver()

        at(30 * 60_000L)
        foreground.value = false
        at(NativeAdPolicy.MAX_AGE_MILLIS)
        assertEquals(1, ad.destroyCount)
        assertEquals(1, loads.sent.size)

        at(NativeAdPolicy.MAX_AGE_MILLIS + 10_000)
        assertEquals(1, loads.sent.size)
        foreground.value = true
        runCurrent()
        assertEquals(2, loads.sent.size)
        assertEquals(NativeAdPolicy.MAX_AGE_MILLIS + 10_000, loads.times.last())
        assertEquals(PRELOAD, loads.uses.last())
    }

    @Test
    fun stockMayFill_rules() {
        val bottomOn = flagsOf(bottom = true)
        val bottomOff = flagsOf(bottom = false)
        val collapOff = flagsOf(bottom = true, collap = false)
        assertTrue(bottomOn.showNativeCollap)
        assertFalse(bottomOff.showNativeCollap)
        assertFalse(collapOff.showNativeCollap)
        val small = AdFormat.NATIVE_SMALL
        val collap = AdFormat.NATIVE_COLLAPSIBLE
        // Format, consent, a placement of the stock on, flags, a slot attached: may fill.
        val cases = listOf(
            Case(small, true, true, bottomOn, false, true),
            Case(small, true, true, bottomOff, false, false),
            Case(small, true, true, bottomOff, true, true),
            Case(small, false, true, bottomOn, true, false),
            Case(small, true, false, bottomOn, true, false),
            Case(collap, true, true, bottomOn, false, true),
            Case(collap, true, true, collapOff, true, false),
            Case(collap, true, true, bottomOff, true, false),
            Case(collap, false, true, bottomOn, true, false),
            Case(collap, true, false, bottomOn, false, false),
        )
        cases.forEachIndexed { i, case ->
            assertEquals(
                "case $i",
                case.expected,
                stockMayFill(case.format, case.consent, case.flags, case.membersOn, case.slotOnScreen),
            )
        }
    }

    @Test
    fun preloadSlot_notAllowed_sendsNothing_settled() = runTest {
        val sources = newSources()
        val slot = sources.preloadSlots(AdFormat.NATIVE_SMALL).single()

        slot.loadIfNeeded()
        runCurrent()
        assertTrue(loads.sent.isEmpty())
        assertTrue(slot.settled())

        // Control (L-055): with is_show_native_bottom on, the same slot sends one load.
        globalFlags.value = COLLAP_ON_FLAGS
        runCurrent()
        slot.loadIfNeeded()
        runCurrent()
        assertEquals(1, loads.sent.size)
    }

    @Test
    fun expiryReload_noSlot_flagOff_sendsNothing() = runTest {
        globalFlags.value = COLLAP_ON_FLAGS
        val sources = newSources()
        val slot = sources.preloadSlots(AdFormat.NATIVE_SMALL).single()
        slot.loadIfNeeded()
        runCurrent()
        assertEquals(1, loads.sent.size)
        val ad = deliver()

        at(30 * 60_000L)
        globalFlags.value = DEFAULT_FLAGS
        at(NativeAdPolicy.MAX_AGE_MILLIS)
        assertEquals(1, ad.destroyCount)
        assertEquals(1, loads.sent.size)

        // Control (L-055): with the flag on, the expiry of the next stock ad sends one load at once.
        globalFlags.value = COLLAP_ON_FLAGS
        runCurrent()
        slot.loadIfNeeded()
        runCurrent()
        val next = deliver()
        at(2 * NativeAdPolicy.MAX_AGE_MILLIS)
        assertEquals(1, next.destroyCount)
        assertEquals(3, loads.sent.size)
        assertEquals(PRELOAD, loads.uses.last())
        assertEquals(2 * NativeAdPolicy.MAX_AGE_MILLIS, loads.times.last())
    }

    @Test
    fun nextLoadAfterClaim_slotOnScreen_flagOff_sends() = runTest {
        // is_show_native_bottom off (the defaults): a slot on screen is a standalone one, using the stock.
        val sources = newSources()
        collectSlot(sources)
        assertEquals(1, loads.sent.size)

        deliver()

        assertEquals(2, loads.sent.size)
    }

    @Test
    fun nextLoadAfterClaim_consentWithdrawnWhileCovered_sendsNothing() = runTest {
        val sources = newSources()
        collectSlot(sources)
        // Covered: the gate of the test is one door for background and cover.
        foreground.value = false
        val ad = deliver()
        assertEquals(1, loads.sent.size)

        canRequestAds.value = false
        runCurrent()
        foreground.value = true
        runCurrent()

        assertEquals(1, loads.sent.size)
        assertEquals(1, ad.destroyCount)
        assertEquals(1L, sources.source(PLACEMENT).closed.value)
    }

    @Test
    fun nextLoadAfterClaim_allPlacementsOffWhileCovered_sendsNothing() = runTest {
        val sources = newSources()
        collectSlot(sources)
        foreground.value = false
        deliver()
        assertEquals(1, loads.sent.size)

        enabledOf(PLACEMENT).value = false
        runCurrent()
        foreground.value = true
        runCurrent()

        assertEquals(1, loads.sent.size)
        assertEquals(1L, sources.source(PLACEMENT).closed.value)
    }

    @Test
    fun collapFlagOff_layerClaim_blocked_sendsNothing() = runTest {
        globalFlags.value = flagsOf(bottom = true, collap = false)
        val sources = newSources()
        val layer = sources.source(PLACEMENT, AdFormat.NATIVE_COLLAPSIBLE)
        layer.attach()

        assertEquals(NativeClaim.Blocked, claim(layer).result())
        assertTrue(loads.sent.isEmpty())

        // Control (L-055): with the layer on, the same claim sends one load for the slot.
        globalFlags.value = COLLAP_ON_FLAGS
        runCurrent()
        val c = claim(layer)
        assertFalse(c.isCompleted)
        assertEquals(1, loads.sent.size)
        assertEquals(
            Triple<AdPlacement?, AdFormat, NativeLoadUse>(
                PLACEMENT,
                AdFormat.NATIVE_COLLAPSIBLE,
                NativeLoadUse.SINGLE_AD,
            ),
            newLoads.last(),
        )
    }

    @Test
    fun onePlacementOff_otherOn_stockKept_offSlotsEnd() = runTest {
        val sources = newSources(placements = listOf(PLACEMENT, OTHER))
        val (statesA, jobA) = collectSlot(sources, PLACEMENT)
        val adA = deliver()
        val adB = deliver()
        val (statesB, jobB) = collectSlot(sources, OTHER)
        assertSame(adB, (statesB.last() as NativeAdState.Loaded).ad)
        val kept = deliver()
        val stock = sources.source(PLACEMENT)
        assertSame(kept, stock.pending)

        enabledOf(PLACEMENT).value = false
        runCurrent()

        assertTrue(jobA.isCompleted)
        assertEquals(NativeAdState.Unavailable, statesA.last())
        assertEquals(1, adA.destroyCount)
        assertSame(kept, stock.pending)
        assertEquals(0, kept.destroyCount)
        assertEquals(0L, stock.closed.value)
        // The slot of the placement still on refreshes from the stock at its tick.
        at(60_000)
        assertFalse(jobB.isCompleted)
        assertSame(kept, (statesB.last() as NativeAdState.Loaded).ad)
        assertEquals(1, adB.destroyCount)
    }

    @Test
    fun placementOff_slotWaitingOnSharedStock_endsWithoutAd_adKept() = runTest {
        val sources = newSources(placements = listOf(PLACEMENT, OTHER))
        val (states, job) = collectSlot(sources, PLACEMENT)
        assertEquals(1, loads.sent.size)

        // The placement of the slot is switched off while its first load flies; the other one of the stock is on.
        enabledOf(PLACEMENT).value = false
        runCurrent()
        val ad = deliver()

        assertTrue(job.isCompleted)
        assertFalse(states.any { it is NativeAdState.Loaded })
        assertEquals(NativeAdState.Unavailable, states.last())
        // The ad is not lost: the stock keeps it, and a slot of the placement still on takes it at once.
        assertSame(ad, sources.source(PLACEMENT).pending)
        assertEquals(0, ad.destroyCount)
        val (other, _) = collectSlot(sources, OTHER)
        assertSame(ad, (other.last() as NativeAdState.Loaded).ad)
        // Then only the next load of the stock, after that claim.
        assertEquals(2, loads.sent.size)
    }

    @Test
    fun placementOffRightAfterAdHandedToSlot_slotEndsAndDestroysIt() = runTest {
        val sources = newSources(placements = listOf(PLACEMENT, OTHER))
        val states = mutableListOf<NativeAdState>()
        // The slot runs only when the test lets it: the ad can be handed to it before it runs again.
        val slotThread = HeldDispatcher()
        val job = backgroundScope.launch(slotThread) {
            singleAdSlotStates(
                sources.source(PLACEMENT),
                PLACEMENT,
                sources.stopped(PLACEMENT),
                foreground,
                { mayRequest },
                retryResets,
                { testScheduler.currentTime },
                logger,
                PLACEMENT.key,
                intervalMillis = MutableStateFlow(60_000L),
            ).collect { states += it }
        }
        slotThread.runAll()
        runCurrent()
        assertEquals(1, loads.sent.size)

        // The load hands its ad to the waiting slot; the placement is switched off (the other placement keeps the
        // stock open) before the slot runs again.
        val ad = deliver()
        assertTrue("the ad was not handed to the slot", sources.source(PLACEMENT).pending == null)
        enabledOf(PLACEMENT).value = false
        runCurrent()
        repeat(5) {
            slotThread.runAll()
            runCurrent()
        }

        assertTrue(job.isCompleted)
        assertEquals(NativeAdState.Unavailable, states.last())
        assertEquals(1, ad.destroyCount)
    }

    /** Runs what is dispatched to it only on [runAll]. */
    private class HeldDispatcher : CoroutineDispatcher() {
        private val tasks = ArrayDeque<Runnable>()

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            tasks.addLast(block)
        }

        fun runAll() {
            while (tasks.isNotEmpty()) tasks.removeFirst().run()
        }
    }

    @Test
    fun stockLogs_holdNoUnitId_ofStock() = runTest {
        globalFlags.value = COLLAP_ON_FLAGS
        val sources = newSources()
        sources.preloadSlots(AdFormat.NATIVE_SMALL).single().loadIfNeeded()
        runCurrent()
        deliver()
        val (_, job) = collectSlot(sources)
        deliver()
        job.cancel()
        runCurrent()
        at(NativeAdPolicy.MAX_AGE_MILLIS)

        val messages = logger.entries.map { it.message }
        // Control (L-055): the stock did log its preload, the slot leaving and the expiry.
        assertTrue(messages.toString(), messages.any { it.endsWith(" preload") })
        assertTrue(messages.toString(), messages.any { it.endsWith(" kept after last slot left") })
        assertTrue(messages.toString(), messages.any { it.endsWith(" expired") })
        // The ad units of this test are "unit-<format code>": none is ever logged (ADR-0019).
        assertFalse(messages.toString(), messages.any { it.contains("unit-") })
    }

    @Test
    fun preloadFill_inBackground_waitsForForeground() = runTest {
        globalFlags.value = COLLAP_ON_FLAGS
        foreground.value = false
        val sources = newSources()
        sources.preloadSlots(AdFormat.NATIVE_SMALL).single().loadIfNeeded()
        at(30_000)
        assertTrue(loads.sent.isEmpty())

        foreground.value = true
        runCurrent()
        assertEquals(listOf(30_000L), loads.times)
        assertEquals(listOf(PRELOAD), loads.uses)
    }

    @Test
    fun preloadFill_whileCovered_waitsUntilUncovered() = runTest {
        globalFlags.value = COLLAP_ON_FLAGS
        val window = InScreenAdWindow(logger).apply { onForeground(true) }
        window.onCovered(true)
        val sources = newSources(gate = window.open)
        sources.preloadSlots(AdFormat.NATIVE_SMALL).single().loadIfNeeded()
        at(30_000)
        assertTrue(loads.sent.isEmpty())

        window.onCovered(false)
        runCurrent()
        assertEquals(listOf(30_000L), loads.times)
        assertEquals(listOf(PRELOAD), loads.uses)
    }

    @Test
    fun fillAndWaitingClaim_newLoadGetsUseAndPlacement() = runTest {
        globalFlags.value = COLLAP_ON_FLAGS
        val sources = newSources()
        sources.preloadSlots(AdFormat.NATIVE_SMALL).single().loadIfNeeded()
        runCurrent()
        val layer = sources.source(PLACEMENT, AdFormat.NATIVE_COLLAPSIBLE)
        layer.attach()
        claim(layer)

        assertEquals(
            listOf(
                Triple<AdPlacement?, AdFormat, NativeLoadUse>(null, AdFormat.NATIVE_SMALL, PRELOAD),
                Triple<AdPlacement?, AdFormat, NativeLoadUse>(
                    PLACEMENT,
                    AdFormat.NATIVE_COLLAPSIBLE,
                    NativeLoadUse.SINGLE_AD,
                ),
            ),
            newLoads,
        )
    }

    /** [PLACEMENT] reads [first], [OTHER] reads [second], each as the shipped sample placement reads its table. */
    private fun productionUnits(first: String, second: String): (AdPlacement, AdFormat) -> String? {
        val units = mapOf(PLACEMENT to ConfiguredAdUnitIds(first), OTHER to ConfiguredAdUnitIds(second))
        return { placement, format -> units.getValue(placement).of(PLACEMENT, format) }
    }

    private class Case(
        val format: AdFormat,
        val consent: Boolean,
        val membersOn: Boolean,
        val flags: AdsGlobalFlags,
        val slotOnScreen: Boolean,
        val expected: Boolean,
    )

    private companion object {
        val PLACEMENT = AdPlacement.POSES_BOTTOM
        val OTHER = AdPlacement.GALLERY_NATIVE_SMALL
        val PRELOAD = NativeLoadUse.PRELOAD

        /** The defaults of T-019, as read when Remote Config has no value. */
        val DEFAULT_FLAGS: AdsGlobalFlags = runBlocking { DefaultAdsFlags(FakeRemoteSettings()).global.first() }

        /** `is_show_native_bottom` on, `is_show_native_collap` at its default (on): native bar and layer. */
        val COLLAP_ON_FLAGS: AdsGlobalFlags = flagsOf(bottom = true)

        fun flagsOf(bottom: Boolean, collap: Boolean? = null): AdsGlobalFlags {
            val settings = FakeRemoteSettings().apply {
                set(AdsSettings.IS_SHOW_NATIVE_BOTTOM, bottom)
                collap?.let { set(AdsSettings.IS_SHOW_NATIVE_COLLAP, it) }
            }
            return runBlocking { DefaultAdsFlags(settings).global.first() }
        }
    }
}
