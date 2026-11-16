package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Activity
import android.app.Application
import android.os.Looper
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import com.vglstudio.camera.pose.line.guide.core.network.InternetConnectivity
import java.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

/**
 * The retry waits of every native place with no ad in the real [AdsController], on the virtual clock of the main
 * looper (KB-24, O-05, plan T-103): 5, 10, 20, 40, then 60 s, at most 13 requests in 10 minutes, and the next failure
 * waits 5 s again after an ad loads, the whole process comes back to foreground, Internet comes back, or a new screen
 * opens the slot or row. Each place: the native small slot (its stock), the slot drawn as another format, the list row,
 * the pool, the native full-screen ad. The in-screen loads go to [AdsController.nativeLoaderForTest], the native
 * full-screen sends to [AdsController.nativeFullScreenRequestForTest]; times are counted from the first send.
 */
@RunWith(AndroidJUnit4::class)
class AdsControllerNativeRetryTest {
    private val settings = FakeRemoteSettings().apply {
        listOf(
            AdPlacement.GALLERY_NATIVE_SMALL,
            AdPlacement.GALLERY_NATIVE_MEDIUM,
            AdPlacement.GALLERY_INLIST_SMALL,
            AdPlacement.GALLERY_NATIVE_FULL_SCREEN,
        ).forEach { set(AdsSettings.placementEnabled(it), true) }
    }
    private val logger = RecordingAppLogger()
    private val connected = MutableStateFlow(true)
    private val connectivity = object : InternetConnectivity {
        override val connected: Flow<Boolean> = this@AdsControllerNativeRetryTest.connected
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Made before the controller starts: its lifecycle never reaches the activity tracker (no resume, S-25).
    private val activity: Activity = Robolectric.buildActivity(ComponentActivity::class.java).get()

    /** Times of the in-screen native sends, from [startMillis]. */
    private val sends = mutableListOf<Long>()

    /** Times of the native full-screen sends, from [startMillis]. */
    private val fullScreenSends = mutableListOf<Long>()

    private var startMillis = 0L
    private lateinit var controller: AdsController

    @After
    fun tearDown() {
        scope.cancel()
        idle()
    }

    private fun idleFor(millis: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun now(): Long = SystemClock.elapsedRealtime() - startMillis

    /** Moves the virtual clock to [millis] after the start and runs what is due. */
    private fun at(millis: Long) {
        val left = millis - now()
        require(left >= 0) { "at($millis) is in the past: now ${now()}" }
        idleFor(left)
    }

    /** A loader whose request cannot be built: every load fails at once (O-09), its time recorded. */
    private inner class ThrowingNativeAdLoader : NativeAdLoader {
        override fun load(
            unitId: String,
            size: NativeAdSize,
            onLoaded: (LoadedNativeAd) -> Unit,
            onFailed: (code: Int, logMessage: String?) -> Unit,
        ) {
            sends += now()
            throw IllegalStateException("request not built")
        }
    }

    /** [fake], with the time of each request recorded. */
    private inner class TimedNativeAdLoader(private val fake: FakeNativeAdLoader) : NativeAdLoader {
        override fun load(
            unitId: String,
            size: NativeAdSize,
            onLoaded: (LoadedNativeAd) -> Unit,
            onFailed: (code: Int, logMessage: String?) -> Unit,
        ) {
            sends += now()
            fake.load(unitId, size, onLoaded, onFailed)
        }
    }

    /**
     * The controller of the test, started, consent gate open, app in foreground, a [FullScreenAdHost] attached (HD-05),
     * no preload asked and the SDK never started; the clock starts here.
     */
    private fun startController(
        unitIds: AdUnitIds = InScreenOnlyUnitIds,
        loader: NativeAdLoader = ThrowingNativeAdLoader(),
    ): AdsController {
        val made = testAdsController(
            unitIds = unitIds,
            flags = DefaultAdsFlags(settings),
            connectivity = connectivity,
            logger = logger,
        )
        made.nativeLoaderForTest = loader
        made.attachFullScreenHost()
        made.start(ApplicationProvider.getApplicationContext<Application>(), AppActivityFilter { true })
        made.openConsentGateForTest()
        made.setAppForegroundForTest(true)
        idle()
        controller = made
        startMillis = SystemClock.elapsedRealtime()
        return made
    }

    /** The controller of [startController] with native full-screen units; each send of that ad fails. */
    private fun startFullScreen(answer: (Int) -> LoadedNativeAd? = { null }) {
        startController(unitIds = GoogleTestAdUnitIds)
        var calls = 0
        controller.nativeFullScreenRequestForTest = {
            fullScreenSends += now()
            calls++
            answer(calls)
        }
        controller.loadFullScreenAdIfNeeded(AdPlacement.GALLERY_NATIVE_FULL_SCREEN)
        idle()
    }

    private fun collect(states: Flow<NativeAdState>, into: MutableList<NativeAdState> = mutableListOf()): Job {
        val job = scope.launch { states.collect { into += it } }
        idle()
        return job
    }

    private fun slot() = controller.nativeAd(AdPlacement.GALLERY_NATIVE_SMALL)

    /** The slot drawn as another format: no stock, no pool, the retries of [retryingNativeAdStates] alone. */
    private fun retryingSlot() = controller.nativeAd(AdPlacement.GALLERY_NATIVE_SMALL, AdFormat.NATIVE_MEDIUM)

    private fun row(slot: Int = 0) =
        controller.inListRowStates(AdPlacement.GALLERY_INLIST_SMALL, slot, MutableStateFlow(true))

    private fun cell() = controller.pooledCell(AdPlacement.GALLERY_NATIVE_MEDIUM)

    /** The app goes to background: its activities and the whole process. */
    private fun toBackground() {
        controller.setAppForegroundForTest(false)
        controller.setProcessForegroundForTest(false)
        idle()
    }

    /** The app comes back to foreground in the order of [AdsActivityTracker]: the process, then the app activity. */
    private fun backToForeground() {
        controller.setProcessForegroundForTest(true)
        controller.setAppForegroundForTest(true)
        idle()
    }

    private fun messages(): List<String> = logger.entries.map { it.message }

    private fun count(message: String): Int = messages().count { it == message }

    /** Control of every 10-minute count: no preload sent anything, the SDK never started. */
    private fun assertNothingElseSent() {
        val messages = messages()
        assertFalse(messages.toString(), messages.any { PRELOAD_RUN.matches(it) })
        assertFalse(messages.toString(), messages.any { STOCK_LOAD.matches(it) })
        assertFalse(messages.toString(), messages.any { it.contains("SDK ready") })
    }

    // KB-24, criterion 3: 10 minutes in foreground, no reset, never an ad.

    @Test
    fun slot_alwaysFailing_10min_13Requests() {
        startController()
        collect(slot())
        at(TEN_MINUTES)

        assertEquals(TEN_MINUTE_SENDS, sends)
        assertTrue(sends.size <= 15)
        assertNothingElseSent()
        BACKOFF.forEachIndexed { index, wait ->
            val line = "Native gallery_native_small retry in $wait ms (failures ${index + 1})"
            assertEquals(line, 1, count(line))
        }
    }

    @Test
    fun inListRow_alwaysFailing_10min_13Requests() {
        startController()
        collect(row())
        at(TEN_MINUTES)

        assertEquals(TEN_MINUTE_SENDS, sends)
        assertNothingElseSent()
    }

    @Test
    fun pool_alwaysFailing_10min_13Requests() {
        startController()
        // Two cells: their 500 ms and 2 s rounds (PoolCellPolicy) run all along and send nothing more.
        collect(cell())
        collect(cell())
        at(TEN_MINUTES)

        assertEquals(TEN_MINUTE_SENDS, sends)
        assertNothingElseSent()
        BACKOFF.forEachIndexed { index, wait ->
            val line = Regex("Pool \\S+ failed, retry in $wait ms \\(failures ${index + 1}\\)")
            assertEquals(line.pattern, 1, messages().count { line.matches(it) })
        }
    }

    @Test
    fun nativeFullScreen_alwaysFailing_10min_13Requests() {
        startFullScreen()
        at(TEN_MINUTES)

        assertEquals(TEN_MINUTE_SENDS, fullScreenSends)
        assertNothingElseSent()
        BACKOFF.forEachIndexed { index, wait ->
            val line = "Retry gallery_native_full_screen in $wait ms (failures ${index + 1}, code 3)"
            assertEquals(line, 1, count(line))
        }
    }

    @Test
    fun twoRows_countApart() {
        startController()
        collect(row(0))
        at(15_000)
        collect(row(1))
        at(80_000)

        // Row 0: 0, 5 000, 15 000, 35 000, 75 000; row 1: 15 000, 20 000, 30 000, 50 000.
        assertEquals(
            listOf(0L, 5_000L, 15_000L, 15_000L, 20_000L, 30_000L, 35_000L, 50_000L, 75_000L),
            sends.sorted(),
        )
    }

    // An ad loaded.

    @Test
    fun slot_successThenFailure_waits5s() {
        val fake = FakeNativeAdLoader()
        startController(loader = TimedNativeAdLoader(fake))
        val states = mutableListOf<NativeAdState>()
        collect(slot(), states)
        fake.fail(3)
        idle()
        at(5_000)
        fake.fail(3)
        idle()
        at(15_000)
        fake.fail(3)
        idle()
        at(35_000)
        assertEquals(listOf(0L, 5_000L, 15_000L, 35_000L), sends)
        fake.deliver(FakeLoadedNativeAd())
        idle()
        assertTrue(states.toString(), states.last() is NativeAdState.Loaded)
        val loadedAt = logger.entries.size

        at(95_000)
        // The slot holds an ad: its refresh takes over, by the rules of a slot holding an ad (NativeAdRefresh).
        val after = logger.entries.drop(loadedAt).map { it.message }
        assertFalse(after.toString(), after.any { it.startsWith("Native gallery_native_small retry in") })
    }

    @Test
    fun pool_successThenEmptyAgain_failureWaits5s() {
        val fake = FakeNativeAdLoader()
        startController(loader = TimedNativeAdLoader(fake))
        val states = mutableListOf<NativeAdState>()
        val first = collect(cell(), states)
        fake.fail(3)
        idle()
        at(5_000)
        fake.fail(3)
        idle()
        at(15_000)
        fake.fail(3)
        idle()
        at(35_000)
        // An ad comes: the pool holds one and sends its next request at once.
        val ad = FakeLoadedNativeAd()
        fake.deliver(ad)
        idle()
        assertEquals(listOf(0L, 5_000L, 15_000L, 35_000L, 35_000L), sends)
        // Failures while holding an ad: 2, 4, 8 s; meanwhile the cell takes the ad.
        fake.fail(3)
        idle()
        at(37_000)
        fake.fail(3)
        idle()
        at(41_000)
        fake.fail(3)
        idle()
        // The cell leaves with the ad after its impression: the pool is empty again.
        at(42_000)
        assertTrue(states.toString(), states.any { it is NativeAdState.Loaded && it.ad === ad })
        ad.impressionRecorded = true
        first.cancel()
        idle()
        at(49_000)
        // The first failure of the empty pool again waits 5 s, not 40 or 60 s.
        fake.fail(3)
        idle()
        at(54_000)

        assertEquals(listOf(0L, 5_000L, 15_000L, 35_000L, 35_000L, 37_000L, 41_000L, 49_000L, 54_000L), sends)
    }

    @Test
    fun nativeFullScreen_loadedThenFailure_waits5s() {
        val ad = FakeLoadedNativeAd()
        startFullScreen { call -> if (call == 4) ad else null }
        at(35_000)
        assertEquals(listOf(0L, 5_000L, 15_000L, 35_000L), fullScreenSends)
        assertEquals(
            FullScreenLoadState.Ready,
            controller.nativeFullScreenLoadStateForTest(AdPlacement.GALLERY_NATIVE_FULL_SCREEN),
        )

        // Shown through the path of rememberShowFullScreenAd, then closed after its countdown.
        var shown: Boolean? = null
        scope.launch { shown = controller.showFullScreenAd(activity, AdPlacement.GALLERY_NATIVE_FULL_SCREEN) }
        idle()
        assertEquals(true, shown)
        assertTrue(controller.nativeFullScreenView.value!!.ad === ad)
        assertNotEquals(
            FullScreenLoadState.Ready,
            controller.nativeFullScreenLoadStateForTest(AdPlacement.GALLERY_NATIVE_FULL_SCREEN),
        )
        at(40_000)
        assertTrue(controller.nativeFullScreenView.value!!.closeEnabled)
        controller.closeNativeFullScreen()
        idle()
        // The reload after the close (F-06, at once) fails: the next try waits 5 s, not 40 s.
        at(49_000)

        assertEquals(listOf(0L, 5_000L, 15_000L, 35_000L, 40_000L, 45_000L), fullScreenSends)
    }

    // The whole process back in foreground.

    @Test
    fun slot_backgroundAndBack_nextFailureWaits5s() {
        startController()
        collect(slot())
        assertBackgroundAndBack()
    }

    @Test
    fun slot_backgroundAndBack_control_keepsBackoff() {
        // Control (L-055) of slot_backgroundAndBack_nextFailureWaits5s: never in background, 75 000 then 135 000.
        startController()
        collect(slot())
        at(140_000)
        assertEquals(listOf(0L, 5_000L, 15_000L, 35_000L, 75_000L, 135_000L), sends)
    }

    @Test
    fun retryingSlot_backgroundAndBack_nextFailureWaits5s() {
        startController()
        collect(retryingSlot())
        assertBackgroundAndBack()
    }

    @Test
    fun inListRow_backgroundAndBack_nextFailureWaits5s() {
        startController()
        collect(row())
        assertBackgroundAndBack()
    }

    @Test
    fun pool_backgroundAndBack_nextFailureWaits5s() {
        startController()
        collect(cell())
        assertBackgroundAndBack()
    }

    @Test
    fun nativeFullScreen_backgroundAndBack_nextFailureWaits5s() {
        startFullScreen()
        assertBackgroundAndBack(fullScreenSends)
    }

    /**
     * Failures at 0, 5 000, 15 000, 35 000 (the next try due at 75 000); background at 40 000, foreground at 50 000:
     * the try still goes at 75 000 (never sooner), and its failure waits 5 s.
     */
    private fun assertBackgroundAndBack(times: List<Long> = sends) {
        at(40_000)
        toBackground()
        at(50_000)
        backToForeground()
        at(74_999)
        assertEquals(listOf(0L, 5_000L, 15_000L, 35_000L), times)
        at(89_000)
        assertEquals(listOf(0L, 5_000L, 15_000L, 35_000L, 75_000L, 80_000L), times)
        assertEquals(1, count("Native retry counters reset: foreground"))
    }

    @Test
    fun slot_appActivityHiddenOnly_doesNotReset() {
        // An SDK activity over the app, then closed, or a rotation: the app activity stops and starts, the process
        // stays in foreground (AdsActivityTracker): not a return to foreground.
        startController()
        collect(slot())
        at(16_000)
        controller.setAppForegroundForTest(false)
        idle()
        at(20_000)
        controller.setAppForegroundForTest(true)
        idle()
        at(74_999)
        assertEquals(listOf(0L, 5_000L, 15_000L, 35_000L), sends)
        at(75_000)
        assertEquals(listOf(0L, 5_000L, 15_000L, 35_000L, 75_000L), sends)
        assertEquals(0, messages().count { it.startsWith("Native retry counters reset") })
    }

    @Test
    fun slot_processForeground_resets() {
        // The same, through the real entry point of the tracker for the whole process.
        startController()
        collect(slot())
        at(16_000)
        controller.setAppForegroundForTest(false)
        controller.onProcessForegroundChanged(false)
        idle()
        at(20_000)
        controller.onProcessForegroundChanged(true)
        controller.setAppForegroundForTest(true)
        idle()
        at(40_000)
        assertEquals(listOf(0L, 5_000L, 15_000L, 35_000L, 40_000L), sends)
        assertEquals(1, count("Native retry counters reset: foreground"))
    }

    @Test
    fun slot_retryDueInBackground_sentOnReturn() {
        startController()
        collect(slot())
        at(1_000)
        toBackground()
        at(30_000)
        assertEquals(listOf(0L), sends)
        backToForeground()
        at(70_000)

        assertEquals(listOf(0L, 30_000L, 35_000L, 45_000L, 65_000L), sends)
    }

    @Test
    fun slot_coverClosed_doesNotReset() {
        startController()
        collect(slot())
        at(16_000)
        assertTrue(
            controller.beginStandaloneNativeTurn(AdPlacement.GALLERY_NATIVE_FULL_SCREEN, { FakeLoadedNativeAd() }) {},
        )
        assertFalse(controller.inScreenAdWindow.value)
        at(16_000 + NATIVE_CLOSE_COUNTDOWN_DEFAULT_SECONDS * 1_000)
        assertTrue(controller.nativeFullScreenView.value!!.closeEnabled)
        controller.closeNativeFullScreen()
        idle()
        assertTrue(controller.inScreenAdWindow.value)
        at(74_999)
        assertEquals(listOf(0L, 5_000L, 15_000L, 35_000L), sends)
        at(75_000)
        assertEquals(listOf(0L, 5_000L, 15_000L, 35_000L, 75_000L), sends)
        assertEquals(0, messages().count { it.startsWith("Native retry counters reset") })
    }

    // KB-24, criterion 4: Internet back.

    @Test
    fun slot_networkBack_cutsWaitAndNextFailureWaits5s() {
        startController()
        collect(slot())
        assertNetworkBack()
    }

    @Test
    fun inListRow_networkBack_cutsWaitAndNextFailureWaits5s() {
        startController()
        collect(row())
        assertNetworkBack()
    }

    @Test
    fun pool_networkBack_cutsWaitAndNextFailureWaits5s() {
        startController()
        collect(cell())
        assertNetworkBack()
    }

    @Test
    fun nativeFullScreen_networkBack_cutsWaitAndNextFailureWaits5s() {
        startFullScreen()
        assertNetworkBack(fullScreenSends) {
            // Control: before Internet counts as back, the try is due at 75 000.
            assertEquals(
                FullScreenLoadState.WaitingRetry(startMillis + 75_000),
                controller.nativeFullScreenLoadStateForTest(AdPlacement.GALLERY_NATIVE_FULL_SCREEN),
            )
        }
        // Not stuck on the old due time: the try cut short was sent, and its failure is due 5 s later.
        at(55_000)
        assertEquals(
            FullScreenLoadState.WaitingRetry(startMillis + 63_000),
            controller.nativeFullScreenLoadStateForTest(AdPlacement.GALLERY_NATIVE_FULL_SCREEN),
        )
    }

    /**
     * Failures at 0, 5 000, 15 000, 35 000; Internet lost at 40 000, back at 45 000, counted back at 48 000: the wait is
     * cut short at 48 000, and that failure waits 5 s.
     */
    private fun assertNetworkBack(times: List<Long> = sends, beforeBack: () -> Unit = {}) {
        at(40_000)
        connected.value = false
        idle()
        at(45_000)
        connected.value = true
        idle()
        at(45_000 + NETWORK_SETTLE_MILLIS - 1)
        assertEquals(listOf(0L, 5_000L, 15_000L, 35_000L), times)
        beforeBack()
        at(45_000 + NETWORK_SETTLE_MILLIS)
        assertEquals(listOf(0L, 5_000L, 15_000L, 35_000L, 48_000L), times)
        at(54_000)
        assertEquals(listOf(0L, 5_000L, 15_000L, 35_000L, 48_000L, 53_000L), times)
        assertEquals(1, count("Native retry counters reset: network back"))
    }

    // A new screen.

    @Test
    fun slot_reopened_nextFailureWaits5s() {
        startController()
        assertReopened { slot() }
    }

    @Test
    fun inListRow_reopened_nextFailureWaits5s() {
        startController()
        assertReopened { row() }
    }

    /** Failures at 0, 5 000, 15 000; the screen left at 20 000, a new one at 25 000: the new flow counts from 1. */
    private fun assertReopened(states: () -> Flow<NativeAdState>) {
        val old = collect(states())
        at(20_000)
        old.cancel()
        idle()
        at(25_000)
        collect(states())
        at(70_000)

        assertEquals(listOf(0L, 5_000L, 15_000L, 25_000L, 30_000L, 40_000L, 60_000L), sends)
    }

    @Test
    fun pool_newScreen_keepsBackoff() {
        // The pool is shared by many screens and cells: a new cell does not count its failures from 1 (question 1).
        startController()
        val old = collect(cell())
        at(40_000)
        old.cancel()
        idle()
        at(45_000)
        collect(cell())
        at(74_999)
        assertEquals(listOf(0L, 5_000L, 15_000L, 35_000L), sends)
        at(134_999)
        assertEquals(listOf(0L, 5_000L, 15_000L, 35_000L, 75_000L), sends)
        at(135_000)
        assertEquals(listOf(0L, 5_000L, 15_000L, 35_000L, 75_000L, 135_000L), sends)
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

    private companion object {
        const val TEN_MINUTES = 600_000L

        /** 5, 10, 20, 40, then every 60 s, failing at once: 13 sends in 10 minutes (KB-24, at most 15). */
        val TEN_MINUTE_SENDS = listOf(
            0L,
            5_000L,
            15_000L,
            35_000L,
            75_000L,
            135_000L,
            195_000L,
            255_000L,
            315_000L,
            375_000L,
            435_000L,
            495_000L,
            555_000L,
        )

        /** The waits after the first five failures in a row (O-05). */
        val BACKOFF = listOf(5_000L, 10_000L, 20_000L, 40_000L, 60_000L)

        val PRELOAD_RUN = Regex("Preload [A-Z_]+: RUN.*")
        val STOCK_LOAD = Regex("Native \\S+ (preload|next load)")
    }
}
