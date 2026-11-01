package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.vglstudio.camera.pose.line.guide.core.analytics.AdMetricEvent
import com.vglstudio.camera.pose.line.guide.core.analytics.RecordingAdFunnel
import com.vglstudio.camera.pose.line.guide.core.analytics.RecordingAdMetrics
import com.vglstudio.camera.pose.line.guide.core.logging.LogEntry
import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

/** The wrapper of a real [AdView]: requests (collapsible extras), results and metrics (B-03, plan T-023). */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SdkBannerAdViewTest {
    private val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
    private val adView = AdView(activity)
    private val requests = mutableListOf<AdRequest>()
    private val metrics = RecordingAdMetrics()
    private var nowMillis = 0L

    private val telemetry = AdTelemetry(
        metrics = metrics,
        funnel = RecordingAdFunnel(),
        lifecycleSwitches = { DEFAULT_FLAGS.copy(logSlotEvents = true) },
        serving = { true },
        offline = { false },
        counter = LoadAttemptCounter(),
        logger = RecordingAppLogger(),
    )

    // No banner placement is declared yet (question 1 of the plan): any placement names the slot of the metrics.
    private fun banner(refresh: BannerRefresh) = SdkBannerAdView(
        adView = adView,
        refresh = refresh,
        placement = AdPlacement.SAMPLE_NATIVE_SMALL,
        unitId = "unit",
        telemetry = telemetry,
        now = { nowMillis },
        load = { requests += it },
    )

    @After
    fun tearDown() {
        adView.destroy()
    }

    @Test
    fun collapsibleSends_haveBottomAnchorAndNewRequestIdEachTime() {
        val view = banner(BannerRefresh.MANUAL)

        view.send()
        view.send()

        assertEquals(2, requests.size)
        val extras = requests.map { requireNotNull(it.getNetworkExtrasBundle(AdMobAdapter::class.java)) }
        extras.forEach { assertEquals("bottom", it.getString("collapsible")) }
        val ids = extras.map { it.getString("collapsible_request_id").orEmpty() }
        ids.forEach { assertTrue("request id is empty", it.isNotBlank()) }
        assertNotEquals(ids[0], ids[1])
    }

    @Test
    fun fixedBannerRequest_hasNoCollapsibleExtras() {
        // Control (L-055): the same builder gives a collapsible request the extras, so their absence below counts.
        val collapsibleRequest = bannerAdRequest(BannerRefresh.MANUAL, requestId = { "id" })
        val collapsible = collapsibleRequest.getNetworkExtrasBundle(AdMobAdapter::class.java)
        assertEquals("bottom", collapsible?.getString("collapsible"))

        val view = banner(BannerRefresh.BY_AD_UNIT)
        view.send()

        val extras = requests.single().getNetworkExtrasBundle(AdMobAdapter::class.java)
        assertTrue("$extras", extras == null || !extras.containsKey("collapsible"))
        assertFalse(extras?.containsKey("collapsible_request_id") == true)
    }

    @Test
    fun listenerResults_reachResultsAndMetrics() = runTest {
        val view = banner(BannerRefresh.BY_AD_UNIT)
        val results = mutableListOf<BannerLoadResult>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { view.results.collect { results += it } }

        nowMillis = 1_000
        view.send()
        assertEquals(1, requests.size)
        nowMillis = 1_400
        adView.adListener.onAdFailedToLoad(LoadAdError(3, "No fill.", "com.google.android.gms.ads", null, null))
        assertEquals(listOf<BannerLoadResult>(BannerLoadResult.Failed(3)), results)
        assertTrue(metrics.events.any { it is AdMetricEvent.LoadFail })

        nowMillis = 3_400
        view.send()
        nowMillis = 3_600
        adView.adListener.onAdLoaded()
        assertEquals(listOf(BannerLoadResult.Failed(3), BannerLoadResult.Loaded), results)
        val success = metrics.events.filterIsInstance<AdMetricEvent.LoadSuccess>().single()
        assertEquals(2, success.loadAttempt)
        assertEquals(2, requests.size)
    }

    @Test
    fun barBanner_reportsBannerFormatAndBarPlacement() {
        // The banner layer of the bottom bar of a native small placement (plan T-030, D-05): metrics name the format
        // of the layer and the placement of the bar.
        val view = SdkBannerAdView(
            adView = adView,
            refresh = BannerRefresh.BY_AD_UNIT,
            placement = AdPlacement.SAMPLE_NATIVE_SMALL,
            unitId = "unit",
            telemetry = telemetry,
            now = { nowMillis },
            load = { requests += it },
            format = AdFormat.BANNER,
        )
        view.send()
        adView.adListener.onAdLoaded()
        adView.adListener.onAdClicked()

        val success = metrics.events.filterIsInstance<AdMetricEvent.LoadSuccess>().single()
        assertEquals(AdFormat.BANNER.metricCode(), success.adFormat)
        assertEquals("sample_native_small", success.adPlacement)
        val click = metrics.events.filterIsInstance<AdMetricEvent.Clicked>().single()
        assertEquals(AdFormat.BANNER.metricCode(), click.adFormat)
        assertEquals("sample_native_small", click.adPlacement)
    }

    @Test
    fun mrecBanner_reportsBannerMrecFormatAndPlacement() {
        // Plan T-062, KB-29: the MREC placement reports its own format code and its placement.
        val view = SdkBannerAdView(
            adView = adView,
            refresh = bannerRefreshOf(AdFormat.BANNER_MREC),
            placement = AdPlacement.GALLERY_BANNER_MREC,
            unitId = "unit",
            telemetry = telemetry,
            now = { nowMillis },
            load = { requests += it },
            format = AdFormat.BANNER_MREC,
        )
        view.send()
        adView.adListener.onAdLoaded()
        adView.adListener.onAdClicked()

        val success = metrics.events.filterIsInstance<AdMetricEvent.LoadSuccess>().single()
        assertEquals("BANNER_MREC", success.adFormat)
        assertEquals("gallery_banner_mrec", success.adPlacement)
        val click = metrics.events.filterIsInstance<AdMetricEvent.Clicked>().single()
        assertEquals("BANNER_MREC", click.adFormat)
        assertEquals("gallery_banner_mrec", click.adPlacement)
        // Refreshed by its ad unit: the request has no collapsible extras.
        val extras = requests.single().getNetworkExtrasBundle(AdMobAdapter::class.java)
        assertTrue("$extras", extras == null || !extras.containsKey("collapsible"))
    }

    @Test
    fun networkLog_onLoadResults_logsAtStaging() {
        // Plan T-058, KB-28: every result of the banner, sent by the app or refreshed by the SDK, logs its networks.
        // The AdView of Robolectric has no response info, so each result is one "none tried" line.
        val logger = RecordingAppLogger()
        SdkBannerAdView(
            adView = adView,
            refresh = BannerRefresh.BY_AD_UNIT,
            placement = AdPlacement.SAMPLE_NATIVE_SMALL,
            unitId = "unit",
            telemetry = telemetry,
            now = { nowMillis },
            load = { requests += it },
            networkLog = AdNetworkLog(true, logger),
        )

        adView.adListener.onAdFailedToLoad(LoadAdError(3, "No fill.", "com.google.android.gms.ads", null, null))
        adView.adListener.onAdLoaded()

        val lines = logger.entries.filter { it.message.startsWith("Network ") }
        assertEquals(
            listOf(
                LogEntry(LogLevel.INFO, "Ads", "Network sample_native_small: none tried"),
                LogEntry(LogLevel.INFO, "Ads", "Network sample_native_small: none tried"),
            ),
            lines,
        )
    }

    @Test
    fun networkLog_defaultOff_logsNothing() {
        val logger = RecordingAppLogger()
        val quietTelemetry = AdTelemetry(
            metrics = metrics,
            funnel = RecordingAdFunnel(),
            lifecycleSwitches = { DEFAULT_FLAGS.copy(logSlotEvents = true) },
            serving = { true },
            offline = { false },
            counter = LoadAttemptCounter(),
            logger = logger,
        )
        SdkBannerAdView(
            adView = adView,
            refresh = BannerRefresh.BY_AD_UNIT,
            placement = AdPlacement.SAMPLE_NATIVE_SMALL,
            unitId = "unit",
            telemetry = quietTelemetry,
            now = { nowMillis },
            load = { requests += it },
        )

        adView.adListener.onAdFailedToLoad(LoadAdError(3, "No fill.", "com.google.android.gms.ads", null, null))
        adView.adListener.onAdLoaded()

        // Control: both results reached the banner (its metrics), so the absence of a network line counts.
        assertTrue(metrics.events.any { it is AdMetricEvent.LoadFail })
        assertTrue(metrics.events.any { it is AdMetricEvent.LoadSuccess })
        assertTrue(logger.entries.toString(), logger.entries.none { it.message.startsWith("Network ") })

        // Control (L-055): the same callbacks with the staging log on write the network lines to that logger.
        SdkBannerAdView(
            adView = adView,
            refresh = BannerRefresh.BY_AD_UNIT,
            placement = AdPlacement.SAMPLE_NATIVE_SMALL,
            unitId = "unit",
            telemetry = quietTelemetry,
            now = { nowMillis },
            load = { requests += it },
            networkLog = AdNetworkLog(true, logger),
        )
        adView.adListener.onAdLoaded()
        assertEquals(1, logger.entries.count { it.message.startsWith("Network ") })
    }

    private companion object {
        /** The defaults of T-019, as read when Remote Config has no value. */
        val DEFAULT_FLAGS: AdsGlobalFlags = runBlocking { DefaultAdsFlags(FakeRemoteSettings()).global.first() }
    }
}
