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
 * The banner of AppLovin MAX (plan T-071, KB-31): built lazily on its first send (G2), callbacks as results, pause and
 * resume as the auto refresh of MAX, load metrics and clicks through [AdTelemetry], no ad unit in a log line; and the
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
    fun pauseResume_mapToAutoRefresh() {
        val view = banner()
        view.send()
        view.pause()
        assertEquals(1, sdk.stopCount)
        assertEquals(0, sdk.startCount)
        view.resume()
        assertEquals(1, sdk.startCount)
        assertEquals(1, sdk.stopCount)
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
    fun pauseBeforeBuild_stopsRightAfterBuild() {
        val view = banner()
        view.pause()
        view.send()
        assertEquals(
            listOf(FakeMaxBannerSdkView.BUILD, FakeMaxBannerSdkView.LOAD, FakeMaxBannerSdkView.STOP),
            sdk.calls.filter { it != FakeMaxBannerSdkView.CALLBACKS },
        )

        // Control: with no pause before, the build is followed by loadAd only.
        val other = FakeMaxBannerSdkView()
        banner(sdkView = other).send()
        assertEquals(
            listOf(FakeMaxBannerSdkView.BUILD, FakeMaxBannerSdkView.LOAD),
            other.calls.filter { it != FakeMaxBannerSdkView.CALLBACKS },
        )
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
        banner.setCallbacks({}, {}, {})
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
