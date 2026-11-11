package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.activity.ComponentActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.analytics.AdMetricEvent
import com.vglstudio.camera.pose.line.guide.core.analytics.RecordingAdFunnel
import com.vglstudio.camera.pose.line.guide.core.analytics.RecordingAdMetrics
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The banner of AppLovin MAX (plan T-071, KB-31): built lazily on its first send (G2), callbacks as results, no auto
 * refresh of MAX (plan T-088), load metrics and clicks through [AdTelemetry], no ad unit in a log line; and the
 * real `MaxAdView` built in the JVM.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class MaxBannerAdViewTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val logger = RecordingAppLogger()
    private var metrics = RecordingAdMetrics()
    private val counter = LoadAttemptCounter()
    private var logSlot = true
    private var clicks = 0
    private var nowMillis = 0L
    private var builds = 0
    private val sdk = FakeMaxBannerSdkView()

    private fun telemetry() = AdTelemetry(
        metrics = metrics,
        funnel = RecordingAdFunnel(),
        lifecycleSwitches = { DEFAULT_FLAGS.copy(logSlotEvents = logSlot) },
        serving = { true },
        offline = { false },
        counter = counter,
        logger = logger,
        onClicked = { clicks++ },
    )

    private fun banner(
        placement: AdPlacement = AdPlacement.GALLERY_BANNER,
        format: AdFormat = placement.format,
        sdkView: FakeMaxBannerSdkView = sdk,
        loadErrorLog: LoadErrorLog = LoadErrorLog.CODE_ONLY,
    ): MaxBannerAdView {
        // The ad unit lives in the lambda only, as in BannerSlot: the wrapper never gets it.
        val unitId = UNIT
        return MaxBannerAdView(
            newSdkView = {
                builds++
                check(unitId.isNotEmpty())
                sdkView.also { it.calls += FakeMaxBannerSdkView.BUILD }
            },
            placement = placement,
            format = format,
            telemetry = telemetry(),
            now = { nowMillis },
            logger = logger,
            loadErrorLog = loadErrorLog,
        )
    }

    private fun TestScope.collect(view: BannerAdView): MutableList<BannerLoadResult> {
        val got = mutableListOf<BannerLoadResult>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { view.results.collect { got += it } }
        return got
    }

    @Test
    fun callbacks_becomeResults() = runTest {
        val view = banner()
        val got = collect(view)
        view.send()
        sdk.emitLoaded()
        runCurrent()
        sdk.emitFailed(204)
        runCurrent()
        assertEquals(listOf(BannerLoadResult.Loaded, BannerLoadResult.Failed(204)), got)
    }

    @Test
    fun pauseStops_resumeNeverStartsAutoRefresh() {
        // Plan T-088, section 3.5: MAX never refreshes by itself; running the frame again does not start it.
        val view = banner()
        view.send()
        val stops = sdk.stopCount
        view.pause()
        assertEquals(stops + 1, sdk.stopCount)
        view.resume()
        assertEquals(0, sdk.startCount)
        assertEquals(stops + 1, sdk.stopCount)
        // Neither sends a request.
        assertEquals(1, sdk.loadCount)
    }

    @Test
    fun send_callsLoadAd() {
        val view = banner()
        view.send()
        assertEquals(1, sdk.loadCount)
        view.send()
        assertEquals(2, sdk.loadCount)
    }

    @Test
    fun destroy_destroysSdkView() {
        val view = banner()
        view.send()
        assertEquals(0, sdk.destroyCount)
        view.destroy()
        assertEquals(1, sdk.destroyCount)
    }

    @Test
    fun logs_neverContainUnitId() = runTest {
        val view = banner()
        collect(view)
        view.send()
        sdk.emitFailed(204)
        view.send()
        sdk.emitLoaded()
        sdk.emitClicked()
        view.pause()
        view.resume()
        view.destroy()
        runCurrent()
        assertTrue(logger.entries.none { UNIT in it.message })
        // Control (L-055): the wrapper does log its loads, under the key of the slot.
        val messages = logger.entries.map { it.message }
        assertTrue(messages.toString(), "Banner gallery_banner MAX view created" in messages)
        assertTrue(messages.toString(), "Banner gallery_banner MAX loaded" in messages)
        assertTrue(messages.toString(), "Banner gallery_banner MAX failed (code 204)" in messages)
    }

    @Test
    fun failed_staging_lineHasCodeAndMaskedMessage() = runTest {
        // Plan T-093, KB-28: the masked message of MAX is in its failure line; the result keeps the code only
        // (question 4), so the retry line does not repeat it.
        val view = banner(loadErrorLog = LoadErrorLog(true))
        val got = collect(view)
        view.send()
        sdk.emitFailed(204, MAX_UNIT_MESSAGE)
        runCurrent()

        val messages = logger.entries.map { it.message }
        assertTrue(
            messages.toString(),
            "Banner gallery_banner MAX failed (code 204 \"No fill for <ad unit>\")" in messages,
        )
        assertTrue(messages.toString(), messages.none { "2d0ec3c0d7d4b5ad" in it })
        assertEquals(listOf<BannerLoadResult>(BannerLoadResult.Failed(204)), got)
    }

    @Test
    fun failed_default_lineCodeOnly() = runTest {
        val view = banner()
        collect(view)
        view.send()
        sdk.emitFailed(204, "No Fill")
        runCurrent()

        val messages = logger.entries.map { it.message }
        assertTrue(messages.toString(), "Banner gallery_banner MAX failed (code 204)" in messages)
        assertTrue(messages.toString(), messages.none { "No Fill" in it })
    }

    @Test
    fun sdkView_builtOnFirstSendOnly() {
        val view = banner()
        view.pause()
        view.resume()
        view.destroy()
        composeRule.setContent { view.Content(showAd = true, modifier = Modifier) }
        composeRule.waitForIdle()
        assertEquals(0, builds)
        assertTrue(sdk.calls.isEmpty())

        composeRule.runOnIdle { view.send() }
        composeRule.waitForIdle()
        assertEquals(1, builds)
        assertEquals(1, sdk.loadCount)
        composeRule.runOnIdle { view.send() }
        assertEquals(1, builds)
        assertEquals(2, sdk.loadCount)
        // Control: the build comes before the first loadAd.
        assertTrue(sdk.calls.indexOf(FakeMaxBannerSdkView.BUILD) < sdk.calls.indexOf(FakeMaxBannerSdkView.LOAD))

        composeRule.runOnIdle { view.destroy() }
        assertEquals(1, sdk.destroyCount)
    }

    @Test
    fun newView_stopsAutoRefreshAroundFirstLoad_pausedOrNot() {
        // Plan T-088, section 3.5: a MAX view never refreshes by itself, built while the frame is paused or running.
        val stopAroundLoad = listOf(
            FakeMaxBannerSdkView.BUILD,
            FakeMaxBannerSdkView.STOP,
            FakeMaxBannerSdkView.LOAD,
            FakeMaxBannerSdkView.STOP,
        )
        val view = banner()
        view.pause()
        view.send()
        assertEquals(stopAroundLoad, sdk.calls.filter { it != FakeMaxBannerSdkView.CALLBACKS })
        assertEquals(0, sdk.startCount)

        // Control: a view built while the frame runs stops the same way; its next send is one more load.
        val other = FakeMaxBannerSdkView()
        val running = banner(sdkView = other)
        running.send()
        assertEquals(stopAroundLoad, other.calls.filter { it != FakeMaxBannerSdkView.CALLBACKS })
        val loads = other.loadCount
        running.send()
        assertEquals(loads + 1, other.loadCount)
        assertEquals(0, other.startCount)
    }

    @Test
    fun results_reachMetrics_withMaxLabels() {
        listOf(
            Triple(AdPlacement.GALLERY_BANNER, AdFormat.BANNER, "BANNER"),
            Triple(AdPlacement.GALLERY_BANNER_MREC, AdFormat.BANNER_MREC, "BANNER_MREC"),
        ).forEach { (placement, format, code) ->
            metrics = RecordingAdMetrics()
            clicks = 0
            val fake = FakeMaxBannerSdkView()
            val view = banner(placement, format, fake)
            val key = placement.key

            nowMillis = 100
            view.send()
            nowMillis = 110
            fake.emitFailed(204)
            assertEquals(1, counter.failuresInARow("$key/max"))
            assertEquals(0, counter.failuresInARow(key))

            nowMillis = 200
            view.send()
            nowMillis = 230
            fake.emitLoaded()
            // A refresh of the SDK: no send before it, so no latency.
            nowMillis = 300
            fake.emitFailed(-1009)
            fake.emitClicked()

            assertEquals(
                code,
                listOf(
                    AdMetricEvent.LoadFail(code, key, 1, 10, "204", "no_fill"),
                    AdMetricEvent.LoadSuccess(code, key, 2, 30),
                    AdMetricEvent.LoadFail(code, key, 1, null, "-1009", "network"),
                    AdMetricEvent.Clicked(code, key),
                ),
                metrics.events,
            )
            assertEquals(1, clicks)
            view.destroy()
        }

        // Control: the slot group switched off logs no event, but the click still skips the next resume (A-14).
        logSlot = false
        metrics = RecordingAdMetrics()
        clicks = 0
        val fake = FakeMaxBannerSdkView()
        val view = banner(sdkView = fake)
        view.send()
        fake.emitFailed(204)
        fake.emitClicked()
        assertTrue(metrics.events.isEmpty())
        assertEquals(1, clicks)
    }

    @Test
    fun realView_buildsForBannerAndMrec_rejectsCollapsible() {
        val activity = composeRule.activity
        val banner = AppLovinMaxBannerView("trial-unit", BannerSizeKind.ANCHORED, 360, activity)
        val mrec = AppLovinMaxBannerView("trial-unit-mrec", BannerSizeKind.MEDIUM_RECTANGLE, 360, activity)
        assertThrows(IllegalArgumentException::class.java) {
            AppLovinMaxBannerView("trial-unit", BannerSizeKind.LARGE_ANCHORED, 360, activity)
        }
        assertEquals(56, maxBannerHeightDp(activity, BannerSizeKind.ANCHORED, 360))
        assertEquals(250, maxBannerHeightDp(activity, BannerSizeKind.MEDIUM_RECTANGLE, 360))
        // Pause and resume of MAX work on a view that never loaded.
        banner.setCallbacks({}, { _, _ -> }, {})
        banner.stopAutoRefresh()
        banner.startAutoRefresh()
        assertFalse(banner.view === mrec.view)
        banner.destroy()
        mrec.destroy()
    }

    private companion object {
        const val UNIT = "unit-secret"

        val DEFAULT_FLAGS: AdsGlobalFlags = runBlocking { DefaultAdsFlags(FakeRemoteSettings()).global.first() }
    }
}
