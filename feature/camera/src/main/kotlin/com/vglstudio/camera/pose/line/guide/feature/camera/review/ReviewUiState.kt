package com.vglstudio.camera.pose.line.guide.feature.camera.review

/** What the review screen shows (spec camera, KB-6). */
sealed interface ReviewUiState {
    /** Reading the photo to show. */
    data object Loading : ReviewUiState

    data class Ready(
        /** File path of the photo just taken, or content uri of a saved photo. */
        val location: String,
        val save: SaveStatus,
        /** Non-null while "Bỏ ảnh này?" is open: what to do after the answer. */
        val pendingExit: ExitAction? = null,
    ) : ReviewUiState
}

enum class SaveStatus {
    NOT_SAVED,
    SAVING,
    SAVED,
    FAILED_STORAGE_FULL,
    FAILED,

    /** Android 9 or lower: the storage permission was denied, asking again is possible (plan T-016, version 2). */
    PERMISSION_DENIED,

    /** Android 9 or lower: denied with "don't ask again"; only the system settings can grant it. */
    PERMISSION_BLOCKED,
}

/** The ways out of the review screen; each one asks "Bỏ ảnh này?" while the photo is not saved. */
enum class ExitAction(val logValue: String) {
    RETAKE("retake"),
    NEXT_POSE("next_pose"),
    CLOSE("close"),
}

sealed interface ReviewEvent {
    /** Back to the camera. [savedThisVisit] is true when this visit saved the photo (T-018, T-019 build on it). */
    data class Leave(val savedThisVisit: Boolean) : ReviewEvent

    /** Opens the system share sheet: a cache file path, or the content uri of the saved photo. */
    data class Share(val location: String) : ReviewEvent

    data object RequestStoragePermission : ReviewEvent

    data object OpenAppSettings : ReviewEvent
}
