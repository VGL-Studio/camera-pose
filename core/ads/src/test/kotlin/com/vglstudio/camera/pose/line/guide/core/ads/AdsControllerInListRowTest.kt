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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/**
 * The native small row of a list in the real [AdsController] (plan T-059): each send of the row waits for the
 * in-screen window of T-054 (foreground, no full-screen ad over the app, KB-27), and a failed row tries again 5 s
 * later under its own log key. A fake native loader ([AdsController.nativeLoaderForTest]) takes the requests.
 */
@RunWith(AndroidJUnit4::class)
class AdsControllerInListRowTest {
    private val logger = RecordingAppLogger()
    private val loader = FakeNativeAdLoader()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val controller = testAdsController(
        unitIds = InScreenOnlyUnitIds,
        flags = DefaultAdsFlags(FakeRemoteSettings()),
        logger = logger,
    ).also { it.nativeLoaderForTest = loader }

    @After
    fun tearDown() {
        scope.cancel()
        idle()
    }

    private fun idleFor(millis: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun openGate() {
        controller.openConsentGateForTest()
        controller.markSdkReadyForTest()
    }

    /** A standalone native full-screen ad over the app. */
    private fun cover() {
        assertTrue(
            controller.beginStandaloneNativeTurn(AdPlacement.GALLERY_NATIVE_FULL_SCREEN, { FakeLoadedNativeAd() }) {},
        )
        assertFalse(controller.inScreenAdWindow.value)
    }

    /** Closes the standalone turn of [cover], once its countdown is over (K-03). */
    private fun uncover() {
        if (controller.nativeFullScreenView.value?.closeEnabled != true) {
            idleFor(NATIVE_CLOSE_COUNTDOWN_DEFAULT_SECONDS * 1_000)
        }
        controller.closeNativeFullScreen()
        assertTrue(controller.inScreenAdWindow.value)
    }

    private fun collect(states: Flow<NativeAdState>): MutableList<NativeAdState> {
        val received = mutableListOf<NativeAdState>()
        scope.launch { states.collect { received += it } }
        idle()
        return received
    }

    private fun row(slot: Int) =
        controller.inListRowStates(AdPlacement.GALLERY_INLIST_SMALL, slot, MutableStateFlow(true))

    @Test
    fun inListRow_waitsWhileCovered_thenSends() {
        openGate()
        controller.setAppForegroundForTest(true)
        idle()
        // Control (L-055): not covered, a row sends at once.
        val free = collect(row(1))
        assertEquals(1, loader.requests.size)
        loader.deliver(FakeLoadedNativeAd())
        idle()
        assertTrue(free.last() is NativeAdState.Loaded)

        cover()
        collect(row(0))
        idleFor(60_000)
        assertEquals(1, loader.requests.size)

        uncover()
        idle()
        assertEquals(2, loader.requests.size)
    }

    @Test
    fun inListRow_inBackground_sendsNothing() {
        openGate()
        controller.setAppForegroundForTest(false)
        idle()

        collect(row(0))
        idleFor(60_000)
        assertEquals(0, loader.requests.size)

        controller.setAppForegroundForTest(true)
        idle()
        assertEquals(1, loader.requests.size)
    }

    @Test
    fun inListRow_failedThenRetriesAt5s_listRowMetrics() {
        // The slot metrics on and the controller started, so each load of the row reports to the metrics.
        val settings = FakeRemoteSettings().apply { set(AdsSettings.IS_LOG_AD_SLOT_EVENTS, true) }
        val metrics = RecordingAdMetrics()
        val throwing = ThrowingNativeAdLoader()
        val started = testAdsController(
            unitIds = InScreenOnlyUnitIds,
            flags = DefaultAdsFlags(settings),
            metrics = metrics,
            logger = logger,
        ).also { it.nativeLoaderForTest = throwing }
        started.start(ApplicationProvider.getApplicationContext<Application>(), AppActivityFilter { true })
        started.openConsentGateForTest()
        started.setAppForegroundForTest(true)
        idle()

        val states = collect(started.inListRowStates(AdPlacement.GALLERY_INLIST_SMALL, 0, MutableStateFlow(true)))
        assertEquals(1, throwing.calls)
        assertEquals(NativeAdState.Unavailable, states.last())

        idleFor(4_999)
        assertEquals(1, throwing.calls)
        idleFor(1)
        assertEquals(2, throwing.calls)

        // Each load of the row has its own report: two load failures, no load_attempt, the placement of the row.
        val fails = metrics.events.filterIsInstance<AdMetricEvent.LoadFail>()
        assertEquals(metrics.events.toString(), 2, fails.size)
        assertEquals(listOf<Int?>(null, null), fails.map { it.loadAttempt })
        assertEquals(listOf("gallery_inlist_small", "gallery_inlist_small"), fails.map { it.adPlacement })
        assertTrue(
            logger.entries.map { it.message }.toString(),
            logger.entries.any { it.message == "Native gallery_inlist_small#0 retry in 5000 ms (failures 1)" },
        )
    }

    /** A loader whose request cannot be built: every load throws before anything is sent (O-09). */
    private class ThrowingNativeAdLoader : NativeAdLoader {
        var calls = 0
            private set

        override fun load(
            unitId: String,
            size: NativeAdSize,
            onLoaded: (LoadedNativeAd) -> Unit,
            onFailed: (code: Int) -> Unit,
        ) {
            calls++
            throw IllegalStateException("request not built")
        }
    }

    /** Test units for the in-screen native formats only: no full-screen ad or banner of this test sends anything. */
    private object InScreenOnlyUnitIds : AdUnitIds {
        override fun of(placement: AdPlacement): String? = of(placement, placement.format)

        override fun of(placement: AdPlacement, format: AdFormat): String? = when (format) {
            AdFormat.NATIVE_SMALL, AdFormat.NATIVE_MEDIUM, AdFormat.NATIVE_COLLAPSIBLE ->
                GoogleTestAdUnitIds.unitFor(format)
            else -> null
        }
    }
}
