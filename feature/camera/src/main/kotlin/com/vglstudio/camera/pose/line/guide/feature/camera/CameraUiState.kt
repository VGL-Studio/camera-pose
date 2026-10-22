package com.vglstudio.camera.pose.line.guide.feature.camera

import com.vglstudio.camera.pose.line.guide.feature.camera.model.FlashMode
import com.vglstudio.camera.pose.line.guide.feature.camera.model.Lens
import com.vglstudio.camera.pose.line.guide.feature.camera.model.TimerSetting

/** Every state of the camera screen (spec camera, KB-4 to KB-8). */
sealed interface CameraUiState {
    /** The camera is opening, or the permission prompt is showing. */
    data object Starting : CameraUiState

    data object PermissionDenied : CameraUiState

    data object Error : CameraUiState

    data class Ready(
        val flash: FlashMode,
        val flashAvailable: Boolean,
        val timer: TimerSetting,
        val gridVisible: Boolean,
        val lens: Lens,
        val zoomLevels: List<Float>,
        val zoom: Float,
        val countdown: Int? = null,
        val capturing: Boolean = false,
    ) : CameraUiState
}

/** One-off events for the route: ask the system for the permission, or open the review screen. */
sealed interface CameraEvent {
    data object RequestPermission : CameraEvent

    data object PhotoCaptured : CameraEvent
}
