package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Activity
import android.app.Application
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import java.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

/**
 * Which interstitial turns open the native combo in this app (spec ads, question 18; ADR-1009; plan T-090), through
 * the real [AdsController] with `is_show_native_after_inter` on and a fake interstitial put in its slot, on each of
 * the three ways an interstitial turn opens (L-072): [AdsController.showInterstitial] of a feature,
 * [AdsController.showStartupAd] and the resume ad of [ResumeAdTurns].
 *
 * - The interstitials of the screens (`review_interstitial`, `inter_pose_detail`) open the combo, and their
 *   completion runs only when the combo closes.
 * - `resume_inter` (the path of FB-20261102-01) and `open_app_inter` never open the combo: the completion runs at
 *   once, nothing holds the showing flag or the resume block.
 * - The showing flag has no gap between the interstitial and the combo (`features/network/spec.md`, section 5).
 */
@RunWith(AndroidJUnit4::class)
class AdsControllerComboScopeTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val settings = FakeRemoteSettings().apply { set(AdsSettings.IS_SHOW_NATIVE_AFTER_INTER, true) }

    private fun controller(
        logger: RecordingAppLogger,
        firstOpenMark: FirstOpenMark = FirstOpenMark(EmptyPreferencesForTests(), RecordingAppLogger()),
    ): AdsController = testAdsController(
        context = application,
        // No ad unit: nothing is ever loaded; the ads are put in their slot by the tests, and the combo load fails.
        unitIds = AdUnitIds { null },
        flags = DefaultAdsFlags(settings),
        logger = logger,
        firstOpenMark = firstOpenMark,
    ).also {
        // A FullScreenAdHost is in a composition, as with the shell of the app (K-09 of the template, HD-05).
        it.attachFullScreenHost()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun idleFor(millis: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))

    /** Starts [controller] (activity tracker) and opens the consent gate, as on a device. */
    private fun start(controller: AdsController) {
        controller.start(application, AppActivityFilter { true })
        idle()
        controller.openConsentGateForTest()
        idle()
        assertTrue(controller.canRequestAds.value)
    }

    /** An activity of the app, built after [start] so the tracker sees it resumed. */
    private fun resumedActivity(): ActivityController<ComponentActivity> =
        Robolectric.buildActivity(ComponentActivity::class.java).setup()

    private fun comboOpenedLogs(logger: RecordingAppLogger): List<String> =
        logger.entries.map { it.message }.filter { it.startsWith("Native combo opened") }

    /** [AdsController.showInterstitial] of a feature on the main thread, as the feature's composable does. */
    private fun showInterstitial(
        controller: AdsController,
        activity: Activity,
        placement: AdPlacement,
        onFinished: () -> Unit,
    ): Boolean? {
        var result: Boolean? = null
        CoroutineScope(Dispatchers.Main).launch {
            result = controller.showInterstitial(activity, placement, onFinished)
        }
        idle()
        return result
    }

    @Test
    fun resumeInter_closed_noCombo() {
        settings.set(AdsSettings.SHOW_RESUM_ADS, true)
        settings.set(AdsSettings.RESUME_AD_TYPE, "inter")
        val logger = RecordingAppLogger()
        val controller = controller(logger)
        start(controller)
        // The 45 s cap of the startup turn over, so a return reaches the resume ad (AdsControllerResumeTest).
        idleFor(45_100)
        val fake = FakeInterstitialAd()
        controller.putLoadedInterstitialForTest(AdPlacement.RESUME_INTER, fake)
        val host = resumedActivity()
        assertEquals(0, fake.shows)

        // Out to background and back: the resume interstitial shows from onActivityResumed of the tracker.
        host.pause().stop()
        host.start().resume()
        idle()
        assertEquals(1, fake.shows)
        // Control: the turn went through the real path of the resume ad.
        assertTrue(logger.entries.any { it.message == "Resume ad SHOW: resume_inter" })
        assertTrue(logger.entries.any { it.message == "Interstitial shown: resume_inter" })
        assertTrue(controller.fullScreenAdShowing.value)

        fake.callback!!.onAdDismissedFullScreenContent()
        idleFor(2_100)

        // Spec ads, question 18: no combo after the resume interstitial, even with the key on (FB-20261102-01).
        assertEquals(emptyList<String>(), comboOpenedLogs(logger))
        assertNull(controller.nativeFullScreenView.value)
        assertFalse(controller.fullScreenAdShowing.value)
        // Nothing holds the block of the resume ads after the interstitial closed.
        assertFalse(controller.resumeAdsSuppressed.value)
        idleFor(5_000)
        assertNull(controller.nativeFullScreenView.value)
        assertFalse(controller.fullScreenAdShowing.value)
    }

    @Test
    fun screenInterstitial_closed_opensCombo_finishesOnClose() {
        listOf(AdPlacement.REVIEW_INTERSTITIAL, AdPlacement.INTER_POSE_DETAIL).forEach { placement ->
            val logger = RecordingAppLogger()
            settings.set(AdsSettings.placementEnabled(placement), true)
            val controller = controller(logger)
            start(controller)
            val fake = FakeInterstitialAd()
            controller.putLoadedInterstitialForTest(placement, fake)
            val activity = resumedActivity().get()
            var finishes = 0

            assertEquals(placement.key, true, showInterstitial(controller, activity, placement) { finishes++ })
            assertEquals(placement.key, 1, fake.shows)
            assertEquals(placement.key, 0, finishes)

            fake.callback!!.onAdDismissedFullScreenContent()
            idleFor(2_100)

            // The combo opened: the completion of the interstitial waits for it (K-01).
            assertNotNull(placement.key, controller.nativeFullScreenView.value)
            assertEquals(placement.key, 0, finishes)
            assertEquals(placement.key, 1, comboOpenedLogs(logger).size)
            assertTrue(placement.key, controller.fullScreenAdShowing.value)
            // No ad unit, so the combo load fails: the close button works at once (K-03).
            assertTrue(placement.key, controller.nativeFullScreenView.value!!.closeEnabled)

            controller.closeNativeFullScreen()
            assertEquals(placement.key, 1, finishes)
            assertNull(placement.key, controller.nativeFullScreenView.value)
            assertFalse(placement.key, controller.fullScreenAdShowing.value)
            assertTrue(placement.key, logger.entries.any { it.message == "Native combo closed (shown: false)" })
            idleFor(5_000)
            assertEquals(placement.key, 1, finishes)
        }
    }

    @Test
    fun startupInterstitial_shownAndClosed_noCombo() {
        // From the second open on (A-01): the first-open mark is already written.
        settings.set(AdsSettings.SHOW_OPEN_ADS, true)
        settings.set(AdsSettings.OPEN_AD_TYPE, "inter")
        val logger = RecordingAppLogger()
        val controller = controller(logger, FirstOpenMark(LaterOpenPreferences(), RecordingAppLogger()))
        start(controller)
        val fake = FakeInterstitialAd()
        controller.putLoadedInterstitialForTest(AdPlacement.OPEN_APP_INTER, fake)
        val activity = resumedActivity().get()
        val results = mutableListOf<Boolean>()

        controller.showStartupAd(activity, StartupAdTurn(AdPlacement.OPEN_APP_INTER)) { results += it }
        idle()
        assertEquals(1, fake.shows)
        assertTrue(results.isEmpty())
        assertTrue(controller.fullScreenAdShowing.value)

        fake.callback!!.onAdDismissedFullScreenContent()
        idle()

        // At once, with no combo: the startup interstitial never opens it (spec ads, question 18).
        assertEquals(listOf(true), results)
        assertFalse(controller.fullScreenAdShowing.value)
        idleFor(5_000)
        assertNull(controller.nativeFullScreenView.value)
        assertEquals(listOf(true), results)
        assertEquals(emptyList<String>(), comboOpenedLogs(logger))
        assertFalse(controller.resumeAdsSuppressed.value)
    }

    @Test
    fun screenInterstitial_showingFlag_noGapUntilComboCloses() {
        val placement = AdPlacement.REVIEW_INTERSTITIAL
        settings.set(AdsSettings.placementEnabled(placement), true)
        val logger = RecordingAppLogger()
        val controller = controller(logger)
        start(controller)
        val fake = FakeInterstitialAd()
        controller.putLoadedInterstitialForTest(placement, fake)
        val activity = resumedActivity().get()
        val flag = controller.fullScreenAdShowing
        val values = mutableListOf<Boolean>()
        // Collected with no queue, so every value the flag takes is seen at once on the main thread.
        val job = CoroutineScope(Dispatchers.Main.immediate).launch { flag.collect { values += it } }
        try {
            val steps = mutableListOf<Boolean>()
            steps += flag.value
            var finishes = 0
            assertEquals(true, showInterstitial(controller, activity, placement) { finishes++ })
            assertEquals(1, fake.shows)
            steps += flag.value
            fake.callback!!.onAdDismissedFullScreenContent()
            // On the same stack as the close: the combo already holds the flag.
            steps += flag.value
            idleFor(2_100)
            assertNotNull(controller.nativeFullScreenView.value)
            steps += flag.value
            controller.closeNativeFullScreen()
            steps += flag.value
            assertEquals(1, finishes)

            assertEquals(listOf(false, true, true, true, false), steps)
            // No false between the interstitial and the combo: the network popup never slips in between.
            assertEquals(listOf(false, true, false), values)
        } finally {
            job.cancel()
        }
    }
}

/** A preferences store of an app opened before: the first-open mark is written, every write is dropped. */
private class LaterOpenPreferences : DataStore<Preferences> {
    override val data: Flow<Preferences> = flowOf(preferencesOf(booleanPreferencesKey("ads_first_open_done") to true))

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        transform(data.first())
}
