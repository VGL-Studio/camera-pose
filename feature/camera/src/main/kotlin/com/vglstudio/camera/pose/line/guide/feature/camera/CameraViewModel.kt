package com.vglstudio.camera.pose.line.guide.feature.camera

import androidx.camera.core.SurfaceRequest
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vglstudio.camera.pose.line.guide.core.analytics.Analytics
import com.vglstudio.camera.pose.line.guide.core.analytics.AnalyticsEvent
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.feature.camera.camera.CameraController
import com.vglstudio.camera.pose.line.guide.feature.camera.camera.CameraOpenException
import com.vglstudio.camera.pose.line.guide.feature.camera.camera.CaptureFailedException
import com.vglstudio.camera.pose.line.guide.feature.camera.capture.CapturedPhoto
import com.vglstudio.camera.pose.line.guide.feature.camera.capture.CapturedPhotoHolder
import com.vglstudio.camera.pose.line.guide.feature.camera.data.CameraSettingsRepository
import com.vglstudio.camera.pose.line.guide.feature.camera.model.CameraSettings
import com.vglstudio.camera.pose.line.guide.feature.camera.model.TimerSetting
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.ActivePoseHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Returns the wall-clock time in milliseconds. A fun interface so tests pass a fixed time. */
fun interface CaptureClock {
    fun nowMillis(): Long
}

/**
 * The camera screen without poses (spec camera, KB-4 to KB-8): permission, opening, capture tools, capture and
 * countdown. The camera hardware is behind [CameraController]. Tools are read once from storage, then kept in memory
 * and saved on each change; a failed save is logged and the choice still works for this session.
 */
