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
 * AppsFlyer behind the interfaces of :core:analytics (ADR-0027; docs/ops/ads-business-rules.md, section 8.3; plan
 * T-033): no key sends nothing, `init` before `start`, one switch per event, errors swallowed. The SDK is a fake.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppsFlyerTrackerTest {
    private val sink = FakeAppsFlyerSink()
    private val settings = FakeRemoteSettings()
    private val logger = RecordingAppLogger()

    private fun tracker(scope: CoroutineScope, devKey: String = DEV_KEY) = AppsFlyerTracker(
        config = AppsFlyerConfig(devKey),
        sink = sink,
        switches = AppsFlyerSwitches(settings, scope),
        country = FixedCountry("VN"),
        logger = logger,
    )

    private fun TestScope.started(devKey: String = DEV_KEY) = tracker(backgroundScope, devKey).apply {
        prepare()
        start()
    }

    private fun runUnconfined(body: suspend TestScope.() -> Unit) = runTest(UnconfinedTestDispatcher()) { body() }

    @Test
    fun everyImpression_sendsAfAdRevenue() = runUnconfined {
        val tracker = started()
        FORMAT_CODES.forEach { tracker.log(revenue(adFormat = it)) }

        assertEquals(10, sink.events.size)
        assertTrue(sink.events.all { it.name == "af_ad_revenue" })
        assertEquals(FORMAT_CODES, sink.events.map { it.params!!["ad_type"] })
        assertEquals("VN", sink.events.first().params!!["country"])
    }

    @Test
    fun revenue_sendsOneEventOnly() = runUnconfined {
        val tracker = started()
        tracker.log(revenue())

        assertEquals(listOf("init", "start", "logEvent"), sink.calls)
        assertEquals(listOf("af_ad_revenue"), sink.events.map { it.name })
    }

    @Test
    fun zeroValueImpression_isSent() = runUnconfined {
        val tracker = started()
        tracker.log(revenue(adFormat = "NATIVE_SMALL", value = 0.0, currency = "USD"))

        val params = sink.events.single().params!!
        assertEquals(0.0, params["af_revenue"] as Double, 0.0)
        assertEquals("USD", params["af_currency"])
    }

    @Test
    fun lifecycleEvents_areNotSentToAppsFlyer() = runUnconfined {
        val tracker = started()
        val others = listOf(
            AdMetricEvent.LoadSuccess("INTER", "preload", 1, 10),
            AdMetricEvent.LoadFail("INTER", "preload", 1, 10, "3", "no_fill"),
            AdMetricEvent.Opportunity("INTER", "p", "shown", "ready"),
            AdMetricEvent.ShowFail("INTER", "p", "1", "ad_reused"),
            AdMetricEvent.Clicked("INTER", "p"),
            AdMetricEvent.RewardEarned("REWARDED", "p"),
            AdMetricEvent.RewardDialog("REWARDED", "p", "show"),
        )
        // One event of every subclass but the revenue, so a new subclass is tried too.
        assertEquals(
            AdMetricEvent::class.sealedSubclasses.toSet() - AdMetricEvent.ImpressionRevenue::class,
            others.map { it::class }.toSet(),
        )

        others.forEach { tracker.log(it) }
        assertTrue(sink.events.isEmpty())

        // Control: a revenue event is sent by the same tracker.
        tracker.log(revenue())
        assertEquals(1, sink.events.size)
    }

    @Test
    fun noDevKey_neverTouchesSdk() = runUnconfined {
        val tracker = tracker(backgroundScope, devKey = "")
        tracker.prepare()
        tracker.prepare()
        tracker.start()
        tracker.log(revenue())
        tracker.log(AdFunnelEvent.Eligible(AdFunnelKind.INTERS))
        tracker.log(AdFunnelEvent.InterDisplayedTimes(1))

        assertTrue(sink.calls.isEmpty())
        val off = logger.entries.filter { it.level == LogLevel.INFO && "no dev key" in it.message }
        assertEquals(1, off.size)
        assertEquals("Analytics", off.single().tag)
        // The events dropped are logged by name, at debug level.
        assertTrue(
            logger.entries.any {
                it.level == LogLevel.DEBUG && it.message == "AppsFlyer off: af_inters_ad_eligible not sent"
            },
        )
    }

    @Test
    fun withDevKey_initStartAndSend() = runUnconfined {
        val tracker = tracker(backgroundScope)
        tracker.prepare()
        tracker.start()
        tracker.log(revenue())
        tracker.log(AdFunnelEvent.Eligible(AdFunnelKind.INTERS))
        tracker.log(AdFunnelEvent.InterDisplayedTimes(1))

        assertEquals(listOf("init", "start", "logEvent", "logEvent", "logEvent"), sink.calls)
        assertEquals(listOf(DEV_KEY), sink.keys)
        assertEquals(
            listOf("af_ad_revenue", "af_inters_ad_eligible", "af_inters_displayed_1_times"),
            sink.events.map { it.name },
        )
    }

    @Test
    fun switchOff_stopsOnlyThatEvent_fromNextEvent() = runUnconfined {
        val tracker = started()
        val displayed = AdFunnelEvent.Displayed(AdFunnelKind.INTERS, "INTER", "unit", "x.Adapter")
        val apiCalled = AdFunnelEvent.ApiCalled(AdFunnelKind.INTERS, "INTER", "unit", "x.Adapter")
        tracker.log(displayed)
        assertEquals(listOf("af_inters_displayed"), sink.events.map { it.name })

        settings.set(AppsFlyerSettings.IS_LOG_AF_INTERS_DISPLAYED, false)
        tracker.log(displayed)
        tracker.log(apiCalled)
        assertEquals(listOf("af_inters_displayed", "af_inters_api_called"), sink.events.map { it.name })
        assertTrue(logger.entries.any { it.message == "AppsFlyer af_inters_displayed switched off" })

        settings.set(AppsFlyerSettings.IS_LOG_AF_INTERS_DISPLAYED, true)
        tracker.log(displayed)
        assertEquals(
            listOf("af_inters_displayed", "af_inters_api_called", "af_inters_displayed"),
            sink.events.map { it.name },
        )
    }

    @Test
    fun revenueSwitchOff_stopsRevenue_funnelStillSent() = runUnconfined {
        val tracker = started()
        settings.set(AppsFlyerSettings.IS_LOG_AF_AD_REVENUE, false)

        tracker.log(revenue())
        tracker.log(AdFunnelEvent.Eligible(AdFunnelKind.REWARDED))
        assertEquals(listOf("af_rewarded_ad_eligible"), sink.events.map { it.name })

        // Control: on again, the revenue is sent from the next event.
        settings.set(AppsFlyerSettings.IS_LOG_AF_AD_REVENUE, true)
        tracker.log(revenue())
        assertEquals(listOf("af_rewarded_ad_eligible", "af_ad_revenue"), sink.events.map { it.name })
    }

    @Test
    fun devKey_neverInLogs() = runUnconfined {
        val tracker = started()
        tracker.log(revenue())
        tracker.log(AdFunnelEvent.ApiCalled(AdFunnelKind.NATIVE, "NATIVE_SMALL", "unit", "x.Adapter"))
        sink.failure = IllegalStateException("bad key $DEV_KEY")
        tracker.log(revenue())
        val other = tracker(backgroundScope)
        other.prepare()
        other.start()

        // Control: the run logged something, the failure included.
        assertTrue(logger.entries.size >= 3)
        assertTrue(logger.entries.any { it.level == LogLevel.WARN })
        logger.entries.forEach {
            assertFalse(it.message, DEV_KEY in it.message)
            assertFalse(it.message, it.error?.message?.contains(DEV_KEY) == true)
        }
        assertFalse(DEV_KEY in AppsFlyerConfig(DEV_KEY).toString())
        assertTrue(AppsFlyerConfig(DEV_KEY).enabled)
        assertFalse(AppsFlyerConfig(" ").enabled)
    }

    @Test
    fun prepare_initsOnly_noStart() = runUnconfined {
        val tracker = tracker(backgroundScope)
        tracker.prepare()
        assertEquals(listOf("init"), sink.calls)
        tracker.prepare()
        assertEquals(listOf("init"), sink.calls)
        assertTrue(logger.entries.any { it.message == "AppsFlyer ready" })
    }

    @Test
    fun start_twice_startsOnce() = runUnconfined {
        val tracker = tracker(backgroundScope)
        // Before prepare, start does nothing.
        tracker.start()
        assertTrue(sink.calls.isEmpty())
        tracker.prepare()
        tracker.start()
        tracker.start()
        assertEquals(listOf("init", "start"), sink.calls)
        assertEquals(1, logger.entries.count { it.message == "AppsFlyer started" })
    }

    @Test
    fun eventBeforeStart_isDropped() = runUnconfined {
        val tracker = tracker(backgroundScope)
        tracker.prepare()
        tracker.log(AdFunnelEvent.Eligible(AdFunnelKind.REWARDED))

        assertEquals(listOf("init"), sink.calls)
        assertTrue(
            logger.entries.any {
                it.level == LogLevel.DEBUG && it.message == "AppsFlyer af_rewarded_ad_eligible before start"
            },
        )
        // Control: once started, the same event is sent.
        tracker.start()
        tracker.log(AdFunnelEvent.Eligible(AdFunnelKind.REWARDED))
        assertEquals(listOf("af_rewarded_ad_eligible"), sink.events.map { it.name })
    }

    @Test
    fun sinkThrows_isSwallowed_logsClassName() = runUnconfined {
        val tracker = started()
        sink.failure = IllegalStateException("message of the sdk")

        tracker.log(revenue())
        tracker.log(AdFunnelEvent.Eligible(AdFunnelKind.NATIVE))

        val warnings = logger.entries.filter { it.level == LogLevel.WARN }
        assertEquals(2, warnings.size)
        warnings.forEach {
            assertTrue(it.message, "IllegalStateException" in it.message)
            assertFalse(it.message, "message of the sdk" in it.message)
        }
        // Control: the calls reached the SDK.
        assertEquals(2, sink.calls.count { it == "logEvent" })
    }

    private fun revenue(adFormat: String = "INTER", value: Double = 0.0123, currency: String = "USD") =
        AdMetricEvent.ImpressionRevenue(
            adFormat = adFormat,
            adSource = "AppLovin",
            monetizationNetwork = "AppLovin",
            adUnitName = "unit",
            currency = currency,
            value = value,
            adPlacement = "home",
            adapterClassName = "x.Adapter",
        )

    private companion object {
        const val DEV_KEY = "dev-key-for-test"

        /** The format codes of T-04. */
        val FORMAT_CODES = listOf(
            "INTER",
            "REWARDED",
            "APP_OPEN",
            "BANNER",
            "NATIVE_FULL",
            "NATIVE_COLLAP",
            "NATIVE_SMALL",
            "NATIVE_ONBOARDING",
            "NATIVE_CONTENT",
            "NATIVE_MEDIUM",
        )
    }
}
