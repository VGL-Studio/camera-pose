package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.ads.AdPlatform.ADMOB
import com.vglstudio.camera.pose.line.guide.core.ads.AdPlatform.MAX
import com.vglstudio.camera.pose.line.guide.core.ads.BannerLoadResult.Failed
import com.vglstudio.camera.pose.line.guide.core.ads.BannerLoadResult.Loaded
import com.vglstudio.camera.pose.line.guide.core.ads.BannerLoadResult.TryNext
import com.vglstudio.camera.pose.line.guide.core.analytics.RecordingAdFunnel
import com.vglstudio.camera.pose.line.guide.core.analytics.RecordingAdMetrics
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The banner frame of two platforms (plan T-071, KB-31, ADR-0037): which platforms a slot asks, in which order, its
 * height and key, and [OrderedBannerAdView], which asks the first platform, then the second one in the same frame,
 * pauses the platforms not shown and goes through one send path (L-072).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlatformBannersTest {
    private val logger = RecordingAppLogger()
    private val max = FakeBannerAdView(tag = "max")
    private val admob = FakeBannerAdView(tag = "admob")
    private var ready = true

    private fun ordered(vararg children: Pair<AdPlatform, BannerAdView>, maxReady: () -> Boolean = { ready }) =
        OrderedBannerAdView(children.toList(), maxReady, logger, KEY)

    /** Collects the results of [view] from before its first send, as [runBannerLoads] does. */
    private fun TestScope.collect(view: BannerAdView): MutableList<BannerLoadResult> {
        val got = mutableListOf<BannerLoadResult>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { view.results.collect { got += it } }
        return got
    }

    @Test
    fun platforms_followOrder_dropMissingUnits_collapsibleAdmobOnly() {
        val units = listOf(null, "", "u")
        val formats = listOf(AdFormat.BANNER, AdFormat.BANNER_MREC, AdFormat.BANNER_COLLAPSIBLE)
        MediationOrder.entries.forEach { order ->
            formats.forEach { format ->
                units.forEach { admobUnit ->
                    units.forEach { maxUnit ->
                        val hasAdmob = !admobUnit.isNullOrBlank()
                        val hasMax = !maxUnit.isNullOrBlank()
                        val expected = if (format == AdFormat.BANNER_COLLAPSIBLE) {
                            if (hasAdmob) listOf(ADMOB) else emptyList()
                        } else {
                            order.platforms.filter { if (it == ADMOB) hasAdmob else hasMax }
                        }
                        assertEquals(
                            "$order $format admob=$admobUnit max=$maxUnit",
                            expected,
                            bannerPlatforms(format, order, admobUnit, maxUnit),
                        )
                    }
                }
            }
        }
        // The cases the plan names, written out.
        assertEquals(emptyList<AdPlatform>(), bannerPlatforms(AdFormat.BANNER, MediationOrder.MAX, "a", null))
        assertEquals(listOf(ADMOB), bannerPlatforms(AdFormat.BANNER, MediationOrder.MAX_ADMOB, "a", null))
        assertEquals(listOf(ADMOB), bannerPlatforms(AdFormat.BANNER_COLLAPSIBLE, MediationOrder.MAX, "a", "m"))
        // Control: both units keep the order of the remote key.
        assertEquals(listOf(MAX, ADMOB), bannerPlatforms(AdFormat.BANNER, MediationOrder.MAX_ADMOB, "a", "m"))
        assertEquals(listOf(ADMOB, MAX), bannerPlatforms(AdFormat.BANNER_MREC, MediationOrder.ADMOB_MAX, "a", "m"))
    }

    @Test
    fun firstFails_sendsSecond_showsSecond() = runTest {
        val view = ordered(MAX to max, ADMOB to admob)
        val got = collect(view)
        view.send()
        assertEquals(1, max.sendCount)
        assertEquals(0, admob.sendCount)

        max.emitFailed(204)
        runCurrent()
        assertEquals(listOf<BannerLoadResult>(TryNext), got)
        assertEquals(1, max.pauseCount)

        view.send()
        assertEquals(1, max.sendCount)
        assertEquals(1, admob.sendCount)
        admob.emitLoaded()
        runCurrent()
        assertEquals(listOf(TryNext, Loaded), got)
    }

    @Test
    fun shownAfterPaused_isResumed() = runTest {
        val view = ordered(MAX to max, ADMOB to admob)
        val got = collect(view)
        view.send()
        max.emitFailed(204)
        runCurrent()
        view.send()
        admob.emitLoaded()
        runCurrent()
        assertEquals(1, max.pauseCount)

        // A refresh of AdMob fails: the retry asks MAX again, which loads and becomes the platform shown.
        admob.emitFailed(3)
        runCurrent()
        view.send()
        max.emitLoaded()
        runCurrent()
        assertEquals(listOf(TryNext, Loaded, Failed(3), Loaded), got)
        assertEquals(1, max.resumeCount)
        assertTrue("MAX runs once shown", max.isRunning)

        // Control: the frame paused before MAX loads keeps MAX paused until the frame resumes.
        val max2 = FakeBannerAdView(tag = "max")
        val admob2 = FakeBannerAdView(tag = "admob")
        val view2 = ordered(MAX to max2, ADMOB to admob2)
        collect(view2)
        view2.send()
        max2.emitFailed(204)
        runCurrent()
        view2.send()
        admob2.emitLoaded()
        runCurrent()
        admob2.emitFailed(3)
        runCurrent()
        view2.send()
        view2.pause()
        val resumesBefore = max2.resumeCount
        max2.emitLoaded()
        runCurrent()
        assertEquals(resumesBefore, max2.resumeCount)
        assertFalse("paused frame: MAX stays paused", max2.isRunning)
        view2.resume()
        assertEquals(resumesBefore + 1, max2.resumeCount)
        assertTrue(max2.isRunning)
    }

    @Test
    fun singleChild_pausedWhileLoading_resumedThenLoaded_runs() = runTest {
        val view = ordered(ADMOB to admob)
        val got = collect(view)
        view.send()
        view.pause()
        assertFalse(admob.isRunning)
        view.resume()
        admob.emitLoaded()
        runCurrent()

        assertEquals(listOf<BannerLoadResult>(Loaded), got)
        assertTrue("the last call to AdMob is a resume", admob.isRunning)
        assertEquals(1, admob.pauseCount)
        assertEquals(1, admob.resumeCount)
    }

    @Test
    fun admobOnly_failure_doesNotPause_retrySendsOnRunningView() = runTest {
        val view = ordered(ADMOB to admob)
        val got = collect(view)
        view.send()
        admob.emitFailed(3)
        runCurrent()
        assertEquals(listOf<BannerLoadResult>(Failed(3)), got)
        assertEquals(0, admob.pauseCount)

        view.send()
        assertEquals(2, admob.sendCount)
        assertEquals(0, admob.pauseCount)
        assertEquals(0, admob.resumeCount)

        // Control: of two platforms, only the MAX one that failed is paused.
        val max2 = FakeBannerAdView(tag = "max")
        val admob2 = FakeBannerAdView(tag = "admob")
        val view2 = ordered(MAX to max2, ADMOB to admob2)
        collect(view2)
        view2.send()
        max2.emitFailed(204)
        runCurrent()
        assertEquals(1, max2.pauseCount)
        assertEquals(0, admob2.pauseCount)
    }

    @Test
    fun pendingResult_afterPause_isHandled() = runTest {
        val view = ordered(MAX to max, ADMOB to admob)
        val got = collect(view)
        view.send()
        max.emitFailed(204)
        runCurrent()
        view.send()
        view.pause()
        admob.emitLoaded()
        runCurrent()
        assertEquals(listOf(TryNext, Loaded), got)
        assertEquals(0, admob.resumeCount)
        assertFalse(admob.isRunning)
        view.resume()
        assertEquals(1, admob.resumeCount)
        assertTrue(admob.isRunning)

        // Control: AdMob fails after the pause instead: the last platform fails, the next send asks MAX first.
        val max2 = FakeBannerAdView(tag = "max")
        val admob2 = FakeBannerAdView(tag = "admob")
        val view2 = ordered(MAX to max2, ADMOB to admob2)
        val got2 = collect(view2)
        view2.send()
        max2.emitFailed(204)
        runCurrent()
        view2.send()
        view2.pause()
        admob2.emitFailed(3)
        runCurrent()
        assertEquals(listOf(TryNext, Failed(3)), got2)
        view2.send()
        assertEquals(2, max2.sendCount)
        assertEquals(1, admob2.sendCount)
    }

    @Test
    fun firstShownRefreshFails_isFailed_notTryNext() = runTest {
        val view = ordered(MAX to max, ADMOB to admob)
        val got = collect(view)
        view.send()
        max.emitLoaded()
        runCurrent()
        // A refresh of the SDK, with no send before it.
        max.emitFailed(204)
        runCurrent()
        assertEquals(listOf(Loaded, Failed(204)), got)

        // Control: after a send to MAX, the same failure moves to the next platform.
        view.send()
        assertEquals(2, max.sendCount)
        max.emitFailed(204)
        runCurrent()
        assertEquals(listOf(Loaded, Failed(204), TryNext), got)
    }

    @Test
    fun maxBuiltAfterPaused_isStoppedOnNextPause() = runTest {
        val sdk = FakeMaxBannerSdkView()
        val maxView = MaxBannerAdView(
            newSdkView = { sdk.also { it.calls += FakeMaxBannerSdkView.BUILD } },
            placement = AdPlacement.GALLERY_BANNER,
            format = AdFormat.BANNER,
            telemetry = telemetry(),
            now = { 0L },
            logger = logger,
        )
        ready = false
        val view = ordered(MAX to maxView, ADMOB to admob)
        val got = collect(view)
        view.send()
        runCurrent()
        view.send()
        admob.emitLoaded()
        runCurrent()
        assertEquals(listOf(TryNext, Loaded), got)
        assertTrue("no MAX view before MAX is ready", sdk.calls.isEmpty())

        ready = true
        admob.emitFailed(3)
        runCurrent()
        view.send()
        sdk.emitLoaded()
        runCurrent()
        assertEquals(listOf(TryNext, Loaded, Failed(3), Loaded), got)
        assertEquals(1, sdk.loadCount)
        assertEquals(0, sdk.stopCount)

        view.pause()
        assertEquals(1, sdk.stopCount)
        assertEquals(FakeMaxBannerSdkView.STOP, sdk.calls.last())
    }

    @Test
    fun lastPlatformFails_afterPause_isFailed() = runTest {
        val view = ordered(ADMOB to admob)
        val got = collect(view)
        view.send()
        view.pause()
        admob.emitFailed(3)
        runCurrent()
        assertEquals(listOf<BannerLoadResult>(Failed(3)), got)
        view.send()
        assertEquals(2, admob.sendCount)
        assertEquals(1, admob.resumeCount)
        assertTrue(admob.isRunning)

        // Control: the first of two platforms fails after the pause: TryNext, and the round starts again from MAX.
        val max2 = FakeBannerAdView(tag = "max")
        val admob2 = FakeBannerAdView(tag = "admob")
        val view2 = ordered(MAX to max2, ADMOB to admob2)
        val got2 = collect(view2)
        view2.send()
        view2.pause()
        max2.emitFailed(204)
        runCurrent()
        assertEquals(listOf<BannerLoadResult>(TryNext), got2)
        view2.send()
        assertEquals(2, max2.sendCount)
        assertEquals(0, admob2.sendCount)
    }

    @Test
    fun pauseAfterTryNext_resetsToFirst() = runTest {
        val view = ordered(MAX to max, ADMOB to admob)
        collect(view)
        view.send()
        max.emitFailed(204)
        runCurrent()
        view.pause()
        view.resume()
        view.send()
        assertEquals(2, max.sendCount)
        assertEquals(0, admob.sendCount)

        // Control: with no pause, the next send goes to AdMob.
        val max2 = FakeBannerAdView(tag = "max")
        val admob2 = FakeBannerAdView(tag = "admob")
        val view2 = ordered(MAX to max2, ADMOB to admob2)
        collect(view2)
        view2.send()
        max2.emitFailed(204)
        runCurrent()
        view2.send()
        assertEquals(1, max2.sendCount)
        assertEquals(1, admob2.sendCount)

        // The same after destroy.
        val max3 = FakeBannerAdView(tag = "max")
        val admob3 = FakeBannerAdView(tag = "admob")
        val view3 = ordered(MAX to max3, ADMOB to admob3)
        collect(view3)
        view3.send()
        max3.emitFailed(204)
        runCurrent()
        view3.destroy()
        view3.send()
        assertEquals(2, max3.sendCount)
        assertEquals(0, admob3.sendCount)
    }

    @Test
    fun bothFail_oneFailure_retryStartsFromFirst() = runTest {
        val view = ordered(MAX to max, ADMOB to admob)
        val got = collect(view)
        view.send()
        max.emitFailed(204)
        runCurrent()
        view.send()
        admob.emitFailed(3)
        runCurrent()
        assertEquals(listOf(TryNext, Failed(3)), got)

        view.send()
        assertEquals(2, max.sendCount)
        assertEquals(1, admob.sendCount)
    }

    @Test
    fun maxNotReady_skipsMaxWithoutSending() = runTest {
        ready = false
        val view = ordered(MAX to max, ADMOB to admob)
        val got = collect(view)
        view.send()
        runCurrent()
        assertEquals(listOf<BannerLoadResult>(TryNext), got)
        assertEquals(0, max.sendCount)
        assertTrue(logger.entries.any { it.message == "Banner $KEY MAX not ready" })

        // Control: MAX ready is asked.
        val max2 = FakeBannerAdView(tag = "max")
        val view2 = ordered(MAX to max2, ADMOB to FakeBannerAdView(), maxReady = { true })
        collect(view2)
        view2.send()
        assertEquals(1, max2.sendCount)

        // MAX only, not ready: one failure of the frame.
        val max3 = FakeBannerAdView(tag = "max")
        val view3 = ordered(MAX to max3, maxReady = { false })
        val got3 = collect(view3)
        view3.send()
        runCurrent()
        assertEquals(listOf<BannerLoadResult>(Failed(MAX_NOT_READY)), got3)
        assertEquals(-9000, MAX_NOT_READY)
        assertEquals(0, max3.sendCount)
    }

    // The three tests below run the real runBannerLoads on the standard dispatcher of runTest (plan T-071, version 3).

    private var loads = 0
    private var failures = 0

    private fun TestScope.runLoads(view: BannerAdView) {
        backgroundScope.launch {
            runBannerLoads(
                refresh = BannerRefresh.BY_AD_UNIT,
                view = view,
                visible = MutableStateFlow(true),
                mayRequestNow = { true },
                retryResets = MutableStateFlow(0),
                onLoaded = { loads++ },
                logger = logger,
                placementKey = KEY,
                onFailed = { failures++ },
            )
        }
    }

    @Test
    fun notReadyMax_throughRunBannerLoads_sendsAdmob() = runTest {
        runLoads(ordered(MAX to max, ADMOB to admob, maxReady = { false }))
        runCurrent()
        assertEquals(1, admob.sendCount)
        assertEquals(0, max.sendCount)
        admob.emitLoaded()
        runCurrent()
        assertEquals(1, loads)
        assertEquals(0, failures)
    }

    @Test
    fun maxOnlyNotReady_throughRunBannerLoads_isOneFailure() = runTest {
        runLoads(ordered(MAX to max, maxReady = { false }))
        runCurrent()
        assertEquals(1, failures)
        assertEquals(0, max.sendCount)
        assertTrue(logger.entries.any { "Banner $KEY retry in 2000 ms" in it.message })

        // Control: the retry 2 s later is one more failure.
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(2, failures)
        assertEquals(0, max.sendCount)
    }

    @Test
    fun failsInsideSend_throughRunBannerLoads_isOneFailure() = runTest {
        admob.onSend = { admob.emitFailed(3) }
        runLoads(ordered(ADMOB to admob))
        runCurrent()
        assertEquals(1, failures)

        // Control: a load given inside send is heard too.
        val admob2 = FakeBannerAdView(tag = "admob")
        admob2.onSend = { admob2.emitLoaded() }
        runLoads(ordered(ADMOB to admob2))
        runCurrent()
        assertEquals(1, loads)
    }

    @Test
    fun refreshOfShown_passesThrough_otherIgnored() = runTest {
        val view = ordered(MAX to max, ADMOB to admob)
        val got = collect(view)
        view.send()
        max.emitFailed(204)
        runCurrent()
        view.send()
        admob.emitLoaded()
        runCurrent()
        assertEquals(listOf(TryNext, Loaded), got)

        admob.emitLoaded()
        runCurrent()
        admob.emitFailed(3)
        runCurrent()
        assertEquals(listOf(TryNext, Loaded, Loaded, Failed(3)), got)

        max.emitLoaded()
        max.emitFailed(204)
        runCurrent()
        assertEquals(listOf(TryNext, Loaded, Loaded, Failed(3)), got)
    }

    @Test
    fun pauseAll_resumeOnlyShown() = runTest {
        val view = ordered(MAX to max, ADMOB to admob)
        collect(view)
        view.send()
        max.emitFailed(204)
        runCurrent()
        view.send()
        admob.emitLoaded()
        runCurrent()
        assertEquals(1, max.pauseCount)
        assertEquals(0, admob.pauseCount)

        view.pause()
        assertEquals(1, admob.pauseCount)
        assertEquals(1, max.pauseCount)

        view.resume()
        assertEquals(1, admob.resumeCount)
        assertEquals(0, max.resumeCount)

        view.destroy()
        assertEquals(1, admob.destroyCount)
        assertEquals(1, max.destroyCount)
    }

    @Test
    fun frameHeight_isTallestOfPlatforms() {
        assertEquals(62, bannerFrameHeightDp(listOf(MAX, ADMOB), admobHeightDp = 62, maxHeightDp = 50))
        assertEquals(62, bannerFrameHeightDp(listOf(ADMOB, MAX), admobHeightDp = 50, maxHeightDp = 62))
        assertEquals(50, bannerFrameHeightDp(listOf(MAX), admobHeightDp = 62, maxHeightDp = 50))
        assertEquals(62, bannerFrameHeightDp(listOf(ADMOB), admobHeightDp = 62, maxHeightDp = 50))
        assertEquals(250, bannerFrameHeightDp(listOf(MAX, ADMOB), admobHeightDp = 250, maxHeightDp = 250))
    }

    @Test
    fun unitKey_nullWithoutPlatforms_changesWithOrderOrUnit() {
        assertNull(bannerSlotUnitKey(emptyList(), "a", "m"))
        val key = bannerSlotUnitKey(listOf(MAX, ADMOB), "a", "m")
        // Control: the same inputs give the same key, so the frame is kept.
        assertEquals(key, bannerSlotUnitKey(listOf(MAX, ADMOB), "a", "m"))
        assertTrue(key != null)
        assertNotEquals(key, bannerSlotUnitKey(listOf(ADMOB, MAX), "a", "m"))
        assertNotEquals(key, bannerSlotUnitKey(listOf(MAX, ADMOB), "a2", "m"))
        assertNotEquals(key, bannerSlotUnitKey(listOf(MAX, ADMOB), "a", "m2"))
        assertNotEquals(key, bannerSlotUnitKey(listOf(ADMOB), "a", "m"))
    }

    private fun telemetry() = AdTelemetry(
        metrics = RecordingAdMetrics(),
        funnel = RecordingAdFunnel(),
        lifecycleSwitches = { DEFAULT_FLAGS },
        serving = { true },
        offline = { false },
        counter = LoadAttemptCounter(),
        logger = logger,
    )

    private companion object {
        const val KEY = "gallery_banner"

        val DEFAULT_FLAGS: AdsGlobalFlags = runBlocking { DefaultAdsFlags(FakeRemoteSettings()).global.first() }
    }
}
