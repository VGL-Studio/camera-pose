package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.analytics.AdMetricEvent
import com.vglstudio.camera.pose.line.guide.core.analytics.RecordingAdMetrics
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import java.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

/**
 * The real [AdsController] and the activity that shows a full-screen ad (KB-26, criteria 3 and 4; plan T-055).
 *
 * - Before every `show` of the SDK the activity must not be finishing nor destroyed and must be the resumed activity of
 *   the app; else the ad stays in its slot, the completion runs once, with `shown` then `ad_show_fail`
 *   `app_not_foreground`.
 * - An activity recreated for a configuration change (rotation) keeps the turn: the completion runs when the ad closes,
 *   once; a real destruction still forces it (N-04, HD-03).
 *
 * The SDK ads are fakes put in their slot ([FakeInterstitialAd] and the others); the controller is started, so the
 * activity tracker follows the activities built after it.
 */
@RunWith(AndroidJUnit4::class)
class AdsControllerHostTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val settings = FakeRemoteSettings().apply {
        // The lifecycle metrics on, else no `ad_show_fail` is ever recorded and the checks prove nothing.
        set(AdsSettings.IS_LOG_AD_FULLSCREEN_EVENTS, true)
        set(AdsSettings.placementEnabled(AdPlacement.GALLERY_INTERSTITIAL), true)
        set(AdsSettings.placementEnabled(AdPlacement.GALLERY_APP_OPEN), true)
        set(AdsSettings.placementEnabled(AdPlacement.GALLERY_RW_ACTION), true)
    }
    private val metrics = RecordingAdMetrics()
    private val logger = RecordingAppLogger()
    private var finishes = 0
    private val onFinished: () -> Unit = { finishes++ }

    private val controller = testAdsController(
        context = application,
        // No ad unit: nothing is ever loaded; the ads are put in their slot by the tests.
        unitIds = AdUnitIds { null },
        flags = DefaultAdsFlags(settings),
        metrics = metrics,
        logger = logger,
    )

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun idleFor(millis: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))

    /** Starts the controller (activity tracker) and opens the consent gate, as on a device. */
    private fun start() {
        controller.start(application, AppActivityFilter { true })
        idle()
        controller.openConsentGateForTest()
        idle()
        assertTrue(controller.canRequestAds.value)
    }

    /** An activity of the app, built after [start] so the tracker sees it resumed. */
    private fun resumedActivity(): ActivityController<ComponentActivity> =
        Robolectric.buildActivity(ComponentActivity::class.java).setup()

    private fun showFails(): List<AdMetricEvent.ShowFail> = metrics.events.filterIsInstance<AdMetricEvent.ShowFail>()

    private fun opportunities(placement: AdPlacement): List<AdMetricEvent.Opportunity> =
        metrics.events.filterIsInstance<AdMetricEvent.Opportunity>().filter { it.adPlacement == placement.key }

    private fun showInterstitial(activity: Activity, done: () -> Unit = onFinished): Boolean? {
        var result: Boolean? = null
        CoroutineScope(Dispatchers.Main).launch {
            result = controller.showInterstitial(activity, AdPlacement.GALLERY_INTERSTITIAL, done)
        }
        idle()
        return result
    }

    private fun showGalleryAppOpen(activity: Activity, done: () -> Unit = onFinished): Boolean? {
        var result: Boolean? = null
        CoroutineScope(Dispatchers.Main).launch {
            result = controller.showStagingAppOpen(activity, AdPlacement.GALLERY_APP_OPEN, done)
        }
        idle()
        return result
    }

    /** Checks an interstitial turn skipped because its activity is not usable. */
    private fun assertInterstitialSkippedHostGone(fake: FakeInterstitialAd, result: Boolean?) {
        assertEquals(false, result)
        assertEquals(0, fake.shows)
        assertEquals(1, finishes)
        assertTrue(controller.isFullScreenAdReady(AdPlacement.GALLERY_INTERSTITIAL))
        assertFalse(controller.fullScreenAdShowing.value)
        assertEquals(
            listOf(AdMetricEvent.ShowFail("INTER", "gallery_interstitial", "3", "app_not_foreground")),
            showFails(),
        )
        assertEquals(
            listOf(AdMetricEvent.Opportunity("INTER", "gallery_interstitial", "shown", "ready")),
            opportunities(AdPlacement.GALLERY_INTERSTITIAL),
        )
        idleFor(5_000)
        assertEquals(1, finishes)
    }

    /** Control of the skipped turns: a resumed activity of the app gets the same ad. */
    private fun assertShownOnResumedActivity(fake: FakeInterstitialAd) {
        val second = resumedActivity().get()
        assertEquals(true, showInterstitial(second) {})
        assertEquals(1, fake.shows)
        assertSame(second, fake.shownOn.single())
    }

    @Test
    fun interstitial_stoppedHost_notShown_keptInSlot_showFailOnce() {
        start()
        val fake = FakeInterstitialAd()
        controller.putLoadedInterstitialForTest(AdPlacement.GALLERY_INTERSTITIAL, fake)
        val activity = resumedActivity().pause().stop().get()

        assertInterstitialSkippedHostGone(fake, showInterstitial(activity))
        assertShownOnResumedActivity(fake)
    }

    @Test
    fun interstitial_finishingHost_notShown_keptInSlot() {
        start()
        val fake = FakeInterstitialAd()
        controller.putLoadedInterstitialForTest(AdPlacement.GALLERY_INTERSTITIAL, fake)
        val activity = resumedActivity().get()
        activity.finish()
        assertTrue(activity.isFinishing)

        assertInterstitialSkippedHostGone(fake, showInterstitial(activity))
        assertShownOnResumedActivity(fake)
    }

    @Test
    fun interstitial_destroyedHost_notShown_keptInSlot() {
        start()
        val fake = FakeInterstitialAd()
        controller.putLoadedInterstitialForTest(AdPlacement.GALLERY_INTERSTITIAL, fake)
        val activity = resumedActivity().pause().stop().destroy().get()
        assertTrue(activity.isDestroyed)

        assertInterstitialSkippedHostGone(fake, showInterstitial(activity))
        assertShownOnResumedActivity(fake)
    }

    @Test
    fun interstitial_hostGone_comboOn_noNativeFullScreen() {
        settings.set(AdsSettings.IS_SHOW_NATIVE_AFTER_INTER, true)
        start()
        val fake = FakeInterstitialAd()
        controller.putLoadedInterstitialForTest(AdPlacement.GALLERY_INTERSTITIAL, fake)
        val activity = resumedActivity().pause().stop().get()

        assertEquals(false, showInterstitial(activity))
        // KB-26, criterion 3, and the exception of KB-16, criterion 7: the user goes on at once, no native ad.
        idleFor(2_100)
        assertNull(controller.nativeFullScreenView.value)
        assertEquals(1, finishes)
        assertFalse(controller.fullScreenAdShowing.value)
        assertEquals(0, fake.shows)
    }

    @Test
    fun appOpen_stoppedHost_notShown_keptInSlot_showFailOnce() {
        start()
        val fake = FakeAppOpenAd()
        controller.putLoadedAppOpenForTest(AdPlacement.GALLERY_APP_OPEN, fake)
        val activity = resumedActivity().pause().stop().get()

        assertEquals(false, showGalleryAppOpen(activity))
        assertEquals(0, fake.shows)
        assertEquals(1, finishes)
        assertTrue(controller.isFullScreenAdReady(AdPlacement.GALLERY_APP_OPEN))
        assertFalse(controller.fullScreenAdShowing.value)
        assertEquals(
            listOf(AdMetricEvent.ShowFail("APP_OPEN", "gallery_app_open", "3", "app_not_foreground")),
            showFails(),
        )
        assertEquals(
            listOf(AdMetricEvent.Opportunity("APP_OPEN", "gallery_app_open", "shown", "ready")),
            opportunities(AdPlacement.GALLERY_APP_OPEN),
        )

        // Control: a resumed activity gets the same ad.
        val second = resumedActivity().get()
        assertEquals(true, showGalleryAppOpen(second) {})
        assertEquals(1, fake.shows)
        assertSame(second, fake.shownOn.single())
    }

    @Test
    fun startup_destroyedHost_appOpen_showFail_finishesFalse() {
        // The keys of this open choose `open_app` (StartupAdChoiceTest).
        settings.set(AdsSettings.SHOW_OPEN_ADS, true)
        settings.set(AdsSettings.SHOW_OPEN_ADS_FIRST_OPEN, true)
        start()
        val fake = FakeAppOpenAd()
        controller.putLoadedAppOpenForTest(AdPlacement.OPEN_APP, fake)
        val activity = resumedActivity().pause().stop().destroy().get()
        val results = mutableListOf<Boolean>()

        controller.showStartupAd(activity, StartupAdTurn(AdPlacement.OPEN_APP)) { results += it }
        idle()

        assertEquals(listOf(false), results)
        assertEquals(0, fake.shows)
        assertTrue(controller.isFullScreenAdReady(AdPlacement.OPEN_APP))
        assertEquals(
            listOf(AdMetricEvent.ShowFail("APP_OPEN", "open_app", "3", "app_not_foreground")),
            showFails(),
        )
        idleFor(5_000)
        assertEquals(listOf(false), results)

        // Control: on a resumed activity the same startup turn shows the ad.
        val second = resumedActivity().get()
        controller.showStartupAd(second, StartupAdTurn(AdPlacement.OPEN_APP)) { results += it }
        idle()
        assertEquals(1, fake.shows)
        assertSame(second, fake.shownOn.single())
    }

    @Test
    fun resumeAd_fromOnActivityResumed_passesHostCheck() {
        settings.set(AdsSettings.SHOW_RESUM_ADS, true)
        start()
        // The 45 s cap of the startup turn over, so a return reaches the resume ad (AdsControllerResumeTest).
        idleFor(45_100)
        val fake = FakeAppOpenAd()
        controller.putLoadedAppOpenForTest(AdPlacement.RESUME, fake)
        val host = resumedActivity()
        assertEquals(0, fake.shows)

        // Out to background and back: the resume ad shows from onActivityResumed of the tracker.
        host.pause().stop()
        host.start().resume()
        idle()

        assertEquals(1, fake.shows)
        assertSame(host.get(), fake.shownOn.single())
        assertTrue(showFails().isEmpty())
    }

    @Test
    fun hostCheck_insideOnActivityResumed_usesTracker() {
        start()
        val usable = mutableListOf<Boolean>()
        val states = mutableListOf<Lifecycle.State>()
        // Registered after the tracker of start(), so it runs after it, as the resume ad does.
        val probe = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                usable += controller.isHostUsable(activity)
                states += (activity as ComponentActivity).lifecycle.currentState
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

            override fun onActivityStarted(activity: Activity) = Unit

            override fun onActivityPaused(activity: Activity) = Unit

            override fun onActivityStopped(activity: Activity) = Unit

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

            override fun onActivityDestroyed(activity: Activity) = Unit
        }
        application.registerActivityLifecycleCallbacks(probe)
        try {
            resumedActivity()
        } finally {
            application.unregisterActivityLifecycleCallbacks(probe)
        }

        assertEquals(listOf(true), usable)
        // Control: Lifecycle is not RESUMED yet inside onActivityResumed (API 29 and up), so only the tracker passes.
        assertEquals(listOf(Lifecycle.State.STARTED), states)
    }

    @Test
    fun rewarded_stoppedHost_notShown_keptInSlot() {
        start()
        val fake = FakeRewardedAd()
        controller.putLoadedRewardedForTest(AdPlacement.GALLERY_RW_ACTION, fake)
        val activity = resumedActivity().pause().stop().get()
        val outcomes = mutableListOf<RewardedOutcome>()

        controller.showRewarded(activity, AdPlacement.GALLERY_RW_ACTION) { outcomes += it }
        idle()

        assertEquals(0, fake.shows)
        assertTrue(controller.isFullScreenAdReady(AdPlacement.GALLERY_RW_ACTION))
        assertFalse(controller.fullScreenAdShowing.value)
        assertEquals(
            listOf(AdMetricEvent.ShowFail("REWARDED", "gallery_rw_action", "3", "app_not_foreground")),
            showFails(),
        )
        // The line's policy (block) applies, as for a show that fails (R-04).
        assertEquals(listOf(RewardedOutcome.NoAd(network = false, granted = false)), outcomes)

        // Control: a resumed activity gets the same ad.
        val second = resumedActivity().get()
        controller.showRewarded(second, AdPlacement.GALLERY_RW_ACTION) {}
        idle()
        assertEquals(1, fake.shows)
        assertSame(second, fake.shownOn.single())
    }

    /** Opens an interstitial turn of the gallery on [host], with no combo. */
    private fun beginTurnOn(host: Activity): FullScreenCompletion {
        val turn = controller.beginInterstitialTurn(
            AdPlacement.GALLERY_INTERSTITIAL,
            System.identityHashCode(host),
            combo = false,
            onFinished,
        )
        assertNotNull(turn)
        assertTrue(controller.fullScreenAdShowing.value)
        return turn!!
    }

    @Test
    fun rotation_resumedHost_recreated_turnKept_finishesOnClose() {
        start()
        val host = resumedActivity()
        val turn = beginTurnOn(host.get())

        host.recreate()
        idle()

        assertEquals(0, finishes)
        assertTrue(controller.fullScreenAdShowing.value)
        controller.onInterstitialClosed(AdPlacement.GALLERY_INTERSTITIAL, turn)
        assertEquals(1, finishes)
        assertFalse(controller.fullScreenAdShowing.value)
        idleFor(5_000)
        assertEquals(1, finishes)
    }

    @Test
    fun rotation_coveredHost_recreated_turnKept_finishesOnClose() {
        start()
        val host = resumedActivity()
        val turn = beginTurnOn(host.get())
        // Covered by the ad's activity, then recreated.
        host.pause().stop()

        host.recreate()
        idle()

        assertEquals(0, finishes)
        assertTrue(controller.fullScreenAdShowing.value)
        assertTrue(logger.entries.any { it.message.contains("host recreated, turn kept") })
        assertFalse(logger.entries.any { it.message.contains("stuck_recovered") })
        controller.onInterstitialClosed(AdPlacement.GALLERY_INTERSTITIAL, turn)
        assertEquals(1, finishes)
        assertFalse(controller.fullScreenAdShowing.value)
        idleFor(5_000)
        assertEquals(1, finishes)
    }

    @Test
    fun rotation_closeLost_newHostCoveredThenBack_recoversOnce() {
        start()
        val host = resumedActivity()
        val turn = beginTurnOn(host.get())
        host.pause().stop()
        host.recreate()
        idle()
        // Control: the turn is still open after the recreation.
        assertEquals(0, finishes)

        // The new activity was covered once (its pause in recreate), and comes back: the turn is stuck (F-03).
        host.start().resume()
        idle()

        assertEquals(1, finishes)
        assertFalse(controller.fullScreenAdShowing.value)
        assertTrue(logger.entries.any { it.message == "Full-screen turn finished: stuck_recovered" })
        controller.onInterstitialClosed(AdPlacement.GALLERY_INTERSTITIAL, turn)
        assertEquals(1, finishes)
    }

    @Test
    fun handover_closeDuringHandover_nextTurnForcedOnce() {
        val a1 = Robolectric.buildActivity(ChangingActivity::class.java).setup().get().apply { changing = true }
        val a2 = Robolectric.buildActivity(ChangingActivity::class.java).setup().get()
        val a3 = Robolectric.buildActivity(ChangingActivity::class.java).setup().get()
        var firstFinishes = 0
        var secondFinishes = 0
        val first = controller.beginInterstitialTurn(
            AdPlacement.GALLERY_INTERSTITIAL,
            System.identityHashCode(a1),
            combo = false,
        ) { firstFinishes++ }!!

        // Destroyed for a configuration change: the turn waits for the new activity.
        controller.onAppActivityDestroyed(a1)
        assertEquals(0, firstFinishes)
        assertTrue(controller.fullScreenAdShowing.value)
        // The ad closes before any new activity is resumed.
        controller.onInterstitialClosed(AdPlacement.GALLERY_INTERSTITIAL, first)
        assertEquals(1, firstFinishes)

        controller.beginInterstitialTurn(
            AdPlacement.GALLERY_INTERSTITIAL,
            System.identityHashCode(a2),
            combo = false,
        ) { secondFinishes++ }!!
        // Another activity resumed does not take the second turn: the wait of the first one is gone.
        controller.onAppActivityResumed(a3)
        controller.onAppActivityDestroyed(a2)

        assertEquals(1, secondFinishes)
        assertFalse(controller.fullScreenAdShowing.value)
        assertEquals(1, firstFinishes)
    }

    @Test
    fun realDestroy_coveredHost_forcesCompletion() {
        start()
        val host = resumedActivity()
        beginTurnOn(host.get())
        host.pause().stop()
        host.get().finish()

        host.destroy()

        assertEquals(1, finishes)
        assertFalse(controller.fullScreenAdShowing.value)
        assertTrue(logger.entries.any { it.message == "Full-screen turn finished: host_destroyed" })
    }

    @Test
    fun rotation_rewardedTurn_resumedHost_kept() {
        start()
        val fake = FakeRewardedAd()
        controller.putLoadedRewardedForTest(AdPlacement.GALLERY_RW_ACTION, fake)
        val host = resumedActivity()
        val outcomes = mutableListOf<RewardedOutcome>()
        controller.showRewarded(host.get(), AdPlacement.GALLERY_RW_ACTION) { outcomes += it }
        idle()
        assertEquals(1, fake.shows)
        assertTrue(controller.fullScreenAdShowing.value)

        host.recreate()
        idle()

        assertTrue(controller.fullScreenAdShowing.value)
        assertTrue(outcomes.isEmpty())
        fake.earnReward()
        assertEquals(listOf(RewardedOutcome.Granted), outcomes)
        fake.callback!!.onAdDismissedFullScreenContent()
        assertFalse(controller.fullScreenAdShowing.value)
        assertEquals(listOf(RewardedOutcome.Granted), outcomes)
    }

    @Test
    fun rotation_rewardedTurn_coveredHost_rewardKept() {
        start()
        val fake = FakeRewardedAd()
        controller.putLoadedRewardedForTest(AdPlacement.GALLERY_RW_ACTION, fake)
        val host = resumedActivity()
        val outcomes = mutableListOf<RewardedOutcome>()
        controller.showRewarded(host.get(), AdPlacement.GALLERY_RW_ACTION) { outcomes += it }
        idle()
        assertEquals(1, fake.shows)
        fake.callback!!.onAdShowedFullScreenContent()
        assertEquals(listOf(RewardedOutcome.OnScreen), outcomes)

        // Covered by the ad's activity, then recreated (the order seen most on devices).
        host.pause().stop()
        host.recreate()
        idle()

        assertTrue(controller.fullScreenAdShowing.value)
        assertFalse(logger.entries.any { it.message.contains("stuck") })
        fake.earnReward()
        assertEquals(listOf(RewardedOutcome.OnScreen, RewardedOutcome.Granted), outcomes)
        fake.callback!!.onAdDismissedFullScreenContent()
        assertFalse(controller.fullScreenAdShowing.value)
        host.start().resume()
        idle()
        assertEquals(listOf(RewardedOutcome.OnScreen, RewardedOutcome.Granted), outcomes)
    }
}

/** An activity whose [isChangingConfigurations] the test sets, for a destruction by a configuration change. */
class ChangingActivity : ComponentActivity() {
    var changing = false

    override fun isChangingConfigurations(): Boolean = changing
}
