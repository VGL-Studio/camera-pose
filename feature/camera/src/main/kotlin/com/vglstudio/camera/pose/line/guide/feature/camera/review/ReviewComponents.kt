package com.vglstudio.camera.pose.line.guide.feature.camera.review

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.CameraIconButton
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PrimaryButton
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.SecondaryButton
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Radius
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Stroke
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.appColors
import com.vglstudio.camera.pose.line.guide.feature.camera.R

// Sizes from design/context/ReviewScreen_*.md.
private val TOP_BAR_HEIGHT = 64.dp
private val ACTIONS_MAX_WIDTH = 331.dp
private val DIALOG_MAX_WIDTH = 331.dp
private const val PHOTO_ASPECT_RATIO = 3f / 4f
private const val DISABLED_ALPHA = 0.38f
private const val SCRIM_ALPHA = 0.5f

/** Close button and "Xem lại". Not Material's TopAppBar, which adds the status bar inset on its own. */
@Composable
internal fun ReviewTopBar(enabled: Boolean, onCloseClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TOP_BAR_HEIGHT)
            .padding(horizontal = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onCloseClick,
            enabled = enabled,
            colors = IconButtonDefaults.iconButtonColors(contentColor = colors.onSurface),
            modifier = Modifier.testTag(ReviewTestTags.CLOSE),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_camera_close),
                contentDescription = stringResource(R.string.review_cd_close),
            )
        }
        Text(
            text = stringResource(R.string.review_title),
            style = MaterialTheme.typography.titleLarge,
            color = colors.onSurface,
        )
    }
}

/** The photo in a 3:4 frame; an empty frame while it is read or when it cannot be read. */
@Composable
internal fun ReviewPhoto(image: ImageBitmap?, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(Radius.card)
    Box(
        modifier = modifier
            .aspectRatio(PHOTO_ASPECT_RATIO, matchHeightConstraintsFirst = true)
            .clip(shape)
            .background(MaterialTheme.appColors.cameraSurface, shape)
            .border(Stroke.sticker, MaterialTheme.colorScheme.outline, shape)
            .testTag(ReviewTestTags.PHOTO),
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** "Chụp lại", "Chia sẻ", "Tư thế tiếp": round buttons with a label, faded and disabled while saving. */
@Composable
internal fun ReviewActions(
    enabled: Boolean,
    onRetakeClick: () -> Unit,
    onShareClick: () -> Unit,
    onNextPoseClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .widthIn(max = ACTIONS_MAX_WIDTH)
            .fillMaxWidth()
            .testTag(ReviewTestTags.ACTIONS),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        ReviewAction(
            icon = painterResource(R.drawable.ic_review_replay),
            label = stringResource(R.string.review_retake),
            enabled = enabled,
            onClick = onRetakeClick,
            tag = ReviewTestTags.RETAKE,
        )
        ReviewAction(
            icon = painterResource(R.drawable.ic_review_share),
            label = stringResource(R.string.review_share),
            enabled = enabled,
            onClick = onShareClick,
            tag = ReviewTestTags.SHARE,
        )
        ReviewAction(
            icon = painterResource(R.drawable.ic_review_skip_next),
            label = stringResource(R.string.review_next_pose),
            enabled = enabled,
            onClick = onNextPoseClick,
            tag = ReviewTestTags.NEXT_POSE,
        )
    }
}

/** One action: screen readers hear a single button named by its label. */
@Composable
private fun ReviewAction(icon: Painter, label: String, enabled: Boolean, onClick: () -> Unit, tag: String) {
    Column(
        modifier = Modifier
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        CameraIconButton(
            icon = icon,
            onClick = onClick,
            contentDescription = null,
            enabled = enabled,
            modifier = Modifier.clearAndSetSemantics {},
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.alpha(if (enabled) 1f else DISABLED_ALPHA),
        )
    }
}

/**
 * Why the save failed, with "Thử lại", or "Mở cài đặt" when the storage permission can only be granted in the system
 * settings. Stays until the next try or until the screen is left.
 */
@Composable
internal fun SaveErrorMessage(
    save: SaveStatus,
    onRetrySave: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val message = when (save) {
        SaveStatus.FAILED_STORAGE_FULL -> R.string.review_save_error_storage_full
        SaveStatus.PERMISSION_DENIED -> R.string.review_permission_denied
        SaveStatus.PERMISSION_BLOCKED -> R.string.review_permission_blocked
        else -> R.string.review_save_error
    }
    val blocked = save == SaveStatus.PERMISSION_BLOCKED
    Snackbar(
        modifier = modifier.testTag(ReviewTestTags.SAVE_ERROR),
        shape = RoundedCornerShape(Radius.card),
        containerColor = colors.inverseSurface,
        contentColor = colors.inverseOnSurface,
        actionContentColor = colors.inversePrimary,
        action = {
            TextButton(onClick = if (blocked) onOpenAppSettings else onRetrySave) {
                Text(
                    text = stringResource(if (blocked) R.string.camera_permission_action else R.string.review_retry),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
    ) {
        Text(text = stringResource(message), style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * "Bỏ ảnh này?" drawn in the screen itself, not in a Dialog window, so screenshots show it on its scrim. A tap on the
 * scrim dismisses it; taps on the card do not.
 */
@Composable
internal fun DiscardDialog(onDiscardConfirm: () -> Unit, onDiscardSave: () -> Unit, onDismissDialog: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(Radius.sheet)
    val title = stringResource(R.string.review_discard_title)
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // A layer of its own behind the card: a clickable parent would merge the card and its pane title.
        // Screen readers close the dialog with Back instead.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.appColors.cameraScrim.copy(alpha = SCRIM_ALPHA))
                .clearAndSetSemantics {}
                .clickable(interactionSource = null, indication = null, onClick = onDismissDialog),
        )
        Column(
            modifier = Modifier
                .padding(Spacing.md)
                .widthIn(max = DIALOG_MAX_WIDTH)
                .fillMaxWidth()
                .background(colors.surface, shape)
                .border(Stroke.sticker, colors.outline, shape)
                // Taps on the card are not taps on the scrim.
                .pointerInput(Unit) { detectTapGestures {} }
                // TalkBack announces the dialog by its title when it opens.
                .semantics { paneTitle = title }
                .testTag(ReviewTestTags.DISCARD_DIALOG)
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
            Text(
                text = stringResource(R.string.review_discard_message),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SecondaryButton(
                    text = stringResource(R.string.review_discard_confirm),
                    onClick = onDiscardConfirm,
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = stringResource(R.string.review_discard_save),
                    onClick = onDiscardSave,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
