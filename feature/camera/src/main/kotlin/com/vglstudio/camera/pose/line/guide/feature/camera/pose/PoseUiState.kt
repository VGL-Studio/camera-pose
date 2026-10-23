package com.vglstudio.camera.pose.line.guide.feature.camera.pose

import androidx.compose.ui.graphics.ImageBitmap

/** The pose part of the camera: chip row, pose strip, outline and step card (spec camera, KB-1, KB-2). */
sealed interface PoseUiState {
    /** The catalog is loading (for example after the system restored the camera without the loading screen). */
    data object Loading : PoseUiState

    /** The catalog could not load: the pose area offers "Thử lại" (plan T-014, question 8). */
    data object Unavailable : PoseUiState

    data class Ready(
        /** Categories with poses, in catalog order, without "Tất cả", which the screen adds first. */
        val categories: List<CategoryChip>,
        /** Null: "Tất cả". */
        val selectedCategoryId: String?,
        /** Poses of the selected chip, in the order taken when the camera opened. */
        val strip: List<PoseThumbItem>,
        /** Null: there is no pose (the catalog has none). */
        val selected: SelectedPose?,
        val stepsHidden: Boolean,
        /** The strip scrolls to [StripScroll.index] once per new key. */
        val scrollRequest: StripScroll? = null,
    ) : PoseUiState
}

data class CategoryChip(val id: String, val label: String)

/** One cell of the strip; [label] is the pose name for screen readers. */
data class PoseThumbItem(val id: String, val label: String, val image: PoseImage)

data class SelectedPose(val id: String, val steps: List<String>, val stepIndex: Int, val outline: PoseImage)

data class StripScroll(val index: Int, val key: Int)

sealed interface PoseImage {
    data object Loading : PoseImage

    data class Ready(val bitmap: ImageBitmap) : PoseImage

    data object Failed : PoseImage
}
