package com.vglstudio.camera.pose.line.guide.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap

/** Test tag of the retry icon of a thumbnail that failed to load. */
const val POSE_THUMB_RETRY_TAG = "pose_thumb_retry"

@Suppress("UNUSED_PARAMETER")
@Composable
fun PoseThumb(
    image: ImageBitmap?,
    selected: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    failed: Boolean = false,
) {
    Box(modifier)
}
