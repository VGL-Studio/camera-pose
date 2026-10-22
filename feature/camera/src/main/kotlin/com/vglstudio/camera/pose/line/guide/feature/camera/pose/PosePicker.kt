package com.vglstudio.camera.pose.line.guide.feature.camera.pose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vglstudio.camera.pose.line.guide.feature.camera.POSE_AREA_HEIGHT

/** Callbacks of the pose part of the camera; each does nothing by default (previews and tests of T-013). */
class PoseActions(
    val onCategorySelect: (String?) -> Unit = {},
    val onPoseSelect: (String) -> Unit = {},
    val onThumbShown: (String) -> Unit = {},
    val onPreviousStep: () -> Unit = {},
    val onNextStep: () -> Unit = {},
    val onHideSteps: () -> Unit = {},
    val onShowSteps: () -> Unit = {},
    val onRetryOutline: () -> Unit = {},
    val onRetryLibrary: () -> Unit = {},
)

@Suppress("UNUSED_PARAMETER")
@Composable
internal fun PosePicker(state: PoseUiState, actions: PoseActions, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(POSE_AREA_HEIGHT))
}
