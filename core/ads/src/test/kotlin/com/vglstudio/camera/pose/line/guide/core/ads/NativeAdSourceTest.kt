package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
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
 * The single-ad source of one native placement on a virtual clock: claim, next load, the loser's one retry, late
 * ads, close, expiry at 55 minutes (O-01 to O-04, O-09), and the send gate of paths (b) and (c) (L-072; plan T-040).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NativeAdSourceTest {
    private val loader = FakeNativeAdLoader()
    private val logger = RecordingAppLogger()
    private val foreground = MutableStateFlow(true)
    private var mayRequest = true
    private var allowed = true
    private lateinit var loads: FakeNativeLoads

    /** The window of the in-screen ads as AdsController builds it, in foreground (KB-27, plan T-054). */
    private val window = InScreenAdWindow(logger).apply { onForeground(true) }

    private fun TestScope.newSource(gate: StateFlow<Boolean> = foreground): NativeAdSource {
        loads = FakeNativeLoads(loader, { testScheduler.currentTime }, logger)
        return NativeAdSource(
            load = { use -> loads.load(use) },
            sendGate = { awaitSendAllowed(gate) { mayRequest } },
            clock = { testScheduler.currentTime },
            // A supervisor like the scope of AdsController; an uncaught error fails the test.
            scope = backgroundScope,
            logger = logger,
            placementKey = KEY,
            allowed = { allowed },
        )
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

    private fun TestScope.fail(code: Int = 3) {
        loader.fail(code)
        runCurrent()
    }

    /** Moves the virtual clock to [millis] after the start and runs what is due. */
    private fun TestScope.at(millis: Long) {
        advanceTimeBy(millis - testScheduler.currentTime)
        runCurrent()
    }

    private fun Deferred<NativeClaim>.result(): NativeClaim {
        assertTrue("the claim has not returned", isCompleted)
        return getCompleted()
    }

    private fun Deferred<NativeClaim>.ad(): LoadedNativeAd {
        val claim = result()
        assertTrue("expected an ad, got $claim", claim is NativeClaim.Ad)
        return (claim as NativeClaim.Ad).ad
    }

    /** A first claim gets ad1, the next load brings ad2, kept as the pending ad. */
    private fun TestScope.withPendingAd(source: NativeAdSource): Pair<FakeLoadedNativeAd, FakeLoadedNativeAd> {
        val first = claim(source)
        val ad1 = deliver()
        assertSame(ad1, first.ad())
        val ad2 = deliver()
        assertSame("ad2 is not the pending ad", ad2, source.pending)
        return ad1 to ad2
    }

    @Test
    fun claim_returnsPendingAd_andStartsNextLoad() = runTest {
        val source = newSource()
        source.attach()
        val (_, ad2) = withPendingAd(source)
        assertEquals(2, loads.sent.size)
        assertFalse(source.isLoading)

        val second = claim(source)

        assertSame(ad2, second.ad())
        assertNull(source.pending)
        assertEquals(listOf(NativeLoadUse.SINGLE_AD, NativeLoadUse.REFRESH, NativeLoadUse.REFRESH), loads.uses)
        assertTrue(source.isLoading)
    }

    @Test
    fun twoSlots_samePlacement_getDifferentAds() = runTest {
        val source = newSource()
        // Slot A is on screen with its ad when slot B of the same placement comes (L-068).
        source.attach()
        val a = claim(source)
        val ad1 = deliver()
        val ad2 = deliver()
        source.attach()
        val b = claim(source)

        assertSame(ad1, a.ad())
        assertSame(ad2, b.ad())
        assertNotSame(a.ad(), b.ad())
    }

    @Test
    fun sameAd_neverGivenTwice() = runTest {
        val source = newSource()
        repeat(4) { source.attach() }
        val handed = mutableListOf<LoadedNativeAd>()
        val c1 = claim(source)
        val c2 = claim(source)
        deliver()
        deliver()
        handed += c1.ad()
        handed += c2.ad()
        deliver()
        handed += claim(source).ad()
        deliver()
        handed += claim(source).ad()

        assertEquals(4, handed.size)
        assertEquals(handed.size, handed.toSet().size)
    }

    @Test
    fun twoClaimersOneLoad_loserRetriesOnce_getsSecondAd() = runTest {
        val source = newSource()
        repeat(2) { source.attach() }
        val a = claim(source)
        val b = claim(source)
        assertEquals("two claimers wait on one load", 1, loads.sent.size)

        val ad1 = deliver()
        assertSame(ad1, a.ad())
        assertFalse("the loser gave up before its retry", b.isCompleted)
        assertEquals(2, loads.sent.size)
        assertTrue(logger.entries.any { it.message == "Native $KEY claim lost, retry once" })

        val ad2 = deliver()
        assertSame(ad2, b.ad())
    }

    @Test
    fun loserRetryFails_getsNoAd_noThirdLoad() = runTest {
        val source = newSource()
        repeat(2) { source.attach() }
        val a = claim(source)
        val b = claim(source)
        deliver()
        assertTrue(a.result() is NativeClaim.Ad)
        assertEquals(2, loads.sent.size)

        fail()

        assertEquals(NativeClaim.NoAd, b.result())
        at(60_000)
        assertEquals(2, loads.sent.size)
        assertFalse(source.isLoading)
    }

    @Test
    fun threeClaimers_loserRetriesSequential_oneInFlight() = runTest {
        val source = newSource()
        repeat(3) { source.attach() }
        val a = claim(source)
        val b = claim(source)
        val c = claim(source)
        assertEquals(1, loads.sent.size)

        val ad1 = deliver()
        assertSame(ad1, a.ad())
        assertEquals("one retry at a time", 2, loads.sent.size)
        val ad2 = deliver()
        assertSame(ad2, b.ad())
        assertFalse(c.isCompleted)
        assertEquals(3, loads.sent.size)
        val ad3 = deliver()
        assertSame(ad3, c.ad())

        assertEquals(1, loader.maxOpen)
        assertEquals(
            listOf(NativeLoadUse.SINGLE_AD, NativeLoadUse.SINGLE_AD, NativeLoadUse.SINGLE_AD, NativeLoadUse.REFRESH),
            loads.uses,
        )
    }

    @Test
    fun loserRetry_inBackground_waitsForForeground() = runTest {
        val source = newSource()
        repeat(2) { source.attach() }
        val a = claim(source)
        val b = claim(source)
        foreground.value = false
        deliver()
        assertTrue(a.result() is NativeClaim.Ad)

        at(30_000)
        assertEquals(1, loads.sent.size)
        assertFalse(b.isCompleted)

        foreground.value = true
        runCurrent()
        assertEquals(listOf(0L, 30_000L), loads.times)
        val ad2 = deliver()
        assertSame(ad2, b.ad())
    }

    @Test
    fun loserRetry_whileCovered_waitsUntilUncovered() = runTest {
        val source = newSource(gate = window.open)
        repeat(2) { source.attach() }
        val a = claim(source)
        val b = claim(source)
        window.onCovered(true)
        deliver()
        assertTrue(a.result() is NativeClaim.Ad)

        at(30_000)
        assertEquals(1, loads.sent.size)
        assertFalse(b.isCompleted)

        // Control (L-055): the cover goes, the one retry of the loser is sent.
        window.onCovered(false)
        runCurrent()
        assertEquals(listOf(0L, 30_000L), loads.times)
        val ad2 = deliver()
        assertSame(ad2, b.ad())
    }

    @Test
    fun nextLoadAfterClaim_whileCovered_waitsUntilUncovered() = runTest {
        val source = newSource(gate = window.open)
        source.attach()
        val a = claim(source)
        window.onCovered(true)
        deliver()
        assertTrue(a.result() is NativeClaim.Ad)

        at(30_000)
        assertEquals(1, loads.sent.size)

        // Control (L-055): the cover goes, the next load is sent once.
        window.onCovered(false)
        runCurrent()
        assertEquals(listOf(0L, 30_000L), loads.times)
        assertEquals(NativeLoadUse.REFRESH, loads.uses.last())
    }

    @Test
    fun twoSlotsCovered_neitherSends_bothResumeOnce() = runTest {
        // L-068: two slots of one placement under one cover; the window is one for the process.
        val source = newSource(gate = window.open)
        window.onCovered(true)
        repeat(2) { source.attach() }
        val a = claim(source)
        val b = claim(source)
        at(60_000)
        assertTrue(loads.sent.isEmpty())

        window.onCovered(false)
        runCurrent()
        assertEquals(listOf(60_000L), loads.times)
        deliver()
        assertTrue(a.result() is NativeClaim.Ad)
        // The loser gets its one retry, at once: two sends for the two claims that waited.
        assertEquals(listOf(60_000L, 60_000L), loads.times)
        deliver()
        assertTrue(b.result() is NativeClaim.Ad)
    }

    @Test
    fun loserRetry_gateClosed_getsBlocked_sendsNothing() = runTest {
        val source = newSource()
        repeat(2) { source.attach() }
        val a = claim(source)
        val b = claim(source)
        mayRequest = false
        deliver()

        assertTrue(a.result() is NativeClaim.Ad)
        assertEquals(NativeClaim.Blocked, b.result())
        at(60_000)
        assertEquals(1, loads.sent.size)
    }

    @Test
    fun nextLoadAfterClaim_inBackground_waitsForForeground() = runTest {
        val source = newSource()
        source.attach()
        val a = claim(source)
        foreground.value = false
        deliver()
        assertTrue(a.result() is NativeClaim.Ad)

        at(30_000)
        assertEquals(1, loads.sent.size)

        foreground.value = true
        runCurrent()
        assertEquals(listOf(0L, 30_000L), loads.times)
        assertEquals(NativeLoadUse.REFRESH, loads.uses.last())
    }

    @Test
    fun nextLoadAfterClaim_gateClosed_sendsNothing_noRetry() = runTest {
        val source = newSource()
        source.attach()
        val a = claim(source)
        mayRequest = false
        deliver()
        assertTrue(a.result() is NativeClaim.Ad)

        at(60_000)
        assertEquals(1, loads.sent.size)
        assertFalse(source.isLoading)

        // Control (L-055): with the gate open again a claim sends at once, so the source was not stuck.
        mayRequest = true
        claim(source)
        assertEquals(listOf(0L, 60_000L), loads.times)
    }

    @Test
    fun loadThrows_waiterGetsNoAd_noUncaught_firstLoad() = runTest {
        val source = newSource()
        source.attach()
        loads.throwNext = IllegalStateException("secret sdk detail")

        val a = claim(source)

        assertEquals(NativeClaim.NoAd, a.result())
        assertFalse(source.isLoading)
        at(60_000)
        assertTrue(loads.sent.isEmpty())
        assertTrue(logger.entries.any { it.message.contains("IllegalStateException") })
        assertFalse(logger.entries.any { it.message.contains("secret sdk detail") })
        // Control (L-055): the next claim sends, so the error did not break the source.
        claim(source)
        assertEquals(1, loads.sent.size)
    }

    @Test
    fun loadThrows_waiterGetsNoAd_noUncaught_nextLoad() = runTest {
        val source = newSource()
        source.attach()
        val a = claim(source)
        loads.throwNext = IllegalStateException("secret sdk detail")
        deliver()
        assertTrue(a.result() is NativeClaim.Ad)

        assertFalse(source.isLoading)
        at(60_000)
        assertEquals(1, loads.sent.size)
        assertTrue(logger.entries.any { it.message.contains("IllegalStateException") })
        claim(source)
        assertEquals(2, loads.sent.size)
    }

    @Test
    fun loadThrows_waiterGetsNoAd_noUncaught_loserRetry() = runTest {
        val source = newSource()
        repeat(2) { source.attach() }
        val a = claim(source)
        val b = claim(source)
        loads.throwNext = IllegalStateException("secret sdk detail")
        deliver()

        assertTrue(a.result() is NativeClaim.Ad)
        assertEquals(NativeClaim.NoAd, b.result())
        assertFalse(source.isLoading)
        at(60_000)
        assertEquals(1, loads.sent.size)
    }

    @Test
    fun claimerCancelled_whileLoading_otherSlotStillThere_adBecomesPending() = runTest {
        val source = newSource()
        repeat(2) { source.attach() }
        val a = claim(source)
        a.cancel()
        runCurrent()
        // Slot A leaves; slot B is still on screen.
        source.detach()

        val ad = deliver()

        assertSame(ad, source.pending)
        assertEquals(0, ad.destroyCount)
        assertFalse(source.isLoading)
    }

    @Test
    fun lastSlotDetached_whileLoading_lateAdDestroyed_sourceNotLoading() = runTest {
        val source = newSource()
        source.attach()
        val a = claim(source)
        assertTrue(source.isLoading)

        source.detach()
        runCurrent()

        assertEquals(NativeClaim.NoAd, a.result())
        assertFalse(source.isLoading)
        val late = deliver()
        assertEquals(1, late.destroyCount)
        assertNull(source.pending)
        at(60_000)
        assertEquals(1, loads.sent.size)
    }

    @Test
    fun lastSlotDetached_destroysPendingAd() = runTest {
        val source = newSource()
        source.attach()
        val (ad1, ad2) = withPendingAd(source)

        source.detach()
        runCurrent()

        assertEquals(1, ad2.destroyCount)
        assertNull(source.pending)
        // The ad of the slot is destroyed by the slot itself, not by the source.
        assertEquals(0, ad1.destroyCount)
        assertTrue(logger.entries.any { it.message == "Native $KEY source closed" })
    }

    @Test
    fun detachAll_thenAttach_reopensWithFreshLoad() = runTest {
        val source = newSource()
        source.attach()
        val (_, ad2) = withPendingAd(source)
        source.detach()
        runCurrent()
        assertEquals(1, ad2.destroyCount)

        // The activity is built again: the slot comes back.
        at(1_000)
        source.attach()
        val c = claim(source)

        assertEquals(listOf(0L, 0L, 1_000L), loads.times)
        assertEquals(NativeLoadUse.SINGLE_AD, loads.uses.last())
        val ad3 = deliver()
        assertSame(ad3, c.ad())
    }

    @Test
    fun failureAfterClose_schedulesNoRetry() = runTest {
        val source = newSource()
        source.attach()
        val a = claim(source)
        assertEquals(1, loads.sent.size)
        source.detach()
        runCurrent()

        fail()

        assertEquals(NativeClaim.NoAd, a.result())
        at(60_000)
        assertEquals(1, loads.sent.size)
        assertFalse(source.isLoading)
    }

    @Test
    fun waitersGetNoAd_atOnce_whenClosed() = runTest {
        val source = newSource()
        repeat(2) { source.attach() }
        val a = claim(source)
        val b = claim(source)
        assertEquals(0L, source.closed.value)

        source.shutDown()
        runCurrent()

        assertEquals(NativeClaim.NoAd, a.result())
        assertEquals(NativeClaim.NoAd, b.result())
        assertEquals(1L, source.closed.value)
        assertFalse(source.isLoading)
        val late = deliver()
        assertEquals(1, late.destroyCount)
        at(60_000)
        assertEquals(1, loads.sent.size)
    }

    @Test
    fun pendingAdAt54m59s_isClaimed() = runTest {
        val source = newSource()
        source.attach()
        val (_, ad2) = withPendingAd(source)

        at(NativeAdPolicy.MAX_AGE_MILLIS - 1)
        val c = claim(source)

        assertSame(ad2, c.ad())
        assertEquals(0, ad2.destroyCount)
        assertEquals(55 * 60 * 1_000L, NativeAdPolicy.MAX_AGE_MILLIS)
    }

    @Test
    fun pendingAdAt55m_isDestroyed_newLoadSent() = runTest {
        val source = newSource()
        source.attach()
        val (_, ad2) = withPendingAd(source)
        assertSame(ad2, source.pending)

        at(NativeAdPolicy.MAX_AGE_MILLIS)
        val c = claim(source)

        assertEquals(1, ad2.destroyCount)
        assertFalse(c.isCompleted)
        assertEquals(NativeAdPolicy.MAX_AGE_MILLIS, loads.times.last())
        assertEquals(NativeLoadUse.SINGLE_AD, loads.uses.last())
        assertTrue(logger.entries.any { it.message == "Native $KEY expired" })
        val ad3 = deliver()
        assertSame(ad3, c.ad())
    }

    @Test
    fun logs_holdNoUnitIdOrAdText() = runTest {
        val source = newSource()
        source.attach()
        withPendingAd(source)
        at(NativeAdPolicy.MAX_AGE_MILLIS)
        claim(source)
        deliver()
        source.detach()
        runCurrent()

        val messages = logger.entries.filter { it.tag == "Ads" }.map { it.message }
        assertTrue(messages.toString(), messages.contains("Native $KEY claimed"))
        assertTrue(messages.toString(), messages.contains("Native $KEY expired"))
        assertTrue(messages.toString(), messages.contains("Native $KEY source closed"))
        assertTrue(messages.toString(), messages.contains("Native $KEY next load"))
        assertFalse(messages.any { it.contains(FakeNativeLoads.FAKE_UNIT_ID) })
        assertFalse(messages.any { it.contains(sampleNativeAdAssets.headline) })
    }

    private companion object {
        const val KEY = "sample_native_small"
    }
}
