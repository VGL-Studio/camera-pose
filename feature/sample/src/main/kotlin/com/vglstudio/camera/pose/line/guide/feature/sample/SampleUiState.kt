package com.vglstudio.camera.pose.line.guide.feature.sample

/** Everything the sample screen needs to draw itself. */
sealed interface SampleUiState {
    data object Loading : SampleUiState

    data object Disabled : SampleUiState

    /** [saveFailed] is true when the last attempt to save failed (for example, the device storage is full). */
    data class Ready(val count: Int, val saveFailed: Boolean = false) : SampleUiState
}
