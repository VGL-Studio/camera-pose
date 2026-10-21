package com.vglstudio.camera.pose.line.guide.feature.loading

/** Everything the loading screen needs to draw itself. [progress] is between 0 and 1. */
sealed interface LoadingUiState {
    data class InProgress(val progress: Float) : LoadingUiState

    /** At least one task could not be done. The screen offers "Retry". */
    data class Failed(val progress: Float) : LoadingUiState

    /** Everything is ready (or the feature is off): the app moves on to the start screen. */
    data object Finished : LoadingUiState
}
