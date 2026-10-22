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

    private companion object {
        val HIDDEN = NetworkDialogState.Hidden
        val SHOWN = NetworkDialogState.Shown(checking = false, stillOffline = false)
        val CHECKING = NetworkDialogState.Shown(checking = true, stillOffline = false)
        val STILL_OFFLINE = NetworkDialogState.Shown(checking = false, stillOffline = true)
    }
}