@HiltViewModel
class CameraViewModel @Inject constructor(
    private val controller: CameraController,
    private val repository: CameraSettingsRepository,
    private val holder: CapturedPhotoHolder,
    @Suppress("unused") private val activePose: ActivePoseHolder,
    private val countdownInterruptions: CountdownInterruptions,
    private val analytics: Analytics,
    private val logger: AppLogger,
    private val savedStateHandle: SavedStateHandle,
    private val clock: CaptureClock,
) : ViewModel() {
    private val state = MutableStateFlow<CameraUiState>(CameraUiState.Starting)
    val uiState: StateFlow<CameraUiState> = state.asStateFlow()

    private val eventChannel = Channel<CameraEvent>(Channel.BUFFERED)
    val events: Flow<CameraEvent> = eventChannel.receiveAsFlow()

    val surfaceRequest: StateFlow<SurfaceRequest?> = controller.surfaceRequest

    /** Tools in use; null until read from storage once per view model. */
    private var tools: CameraSettings? = null
    private var openJob: Job? = null
    private var countdownJob: Job? = null

    init {
        viewModelScope.launch {
            countdownInterruptions.active().collect { active ->
                if (active && countdownJob?.isActive == true) cancelCountdown(reason = "interruption")
            }
        }
    }

    /** Called each time the screen starts (also back from the background or from the system settings). */
    fun onScreenStarted(permissionGranted: Boolean) {
        if (permissionGranted) {
            openCamera()
            return
        }
        val ask = savedStateHandle.get<Boolean>(KEY_ASK_PERMISSION) ?: true
        val alreadyAsked = savedStateHandle.get<Boolean>(KEY_PERMISSION_REQUESTED) ?: false
        if (ask && !alreadyAsked) {
            savedStateHandle[KEY_PERMISSION_REQUESTED] = true
            state.value = CameraUiState.Starting
            logger.info(LOG_TAG, "Asking for the camera permission")
            eventChannel.trySend(CameraEvent.RequestPermission)
        } else {
            logger.info(LOG_TAG, "No camera permission")
            state.value = CameraUiState.PermissionDenied
        }
    }

    fun onPermissionResult(granted: Boolean, dialogShown: Boolean = true) {
        if (!granted && !dialogShown) {
            // Permanently denied: the system answered without asking, so there is no user answer to record (KB-7).
            logger.info(LOG_TAG, "Camera permission denied without dialog")
            state.value = CameraUiState.PermissionDenied
            return
        }
        val result = if (granted) "granted" else "denied"
        analytics.log(AnalyticsEvent(EVENT_PERMISSION_RESULT, mapOf("result" to result, "source" to "camera")))
        logger.info(LOG_TAG, "Camera permission $result")
        if (granted) openCamera() else state.value = CameraUiState.PermissionDenied
    }

    fun onScreenStopped() {
        cancelCountdown(reason = "stopped")
        openJob?.cancel()
        controller.close()
        logger.info(LOG_TAG, "Camera closed")
    }

    fun onRetry() {
        logger.info(LOG_TAG, "Retrying camera open")
        openCamera()
    }

    fun onFlashClick() {
        val ready = state.value as? CameraUiState.Ready ?: return
        if (!ready.flashAvailable) return
        val flash = ready.flash.next()
        controller.setFlash(flash)
        updateTools { it.copy(flash = flash) }
        state.value = ready.copy(flash = flash)
        save { repository.setFlash(flash) }
    }

    fun onTimerClick() {
        val ready = state.value as? CameraUiState.Ready ?: return
        val timer = ready.timer.next()
        updateTools { it.copy(timer = timer) }
        state.value = ready.copy(timer = timer)
        save { repository.setTimer(timer) }
    }

    fun onGridClick() {
        val ready = state.value as? CameraUiState.Ready ?: return
        val visible = !ready.gridVisible
        updateTools { it.copy(gridVisible = visible) }
        state.value = ready.copy(gridVisible = visible)
        save { repository.setGridVisible(visible) }
    }

    /** Switches front and back; zoom goes back to 1x because each camera has its own zoom range. */
    fun onFlipClick() {
        val ready = state.value as? CameraUiState.Ready ?: return
        // A photo being taken keeps its camera: switching would reopen the camera under it.
        if (ready.capturing) return
        cancelCountdown(reason = "flip")
        // From the tools, not the state: the state keeps the old lens while the new camera opens.
        val lens = (tools?.lens ?: ready.lens).next()
        updateTools { it.copy(lens = lens) }
        save { repository.setLens(lens) }
        logger.info(LOG_TAG, "Switching to the ${lens.name.lowercase()} camera")
        openCamera(keepReady = true)
    }

    fun onZoomSelect(ratio: Float) {
        val ready = state.value as? CameraUiState.Ready ?: return
        if (ratio !in ready.zoomLevels) return
        controller.setZoom(ratio)
        state.value = ready.copy(zoom = ratio)
    }

    fun onShutterClick() {
        val ready = state.value as? CameraUiState.Ready ?: return
        if (ready.capturing || ready.countdown != null) return
        if (ready.timer == TimerSetting.OFF) {
            capture(ready.timer)
            return
        }
        val timer = ready.timer
        countdownJob = viewModelScope.launch {
            for (second in timer.seconds downTo 1) {
                updateReady { it.copy(countdown = second) }
                delay(MILLIS_PER_SECOND)
            }
            updateReady { it.copy(countdown = null) }
            capture(timer)
        }
    }

    fun onCancelCountdown() = cancelCountdown(reason = "user")

    override fun onCleared() {
        controller.release()
    }

    private fun openCamera(keepReady: Boolean = false) {
        openJob?.cancel()
        if (!keepReady || state.value !is CameraUiState.Ready) state.value = CameraUiState.Starting
        openJob = viewModelScope.launch {
            val tools = loadTools()
            val capabilities = try {
                controller.open(tools.lens, tools.flash)
            } catch (e: CameraOpenException) {
                logger.error(LOG_TAG, "Camera could not open", e)
                state.value = CameraUiState.Error
                return@launch
            }
            state.value = CameraUiState.Ready(
                flash = tools.flash,
                flashAvailable = capabilities.hasFlash,
                timer = tools.timer,
                gridVisible = tools.gridVisible,
                lens = tools.lens,
                zoomLevels = ZOOM_LEVELS.filter { it in capabilities.minZoomRatio..capabilities.maxZoomRatio },
                zoom = 1f,
            )
            logger.info(LOG_TAG, "Camera ready: ${tools.lens.name.lowercase()}, flash ${capabilities.hasFlash}")
        }
    }

    private fun capture(timer: TimerSetting) {
        val lens = (state.value as? CameraUiState.Ready)?.lens ?: return
        updateReady { it.copy(capturing = true) }
        viewModelScope.launch {
            try {
                val file = controller.takePicture()
                holder.put(CapturedPhoto(file, lens, timer, clock.nowMillis()))
                analytics.log(
                    AnalyticsEvent(
                        EVENT_PHOTO_CAPTURED,
                        // No pose and no match score yet: T-014 and T-017 fill these in.
                        mapOf(
                            "pose_id" to "",
                            "timer" to if (timer == TimerSetting.OFF) "off" else timer.seconds.toString(),
                            "matched" to "false",
                        ),
                    ),
                )
                logger.info(LOG_TAG, "Photo captured")
                eventChannel.trySend(CameraEvent.PhotoCaptured)
            } catch (e: CaptureFailedException) {
                logger.warn(LOG_TAG, "Capture failed", e)
            } finally {
                updateReady { it.copy(capturing = false) }
            }
        }
    }

    private fun cancelCountdown(reason: String) {
        if (countdownJob?.isActive != true) return
        countdownJob?.cancel()
        countdownJob = null
        updateReady { it.copy(countdown = null) }
        logger.info(LOG_TAG, "Countdown cancelled: $reason")
    }

    private suspend fun loadTools(): CameraSettings = tools ?: repository.settings.first().also { tools = it }

    private fun updateTools(change: (CameraSettings) -> CameraSettings) {
        tools = change(tools ?: CameraSettings())
    }

    private fun updateReady(change: (CameraUiState.Ready) -> CameraUiState.Ready) {
        state.update { if (it is CameraUiState.Ready) change(it) else it }
    }

    private fun save(write: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                write()
            } catch (e: IOException) {
                logger.warn(LOG_TAG, "Camera tools could not be saved", e)
            }
        }
    }

    internal companion object {
        const val LOG_TAG = "Camera"
        const val EVENT_PERMISSION_RESULT = "camera_permission_result"
        const val EVENT_PHOTO_CAPTURED = "photo_captured"
        const val KEY_ASK_PERMISSION = "askPermission"
        const val KEY_PERMISSION_REQUESTED = "permission_requested"
        const val MILLIS_PER_SECOND = 1_000L
        val ZOOM_LEVELS = listOf(0.5f, 1f, 2f)
    }
}
