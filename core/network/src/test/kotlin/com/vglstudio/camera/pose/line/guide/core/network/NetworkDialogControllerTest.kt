package com.vglstudio.camera.pose.line.guide.core.network

import app.cash.turbine.test
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlag
import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** When the network dialog shows and closes (spec network, KB-1 to KB-4), on virtual time. */
@OptIn(ExperimentalCoroutinesApi::class)
class NetworkDialogControllerTest {
    private val connectivity = FakeConnectivitySource(connected = true)
    private val flags = FakeFeatureFlags(setOf(FeatureFlag.NETWORK_DIALOG))
    private val settings = FakeRemoteSettings()
    private val logger = RecordingAppLogger()

    /** A controller past the loading screen ([active]), with the flag on and the network up unless a test says so. */
    private fun TestScope.controller(active: Boolean = true): NetworkDialogController =
        NetworkDialogController(connectivity, flags, settings, logger, backgroundScope).also {
            if (active) it.setActive(true)
            runCurrent()
        }

    private fun TestScope.recoveredCount(controller: NetworkDialogController): () -> Int {
        var count = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { controller.recovered.collect { count++ } }
        return { count }
    }

    private fun TestScope.seenStates(controller: NetworkDialogController): List<NetworkDialogState> {
        val seen = mutableListOf<NetworkDialogState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { controller.state.collect { seen += it } }
        return seen
    }

    @Test
    fun deviceOffline_shownOnlyAfterDelay() = runTest {
        val controller = controller()

        connectivity.connected.value = false
        advanceTimeBy(2_999)
        assertEquals(HIDDEN, controller.state.value)

        advanceTimeBy(2)
        assertEquals(SHOWN, controller.state.value)
    }

    @Test
    fun delay_followsRemoteSetting() = runTest {
        settings.longs.value = mapOf("network_lost_delay_seconds" to 5L)
        val controller = controller()

        connectivity.connected.value = false
        advanceTimeBy(4_999)
        assertEquals(HIDDEN, controller.state.value)

        advanceTimeBy(2)
        assertEquals(SHOWN, controller.state.value)
    }

    @Test
    fun flapping_underDelay_showsNothing() = runTest {
        val controller = controller()
        val seen = seenStates(controller)

        repeat(2) {
            connectivity.connected.value = false
            advanceTimeBy(2_000)
            connectivity.connected.value = true
            advanceTimeBy(1)
        }
        advanceTimeBy(10_000)
        assertTrue("$seen", seen.all { it == HIDDEN })

        // The controller does watch the network: a loss longer than the delay shows the dialog.
        connectivity.connected.value = false
        advanceTimeBy(3_001)
        assertEquals(SHOWN, controller.state.value)
    }

    @Test
    fun networkFailure_showsAtOnce() = runTest {
        val controller = controller()

        controller.reportNetworkFailure("assets")
        runCurrent()

        assertEquals(SHOWN, controller.state.value)
    }

