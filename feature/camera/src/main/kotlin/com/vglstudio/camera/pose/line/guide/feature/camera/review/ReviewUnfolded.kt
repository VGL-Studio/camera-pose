package com.vglstudio.camera.pose.line.guide.feature.camera.review

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import com.vglstudio.camera.pose.line.guide.feature.camera.review.ReviewUiState.Ready

/** The unfolded layout of what is under the top bar of the review screen (plan T-020). */
@Composable
internal fun ReviewUnfoldedContent(
    ready: Ready,
    image: ImageBitmap?,
    onSaveClick: () -> Unit,
    onRetrySave: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onRetakeClick: () -> Unit,
    onShareClick: () -> Unit,
    onNextPoseClick: () -> Unit,
) {
    ReviewPhoneContent(
        ready = ready,
        image = image,
        onSaveClick = onSaveClick,
        onRetrySave = onRetrySave,
        onOpenAppSettings = onOpenAppSettings,
        onRetakeClick = onRetakeClick,
        onShareClick = onShareClick,
        onNextPoseClick = onNextPoseClick,
    )
}
