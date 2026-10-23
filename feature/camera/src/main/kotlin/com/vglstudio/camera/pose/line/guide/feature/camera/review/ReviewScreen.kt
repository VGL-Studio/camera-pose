package com.vglstudio.camera.pose.line.guide.feature.camera.review

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag

/** Skeleton of plan T-016, step 3. */
@Composable
internal fun ReviewScreen(
    uiState: ReviewUiState,
    image: ImageBitmap?,
    onCloseClick: () -> Unit,
    onSaveClick: () -> Unit,
    onRetrySave: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onRetakeClick: () -> Unit,
    onShareClick: () -> Unit,
    onNextPoseClick: () -> Unit,
    onDiscardConfirm: () -> Unit,
    onDiscardSave: () -> Unit,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().testTag(ReviewTestTags.SCREEN))
}

internal object ReviewTestTags {
    const val SCREEN = "review_screen"
    const val CLOSE = "review_close"
    const val PHOTO = "review_photo"
    const val ACTIONS = "review_actions"
    const val RETAKE = "review_retake"
    const val SHARE = "review_share"
    const val NEXT_POSE = "review_next_pose"
    const val SAVE_ERROR = "review_save_error"
    const val SAVE = "review_save"
    const val DISCARD_DIALOG = "review_discard_dialog"
}
