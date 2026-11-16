package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Activity
import android.app.Application
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.analytics.AdFunnelEvent
import com.vglstudio.camera.pose.line.guide.core.analytics.AdFunnelKind
import com.vglstudio.camera.pose.line.guide.core.analytics.AdMetricEvent
import com.vglstudio.camera.pose.line.guide.core.analytics.AdMetricPlatform
import com.vglstudio.camera.pose.line.guide.core.analytics.RecordingAdFunnel
import com.vglstudio.camera.pose.line.guide.core.analytics.RecordingAdMetrics
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import java.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

/**
 * The interstitial of AppLovin MAX in the real [AdsController] (plan T-069, KB-31): the platforms of a turn follow
 * `ads_mediation_order` (`open_app_inter` AdMob only), both platforms in the order preload without blocking each other,
 * and a MAX turn goes through the same gate, showing flag, mark, combo and recovery as an AdMob one.
 *
 * AdMob loads go through [AdsController.interstitialRequestForTest] (no SDK), answered with [admobAnswer]; the MAX
 * interstitial is the [FakeMaxInterstitialSdkAd] that [FakeMaxPlatform] builds.
 */
@RunWith(AndroidJUnit4::class)
class AdsControllerMaxInterstitialTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val settings = FakeRemoteSettings().apply {
        set(AdsSettings.IS_LOG_AD_FULLSCREEN_EVENTS, true)
        set(AdsSettings.placementEnabled(AdPlacement.GALLERY_INTERSTITIAL), true)
        // The app-wide placements are switched by their global keys (StartupAdChoice): resume_inter, open_app_inter.
        set(AdsSettings.SHOW_RESUM_ADS, true)
        set(AdsSettings.RESUME_AD_TYPE, "inter")
        set(AdsSettings.SHOW_OPEN_ADS, true)
        set(AdsSettings.SHOW_OPEN_ADS_FIRST_OPEN, true)
        set(AdsSettings.OPEN_AD_TYPE, "inter")
    }
    private val metrics = RecordingAdMetrics()
    private val funnel = RecordingAdFunnel()
    private val logger = RecordingAppLogger()
    private val platform = FakeMaxPlatform()
    private val requested = mutableListOf<AdPlacement>()
    private var admobAnswer: () -> FakeInterstitialAd? = { null }
    private var finishes = 0
    private val onFinished: () -> Unit = { finishes++ }
    private lateinit var controller: AdsController
    private lateinit var activity: Activity

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun idleFor(millis: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))

    /** The controller with [order], started, consent open, AdMob ready, MAX ready (when the order has MAX). */
    private fun start(
        order: String,
        maxConfig: MaxConfig = MAX_CONFIG,
        maxReady: Boolean = true,
        plan: Boolean = false,
    ) {
        settings.set(AdsSettings.ADS_MEDIATION_ORDER, order)
        controller = testAdsController(
            context = application,
            flags = DefaultAdsFlags(settings),
            metrics = metrics,
            funnel = funnel,
            logger = logger,
            maxConfig = maxConfig,
            maxPlatform = platform,
        )
        controller.interstitialRequestForTest = {
            requested += it
            admobAnswer()
        }
        if (plan) controller.setPreloadPlan(AdPreloadPlan(listOf(setOf(AdFormat.INTERSTITIAL))))
        controller.start(application, AppActivityFilter { true })
        idle()
        controller.openConsentGateForTest()
        controller.admobReadyForTest()
        idle()
        if (maxReady) {
            platform.finishInitialization()
            idle()
        }
        activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        idle()
    }

    private val maxAd: FakeMaxInterstitialSdkAd get() = platform.interstitials.single()

    /** Asks the slots of [placement] to load, then the MAX interstitial answers loaded with [info]. */
    private fun loadMax(placement: AdPlacement = AdPlacement.GALLERY_INTERSTITIAL, info: MaxAdInfo = MaxAdInfo.EMPTY) {
        controller.loadFullScreenAdIfNeeded(placement)
        idle()
        maxAd.emitLoaded(info)
        idle()
    }

    private fun show(placement: AdPlacement = AdPlacement.GALLERY_INTERSTITIAL, on: Activity = activity): Boolean? {
        var result: Boolean? = null
        CoroutineScope(Dispatchers.Main).launch { result = controller.showInterstitial(on, placement, onFinished) }
        idle()
        return result
    }

    private fun opportunities(placement: AdPlacement = AdPlacement.GALLERY_INTERSTITIAL) =
        metrics.events.filterIsInstance<AdMetricEvent.Opportunity>().filter { it.adPlacement == placement.key }

    private fun putAdmob(placement: AdPlacement = AdPlacement.GALLERY_INTERSTITIAL): FakeInterstitialAd =
        FakeInterstitialAd().also { controller.putLoadedInterstitialForTest(placement, it) }

    // KB-31, criterion 1

    @Test
    fun maxFirst_noMaxAd_admobReady_showsAdmob_oneOpportunity() {
        start("max_admob")
        val admob = putAdmob()
        assertEquals(true, show())
        assertEquals(1, admob.shows)
        assertEquals(0, platform.interstitials.sumOf { it.showCount })
        assertEquals(
            listOf(AdMetricEvent.Opportunity("INTER", "gallery_interstitial", "shown", "ready")),
            opportunities(),
        )
    }

    @Test
    fun admobFirst_noAdmobAd_maxReady_showsMax_oneOpportunity() {
        start("admob_max")
        loadMax()
        assertEquals(true, show())
        assertEquals(1, maxAd.showCount)
        assertEquals(
            listOf(AdMetricEvent.Opportunity("INTER", "gallery_interstitial", "shown", "ready")),
            opportunities(),
        )
    }

    @Test
    fun bothReady_firstPlatformShown() {
        start("max_admob")
        val admob = putAdmob()
        loadMax()
        assertEquals(true, show())
        assertEquals(1, maxAd.showCount)
        assertEquals(0, admob.shows)
    }

    // ADR-0045, section 2.3

    @Test
    fun maxAdmob_preload_bothPlatformsLoad() {
        admobAnswer = { FakeInterstitialAd() }
        start("max_admob", plan = true)
        controller.requestPreloadForTest()
        idle()
        assertTrue(requested.toString(), AdPlacement.GALLERY_INTERSTITIAL in requested)
        assertEquals(1, maxAd.loadCount)
    }

    @Test
    fun maxReady_admobFailed_retryStillLoadsAdmob() {
        start("admob_max")
        loadMax()
        assertEquals(1, requested.count { it == AdPlacement.GALLERY_INTERSTITIAL })
        assertTrue(controller.isFullScreenAdReady(AdPlacement.GALLERY_INTERSTITIAL))
        // The AdMob failure retries (F-05) although the turn already has the MAX ad.
        idleFor(2_100)
        assertEquals(2, requested.count { it == AdPlacement.GALLERY_INTERSTITIAL })
    }

    @Test
    fun admobShown_maxWaitingRetry_admobReloadsAfterTwoSeconds() {
        var lastAdmob: FakeInterstitialAd? = null
        admobAnswer = { FakeInterstitialAd().also { lastAdmob = it } }
        start("admob_max")
        controller.loadFullScreenAdIfNeeded(AdPlacement.GALLERY_INTERSTITIAL)
        idle()
        maxAd.emitLoadFailed(204)
        assertEquals(true, show())
        assertEquals(1, lastAdmob!!.shows)
        val sent = requested.count { it == AdPlacement.GALLERY_INTERSTITIAL }
        lastAdmob!!.callback!!.onAdDismissedFullScreenContent()
        idleFor(2_100)
        assertEquals(sent + 1, requested.count { it == AdPlacement.GALLERY_INTERSTITIAL })
    }

    // KB-31, criterion 2

    @Test
    fun orderAdmob_maxSlotNeverLoads() {
        admobAnswer = { FakeInterstitialAd() }
        start("admob", plan = true)
        controller.requestPreloadForTest()
        idle()
        controller.loadFullScreenAdIfNeeded(AdPlacement.GALLERY_INTERSTITIAL)
        idle()
        assertTrue(platform.interstitials.isEmpty())
        // Control: AdMob was asked.
        assertTrue(AdPlacement.GALLERY_INTERSTITIAL in requested)
    }

    @Test
    fun orderMax_noAdmobRequest_onlyMaxLoads() {
        start("max", plan = true)
        controller.requestPreloadForTest()
        idle()
        assertEquals(listOf<AdPlacement>(), requested.filter { it != AdPlacement.OPEN_APP_INTER })
        assertEquals(1, maxAd.loadCount)
    }

    @Test
    fun orderMax_onlyMaxShown() {
        start("max")
        val admob = putAdmob()
        loadMax()
        assertEquals(true, show())
        assertEquals(1, maxAd.showCount)
        assertEquals(0, admob.shows)
    }

    @Test
    fun orderChangedToAdmob_whileMaxHoldsAd_maxNotShown_admobShown() {
        start("max_admob")
        loadMax()
        val admob = putAdmob()
        settings.set(AdsSettings.ADS_MEDIATION_ORDER, "admob")
        idle()
        assertEquals(true, show())
        assertEquals(1, admob.shows)
        assertEquals(0, maxAd.showCount)
    }

    @Test
    fun allInterPlacementsOff_maxRetryDue_noRequest() {
        start("max")
        controller.loadFullScreenAdIfNeeded(AdPlacement.GALLERY_INTERSTITIAL)
        idle()
        maxAd.emitLoadFailed(204)
        settings.set(AdsSettings.placementEnabled(AdPlacement.GALLERY_INTERSTITIAL), false)
        settings.set(AdsSettings.SHOW_RESUM_ADS, false)
        idle()
        idleFor(2_100)
        assertEquals(1, maxAd.loadCount)
    }

    // KB-31, criterion 3 (question 41, changed 2026-11-15)

    @Test
    fun startupInter_maxOrder_loadsAndShowsAdmobOnly() {
        var lastAdmob: FakeInterstitialAd? = null
        admobAnswer = { FakeInterstitialAd().also { lastAdmob = it } }
        start("max")
        // The MAX slot holds an ad, loaded for the in-app placement.
        loadMax(AdPlacement.GALLERY_INTERSTITIAL)
        requested.clear()
        controller.loadFullScreenAdIfNeeded(AdPlacement.OPEN_APP_INTER)
        idle()
        assertEquals(listOf(AdPlacement.OPEN_APP_INTER), requested)
        var interShown: Boolean? = null
        controller.showStartupAd(activity, StartupAdTurn(AdPlacement.OPEN_APP_INTER)) { interShown = it }
        idle()
        assertEquals(1, lastAdmob!!.shows)
        assertEquals(0, maxAd.showCount)
        // The startup turn ends when the AdMob ad closes.
        lastAdmob!!.callback!!.onAdDismissedFullScreenContent()
        idle()
        assertEquals(true, interShown)
    }

    @Test
    fun resumeInter_maxFirst_showsMax() {
        start("max_admob")
        putAdmob(AdPlacement.RESUME_INTER)
        loadMax(AdPlacement.RESUME_INTER)
        assertEquals(true, show(AdPlacement.RESUME_INTER))
        assertEquals(1, maxAd.showCount)
    }

    // KB-31, criterion 5

    @Test
    fun consentClosedAfterLoad_maxAdNotShown() {
        start("max")
        loadMax()
        controller.closeConsentGateForTest()
        idle()
        assertEquals(false, show())
        assertEquals(0, maxAd.showCount)
        assertEquals(1, finishes)
    }

    @Test
    fun gateClosedAtSend_thenReopened_maxLoadsOnce() {
        start("max", plan = true)
        // The plan ran once, so the gate open again preloads (GATE_REOPENED needs a first preload).
        controller.requestPreloadForTest()
        idle()
        maxAd.emitLoadFailed(204)
        controller.closeConsentGateForTest()
        idle()
        idleFor(2_100)
        assertEquals(1, maxAd.loadCount)
        controller.reopenConsentGateForTest()
        idle()
        assertEquals(2, maxAd.loadCount)
    }

    @Test
    fun gateClosed_loadFullScreenAdIfNeeded_noStateChange() {
        start("max_admob")
        controller.loadFullScreenAdIfNeeded(AdPlacement.GALLERY_INTERSTITIAL)
        idle()
        maxAd.emitLoadFailed(204)
        val before = controller.nativeFullScreenLoadStateForTest(AdPlacement.GALLERY_INTERSTITIAL)
        assertTrue(before is FullScreenLoadState.WaitingRetry)
        val sent = requested.size
        controller.closeConsentGateForTest()
        idle()
        controller.loadFullScreenAdIfNeeded(AdPlacement.GALLERY_INTERSTITIAL)
        idle()
        // A placement that never loaded (AdMob idle): no Loading, no blocked round either.
        controller.loadFullScreenAdIfNeeded(AdPlacement.RESUME_INTER)
        idle()
        assertEquals(sent, requested.size)
        assertEquals(
            controller.nativeFullScreenLoadStateForTest(AdPlacement.GALLERY_INTERSTITIAL),
            controller.nativeFullScreenLoadStateForTest(AdPlacement.RESUME_INTER),
        )
        assertEquals(before, controller.nativeFullScreenLoadStateForTest(AdPlacement.GALLERY_INTERSTITIAL))
        assertTrue(logger.entries.none { "not requested: consent gate closed" in it.message })
    }

    @Test
    fun otherPlacementLoadWhileMaxShows_noMaxLoad() {
        // Review of T-069: the MAX ad on screen is the one SDK object of the slot; no load reaches it meanwhile.
        start("max_admob")
        loadMax()
        assertEquals(true, show())
        controller.loadFullScreenAdIfNeeded(AdPlacement.RESUME_INTER)
        idle()
        assertEquals(1, maxAd.loadCount)
        maxAd.emitHidden()
        idleFor(2_100)
        assertEquals(2, maxAd.loadCount)
    }

    // L-072

    @Test
    fun preloadPlan_beforeMaxReady_maxLoadsWhenReady() {
        start("max_admob", maxReady = false, plan = true)
        controller.requestPreloadForTest()
        idle()
        assertTrue(platform.interstitials.isEmpty())
        platform.finishInitialization()
        idle()
        assertEquals(1, maxAd.loadCount)
    }

    // L-068

    @Test
    fun twoPlacementsShareSlot_secondTurnNotShownSameMaxAd() {
        start("max")
        loadMax()
        assertEquals(true, show())
        maxAd.emitHidden()
        idle()
        assertEquals(false, show(AdPlacement.RESUME_INTER))
        assertEquals(1, maxAd.showCount)
    }

    @Test
    fun twoInterPlacementsInOnePreloadGroup_oneMaxLoad() {
        start("max", plan = true)
        controller.requestPreloadForTest()
        idle()
        assertEquals(1, platform.interstitials.size)
        assertEquals(1, maxAd.loadCount)
    }

    // KB-31, criterion 6

    @Test
    fun maxShown_raisesPresence_secondTurnBusy() {
        start("max")
        loadMax()
        assertEquals(true, show())
        assertTrue(controller.fullScreenAdShowing.value)
        assertEquals(false, show(AdPlacement.RESUME_INTER))
        assertEquals("busy", opportunities(AdPlacement.RESUME_INTER).single().result)
    }

    @Test
    fun maxHidden_recordsIntervalMarkFirst_thenFinishes() {
        settings.set(AdsSettings.ADS_INTERVAL, 30L)
        start("max")
        loadMax()
        assertEquals(true, show())
        maxAd.emitHidden()
        idle()
        assertEquals(1, finishes)
        assertFalse(controller.fullScreenAdShowing.value)
        idleFor(2_100)
        maxAd.emitLoaded()
        idle()
        assertEquals(false, show())
        assertEquals("throttled", opportunities().last().result)
    }

    @Test
    fun maxHidden_opensCombo_whenNativeAfterInterOn() {
        settings.set(AdsSettings.IS_SHOW_NATIVE_AFTER_INTER, true)
        start("max")
        controller.attachFullScreenHost()
        loadMax()
        assertEquals(true, show())
        maxAd.emitHidden()
        idle()
        assertTrue(controller.fullScreenAdShowing.value)
        assertEquals(0, finishes)
        idleFor(2_100)
        assertNotNull(controller.nativeFullScreenView.value)
    }

    @Test
    fun maxDisplayFailed_finishesOnce_showFailMetric() {
        start("max")
        loadMax()
        show()
        maxAd.emitDisplayFailed(-23)
        idle()
        assertEquals(1, finishes)
        assertFalse(controller.fullScreenAdShowing.value)
        assertEquals("-23", metrics.events.filterIsInstance<AdMetricEvent.ShowFail>().single().showErrorCode)
        maxAd.emitHidden()
        idle()
        assertEquals(1, finishes)
    }

    @Test
    fun maxStuck_recovered_finishesOnce() {
        start("max")
        loadMax()
        show()
        controller.onAppActivityPaused(activity)
        controller.onAppActivityResumed(activity)
        idle()
        assertEquals(1, finishes)
        assertFalse(controller.fullScreenAdShowing.value)
    }

    @Test
    fun maxHostGone_skipsBeforeTurn() {
        start("max")
        loadMax()
        activity.finish()
        idle()
        assertEquals(false, show())
        assertEquals(0, maxAd.showCount)
        assertEquals(1, finishes)
        assertEquals(
            "app_not_foreground",
            metrics.events.filterIsInstance<AdMetricEvent.ShowFail>().single().showFailReason,
        )
    }

    // Metrics

    @Test
    fun maxDisplayed_funnelAndInterCount() {
        start("max")
        loadMax()
        show()
        maxAd.emitDisplayed()
        idle()
        assertEquals(
            listOf(AdFunnelEvent.Displayed(AdFunnelKind.INTERS, "INTER", MAX_UNIT, "")),
            funnel.events.filterIsInstance<AdFunnelEvent.Displayed>(),
        )
        assertEquals(1, funnel.events.filterIsInstance<AdFunnelEvent.InterDisplayedTimes>().size)
    }

    @Test
    fun maxClicked_recordsClick() {
        start("max")
        loadMax()
        show()
        maxAd.emitClicked()
        assertEquals(
            listOf(AdMetricEvent.Clicked("INTER", "gallery_interstitial", AdMetricPlatform.APPLOVIN)),
            metrics.events.filterIsInstance<AdMetricEvent.Clicked>(),
        )
    }

    // Plan T-072

    @Test
    fun maxShown_revenue_recordedWithPlacementApplovin() {
        start("max")
        loadMax()
        show()
        maxAd.emitDisplayed()
        idle()
        maxAd.emitRevenue(MAX_INFO)
        idle()
        assertEquals(
            listOf(
                AdMetricEvent.ImpressionRevenue(
                    adFormat = "INTER",
                    adSource = "AppLovin",
                    monetizationNetwork = "AppLovin",
                    adUnitName = MAX_UNIT,
                    currency = "USD",
                    value = 0.0123,
                    adPlacement = "gallery_interstitial",
                    adapterClassName = MAX_ADAPTER,
                    platform = AdMetricPlatform.APPLOVIN,
                ),
            ),
            metrics.events.filterIsInstance<AdMetricEvent.ImpressionRevenue>(),
        )
    }

    @Test
    fun maxDisplayFailed_reasonFromMaxTable() {
        start("max")
        loadMax()
        show()
        maxAd.emitDisplayFailed(-23)
        idle()
        assertEquals(
            listOf(AdMetricEvent.ShowFail("INTER", "gallery_interstitial", "-23", "ad_reused")),
            metrics.events.filterIsInstance<AdMetricEvent.ShowFail>(),
        )
    }

    @Test
    fun maxDisplayed_funnelHasAdapter() {
        start("max")
        loadMax(info = MAX_INFO)
        show()
        maxAd.emitDisplayed()
        idle()
        assertEquals(
            listOf(AdFunnelEvent.ApiCalled(AdFunnelKind.INTERS, "INTER", MAX_UNIT, MAX_ADAPTER)),
            funnel.events.filterIsInstance<AdFunnelEvent.ApiCalled>(),
        )
        assertEquals(
            listOf(AdFunnelEvent.Displayed(AdFunnelKind.INTERS, "INTER", MAX_UNIT, MAX_ADAPTER)),
            funnel.events.filterIsInstance<AdFunnelEvent.Displayed>(),
        )
    }

    @Test
    fun maxFirst_maxFailed_opportunityHasMaxLoadState() {
        start("max_admob")
        controller.loadFullScreenAdIfNeeded(AdPlacement.GALLERY_INTERSTITIAL)
        idle()
        // AdMob fails with no fill (code 3), MAX with no network: the label of the first platform, MAX, is kept.
        maxAd.emitLoadFailed(MAX_NO_NETWORK)
        assertEquals(false, show())
        assertEquals("not_ready", opportunities().single().result)
        assertEquals("network", opportunities().single().loadState)
    }

    @Test
    fun maxShown_hidden_reloadsMaxSlot_noAdmobWhenOrderMax() {
        start("max")
        loadMax()
        show()
        maxAd.emitHidden()
        idleFor(2_100)
        assertEquals(2, maxAd.loadCount)
        assertEquals(listOf<AdPlacement>(), requested.filter { it == AdPlacement.GALLERY_INTERSTITIAL })
    }

    @Test
    fun noMaxKey_interstitialAdmobOnly_noMaxObjectBuilt() {
        admobAnswer = { FakeInterstitialAd() }
        start("max_admob", maxConfig = MaxConfig.NONE)
        controller.loadFullScreenAdIfNeeded(AdPlacement.GALLERY_INTERSTITIAL)
        idle()
        assertTrue(AdPlacement.GALLERY_INTERSTITIAL in requested)
        assertTrue(platform.interstitials.isEmpty())
        assertEquals(true, show())
    }

    // Plan T-070, step 6b

    @Test
    fun admobWaitingRetry_maxShownAndClosed_admobRetriesAtDue() {
        start("max_admob")
        loadMax()
        assertEquals(1, requested.count { it == AdPlacement.GALLERY_INTERSTITIAL })
        // The second AdMob failure: its retry is due 4 s later (F-05).
        idleFor(2_100)
        assertEquals(2, requested.count { it == AdPlacement.GALLERY_INTERSTITIAL })
        assertEquals(true, show())
        assertEquals(1, maxAd.showCount)
        maxAd.emitHidden()
        idle()
        // The reload after the close (2 s) does not cancel that retry: AdMob is sent a third time when it is due.
        idleFor(4_000)
        assertEquals(3, requested.count { it == AdPlacement.GALLERY_INTERSTITIAL })
    }

    private companion object {
        const val MAX_UNIT = "FAKE_UNIT_A"
        const val MAX_NO_NETWORK = -1009
        val MAX_CONFIG = MaxConfig("FAKE_MAX_KEY_0123", "interstitial=$MAX_UNIT")
        const val MAX_ADAPTER = "com.applovin.mediation.adapters.GoogleMediationAdapter"
        val MAX_INFO = MaxAdInfo("FAKE_UNIT_OF_AD", 0.0123, "AppLovin", "exact", MAX_ADAPTER, emptyList())
    }
}
