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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

/**
 * The rewarded ad of AppLovin MAX in the real [AdsController] (plan T-106, KB-31): a decision asks both platforms in
 * the order of `ads_mediation_order` and shows the first that holds an ad, the policy of the placement only when none
 * does (MC-05, R-03); both platforms of the order preload without blocking each other; a MAX turn goes through the
 * same consent gate, showing flag, R-05, R-06, F-03 and F-08 as an AdMob one; the one MAX rewarded object never loads
 * while it shows.
 *
 * AdMob loads go through [AdsController.rewardedRequestForTest] (no SDK), answered with [admobAnswer]; the MAX rewarded
 * ad is the [FakeMaxInterstitialSdkAd] that [FakeMaxPlatform.rewarded] gives, one per unit like the SDK.
 */
@RunWith(AndroidJUnit4::class)
class AdsControllerMaxRewardedTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val settings = FakeRemoteSettings().apply {
        set(AdsSettings.IS_LOG_AD_FULLSCREEN_EVENTS, true)
        set(AdsSettings.placementEnabled(RW), true)
        set(AdsSettings.placementEnabled(AdPlacement.GALLERY_INTERSTITIAL), true)
    }
    private val metrics = RecordingAdMetrics()
    private val funnel = RecordingAdFunnel()
    private val logger = RecordingAppLogger()
    private val platform = FakeMaxPlatform()
    private val rewardedRequested = mutableListOf<AdPlacement>()
    private var admobAnswer: () -> FakeRewardedAd? = { null }
    private val outcomes = mutableListOf<RewardedOutcome>()
    private lateinit var controller: AdsController
    private lateinit var host: ActivityController<ComponentActivity>
    private lateinit var activity: Activity

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun idleFor(millis: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))

    /**
     * The controller with [order], started, consent open, AdMob ready, MAX ready when [maxReady]. The preload plan has
     * the rewarded group with [plan], else no group, so every slot is idle until a test asks for a load.
     */
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
        controller.rewardedRequestForTest = {
            rewardedRequested += it
            admobAnswer()
        }
        controller.setPreloadPlan(AdPreloadPlan(if (plan) listOf(setOf(AdFormat.REWARDED)) else emptyList()))
        controller.start(application, AppActivityFilter { true })
        idle()
        controller.openConsentGateForTest()
        controller.admobReadyForTest()
        idle()
        if (maxReady) {
            platform.finishInitialization()
            idle()
        }
        host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        activity = host.get()
        idle()
    }

    private val maxAd: FakeMaxInterstitialSdkAd get() = platform.rewardeds.last()

    /** Asks the platforms of the rewarded placement to load, then the MAX rewarded answers loaded with [info]. */
    private fun loadMax(info: MaxAdInfo = MaxAdInfo.EMPTY) {
        controller.loadFullScreenAdIfNeeded(RW)
        idle()
        maxAd.emitLoaded(info)
        idle()
    }

    private fun putAdmob(): FakeRewardedAd = FakeRewardedAd().also { controller.putLoadedRewardedForTest(RW, it) }

    /** One tap on "Watch ad". */
    private fun tap(
        policy: RewardedNoAdPolicy = RewardedNoAdPolicy.Block,
        on: Activity = activity,
        into: MutableList<RewardedOutcome> = outcomes,
    ) {
        controller.showRewarded(on, RW, policy) { into += it }
        idle()
    }

    private fun showInterstitial(): Boolean? {
        var result: Boolean? = null
        CoroutineScope(Dispatchers.Main).launch {
            result = controller.showInterstitial(activity, AdPlacement.GALLERY_INTERSTITIAL) {}
        }
        idle()
        return result
    }

    private fun opportunities(placement: AdPlacement = RW) =
        metrics.events.filterIsInstance<AdMetricEvent.Opportunity>().filter { it.adPlacement == placement.key }

    private fun messages() = logger.entries.map { it.message }

    /** The calls of [ad] after its last show. */
    private fun callsAfterShow(ad: FakeMaxInterstitialSdkAd): List<String> =
        ad.calls.drop(ad.calls.lastIndexOf(FakeMaxInterstitialSdkAd.SHOW) + 1)

    private fun assertNoLoadAfterShow(ad: FakeMaxInterstitialSdkAd) {
        val after = callsAfterShow(ad)
        assertTrue(after.toString(), FakeMaxInterstitialSdkAd.LOAD !in after)
        assertTrue(after.toString(), FakeMaxInterstitialSdkAd.LOAD_IGNORED !in after)
    }

    /** No slot of the rewarded placement loaded yet. */
    private fun assertNothingLoadedYet() {
        assertEquals(FullScreenLoadState.Idle, controller.nativeFullScreenLoadStateForTest(RW))
        assertTrue(rewardedRequested.isEmpty())
        assertEquals(0, platform.rewardedCalls)
    }

    // KB-31, criterion 1

    @Test
    fun maxFirst_noMaxAd_admobReady_showsAdmob_oneOpportunity() {
        start("max_admob")
        val admob = putAdmob()
        tap()
        assertEquals(1, admob.shows)
        assertEquals(0, platform.rewardeds.sumOf { it.showCount })
        assertEquals(
            listOf(AdMetricEvent.Opportunity("REWARDED", "gallery_rw_action", "shown", "ready")),
            opportunities(),
        )
        assertTrue(outcomes.none { it is RewardedOutcome.NoAd })
        // Control: after the close, both platforms of the order load again (F-06).
        admob.callback!!.onAdDismissedFullScreenContent()
        idleFor(2_100)
        assertTrue(rewardedRequested.toString(), RW in rewardedRequested)
        assertEquals(1, maxAd.loadCount)
    }

    @Test
    fun admobFirst_noAdmobAd_maxReady_showsMax_rewardInCallback() {
        start("admob_max")
        loadMax()
        tap()
        assertEquals(1, maxAd.showCount)
        assertTrue(outcomes.isEmpty())
        maxAd.emitDisplayed()
        idle()
        assertEquals(listOf(RewardedOutcome.OnScreen), outcomes)
        // R-02: the reward in the reward callback, before the close.
        maxAd.emitRewarded()
        idle()
        assertEquals(listOf(RewardedOutcome.OnScreen, RewardedOutcome.Granted), outcomes)
        assertEquals(1, metrics.events.filterIsInstance<AdMetricEvent.RewardEarned>().size)
        maxAd.emitHidden()
        idle()
        assertEquals(listOf(RewardedOutcome.OnScreen, RewardedOutcome.Granted), outcomes)
        assertEquals(
            listOf(AdMetricEvent.Opportunity("REWARDED", "gallery_rw_action", "shown", "ready")),
            opportunities(),
        )
    }

    @Test
    fun bothReady_firstPlatformShown() {
        start("max_admob")
        val admob = putAdmob()
        loadMax()
        tap()
        assertEquals(1, maxAd.showCount)
        assertEquals(0, admob.shows)
    }

    // MC-05, R-03

    @Test
    fun bothNoAd_policyAppliedOnce() {
        start("max_admob")
        assertNothingLoadedYet()
        admobAnswer = { null }
        tap(RewardedNoAdPolicy.Allow)
        assertEquals(listOf(RewardedOutcome.NoAd(network = false, granted = true)), outcomes)
        assertEquals(
            listOf(AdMetricEvent.Opportunity("REWARDED", "gallery_rw_action", "not_ready", "idle")),
            opportunities(),
        )
        // The decision asked for a load (F-07): both platforms were sent.
        assertEquals(listOf(RW), rewardedRequested)
        assertEquals(1, platform.rewardeds.size)
        assertEquals(1, maxAd.loadCount)
        maxAd.emitLoadFailed(204)
        idle()
        tap(RewardedNoAdPolicy.Block)
        assertEquals(RewardedOutcome.NoAd(network = false, granted = false), outcomes.last())
        assertEquals(2, outcomes.size)
    }

    @Test
    fun maxOnly_networkErrorCode_noAdNetworkTrue() {
        start("max")
        controller.loadFullScreenAdIfNeeded(RW)
        idle()
        maxAd.emitLoadFailed(MAX_NO_NETWORK)
        idle()
        tap()
        assertEquals(listOf(RewardedOutcome.NoAd(network = true, granted = false)), outcomes)
    }

    @Test
    fun maxOnly_noFill_noAdNetworkFalse() {
        start("max")
        controller.loadFullScreenAdIfNeeded(RW)
        idle()
        maxAd.emitLoadFailed(204)
        idle()
        tap()
        assertEquals(listOf(RewardedOutcome.NoAd(network = false, granted = false)), outcomes)
    }

    @Test
    fun bothPlatforms_maxNetworkError_admobNoFill_networkFalse() {
        start("max_admob")
        controller.loadFullScreenAdIfNeeded(RW)
        idle()
        assertEquals(listOf(RW), rewardedRequested)
        maxAd.emitLoadFailed(-1000)
        idle()
        tap()
        assertEquals(listOf(RewardedOutcome.NoAd(network = false, granted = false)), outcomes)
    }

    @Test
    fun bothPlatforms_bothNetworkErrors_networkTrue() {
        start("max_admob")
        controller.rewardedLoadErrorCodeForTest = ADMOB_NETWORK_ERROR
        controller.loadFullScreenAdIfNeeded(RW)
        idle()
        assertEquals(listOf(RW), rewardedRequested)
        maxAd.emitLoadFailed(-1001)
        idle()
        tap()
        assertEquals(listOf(RewardedOutcome.NoAd(network = true, granted = false)), outcomes)
    }

    @Test
    fun bothPlatforms_admobNetworkError_maxNotLoadedYet_networkTrue() {
        start("max_admob", maxReady = false)
        controller.rewardedLoadErrorCodeForTest = ADMOB_NETWORK_ERROR
        controller.loadFullScreenAdIfNeeded(RW)
        idle()
        assertEquals(listOf(RW), rewardedRequested)
        assertTrue(platform.rewardeds.isEmpty())
        tap()
        assertEquals(listOf(RewardedOutcome.NoAd(network = true, granted = false)), outcomes)
    }

    // ADR-0045, section 2.3

    @Test
    fun maxAdmob_preload_bothPlatformsLoad() {
        admobAnswer = { FakeRewardedAd() }
        start("max_admob", plan = true)
        controller.requestPreloadForTest()
        idle()
        assertTrue(rewardedRequested.toString(), RW in rewardedRequested)
        assertEquals(1, maxAd.loadCount)
    }

    @Test
    fun maxReady_admobFailed_retryStillLoadsAdmob() {
        start("admob_max")
        loadMax()
        assertEquals(1, rewardedRequested.count { it == RW })
        assertTrue(controller.isFullScreenAdReady(RW))
        // The AdMob failure retries (F-05) although the turn already has the MAX ad.
        idleFor(2_100)
        assertEquals(2, rewardedRequested.count { it == RW })
    }

    // KB-31, criterion 2

    @Test
    fun orderAdmob_maxRewardedNeverLoads() {
        start("admob", plan = true)
        controller.requestPreloadForTest()
        idle()
        tap()
        assertTrue(platform.rewardeds.isEmpty())
        assertEquals(0, platform.rewardedCalls)
        // Control: AdMob was asked.
        assertTrue(rewardedRequested.toString(), RW in rewardedRequested)
    }

    @Test
    fun orderMax_noAdmobRewardedRequest_onlyMaxLoads() {
        start("max", plan = true)
        controller.requestPreloadForTest()
        idle()
        tap()
        assertEquals(listOf<AdPlacement>(), rewardedRequested)
        assertEquals(1, maxAd.loadCount)
    }

    @Test
    fun orderMax_noMaxRewardedUnit_noAd_noAdmobRequest() {
        start("max", maxConfig = MaxConfig(MAX_KEY, "interstitial=FAKE_UNIT_I"), plan = true)
        controller.requestPreloadForTest()
        idle()
        tap()
        assertEquals(listOf(RewardedOutcome.NoAd(network = false, granted = false)), outcomes)
        assertEquals(listOf<AdPlacement>(), rewardedRequested)
        assertTrue(platform.rewardeds.isEmpty())
        // No platform at all: nothing loads, not even AdMob.
        assertEquals(FullScreenLoadState.Idle, controller.nativeFullScreenLoadStateForTest(RW))
    }

    @Test
    fun orderChangedToAdmob_whileMaxHoldsAd_maxNotShown() {
        start("max_admob")
        loadMax()
        val admob = putAdmob()
        settings.set(AdsSettings.ADS_MEDIATION_ORDER, "admob")
        idle()
        tap()
        assertEquals(1, admob.shows)
        assertEquals(0, maxAd.showCount)
    }

    @Test
    fun rewardedSwitchedOff_maxRetryDue_noRequest() {
        start("max")
        controller.loadFullScreenAdIfNeeded(RW)
        idle()
        maxAd.emitLoadFailed(204)
        settings.set(AdsSettings.placementEnabled(RW), false)
        idle()
        idleFor(2_100)
        assertEquals(1, maxAd.loadCount)
        assertTrue(messages().toString(), "Rewarded (MAX) not requested: not in order or placements off" in messages())
    }

    // KB-31, criterion 5

    @Test
    fun consentClosed_maxHoldsAd_grantsAtOnce_notShown() {
        start("max")
        loadMax()
        controller.closeConsentGateForTest()
        idle()
        tap()
        assertEquals(listOf(RewardedOutcome.Granted), outcomes)
        assertTrue(opportunities().isEmpty())
        assertEquals(0, maxAd.showCount)
    }

    @Test
    fun gateClosedAtSend_thenReopened_maxRewardedLoadsOnce() {
        start("max", plan = true)
        // The plan ran once, so the gate open again preloads (GATE_REOPENED needs a first preload).
        controller.requestPreloadForTest()
        idle()
        maxAd.emitLoadFailed(204)
        controller.closeConsentGateForTest()
        idle()
        idleFor(2_100)
        assertEquals(1, maxAd.loadCount)
        assertEquals(FullScreenLoadState.Idle, controller.nativeFullScreenLoadStateForTest(RW))
        controller.reopenConsentGateForTest()
        idle()
        assertEquals(2, maxAd.loadCount)
    }

    @Test
    fun gateClosed_loadFullScreenAdIfNeeded_noSendNoLog() {
        start("max_admob")
        assertNothingLoadedYet()
        controller.closeConsentGateForTest()
        idle()
        controller.loadFullScreenAdIfNeeded(RW)
        idle()
        assertEquals(FullScreenLoadState.Idle, controller.nativeFullScreenLoadStateForTest(RW))
        assertTrue(rewardedRequested.isEmpty())
        assertEquals(0, platform.rewardedCalls)
        assertFalse("Rewarded gallery_rw_action not requested: consent gate closed" in messages())
        assertFalse("Rewarded (MAX) not requested: consent gate closed" in messages())
    }

    // L-072

    @Test
    fun preloadPlan_beforeMaxReady_maxRewardedLoadsWhenReady() {
        start("max_admob", maxReady = false, plan = true)
        controller.requestPreloadForTest()
        idle()
        assertTrue(platform.rewardeds.isEmpty())
        platform.finishInitialization()
        idle()
        assertEquals(1, maxAd.loadCount)
    }

    // L-068

    @Test
    fun decisionLoad_whileMaxLoading_oneMaxLoad() {
        start("max", plan = true)
        controller.requestPreloadForTest()
        idle()
        assertEquals(1, maxAd.loadCount)
        tap()
        assertEquals(listOf(RewardedOutcome.NoAd(network = false, granted = false)), outcomes)
        assertEquals(1, maxAd.loadCount)
        assertEquals(1, platform.rewardeds.size)
    }

    @Test
    fun twoTapsBeforeLoad_oneMaxLoad() {
        start("max")
        val first = mutableListOf<RewardedOutcome>()
        val second = mutableListOf<RewardedOutcome>()
        controller.showRewarded(activity, RW, RewardedNoAdPolicy.Block) { first += it }
        controller.showRewarded(activity, RW, RewardedNoAdPolicy.Block) { second += it }
        idle()
        assertEquals(1, platform.rewardeds.size)
        assertEquals(1, maxAd.loadCount)
        assertEquals(1, first.size)
        assertEquals(1, second.size)
    }

    // R-04, F-06

    @Test
    fun maxClosedBeforeReward_noGrant_reloadsAfterTwoSeconds() {
        start("max")
        loadMax()
        tap()
        maxAd.emitDisplayed()
        idle()
        maxAd.emitHidden()
        idle()
        assertEquals(listOf(RewardedOutcome.OnScreen), outcomes)
        assertTrue(messages().toString(), "Rewarded gallery_rw_action closed without reward" in messages())
        idleFor(1_999)
        assertEquals(1, maxAd.loadCount)
        idleFor(1)
        assertEquals(2, maxAd.loadCount)
    }

    @Test
    fun maxDisplayFailed_policyOutcome_showFailMetric_reloads() {
        start("max")
        loadMax()
        tap(RewardedNoAdPolicy.Block)
        maxAd.emitDisplayFailed(-24)
        idle()
        assertEquals(listOf(RewardedOutcome.NoAd(network = false, granted = false)), outcomes)
        assertEquals("-24", metrics.events.filterIsInstance<AdMetricEvent.ShowFail>().single().showErrorCode)
        assertFalse(controller.fullScreenAdShowing.value)
        idleFor(2_100)
        assertEquals(2, maxAd.loadCount)
    }

    // R-06

    @Test
    fun maxSecondTapBeforeOnScreen_showsAgain_busy_rewardOnce() {
        start("max")
        loadMax()
        val first = mutableListOf<RewardedOutcome>()
        val second = mutableListOf<RewardedOutcome>()
        tap(into = first)
        tap(into = second)
        assertEquals(2, maxAd.showCount)
        assertEquals(listOf("shown", "busy"), opportunities().map { it.result })
        maxAd.emitDisplayed()
        idle()
        // The second show fails while the first is on screen: the turn goes on.
        maxAd.emitDisplayFailed(-24)
        idle()
        maxAd.emitRewarded()
        idle()
        assertEquals(listOf(RewardedOutcome.OnScreen, RewardedOutcome.Granted), second)
        assertEquals(listOf(RewardedOutcome.OnScreen), first)
        assertEquals(1, metrics.events.filterIsInstance<AdMetricEvent.RewardEarned>().size)
        maxAd.emitHidden()
        idleFor(2_100)
        assertEquals(2, maxAd.loadCount)
        assertEquals(listOf(RewardedOutcome.OnScreen, RewardedOutcome.Granted), second)
    }

    // R-05

    @Test
    fun maxRewardedShown_noIntervalMark() {
        settings.set(AdsSettings.ADS_INTERVAL, 30L)
        start("max_admob")
        loadMax()
        tap()
        maxAd.emitDisplayed()
        maxAd.emitHidden()
        idle()
        assertEquals(1, outcomes.count { it == RewardedOutcome.OnScreen })
        val inter = FakeInterstitialAd()
        controller.putLoadedInterstitialForTest(AdPlacement.GALLERY_INTERSTITIAL, inter)
        assertEquals(true, showInterstitial())
        assertEquals(1, inter.shows)
        assertEquals("shown", opportunities(AdPlacement.GALLERY_INTERSTITIAL).single().result)
    }

    // KB-31, criterion 6

    @Test
    fun maxRewarded_raisesPresence_interstitialBusy() {
        start("max_admob")
        loadMax()
        tap()
        maxAd.emitDisplayed()
        idle()
        assertTrue(controller.fullScreenAdShowing.value)
        controller.putLoadedInterstitialForTest(AdPlacement.GALLERY_INTERSTITIAL, FakeInterstitialAd())
        assertEquals(false, showInterstitial())
        assertEquals("busy", opportunities(AdPlacement.GALLERY_INTERSTITIAL).single().result)
        maxAd.emitHidden()
        idle()
        assertFalse(controller.fullScreenAdShowing.value)
    }

    // F-08

    @Test
    fun maxReady_hostGone_skipped_adStaysInSlot() {
        start("max")
        loadMax()
        activity.finish()
        idle()
        tap()
        assertEquals(
            listOf(AdMetricEvent.Opportunity("REWARDED", "gallery_rw_action", "shown", "ready")),
            opportunities(),
        )
        assertEquals(
            "app_not_foreground",
            metrics.events.filterIsInstance<AdMetricEvent.ShowFail>().single().showFailReason,
        )
        assertEquals(0, maxAd.showCount)
        assertTrue(controller.isFullScreenAdReady(RW))
    }

    @Test
    fun maxReady_stoppedHost_notShown_keptInSlot() {
        start("max")
        loadMax()
        host.pause().stop()
        idle()
        tap()
        assertEquals(0, maxAd.showCount)
        assertTrue(controller.isFullScreenAdReady(RW))
        host.start().resume()
        idleFor(6_000)
        assertEquals(0, maxAd.showCount)
    }

    // F-03

    @Test
    fun maxRewardedStuck_lateRewardIgnored_lateHiddenReloads() {
        start("max_admob")
        loadMax()
        val first = mutableListOf<RewardedOutcome>()
        tap(into = first)
        assertEquals(1, maxAd.showCount)
        controller.onAppActivityPaused(activity)
        controller.onAppActivityResumed(activity)
        idle()
        assertFalse(controller.fullScreenAdShowing.value)
        val admob = putAdmob()
        val second = mutableListOf<RewardedOutcome>()
        tap(into = second)
        assertEquals(1, admob.shows)
        // The MAX ad of the stuck turn rewards late: nobody gets it.
        maxAd.emitRewarded()
        idle()
        assertTrue(first.isEmpty())
        assertTrue(second.isEmpty())
        admob.earnReward()
        assertEquals(listOf(RewardedOutcome.Granted), second)
        val sent = rewardedRequested.size
        admob.callback!!.onAdDismissedFullScreenContent()
        idleFor(2_100)
        assertTrue(rewardedRequested.size > sent)
        // The MAX object has not reported its close: no load reached it.
        assertNoLoadAfterShow(maxAd)
        maxAd.emitHidden()
        idleFor(2_100)
        assertEquals(2, maxAd.loadCount)
        assertEquals(1, platform.rewardeds.size)
        assertTrue(first.isEmpty())
    }

    @Test
    fun maxRewardedNeverClosed_afterCap_newSdkObject() {
        start("max_admob")
        loadMax()
        tap()
        val old = maxAd
        controller.onAppActivityPaused(activity)
        controller.onAppActivityResumed(activity)
        idle()
        idleFor(600_000)
        tap()
        assertTrue(FakeMaxInterstitialSdkAd.DESTROY in old.calls)
        assertEquals(2, platform.rewardeds.size)
        assertEquals(1, platform.rewardeds[1].loadCount)
        assertEquals(2, platform.rewardedCalls)
    }

    @Test
    fun maxRewardedShowing_preloadRound_noMaxLoad() {
        start("max", plan = true)
        loadMax()
        tap()
        maxAd.emitDisplayed()
        idle()
        controller.requestPreloadForTest()
        idle()
        controller.loadFullScreenAdIfNeeded(RW)
        idle()
        idleFor(3_000)
        assertNoLoadAfterShow(maxAd)
        maxAd.emitHidden()
        idleFor(2_100)
        assertEquals(2, maxAd.loadCount)
    }

    // Metrics

    @Test
    fun maxRewarded_displayed_clicked_metrics() {
        start("max")
        loadMax()
        tap()
        maxAd.emitDisplayed()
        maxAd.emitClicked()
        maxAd.emitRewarded()
        idle()
        assertEquals(
            listOf(AdFunnelEvent.Displayed(AdFunnelKind.REWARDED, "REWARDED", MAX_UNIT, "")),
            funnel.events.filterIsInstance<AdFunnelEvent.Displayed>(),
        )
        assertEquals(
            listOf(AdMetricEvent.Clicked("REWARDED", "gallery_rw_action", AdMetricPlatform.APPLOVIN)),
            metrics.events.filterIsInstance<AdMetricEvent.Clicked>(),
        )
        assertEquals(
            listOf(AdMetricEvent.RewardEarned("REWARDED", "gallery_rw_action")),
            metrics.events.filterIsInstance<AdMetricEvent.RewardEarned>(),
        )
    }

    // Plan T-072

    @Test
    fun maxShown_revenue_recordedWithPlacementApplovin() {
        start("max")
        loadMax()
        tap()
        maxAd.emitDisplayed()
        maxAd.emitRevenue(MAX_INFO)
        idle()
        assertEquals(
            listOf(
                AdMetricEvent.ImpressionRevenue(
                    adFormat = "REWARDED",
                    adSource = "AppLovin",
                    monetizationNetwork = "AppLovin",
                    adUnitName = MAX_UNIT,
                    currency = "USD",
                    value = 0.0123,
                    adPlacement = "gallery_rw_action",
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
        tap()
        maxAd.emitDisplayFailed(-24)
        idle()
        assertEquals(
            listOf(AdMetricEvent.ShowFail("REWARDED", "gallery_rw_action", "-24", "not_ready")),
            metrics.events.filterIsInstance<AdMetricEvent.ShowFail>(),
        )
    }

    @Test
    fun maxDisplayed_funnelHasAdapter() {
        start("max")
        loadMax(MAX_INFO)
        tap()
        maxAd.emitDisplayed()
        idle()
        assertEquals(
            listOf(AdFunnelEvent.ApiCalled(AdFunnelKind.REWARDED, "REWARDED", MAX_UNIT, MAX_ADAPTER)),
            funnel.events.filterIsInstance<AdFunnelEvent.ApiCalled>(),
        )
        assertEquals(
            listOf(AdFunnelEvent.Displayed(AdFunnelKind.REWARDED, "REWARDED", MAX_UNIT, MAX_ADAPTER)),
            funnel.events.filterIsInstance<AdFunnelEvent.Displayed>(),
        )
    }

    @Test
    fun maxClicked_applovinPlatform() {
        start("max")
        loadMax()
        tap()
        maxAd.emitDisplayed()
        maxAd.emitClicked()
        idle()
        assertEquals(
            listOf(AdMetricPlatform.APPLOVIN),
            metrics.events.filterIsInstance<AdMetricEvent.Clicked>().map { it.platform },
        )
        assertEquals(
            listOf(AdMetricPlatform.APPLOVIN),
            metrics.events.filterIsInstance<AdMetricEvent.LoadSuccess>().map { it.platform },
        )
    }

    @Test
    fun maxFirst_maxFailed_opportunityHasMaxLoadState() {
        start("max_admob")
        controller.loadFullScreenAdIfNeeded(RW)
        idle()
        // AdMob fails with no fill (code 3), MAX with no network: the label of the first platform, MAX, is kept.
        maxAd.emitLoadFailed(MAX_NO_NETWORK)
        idle()
        tap()
        assertEquals("not_ready", opportunities().single().result)
        assertEquals("network", opportunities().single().loadState)
    }

    @Test
    fun maxFirst_maxFailed_admobNeverSent_opportunityHasMaxLoadState() {
        // MAX alone loads and fails on the network; the order then puts AdMob second, AdMob not sent yet when tapped.
        start("max")
        controller.loadFullScreenAdIfNeeded(RW)
        idle()
        maxAd.emitLoadFailed(MAX_NO_NETWORK)
        idle()
        settings.set(AdsSettings.ADS_MEDIATION_ORDER, "max_admob")
        idle()
        assertTrue(rewardedRequested.toString(), rewardedRequested.isEmpty())
        tap()
        assertEquals("not_ready", opportunities().single().result)
        assertEquals("network", opportunities().single().loadState)
    }

    // No MAX key: as before

    @Test
    fun noMaxKey_rewardedAdmobOnly_noMaxObjectBuilt() {
        start("max_admob", maxConfig = MaxConfig.NONE, plan = true)
        controller.requestPreloadForTest()
        idle()
        tap()
        assertEquals(0, platform.rewardedCalls)
        // Control: AdMob was asked.
        assertTrue(rewardedRequested.toString(), RW in rewardedRequested)
    }

    // Plan T-070, step 6b

    @Test
    fun admobWaitingRetry_maxShownAndClosed_admobRetriesAtDue() {
        start("max_admob")
        loadMax()
        assertEquals(1, rewardedRequested.count { it == RW })
        // The second AdMob failure: its retry is due 4 s later (F-05).
        idleFor(2_100)
        assertEquals(2, rewardedRequested.count { it == RW })
        tap()
        assertEquals(1, maxAd.showCount)
        maxAd.emitDisplayed()
        maxAd.emitHidden()
        idle()
        // The reload after the close (2 s) does not cancel that retry: AdMob is sent a third time when it is due.
        idleFor(4_000)
        assertEquals(3, rewardedRequested.count { it == RW })
    }

    private companion object {
        val RW = AdPlacement.GALLERY_RW_ACTION
        const val MAX_UNIT = "FAKE_UNIT_R"
        const val MAX_KEY = "FAKE_MAX_KEY_0123"
        const val MAX_NO_NETWORK = -1009
        const val ADMOB_NETWORK_ERROR = 2
        val MAX_CONFIG = MaxConfig(MAX_KEY, "rewarded=$MAX_UNIT")
        const val MAX_ADAPTER = "com.applovin.mediation.adapters.GoogleMediationAdapter"
        val MAX_INFO = MaxAdInfo("FAKE_UNIT_OF_AD", 0.0123, "AppLovin", "exact", MAX_ADAPTER, emptyList())
    }
}
