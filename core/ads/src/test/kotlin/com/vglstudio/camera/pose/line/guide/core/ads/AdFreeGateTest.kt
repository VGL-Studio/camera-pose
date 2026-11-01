package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Activity
import android.app.Application
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.vglstudio.camera.pose.line.guide.core.analytics.AdMetricEvent
import com.vglstudio.camera.pose.line.guide.core.analytics.RecordingAdMetrics
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import java.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

/**
 * The ad-free signal of the app ([AdFreeStatus], KB-30, plan T-063) through the real [AdsController] with no SDK
 * (L-072): every switch of the controller, the rewarded switch, the in-list rows, the full-screen gate and the startup
 * ad follow it, mid session too (L-061); consent and AppsFlyer do not (question 50 of the ads spec).
 */
@RunWith(AndroidJUnit4::class)
class AdFreeGateTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val metrics = RecordingAdMetrics()
    private val logger = RecordingAppLogger()

    private fun adFreeStatus(flow: StateFlow<Boolean>): AdFreeStatus = object : AdFreeStatus {
        override val adFree = flow
    }

    private fun controller(settings: FakeRemoteSettings, adFree: StateFlow<Boolean>) = testAdsController(
        context = application,
        unitIds = AdUnitIds { null },
        flags = DefaultAdsFlags(settings, adFreeStatus(adFree)),
        metrics = metrics,
        logger = logger,
    )

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun idleFor(millis: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))

    @Test
    fun adFreeMidSession_everyPlacementSwitchOff() {
        val checked = mutableSetOf<AdPlacement>()
        // Two rounds, so each app-wide placement is chosen once: app open and resume ad, then their interstitials.
        listOf("aoa", "inter").forEach { type ->
            val settings = FakeRemoteSettings().apply {
                AdPlacement.entries.filterNot { it.appWide }
                    .forEach { set(AdsSettings.placementEnabled(it), true) }
                set(AdsSettings.SHOW_OPEN_ADS, true)
                set(AdsSettings.SHOW_OPEN_ADS_FIRST_OPEN, true)
                set(AdsSettings.SHOW_RESUM_ADS, true)
                set(AdsSettings.IS_SHOW_NATIVE_AFTER_INTER, true)
                set(AdsSettings.OPEN_AD_TYPE, type)
                set(AdsSettings.RESUME_AD_TYPE, type)
            }
            val adFree = MutableStateFlow(false)
            val controller = controller(settings, adFree)
            val on = AdPlacement.entries.filter { runBlocking { controller.isEnabled(it).first() } }
            adFree.value = true
            on.forEach { placement ->
                assertEquals("$type ${placement.key}", false, runBlocking { controller.isEnabled(placement).first() })
            }
            checked += on
        }
        // Control: every placement was on in a round and checked, a placement added later too.
        assertEquals((AdPlacement.entries - checked).toString(), AdPlacement.entries.size, checked.size)
    }

    @Test
    fun adFreeMidSession_rewardedSwitchOff() {
        val settings = FakeRemoteSettings().apply {
            set(AdsSettings.placementEnabled(AdPlacement.GALLERY_RW_ACTION), true)
        }
        val adFree = MutableStateFlow(false)
        val controller = controller(settings, adFree)
        controller.openConsentGateForTest()
        idle()
        assertEquals(true, runBlocking { controller.rewardedSwitch(AdPlacement.GALLERY_RW_ACTION).first() })
        adFree.value = true
        assertEquals(false, runBlocking { controller.rewardedSwitch(AdPlacement.GALLERY_RW_ACTION).first() })
    }

    @Test
    fun adFreeMidSession_inListEveryNull() {
        val settings = FakeRemoteSettings().apply {
            set(AdsSettings.placementEnabled(AdPlacement.GALLERY_INLIST_SMALL), true)
        }
        val adFree = MutableStateFlow(false)
        val controller = controller(settings, adFree)
        controller.openConsentGateForTest()
        idle()
        runBlocking {
            controller.inListEvery(AdPlacement.GALLERY_INLIST_SMALL).test {
                assertEquals(8, awaitItem())
                adFree.value = true
                assertNull(awaitItem())
                adFree.value = false
                assertEquals(8, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun adFree_fullScreenTurn_disabled_noOpportunity() {
        val activity: Activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        listOf(AdPlacement.GALLERY_INTERSTITIAL, AdPlacement.GALLERY_NATIVE_FULL_SCREEN).forEach { placement ->
            // Signal on: the gate says DISABLED, nothing shows, no opportunity.
            val off = fullScreenTurn(activity, placement, adFree = true)
            assertEquals(placement.key, FullScreenShowDecision.DISABLED, off.decision)
            assertEquals(placement.key, false, off.shown)
            assertEquals(placement.key, 1, off.finishes)
            assertTrue(placement.key, off.opportunities.isEmpty())
            assertTrue(
                "${logger.entries}",
                logger.entries.any { it.message == "Full-screen ${placement.key} skipped: DISABLED" },
            )
            // Control (L-055): signal off, the same turn reaches the ad check (no SDK) and records an opportunity.
            val on = fullScreenTurn(activity, placement, adFree = false)
            assertEquals(placement.key, FullScreenShowDecision.NOT_READY, on.decision)
            assertEquals(placement.key, false, on.shown)
            assertEquals(placement.key, 1, on.finishes)
            assertEquals(placement.key, listOf("not_ready"), on.opportunities.map { it.result })
        }
    }

    private class TurnResult(
        val decision: FullScreenShowDecision?,
        val shown: Boolean?,
        val finishes: Int,
        val opportunities: List<AdMetricEvent.Opportunity>,
    )

    /** One full-screen turn of [placement] on a fresh, started controller with the gate open. */
    private fun fullScreenTurn(activity: Activity, placement: AdPlacement, adFree: Boolean): TurnResult {
        val settings = FakeRemoteSettings().apply {
            set(AdsSettings.IS_LOG_AD_FULLSCREEN_EVENTS, true)
            set(AdsSettings.placementEnabled(placement), true)
        }
        val before = metrics.events.size
        val controller = controller(settings, MutableStateFlow(adFree))
        controller.start(application, AppActivityFilter { true })
        idle()
        controller.openConsentGateForTest()
        idle()
        var decision: FullScreenShowDecision? = null
        var shown: Boolean? = null
        var finishes = 0
        CoroutineScope(Dispatchers.Main.immediate).launch {
            decision = controller.decideFullScreenTurn(placement)
            shown = controller.showFullScreenAd(activity, placement) { finishes++ }
        }
        idle()
        val opportunities = metrics.events.drop(before).filterIsInstance<AdMetricEvent.Opportunity>()
        return TurnResult(decision, shown, finishes, opportunities)
    }

    @Test
    fun adFree_awaitStartupAd_nullAtOnce() {
        // KB-30 and KB-13, criterion 5: the loading screen does not wait for an app open ad.
        val adFreeRun = startupWait(adFree = true)
        assertTrue(adFreeRun.done)
        assertNull(adFreeRun.result)
        assertTrue(metrics.events.none { it is AdMetricEvent.Opportunity })
        // Control (L-055): signal off, the wait of A-03 still runs (no consent, no SDK), so the call is not always
        // over at once.
        val notAdFree = startupWait(adFree = false)
        assertFalse(notAdFree.done)
    }

    private class StartupWait(val done: Boolean, val result: StartupAdTurn?)

    private fun startupWait(adFree: Boolean): StartupWait {
        val settings = FakeRemoteSettings().apply {
            set(AdsSettings.SHOW_OPEN_ADS_FIRST_OPEN, true)
            set(AdsSettings.SHOW_OPEN_ADS, true)
            set(AdsSettings.IS_LOG_AD_FULLSCREEN_EVENTS, true)
        }
        val controller = controller(settings, MutableStateFlow(adFree))
        controller.start(application, AppActivityFilter { true })
        idle()
        var done = false
        var result: StartupAdTurn? = null
        CoroutineScope(Dispatchers.Main).launch {
            result = controller.awaitStartupAd()
            done = true
        }
        idle()
        return StartupWait(done, result)
    }

    @Test
    fun adFree_consentSdkAndAppsFlyerUnchanged() {
        // KB-30, "Thêm 2026-10-30" (question 50): a user who bought ad removal keeps consent, the SDK and AppsFlyer
        // as before; only ads are off. Control (L-055): the same steps with the signal off differ only in isEnabled.
        val adFreeRun = consentAndAttribution(adFree = true)
        val notAdFree = consentAndAttribution(adFree = false)
        assertEquals(listOf(0, 1, 1), adFreeRun.starts)
        assertEquals(notAdFree.starts, adFreeRun.starts)
        assertEquals(0, adFreeRun.prepares)
        assertEquals(notAdFree.prepares, adFreeRun.prepares)
        assertTrue(adFreeRun.canRequestAds)
        assertEquals(notAdFree.canRequestAds, adFreeRun.canRequestAds)
        assertFalse(adFreeRun.enabled)
        assertTrue(notAdFree.enabled)
    }

    private class ConsentRun(val starts: List<Int>, val prepares: Int, val canRequestAds: Boolean, val enabled: Boolean)

    /** As FullScreenAdsTest.start_startsAppsFlyerAt10s_notBefore, then the consent gate opened. */
    private fun consentAndAttribution(adFree: Boolean): ConsentRun {
        val settings = FakeRemoteSettings().apply {
            set(AdsSettings.placementEnabled(AdPlacement.SAMPLE_NATIVE_SMALL), true)
        }
        val attribution = FakeAppsFlyerStartup()
        val controller = testAdsController(
            context = application,
            attribution = attribution,
            flags = DefaultAdsFlags(settings, adFreeStatus(MutableStateFlow(adFree))),
            logger = logger,
        )
        // No activity: no consent form opens and the gate stays closed, so only the 10 s path starts AppsFlyer.
        controller.start(application, AppActivityFilter.inPackageOf(ComponentActivity::class.java))
        val starts = mutableListOf<Int>()
        idleFor(9_999)
        starts += attribution.starts
        idleFor(1)
        starts += attribution.starts
        idleFor(20_000)
        starts += attribution.starts
        // The "allowed" result of the gatherConsent path.
        controller.openConsentGateForTest()
        idle()
        return ConsentRun(
            starts = starts,
            prepares = attribution.prepares,
            canRequestAds = controller.canRequestAds.value,
            enabled = runBlocking { controller.isEnabled(AdPlacement.SAMPLE_NATIVE_SMALL).first() },
        )
    }
}
