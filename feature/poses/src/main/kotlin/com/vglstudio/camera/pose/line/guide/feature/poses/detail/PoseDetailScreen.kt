package com.vglstudio.camera.pose.line.guide.feature.poses.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel

object PoseDetailTestTags {
    const val SCREEN = "pose_detail_screen"
    const val IMAGE = "pose_detail_image"
    const val BOTTOM_BAR = "pose_detail_bottom_bar"
}

@Composable
fun PoseDetailRoute(
    onBack: () -> Unit,
    onShootWithPose: (String) -> Unit,
    viewModel: PoseDetailViewModel = hiltViewModel(),
) {
    PoseDetailScreen(
        uiState = PoseDetailUiState.Loading,
        onBack = onBack,
        onToggleSave = viewModel::onToggleSave,
        onRetryImage = viewModel::onRetryImage,
        onShoot = { viewModel.onShoot()?.let(onShootWithPose) },
        onSaveFailedShown = viewModel::onSaveFailedShown,
    )
}

/** Empty frame (plan T-022, step 3): only the box that carries the tag. */
@Composable
fun PoseDetailScreen(
    uiState: PoseDetailUiState,
    onBack: () -> Unit,
    onToggleSave: () -> Unit,
    onRetryImage: () -> Unit,
    onShoot: () -> Unit,
    onSaveFailedShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.testTag(PoseDetailTestTags.SCREEN))
}
