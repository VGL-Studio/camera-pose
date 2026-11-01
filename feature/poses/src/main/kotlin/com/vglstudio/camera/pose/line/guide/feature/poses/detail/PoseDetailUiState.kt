package com.vglstudio.camera.pose.line.guide.feature.poses.detail

import androidx.compose.ui.graphics.ImageBitmap
import com.vglstudio.camera.pose.line.guide.core.poses.model.Framing
import com.vglstudio.camera.pose.line.guide.core.poses.model.LocalizedList
import com.vglstudio.camera.pose.line.guide.core.poses.model.LocalizedText
import com.vglstudio.camera.pose.line.guide.core.poses.model.SubjectType

/** Every state of the pose detail screen (spec poses, KB-3, KB-4, KB-5; plan T-022). */
sealed interface PoseDetailUiState {
    /** The flag is not known yet, or the catalog is loading. */
    data object Loading : PoseDetailUiState

    /** The flag is off: the screen goes back. */
    data object Disabled : PoseDetailUiState

    /** The pose is not in the catalog any more, or the catalog failed: the screen goes back. */
    data object Missing : PoseDetailUiState

    data class Ready(
        val pose: PoseDetail,
        val saved: Boolean,
        val image: DetailImageState,
        val saveFailed: Boolean = false,
        /** A premium pose not unlocked yet: the main button unlocks it (spec poses, KB-8). */
        val locked: Boolean = false,
        /** The unlock could not be written: the screen shows a message once. */
        val unlockFailed: Boolean = false,
    ) : PoseDetailUiState
}

/** Texts stay in all languages: the screen resolves them in its own language, as the library does. */
data class PoseDetail(
    val id: String,
    val categoryId: String,
    val name: LocalizedText,
    /** Null when the catalog has no name for the category: the second line leaves it out. */
    val categoryName: LocalizedText?,
    val subjectType: SubjectType,
    val framing: Framing,
    val steps: LocalizedList,
    val mistakes: LocalizedList,
)

sealed interface DetailImageState {
    data object Loading : DetailImageState

    data class Ready(val image: ImageBitmap) : DetailImageState

    /** The image area shows the message and "Thử lại"; the rest of the screen stays. */
    data object Failed : DetailImageState
}
