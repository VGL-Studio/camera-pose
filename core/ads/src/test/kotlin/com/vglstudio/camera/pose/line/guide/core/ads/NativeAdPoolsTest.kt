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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The pools of the native placements (plan T-028): one per (format, ad unit), closed when consent is withdrawn or every
 * placement of the pool is switched off (L-061), and their slots of the preload plan (HD-02, S-22, S-25).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NativeAdPoolsTest {
    private val loader = FakeNativeAdLoader()
    private val logger = RecordingAppLogger()
    private val foreground = MutableStateFlow(true)
    private val consent = MutableStateFlow(true)
    private var mayRequest = true
    private var gateCalls = 0
    private val units = mutableMapOf<AdPlacement, String?>(
        AdPlacement.GALLERY_NATIVE_MEDIUM to UNIT_A,
        AdPlacement.GALLERY_INLIST_MEDIUM to UNIT_A,
    )
    private val switches = mutableMapOf<AdPlacement, MutableStateFlow<Boolean>>()
    private val loads = mutableListOf<FakePoolLoads>()

    private fun switchOf(placement: AdPlacement) = switches.getOrPut(placement) { MutableStateFlow(true) }

    private fun TestScope.newPools(
        placements: List<AdPlacement> = listOf(AdPlacement.GALLERY_NATIVE_MEDIUM, AdPlacement.GALLERY_INLIST_MEDIUM),
    ): NativeAdPools = NativeAdPools(
        placements = placements,
        unitId = { units[it] },
        enabled = { switchOf(it) },
        canRequestAds = consent,
        newPool = { key, _ ->
            val poolLoads = FakePoolLoads(loader, { testScheduler.currentTime }, logger).also { loads += it }
            NativeAdPool(
                key = key,
                capacity = requireNotNull(key.format.poolCapacity()),
                load = { poolLoads.load() },
                sendGate = {
                    gateCalls++
                    awaitSendAllowed(foreground) { mayRequest }
                },
                canLoadNow = { foreground.value && mayRequest },
                canShowNow = { consent.value },
                retryResets = MutableStateFlow(0),
                clock = { testScheduler.currentTime },
                scope = backgroundScope,
                logger = logger,
                logKey = "${key.format.code}#${loads.size}",
            )
        },
        scope = backgroundScope,
        logger = logger,
    ).also { runCurrent() }

    private fun TestScope.at(millis: Long) {
        advanceTimeBy(millis - testScheduler.currentTime)
        runCurrent()
    }

    private fun TestScope.deliver(): FakeLoadedNativeAd {
        val ad = FakeLoadedNativeAd()
        loader.deliver(ad)
        runCurrent()
        return ad
    }

    private fun sentTimes(): List<Long> = loads.flatMap { it.times }.sorted()

    private fun NativeAdPools.slot(format: AdFormat = AdFormat.NATIVE_MEDIUM): PreloadSlot =
        preloadSlots(format).single()

    @Test
    fun sameFormatAndUnit_sharePool_differentUnits_separatePools() = runTest {
        val pools = newPools()
        val a = assertPool(pools.pool(AdPlacement.GALLERY_NATIVE_MEDIUM))
        assertSame(a, pools.pool(AdPlacement.GALLERY_INLIST_MEDIUM))
        assertEquals(NativePoolKey(AdFormat.NATIVE_MEDIUM, UNIT_A), a.key)

        units[AdPlacement.GALLERY_INLIST_MEDIUM] = UNIT_B
        val separate = newPools()
        val first = assertPool(separate.pool(AdPlacement.GALLERY_NATIVE_MEDIUM))
        val second = assertPool(separate.pool(AdPlacement.GALLERY_INLIST_MEDIUM))
        assertNotSame(first, second)
        assertEquals(UNIT_B, second.key.unitId)
    }

    @Test
    fun noPool_forOtherFormatOrWithoutUnit() = runTest {
        units[AdPlacement.GALLERY_INLIST_MEDIUM] = null
        val pools = newPools(
            listOf(
                AdPlacement.GALLERY_NATIVE_MEDIUM,
                AdPlacement.GALLERY_INLIST_MEDIUM,
                AdPlacement.SAMPLE_NATIVE_SMALL,
            ),
        )
        assertNull(pools.pool(AdPlacement.SAMPLE_NATIVE_SMALL))
        assertNull(pools.pool(AdPlacement.GALLERY_INLIST_MEDIUM))
        // Control: the medium placement with an ad unit has one.
        assertNotNull(pools.pool(AdPlacement.GALLERY_NATIVE_MEDIUM))
    }

    private fun assertPool(pool: NativeAdPool?): NativeAdPool {
        assertNotNull("expected a pool", pool)
        return pool!!
    }

    @Test
    fun consentWithdrawn_closesEveryPool() = runTest {
        val pools = newPools()
        val pool = assertPool(pools.pool(AdPlacement.GALLERY_NATIVE_MEDIUM))
        pool.refill()
        runCurrent()
        val ad = deliver()
        assertFalse(pool.exhausted)

        consent.value = false
        runCurrent()

        assertEquals(1, ad.destroyCount)
        assertTrue(pool.exhausted)
        assertFalse(pool.isLoading)
    }

    @Test
    fun allPlacementsOff_closesPool_oneStillOn_keepsIt() = runTest {
        val pools = newPools()
        val pool = assertPool(pools.pool(AdPlacement.GALLERY_NATIVE_MEDIUM))
        pool.refill()
        runCurrent()
        val ad = deliver()

        switchOf(AdPlacement.GALLERY_NATIVE_MEDIUM).value = false
        runCurrent()
        assertEquals(0, ad.destroyCount)
        assertFalse(pool.exhausted)

        switchOf(AdPlacement.GALLERY_INLIST_MEDIUM).value = false
        runCurrent()
        assertEquals(1, ad.destroyCount)
        assertTrue(pool.exhausted)
    }

    @Test
    fun consentBack_reopens() = runTest {
        val pools = newPools()
        val pool = assertPool(pools.pool(AdPlacement.GALLERY_NATIVE_MEDIUM))
        consent.value = false
        runCurrent()
        pool.refill()
        runCurrent()
        assertTrue(sentTimes().isEmpty())

        consent.value = true
        runCurrent()
        pool.refill()
        runCurrent()
        assertEquals(1, sentTimes().size)
    }

    @Test
    fun preloadSlot_refillsEnabledPlacementsOnly() = runTest {
        units[AdPlacement.GALLERY_INLIST_MEDIUM] = UNIT_B
        switchOf(AdPlacement.GALLERY_INLIST_MEDIUM).value = false
        val pools = newPools()

        pools.slot().loadIfNeeded()
        runCurrent()

        // The placement switched off gets no pool and sends nothing; only the one switched on loads.
        assertEquals(1, loads.size)
        assertEquals(listOf(0L), sentTimes())

        units[AdPlacement.GALLERY_NATIVE_MEDIUM] = null
        val withoutUnit = newPools()
        withoutUnit.slot().loadIfNeeded()
        runCurrent()
        assertEquals(1, loads.size)
    }

    @Test
    fun preloadSlot_settled_whenOneAdOrFailedOrClosed() = runTest {
        val pools = newPools()
        val slot = pools.slot()
        slot.loadIfNeeded()
        runCurrent()
        assertFalse(slot.settled())
        deliver()
        assertTrue("one ad", slot.settled())

        val failing = newPools()
        val failingSlot = failing.slot()
        failingSlot.loadIfNeeded()
        runCurrent()
        assertFalse(failingSlot.settled())
        loader.fail(3)
        runCurrent()
        assertTrue("failed, waiting for a retry", failingSlot.settled())

        val closing = newPools()
        val closingSlot = closing.slot()
        closingSlot.loadIfNeeded()
        runCurrent()
        assertFalse(closingSlot.settled())
        consent.value = false
        runCurrent()
        assertTrue("closed", closingSlot.settled())
    }

    @Test
    fun preloadSlotAfter55m_dropsExpired_sendsNew() = runTest {
        val pools = newPools()
        val slot = pools.slot()
        slot.loadIfNeeded()
        runCurrent()
        val ads = List(3) { deliver() }

        at(MAX_AGE - 1)
        slot.loadIfNeeded()
        runCurrent()
        assertEquals(3, sentTimes().size)

        at(MAX_AGE)
        slot.loadIfNeeded()
        runCurrent()
        assertEquals(listOf(1, 1, 1), ads.map { it.destroyCount })
        assertEquals(listOf(0L, 0L, 0L, MAX_AGE), sentTimes())
    }

    @Test
    fun preloadSlot_inBackground_waitsForForeground() = runTest {
        val pools = newPools()
        foreground.value = false
        pools.slot().loadIfNeeded()
        at(4_000)
        assertTrue(sentTimes().isEmpty())

        foreground.value = true
        runCurrent()
        assertEquals(listOf(4_000L), sentTimes())
    }

    @Test
    fun preloadSlot_gateClosed_sendsNothing() = runTest {
        val pools = newPools()
        mayRequest = false
        pools.slot().loadIfNeeded()
        at(120_000)

        assertTrue(sentTimes().isEmpty())
        assertEquals(1, gateCalls)
    }

    @Test
    fun everySend_goesThroughSendGate() = runTest {
        val pools = newPools()
        val pool = assertPool(pools.pool(AdPlacement.GALLERY_NATIVE_MEDIUM))
        // (e) the preload slot, then (a) loads behind it.
        pools.slot().loadIfNeeded()
        runCurrent()
        deliver()
        // (d) a retry after a failure.
        loader.fail(3)
        runCurrent()
        at(2_000)
        deliver()
        deliver()
        // (a) a claim, (c) a refill after an impression.
        val claimed = pool.claim(AdPlacement.GALLERY_NATIVE_MEDIUM) as FakeLoadedNativeAd
        claimed.impressionRecorded = true
        pool.release(claimed)
        runCurrent()
        deliver()
        // (b) the refill after a claim, once a send was blocked.
        mayRequest = false
        val second = pool.claim(AdPlacement.GALLERY_NATIVE_MEDIUM) as FakeLoadedNativeAd
        second.impressionRecorded = true
        pool.release(second)
        runCurrent()
        mayRequest = true
        pool.claim(AdPlacement.GALLERY_NATIVE_MEDIUM)
        runCurrent()

        assertEquals(sentTimes().size + 1, gateCalls)
    }

    private companion object {
        const val UNIT_A = "unit-a"
        const val UNIT_B = "unit-b"
        const val MAX_AGE = 55 * 60 * 1_000L
    }
}
