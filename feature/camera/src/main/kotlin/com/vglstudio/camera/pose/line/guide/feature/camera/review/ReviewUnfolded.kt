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

/**
 * What is under the top bar of the review screen on an unfolded phone (spec camera, KB-11; plan T-020), from
 * design/context/ReviewScreen_default_dark_unfolded.md: the photo on the left, as high as the room allows but leaving
 * the right column its width (450 x 600 at 673 x 841); in that column the three actions stacked from the top of the
 * photo, and at the foot the save error (if any) over the main button, which is as wide as its text.
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
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                ReviewActions(
                    enabled = !saving,
                    onRetakeClick = onRetakeClick,
                    onShareClick = onShareClick,
                    onNextPoseClick = onNextPoseClick,
                    vertical = true,
                )
                Spacer(modifier = Modifier.weight(1f))
                if (ready.save in ERROR_STATUSES) {
                    SaveErrorMessage(
                        save = ready.save,
                        onRetrySave = onRetrySave,
                        onOpenAppSettings = onOpenAppSettings,
                        modifier = Modifier.fillMaxWidth(),
                        actionOnNewLine = true,
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
