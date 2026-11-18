package com.vglstudio.camera.pose.line.guide.core.analytics

import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tenjin behind the interfaces of :core:analytics (plan T-099, ADR-1010; spec ads, question 21): no key touches
 * nothing, a connection only once both start and the consent signal came, the consent of UMP applied right before
 * every connection and every revenue, the revenue of paid AdMob impressions only, one switch, errors swallowed. The ad
 * funnel events by name (plan T-100), through the same gate, one switch each. The SDK is a fake.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TenjinTrackerTest {
    private val sink = FakeTenjinSink()
    private val settings = FakeRemoteSettings()
    private val foreground = FakeAppForeground()
    private val logger = RecordingAppLogger()

    private fun tracker(
        scope: CoroutineScope,
        sdkKey: String = SDK_KEY,
        sink: FakeTenjinSink = this.sink,
        foreground: FakeAppForeground = this.foreground,
    ) = TenjinTracker(
        config = TenjinConfig(sdkKey),
        sink = sink,
        switches = TenjinSwitches(settings, scope),
        foreground = foreground,
        logger = logger,
    )

    /** Prepared, the consent known, started: the usual state of a session with a key. */
    private fun TestScope.started(sdkKey: String = SDK_KEY, sink: FakeTenjinSink = this@TenjinTrackerTest.sink) =
        tracker(backgroundScope, sdkKey, sink).apply {
            prepare()
            onConsentKnown()
            start()
        }

    private fun runUnconfined(body: suspend TestScope.() -> Unit) = runTest(UnconfinedTestDispatcher()) { body() }

    @Test
    fun noKey_prepareAndStart_neverTouchSdk() = runUnconfined {
        val tracker = tracker(backgroundScope, sdkKey = "")
        tracker.prepare()
        tracker.prepare()
        tracker.onConsentKnown()
        tracker.start()
        tracker.log(revenue())
        foreground.fire()

        assertTrue(sink.calls.toString(), sink.calls.isEmpty())
        val off = logger.entries.filter { it.message == "Tenjin off: no SDK key in this build" }
        assertEquals(1, off.size)
        assertEquals("Analytics", off.single().tag)

        // Control: the same calls with a key reach the SDK, init first.
        val withKey = FakeTenjinSink()
        val other = tracker(backgroundScope, sink = withKey)
        other.prepare()
        other.onConsentKnown()
        other.start()
        assertEquals("init", withKey.calls.firstOrNull())
        assertEquals(listOf(SDK_KEY), withKey.keys)
    }

    @Test
    fun start_appliesConsentThenConnects_once() = runUnconfined {
        val tracker = tracker(backgroundScope)
        tracker.prepare()
        tracker.onConsentKnown()
        tracker.start()
        tracker.start()

        assertEquals(listOf("init", "applyConsent", "connect"), sink.calls)
        assertTrue(logger.entries.any { it.message == "Tenjin connected" })
    }

    @Test
    fun startBeforePrepare_doesNothing() = runUnconfined {
        val tracker = tracker(backgroundScope)
        tracker.onConsentKnown()
        tracker.start()
        assertTrue(sink.calls.toString(), sink.calls.isEmpty())

        // Control: prepared, the next start connects.
        tracker.prepare()
        tracker.start()
        assertEquals(listOf("init", "applyConsent", "connect"), sink.calls)
    }

    @Test
    fun startWithoutConsentKnown_noConnect_thenKnown_connectsOnce() = runUnconfined {
        val tracker = tracker(backgroundScope)
        tracker.prepare()
        tracker.start()
        assertEquals(listOf("init"), sink.calls)

        foreground.fire()
        assertEquals(listOf("init"), sink.calls)

        tracker.onConsentKnown()
        assertEquals(listOf("init", "applyConsent", "connect"), sink.calls)

        tracker.onConsentKnown()
        assertEquals(listOf("init", "applyConsent", "connect"), sink.calls)
    }

    @Test
    fun consentKnownBeforeStart_connectsAtStart_once() = runUnconfined {
        val tracker = tracker(backgroundScope)
        tracker.prepare()
        tracker.onConsentKnown()
        assertEquals(listOf("init"), sink.calls)

        tracker.start()
        assertEquals(listOf("init", "applyConsent", "connect"), sink.calls)

        tracker.start()
        assertEquals(listOf("init", "applyConsent", "connect"), sink.calls)
    }

    @Test
    fun revenueBeforeConsentKnown_dropped() = runUnconfined {
        val tracker = tracker(backgroundScope)
        tracker.prepare()
        tracker.start()
        tracker.log(revenue())
        assertFalse(sink.calls.toString(), "adMobImpression" in sink.calls)

        tracker.onConsentKnown()
        tracker.log(revenue())
        assertEquals(1, sink.calls.count { it == "adMobImpression" })
    }

    @Test
    fun consentRefused_atStart_noConnect() = runUnconfined {
        sink.consent = false
        started()

        assertEquals(listOf("init", "applyConsent"), sink.calls)
        assertTrue(logger.entries.any { it.message == "Tenjin not connected: no consent" })
    }

    @Test
    fun consentWithdrawn_thenReturnToForeground_noConnect_thenGrantedAgain_connects() = runUnconfined {
        started()
        assertEquals(listOf("init", "applyConsent", "connect"), sink.calls)

        sink.consent = false
        foreground.fire()
        assertEquals(listOf("init", "applyConsent", "connect", "applyConsent"), sink.calls)

        sink.consent = true
        foreground.fire()
        assertEquals(
            listOf("init", "applyConsent", "connect", "applyConsent", "applyConsent", "connect"),
            sink.calls,
        )
    }

    @Test
    fun start_thenEachReturnToForeground_connectsAgain() = runUnconfined {
        val tracker = tracker(backgroundScope)
        tracker.prepare()
        tracker.onConsentKnown()
        // Before start: a return to foreground calls nothing after init.
        foreground.fire()
        assertEquals(listOf("init"), sink.calls)

        tracker.start()
        foreground.fire()
        foreground.fire()
        assertEquals(
            listOf("init", "applyConsent", "connect", "applyConsent", "connect", "applyConsent", "connect"),
            sink.calls,
        )
    }

    @Test
    fun paidAdMobImpression_sentOnce() = runUnconfined {
        val tracker = started()
        tracker.log(revenue(value = 0.0123, currency = "USD"))

        assertEquals(1, sink.calls.count { it == "adMobImpression" })
        assertEquals(12_300L, sink.payloads.single()["value_micros"])
    }

    @Test
    fun nonRevenueMetrics_ignored() = runUnconfined {
        val tracker = started()
        val before = sink.calls.toList()
        listOf(
            AdMetricEvent.LoadSuccess("INTER", "preload", 1, 10),
            AdMetricEvent.LoadFail("INTER", "preload", 1, 10, "3", "no_fill"),
            AdMetricEvent.Clicked("INTER", "p"),
            AdMetricEvent.Opportunity("INTER", "p", "shown", "ready"),
            AdMetricEvent.ShowFail("INTER", "p", "1", "ad_reused"),
        ).forEach { tracker.log(it) }
        assertEquals(before, sink.calls)

        // Control: a revenue event is sent by the same tracker.
        tracker.log(revenue())
        assertEquals(before + listOf("applyConsent", "adMobImpression"), sink.calls)
    }

    @Test
    fun revenueBeforeStart_dropped() = runUnconfined {
        val tracker = tracker(backgroundScope)
        tracker.prepare()
        tracker.onConsentKnown()
        tracker.log(revenue())
        assertFalse(sink.calls.toString(), "adMobImpression" in sink.calls)

        // Control: started, the next revenue is sent.
        tracker.start()
        tracker.log(revenue())
        assertEquals(1, sink.calls.count { it == "adMobImpression" })
    }

    @Test
    fun consentRefused_revenueNotSent() = runUnconfined {
        val tracker = started()
        sink.consent = false
        val before = sink.calls.toList()
        tracker.log(revenue())

        assertEquals(before + "applyConsent", sink.calls)
        assertTrue(logger.entries.any { it.message == "Tenjin ad revenue opted out" })
    }

    @Test
    fun consentWithdrawnAfterStart_nextRevenueNotSent() = runUnconfined {
        val tracker = started()
        tracker.log(revenue())
        assertEquals(1, sink.payloads.size)

        sink.consent = false
        tracker.log(revenue())
        assertEquals(1, sink.payloads.size)

        sink.consent = true
        tracker.log(revenue())
        assertEquals(2, sink.payloads.size)

        // The consent is applied right before every impression sent.
        sink.calls.forEachIndexed { index, call ->
            if (call == "adMobImpression") assertEquals(sink.calls.toString(), "applyConsent", sink.calls[index - 1])
        }
    }

    @Test
    fun revenueSwitchOff_notSent_thenOnAgain() = runUnconfined {
        val tracker = started()
        settings.set(TenjinSettings.IS_LOG_TENJIN_AD_REVENUE, false)
        tracker.log(revenue())
        assertTrue(sink.payloads.isEmpty())

        settings.set(TenjinSettings.IS_LOG_TENJIN_AD_REVENUE, true)
        tracker.log(revenue())
        assertEquals(1, sink.payloads.size)
    }

    @Test
    fun sdkThrows_isSwallowedAndLogged() = runUnconfined {
        // One tracker per failing call: a failing init leaves the tracker unprepared, so it never connects.
        val initFails = FakeTenjinSink().apply { failOn = "init" }
        started(sink = initFails)
        val connectFails = FakeTenjinSink().apply { failOn = "connect" }
        started(sink = connectFails)
        val revenueFails = FakeTenjinSink().apply { failOn = "adMobImpression" }
        started(sink = revenueFails).log(revenue())

        assertEquals(listOf("init"), initFails.calls)
        assertEquals(listOf("init", "applyConsent", "connect"), connectFails.calls)
        assertEquals("adMobImpression", revenueFails.calls.last())
        val failures = logger.entries.filter { it.level == LogLevel.WARN }.map { it.message }
        assertEquals(
            listOf(
                "Tenjin init failed: IllegalStateException",
                "Tenjin connect failed: IllegalStateException",
                "Tenjin ad revenue failed: IllegalStateException",
            ),
            failures,
        )
        // Never the key nor the value in a log line.
        assertTrue(logger.entries.none { SDK_KEY in it.message || "12300" in it.message })
    }

    @Test
    fun consentCheckThrows_neitherConnectsNorSends() = runUnconfined {
        // A consent check that throws closes the gate: no connection at start or on return, no revenue.
        val consentFails = FakeTenjinSink().apply { failOn = "applyConsent" }
        val tracker = started(sink = consentFails)
        tracker.log(revenue())
        foreground.fire()

        assertEquals(listOf("init", "applyConsent", "applyConsent", "applyConsent"), consentFails.calls)
        assertFalse("connect" in consentFails.calls)
        assertFalse("adMobImpression" in consentFails.calls)
        val failures = logger.entries.filter { it.level == LogLevel.WARN }.map { it.message }
        assertEquals(List(3) { "Tenjin consent failed: IllegalStateException" }, failures)
    }

    @Test
    fun funnelEvent_sentByNameOnly_afterConsentCheck() = runUnconfined {
        val tracker = started()
        val before = sink.calls.size
        tracker.log(AdFunnelEvent.Eligible(AdFunnelKind.INTERS))

        assertEquals(listOf("applyConsent", "event"), sink.calls.drop(before))
        assertEquals(listOf("inters_ad_eligible"), sink.events)
        assertTrue(sink.payloads.toString(), sink.payloads.isEmpty())
    }

    @Test
    fun everyFunnelEvent_sentWithItsName() = runUnconfined {
        val tracker = started()
        (funnelSteps() + AdFunnelEvent.InterDisplayedTimes(1) + AdFunnelEvent.InterDisplayedTimes(20))
            .forEach { tracker.log(it) }

        assertEquals(
            listOf(
                "inters_ad_eligible",
                "inters_api_called",
                "inters_displayed",
                "rewarded_ad_eligible",
                "rewarded_api_called",
                "rewarded_ad_displayed",
                "native_ad_eligible",
                "native_api_called",
                "native_ad_displayed",
                "inters_displayed_1_times",
                "inters_displayed_20_times",
            ),
            sink.events,
        )
        assertEquals(11, sink.calls.count { it == "event" })
        sink.calls.forEachIndexed { index, call ->
            if (call == "event") assertEquals(sink.calls.toString(), "applyConsent", sink.calls[index - 1])
        }
    }

    @Test
    fun interDisplayedTimesOutOfRange_notSent() = runUnconfined {
        val tracker = started()
        val before = sink.calls.toList()
        tracker.log(AdFunnelEvent.InterDisplayedTimes(0))
        tracker.log(AdFunnelEvent.InterDisplayedTimes(21))
        assertEquals(before, sink.calls)

        // Control: 20 is sent by the same tracker.
        tracker.log(AdFunnelEvent.InterDisplayedTimes(20))
        assertEquals(before + listOf("applyConsent", "event"), sink.calls)
        assertEquals(listOf("inters_displayed_20_times"), sink.events)
    }

    @Test
    fun funnelSwitchOff_notSent_thenOnAgain() = runUnconfined {
        val tracker = started()
        settings.set(TenjinSettings.IS_LOG_TENJIN_INTERS_AD_ELIGIBLE, false)
        val before = sink.calls.toList()
        tracker.log(AdFunnelEvent.Eligible(AdFunnelKind.INTERS))
        assertEquals("switched off: no consent read, no event", before, sink.calls)

        // Control: one switch per event, so another event is still sent.
        tracker.log(AdFunnelEvent.Eligible(AdFunnelKind.NATIVE))
        assertEquals(listOf("native_ad_eligible"), sink.events)

        settings.set(TenjinSettings.IS_LOG_TENJIN_INTERS_AD_ELIGIBLE, true)
        tracker.log(AdFunnelEvent.Eligible(AdFunnelKind.INTERS))
        assertEquals(listOf("native_ad_eligible", "inters_ad_eligible"), sink.events)

        settings.set(TenjinSettings.IS_LOG_TENJIN_INTERS_DISPLAYED_N_TIMES, false)
        tracker.log(AdFunnelEvent.InterDisplayedTimes(3))
        assertEquals(listOf("native_ad_eligible", "inters_ad_eligible"), sink.events)

        settings.set(TenjinSettings.IS_LOG_TENJIN_INTERS_DISPLAYED_N_TIMES, true)
        tracker.log(AdFunnelEvent.InterDisplayedTimes(3))
        assertEquals(listOf("native_ad_eligible", "inters_ad_eligible", "inters_displayed_3_times"), sink.events)
    }

    @Test
    fun revenueSwitchOff_funnelStillSent() = runUnconfined {
        val tracker = started()
        settings.set(TenjinSettings.IS_LOG_TENJIN_AD_REVENUE, false)
        tracker.log(revenue())
        tracker.log(AdFunnelEvent.Displayed(AdFunnelKind.INTERS, "INTER", UNIT, SOURCE))

        assertTrue(sink.payloads.toString(), sink.payloads.isEmpty())
        assertEquals(listOf("inters_displayed"), sink.events)
    }

    @Test
    fun noKey_funnelNeverTouchesSdk() = runUnconfined {
        val tracker = tracker(backgroundScope, sdkKey = "")
        tracker.prepare()
        tracker.start()
        tracker.onConsentKnown()
        allFunnelEvents().forEach { tracker.log(it) }
        assertTrue(sink.calls.toString(), sink.calls.isEmpty())

        // Control: the same calls with a key reach the SDK.
        val withKey = FakeTenjinSink()
        val other = tracker(backgroundScope, sink = withKey)
        other.prepare()
        other.start()
        other.onConsentKnown()
        allFunnelEvents().forEach { other.log(it) }
        assertTrue(withKey.events.toString(), "inters_ad_eligible" in withKey.events)
    }

    @Test
    fun funnelBeforeStart_dropped() = runUnconfined {
        val tracker = tracker(backgroundScope)
        tracker.prepare()
        tracker.onConsentKnown()
        tracker.log(AdFunnelEvent.Eligible(AdFunnelKind.INTERS))
        assertEquals(listOf("init"), sink.calls)
        assertTrue(logger.entries.any { it.message == "Tenjin inters_ad_eligible before start" })

        // Control: started, the next event is sent, the dropped one is not sent later.
        tracker.start()
        tracker.log(AdFunnelEvent.Eligible(AdFunnelKind.NATIVE))
        assertEquals(listOf("native_ad_eligible"), sink.events)
    }

    @Test
    fun funnelBeforeConsentKnown_dropped() = runUnconfined {
        val tracker = tracker(backgroundScope)
        tracker.prepare()
        tracker.start()
        tracker.log(AdFunnelEvent.Eligible(AdFunnelKind.INTERS))
        assertEquals(listOf("init"), sink.calls)
        assertTrue(logger.entries.any { it.message == "Tenjin inters_ad_eligible before consent known" })

        // Control: the consent known, the next event is sent, the dropped one is not sent later.
        tracker.onConsentKnown()
        tracker.log(AdFunnelEvent.Eligible(AdFunnelKind.NATIVE))
        assertEquals(listOf("native_ad_eligible"), sink.events)
    }

    @Test
    fun funnelConsentRefused_notSent() = runUnconfined {
        sink.consent = false
        val tracker = started()
        val before = sink.calls.size
        val events = allFunnelEvents()
        events.forEach { tracker.log(it) }

        assertEquals(List(events.size) { "applyConsent" }, sink.calls.drop(before))
        assertTrue(sink.events.toString(), sink.events.isEmpty())
        assertTrue(logger.entries.any { it.message == "Tenjin inters_ad_eligible opted out" })
    }

    @Test
    fun funnelConsentWithdrawnThenGranted_followsEachEvent() = runUnconfined {
        val tracker = started()
        val consentChecks = { sink.calls.count { it == "applyConsent" } }
        val start = consentChecks()

        tracker.log(AdFunnelEvent.Eligible(AdFunnelKind.INTERS))
        assertEquals(listOf("inters_ad_eligible"), sink.events)
        assertEquals(start + 1, consentChecks())

        sink.consent = false
        tracker.log(AdFunnelEvent.Eligible(AdFunnelKind.NATIVE))
        assertEquals(listOf("inters_ad_eligible"), sink.events)
        assertEquals(start + 2, consentChecks())

        sink.consent = true
        tracker.log(AdFunnelEvent.Eligible(AdFunnelKind.REWARDED))
        assertEquals(listOf("inters_ad_eligible", "rewarded_ad_eligible"), sink.events)
        assertEquals(start + 3, consentChecks())
    }

    @Test
    fun funnelSdkThrows_isSwallowedAndLogged() = runUnconfined {
        val eventFails = FakeTenjinSink().apply { failOn = "event" }
        started(sink = eventFails).log(AdFunnelEvent.Eligible(AdFunnelKind.INTERS))
        val consentFails = FakeTenjinSink()
        val connected = started(sink = consentFails)
        consentFails.failOn = "applyConsent"
        connected.log(AdFunnelEvent.ApiCalled(AdFunnelKind.INTERS, "INTER", UNIT, SOURCE))

        assertEquals("event", eventFails.calls.last())
        assertEquals(listOf("init", "applyConsent", "connect", "applyConsent"), consentFails.calls)
        val failures = logger.entries.filter { it.level == LogLevel.WARN }.map { it.message }
        assertEquals(
            listOf(
                "Tenjin inters_ad_eligible failed: IllegalStateException",
                "Tenjin consent failed: IllegalStateException",
            ),
            failures,
        )
        // Never an ad unit, a format or a source in a log line.
        assertTrue(logger.entries.none { UNIT in it.message || "INTER" in it.message || SOURCE in it.message })
    }

    @Test
    fun configToString_hidesKey() {
        val text = TenjinConfig("SECRETKEY").toString()
        assertFalse(text, "SECRETKEY" in text)
        assertEquals("TenjinConfig(sdkKey=set)", text)
        assertEquals("TenjinConfig(sdkKey=none)", TenjinConfig("").toString())
    }

    private fun revenue(value: Double = 0.0123, currency: String = "USD") = AdMetricEvent.ImpressionRevenue(
        adFormat = "INTER",
        adSource = "admob",
        monetizationNetwork = "",
        adUnitName = "ca-app-pub-unit",
        currency = currency,
        value = value,
        adPlacement = "p",
        adapterClassName = "x.Adapter",
    )

    /** Eligible, loaded, displayed, for each of interstitial, rewarded, native, in that order. */
    private fun funnelSteps(): List<AdFunnelEvent> = AdFunnelKind.entries.flatMap { kind ->
        listOf(
            AdFunnelEvent.Eligible(kind),
            AdFunnelEvent.ApiCalled(kind, "INTER", UNIT, SOURCE),
            AdFunnelEvent.Displayed(kind, "INTER", UNIT, SOURCE),
        )
    }

    private fun allFunnelEvents(): List<AdFunnelEvent> = funnelSteps() + AdFunnelEvent.InterDisplayedTimes(1)

    private companion object {
        const val SDK_KEY = "KEY"
        const val UNIT = "ca-app-pub-test/1"
        const val SOURCE = "com.google.ads.mediation.Adapter"
    }
}
