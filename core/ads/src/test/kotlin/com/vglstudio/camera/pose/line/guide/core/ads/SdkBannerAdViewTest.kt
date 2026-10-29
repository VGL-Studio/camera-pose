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

    private companion object {
        /** The defaults of T-019, as read when Remote Config has no value. */
        val DEFAULT_FLAGS: AdsGlobalFlags = runBlocking { DefaultAdsFlags(FakeRemoteSettings()).global.first() }
    }
}
