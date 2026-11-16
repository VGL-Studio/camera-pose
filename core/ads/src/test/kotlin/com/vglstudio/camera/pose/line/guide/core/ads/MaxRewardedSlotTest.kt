package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Activity
import com.vglstudio.camera.pose.line.guide.core.analytics.AdFunnelEvent
import com.vglstudio.camera.pose.line.guide.core.analytics.AdFunnelKind
import com.vglstudio.camera.pose.line.guide.core.analytics.AdMetricEvent
import com.vglstudio.camera.pose.line.guide.core.analytics.AdMetricPlatform
import com.vglstudio.camera.pose.line.guide.core.analytics.RecordingAdFunnel
import com.vglstudio.camera.pose.line.guide.core.analytics.RecordingAdMetrics
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The one rewarded slot of AppLovin MAX (plan T-106): [MaxInterstitialSlot] with the rewarded format, on a virtual
 * clock. Its own counter key, metrics and log label; every way to a send asks the order and switches ([shouldLoad])
 * and the consent gate ([mayRequestNow]) right before sending; one request at a time (L-068); the network flag of R-03;
 * and the show followed by the SDK itself, since the SDK keeps one rewarded object per ad unit and drops a load sent
 * while that object shows ([FakeMaxInterstitialSdkAd] with `ignoreLoadWhileShowing`).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MaxRewardedSlotTest {
    private val logger = RecordingAppLogger()
    private val metrics = RecordingAdMetrics()
    private val funnel = RecordingAdFunnel()
    private val counter = LoadAttemptCounter()
    private val maxReady = MutableStateFlow(true)
    private var shouldLoad = true
    private var gateOpen = true
    private var networkError: (Int) -> Boolean = { false }
    private val activity = mockk<Activity>(relaxed = true)

    /** The objects `newAd` built, in order; like `MaxRewardedAd.getInstance`, one per unit until destroyed. */
    private val built = mutableListOf<FakeMaxInterstitialSdkAd>()
    private var cached: FakeMaxInterstitialSdkAd? = null
    private var newAdCalls = 0
    private val ad: FakeMaxInterstitialSdkAd get() = built.single()

    private val telemetry = AdTelemetry(
        metrics = metrics,
        funnel = funnel,
        lifecycleSwitches = { FLAGS.copy(logFullScreenEvents = true) },
        serving = { true },
        offline = { false },
        counter = counter,
        logger = logger,
    )

    private fun newAd(unit: String): FakeMaxInterstitialSdkAd {
        newAdCalls++
        return cached ?: FakeMaxInterstitialSdkAd(unit, ignoreLoadWhileShowing = true, onDestroy = { cached = null })
            .also {
                cached = it
                built += it
            }
    }

    private fun TestScope.slot(
        format: AdFormat = AdFormat.REWARDED,
        unit: String? = UNIT,
        shouldLoadGate: suspend () -> Boolean = { shouldLoad },
        networkLog: AdNetworkLog = AdNetworkLog.OFF,
    ) = MaxInterstitialSlot(
        format = format,
        scope = backgroundScope,
        clock = MonotonicClock { testScheduler.currentTime },
        unitId = { unit },
        maxReady = maxReady,
        shouldLoad = shouldLoadGate,
        mayRequestNow = { gateOpen },
        newAd = ::newAd,
        telemetry = telemetry,
        failuresInARow = { counter.failuresInARow(MaxInterstitialSlot.REWARDED_SLOT) },
        logger = logger,
        networkError = { networkError(it) },
        networkLog = networkLog,
    )

    private fun TestScope.at(millis: Long) {
        advanceTimeBy(millis - testScheduler.currentTime)
        runCurrent()
    }

    private fun loads(): Int = built.sumOf { it.loadCount }

    private fun ignoredLoads(): Int = built.sumOf { obj ->
        obj.calls.count { it == FakeMaxInterstitialSdkAd.LOAD_IGNORED }
    }

    private fun messages() = logger.entries.map { it.message }

    /** The slot loaded and holds its ad. */
    private fun TestScope.loaded(slot: MaxInterstitialSlot) {
        slot.loadIfNeeded()
        runCurrent()
        ad.emitLoaded()
        assertEquals(FullScreenLoadState.Ready, slot.state)
    }

    /** The last request was counted on the rewarded key: a failure now is counted there, with its latency. */
    private fun TestScope.assertSentOnRewardedKey(waitMillis: Long = 300) {
        val failuresBefore = counter.failuresInARow(REWARDED_KEY)
        advanceTimeBy(waitMillis)
        built.last().emitLoadFailed(204)
        assertEquals(failuresBefore + 1, counter.failuresInARow(REWARDED_KEY))
        assertEquals(0, counter.failuresInARow(MaxInterstitialSlot.SLOT))
        assertEquals(waitMillis, metrics.events.filterIsInstance<AdMetricEvent.LoadFail>().last().latencyMs)
    }

    /** Show callbacks of a turn, recorded in order. */
    private class RecordingShow : MaxInterstitialListener {
        val events = mutableListOf<String>()

        override fun onLoaded(info: MaxAdInfo) {
            events += "loaded"
        }

        override fun onLoadFailed(code: Int, networks: List<MaxNetworkAttempt>) {
            events += "loadFailed:$code"
        }

        override fun onDisplayed() {
            events += "displayed"
        }

        override fun onHidden() {
            events += "hidden"
        }

        override fun onDisplayFailed(code: Int) {
            events += "displayFailed:$code"
        }

        override fun onClicked() {
            events += "clicked"
        }

        override fun onRewarded() {
            events += "rewarded"
        }
    }

    // Counter key, metrics, log

    @Test
    fun load_sendsOnce_rewardedSlotKey() = runTest {
        val slot = slot()
        assertEquals("preload:rewarded/max", MaxInterstitialSlot.REWARDED_SLOT)
        assertEquals(REWARDED_KEY, slot.slot)
        assertEquals(REWARDED_KEY, MaxInterstitialSlot.slotOf(AdFormat.REWARDED))
        assertEquals(MaxInterstitialSlot.SLOT, MaxInterstitialSlot.slotOf(AdFormat.INTERSTITIAL))
        slot.loadIfNeeded()
        runCurrent()
        assertEquals(1, loads())
        assertEquals(FullScreenLoadState.Loading, slot.state)
        assertSentOnRewardedKey()
        val fail = metrics.events.filterIsInstance<AdMetricEvent.LoadFail>().single()
        assertEquals("REWARDED", fail.adFormat)
        assertEquals("preload", fail.adPlacement)
    }

    @Test
    fun loaded_rewardedMetrics_apiCalled() = runTest {
        val slot = slot()
        loaded(slot)
        assertEquals("REWARDED", metrics.events.filterIsInstance<AdMetricEvent.LoadSuccess>().single().adFormat)
        assertEquals(
            listOf(AdFunnelEvent.ApiCalled(AdFunnelKind.REWARDED, "REWARDED", UNIT, "")),
            funnel.events.filterIsInstance<AdFunnelEvent.ApiCalled>(),
        )
        assertTrue(messages().toString(), "Rewarded (MAX) loaded" in messages())
        assertTrue(messages().none { it.startsWith("Interstitial") })
    }

    @Test
    fun logs_neverContainUnitId() = runTest {
        val slot = slot()
        slot.loadIfNeeded()
        runCurrent()
        ad.emitLoadFailed(204)
        at(2_000)
        ad.emitLoaded()
        slot.take(A, RecordingShow())
        slot.showTaken(activity)
        ad.emitHidden()
        gateOpen = false
        at(5_000)
        // Control: the slot logged with its own label.
        assertTrue(messages().toString(), messages().any { it.startsWith("Rewarded (MAX)") })
        assertTrue(messages().none { UNIT in it })
    }

    // F-05

    @Test
    fun loadFailed_retriesAfterFullScreenDelay() = runTest {
        val slot = slot()
        slot.loadIfNeeded()
        runCurrent()
        ad.emitLoadFailed(204)
        assertEquals(FullScreenLoadState.WaitingRetry(2_000), slot.state)
        assertEquals("no_fill", slot.lastLoadError)
        at(1_999)
        assertEquals(1, loads())
        at(2_000)
        assertEquals(2, loads())
        ad.emitLoadFailed(204)
        // The second failure in a row on the rewarded key: 4 s.
        assertEquals(FullScreenLoadState.WaitingRetry(6_000), slot.state)
        at(5_999)
        assertEquals(2, loads())
        at(6_000)
        assertEquals(3, loads())
        val fails = metrics.events.filterIsInstance<AdMetricEvent.LoadFail>()
        assertEquals(listOf("REWARDED", "REWARDED"), fails.map { it.adFormat })
        assertEquals(2, counter.failuresInARow(REWARDED_KEY))
        assertTrue(messages().contains("Rewarded (MAX) failed to load: 204; retry in 4000 ms"))
        ad.emitLoaded()
        assertNull(slot.lastLoadError)
    }

    // R-03

    @Test
    fun loadFailed_networkFlag_setAtFailure_clearedAtNextSend() = runTest {
        networkError = { it == -1009 }
        val slot = slot()
        assertNull(slot.lastLoadNetworkError)
        slot.loadIfNeeded()
        runCurrent()
        assertNull(slot.lastLoadNetworkError)
        ad.emitLoadFailed(-1009)
        assertEquals(true, slot.lastLoadNetworkError)
        // The retry is sent: the flag is cleared before any result.
        at(2_000)
        assertEquals(2, loads())
        assertNull(slot.lastLoadNetworkError)
        ad.emitLoadFailed(204)
        assertEquals(false, slot.lastLoadNetworkError)
        at(6_000)
        assertEquals(3, loads())
        ad.emitLoaded()
        assertNull(slot.lastLoadNetworkError)
    }

    // F-06

    @Test
    fun hidden_reloadsAfterTwoSeconds() = runTest {
        val slot = slot()
        loaded(slot)
        assertSame(ad, slot.take(A, RecordingShow()))
        slot.showTaken(activity)
        ad.emitDisplayed()
        ad.emitHidden()
        // No onClosedOrShowFailed from the controller: the slot reloads by itself.
        at(1_999)
        assertEquals(1, loads())
        at(2_000)
        assertEquals(2, loads())
        assertEquals(0, ignoredLoads())
    }

    // KB-31, criterion 5; S-15

    @Test
    fun load_gateClosedAtSend_noRequest_idle_noRetry() = runTest {
        gateOpen = false
        val slot = slot()
        slot.loadIfNeeded()
        runCurrent()
        assertEquals(0, loads())
        assertEquals(FullScreenLoadState.Idle, slot.state)
        at(60_000)
        assertEquals(0, loads())
        assertTrue(messages().toString(), "Rewarded (MAX) not requested: consent gate closed" in messages())
        // Control: the gate open, the same call sends.
        gateOpen = true
        slot.loadIfNeeded()
        runCurrent()
        assertEquals(1, loads())
    }

    @Test
    fun retryDue_gateClosed_noRequest() = runTest {
        val slot = slot()
        slot.loadIfNeeded()
        runCurrent()
        ad.emitLoadFailed(204)
        gateOpen = false
        at(2_000)
        assertEquals(1, loads())
        assertEquals(FullScreenLoadState.Idle, slot.state)
        assertTrue(messages().toString(), "Rewarded (MAX) not requested: consent gate closed" in messages())
        // Control: the same retry with the gate open sends.
        gateOpen = true
        slot.loadIfNeeded()
        runCurrent()
        ad.emitLoadFailed(204)
        at(testScheduler.currentTime + 4_000)
        assertEquals(3, loads())
    }

    @Test
    fun reloadAfterClose_gateClosed_noRequest() = runTest {
        val slot = slot()
        loaded(slot)
        slot.take(A, RecordingShow())
        slot.showTaken(activity)
        ad.emitHidden()
        slot.onClosedOrShowFailed()
        gateOpen = false
        at(2_000)
        assertEquals(1, loads())
        assertEquals(FullScreenLoadState.Idle, slot.state)
        assertTrue(messages().toString(), "Rewarded (MAX) not requested: consent gate closed" in messages())
        // Control: the reload with the gate open sends.
        gateOpen = true
        slot.onClosedOrShowFailed()
        at(4_000)
        assertEquals(2, loads())
    }

    @Test
    fun maxReadyLater_gateClosed_noRequest() = runTest {
        maxReady.value = false
        val slot = slot()
        slot.loadIfNeeded()
        runCurrent()
        gateOpen = false
        maxReady.value = true
        runCurrent()
        assertEquals(0, loads())
        assertEquals(FullScreenLoadState.Idle, slot.state)
        assertTrue(messages().toString(), "Rewarded (MAX) not requested: consent gate closed" in messages())
        // Control: the same wait with the gate open sends once MAX is ready.
        gateOpen = true
        maxReady.value = false
        slot.loadIfNeeded()
        runCurrent()
        maxReady.value = true
        runCurrent()
        assertEquals(1, loads())
    }

    @Test
    fun shouldLoadSuspended_gateClosesMeanwhile_noRequest_idle_thenF07Sends() = runTest {
        val answer = CompletableDeferred<Boolean>()
        var first = true
        val slot = slot(shouldLoadGate = { if (first) answer.await() else true })
        slot.loadIfNeeded()
        runCurrent()
        assertEquals(FullScreenLoadState.Loading, slot.state)
        // The app goes to the background with an ERROR_ALLOWED consent (S-08): no request may go now.
        gateOpen = false
        answer.complete(true)
        runCurrent()
        assertEquals(0, loads())
        assertEquals(FullScreenLoadState.Idle, slot.state)
        assertTrue(messages().toString(), "Rewarded (MAX) not requested: consent gate closed" in messages())
        at(10_000)
        assertEquals(0, loads())
        // Back in the app, the next tap asks for a load (F-07).
        first = false
        gateOpen = true
        slot.loadIfNeeded()
        runCurrent()
        assertEquals(1, loads())
    }

    // Order or switches changed while the slot waits

    @Test
    fun retryDue_shouldLoadFalse_noRequest_idle() = runTest {
        val slot = slot()
        slot.loadIfNeeded()
        runCurrent()
        ad.emitLoadFailed(204)
        shouldLoad = false
        at(2_000)
        assertEquals(1, loads())
        assertEquals(FullScreenLoadState.Idle, slot.state)
        at(60_000)
        assertEquals(1, loads())
        assertTrue(
            messages().toString(),
            "Rewarded (MAX) not requested: not in order or placements off" in messages(),
        )
    }

    @Test
    fun reloadAfterClose_shouldLoadFalse_noRequest() = runTest {
        val slot = slot()
        loaded(slot)
        slot.take(A, RecordingShow())
        slot.showTaken(activity)
        ad.emitHidden()
        slot.onClosedOrShowFailed()
        shouldLoad = false
        at(2_000)
        assertEquals(1, loads())
        assertTrue(
            messages().toString(),
            "Rewarded (MAX) not requested: not in order or placements off" in messages(),
        )
    }

    @Test
    fun maxReadyLater_shouldLoadFalse_noRequest() = runTest {
        maxReady.value = false
        val slot = slot()
        slot.loadIfNeeded()
        runCurrent()
        shouldLoad = false
        maxReady.value = true
        runCurrent()
        assertEquals(0, loads())
        assertEquals(FullScreenLoadState.Idle, slot.state)
        assertTrue(
            messages().toString(),
            "Rewarded (MAX) not requested: not in order or placements off" in messages(),
        )
    }

    // L-072

    @Test
    fun load_beforeMaxReady_sentOnceWhenReady() = runTest {
        maxReady.value = false
        val slot = slot()
        slot.loadIfNeeded()
        runCurrent()
        assertEquals(0, loads())
        maxReady.value = true
        runCurrent()
        assertEquals(1, loads())
        assertEquals(FullScreenLoadState.Loading, slot.state)
        maxReady.value = false
        runCurrent()
        maxReady.value = true
        runCurrent()
        assertEquals(1, loads())
        assertSentOnRewardedKey()
    }

    // L-068

    @Test
    fun loadTwiceWhileLoading_oneRequest() = runTest {
        val slot = slot()
        slot.loadIfNeeded()
        slot.loadIfNeeded()
        runCurrent()
        slot.loadIfNeeded()
        runCurrent()
        assertEquals(1, loads())
        assertEquals(1, built.size)
        assertSentOnRewardedKey()
    }

    @Test
    fun loadTwice_whileShouldLoadSuspended_oneRequest() = runTest {
        val answer = CompletableDeferred<Boolean>()
        val slot = slot(shouldLoadGate = { answer.await() })
        slot.loadIfNeeded()
        runCurrent()
        assertEquals(FullScreenLoadState.Loading, slot.state)
        slot.loadIfNeeded()
        runCurrent()
        assertEquals(0, loads())
        answer.complete(true)
        runCurrent()
        assertEquals(1, loads())
        assertSentOnRewardedKey()
    }

    @Test
    fun take_twice_secondNull() = runTest {
        val slot = slot()
        loaded(slot)
        assertTrue("Rewarded (MAX) loaded" in messages())
        // Two placements share the slot: the second take gets nothing.
        assertNotNull(slot.take(A, RecordingShow()))
        assertNull(slot.take(A, RecordingShow()))
        assertEquals(FullScreenLoadState.Idle, slot.state)
    }

    // R-06 on the slot

    @Test
    fun displayFailedAfterDisplayed_keepsShowCallbacks_noLoad() = runTest {
        val slot = slot()
        loaded(slot)
        val show = RecordingShow()
        slot.take(A, show)
        slot.showTaken(activity)
        ad.emitDisplayed()
        // The second tap shows again; the SDK answers FULLSCREEN_AD_NOT_READY while the first show is on screen.
        slot.showTaken(activity)
        ad.emitDisplayFailed(-24)
        assertEquals(listOf("displayed", "displayFailed:-24"), show.events)
        slot.loadIfNeeded()
        runCurrent()
        assertEquals(1, loads())
        assertEquals(0, ignoredLoads())
        ad.emitRewarded()
        ad.emitHidden()
        assertEquals(listOf("displayed", "displayFailed:-24", "rewarded", "hidden"), show.events)

        // Control: the interstitial slot drops the show callbacks at the first failed show.
        built.clear()
        cached = null
        val inter = slot(format = AdFormat.INTERSTITIAL)
        inter.loadIfNeeded()
        runCurrent()
        val interAd = built.single()
        interAd.emitLoaded()
        val interShow = RecordingShow()
        inter.take(AdPlacement.GALLERY_INTERSTITIAL, interShow)
        inter.showTaken(activity)
        interAd.emitDisplayed()
        interAd.emitDisplayFailed(-24)
        interAd.emitHidden()
        assertEquals(listOf("displayed", "displayFailed:-24"), interShow.events)
    }

    @Test
    fun secondShowFailedBeforeFirstDisplayed_staysShowing_releaseThenNoLoad() = runTest {
        val slot = slot()
        loaded(slot)
        slot.take(A, RecordingShow())
        slot.showTaken(activity)
        slot.showTaken(activity)
        // The second show fails at once, before the first one is on screen.
        ad.emitDisplayFailed(-23)
        slot.releaseShow()
        slot.loadIfNeeded()
        runCurrent()
        assertEquals(1, loads())
        assertEquals(0, ignoredLoads())
        assertNotEquals(FullScreenLoadState.Loading, slot.state)
        ad.emitDisplayed()
        slot.loadIfNeeded()
        runCurrent()
        assertEquals(1, loads())
        assertEquals(0, ignoredLoads())
        // Control: the SDK reports the close; 2 s later one load reaches the SDK.
        ad.emitHidden()
        at(testScheduler.currentTime + 2_000)
        assertEquals(2, loads())
        assertEquals(0, ignoredLoads())
    }

    @Test
    fun displayFailedBeforeDisplayed_sdkNotShowing_reloadAfterTurnEnds() = runTest {
        val slot = slot()
        loaded(slot)
        val show = RecordingShow()
        slot.take(A, show)
        slot.showTaken(activity)
        ad.emitDisplayFailed(-24)
        assertEquals(listOf("displayFailed:-24"), show.events)
        slot.onClosedOrShowFailed()
        at(1_999)
        assertEquals(1, loads())
        at(2_000)
        assertEquals(2, loads())
        assertEquals(0, ignoredLoads())
        ad.emitLoaded()
        assertEquals(2, messages().count { it == "Rewarded (MAX) loaded" })
    }

    // R-02 on the slot

    @Test
    fun rewardedCallback_reachesShowCallbacks() = runTest {
        val slot = slot()
        loaded(slot)
        val show = RecordingShow()
        slot.take(A, show)
        slot.showTaken(activity)
        ad.emitDisplayed()
        ad.emitRewarded()
        assertEquals(listOf("displayed", "rewarded"), show.events)
        ad.emitHidden()
        ad.emitRewarded()
        assertEquals(listOf("displayed", "rewarded", "hidden"), show.events)
    }

    // The SDK object shows: no load

    @Test
    fun sdkShowing_loadIfNeeded_noLoad_thenHiddenReloads() = runTest {
        val slot = slot()
        loaded(slot)
        slot.take(A, RecordingShow())
        slot.showTaken(activity)
        // The controller ended the turn (R-04) while the SDK object still shows.
        slot.onClosedOrShowFailed()
        at(2_000)
        slot.loadIfNeeded()
        runCurrent()
        assertEquals(1, loads())
        assertEquals(0, ignoredLoads())
        assertNotEquals(FullScreenLoadState.Loading, slot.state)
        ad.emitHidden()
        at(4_000)
        assertEquals(2, loads())
        assertEquals(0, ignoredLoads())
    }

    @Test
    fun releasedWhileSdkShowing_noLoad_lateHiddenReloads() = runTest {
        val slot = slot()
        loaded(slot)
        val first = RecordingShow()
        slot.take(A, first)
        slot.showTaken(activity)
        // Stuck recovery (F-03): the controller drops the turn; the SDK object may still show.
        slot.releaseShow()
        slot.loadIfNeeded()
        runCurrent()
        assertEquals(1, loads())
        assertEquals(0, ignoredLoads())
        assertNotEquals(FullScreenLoadState.Loading, slot.state)
        ad.emitRewarded()
        assertTrue(first.events.isEmpty())
        // The SDK closes late: the slot loads again 2 s later on the same object.
        val closedAt = testScheduler.currentTime
        ad.emitHidden()
        at(closedAt + 1_999)
        assertEquals(1, loads())
        at(closedAt + 2_000)
        assertEquals(2, loads())
        assertEquals(1, built.size)
        assertEquals(0, ignoredLoads())
        ad.emitLoaded()
        val second = RecordingShow()
        slot.take(A, second)
        slot.showTaken(activity)
        ad.emitRewarded()
        assertEquals(listOf("rewarded"), second.events)
        assertTrue(first.events.isEmpty())
    }

    @Test
    fun sdkNeverCloses_afterCap_destroysAndLoadsNewObject() = runTest {
        val slot = slot()
        loaded(slot)
        val show = RecordingShow()
        slot.take(A, show)
        val shownAt = testScheduler.currentTime
        slot.showTaken(activity)
        slot.releaseShow()
        val old = ad
        at(shownAt + 599_999)
        slot.loadIfNeeded()
        runCurrent()
        assertEquals(1, loads())
        assertFalse(FakeMaxInterstitialSdkAd.DESTROY in old.calls)
        at(shownAt + 600_000)
        slot.loadIfNeeded()
        runCurrent()
        assertTrue(FakeMaxInterstitialSdkAd.DESTROY in old.calls)
        assertEquals(2, built.size)
        assertEquals(2, newAdCalls)
        val fresh = built[1]
        assertEquals(1, fresh.loadCount)
        assertEquals(1, old.loadCount)
        assertTrue(messages().contains("Rewarded (MAX) still showing after the cap: object dropped"))
        // Late callbacks of the old object reach nothing and set no reload.
        old.emitHidden()
        old.emitRewarded()
        assertTrue(show.events.isEmpty())
        at(shownAt + 700_000)
        assertEquals(1, fresh.loadCount)
        assertEquals(1, old.loadCount)
    }

    // Plan T-072: network log, revenue by the placement of the take, platform, funnel adapter

    @Test
    fun loadedAndFailed_logNetworks_whenLogOn() = runTest {
        val slot = slot(networkLog = AdNetworkLog(true, logger))
        slot.loadIfNeeded()
        runCurrent()
        ad.emitLoadFailed(204, listOf(MaxNetworkAttempt("Mintegral", 40, MaxNetworkResult.FAILED, 204)))
        at(2_000)
        ad.emitLoaded(info(networks = listOf(MaxNetworkAttempt("Google", 120, MaxNetworkResult.LOADED, null))))
        assertEquals(
            listOf(
                "Network preload:rewarded/max: Mintegral, 40 ms, error 204",
                "Network preload:rewarded/max: Google, 120 ms, loaded",
            ),
            networkLines(),
        )
        assertTrue(messages().none { UNIT in it || AD_UNIT in it })
    }

    @Test
    fun networkLogOff_noNetworkLines() = runTest {
        val slot = slot()
        slot.loadIfNeeded()
        runCurrent()
        ad.emitLoadFailed(204, listOf(MaxNetworkAttempt("Mintegral", 40, MaxNetworkResult.FAILED, 204)))
        assertEquals(emptyList<String>(), networkLines())

        // Control: the same failure on a slot with the log on writes the line.
        val logged = slot(networkLog = AdNetworkLog(true, logger))
        cached = null
        logged.loadIfNeeded()
        runCurrent()
        built.last().emitLoadFailed(204, listOf(MaxNetworkAttempt("Mintegral", 40, MaxNetworkResult.FAILED, 204)))
        assertTrue(messages().toString(), networkLines().isNotEmpty())
    }

    @Test
    fun revenue_placementOfTake() = runTest {
        val slot = slot()
        loaded(slot)
        slot.take(A, RecordingShow())
        slot.showTaken(activity)
        ad.emitDisplayed()
        ad.emitRevenue(info())
        assertEquals(
            listOf(
                AdMetricEvent.ImpressionRevenue(
                    adFormat = "REWARDED",
                    adSource = "AppLovin",
                    monetizationNetwork = "AppLovin",
                    adUnitName = UNIT,
                    currency = "USD",
                    value = 0.0123,
                    adPlacement = "gallery_rw_action",
                    adapterClassName = ADAPTER,
                    platform = AdMetricPlatform.APPLOVIN,
                ),
            ),
            revenues(),
        )
    }

    @Test
    fun twoPlacements_sequentialTakes_eachRevenueOwnPlacement() = runTest {
        val slot = slot()
        loaded(slot)
        slot.take(A, RecordingShow())
        slot.showTaken(activity)
        ad.emitDisplayed()
        ad.emitRevenue(info())
        val closedAt = testScheduler.currentTime
        ad.emitHidden()
        at(closedAt + 2_000)
        assertEquals(2, loads())
        ad.emitLoaded()
        slot.take(B, RecordingShow())
        slot.showTaken(activity)
        ad.emitRevenue(info())
        assertEquals(listOf(A.key, B.key), revenues().map { it.adPlacement })
    }

    @Test
    fun sameWrapperReused_revenueListenerOnce_placementOfLatestTake() = runTest {
        val slot = slot()
        loaded(slot)
        val listener = ad.onRevenue
        assertNotNull(listener)
        slot.take(A, RecordingShow())
        slot.showTaken(activity)
        slot.releaseShow()
        // The SDK closes late; the slot loads again 2 s later on the same wrapper.
        val closedAt = testScheduler.currentTime
        ad.emitHidden()
        at(closedAt + 2_000)
        assertEquals(2, loads())
        ad.emitLoaded()
        slot.take(B, RecordingShow())

        ad.emitRevenue(info())

        assertEquals(listOf(B.key), revenues().map { it.adPlacement })
        assertSame(listener, ad.onRevenue)
        assertEquals(1, built.size)
    }

    @Test
    fun rewardedWrapperRefetched_revenueListenerNotReattached() = runTest {
        // The branch "already has a revenue listener": the slot drops its object without destroying it, then newAd
        // hands back the same wrapper (as MaxPlatform.rewarded does for one unit). Built here with the slot that drops
        // an object itself (the end of a turn with no close), since the rewarded slot of today never does.
        val slot = MaxInterstitialSlot(
            format = AdFormat.INTERSTITIAL,
            scope = backgroundScope,
            clock = MonotonicClock { testScheduler.currentTime },
            unitId = { UNIT },
            maxReady = maxReady,
            shouldLoad = { true },
            mayRequestNow = { true },
            newAd = ::newAd,
            telemetry = telemetry,
            failuresInARow = { 0 },
            logger = logger,
            fullScreenBusy = { false },
        )
        slot.loadIfNeeded()
        runCurrent()
        ad.emitLoaded()
        val listener = ad.onRevenue
        assertNotNull(listener)
        slot.take(A, RecordingShow())
        slot.loadIfNeeded()
        runCurrent()
        assertEquals(2, newAdCalls)
        assertEquals(1, built.size)
        ad.emitLoaded()
        slot.take(B, RecordingShow())

        ad.emitRevenue(info())

        assertSame(listener, ad.onRevenue)
        assertEquals(listOf(B.key), revenues().map { it.adPlacement })
    }

    @Test
    fun wrapperDestroyedAtCap_newWrapperGetsRevenueListener() = runTest {
        val slot = slot()
        loaded(slot)
        slot.take(A, RecordingShow())
        val shownAt = testScheduler.currentTime
        slot.showTaken(activity)
        slot.releaseShow()
        at(shownAt + 600_000)
        slot.loadIfNeeded()
        runCurrent()
        val old = built[0]
        val fresh = built[1]
        assertTrue(FakeMaxInterstitialSdkAd.DESTROY in old.calls)
        fresh.emitLoaded()
        slot.take(B, RecordingShow())
        assertNotNull(fresh.onRevenue)

        fresh.emitRevenue(info())
        old.emitRevenue(info())

        assertEquals(listOf(B.key, A.key), revenues().map { it.adPlacement })
    }

    @Test
    fun loadSuccessAndFail_applovinPlatform() = runTest {
        val slot = slot()
        slot.loadIfNeeded()
        runCurrent()
        ad.emitLoadFailed(204)
        at(2_000)
        ad.emitLoaded()
        assertEquals(
            listOf(
                AdMetricEvent.LoadFail("REWARDED", "preload", 1, 0, "204", "no_fill", AdMetricPlatform.APPLOVIN),
                AdMetricEvent.LoadSuccess("REWARDED", "preload", 2, 0, AdMetricPlatform.APPLOVIN),
            ),
            metrics.events,
        )
    }

    @Test
    fun loaded_apiCalled_withAdapterOfLoadedNetwork() = runTest {
        val slot = slot()
        slot.loadIfNeeded()
        runCurrent()
        ad.emitLoaded(info())
        assertEquals(
            listOf(AdFunnelEvent.ApiCalled(AdFunnelKind.REWARDED, "REWARDED", UNIT, ADAPTER)),
            funnel.events.filterIsInstance<AdFunnelEvent.ApiCalled>(),
        )
        assertEquals(ADAPTER, slot.heldAdapterClassName)
    }

    private fun revenues() = metrics.events.filterIsInstance<AdMetricEvent.ImpressionRevenue>()

    private fun networkLines(): List<String> = messages().filter { it.startsWith("Network ") }

    private fun info(networks: List<MaxNetworkAttempt> = emptyList()) = MaxAdInfo(
        adUnitId = AD_UNIT,
        revenueUsd = 0.0123,
        networkName = "AppLovin",
        revenuePrecision = "exact",
        adapterClassName = ADAPTER,
        networks = networks,
    )

    private companion object {
        const val UNIT = "FAKE_UNIT_R"
        const val AD_UNIT = "FAKE_UNIT_OF_AD"
        const val ADAPTER = "com.applovin.mediation.adapters.GoogleMediationAdapter"
        val A = AdPlacement.GALLERY_RW_ACTION

        /** A second placement: the slot keys revenue by its SDK object, whatever the format of the placement. */
        val B = AdPlacement.GALLERY_INTERSTITIAL
        const val REWARDED_KEY = "preload:rewarded/max"
        val FLAGS: AdsGlobalFlags = runBlocking { DefaultAdsFlags(FakeRemoteSettings()).global.first() }
    }
}
