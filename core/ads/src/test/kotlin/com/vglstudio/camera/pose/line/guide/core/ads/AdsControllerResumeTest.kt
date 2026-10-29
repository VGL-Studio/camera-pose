package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Application
import android.os.Looper
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/**
 * The real [AdsController] tied to the resume ad (plan T-025, L-072): an ad click raises the resume block, the gate
 * reads the controller's own state, and a switched-off resume records nothing. No SDK ad is needed.
 */
@RunWith(AndroidJUnit4::class)
class AdsControllerResumeTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val settings = FakeRemoteSettings()
    private val metrics = RecordingAdMetrics()
    private val logger = RecordingAppLogger()

    private fun controller() = testAdsController(
        context = application,
        unitIds = AdUnitIds { null },
        flags = DefaultAdsFlags(settings),
        metrics = metrics,
        logger = logger,
    )

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun idleFor(millis: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))

    private fun startedController(): AdsController = controller().also {
        it.start(application, AppActivityFilter { true })
        idle()
    }

    @Test
    fun adClick_raisesResumeSuppression() {
        val controller = startedController()
        assertFalse(controller.resumeAdsSuppressed.value)
        // Any format, even with the lifecycle metrics off and nothing served.
        controller.telemetry.clicked(AdFormat.BANNER, null)
        assertTrue(controller.resumeAdsSuppressed.value)
        // Control: the next resume of an app activity releases it.
        controller.resumeSuppression.onActivityResumed()
        assertFalse(controller.resumeAdsSuppressed.value)
    }

    @Test
    fun resumeOff_gateHasNoPlacement_foregroundRecordsNothing() {
        // The lifecycle metrics on, else no opportunity is ever recorded and the check proves nothing.
        settings.set(AdsSettings.IS_LOG_AD_FULLSCREEN_EVENTS, true)
        val controller = startedController()
        controller.openConsentGateForTest()
        // The 45 s cap of the startup turn over, so a return reaches the OFF gate, not STARTUP.
        idleFor(45_100)
        val gate = controller.readResumeGate()
        assertTrue(gate.toString(), gate.serving)
        assertTrue(gate.toString(), gate.startupFinished)
        assertNull(gate.placement)
        controller.onProcessForegroundChanged(true)
        controller.onProcessForegroundChanged(false)
        controller.onProcessForegroundChanged(true)
        idle()
        assertTrue(metrics.events.none { it is AdMetricEvent.Opportunity })
        val resumeLogs = logger.entries.map { it.message }.filter { it.startsWith("Resume ad ") }
        assertEquals(listOf("Resume ad FIRST_FOREGROUND: none", "Resume ad OFF: none"), resumeLogs)
        // Control (L-055): switched on, the next return is an opportunity of the resume placement.
        settings.set(AdsSettings.SHOW_RESUM_ADS, true)
        idle()
        assertEquals(AdPlacement.RESUME, controller.readResumeGate().placement)
        controller.onProcessForegroundChanged(false)
        controller.onProcessForegroundChanged(true)
        idle()
        val opportunities = metrics.events.filterIsInstance<AdMetricEvent.Opportunity>()
        assertEquals(1, opportunities.size)
        assertEquals("resume", opportunities.single().adPlacement)
        assertEquals("not_ready", opportunities.single().result)
        assertEquals("idle", opportunities.single().loadState)
        assertEquals("APP_OPEN", opportunities.single().adFormat)
        settings.set(AdsSettings.RESUME_AD_TYPE, "inter")
        idle()
        assertEquals(AdPlacement.RESUME_INTER, controller.readResumeGate().placement)
    }

    @Test
    fun readResumeGate_mapsControllerState() {
        val controller = controller()
        // Before the keys are read.
        val before = controller.readResumeGate()
        assertNull(before.placement)
        assertFalse(before.serving)
        assertTrue(before.appOpenLockPassed)
        assertFalse(before.ready)
        assertFalse(before.busy)
        assertFalse(before.startupFinished)
        assertFalse(before.suppressed)
        controller.resumeSuppression.markAdClicked()
        assertTrue(controller.readResumeGate().suppressed)

        // The startup key is off: awaitStartupAd returns null and the startup turn is over.
        controller.start(application, AppActivityFilter { true })
        idle()
        assertFalse(controller.readResumeGate().startupFinished)
        var returned = false
        var turn: StartupAdTurn? = null
        CoroutineScope(Dispatchers.Main).launch {
            turn = controller.awaitStartupAd()
            returned = true
        }
        idle()
        assertTrue(returned)
        assertNull(turn)
        assertTrue(controller.readResumeGate().startupFinished)

        // Control: with no loading screen, the turn is over 45 s after start, not before.
        val other = startedController()
        idleFor(44_000)
        assertFalse(other.readResumeGate().startupFinished)
        idleFor(1_100)
        assertTrue(other.readResumeGate().startupFinished)
    }
}
