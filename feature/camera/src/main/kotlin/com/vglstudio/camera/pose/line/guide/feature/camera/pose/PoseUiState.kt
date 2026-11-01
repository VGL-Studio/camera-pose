package com.vglstudio.camera.pose.line.guide.feature.camera.pose

import androidx.compose.ui.graphics.ImageBitmap
import com.vglstudio.camera.pose.line.guide.feature.camera.data.OverlaySettings

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
        val overlay: OverlayState = OverlayState(),
        /** The locked pose whose unlock dialog may show, so the dialog can name it (spec poses, KB-8). */
        val unlockPrompt: UnlockPrompt? = null,
    ) : PoseUiState
}

/** How the outline is adjusted (spec camera, KB-3). Offsets are parts of the frame from its centre: -0.5 to 0.5. */
data class OverlayState(
    val settings: OverlaySettings = OverlaySettings(),
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val panelOpen: Boolean = false,
    val lockHintVisible: Boolean = false,
)

data class CategoryChip(val id: String, val label: String)

/**
 * One cell of the strip; [label] is the pose name for screen readers. [locked]: a premium pose not unlocked yet, with
 * the premium badge; a tap opens the unlock dialog instead of selecting it (spec poses, KB-8).
 */
data class PoseThumbItem(val id: String, val label: String, val image: PoseImage, val locked: Boolean = false)

/** The pose waiting for its unlock, with its name for the dialog. */
data class UnlockPrompt(val id: String, val name: String)

data class SelectedPose(val id: String, val steps: List<String>, val stepIndex: Int, val outline: PoseImage)

data class StripScroll(val index: Int, val key: Int)

sealed interface PoseImage {
    data object Loading : PoseImage

    data class Ready(val bitmap: ImageBitmap) : PoseImage

    data object Failed : PoseImage
}
