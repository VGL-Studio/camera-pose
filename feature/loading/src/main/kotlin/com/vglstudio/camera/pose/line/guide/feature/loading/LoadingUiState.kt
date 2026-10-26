package com.vglstudio.camera.pose.line.guide.feature.loading

/** Everything the loading screen needs to draw itself. [progress] is between 0 and 1. */
sealed interface LoadingUiState {
    data class InProgress(val progress: Float) : LoadingUiState

    /** At least one task could not be done. The screen offers "Retry" and a guidance line chosen by [cause]. */
    data class Failed(val progress: Float, val cause: LoadingFailure = LoadingFailure.OFFLINE) : LoadingUiState

    /** Everything is ready (or the feature is off): the app moves on to the start screen. */
    data object Finished : LoadingUiState
}

/**
 * Why a task could not be done, read from the system network state at the moment of the failure (plan T-057).
 * The type of the download error is not enough: an unknown host is a network error even when the device is online.
 */
enum class LoadingFailure {
    /** No network, or the system has not confirmed that the network reaches the Internet: "check your network". */
    OFFLINE,

    /** The system confirmed that the device reaches the Internet, so the server did not answer: "try again later". */
    SERVER,
}
