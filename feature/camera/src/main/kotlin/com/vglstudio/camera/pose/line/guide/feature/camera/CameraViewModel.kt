package com.vglstudio.camera.pose.line.guide.feature.camera

import androidx.camera.core.SurfaceRequest
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.vglstudio.camera.pose.line.guide.core.analytics.Analytics
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.feature.camera.camera.CameraController
import com.vglstudio.camera.pose.line.guide.feature.camera.capture.CapturedPhotoHolder
import com.vglstudio.camera.pose.line.guide.feature.camera.data.CameraSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

/** Returns the wall-clock time in milliseconds. A fun interface so tests pass a fixed time. */
fun interface CaptureClock {
    fun nowMillis(): Long
}

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val controller: CameraController,
    private val repository: CameraSettingsRepository,
    private val holder: CapturedPhotoHolder,
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

    fun onScreenStarted(permissionGranted: Boolean) = Unit

    fun onPermissionResult(granted: Boolean) = Unit

    fun onScreenStopped() = Unit

    fun onRetry() = Unit

    fun onFlashClick() = Unit

    fun onTimerClick() = Unit

    fun onGridClick() = Unit

    fun onFlipClick() = Unit

    fun onZoomSelect(ratio: Float) = Unit

    fun onShutterClick() = Unit

    fun onCancelCountdown() = Unit

    internal companion object {
        const val LOG_TAG = "Camera"
        const val EVENT_PERMISSION_RESULT = "camera_permission_result"
        const val EVENT_PHOTO_CAPTURED = "photo_captured"
    }
}
