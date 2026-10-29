package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The native card of a flow step on a virtual clock (plan T-032), with the real pool of T-028 and a fake loader:
 * two steps next to each other never show the same ad (OB-02), a new ad is claimed before the old one goes back
 * (P-01, P-01a), retries of P-06, two cards on one pool, and the ads given back when the card leaves.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NativeStepAdStatesTest {
    private val loader = FakeNativeAdLoader()
    private val logger = RecordingAppLogger()

    /** What happened, in order: "loaded <headline>" for each Loaded state, "release <headline>" for each release. */
    private val events = mutableListOf<String>()

    /** A request may be sent now; false makes the pool exhausted (P-05). */
    private var loadable = true

    /** The real pool, seen through a recorder of claims and releases. */
    private class RecordingPool(
        val pool: NativeAdPool,
        private val events: MutableList<String>,
        private val now: () -> Long,
    ) : PooledNativeAds {
        val claimTimes = mutableListOf<Long>()
        val released = mutableListOf<LoadedNativeAd>()

        override fun claim(holder: AdPlacement): LoadedNativeAd? {
            claimTimes += now()
            return pool.claim(holder)
        }

        override fun release(ad: LoadedNativeAd) {
            released += ad
            events += "release ${ad.assets.headline}"
            pool.release(ad)
        }

        override val exhausted: Boolean get() = pool.exhausted
    }

    private fun TestScope.newPool(): RecordingPool {
        val loads = FakePoolLoads(loader, { testScheduler.currentTime }, logger)
        val pool = NativeAdPool(
            key = NativePoolKey(AdFormat.NATIVE_STEP, FakePoolLoads.FAKE_POOL_UNIT_ID),
            capacity = requireNotNull(AdFormat.NATIVE_STEP.poolCapacity()),
            load = { loads.load() },
            sendGate = { loadable },
            canLoadNow = { loadable },
            canShowNow = { true },
            retryResets = MutableStateFlow(0),
            clock = { testScheduler.currentTime },
            scope = backgroundScope,
            logger = logger,
            logKey = "native_step#1",
        )
        return RecordingPool(pool, events, { testScheduler.currentTime })
    }

    private fun ad(number: Int) = FakeLoadedNativeAd(sampleNativeAdAssets.copy(headline = "Ad $number"))

    /** Starts the pool's first request and answers it with [ads], one after the other. */
    private fun TestScope.fill(pool: RecordingPool, vararg ads: FakeLoadedNativeAd) {
        pool.pool.refill()
        runCurrent()
        ads.forEach {
            loader.deliver(it)
            runCurrent()
        }
    }

    private fun TestScope.deliver(ad: FakeLoadedNativeAd) {
        loader.deliver(ad)
        runCurrent()
    }

    private class Card(val steps: MutableStateFlow<Any>, val states: MutableList<NativeAdState>, val job: Job)

    private fun TestScope.collect(pool: RecordingPool, firstStep: Any = 1): Card {
        val steps = MutableStateFlow(firstStep)
        val states = mutableListOf<NativeAdState>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            stepAdStates(pool, AdPlacement.GALLERY_NATIVE_MEDIUM, steps, logger, "native_onboarding").collect {
                states += it
                if (it is NativeAdState.Loaded) events += "loaded ${it.ad.assets.headline}"
            }
        }
        runCurrent()
        return Card(steps, states, job)
    }

    private fun TestScope.at(millis: Long) {
        advanceTimeBy(millis - testScheduler.currentTime)
        runCurrent()
    }

    private fun Card.loadedAds(): List<LoadedNativeAd> = states.filterIsInstance<NativeAdState.Loaded>().map { it.ad }

    @Test
    fun stepChange_neverShowsSameAd() = runTest {
        val pool = newPool()
        val a = ad(1)
        val b = ad(2)
        fill(pool, a, b, ad(3))
        val card = collect(pool)
        assertEquals(listOf<LoadedNativeAd>(a), card.loadedAds())

        card.steps.value = 2
        runCurrent()

        assertEquals(listOf<LoadedNativeAd>(a, b), card.loadedAds())
        assertNotSame(card.loadedAds()[0], card.loadedAds()[1])
        // The new ad first, then the old one goes back: the pool never had the old one free to hand out again.
        assertEquals(listOf("loaded Ad 1", "loaded Ad 2", "release Ad 1"), events)
    }

    @Test
    fun stepChange_onlyOldAdInRotation_waitsInsteadOfReusing() = runTest {
        val pool = newPool()
        val a = ad(1)
        // Only A came; the next request stays open.
        fill(pool, a)
        val card = collect(pool)
        assertEquals(NativeAdState.Loaded(a), card.states.last())
        assertFalse(a.impressionRecorded)

        card.steps.value = 2
        runCurrent()
        val sinceChange = card.states.size - 1
        assertEquals(NativeAdState.Loading, card.states.last())
        at(5_000)
        assertEquals(
            "no ad but A for 5 s: the card waits",
            listOf<NativeAdState>(NativeAdState.Loading),
            card.states.drop(sinceChange).distinct(),
        )
        assertTrue("A is still held", pool.released.isEmpty())

        val b = ad(2)
        deliver(b)
        at(5_500)
        assertEquals(NativeAdState.Loaded(b), card.states.last())
        assertEquals(listOf("loaded Ad 1", "loaded Ad 2", "release Ad 1"), events)
    }

    @Test
    fun releaseBeforeClaim_wouldHandTheSameAdBack_control() = runTest {
        // Control of the test above: with A given back before the next claim, the real pool hands A out again; so
        // only claiming first keeps A off the next step.
        val pool = newPool()
        val a = ad(1)
        fill(pool, a)
        assertSame(a, pool.pool.claim(AdPlacement.GALLERY_NATIVE_MEDIUM))
        pool.pool.release(a)
        assertSame(a, pool.pool.claim(AdPlacement.GALLERY_NATIVE_MEDIUM))
    }

    @Test
    fun rapidSteps_keepsFirstAdOutOfRotation() = runTest {
        val pool = newPool()
        val a = ad(1)
        fill(pool, a)
        val card = collect(pool)

        card.steps.value = 2
        runCurrent()
        at(300)
        card.steps.value = 3
        runCurrent()
        at(1_000)
        assertEquals("step 3 does not get A either", listOf<LoadedNativeAd>(a), card.loadedAds())

        val b = ad(2)
        deliver(b)
        at(2_000)
        assertEquals(listOf<LoadedNativeAd>(a, b), card.loadedAds())
        assertEquals(listOf<LoadedNativeAd>(a), pool.released)
        assertEquals(listOf("loaded Ad 1", "loaded Ad 2", "release Ad 1"), events)
    }

    @Test
    fun twoCells_samePool_differentAds() = runTest {
        val pool = newPool()
        val a = ad(1)
        val b = ad(2)
        val c = ad(3)
        fill(pool, a, b, c)
        val first = collect(pool)
        val second = collect(pool)
        assertEquals(listOf<LoadedNativeAd>(a), first.loadedAds())
        assertEquals(listOf<LoadedNativeAd>(b), second.loadedAds())

        first.steps.value = 2
        runCurrent()

        assertEquals("the first card never takes the ad of the second", listOf<LoadedNativeAd>(a, c), first.loadedAds())
        assertEquals(listOf<LoadedNativeAd>(b), second.loadedAds())
    }

    @Test
    fun exhausted_emitsUnavailable_retriesEvery2s_lateAdShows() = runTest {
        val pool = newPool()
        loadable = false
        val card = collect(pool)
        at(4_500)
        assertEquals(listOf(0L, 2_000L, 4_000L), pool.claimTimes)
        assertEquals(listOf(NativeAdState.Loading, NativeAdState.Unavailable), card.states)

        // A request can be sent again: the next claim sends it, then the card asks every 500 ms while it loads.
        loadable = true
        at(6_700)
        assertEquals(listOf(0L, 2_000L, 4_000L, 6_000L, 6_500L), pool.claimTimes)
        val a = ad(1)
        deliver(a)
        at(7_000)
        assertEquals(listOf(NativeAdState.Loading, NativeAdState.Unavailable, NativeAdState.Loaded(a)), card.states)
        at(20_000)
        assertEquals("no claim once the card has its ad", 6, pool.claimTimes.size)
    }

    @Test
    fun cancel_releasesCurrentAndPrevious() = runTest {
        // Left while waiting for the ad of step 2: A, still held, goes back.
        val waiting = newPool()
        val a = ad(1)
        fill(waiting, a)
        val first = collect(waiting)
        first.steps.value = 2
        at(1_000)
        assertTrue(waiting.released.isEmpty())
        first.job.cancel()
        runCurrent()
        assertEquals(listOf<LoadedNativeAd>(a), waiting.released)

        // Left with the ad of step 2: B goes back, once, after A went back at the step change.
        val full = newPool()
        val c = ad(3)
        val d = ad(4)
        fill(full, c, d, ad(5))
        val second = collect(full)
        second.steps.value = 2
        runCurrent()
        assertEquals(listOf<LoadedNativeAd>(c), full.released)
        second.job.cancel()
        runCurrent()
        assertEquals(listOf<LoadedNativeAd>(c, d), full.released)
        assertEquals(0, c.destroyCount + d.destroyCount)
    }

    @Test
    fun sameStepAgain_keepsAd() = runTest {
        val pool = newPool()
        val a = ad(1)
        fill(pool, a, ad(2), ad(3))
        val card = collect(pool)

        card.steps.value = 1
        runCurrent()

        assertEquals(listOf<LoadedNativeAd>(a), card.loadedAds())
        assertTrue(pool.released.isEmpty())
    }

    @Test
    fun logs_stepChangeAndNewAd_neverTheStepKey() = runTest {
        val pool = newPool()
        fill(pool, ad(1), ad(2), ad(3))
        val card = collect(pool, firstStep = "secret_step_key")
        card.steps.value = "other_secret_step"
        runCurrent()

        val messages = logger.entries.filter { it.tag == "Ads" }.map { it.message }
        assertTrue(messages.toString(), messages.contains("Step native_onboarding: step changed"))
        assertTrue(messages.toString(), messages.contains("Step native_onboarding: got a new ad"))
        assertFalse(messages.toString(), messages.any { it.contains("secret") })
    }
}
