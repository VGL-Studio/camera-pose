package com.vglstudio.camera.pose.line.guide.feature.poses

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseFilter

object PosesTestTags {
    const val SCREEN = "poses_screen"
    const val GRID = "poses_grid"

    fun card(id: String) = "poses_card_$id"
}

@Composable
fun PosesRoute(
    onBack: () -> Unit,
    onOpenSettings: (() -> Unit)?,
    onOpenPose: ((String) -> Unit)?,
    viewModel: PosesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PosesScreen(
        uiState = uiState,
        gridState = rememberLazyGridState(),
        onBack = onBack,
        onOpenSettings = onOpenSettings,
        onOpenPose = onOpenPose,
        onSelectFilter = viewModel::onSelectFilter,
        onShowAll = viewModel::onShowAll,
        onToggleSave = viewModel::onToggleSave,
        onRetry = viewModel::onRetry,
        onThumbnailNeeded = viewModel::onThumbnailNeeded,
        onThumbnailReleased = viewModel::onThumbnailReleased,
        onRetryThumbnail = viewModel::onRetryThumbnail,
        onSaveFailedShown = viewModel::onSaveFailedShown,
    )
}

// Step 3 skeleton (plan T-021): an empty box.
@Composable
fun PosesScreen(
    uiState: PosesUiState,
    gridState: LazyGridState,
    onBack: () -> Unit,
    onOpenSettings: (() -> Unit)?,
    onOpenPose: ((String) -> Unit)?,
    onSelectFilter: (PoseFilter) -> Unit,
    onShowAll: () -> Unit,
    onToggleSave: (String) -> Unit,
    onRetry: () -> Unit,
    onThumbnailNeeded: (String) -> Unit,
    onThumbnailReleased: (String) -> Unit,
    onRetryThumbnail: (String) -> Unit,
    onSaveFailedShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.testTag(PosesTestTags.SCREEN))
}
