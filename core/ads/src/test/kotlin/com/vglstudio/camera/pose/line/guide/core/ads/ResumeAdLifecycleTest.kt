package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Activity
import android.app.Application
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fakesdk.FakeSdkActivity
import com.vglstudio.camera.pose.line.guide.core.ads.ResumeAdRulesTest.Companion.ALL_PASS
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

/**
 * The resume ad through the real lifecycle (plan T-025): the real [AdsActivityTracker], [ResumeAdTurns],
 * [ResumeSuppression] and [ActivityResumeSteps], tied as in AdsController, with a fake port. Back from background is
 * a turn; the first launch, a rotation and a full-screen activity of the SDK covering the app are not.
 */
@RunWith(AndroidJUnit4::class)
class ResumeAdLifecycleTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val logger = RecordingAppLogger()
    private val suppression = ResumeSuppression(ForegroundAdSuppressor(), logger)
    private val port = FakeResumeAdPort(gate = ALL_PASS, liveSuppressed = { suppression.resumeAdsSuppressed.value })
    private val turns = ResumeAdTurns(port, logger)
    private val steps = ActivityResumeSteps(
        recoverStuck = {},
        regatherConsent = {},
        showResumeAd = { turns.onAppActivityResumed(it) },
        releaseLeaveMark = { suppression.onActivityResumed() },
    )

    // ComponentActivity of the test manifest is not in the package of the test, so the filter rejects the SDK only.
    private val tracker = AdsActivityTracker(
        AppActivityFilter { it !is FakeSdkActivity },
        object : AdsActivityTracker.Listener {
            override fun onProcessForegroundChanged(foreground: Boolean) = turns.onProcessForegroundChanged(foreground)

            override fun onActivityResumed(activity: Activity) = steps.onResumed(activity)
        },
    ).also { application.registerActivityLifecycleCallbacks(it) }

    @After
    fun unregister() {
        application.unregisterActivityLifecycleCallbacks(tracker)
    }

    private fun ActivityScenario<ComponentActivity>.activity(): Activity {
        var activity: Activity? = null
        onActivity { activity = it }
        return activity!!
    }

    private fun ActivityScenario<ComponentActivity>.backgroundAndBack() {
        moveToState(Lifecycle.State.CREATED)
        moveToState(Lifecycle.State.RESUMED)
    }

    @Test
    fun launch_isFirstForeground_noShow() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            assertEquals(1, port.gateReads)
            assertEquals(0, port.shows)
            // Control (L-055): back from background, the ad shows once.
            scenario.backgroundAndBack()
            assertEquals(1, port.shows)
        }
    }

    @Test
    fun backgroundThenForeground_showsOnResumedActivity() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.backgroundAndBack()
            assertEquals(1, port.appOpenShows.size)
            assertSame(scenario.activity(), port.appOpenShows.single().first)
            assertEquals(AdPlacement.RESUME, port.appOpenShows.single().second)
        }
    }

    @Test
    fun recreate_showsNothing() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            port.gateReads = 0
            scenario.recreate()
            scenario.recreate()
            assertEquals(0, port.gateReads)
            assertEquals(0, port.shows)
            // Control: a real trip to background and back still shows once.
            scenario.backgroundAndBack()
            assertEquals(1, port.gateReads)
            assertEquals(1, port.shows)
        }
    }

    @Test
    fun adClick_skipsNextReturn_thenNextReturnShows() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            suppression.markAdClicked()
            scenario.backgroundAndBack()
            assertEquals(0, port.shows)
            assertEquals(listOf(ResumeOpportunity(AdPlacement.RESUME, "suppressed", "idle")), port.opportunities)
            // The mark was released by that resume: the next return shows once.
            scenario.backgroundAndBack()
            assertEquals(1, port.shows)
            assertEquals(1, port.opportunities.size)
            assertTrue(logger.entries.any { it.message == "Resume ads held: ad_click" })
        }
    }

    @Test
    fun sdkActivityCoversApp_thenAppReturns_noTurn() {
        val app = Robolectric.buildActivity(AppTestActivity::class.java).setup()
        port.gateReads = 0
        // A full-screen ad of the SDK opens over the app, then closes: the app activity starts again before the
        // SDK activity stops, as on a device.
        val sdk = Robolectric.buildActivity(FakeSdkActivity::class.java).setup()
        app.pause().stop()
        sdk.pause()
        // restart(), not start(): after start() alone Robolectric skips the next stop of the activity.
        app.restart().resume()
        sdk.stop().destroy()
        assertEquals(0, port.gateReads)
        assertEquals(0, port.shows)
        // Control in the same test: Home for real, then back, is one turn shown once.
        app.pause().stop()
        app.restart().resume()
        assertEquals(1, port.gateReads)
        assertEquals(1, port.shows)
        assertSame(app.get(), port.appOpenShows.single().first)
    }
}
