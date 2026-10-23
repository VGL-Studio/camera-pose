package com.vglstudio.camera.pose.line.guide.feature.poses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vglstudio.camera.pose.line.guide.core.ads.AdPlacement
import com.vglstudio.camera.pose.line.guide.core.designsystem.R as DesignR
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PoseCardSkeleton
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.StateMessage
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseFilter
import kotlinx.coroutines.launch

object PosesTestTags {
    const val SCREEN = "poses_screen"
    const val GRID = "poses_grid"
    const val BANNER_SLOT = "poses_banner_slot"

    fun card(id: String) = "poses_card_$id"
}

// Skeleton cards while the catalog loads (plan T-021, question 13).
private const val LOADING_CARDS = 6

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
        // Saved with the back stack entry: the scroll position is kept when coming back from the detail (KB-2).
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
        bottomBanner = { PosesBottomBanner(AdPlacement.POSES_BANNER) },
    )
}

/**
 * The pose library (spec poses; design README). [onOpenSettings] null hides the settings button; [onOpenPose] null
 * leaves the cards without a tap action. [bottomBanner] sits under the grid, at the bottom of the screen, in every
 * state that shows the screen (KB-6).
 */
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
    bottomBanner: @Composable () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag(PosesTestTags.SCREEN),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PosesTopBar(onBack = onBack, onOpenSettings = onOpenSettings)
            when (uiState) {
                PosesUiState.Loading -> LoadingGrid(modifier = Modifier.weight(1f))
                PosesUiState.Disabled -> {
                    // The flag was turned off: back to the camera, once.
                    val currentOnBack by rememberUpdatedState(onBack)
                    LaunchedEffect(Unit) { currentOnBack() }
                }
                is PosesUiState.Error -> CenteredScrollable(modifier = Modifier.weight(1f)) {
                    StateMessage(
                        icon = painterResource(R.drawable.ic_poses_error),
                        title = stringResource(R.string.poses_error_title),
                        message = stringResource(R.string.poses_error_message),
                        actionLabel = stringResource(R.string.poses_error_action),
                        actionIcon = painterResource(DesignR.drawable.ic_refresh),
                        onAction = onRetry,
                    )
                }
                is PosesUiState.Ready -> ReadyContent(
                    state = uiState,
                    gridState = gridState,
                    onOpenPose = onOpenPose,
                    onSelectFilter = onSelectFilter,
                    onShowAll = onShowAll,
                    onToggleSave = onToggleSave,
                    onThumbnailNeeded = onThumbnailNeeded,
                    onThumbnailReleased = onThumbnailReleased,
                    onRetryThumbnail = onRetryThumbnail,
                )
            }
            bottomBanner()
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
    if ((uiState as? PosesUiState.Ready)?.saveFailed == true) {
        val message = stringResource(R.string.poses_save_failed)
        val currentOnShown by rememberUpdatedState(onSaveFailedShown)
        LaunchedEffect(Unit) {
            snackbarHostState.showSnackbar(message)
            currentOnShown()
        }
    }
}

@Composable
private fun ColumnScope.ReadyContent(
    state: PosesUiState.Ready,
    gridState: LazyGridState,
    onOpenPose: ((String) -> Unit)?,
    onSelectFilter: (PoseFilter) -> Unit,
    onShowAll: () -> Unit,
    onToggleSave: (String) -> Unit,
    onThumbnailNeeded: (String) -> Unit,
    onThumbnailReleased: (String) -> Unit,
    onRetryThumbnail: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    // A chip tapped by the user brings the grid back to its start, also once the new cards are there. Coming back from
    // the detail is not a tap, so the position is kept (KB-2).
    var scrollTopFor by remember { mutableStateOf<PoseFilter?>(null) }
    LaunchedEffect(state.selected, scrollTopFor) {
        if (scrollTopFor != null && scrollTopFor == state.selected) {
            gridState.scrollToItem(0)
            scrollTopFor = null
        }
    }
    Spacer(modifier = Modifier.height(Spacing.sm))
    Text(
        text = stringResource(R.string.poses_summary, state.totalPoses, state.chips.count { it is PoseChip.Category }),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = Spacing.md),
    )
    PoseChipRow(
        chips = state.chips,
        selected = state.selected,
        onSelect = { filter ->
            onSelectFilter(filter)
            scrollTopFor = filter
            scope.launch { gridState.scrollToItem(0) }
        },
    )
    if (state.cards.isEmpty() && state.selected == PoseFilter.Saved) {
        CenteredScrollable(modifier = Modifier.weight(1f)) {
            StateMessage(
                icon = painterResource(DesignR.drawable.ic_bookmark),
                title = stringResource(R.string.poses_saved_empty_title),
                message = stringResource(R.string.poses_saved_empty_message),
                actionLabel = stringResource(R.string.poses_saved_empty_action),
                onAction = onShowAll,
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = Modifier.weight(1f).testTag(PosesTestTags.GRID),
            contentPadding = gridPadding(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            items(state.cards, key = { it.id }) { item ->
                PoseGridCell(
                    item = item,
                    onOpenPose = onOpenPose,
                    onToggleSave = onToggleSave,
                    onThumbnailNeeded = onThumbnailNeeded,
                    onThumbnailReleased = onThumbnailReleased,
                    onRetryThumbnail = onRetryThumbnail,
                )
            }
        }
    }
}

/** Catalog still loading: skeleton cards under the top bar, no summary or chips yet. */
@Composable
private fun LoadingGrid(modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = gridPadding(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        userScrollEnabled = false,
    ) {
        items(LOADING_CARDS) { PoseCardSkeleton() }
    }
}

// Design: the grid starts 8 below the chips plus its own 4; the last card stays off the bottom edge.
private fun gridPadding() = PaddingValues(
    start = Spacing.md,
    end = Spacing.md,
    top = Spacing.sm + Spacing.xs,
    bottom = Spacing.md,
)
