package com.vglstudio.camera.pose.line.guide.feature.poses

import androidx.compose.ui.graphics.ImageBitmap
import com.vglstudio.camera.pose.line.guide.core.poses.LoadFailureReason
import com.vglstudio.camera.pose.line.guide.core.poses.model.LocalizedList
import com.vglstudio.camera.pose.line.guide.core.poses.model.LocalizedText
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseFilter
import com.vglstudio.camera.pose.line.guide.core.poses.model.SubjectType

/** Every state of the pose library screen (spec poses, KB-1, KB-2, KB-3, KB-5). */
sealed interface PosesUiState {
    /** The flag is not known yet, the catalog is loading, or it failed for lack of network under the network dialog. */
    data object Loading : PosesUiState

    /** The flag is off: the screen goes back to the camera. */
    data object Disabled : PosesUiState

    /** The catalog could not be read. */
    data class Error(val reason: LoadFailureReason) : PosesUiState

    /** [cards] empty with [selected] Saved is the "nothing saved" state. */
    data class Ready(
        val totalPoses: Int,
        /** All, Saved, then the categories in catalog order. */
        val chips: List<PoseChip>,
        val selected: PoseFilter,
        val cards: List<PoseCardItem>,
        val saveFailed: Boolean = false,
    ) : PosesUiState
}

sealed interface PoseChip {
    data object All : PoseChip

    data object Saved : PoseChip

    /** The name is resolved by the screen, in the language of the screen. */
    data class Category(val id: String, val name: LocalizedText) : PoseChip
}

data class PoseCardItem(
    val id: String,
    val categoryId: String,
    val name: LocalizedText,
    val subjectType: SubjectType,
    /** Counted by the screen in its language, as the name (review of T-021). */
    val steps: LocalizedList,
    val saved: Boolean,
    val thumbnail: ThumbnailState,
)

sealed interface ThumbnailState {
    data object Loading : ThumbnailState

    data class Ready(val image: ImageBitmap) : ThumbnailState

    /** The card keeps its skeleton; a tap loads the image again (spec poses, section 7). */
    data object Failed : ThumbnailState
}
