package com.vglstudio.camera.pose.line.guide.feature.camera

import androidx.lifecycle.SavedStateHandle
import com.vglstudio.camera.pose.line.guide.core.analytics.AnalyticsEvent
import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import com.vglstudio.camera.pose.line.guide.feature.camera.CameraUiState.Ready
import com.vglstudio.camera.pose.line.guide.feature.camera.camera.CameraCapabilities
import com.vglstudio.camera.pose.line.guide.feature.camera.capture.CapturedPhoto
import com.vglstudio.camera.pose.line.guide.feature.camera.capture.CapturedPhotoHolder
import com.vglstudio.camera.pose.line.guide.feature.camera.model.CameraSettings
import com.vglstudio.camera.pose.line.guide.feature.camera.model.FlashMode
import com.vglstudio.camera.pose.line.guide.feature.camera.model.Lens
import com.vglstudio.camera.pose.line.guide.feature.camera.model.TimerSetting
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CameraViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val controller = FakeCameraController()
    private val repository = FakeCameraSettingsRepository()
    private val holder = CapturedPhotoHolder()
    private val interruptions = FakeCountdownInterruptions()
    private val analytics = FakeAnalytics()
    private val logger = RecordingAppLogger()
    private val events = mutableListOf<CameraEvent>()

    private fun TestScope.viewModel(savedState: SavedStateHandle = SavedStateHandle()): CameraViewModel {
        val viewModel = CameraViewModel(
            controller = controller,
            repository = repository,
            holder = holder,
            countdownInterruptions = interruptions,
            analytics = analytics,
            logger = logger,
            savedStateHandle = savedState,
            clock = { 1_000L },
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.collect { events += it } }
        return viewModel
    }

    private fun TestScope.readyViewModel(): CameraViewModel = viewModel().also {
        it.onScreenStarted(permissionGranted = true)
        runCurrent()
    }

    private fun CameraViewModel.ready(): Ready = uiState.value as Ready

    private fun capturedEvent(timer: String) = AnalyticsEvent(
        name = "photo_captured",
        params = mapOf("pose_id" to "", "timer" to timer, "matched" to "false"),
    )

    // KB-8

    @Test
    fun started_withPermission_opensBackCamera_thenReady() = runTest {
        val viewModel = readyViewModel()
        assertEquals(listOf(Lens.BACK), controller.openCalls)
        assertEquals(
            Ready(
                flash = FlashMode.OFF,
                flashAvailable = true,
                timer = TimerSetting.OFF,
                gridVisible = false,
                lens = Lens.BACK,
                zoomLevels = listOf(1f, 2f),
                zoom = 1f,
            ),
            viewModel.uiState.value,
        )
    }

    @Test
    fun openFails_showsError_thenRetryOpensAgain() = runTest {
        controller.openError = IllegalStateException("camera in use")
        val viewModel = readyViewModel()
        assertEquals(CameraUiState.Error, viewModel.uiState.value)
        assertTrue(logger.entries.any { it.level == LogLevel.ERROR && it.tag == "Camera" })

        controller.openError = null
        viewModel.onRetry()
        runCurrent()
        assertTrue(viewModel.uiState.value is Ready)
        assertEquals(2, controller.openCalls.size)
        assertTrue(logger.entries.any { it.message.contains("Retrying camera open") })
    }

    @Test
    fun stoppedThenStarted_closesThenReopensWithSameTools() = runTest {
        val viewModel = readyViewModel()
        viewModel.onGridClick()
        viewModel.onTimerClick()
        runCurrent()

        viewModel.onScreenStopped()
        assertEquals(1, controller.closeCount)
        viewModel.onScreenStarted(permissionGranted = true)
        runCurrent()
        assertEquals(2, controller.openCalls.size)
        assertTrue(viewModel.ready().gridVisible)
        assertEquals(TimerSetting.THREE, viewModel.ready().timer)
    }

    // KB-7

    @Test
    fun started_withoutPermission_requestsOnce() = runTest {
        val viewModel = viewModel()
        viewModel.onScreenStarted(permissionGranted = false)
        runCurrent()
        assertEquals(listOf<CameraEvent>(CameraEvent.RequestPermission), events)
        assertEquals(CameraUiState.Starting, viewModel.uiState.value)

        // Back from the background without an answer: no second prompt.
        viewModel.onScreenStarted(permissionGranted = false)
        runCurrent()
        assertEquals(listOf<CameraEvent>(CameraEvent.RequestPermission), events)
        assertEquals(CameraUiState.PermissionDenied, viewModel.uiState.value)
    }

    @Test
    fun askPermissionFalse_showsDeniedWithoutRequest() = runTest {
        val viewModel = viewModel(SavedStateHandle(mapOf("askPermission" to false)))
        viewModel.onScreenStarted(permissionGranted = false)
        runCurrent()
        assertEquals(CameraUiState.PermissionDenied, viewModel.uiState.value)
        assertTrue(events.isEmpty())
    }

    @Test
    fun permissionGranted_logsEvent_andOpens() = runTest {
        val viewModel = viewModel()
        viewModel.onPermissionResult(granted = true)
        runCurrent()
        assertEquals(
            listOf(
                AnalyticsEvent("camera_permission_result", mapOf("result" to "granted", "source" to "camera")),
            ),
            analytics.events,
        )
        assertTrue(viewModel.uiState.value is Ready)
    }

    @Test
    fun permissionDenied_logsEvent_andShowsDenied() = runTest {
        val viewModel = viewModel()
        viewModel.onPermissionResult(granted = false)
        runCurrent()
        assertEquals(
            listOf(AnalyticsEvent("camera_permission_result", mapOf("result" to "denied", "source" to "camera"))),
            analytics.events,
        )
        assertEquals(CameraUiState.PermissionDenied, viewModel.uiState.value)
        assertTrue(controller.openCalls.isEmpty())
    }

    @Test
    fun deniedThenStartedWithPermission_opensWithoutNewEvent() = runTest {
        val viewModel = viewModel()
        viewModel.onPermissionResult(granted = false)
        runCurrent()
        assertEquals(CameraUiState.PermissionDenied, viewModel.uiState.value)

        // The user allowed the camera in the system settings and came back.
        viewModel.onScreenStarted(permissionGranted = true)
        runCurrent()
        assertTrue(viewModel.uiState.value is Ready)
        assertEquals(1, analytics.events.size)
    }

    // KB-4

    @Test
    fun flash_cyclesOffOnAuto_andIsSaved() = runTest {
        val viewModel = readyViewModel()
        val seen = mutableListOf<FlashMode>()
        repeat(3) {
            viewModel.onFlashClick()
            runCurrent()
            seen += viewModel.ready().flash
        }
        assertEquals(listOf(FlashMode.ON, FlashMode.AUTO, FlashMode.OFF), seen)
        assertEquals(listOf(FlashMode.ON, FlashMode.AUTO, FlashMode.OFF), controller.flashCalls)
        assertEquals(listOf(FlashMode.ON, FlashMode.AUTO, FlashMode.OFF), repository.flashSaves)
    }

    @Test
    fun flash_unavailable_ignoresClicks() = runTest {
        controller.capabilities = CameraCapabilities(hasFlash = false, minZoomRatio = 1f, maxZoomRatio = 8f)
        val viewModel = readyViewModel()
        assertFalse(viewModel.ready().flashAvailable)
        viewModel.onFlashClick()
        runCurrent()
        assertEquals(FlashMode.OFF, viewModel.ready().flash)
        assertTrue(controller.flashCalls.isEmpty())
    }

    @Test
    fun timer_cyclesOffThreeTen_andIsSaved() = runTest {
        val viewModel = readyViewModel()
        val seen = mutableListOf<TimerSetting>()
        repeat(3) {
            viewModel.onTimerClick()
            runCurrent()
            seen += viewModel.ready().timer
        }
        assertEquals(listOf(TimerSetting.THREE, TimerSetting.TEN, TimerSetting.OFF), seen)
        assertEquals(listOf(TimerSetting.THREE, TimerSetting.TEN, TimerSetting.OFF), repository.timerSaves)
    }

    @Test
    fun grid_toggles_andIsSaved() = runTest {
        val viewModel = readyViewModel()
        viewModel.onGridClick()
        runCurrent()
        assertTrue(viewModel.ready().gridVisible)
        viewModel.onGridClick()
        runCurrent()
        assertFalse(viewModel.ready().gridVisible)
        assertEquals(listOf(true, false), repository.gridSaves)
    }

    @Test
    fun flip_switchesLens_resetsZoom_keepsTools() = runTest {
        val viewModel = readyViewModel()
        viewModel.onTimerClick()
        viewModel.onGridClick()
        viewModel.onZoomSelect(2f)
        runCurrent()
        assertEquals(2f, viewModel.ready().zoom)

        viewModel.onFlipClick()
        runCurrent()
        assertEquals(listOf(Lens.BACK, Lens.FRONT), controller.openCalls)
        assertEquals(Lens.FRONT, viewModel.ready().lens)
        assertEquals(1f, viewModel.ready().zoom)
        assertEquals(TimerSetting.THREE, viewModel.ready().timer)
        assertTrue(viewModel.ready().gridVisible)
        assertEquals(listOf(Lens.FRONT), repository.lensSaves)
    }

    @Test
    fun zoomLevels_withUltraWide_include05() = runTest {
        controller.capabilities = CameraCapabilities(hasFlash = true, minZoomRatio = 0.5f, maxZoomRatio = 10f)
        val viewModel = readyViewModel()
        assertEquals(listOf(0.5f, 1f, 2f), viewModel.ready().zoomLevels)
        viewModel.onZoomSelect(0.5f)
        runCurrent()
        assertEquals(listOf(0.5f), controller.zoomCalls)
        assertEquals(0.5f, viewModel.ready().zoom)
    }

    @Test
    fun zoomLevels_withoutUltraWide_are1And2() = runTest {
        controller.capabilities = CameraCapabilities(hasFlash = true, minZoomRatio = 1f, maxZoomRatio = 4f)
        val viewModel = readyViewModel()
        assertEquals(listOf(1f, 2f), viewModel.ready().zoomLevels)
        viewModel.onZoomSelect(0.5f)
        runCurrent()
        assertTrue(controller.zoomCalls.isEmpty())
        assertEquals(1f, viewModel.ready().zoom)
    }

    @Test
    fun zoomLevels_maxBelow2_onlyOne() = runTest {
        controller.capabilities = CameraCapabilities(hasFlash = true, minZoomRatio = 1f, maxZoomRatio = 1.5f)
        val viewModel = readyViewModel()
        assertEquals(listOf(1f), viewModel.ready().zoomLevels)
    }

    @Test
    fun savedTools_restored_zoomBackTo1x() = runTest {
        repository.stored.value = CameraSettings(FlashMode.AUTO, TimerSetting.TEN, gridVisible = true, lens = Lens.FRONT)
        val viewModel = readyViewModel()
        assertEquals(listOf(Lens.FRONT), controller.openCalls)
        val ready = viewModel.ready()
        assertEquals(FlashMode.AUTO, ready.flash)
        assertEquals(TimerSetting.TEN, ready.timer)
        assertTrue(ready.gridVisible)
        assertEquals(Lens.FRONT, ready.lens)
        assertEquals(1f, ready.zoom)
    }

    @Test
    fun saveFails_keepsChoice_andLogsWarning() = runTest {
        repository.failWrites = true
        val viewModel = readyViewModel()
        viewModel.onGridClick()
        runCurrent()
        assertTrue(viewModel.ready().gridVisible)
        assertTrue(logger.entries.any { it.level == LogLevel.WARN && it.error is IOException })
    }

    // KB-6

    @Test
    fun shutter_timerOff_capturesAtOnce() = runTest {
        val viewModel = readyViewModel()
        viewModel.onShutterClick()
        runCurrent()
        assertEquals(1, controller.captureCount)
        assertEquals(listOf(capturedEvent("off")), analytics.events)
        assertEquals(CapturedPhoto(File("capture_1.jpg"), Lens.BACK, TimerSetting.OFF, 1_000L), holder.latest.value)
        assertEquals(listOf<CameraEvent>(CameraEvent.PhotoCaptured), events)
    }

    @Test
    fun shutter_timer3_countsDownThenCaptures() = runTest {
        val viewModel = readyViewModel()
        viewModel.onTimerClick()
        runCurrent()
        viewModel.onShutterClick()
        runCurrent()
        assertEquals(3, viewModel.ready().countdown)
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(2, viewModel.ready().countdown)
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(1, viewModel.ready().countdown)
        advanceTimeBy(999)
        runCurrent()
        assertEquals(0, controller.captureCount)
        advanceTimeBy(1)
        runCurrent()
        assertNull(viewModel.ready().countdown)
        assertEquals(1, controller.captureCount)
        assertEquals(listOf(capturedEvent("3")), analytics.events)
    }

    @Test
    fun cancelCountdown_stopsWithoutCapture() = runTest {
        repository.stored.value = CameraSettings(timer = TimerSetting.TEN)
        val viewModel = readyViewModel()
        viewModel.onShutterClick()
        advanceTimeBy(2_000)
        runCurrent()
        viewModel.onCancelCountdown()
        advanceTimeBy(20_000)
        runCurrent()
        assertEquals(0, controller.captureCount)
        assertNull(viewModel.ready().countdown)
        assertTrue(analytics.events.none { it.name == "photo_captured" })
    }

    @Test
    fun stopped_duringCountdown_cancels() = runTest {
        repository.stored.value = CameraSettings(timer = TimerSetting.THREE)
        val viewModel = readyViewModel()
        viewModel.onShutterClick()
        runCurrent()
        assertEquals(3, viewModel.ready().countdown)
        viewModel.onScreenStopped()
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(0, controller.captureCount)
    }

    @Test
    fun interruption_duringCountdown_cancels() = runTest {
        repository.stored.value = CameraSettings(timer = TimerSetting.TEN)
        val viewModel = readyViewModel()
        viewModel.onShutterClick()
        runCurrent()
        assertEquals(10, viewModel.ready().countdown)
        interruptions.value.value = true
        advanceTimeBy(20_000)
        runCurrent()
        assertEquals(0, controller.captureCount)
        assertNull(viewModel.ready().countdown)
        assertTrue(logger.entries.any { it.message.contains("interruption") })
    }

    @Test
    fun interruption_whenIdle_changesNothing() = runTest {
        val viewModel = readyViewModel()
        interruptions.value.value = true
        runCurrent()
        viewModel.onShutterClick()
        runCurrent()
        assertEquals(1, controller.captureCount)
    }

    @Test
    fun shutter_doubleTap_capturesOnce() = runTest {
        val pending = CompletableDeferred<Unit>()
        controller.pendingCapture = pending
        val viewModel = readyViewModel()
        viewModel.onShutterClick()
        runCurrent()
        viewModel.onShutterClick()
        runCurrent()
        assertEquals(1, controller.captureCount)
        assertTrue(viewModel.ready().capturing)

        pending.complete(Unit)
        runCurrent()
        assertFalse(viewModel.ready().capturing)
        assertEquals(1, controller.captureCount)
    }

    @Test
    fun captureFails_staysReady_withoutEvent() = runTest {
        controller.captureError = IllegalStateException("capture failed")
        val viewModel = readyViewModel()
        viewModel.onShutterClick()
        runCurrent()
        assertTrue(viewModel.uiState.value is Ready)
        assertFalse(viewModel.ready().capturing)
        assertTrue(analytics.events.none { it.name == "photo_captured" })
        assertTrue(logger.entries.any { it.level == LogLevel.WARN })
    }

    @Test
    fun shutter_ignoredWhenNotReady() = runTest {
        val viewModel = viewModel()
        viewModel.onPermissionResult(granted = false)
        runCurrent()
        assertEquals(CameraUiState.PermissionDenied, viewModel.uiState.value)
        viewModel.onShutterClick()
        runCurrent()
        assertEquals(0, controller.captureCount)
    }
}
