package com.vglstudio.camera.pose.line.guide.core.ads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The wait of the loading screen for the app open ad (A-03, A-04, A-06, KB-13 criteria 4 and 5), in virtual time. */
@OptIn(ExperimentalCoroutinesApi::class)
class StartupAdWaitTest {
    /** The state of the probe at each virtual time; `null` times never come. */
    private class FakeStartupAdProbe(
        private val scope: TestScope,
        private val consentFinishedAt: Long? = null,
        private val consentGateOpen: Boolean = true,
        private val sdkReadyAt: Long? = null,
        private val adReadyAt: Long? = null,
        private val chosenUntil: Long? = null,
        private val placement: AdPlacement = AdPlacement.OPEN_APP,
    ) : StartupAdProbe {
        val ensureLoadingAt = mutableListOf<Long>()
        val notReady = mutableListOf<Pair<AdPlacement, Long>>()

        override fun nowMillis(): Long = scope.currentTime

        private fun reached(at: Long?) = at != null && nowMillis() >= at

        override val consentFinished: Boolean get() = reached(consentFinishedAt)

        override val consentOpen: Boolean get() = consentFinished && consentGateOpen

        override val sdkReady: Boolean get() = reached(sdkReadyAt)

        override fun chosen(): AdPlacement? = if (chosenUntil != null && nowMillis() >= chosenUntil) null else placement

        override fun isReady(placement: AdPlacement): Boolean = reached(adReadyAt)

        override fun ensureLoading(placement: AdPlacement) {
            ensureLoadingAt += nowMillis()
        }

        override fun recordNotReady(placement: AdPlacement) {
            notReady += placement to nowMillis()
        }
    }

    /** Runs the wait from virtual time [callAt]; returns its result and the virtual time it returned at. */
    private suspend fun TestScope.wait(probe: StartupAdProbe, callAt: Long = 0): Pair<AdPlacement?, Long> {
        if (callAt > 0) {
            advanceTimeBy(callAt)
            runCurrent()
        }
        val result = awaitStartupAd(probe, startedAtMillis = 0)
        return result to currentTime
    }

    @Test
    fun consentNotFinished_givesUpAt10s_recordsNothing() = runTest {
        val probe = FakeStartupAdProbe(this)
        assertEquals(null to 10_000L, wait(probe))
        assertEquals(emptyList<Pair<AdPlacement, Long>>(), probe.notReady)
        assertEquals(emptyList<Long>(), probe.ensureLoadingAt)
    }

    @Test
    fun consentClosed_givesUpAtOnce_recordsNothing() = runTest {
        val probe = FakeStartupAdProbe(this, consentFinishedAt = 2_000, consentGateOpen = false, sdkReadyAt = 0)
        assertEquals(null to 2_000L, wait(probe))
        assertEquals(emptyList<Pair<AdPlacement, Long>>(), probe.notReady)
        assertEquals(emptyList<Long>(), probe.ensureLoadingAt)
    }

    @Test
    fun sdkNotReady_givesUpAt35s_recordsNotReadyOnce() = runTest {
        val probe = FakeStartupAdProbe(this, consentFinishedAt = 1_000)
        assertEquals(null to 35_000L, wait(probe))
        assertEquals(listOf(AdPlacement.OPEN_APP to 35_000L), probe.notReady)
    }

    @Test
    fun adNotReady_givesUpAt40s_probesEveryTick_recordsOnce() = runTest {
        val probe = FakeStartupAdProbe(this, consentFinishedAt = 1_000, sdkReadyAt = 5_000)
        assertEquals(null to 40_000L, wait(probe))
        // One probe per tick from 5 000 to 39 750, nothing recorded meanwhile (A-04).
        assertEquals((5_000L..39_750L step 250).toList(), probe.ensureLoadingAt)
        assertEquals(140, probe.ensureLoadingAt.size)
        assertEquals(listOf(AdPlacement.OPEN_APP to 40_000L), probe.notReady)
    }

    @Test
    fun adReady_returnsAtNextTick() = runTest {
        val probe = FakeStartupAdProbe(this, consentFinishedAt = 1_000, sdkReadyAt = 5_000, adReadyAt = 12_100)
        assertEquals(AdPlacement.OPEN_APP to 12_250L, wait(probe))
        assertEquals(emptyList<Pair<AdPlacement, Long>>(), probe.notReady)
    }

    @Test
    fun readyAfterDeadlines_showsAtOnce() = runTest {
        val ready = FakeStartupAdProbe(this, consentFinishedAt = 1_000, sdkReadyAt = 5_000, adReadyAt = 30_000)
        assertEquals(AdPlacement.OPEN_APP to 60_000L, wait(ready, callAt = 60_000))
        assertEquals(emptyList<Pair<AdPlacement, Long>>(), ready.notReady)
        // Control (L-055): at the same time with no ad, the wait gives up at once and records one opportunity.
        val notReady = FakeStartupAdProbe(this, consentFinishedAt = 1_000, sdkReadyAt = 5_000)
        assertEquals(null to 60_000L, wait(notReady))
        assertEquals(listOf(AdPlacement.OPEN_APP to 60_000L), notReady.notReady)
    }

    @Test
    fun keyOff_returnsAtOnce_withoutProbing() = runTest {
        val probe = FakeStartupAdProbe(this, chosenUntil = 0)
        assertEquals(null to 0L, wait(probe))
        assertEquals(emptyList<Long>(), probe.ensureLoadingAt)
        assertEquals(emptyList<Pair<AdPlacement, Long>>(), probe.notReady)
        // Control (L-055): with the key on, the same probe waits for consent until 10 s.
        assertEquals(null to 10_000L, wait(FakeStartupAdProbe(this)))
    }

    @Test
    fun keyTurnedOffDuringWait_returnsNextTick() = runTest {
        val probe = FakeStartupAdProbe(this, consentFinishedAt = 1_000, sdkReadyAt = 5_000, chosenUntil = 7_000)
        assertEquals(null to 7_000L, wait(probe))
        assertEquals(emptyList<Pair<AdPlacement, Long>>(), probe.notReady)
        // The ad was probed until the key went off.
        assertEquals((5_000L..6_750L step 250).toList(), probe.ensureLoadingAt)
        assertNull(probe.chosen())
    }

    @Test
    fun interPlacement_isProbedAndRecorded() = runTest {
        val probe = FakeStartupAdProbe(
            this,
            consentFinishedAt = 1_000,
            sdkReadyAt = 5_000,
            placement = AdPlacement.OPEN_APP_INTER,
        )
        assertEquals(null to 40_000L, wait(probe))
        assertEquals(listOf(AdPlacement.OPEN_APP_INTER to 40_000L), probe.notReady)
    }
}
