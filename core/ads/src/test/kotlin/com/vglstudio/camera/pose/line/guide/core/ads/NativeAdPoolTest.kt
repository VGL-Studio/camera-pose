package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The native pool of one (format, ad unit) on a virtual clock (plan T-028): sequential refill to capacity, round
 * robin, release with and without impression, expiry, exhaustion, close, retries, and the send gate of every path
 * (P-01 to P-08, O-04, O-05, S-11, S-15, S-24, L-072).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NativeAdPoolTest {
    private val loader = FakeNativeAdLoader()
    private val logger = RecordingAppLogger()
    private val foreground = MutableStateFlow(true)
    private val retryResets = MutableStateFlow(0)
    private var mayRequest = true
    private var sdkReady = true
    private var canShow = true
    private var gateCalls = 0
    private lateinit var loads: FakePoolLoads

    private fun TestScope.newPool(capacity: Int = 3): NativeAdPool {
        loads = FakePoolLoads(loader, { testScheduler.currentTime }, logger)
        return NativeAdPool(
            key = NativePoolKey(AdFormat.NATIVE_MEDIUM, FakePoolLoads.FAKE_POOL_UNIT_ID),
            capacity = capacity,
            load = { loads.load() },
            sendGate = {
                gateCalls++
                awaitSendAllowed(foreground) { mayRequest }
            },
            canLoadNow = { sdkReady && foreground.value && mayRequest },
            canShowNow = { canShow },
            retryResets = retryResets,
            clock = { testScheduler.currentTime },
            scope = backgroundScope,
            logger = logger,
            logKey = FakePoolLoads.FAKE_POOL_KEY,
        )
    }

    /** Moves the virtual clock to [millis] after the start and runs what is due. */
    private fun TestScope.at(millis: Long) {
        advanceTimeBy(millis - testScheduler.currentTime)
        runCurrent()
    }

    private fun TestScope.deliver(ad: FakeLoadedNativeAd = FakeLoadedNativeAd()): FakeLoadedNativeAd {
        loader.deliver(ad)
        runCurrent()
        return ad
    }

    private fun TestScope.fail(code: Int = 3) {
        loader.fail(code)
        runCurrent()
    }

    /** Refills [pool] and answers each request at once with a new ad, [count] times. */
    private fun TestScope.fill(pool: NativeAdPool, count: Int): List<FakeLoadedNativeAd> {
        pool.refill()
        runCurrent()
        return List(count) { deliver(FakeLoadedNativeAd(sampleNativeAdAssets.copy(headline = "Pool ad ${it + 1}"))) }
    }

    private fun messages() = logger.entries.map { it.message }

    @Test
    fun refill_sendsOneAtATime_untilCapacity() = runTest {
        val pool = newPool()
        pool.refill()
        runCurrent()
        at(100)
        deliver()
        at(200)
        deliver()
        at(300)
        deliver()
        at(10_000)

        assertEquals(listOf(0L, 100L, 200L), loads.times)
        assertEquals(1, loader.maxOpen)
    }

    @Test
    fun capacity_countsAdsInUse() = runTest {
        val pool = newPool()
        fill(pool, 3)
        val a = pool.claim(AdPlacement.GALLERY_NATIVE_MEDIUM) as FakeLoadedNativeAd
        pool.claim(AdPlacement.GALLERY_NATIVE_MEDIUM)
        runCurrent()
        assertEquals(3, loads.times.size)

        a.impressionRecorded = true
        pool.release(a)
        runCurrent()
        assertEquals(4, loads.times.size)
    }

    @Test
    fun claims_roundRobin_neverReturnInUse() = runTest {
        val pool = newPool()
        val (a, b, c) = fill(pool, 3)
        val holder = AdPlacement.GALLERY_NATIVE_MEDIUM

        assertSame(a, pool.claim(holder))
        assertSame(b, pool.claim(holder))
        assertSame(c, pool.claim(holder))
        assertNull(pool.claim(holder))

        pool.release(a)
        assertSame(a, pool.claim(holder))

        // Control: released in another order, they come back in that order.
        pool.release(b)
        pool.release(a)
        assertSame(b, pool.claim(holder))
        assertSame(a, pool.claim(holder))
    }

    @Test
    fun holderSetAtClaim_metricsReadClaimer() = runTest {
        val pool = newPool(capacity = 1)
        val (ad) = fill(pool, 1)
        val holder = loads.holders.single()
        assertNull(holder.value)

        assertSame(ad, pool.claim(AdPlacement.GALLERY_NATIVE_MEDIUM))
        assertEquals(AdPlacement.GALLERY_NATIVE_MEDIUM, holder.value)

        pool.release(ad)
        assertSame(ad, pool.claim(AdPlacement.GALLERY_INLIST_MEDIUM))
        assertEquals(AdPlacement.GALLERY_INLIST_MEDIUM, holder.value)
    }

    @Test
    fun releaseAfterImpression_destroysAndRefills_neverClaimedAgain() = runTest {
        val pool = newPool()
        val (a, b, c) = fill(pool, 3)
        val holder = AdPlacement.GALLERY_NATIVE_MEDIUM
        assertSame(a, pool.claim(holder))
        a.impressionRecorded = true

        pool.release(a)
        runCurrent()

        assertEquals(1, a.destroyCount)
        assertEquals(4, loads.times.size)
        val d = deliver()
        val claimed = listOf(pool.claim(holder), pool.claim(holder), pool.claim(holder))
        assertEquals(listOf<LoadedNativeAd?>(b, c, d), claimed)
        assertTrue(messages().any { it.contains("released: destroyed (impression)") })
    }

    @Test
    fun impressionAfterRelease_notClaimedAgain_destroyedAndRefilled() = runTest {
        val pool = newPool()
        val (a) = fill(pool, 3)
        assertSame(a, pool.claim(AdPlacement.GALLERY_NATIVE_MEDIUM))
        pool.release(a)
        runCurrent()
        assertEquals(0, a.destroyCount)
        assertEquals(3, loads.times.size)

        // The SDK reports the impression after the cell gave the ad back (P-01a, O-01).
        a.impressionRecorded = true
        val claimed = List(3) { pool.claim(AdPlacement.GALLERY_INLIST_MEDIUM) }
        runCurrent()

        assertTrue("the impressed ad was claimed again", claimed.none { it === a })
        assertEquals(1, a.destroyCount)
        assertEquals(4, loads.times.size)
    }

    @Test
    fun releaseWithoutImpression_backToRotation() = runTest {
        val pool = newPool()
        val (a) = fill(pool, 3)
        assertSame(a, pool.claim(AdPlacement.GALLERY_NATIVE_MEDIUM))

        pool.release(a)
        runCurrent()

        assertEquals(0, a.destroyCount)
        assertEquals(3, loads.times.size)
        pool.claim(AdPlacement.GALLERY_NATIVE_MEDIUM)
        pool.claim(AdPlacement.GALLERY_NATIVE_MEDIUM)
        assertSame(a, pool.claim(AdPlacement.GALLERY_NATIVE_MEDIUM))
        assertTrue(messages().any { it.contains("released: back") })
    }

    @Test
    fun refillAfter55m_dropsExpired_sendsNew() = runTest {
        val pool = newPool()
        val ads = fill(pool, 3)

        // Control: one millisecond before 55 minutes nothing is dropped or sent.
        at(MAX_AGE - 1)
        pool.refill()
        runCurrent()
        assertEquals(3, loads.times.size)
        assertEquals(listOf(0, 0, 0), ads.map { it.destroyCount })

        at(MAX_AGE)
        pool.refill()
        runCurrent()
        assertEquals(listOf(1, 1, 1), ads.map { it.destroyCount })
        at(MAX_AGE + 100)
        deliver()
        at(MAX_AGE + 200)
        deliver()
        at(MAX_AGE + 300)
        deliver()
        assertEquals(listOf(0L, 0L, 0L, MAX_AGE, MAX_AGE + 100, MAX_AGE + 200), loads.times)
        assertEquals(1, loader.maxOpen)
    }

    @Test
    fun freeAd_expiresAt55m_destroyedNotClaimed() = runTest {
        val pool = newPool(capacity = 1)
        val (ad) = fill(pool, 1)
        val control = newPoolWithLoads(capacity = 1)
        val (controlAd) = fill(control.first, 1)

        at(MAX_AGE - 1)
        assertSame(controlAd, control.first.claim(AdPlacement.GALLERY_NATIVE_MEDIUM))

        at(MAX_AGE)
        assertNull(pool.claim(AdPlacement.GALLERY_NATIVE_MEDIUM))
        runCurrent()
        assertEquals(1, ad.destroyCount)
        assertTrue(messages().any { it.endsWith("expired") })
        assertEquals(MAX_AGE, loads.times.last())
    }

    /** A second pool with loads of its own on the same loader; returns the pool and its loads. */
    private fun TestScope.newPoolWithLoads(capacity: Int): Pair<NativeAdPool, FakePoolLoads> {
        val first = loads
        val pool = newPool(capacity)
        val second = loads
        loads = first
        return pool to second
    }

    @Test
    fun inUseAd_neverExpires() = runTest {
        val pool = newPool(capacity = 1)
        val (ad) = fill(pool, 1)
        assertSame(ad, pool.claim(AdPlacement.GALLERY_NATIVE_MEDIUM))

        at(60 * 60 * 1_000L)
        pool.refill()
        runCurrent()

        assertEquals(0, ad.destroyCount)
        assertEquals(1, loads.times.size)
    }

    @Test
    fun exhausted_table() = runTest {
        val pool = newPool(capacity = 1)
        // Empty, nothing in flight, loads possible: not exhausted (a claim starts a load).
        assertFalse(pool.exhausted)
        assertNull(pool.claim(AdPlacement.GALLERY_NATIVE_MEDIUM))
        runCurrent()
        // Empty and in flight.
        assertTrue(pool.isLoading)
        assertFalse(pool.exhausted)

        fail()
        // Empty, waiting for a retry.
        assertTrue(pool.exhausted)

        at(5_000)
        val ad = deliver()
        assertFalse(pool.exhausted)

        // Full and every ad in use.
        assertSame(ad, pool.claim(AdPlacement.GALLERY_NATIVE_MEDIUM))
        assertFalse(pool.exhausted)
        pool.release(ad)

        // canLoadNow false: background, gate closed, SDK not ready; the ad still counts as free.
        assertSame(ad, pool.claim(AdPlacement.GALLERY_NATIVE_MEDIUM))
        ad.impressionRecorded = true
        mayRequest = false
        pool.release(ad)
        runCurrent()
        assertTrue(pool.exhausted)
        mayRequest = true
        sdkReady = false
        assertTrue(pool.exhausted)
        sdkReady = true
        foreground.value = false
        assertTrue(pool.exhausted)
        foreground.value = true
        assertFalse(pool.exhausted)

        pool.close()
        assertTrue(pool.exhausted)
    }

    @Test
    fun exhausted_whileSendWaitsForForeground_isTrue() = runTest {
        val pool = newPool()
        foreground.value = false
        pool.refill()
        runCurrent()
        assertTrue(pool.exhausted)
        assertTrue(loads.times.isEmpty())

        foreground.value = true
        runCurrent()
        assertEquals(1, loads.times.size)
        assertFalse(pool.exhausted)
    }

    @Test
    fun close_destroysFree_cancelsInFlight_notLoading() = runTest {
        val pool = newPool()
        val (a, b) = fill(pool, 2)
        assertTrue(pool.isLoading)
        val requestsBefore = loader.requests.size

        pool.close()
        runCurrent()

        assertEquals(listOf(1, 1), listOf(a.destroyCount, b.destroyCount))
        assertFalse(pool.isLoading)
        assertTrue(pool.exhausted)
        val late = FakeLoadedNativeAd()
        loader.deliver(late)
        runCurrent()
        assertEquals(1, late.destroyCount)
        assertNull(pool.claim(AdPlacement.GALLERY_NATIVE_MEDIUM))
        at(120_000)
        assertEquals(requestsBefore, loader.requests.size)
        assertTrue(messages().any { it.endsWith("closed") })
    }

    @Test
    fun close_inUseReleasedLater_isDestroyed() = runTest {
        val pool = newPool(capacity = 1)
        val (ad) = fill(pool, 1)
        assertSame(ad, pool.claim(AdPlacement.GALLERY_NATIVE_MEDIUM))
        pool.close()
        assertEquals(0, ad.destroyCount)

        pool.release(ad)

        assertEquals(1, ad.destroyCount)
    }

    @Test
    fun reopen_sendsAgain() = runTest {
        val pool = newPool(capacity = 1)
        pool.close()
        pool.refill()
        runCurrent()
        assertTrue(loads.times.isEmpty())

        pool.reopen()
        pool.refill()
        runCurrent()
        assertEquals(1, loads.times.size)
    }

    @Test
    fun claim_gateClosed_returnsNull_keepsAd() = runTest {
        val pool = newPool(capacity = 1)
        val (ad) = fill(pool, 1)
        canShow = false

        assertNull(pool.claim(AdPlacement.GALLERY_NATIVE_MEDIUM))
        assertEquals(0, ad.destroyCount)

        canShow = true
        assertSame(ad, pool.claim(AdPlacement.GALLERY_NATIVE_MEDIUM))
    }

    @Test
    fun claimLoad_inBackground_waitsForForeground() = runTest {
        val pool = newPool()
        foreground.value = false
        pool.claim(AdPlacement.GALLERY_NATIVE_MEDIUM)
        at(10_000)
        assertTrue(loads.times.isEmpty())

        foreground.value = true
        runCurrent()
        assertEquals(listOf(10_000L), loads.times)
    }

    @Test
    fun claimLoad_gateClosed_sendsNothing_noRetry() = runTest {
        val pool = newPool()
        mayRequest = false
        pool.claim(AdPlacement.GALLERY_NATIVE_MEDIUM)
        runCurrent()
        at(120_000)

        assertTrue(loads.times.isEmpty())
        assertEquals(1, gateCalls)
        assertTrue(messages().any { it.endsWith("request blocked") })
    }

    /** One free ad in a pool of two whose second load was blocked by the gate: a claim starts the next load. */
    private fun TestScope.oneFreeOfTwo(): Pair<NativeAdPool, FakeLoadedNativeAd> {
        val pool = newPool(capacity = 2)
        pool.refill()
        runCurrent()
        mayRequest = false
        val ad = deliver()
        assertEquals(1, loads.times.size)
        return pool to ad
    }

    @Test
    fun refillAfterClaim_inBackground_waitsForForeground() = runTest {
        val (pool, ad) = oneFreeOfTwo()
        mayRequest = true
        foreground.value = false
        assertSame(ad, pool.claim(AdPlacement.GALLERY_NATIVE_MEDIUM))
        at(5_000)
        assertEquals(listOf(0L), loads.times)

        foreground.value = true
        runCurrent()
        assertEquals(listOf(0L, 5_000L), loads.times)
    }

    @Test
    fun refillAfterClaim_gateClosed_sendsNothing() = runTest {
        val (pool, ad) = oneFreeOfTwo()
        val gateCallsBefore = gateCalls
        assertSame(ad, pool.claim(AdPlacement.GALLERY_NATIVE_MEDIUM))
        at(120_000)

        assertEquals(listOf(0L), loads.times)
        assertEquals(gateCallsBefore + 1, gateCalls)
        assertTrue(messages().any { it.endsWith("request blocked") })
    }

    @Test
    fun refillAfterImpression_inBackground_waitsForForeground() = runTest {
        val pool = newPool()
        fill(pool, 3)
        val ad = pool.claim(AdPlacement.GALLERY_NATIVE_MEDIUM) as FakeLoadedNativeAd
        ad.impressionRecorded = true
        foreground.value = false
        pool.release(ad)
        at(3_000)
        assertEquals(3, loads.times.size)

        foreground.value = true
        runCurrent()
        assertEquals(3_000L, loads.times.last())
        assertEquals(4, loads.times.size)
    }

    @Test
    fun refillAfterExpiry_gateClosed_sendsNothing() = runTest {
        val pool = newPool()
        val ads = fill(pool, 3)
        mayRequest = false
        at(MAX_AGE)

        assertNull(pool.claim(AdPlacement.GALLERY_NATIVE_MEDIUM))
        at(MAX_AGE + 120_000)

        assertEquals(listOf(1, 1, 1), ads.map { it.destroyCount })
        assertEquals(3, loads.times.size)
        assertTrue(messages().any { it.endsWith("request blocked") })
    }

    @Test
    fun retry_inBackground_waitsForForeground() = runTest {
        val pool = newPool()
        pool.refill()
        runCurrent()
        fail()
        at(1_000)
        foreground.value = false
        at(9_000)
        assertEquals(listOf(0L), loads.times)

        foreground.value = true
        runCurrent()
        assertEquals(listOf(0L, 9_000L), loads.times)
    }

    @Test
    fun retry_gateClosed_sendsNothing_noFurtherRetry() = runTest {
        val pool = newPool()
        pool.refill()
        runCurrent()
        fail()
        mayRequest = false
        at(120_000)

        assertEquals(listOf(0L), loads.times)
        assertEquals(2, gateCalls)
    }

    @Test
    fun failures_retryDelays() = runTest {
        // An empty pool: 5 s after each failure (O-05, holding no ad).
        val empty = newPool()
        empty.refill()
        runCurrent()
        fail()
        at(5_000)
        fail()
        at(10_000)
        fail()
        at(15_000)
        assertEquals(listOf(0L, 5_000L, 10_000L, 15_000L), loads.times)
        empty.close()

        // A pool holding one ad: 2, 4, 8 s; a success counts again from 2 s.
        val start = 100_000L
        at(start)
        val holding = newPool()
        holding.refill()
        runCurrent()
        deliver()
        fail()
        at(start + 2_000)
        fail()
        at(start + 6_000)
        fail()
        at(start + 14_000)
        deliver()
        fail()
        at(start + 16_000)
        val offsets = loads.times.map { it - start }
        assertEquals(listOf(0L, 0L, 2_000L, 6_000L, 14_000L, 14_000L, 16_000L), offsets)
    }

    @Test
    fun loadThrows_isFailureCode0() = runTest {
        val pool = newPool()
        loads.throwNext = IllegalStateException("not built")
        pool.refill()
        runCurrent()

        assertTrue(loads.times.isEmpty())
        assertTrue(messages().any { it.contains("failed: 0") })
        assertFalse(messages().any { it.contains("not built") })
        at(5_000)
        assertEquals(listOf(5_000L), loads.times)
    }

    @Test
    fun networkBack_cutsRetryWaitShort() = runTest {
        val pool = newPool()
        pool.refill()
        runCurrent()
        fail()
        at(1_000)
        retryResets.value += 1
        runCurrent()
        assertEquals(listOf(0L, 1_000L), loads.times)

        // Control: without the network coming back, the next try waits the 5 s.
        fail()
        at(5_999)
        assertEquals(2, loads.times.size)
        at(6_000)
        assertEquals(listOf(0L, 1_000L, 6_000L), loads.times)
    }

    @Test
    fun logs_holdNoUnitIdOrAdText() = runTest {
        val pool = newPool(capacity = 1)
        val (ad) = fill(pool, 1)
        pool.claim(AdPlacement.GALLERY_NATIVE_MEDIUM)
        pool.release(ad)
        pool.close()

        val logs = logger.entries.filter { it.message.startsWith("Pool ") }
        assertTrue(logs.all { it.tag == "Ads" })
        val text = logs.joinToString("\n") { it.message }
        assertTrue(text, text.contains("Pool native_medium#1 loaded (free 1/1)"))
        assertTrue(text, text.contains("claimed by gallery_native_medium"))
        assertTrue(text, text.contains("released: back"))
        assertTrue(text, text.contains("closed"))
        assertFalse(text, text.contains(FakePoolLoads.FAKE_POOL_UNIT_ID))
        assertFalse(text, text.contains("Pool ad 1"))
    }

    private companion object {
        const val MAX_AGE = 55 * 60 * 1_000L
    }
}
