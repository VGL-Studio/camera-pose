package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.analytics.AdMetricEvent
import com.vglstudio.camera.pose.line.guide.core.analytics.AdMetricPlatform
import com.vglstudio.camera.pose.line.guide.core.analytics.RecordingAdFunnel
import com.vglstudio.camera.pose.line.guide.core.analytics.RecordingAdMetrics
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The app open slot of AppLovin MAX (plan T-070): the shared MAX full-screen slot built with `format = APP_OPEN` on a
 * virtual clock. It reloads 2 seconds after a close or a failed show and holds every other send until then (F-06,
 * ADR-0045, section 2.4), retries by F-05, drops an ad an hour old (A-07), and asks the order, switches and consent
 * gate right before every send (S-15). Interstitial and rewarded slots keep their behavior (controls).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MaxAppOpenSlotTest {
    private val logger = RecordingAppLogger()
    private val metrics = RecordingAdMetrics()
    private val funnel = RecordingAdFunnel()
    private val counter = LoadAttemptCounter()
    private val maxReady = MutableStateFlow(true)
    private var shouldLoad = true
    private var gateOpen = true
    private val built = mutableListOf<FakeMaxInterstitialSdkAd>()
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

    private fun TestScope.slot(format: AdFormat = AdFormat.APP_OPEN, networkLog: AdNetworkLog = AdNetworkLog.OFF) =
        MaxInterstitialSlot(
            format = format,
            scope = backgroundScope,
            clock = MonotonicClock { testScheduler.currentTime },
            unitId = { UNIT },
            maxReady = maxReady,
            shouldLoad = { shouldLoad },
            mayRequestNow = { gateOpen },
            newAd = { FakeMaxInterstitialSdkAd(it).also { fake -> built += fake } },
            telemetry = telemetry,
            failuresInARow = { counter.failuresInARow(MaxInterstitialSlot.slotOf(format)) },
            logger = logger,
            networkLog = networkLog,
        )

    /** The key of the load counter the controller reads for the MAX app open slot. */
    private fun TestScope.appOpenSlot() = MaxInterstitialSlot(
        format = AdFormat.APP_OPEN,
        scope = backgroundScope,
        clock = MonotonicClock { testScheduler.currentTime },
        unitId = { UNIT },
        maxReady = maxReady,
        shouldLoad = { shouldLoad },
        mayRequestNow = { gateOpen },
        newAd = { FakeMaxInterstitialSdkAd(it).also { fake -> built += fake } },
        telemetry = telemetry,
        failuresInARow = { counter.failuresInARow(APP_OPEN_COUNTER) },
        logger = logger,
    )

    private fun TestScope.at(millis: Long) {
        advanceTimeBy(millis - testScheduler.currentTime)
        runCurrent()
    }

    private fun loads(): Int = built.sumOf { it.loadCount }

    private fun messages(): List<String> = logger.entries.map { it.message }

    /** Loads the slot at the current time, the ad answers loaded, then it is taken out to show. */
    private fun TestScope.loadedAndTaken(slot: MaxInterstitialSlot, onShow: MaxInterstitialListener? = null) {
        slot.loadIfNeeded()
        runCurrent()
        ad.emitLoaded()
        assertEquals(FullScreenLoadState.Ready, slot.state)
        assertSame(ad, slot.take(AdPlacement.RESUME, onShow))
    }

    // F-06, ADR-0045 section 2.4

    @Test
    fun closed_holdsEveryLoadUntilTwoSeconds() = runTest {
        val slot = appOpenSlot()
        loadedAndTaken(slot)
        slot.onClosedOrShowFailed()
        slot.loadIfNeeded()
        runCurrent()
        assertEquals(1, loads())
        at(1_000)
        slot.loadIfNeeded()
        runCurrent()
        assertEquals(1, loads())
        at(1_999)
        slot.loadIfNeeded()
        runCurrent()
        assertEquals(1, loads())
        assertEquals(FullScreenLoadState.Idle, slot.state)
        // A held send changes no state and logs nothing.
        assertTrue(messages().toString(), messages().none { "not requested" in it })
        at(2_000)
        assertEquals(2, loads())
        slot.loadIfNeeded()
        runCurrent()
        assertEquals(2, loads())
        assertEquals(FullScreenLoadState.Loading, slot.state)
    }

    @Test
    fun showFailed_holdsThenReloadsAfterTwoSeconds() = runTest {
        val slot = appOpenSlot()
        val failed = mutableListOf<Int>()
        val onShow = object : MaxInterstitialListener {
            override fun onLoaded(info: MaxAdInfo) = Unit

            override fun onLoadFailed(code: Int, networks: List<MaxNetworkAttempt>) = Unit

            override fun onDisplayed() = Unit

            override fun onHidden() = Unit

            override fun onDisplayFailed(code: Int) {
                failed += code
                slot.onClosedOrShowFailed()
            }

            override fun onClicked() = Unit
        }
        loadedAndTaken(slot, onShow)
        ad.emitDisplayFailed(-23)
        assertEquals(listOf(-23), failed)
        at(1_000)
        slot.loadIfNeeded()
        runCurrent()
        at(1_999)
        assertEquals(1, loads())
        at(2_000)
        assertEquals(2, loads())
    }

    @Test
    fun interstitialFormat_noHoldAfterClose() = runTest {
        // Control: the interstitial slot is not held after a close; another load sends as before (T-069).
        val slot = slot(AdFormat.INTERSTITIAL)
        loadedAndTaken(slot)
        slot.onClosedOrShowFailed()
        at(1_000)
        slot.loadIfNeeded()
        runCurrent()
        assertEquals(2, loads())
        // The reload due at 2 s finds the slot loading: no second request.
        at(2_000)
        assertEquals(2, loads())
    }

    @Test
    fun rewardedFormat_noHoldAfterClose() = runTest {
        // Control: likewise for the rewarded slot (T-106).
        val slot = slot(AdFormat.REWARDED)
        loadedAndTaken(slot)
        slot.onClosedOrShowFailed()
        at(1_000)
        slot.loadIfNeeded()
        runCurrent()
        assertEquals(2, loads())
        at(2_000)
        assertEquals(2, loads())
    }

    // F-05

    @Test
    fun loadFailed_retriesByF05_notFixedTwoSeconds() = runTest {
        val slot = appOpenSlot()
        slot.loadIfNeeded()
        runCurrent()
        ad.emitLoadFailed(204)
        assertEquals(FullScreenLoadState.WaitingRetry(2_000), slot.state)
        at(1_999)
        assertEquals(1, loads())
        at(2_000)
        assertEquals(2, loads())
        ad.emitLoadFailed(204)
        assertEquals(FullScreenLoadState.WaitingRetry(6_000), slot.state)
        at(5_999)
        assertEquals(2, loads())
        at(6_000)
        assertEquals(3, loads())
        ad.emitLoadFailed(204)
        assertEquals(FullScreenLoadState.WaitingRetry(12_000), slot.state)
        at(11_999)
        assertEquals(3, loads())
        at(12_000)
        assertEquals(4, loads())
    }

    // A-07

    @Test
    fun loadedAd_expiresAfterOneHour_newObjectLoads() = runTest {
        val slot = appOpenSlot()
        slot.loadIfNeeded()
        runCurrent()
        ad.emitLoaded()
        at(3_599_999)
        assertEquals(FullScreenLoadState.Ready, slot.state)
        at(3_600_000)
        assertEquals(FullScreenLoadState.Idle, slot.state)
        // Reading the state has no side effect.
        assertTrue(messages().none { it == "App open (MAX) expired" })
        assertFalse(built[0].calls.contains(FakeMaxInterstitialSdkAd.DESTROY))
        assertNull(slot.take(AdPlacement.RESUME))
        val old = built[0]
        assertTrue(old.calls.toString(), old.calls.contains(FakeMaxInterstitialSdkAd.DESTROY))
        assertNull(old.listener)
        assertEquals(1, messages().count { it == "App open (MAX) expired" })
        slot.loadIfNeeded()
        runCurrent()
        assertEquals(2, built.size)
        assertEquals(1, old.loadCount)
        assertEquals(1, built[1].loadCount)
        assertEquals(1, messages().count { it == "App open (MAX) expired" })
    }

    @Test
    fun interstitialFormat_neverExpires() = runTest {
        // Control: the interstitial slot keeps its ad (no age limit of A-07 for it).
        val slot = slot(AdFormat.INTERSTITIAL)
        slot.loadIfNeeded()
        runCurrent()
        ad.emitLoaded()
        at(7_200_000)
        assertEquals(FullScreenLoadState.Ready, slot.state)
        assertSame(ad, slot.take(AdPlacement.GALLERY_INTERSTITIAL))
        assertFalse(ad.calls.contains(FakeMaxInterstitialSdkAd.DESTROY))
    }

    // Metrics and log

    @Test
    fun metrics_appOpenFormat_slotKey_noFunnel_noNetworkFlag() = runTest {
        val slot = appOpenSlot()
        slot.loadIfNeeded()
        runCurrent()
        assertNull(slot.lastLoadNetworkError)
        ad.emitLoadFailed(204)
        val fail = metrics.events.filterIsInstance<AdMetricEvent.LoadFail>().single()
        assertEquals("APP_OPEN", fail.adFormat)
        assertEquals("preload", fail.adPlacement)
        assertEquals("no_fill", slot.lastLoadError)
        assertEquals(1, counter.failuresInARow(APP_OPEN_COUNTER))
        assertEquals(0, counter.failuresInARow(MaxInterstitialSlot.SLOT))
        // App open has no network rule (R-03): a failure is never a network one.
        assertEquals(false, slot.lastLoadNetworkError)
        at(2_000)
        ad.emitLoaded()
        assertNull(slot.lastLoadError)
        assertNull(slot.lastLoadNetworkError)
        assertEquals("APP_OPEN", metrics.events.filterIsInstance<AdMetricEvent.LoadSuccess>().single().adFormat)
        assertEquals(0, counter.failuresInARow(APP_OPEN_COUNTER))
        // App open has no funnel event (as app open of AdMob).
        assertTrue(funnel.events.toString(), funnel.events.isEmpty())
    }

    @Test
    fun appOpenFormat_acceptedByRequire() = runTest {
        val slot = appOpenSlot()
        assertEquals("preload:app_open/max", MaxInterstitialSlot.slotOf(AdFormat.APP_OPEN))
        assertEquals("preload:app_open/max", slot.slot)
        // Control: a format with no MAX slot is still refused.
        val error = runCatching { slot(AdFormat.BANNER) }.exceptionOrNull()
        assertTrue("expected IllegalArgumentException, got $error", error is IllegalArgumentException)
    }

    @Test
    fun logs_appOpenLabel_neverUnitId() = runTest {
        val slot = appOpenSlot()
        slot.loadIfNeeded()
        runCurrent()
        ad.emitLoadFailed(204)
        at(2_000)
        ad.emitLoaded()
        slot.take(AdPlacement.RESUME)
        slot.onClosedOrShowFailed()
        gateOpen = false
        at(4_000)
        val slotLines = messages().filter { "(MAX)" in it }
        // Control: the slot logged something.
        assertTrue(messages().toString(), slotLines.size >= 3)
        assertTrue(slotLines.toString(), slotLines.all { it.startsWith("App open (MAX) ") })
        assertTrue(messages().none { UNIT in it })
    }

    // S-15: every way to a send asks the gate right before sending

    @Test
    fun reloadDue_gateClosed_noRequest_idle() = runTest {
        val slot = appOpenSlot()
        loadedAndTaken(slot)
        slot.onClosedOrShowFailed()
        gateOpen = false
        at(1_999)
        // The reload is not due yet: the gate was not asked.
        assertTrue(messages().none { "not requested" in it })
        at(2_000)
        assertEquals(1, loads())
        assertEquals(FullScreenLoadState.Idle, slot.state)
        assertTrue(messages().toString(), "App open (MAX) not requested: consent gate closed" in messages())
        at(60_000)
        assertEquals(1, loads())
        // Control: the gate open, the same slot sends.
        gateOpen = true
        slot.loadIfNeeded()
        runCurrent()
        assertEquals(2, loads())
    }

    @Test
    fun retryDue_shouldLoadFalse_noRequest_idle() = runTest {
        val slot = appOpenSlot()
        slot.loadIfNeeded()
        runCurrent()
        ad.emitLoadFailed(204)
        // MAX left the order, or the resume ad is off or became an interstitial.
        shouldLoad = false
        at(2_000)
        assertEquals(1, loads())
        assertEquals(FullScreenLoadState.Idle, slot.state)
        assertTrue(
            messages().toString(),
            "App open (MAX) not requested: not in order or placements off" in messages(),
        )
        at(60_000)
        assertEquals(1, loads())
    }

    @Test
    fun maxReadyLater_gateClosed_noRequest() = runTest {
        maxReady.value = false
        val slot = appOpenSlot()
        slot.loadIfNeeded()
        runCurrent()
        gateOpen = false
        maxReady.value = true
        runCurrent()
        assertEquals(0, loads())
        assertEquals(FullScreenLoadState.Idle, slot.state)
        assertTrue(messages().toString(), "App open (MAX) not requested: consent gate closed" in messages())
    }

    // Plan T-072: network log, revenue of `resume`, platform

    @Test
    fun loadedAndFailed_logNetworks_whenLogOn() = runTest {
        val slot = slot(networkLog = AdNetworkLog(true, logger))
        slot.loadIfNeeded()
        runCurrent()
        ad.emitLoadFailed(204, listOf(MaxNetworkAttempt("Mintegral", 40, MaxNetworkResult.FAILED, 204)))
        at(2_000)
        ad.emitLoaded(info(networks = listOf(MaxNetworkAttempt("AppLovin", 90, MaxNetworkResult.LOADED, null))))
        assertEquals(
            listOf(
                "Network preload:app_open/max: Mintegral, 40 ms, error 204",
                "Network preload:app_open/max: AppLovin, 90 ms, loaded",
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
        logged.loadIfNeeded()
        runCurrent()
        built.last().emitLoadFailed(204, listOf(MaxNetworkAttempt("Mintegral", 40, MaxNetworkResult.FAILED, 204)))
        assertTrue(messages().toString(), networkLines().isNotEmpty())
    }

    @Test
    fun revenue_placementOfTake() = runTest {
        val slot = slot()
        loadedAndTaken(slot)
        ad.emitDisplayed()
        ad.emitRevenue(info())
        assertEquals(
            listOf(
                AdMetricEvent.ImpressionRevenue(
                    adFormat = "APP_OPEN",
                    adSource = "AppLovin",
                    monetizationNetwork = "AppLovin",
                    adUnitName = UNIT,
                    currency = "USD",
                    value = 0.0123,
                    adPlacement = "resume",
                    adapterClassName = ADAPTER,
                    platform = AdMetricPlatform.APPLOVIN,
                ),
            ),
            metrics.events.filterIsInstance<AdMetricEvent.ImpressionRevenue>(),
        )
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
                AdMetricEvent.LoadFail("APP_OPEN", "preload", 1, 0, "204", "no_fill", AdMetricPlatform.APPLOVIN),
                AdMetricEvent.LoadSuccess("APP_OPEN", "preload", 2, 0, AdMetricPlatform.APPLOVIN),
            ),
            metrics.events,
        )
    }

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
        const val UNIT = "FAKE_UNIT_AO"
        const val AD_UNIT = "FAKE_UNIT_OF_AD"
        const val ADAPTER = "com.applovin.mediation.adapters.GoogleMediationAdapter"

        /** The key the controller reads the failures in a row of the MAX app open slot with. */
        const val APP_OPEN_COUNTER = "preload:app_open/max"
        val FLAGS: AdsGlobalFlags = runBlocking { DefaultAdsFlags(FakeRemoteSettings()).global.first() }
    }
}
