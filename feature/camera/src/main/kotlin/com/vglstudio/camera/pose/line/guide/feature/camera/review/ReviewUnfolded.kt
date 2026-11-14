package com.vglstudio.camera.pose.line.guide.feature.camera.review

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PrimaryButton
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import com.vglstudio.camera.pose.line.guide.feature.camera.R
import com.vglstudio.camera.pose.line.guide.feature.camera.review.ReviewUiState.Ready

// The 3:4 photo of the phone, and the room the right column needs at least: the main button of the design (175 dp).
private const val PHOTO_ASPECT_RATIO = 3f / 4f
private val RIGHT_COLUMN_MIN_WIDTH = 175.dp

// What the right column needs for its stacked layout (plan T-094): the three stacked actions (3 x 48 + 2 x 24), the
// main button (PrimaryButton), and the room kept for the save error: 140 covers it in Vietnamese and English in the
// 175 dp column (112 to 132 dp measured at 673 x 841, 2026-11-13).
private val STACKED_ACTIONS_HEIGHT = 192.dp
private val MAIN_BUTTON_HEIGHT = 56.dp
private val ERROR_ROOM = 140.dp

/**
 * What is under the top bar of the review screen on an unfolded phone (spec camera, KB-11; plan T-020), from
 * design/context/ReviewScreen_default_dark_unfolded.md: the photo on the left, as high as the room allows but leaving
 * the right column its width (450 x 600 at 673 x 841); in that column the three actions stacked from the top of the
 * photo, and at the foot the save error (if any) over the main button, which is as wide as its text. When the column
 * is too short for that (a short landscape window with the bottom ad bar, plan T-094), the actions sit side by side
 * and the action of the error on its line, so the main button keeps its height.
 */
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
    val saving = ready.save == SaveStatus.SAVING
    val bottomMargin = Spacing.md + Spacing.xs
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxPhotoWidth = (maxWidth - Spacing.md * 3 - RIGHT_COLUMN_MIN_WIDTH).coerceAtLeast(0.dp)
        val photoHeight = minOf(maxHeight - bottomMargin, maxPhotoWidth / PHOTO_ASPECT_RATIO).coerceAtLeast(0.dp)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = Spacing.md, end = Spacing.md, bottom = bottomMargin),
        ) {
            ReviewPhoto(image = image, modifier = Modifier.size(photoHeight * PHOTO_ASPECT_RATIO, photoHeight))
            Spacer(modifier = Modifier.width(Spacing.md))
            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxHeight()) {
                val hasError = ready.save in ERROR_STATUSES
                // Longer languages need more than ERROR_ROOM in the 175 dp column (pt-BR 152 to 160), which only
                // matters in a column of about 503 dp or more, where the stacked layout still fits
                // (192 + 16 + 160 + 16 + 56 = 440).
                val stackedNeed = STACKED_ACTIONS_HEIGHT + Spacing.md + MAIN_BUTTON_HEIGHT +
                    if (hasError) Spacing.md + ERROR_ROOM else 0.dp
                // Too little room (a short landscape window with the bottom ad bar, plan T-094): the actions side by
                // side as on a phone, the action of the error on its line, so the main button keeps its 56 dp.
                val compact = maxHeight < stackedNeed
                Column(modifier = Modifier.fillMaxSize()) {
                    ReviewActions(
                        enabled = !saving,
                        onRetakeClick = onRetakeClick,
                        onShareClick = onShareClick,
                        onNextPoseClick = onNextPoseClick,
                        vertical = !compact,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (hasError) {
                        SaveErrorMessage(
                            save = ready.save,
                            onRetrySave = onRetrySave,
                            onOpenAppSettings = onOpenAppSettings,
                            // On one row, as on a phone, not forced to the full width: Material's one-row snackbar
                            // then gives the message the whole width and draws its action over it.
                            modifier = if (compact) Modifier else Modifier.fillMaxWidth(),
                            actionOnNewLine = !compact,
                        )
                        Spacer(modifier = Modifier.height(Spacing.md))
                    }
                    PrimaryButton(
                        text = stringResource(if (saving) R.string.review_saving else R.string.review_save_unfolded),
                        onClick = onSaveClick,
                        modifier = Modifier.testTag(ReviewTestTags.SAVE),
                        enabled = ready.save != SaveStatus.SAVED,
                        icon = painterResource(R.drawable.ic_review_download),
                        loading = saving,
                    )
                }
            }
        }
    }
}
