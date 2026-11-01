package com.vglstudio.camera.pose.line.guide.feature.camera.pose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PoseThumb
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.StickerChip
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.appColors
import com.vglstudio.camera.pose.line.guide.feature.camera.CameraTestTags
import com.vglstudio.camera.pose.line.guide.feature.camera.POSE_AREA_HEIGHT
import com.vglstudio.camera.pose.line.guide.feature.camera.R

// Sizes from design/context/: 16 free above each row for the badge that sticks out, then the 36 chip or 64 cell.
private val CHIP_ROW_HEIGHT = 52.dp
private val STRIP_HEIGHT = 80.dp

/** Callbacks of the pose part of the camera; each does nothing by default (previews and tests of T-013). */
class PoseActions(
    val onCategorySelect: (String?) -> Unit = {},
    val onPoseSelect: (String) -> Unit = {},
    /** A tap on the cell of a locked premium pose: the unlock dialog (spec poses, KB-8). */
    val onLockedPoseSelect: (String) -> Unit = {},
    val onThumbShown: (String) -> Unit = {},
    val onPreviousStep: () -> Unit = {},
    val onNextStep: () -> Unit = {},
    val onHideSteps: () -> Unit = {},
    val onShowSteps: () -> Unit = {},
    val onRetryOutline: () -> Unit = {},
    val onRetryLibrary: () -> Unit = {},
    // Outline adjustment (T-015). Pan is a part of the frame, zoom a factor.
    val onOverlayTransform: (panX: Float, panY: Float, zoom: Float) -> Unit = { _, _, _ -> },
    val onOverlayGestureEnd: () -> Unit = {},
    val onOpacityChange: (Float) -> Unit = {},
    val onOpacityChangeFinished: () -> Unit = {},
    val onScaleChange: (Float) -> Unit = {},
    val onScaleChangeFinished: () -> Unit = {},
    val onTogglePanel: () -> Unit = {},
    val onClosePanel: () -> Unit = {},
    val onToggleMirror: () -> Unit = {},
    val onToggleLock: () -> Unit = {},
    val onToggleHidden: () -> Unit = {},
    val onResetOverlay: () -> Unit = {},
)

/**
 * Chip row and pose strip under the preview, 132 dp in every camera state (spec camera, KB-1, KB-7). While the
 * catalog loads the area is empty; when it failed, a message and "Thử lại" (plan T-014, question 8). Not [enabled]
 * (while counting down, KB-6): both rows are faded, do not scroll, and their chips and cells cannot be selected.
 */
@Composable
internal fun PosePicker(
    state: PoseUiState,
    actions: PoseActions,
    modifier: Modifier = Modifier,
    // Hoisted by the screen so the scroll positions outlive a change of layout (plan T-020).
    chipsState: LazyListState = rememberLazyListState(),
    stripState: LazyListState = rememberLazyListState(),
    enabled: Boolean = true,
) {
    Box(modifier = modifier.fillMaxWidth().height(POSE_AREA_HEIGHT), contentAlignment = Alignment.Center) {
        when (state) {
            PoseUiState.Loading -> Unit
            PoseUiState.Unavailable -> PosesUnavailable(onRetry = actions.onRetryLibrary)
            is PoseUiState.Ready -> Column(modifier = Modifier.fillMaxWidth()) {
                CategoryChips(
                    state = state,
                    onSelect = actions.onCategorySelect,
                    listState = chipsState,
                    enabled = enabled,
                )
                PoseStrip(state = state, actions = actions, listState = stripState, enabled = enabled)
            }
        }
    }
}

/** "Tất cả" first, then the categories of the catalog. The selected chip lasts for this opening of the camera. */
@Composable
private fun CategoryChips(
    state: PoseUiState.Ready,
    onSelect: (String?) -> Unit,
    listState: LazyListState,
    enabled: Boolean,
) {
    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .height(CHIP_ROW_HEIGHT)
            .rowAlpha(enabled)
            .testTag(CameraTestTags.CHIPS),
        userScrollEnabled = enabled,
        contentPadding = PaddingValues(start = Spacing.md, top = Spacing.md, end = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item(key = ALL_KEY) {
            StickerChip(
                label = stringResource(R.string.camera_chip_all),
                selected = state.selectedCategoryId == null,
                onClick = { onSelect(null) },
                enabled = enabled,
            )
        }
        items(state.categories, key = { it.id }) { chip ->
            StickerChip(
                label = chip.label,
                selected = chip.id == state.selectedCategoryId,
                onClick = { onSelect(chip.id) },
                enabled = enabled,
            )
        }
    }
}

/**
 * Cells in the order of the view model. Each cell asks for its thumbnail when it is put on screen; a new
 * [PoseUiState.Ready.scrollRequest] scrolls to its cell unless that cell is already fully on screen. A locked cell has
 * the premium badge and calls [PoseActions.onLockedPoseSelect]; one that failed to load keeps the failure description.
 */
@Composable
private fun PoseStrip(state: PoseUiState.Ready, actions: PoseActions, listState: LazyListState, enabled: Boolean) {
    val request = state.scrollRequest
    LaunchedEffect(request) {
        if (request != null && !listState.isFullyVisible(request.index)) listState.animateScrollToItem(request.index)
    }
    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .height(STRIP_HEIGHT)
            .rowAlpha(enabled)
            .testTag(CameraTestTags.STRIP),
        userScrollEnabled = enabled,
        contentPadding = PaddingValues(start = Spacing.md, top = Spacing.md, end = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(state.strip, key = { it.id }) { item ->
            LaunchedEffect(item.id) { actions.onThumbShown(item.id) }
            val failed = item.image == PoseImage.Failed
            PoseThumb(
                image = (item.image as? PoseImage.Ready)?.bitmap,
                selected = item.id == state.selected?.id,
                // A locked cell opens the unlock dialog and keeps the pose in use (spec poses, KB-8).
                onClick = {
                    if (item.locked) actions.onLockedPoseSelect(item.id) else actions.onPoseSelect(item.id)
                },
                contentDescription = when {
                    failed -> stringResource(R.string.camera_cd_thumb_failed, item.label)
                    item.locked -> stringResource(R.string.camera_cd_thumb_premium, item.label)
                    else -> item.label
                },
                modifier = Modifier.testTag(CameraTestTags.pose(item.id)),
                failed = failed,
                enabled = enabled,
                premium = item.locked,
            )
        }
    }
}

@Composable
private fun PosesUnavailable(onRetry: () -> Unit) {
    Row(
        modifier = Modifier.padding(horizontal = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = stringResource(R.string.camera_poses_failed),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.appColors.onCamera,
            modifier = Modifier.weight(1f, fill = false),
        )
        TextButton(onClick = onRetry) { Text(text = stringResource(R.string.camera_poses_retry)) }
    }
}

private fun LazyListState.isFullyVisible(index: Int): Boolean {
    val info = layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.index == index } ?: return false
    return item.offset >= 0 && item.offset + item.size <= info.viewportEndOffset - info.afterContentPadding
}

/**
 * Fades a whole row, as the "Group chips" and "Pose strip" frames of the design. On the row, not on each chip or cell:
 * alpha clips, and the selected badge sticks out of the chip but stays inside the 16 dp free above the row.
 */
private fun Modifier.rowAlpha(enabled: Boolean): Modifier = alpha(if (enabled) 1f else DISABLED_ALPHA)

private const val ALL_KEY = "camera_chip_all"

private const val DISABLED_ALPHA = 0.38f