    @Test
    fun manyFailures_singleDialog() = runTest {
        val controller = controller()

        controller.state.test {
            assertEquals(HIDDEN, awaitItem())
            repeat(5) { controller.reportNetworkFailure("assets") }
            connectivity.connected.value = false
            advanceTimeBy(10_000)
            assertEquals(SHOWN, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun inactive_ignoresOfflineAndFailures() = runTest {
        val controller = controller(active = false)

        connectivity.connected.value = false
        advanceTimeBy(10_000)
        controller.reportNetworkFailure("assets")
        runCurrent()
        assertEquals(HIDDEN, controller.state.value)
        assertEquals(0, connectivity.collectors)

        // A failure reported on the loading screen is not kept for later.
        connectivity.connected.value = true
        controller.setActive(true)
        runCurrent()
        assertEquals(HIDDEN, controller.state.value)
        assertEquals(1, connectivity.collectors)

        controller.reportNetworkFailure("assets")
        runCurrent()
        assertEquals(SHOWN, controller.state.value)
    }

    @Test
    fun activated_whileOffline_showsAfterDelay() = runTest {
        connectivity.connected.value = false
        val controller = controller(active = false)

        controller.setActive(true)
        advanceTimeBy(2_999)
        assertEquals(HIDDEN, controller.state.value)

        advanceTimeBy(2)
        assertEquals(SHOWN, controller.state.value)
    }

    @Test
    fun deactivated_hidesDialog_andStopsWatching() = runTest {
        val controller = controller()
        controller.reportNetworkFailure("assets")
        runCurrent()
        assertEquals(SHOWN, controller.state.value)
        assertEquals(1, connectivity.collectors)

        controller.setActive(false)
        runCurrent()

        assertEquals(HIDDEN, controller.state.value)
        assertEquals(0, connectivity.collectors)
    }

    @Test
    fun networkBack_withoutRetry_keepsDialog() = runTest {
        connectivity.connected.value = false
        val controller = controller()
        advanceTimeBy(3_001)
        assertEquals(SHOWN, controller.state.value)

        connectivity.connected.value = true
        advanceTimeBy(10_000)

        assertEquals(SHOWN, controller.state.value)
    }

    @Test
    fun retry_online_closes_andEmitsRecovered() = runTest {
        val controller = controller()
        val recovered = recoveredCount(controller)
        controller.reportNetworkFailure("assets")
        runCurrent()
        assertEquals(SHOWN, controller.state.value)

        controller.retry()
        runCurrent()

        assertEquals(HIDDEN, controller.state.value)
        assertEquals(1, recovered())
    }

    @Test
    fun retry_offline_spinsAtMost2s_thenStillOffline() = runTest {
        connectivity.connected.value = false
        val controller = controller()
        val recovered = recoveredCount(controller)
        controller.reportNetworkFailure("assets")
        runCurrent()

        controller.retry()
        runCurrent()
        assertEquals(CHECKING, controller.state.value)

        advanceTimeBy(1_999)
        assertEquals(CHECKING, controller.state.value)

        advanceTimeBy(2)
        assertEquals(STILL_OFFLINE, controller.state.value)
        assertEquals(0, recovered())
    }

    @Test
    fun retry_networkReturnsWhileChecking_closes() = runTest {
        connectivity.connected.value = false
        val controller = controller()
        val recovered = recoveredCount(controller)
        controller.reportNetworkFailure("assets")
        runCurrent()

        controller.retry()
        advanceTimeBy(1_000)
        connectivity.connected.value = true
        advanceTimeBy(300)

        assertEquals(HIDDEN, controller.state.value)
        assertEquals(1, recovered())
    }

    @Test
    fun retry_whileChecking_isIgnored() = runTest {
        connectivity.connected.value = false
        val controller = controller()
        controller.reportNetworkFailure("assets")
        runCurrent()

        controller.retry()
        advanceTimeBy(1_000)
        controller.retry()
        advanceTimeBy(1_001)

        assertEquals(STILL_OFFLINE, controller.state.value)
    }

    @Test
    fun retryAgain_clearsStillOffline() = runTest {
        connectivity.connected.value = false
        val controller = controller()
        controller.reportNetworkFailure("assets")
        runCurrent()
        controller.retry()
        advanceTimeBy(2_001)
        assertEquals(STILL_OFFLINE, controller.state.value)

        controller.retry()
        runCurrent()

        assertEquals(CHECKING, controller.state.value)
    }

    @Test
    fun afterRecovery_newFailureShowsAgain() = runTest {
        val controller = controller()
        controller.reportNetworkFailure("assets")
        runCurrent()
        controller.retry()
        runCurrent()
        assertEquals(HIDDEN, controller.state.value)

        controller.reportNetworkFailure("assets")
        runCurrent()

        assertEquals(SHOWN, controller.state.value)
    }

    @Test
    fun afterRecovery_offlineAgainShowsAfterDelay() = runTest {
        val controller = controller()
        controller.reportNetworkFailure("assets")
        runCurrent()
        controller.retry()
        runCurrent()
        assertEquals(HIDDEN, controller.state.value)

        connectivity.connected.value = false
        advanceTimeBy(2_999)
        assertEquals(HIDDEN, controller.state.value)
        advanceTimeBy(2)

        assertEquals(SHOWN, controller.state.value)
    }

    @Test
    fun flagOff_neverShows() = runTest {
        flags.enabled.value = emptySet()
        val controller = controller()

        connectivity.connected.value = false
        advanceTimeBy(10_000)
        controller.reportNetworkFailure("assets")
        runCurrent()
        assertEquals(HIDDEN, controller.state.value)

        // The flag is watched, not read once (ADR-0003): turned on, the dialog shows again.
        flags.enabled.value = setOf(FeatureFlag.NETWORK_DIALOG)
        runCurrent()
        controller.reportNetworkFailure("assets")
        runCurrent()
        assertEquals(SHOWN, controller.state.value)
    }

    @Test
    fun flagTurnedOff_whileShown_hides() = runTest {
        val controller = controller()
        controller.reportNetworkFailure("assets")
        runCurrent()
        assertEquals(SHOWN, controller.state.value)

        flags.enabled.value = emptySet()
        runCurrent()

        assertEquals(HIDDEN, controller.state.value)
    }

    @Test
    fun dialogShown_followsState() = runTest {
        val controller = controller()
        assertFalse(controller.dialogShown.value)

        controller.reportNetworkFailure("assets")
        runCurrent()
        assertTrue(controller.dialogShown.value)

        controller.retry()
        runCurrent()
        assertFalse(controller.dialogShown.value)
    }

    @Test
    fun logsShowAndRetryOutcome() = runTest {
        connectivity.connected.value = false
        val controller = controller()
        advanceTimeBy(3_001)
        controller.retry()
        advanceTimeBy(2_001)
        connectivity.connected.value = true
        controller.retry()
        runCurrent()
        controller.reportNetworkFailure("assets")
        runCurrent()

        val messages = logger.entries.filter { it.tag == "Network" && it.level == LogLevel.INFO }.map { it.message }
        listOf("device_offline", "still_offline", "online", "load_failed").forEach { word ->
            assertTrue("$word in $messages", messages.any { word in it })
        }
        assertTrue("$messages", messages.none { "/" in it })
    }

    // A full-screen ad on screen (plan T-074, pass 2; T-017 of the template): the dialog waits for it to close.

    @Test
    fun lostWhileAdShowing_dialogWaits_thenShowsWhenAdCloses() = runTest {
        val controller = controller()
        controller.setFullScreenAdShowing(true)

        connectivity.connected.value = false
        advanceTimeBy(3_001)
        controller.reportNetworkFailure("assets")
        runCurrent()
        assertEquals(HIDDEN, controller.state.value)
        assertTrue(logger.entries.any { it.tag == "Network" && it.message == "Dialog waits: full_screen_ad" })

        controller.setFullScreenAdShowing(false)
        runCurrent()

        assertEquals(SHOWN, controller.state.value)
    }

    @Test
    fun adStartsWhileDialogShown_hidesThenShowsAgainOnClose() = runTest {
        connectivity.connected.value = false
        val controller = controller()
        controller.reportNetworkFailure("assets")
        runCurrent()
        controller.retry()
        runCurrent()
        assertEquals(CHECKING, controller.state.value)

        controller.setFullScreenAdShowing(true)
        runCurrent()
        assertEquals(HIDDEN, controller.state.value)
        // The check that was running is stopped: it does not bring the dialog back over the ad.
        advanceTimeBy(5_000)
        assertEquals(HIDDEN, controller.state.value)

        controller.setFullScreenAdShowing(false)
        runCurrent()

        // Shown again, as a new dialog: not stuck in the check stopped by the ad.
        assertEquals(SHOWN, controller.state.value)
    }

    @Test
    fun adCloses_withoutLoss_staysHidden() = runTest {
        val controller = controller()

        controller.setFullScreenAdShowing(true)
        runCurrent()
        controller.setFullScreenAdShowing(false)
        advanceTimeBy(10_000)
        assertEquals(HIDDEN, controller.state.value)

        // Control: once the ad is closed, a failure shows the dialog at once, as before.
        controller.reportNetworkFailure("assets")
        runCurrent()
        assertEquals(SHOWN, controller.state.value)
    }

    @Test
    fun inactiveWhileWaiting_dropsPending() = runTest {
        val controller = controller()
        controller.setFullScreenAdShowing(true)
        controller.reportNetworkFailure("assets")
        runCurrent()
        assertEquals(HIDDEN, controller.state.value)

        // Back to loading while the ad shows: the failure waiting for the ad is dropped, as any failure there.
        controller.setActive(false)
        runCurrent()
        controller.setFullScreenAdShowing(false)
        controller.setActive(true)
        runCurrent()
        assertEquals(HIDDEN, controller.state.value)

        // Control: with no ad on screen the dialog shows at once, as before.
        controller.reportNetworkFailure("assets")
        runCurrent()
        assertEquals(SHOWN, controller.state.value)
    }

    // Back from the background without network (spec network KB-3; plan T-077): the dialog shows after a check of at
    // most 2 seconds. "Background" is onAppStopped(changingConfigurations = false), "back" is onAppStarted().

    private fun NetworkDialogController.toBackground() = onAppStopped(changingConfigurations = false)

    private fun TestScope.backToApp(controller: NetworkDialogController) {
        controller.onAppStarted()
        runCurrent()
    }

    private fun infoMessages(): List<String> =
        logger.entries.filter { it.tag == "Network" && it.level == LogLevel.INFO }.map { it.message }

    private fun shownLines(): List<String> = infoMessages().filter { it.startsWith("Dialog shown") }

    @Test
    fun returnOffline_showsAfterGrace_beforeLostDelay() = runTest {
        val controller = controller()
        controller.toBackground()
        connectivity.connected.value = false
        backToApp(controller)

        advanceTimeBy(1_999)
        assertEquals(HIDDEN, controller.state.value)
        advanceTimeBy(2)
        assertEquals(SHOWN, controller.state.value)
        assertEquals(listOf("Dialog shown: offline_on_foreground"), shownLines())
    }

    @Test
    fun returnOffline_networkBackWithinGrace_showsNothing() = runTest {
        val controller = controller()
        controller.toBackground()
        connectivity.connected.value = false
        backToApp(controller)

        advanceTimeBy(500)
        connectivity.connected.value = true
        advanceTimeBy(5_000)
        assertEquals(HIDDEN, controller.state.value)
        assertEquals(emptyList<String>(), shownLines())
    }

    @Test
    fun returnOnline_showsNothing() = runTest {
        val controller = controller()
        controller.toBackground()
        backToApp(controller)

        advanceTimeBy(5_000)
        assertEquals(HIDDEN, controller.state.value)
    }

    @Test
    fun firstStart_isNotAReturn() = runTest {
        val controller = controller()
        connectivity.connected.value = false
        backToApp(controller)

        advanceTimeBy(2_500)
        assertEquals(HIDDEN, controller.state.value)
        // Control: the 3-second path still runs.
        advanceTimeBy(501)
        assertEquals(SHOWN, controller.state.value)
    }

    @Test
    fun configurationChange_isNotAReturn() = runTest {
        val controller = controller()
        controller.onAppStopped(changingConfigurations = true)
        connectivity.connected.value = false
        backToApp(controller)

        advanceTimeBy(2_500)
        assertEquals(HIDDEN, controller.state.value)
    }

    @Test
    fun secondStartWithoutStop_isNotAReturn() = runTest {
        val controller = controller()
        controller.toBackground()
        backToApp(controller)
        advanceTimeBy(2_500)

        connectivity.connected.value = false
        backToApp(controller)
        // 2 500 ms after the loss: a check would have shown at 2 001 ms, the 3-second path is not due yet.
        advanceTimeBy(2_500)
        assertEquals(HIDDEN, controller.state.value)
    }

    @Test
    fun returnOnLoadingScreen_ignored() = runTest {
        val controller = controller(active = false)
        controller.toBackground()
        connectivity.connected.value = false
        backToApp(controller)

        advanceTimeBy(5_000)
        assertEquals(HIDDEN, controller.state.value)
    }

    @Test
    fun returnWithFlagOff_ignored() = runTest {
        flags.enabled.value = emptySet()
        val controller = controller()
        controller.toBackground()
        connectivity.connected.value = false
        backToApp(controller)

        advanceTimeBy(5_000)
        assertEquals(HIDDEN, controller.state.value)
    }

    @Test
    fun flagTurnedOffDuringGrace_dropsShow() = runTest {
        val controller = controller()
        controller.toBackground()
        connectivity.connected.value = false
        backToApp(controller)

        advanceTimeBy(500)
        flags.enabled.value = emptySet()
        advanceTimeBy(100)
        flags.enabled.value = setOf(FeatureFlag.NETWORK_DIALOG)
        advanceTimeBy(1_500)
        // 2 100 ms: the check of the return was dropped, else the dialog would show at 2 000 ms.
        assertEquals(HIDDEN, controller.state.value)
        assertTrue(infoMessages().none { "offline_on_foreground" in it })

        // Control: the 3-second path starts again from the flag turned on at 600 ms.
        advanceTimeBy(1_501)
        assertEquals(SHOWN, controller.state.value)
        assertEquals(1, shownLines().count { "device_offline" in it })
    }

    @Test
    fun deactivatedDuringGrace_dropsShow() = runTest {
        val controller = controller()
        controller.toBackground()
        connectivity.connected.value = false
        backToApp(controller)

        advanceTimeBy(500)
        controller.setActive(false)
        advanceTimeBy(5_000)
        assertEquals(HIDDEN, controller.state.value)
        assertTrue(infoMessages().none { "offline_on_foreground" in it })
    }

    @Test
    fun returnOffline_andLostDelay_singleDialog() = runTest {
        val controller = controller()
        val seen = seenStates(controller)
        controller.toBackground()
        connectivity.connected.value = false
        backToApp(controller)

        advanceTimeBy(2_001)
        assertEquals(SHOWN, controller.state.value)
        assertTrue(infoMessages().contains("Dialog shown: offline_on_foreground"))
        advanceTimeBy(3_000)
        assertEquals(1, seen.count { it == SHOWN })
        assertEquals(1, shownLines().size)
    }

    @Test
    fun shownBeforeBackground_staysOnReturn() = runTest {
        val controller = controller()
        connectivity.connected.value = false
        advanceTimeBy(3_001)
        assertEquals(SHOWN, controller.state.value)

        controller.toBackground()
        backToApp(controller)
        advanceTimeBy(2_500)
        assertEquals(SHOWN, controller.state.value)
        assertEquals(1, shownLines().size)
    }

    @Test
    fun backgroundDuringGrace_dialogStaysOnReturn() = runTest {
        val controller = controller()
        controller.toBackground()
        connectivity.connected.value = false
        backToApp(controller)

        advanceTimeBy(500)
        controller.toBackground()
        advanceTimeBy(1_600)
        // 2 100 ms: the check keeps running in the background.
        assertEquals(SHOWN, controller.state.value)

        backToApp(controller)
        advanceTimeBy(2_500)
        assertEquals(SHOWN, controller.state.value)
        assertEquals(1, shownLines().size)
    }

    @Test
    fun returnOffline_whileAdShowing_waitsForAd() = runTest {
        val controller = controller()
        controller.toBackground()
        connectivity.connected.value = false
        backToApp(controller)

        advanceTimeBy(100)
        controller.setFullScreenAdShowing(true)
        advanceTimeBy(2_000)
        // 2 100 ms, before the 3-second path: the show of the return waits for the ad.
        assertEquals(HIDDEN, controller.state.value)
        assertTrue(infoMessages().contains("Dialog waits: full_screen_ad"))

        advanceTimeBy(3_000)
        assertEquals(HIDDEN, controller.state.value)
        controller.setFullScreenAdShowing(false)
        runCurrent()
        assertEquals(SHOWN, controller.state.value)
        assertEquals(1, shownLines().size)
    }

    @Test
    fun afterRecovery_nextReturnChecksAgain() = runTest {
        val controller = controller()
        controller.toBackground()
        connectivity.connected.value = false
        backToApp(controller)
        advanceTimeBy(2_001)
        assertEquals(SHOWN, controller.state.value)

        connectivity.connected.value = true
        controller.retry()
        runCurrent()
        assertEquals(HIDDEN, controller.state.value)

        controller.toBackground()
        connectivity.connected.value = false
        backToApp(controller)
        advanceTimeBy(2_001)
        assertEquals(SHOWN, controller.state.value)
    }

    // Reopened after the system closed the app in the background (spec network KB-3; plan T-078): the activity of a
    // new process is restored from saved state past loading, and the network is checked as on a return.

    /** A new process restored past loading: onCreate with saved state, onStart, then the restored screen. */
    private fun TestScope.restoredProcess(): NetworkDialogController {
        val controller = controller(active = false)
        controller.onAppRestored()
        backToApp(controller)
        controller.setActive(true)
        runCurrent()
        return controller
    }

    /** The activity recreated in the same process for a configuration change; the screen stays past loading. */
    private fun TestScope.recreate(controller: NetworkDialogController) {
        controller.onAppStopped(changingConfigurations = true)
        controller.onAppRestored()
        backToApp(controller)
        controller.setActive(true)
        runCurrent()
    }

    @Test
    fun restoredOffline_showsAfterGrace_beforeLostDelay() = runTest {
        connectivity.connected.value = false
        val controller = restoredProcess()

        advanceTimeBy(1_999)
        assertEquals(HIDDEN, controller.state.value)
        advanceTimeBy(2)
        assertEquals(SHOWN, controller.state.value)
        assertEquals(listOf("Dialog shown: offline_on_foreground"), shownLines())
    }

    @Test
    fun restoredOnline_showsNothing() = runTest {
        val controller = restoredProcess()

        advanceTimeBy(5_000)
        assertEquals(HIDDEN, controller.state.value)
        assertEquals(emptyList<String>(), shownLines())
    }

    @Test
    fun restoredNetworkBackWithinGrace_showsNothing() = runTest {
        connectivity.connected.value = false
        val controller = restoredProcess()

        advanceTimeBy(500)
        connectivity.connected.value = true
        advanceTimeBy(5_000)
        assertEquals(HIDDEN, controller.state.value)
        assertEquals(emptyList<String>(), shownLines())
    }

    @Test
    fun freshStart_isNotARestore() = runTest {
        connectivity.connected.value = false
        val controller = controller(active = false)
        backToApp(controller)
        controller.setActive(true)
        runCurrent()

        advanceTimeBy(2_500)
        assertEquals(HIDDEN, controller.state.value)
        // Control: the 3-second path still runs.
        advanceTimeBy(501)
        assertEquals(listOf("Dialog shown: device_offline delay=3s"), shownLines())
    }

    @Test
    fun restoredOnLoading_isNotAReturn() = runTest {
        val controller = controller(active = false)
        controller.onAppRestored()
        backToApp(controller)
        // The restored screen is loading.
        controller.setActive(false)
        runCurrent()
        connectivity.connected.value = false
        // Loading finishes.
        controller.setActive(true)
        runCurrent()

        advanceTimeBy(2_500)
        assertEquals(HIDDEN, controller.state.value)
        // Control: the 3-second path still runs.
        advanceTimeBy(501)
        assertEquals(listOf("Dialog shown: device_offline delay=3s"), shownLines())
    }

    @Test
    fun recreatedInSameProcess_isNotARestore_evenWhenFlagTurnsOnLater() = runTest {
        val controller = controller()
        backToApp(controller)
        flags.enabled.value = emptySet()
        // The flag off is seen before the recreation (pre-run review of RUN-20261030-02).
        runCurrent()
        recreate(controller)
        connectivity.connected.value = false
        flags.enabled.value = setOf(FeatureFlag.NETWORK_DIALOG)
        runCurrent()

        advanceTimeBy(2_500)
        assertEquals(HIDDEN, controller.state.value)
        // Control: the 3-second path still runs.
        advanceTimeBy(501)
        assertEquals(listOf("Dialog shown: device_offline delay=3s"), shownLines())
    }

    @Test
    fun recreatedInSameProcess_dialogStays() = runTest {
        val controller = controller()
        backToApp(controller)
        connectivity.connected.value = false
        advanceTimeBy(3_001)
        assertEquals(SHOWN, controller.state.value)

        recreate(controller)
        assertEquals(SHOWN, controller.state.value)
        assertTrue(infoMessages().none { it.startsWith("Dialog hidden") })
    }

    @Test
    fun activityDestroyedInBackground_isAReturn() = runTest {
        val controller = controller()
        backToApp(controller)
        controller.toBackground()
        controller.onAppRestored()
        connectivity.connected.value = false
        backToApp(controller)
        controller.setActive(true)
        runCurrent()

        advanceTimeBy(1_999)
        assertEquals(HIDDEN, controller.state.value)
        advanceTimeBy(2)
        assertEquals(listOf("Dialog shown: offline_on_foreground"), shownLines())
    }

    @Test
    fun restoredRecreatedDuringGrace_showsOnce() = runTest {
        connectivity.connected.value = false
        val controller = restoredProcess()

        advanceTimeBy(500)
        recreate(controller)
        advanceTimeBy(1_501)
        assertEquals(SHOWN, controller.state.value)
        advanceTimeBy(5_000)
        assertEquals(listOf("Dialog shown: offline_on_foreground"), shownLines())
    }

    @Test
    fun restoredWithFlagOff_ignored_evenWhenFlagTurnsOnLater() = runTest {
        flags.enabled.value = emptySet()
        connectivity.connected.value = false
        val controller = restoredProcess()

        advanceTimeBy(5_000)
        assertEquals(HIDDEN, controller.state.value)
        flags.enabled.value = setOf(FeatureFlag.NETWORK_DIALOG)
        runCurrent()
        advanceTimeBy(2_500)
        assertEquals(HIDDEN, controller.state.value)
        // Control: the 3-second path starts from the flag turned on.
        advanceTimeBy(501)
        assertEquals(listOf("Dialog shown: device_offline delay=3s"), shownLines())
    }

    @Test
    fun restoredDeactivatedDuringGrace_dropsShow() = runTest {
        connectivity.connected.value = false
        val controller = restoredProcess()

        advanceTimeBy(500)
        controller.setActive(false)
        advanceTimeBy(5_000)
        assertEquals(HIDDEN, controller.state.value)
        assertTrue(infoMessages().none { "offline_on_foreground" in it })
    }

    @Test
    fun restoredOffline_andLostDelay_singleDialog() = runTest {
        connectivity.connected.value = false
        val controller = restoredProcess()

        advanceTimeBy(5_001)
        assertEquals(SHOWN, controller.state.value)
        assertEquals(listOf("Dialog shown: offline_on_foreground"), shownLines())
    }

    @Test
    fun restoredBackgroundDuringGrace_dialogStays() = runTest {
        connectivity.connected.value = false
        val controller = restoredProcess()

        advanceTimeBy(500)
        controller.toBackground()
        advanceTimeBy(1_600)
        // 2 100 ms: the check keeps running in the background.
        assertEquals(SHOWN, controller.state.value)
    }

    private companion object {
        val HIDDEN = NetworkDialogState.Hidden
        val SHOWN = NetworkDialogState.Shown(checking = false, stillOffline = false)
        val CHECKING = NetworkDialogState.Shown(checking = true, stillOffline = false)
        val STILL_OFFLINE = NetworkDialogState.Shown(checking = false, stillOffline = true)
    }
}
