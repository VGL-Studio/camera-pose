package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.runtime.mutableStateOf
import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ads of the in-list slots of one screen: loaded once per slot, kept while scrolling, destroyed on leaving. */
@OptIn(ExperimentalCoroutinesApi::class)
class InListAdsStateTest {
    private val logger = RecordingAppLogger()

    /** Answers every load with [answer], counting the calls. */
    private class CountingLoader(private val answer: suspend () -> NativeAdState) : InListAdLoader {
        var calls = 0
            private set

        override suspend fun load(placement: AdPlacement): NativeAdState {
            calls++
            return answer()
        }
    }

    /**
     * Loads run on the test scheduler but outside backgroundScope, whose work advanceUntilIdle does not run.
     */
    private fun TestScope.loadScope() = CoroutineScope(StandardTestDispatcher(testScheduler))

    private fun inListAds(loader: InListAdLoader, scope: CoroutineScope) = InListAds(
        placement = AdPlacement.SAMPLE_NATIVE_SMALL,
        every = mutableStateOf<Int?>(3),
        loader = loader,
        scope = scope,
        logger = logger,
    )

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
        val loader = CountingLoader { NativeAdState.Loaded(ad) }
        val ads = inListAds(loader, loadScope())

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
        val loader = CountingLoader { NativeAdState.Loaded(FakeLoadedNativeAd(sampleNativeAdAssets)) }
        val ads = inListAds(loader, loadScope())

        val first = ads.slot(0)
        val second = ads.slot(1)
        advanceUntilIdle()

        assertEquals(2, loader.calls)
        val firstAd = (first.value as NativeAdState.Loaded).ad
        val secondAd = (second.value as NativeAdState.Loaded).ad
        assertNotSame(firstAd, secondAd)
    }

    @Test
    fun unavailableSlot_notRetriedOnSameScreen() = runTest {
        val loader = CountingLoader { NativeAdState.Unavailable }
        val ads = inListAds(loader, loadScope())

        val state = ads.slot(0)
        advanceUntilIdle()
        // Control: the first call did load, so "no new call" below means no retry.
        assertEquals(1, loader.calls)
        assertEquals(NativeAdState.Unavailable, state.value)

        ads.slot(0)
        advanceUntilIdle()
        assertEquals(1, loader.calls)
    }

    @Test
    fun destroyAll_destroysEveryLoadedAd_andCancelsPendingLoads() = runTest {
        val loadedAds = listOf(FakeLoadedNativeAd(sampleNativeAdAssets), FakeLoadedNativeAd(sampleNativeAdAssets))
        val pending = CompletableDeferred<NativeAdState>()
        var pendingCancelled = false
        var call = 0
        val loader = InListAdLoader {
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
        val ads = inListAds(loader, loadScope())

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
        val loader = CountingLoader { NativeAdState.Loaded(FakeLoadedNativeAd(sampleNativeAdAssets)) }
        val ads = inListAds(loader, loadScope())

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
}
