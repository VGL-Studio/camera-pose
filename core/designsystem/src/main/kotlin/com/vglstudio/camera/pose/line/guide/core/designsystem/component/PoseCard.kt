package com.vglstudio.camera.pose.line.guide.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter

/** Test tag of the save button of a [PoseCard] (its 48 dp touch area). */
const val POSE_CARD_SAVE_TAG = "pose_card_save"

/** Test tag of the 36 dp circle of the save button. */
const val POSE_CARD_SAVE_CIRCLE_TAG = "pose_card_save_circle"

/** Test tag of the image area of a [PoseCard] or a [PoseCardSkeleton]. */
const val POSE_CARD_IMAGE_TAG = "pose_card_image"

// Step 3 skeleton (plan T-021).
@Composable
fun PoseCard(
    name: String,
    meta: String,
    image: Painter,
    imageBackground: Color,
    saved: Boolean,
    saveLabel: String,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Box(modifier)
}

// Step 3 skeleton (plan T-021).
@Composable
fun PoseCardSkeleton(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, onClickLabel: String? = null) {
    Box(modifier)
}
