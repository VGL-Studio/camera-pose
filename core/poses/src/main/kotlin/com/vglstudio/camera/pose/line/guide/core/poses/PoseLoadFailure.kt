package com.vglstudio.camera.pose.line.guide.core.poses

import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseCatalog
import java.io.File

enum class PoseContent(val analyticsValue: String) {
    CATALOG("catalog"),
    THUMBNAIL("thumbnail"),
    IMAGE("image"),
    OUTLINE("outline"),
}

enum class LoadFailureReason(val analyticsValue: String) {
    NETWORK("network"),
    NOT_FOUND("not_found"),
    TIMEOUT("timeout"),
    OTHER("other"),
}

data class PoseLoadFailure(val content: PoseContent, val reason: LoadFailureReason)

sealed interface PoseFileResult {
    data class Ready(val file: File) : PoseFileResult

    data class Failed(val failure: PoseLoadFailure) : PoseFileResult
}

sealed interface CatalogState {
    data object NotLoaded : CatalogState

    data object Loading : CatalogState

    data class Ready(val catalog: PoseCatalog) : CatalogState

    data class Failed(val failure: PoseLoadFailure) : CatalogState
}

sealed interface StarterSetResult {
    data object Complete : StarterSetResult

    data class Incomplete(val failedFiles: Int, val failure: PoseLoadFailure) : StarterSetResult
}
