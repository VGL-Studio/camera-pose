package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The groups of the preload plan on a virtual clock: order, 6 s cap, 200 ms checks (S-20, S-22). */
@OptIn(ExperimentalCoroutinesApi::class)
class PreloadPlanRunnerTest {
    private val logger = RecordingAppLogger()

    private fun TestScope.now(): Long = testScheduler.currentTime

    private fun TestScope.slot(format: AdFormat, settleAt: Long? = null) = FakePreloadSlot(format, { now() }, settleAt)

    private fun TestScope.runPlan(plan: AdPreloadPlan, slots: List<PreloadSlot>) {
        val runner = PreloadPlanRunner(MonotonicClock { now() }, logger)
        backgroundScope.launch { runner.run(plan, { format -> slots.filter { it.format == format } }) }
        runCurrent()
    }

    /** Moves the virtual clock to [millis] after the start and runs what is due. */
    private fun TestScope.at(millis: Long) {
        advanceTimeBy(millis - testScheduler.currentTime)
        runCurrent()
    }

    private val twoGroups = AdPreloadPlan(listOf(setOf(AdFormat.APP_OPEN), setOf(AdFormat.INTERSTITIAL)))

    @Test
    fun nextGroupStartsWhenPreviousSettles() = runTest {
        val first = slot(AdFormat.APP_OPEN, settleAt = 500)
        val second = slot(AdFormat.INTERSTITIAL, settleAt = 0)
        runPlan(twoGroups, listOf(first, second))

        assertEquals(listOf(0L), first.loadCalls)
        at(400)
        assertEquals(emptyList<Long>(), second.loadCalls)
        at(600)
        assertEquals(listOf(600L), second.loadCalls)
        assertTrue(logger.entries.any { it.tag == "Ads" && it.message == "Preload group 1/2 moved on: settled" })
    }

    @Test
    fun unsettledGroup_movesOnAfter6s() = runTest {
        val first = slot(AdFormat.APP_OPEN)
        val second = slot(AdFormat.INTERSTITIAL, settleAt = 0)
        runPlan(twoGroups, listOf(first, second))

        at(5_800)
        assertEquals(emptyList<Long>(), second.loadCalls)
        at(6_000)
        assertEquals(listOf(6_000L), second.loadCalls)
        assertTrue(logger.entries.any { it.tag == "Ads" && it.message == "Preload group 1/2 moved on: timed out" })
    }

    @Test
    fun failedSlot_countsAsSettled() = runTest {
        // The fake settles at 300 the way a slot that failed (or waits for a retry) does.
        val first = slot(AdFormat.APP_OPEN, settleAt = 300)
        val second = slot(AdFormat.INTERSTITIAL, settleAt = 0)
        runPlan(twoGroups, listOf(first, second))

        at(399)
        assertEquals(emptyList<Long>(), second.loadCalls)
        at(400)
        assertEquals(listOf(400L), second.loadCalls)
    }

    @Test
    fun settledAtStart_movesOnAtOnce() = runTest {
        val first = slot(AdFormat.APP_OPEN, settleAt = 0)
        val second = slot(AdFormat.INTERSTITIAL, settleAt = 0)
        val third = slot(AdFormat.NATIVE_SMALL, settleAt = 0)
        val plan = AdPreloadPlan(
            listOf(setOf(AdFormat.APP_OPEN), setOf(AdFormat.INTERSTITIAL), setOf(AdFormat.NATIVE_SMALL)),
        )
        runPlan(plan, listOf(first, second, third))

        assertEquals(listOf(0L), second.loadCalls)
        assertEquals(listOf(0L), third.loadCalls)
        assertTrue(logger.entries.any { it.tag == "Ads" && it.message == "Preload plan finished" })
    }

    @Test
    fun formatWithoutSlots_isSettled() = runTest {
        val second = slot(AdFormat.INTERSTITIAL, settleAt = 0)
        // APP_OPEN has no slot: the first group has nothing to wait for.
        runPlan(twoGroups, listOf(second))

        assertEquals(listOf(0L), second.loadCalls)
        assertTrue(logger.entries.any { it.tag == "Ads" && it.message == "Preload group 1/2 moved on: settled" })
    }

    @Test
    fun slotNotInPlan_isNeverLoaded() = runTest {
        val banner = slot(AdFormat.BANNER, settleAt = 0)
        val interstitial = slot(AdFormat.INTERSTITIAL, settleAt = 0)
        val plan = AdPreloadPlan(listOf(setOf(AdFormat.INTERSTITIAL)))
        // The slots of the banner exist too; only the plan decides what loads.
        runPlan(plan, listOf(banner, interstitial))
        at(10_000)

        assertEquals(0, banner.loadCalls.size)
        // Control: the slot in the plan loads once.
        assertEquals(1, interstitial.loadCalls.size)
        assertTrue(logger.entries.any { it.tag == "Ads" && it.message == "Preload group 1/1 started: INTERSTITIAL" })
    }

    @Test
    fun poolSlot_inSecondGroup_startsAfterFirstSettles() = runTest {
        val loader = FakeNativeAdLoader()
        val loads = FakePoolLoads(loader, { now() }, logger)
        val pools = NativeAdPools(
            placements = listOf(AdPlacement.GALLERY_NATIVE_MEDIUM),
            unitId = { "unit" },
            enabled = { flowOf(true) },
            canRequestAds = MutableStateFlow(true),
            newPool = { key, _ ->
                NativeAdPool(
                    key = key,
                    capacity = 3,
                    load = { loads.load() },
                    sendGate = { true },
                    canLoadNow = { true },
                    canShowNow = { true },
                    retryResets = MutableStateFlow(0),
                    clock = { now() },
                    scope = backgroundScope,
                    logger = logger,
                    logKey = "native_medium#1",
                )
            },
            scope = backgroundScope,
            logger = logger,
        )
        runCurrent()
        val plan = AdPreloadPlan.fromPlacements(listOf(AdPlacement.OPEN_APP, AdPlacement.GALLERY_NATIVE_MEDIUM))
        assertEquals(listOf(setOf(AdFormat.APP_OPEN), setOf(AdFormat.NATIVE_MEDIUM)), plan.groups)
        val appOpen = slot(AdFormat.APP_OPEN)
        val runner = PreloadPlanRunner(MonotonicClock { now() }, logger)
        backgroundScope.launch {
            runner.run(plan) { format -> listOf(appOpen).filter { it.format == format } + pools.preloadSlots(format) }
        }
        runCurrent()

        at(5_800)
        assertEquals(emptyList<Long>(), loads.times)
        at(6_000)
        assertEquals(listOf(6_000L), loads.times)
    }

    @Test
    fun firstGroupNativeStep_noUnitId_secondGroupStartsAtOnce() = runTest {
        // HD-02 with `gallery_native_step` (plan T-036): in production the step placement has no ad unit, so its pool
        // slot is settled from the start, like the pool slot of T-028 with no pool; only app open is waited for.
        val plan = AdPreloadPlan(
            listOf(setOf(AdFormat.APP_OPEN, AdFormat.NATIVE_STEP), setOf(AdFormat.INTERSTITIAL)),
        )
        val appOpen = slot(AdFormat.APP_OPEN, settleAt = 300)
        val step = slot(AdFormat.NATIVE_STEP, settleAt = 0)
        val interstitial = slot(AdFormat.INTERSTITIAL, settleAt = 0)
        runPlan(plan, listOf(appOpen, step, interstitial))

        at(399)
        assertEquals(emptyList<Long>(), interstitial.loadCalls)
        // The next check after app open settled at 300.
        at(400)
        assertEquals(listOf(400L), interstitial.loadCalls)
        assertTrue(
            logger.entries.any { it.tag == "Ads" && it.message == "Preload group 1/2 started: APP_OPEN, NATIVE_STEP" },
        )
    }

    @Test
    fun firstGroupNativeStep_unsettled_secondGroupWaits6s() = runTest {
        // Control (L-055): a step slot that never settles holds the second group until the cap of 6 s.
        val plan = AdPreloadPlan(
            listOf(setOf(AdFormat.APP_OPEN, AdFormat.NATIVE_STEP), setOf(AdFormat.INTERSTITIAL)),
        )
        val appOpen = slot(AdFormat.APP_OPEN, settleAt = 300)
        val step = slot(AdFormat.NATIVE_STEP)
        val interstitial = slot(AdFormat.INTERSTITIAL, settleAt = 0)
        runPlan(plan, listOf(appOpen, step, interstitial))

        at(5_800)
        assertEquals(emptyList<Long>(), interstitial.loadCalls)
        at(6_000)
        assertEquals(listOf(6_000L), interstitial.loadCalls)
    }
}
