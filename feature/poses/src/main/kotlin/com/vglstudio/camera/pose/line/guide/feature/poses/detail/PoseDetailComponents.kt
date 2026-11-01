package com.vglstudio.camera.pose.line.guide.feature.poses.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vglstudio.camera.pose.line.guide.core.designsystem.R as DesignR
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PremiumBadge
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PremiumBadgeSize
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PrimaryButton
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.StateMessage
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Radius
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Stroke
import com.vglstudio.camera.pose.line.guide.core.poses.model.Framing
import com.vglstudio.camera.pose.line.guide.feature.poses.R
import com.vglstudio.camera.pose.line.guide.feature.poses.categoryColor
import com.vglstudio.camera.pose.line.guide.feature.poses.screenLocale
import com.vglstudio.camera.pose.line.guide.feature.poses.subjectLabel
import java.util.Locale

// Design: the image area is 380 dp high; the progress ring is 40 dp; the step number circle is 28 dp.
private val ImageHeight = 380.dp
private val ProgressSize = 40.dp
private val StepNumberSize = 28.dp

// Unfolded design: the image is 440 dp high; image column and text column are 315 : 326.
private val WideImageHeight = 440.dp
private const val IMAGE_COLUMN_WEIGHT = 315f
private const val TEXT_COLUMN_WEIGHT = 326f

/**
 * Top bar of the detail: back, the name of the pose, and the save button once the pose is known (design README,
 * section 3). [pose] null: no title and no save button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PoseDetailTopBar(title: String?, saved: Boolean?, onBack: () -> Unit, onToggleSave: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    TopAppBar(
        title = {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_poses_arrow_back),
                    contentDescription = stringResource(R.string.poses_cd_back),
                )
            }
        },
        actions = {
            if (saved != null) {
                IconButton(onClick = onToggleSave) {
                    Icon(
                        painter = painterResource(
                            if (saved) DesignR.drawable.ic_bookmark_added else DesignR.drawable.ic_bookmark,
                        ),
                        contentDescription = stringResource(
                            if (saved) R.string.poses_cd_unsave else R.string.poses_cd_save,
                        ),
                    )
                }
            }
        },
        // The app shell already keeps the screen out of the system bars.
        windowInsets = WindowInsets(0),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colors.background,
            titleContentColor = colors.onSurface,
            navigationIconContentColor = colors.onSurface,
            actionIconContentColor = colors.onSurface,
        ),
    )
}

/** The large image: the figure on the color of its category, a progress ring while it loads, or the error message. */
@Composable
internal fun PoseDetailImage(
    categoryId: String,
    image: DetailImageState,
    onRetry: () -> Unit,
    imageHeight: Dp = ImageHeight,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(Radius.card)
    val background = when (image) {
        DetailImageState.Loading -> colors.surfaceContainerHighest
        is DetailImageState.Ready -> categoryColor(categoryId)
        DetailImageState.Failed -> colors.surfaceVariant
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(imageHeight)
            .clip(shape)
            .background(background)
            .border(BorderStroke(Stroke.sticker, colors.outline), shape)
            .testTag(PoseDetailTestTags.IMAGE),
        contentAlignment = Alignment.Center,
    ) {
        when (image) {
            DetailImageState.Loading -> CircularProgressIndicator(
                modifier = Modifier.size(ProgressSize),
                color = colors.primary,
            )
            is DetailImageState.Ready -> {
                val painter = remember(image.image) { BitmapPainter(image.image) }
                // No description: the name of the pose is in the top bar already (plan T-022, question 7).
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }
            DetailImageState.Failed -> StateMessage(
                icon = painterResource(R.drawable.ic_poses_broken_image),
                title = stringResource(R.string.poses_image_error_title),
                message = stringResource(R.string.poses_image_error_message),
                actionLabel = stringResource(R.string.poses_error_action),
                actionIcon = painterResource(DesignR.drawable.ic_refresh),
                onAction = onRetry,
            )
        }
    }
}

/** The text part of the detail: second line, the steps, and the common mistakes when the catalog has some. */
@Composable
internal fun PoseDetailInfo(pose: PoseDetail, locale: Locale, locked: Boolean) {
    PoseDetailMeta(pose, locked)
    PoseDetailSteps(pose.steps.resolve(locale), locale)
    val mistakes = pose.mistakes.resolve(locale)
    if (mistakes.isNotEmpty()) PoseDetailMistakes(mistakes)
}

/**
 * Second line: subject type, group, framing, number of steps; the group is left out when the catalog has no name.
 * [locked]: the premium badge before it, [Spacing.sm] apart (design PoseDetailScreen_premium; spec poses, KB-8).
 */
@Composable
internal fun PoseDetailMeta(pose: PoseDetail, locked: Boolean) {
    val locale = screenLocale()
    val steps = pose.steps.resolve(locale).size
    val subject = subjectLabel(pose.subjectType)
    val framing = framingLabel(pose.framing)
    val category = pose.categoryName?.resolve(locale)?.takeIf { it.isNotEmpty() }
    val text = if (category != null) {
        stringResource(R.string.poses_detail_meta, subject, category, framing, steps)
    } else {
        stringResource(R.string.poses_detail_meta_no_group, subject, framing, steps)
    }
    val meta = @Composable {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (locked) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PremiumBadge(
                size = PremiumBadgeSize.Inline,
                contentDescription = stringResource(DesignR.string.premium_badge_label),
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            meta()
        }
    } else {
        meta()
    }
}

/** The numbered steps under the title "Các bước". */
@Composable
internal fun PoseDetailSteps(steps: List<String>, locale: Locale) {
    val colors = MaterialTheme.colorScheme
    Text(
        text = stringResource(R.string.poses_detail_steps_title),
        style = MaterialTheme.typography.titleMedium,
        color = colors.onSurface,
    )
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        steps.forEachIndexed { index, step ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Box(
                    modifier = Modifier
                        .size(StepNumberSize)
                        .background(colors.secondaryContainer, CircleShape)
                        .border(BorderStroke(Stroke.sticker, colors.outline), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = String.format(locale, "%d", index + 1),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onSecondaryContainer,
                    )
                }
                Text(
                    text = step,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** "Lỗi hay gặp": one text for each mistake, on the tertiary container. */
@Composable
internal fun PoseDetailMistakes(mistakes: List<String>) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(Radius.card)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.tertiaryContainer, shape)
            .border(BorderStroke(Stroke.sticker, colors.outline), shape)
            .padding(Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_poses_warning),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = colors.onTertiaryContainer,
        )
        // Design: 2 dp between the title and the texts.
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs / 2)) {
            Text(
                text = stringResource(R.string.poses_detail_mistakes_title),
                style = MaterialTheme.typography.titleSmall,
                color = colors.onTertiaryContainer,
            )
            mistakes.forEach { mistake ->
                Text(
                    text = mistake,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onTertiaryContainer,
                )
            }
        }
    }
}

/**
 * The bar under the scrolling content: a line, then the main button. [locked]: the main button unlocks the premium pose
 * ("Xem quảng cáo để dùng", icon smart_display), at the same place and size (spec poses, KB-8).
 */
@Composable
internal fun PoseDetailBottomBar(locked: Boolean, onShoot: () -> Unit, onUnlock: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .testTag(PoseDetailTestTags.BOTTOM_BAR),
    ) {
        HorizontalDivider(thickness = Stroke.sticker, color = MaterialTheme.colorScheme.outlineVariant)
        if (locked) {
            PrimaryButton(
                text = stringResource(R.string.poses_detail_unlock),
                onClick = onUnlock,
                modifier = Modifier.fillMaxWidth().padding(Spacing.md).testTag(PoseDetailTestTags.UNLOCK),
                icon = painterResource(R.drawable.ic_poses_smart_display),
            )
        } else {
            PrimaryButton(
                text = stringResource(R.string.poses_detail_shoot),
                onClick = onShoot,
                modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                icon = painterResource(R.drawable.ic_poses_photo_camera),
            )
        }
    }
}

/**
 * The phone layout: one scrolling column (image, then the text) with the button bar under it (design README,
 * section 3).
 */
@Composable
internal fun PhoneDetailLayout(
    ready: PoseDetailUiState.Ready,
    locale: Locale,
    scrollState: ScrollState,
    onRetryImage: () -> Unit,
    onShoot: () -> Unit,
    onUnlock: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            PoseDetailImage(ready.pose.categoryId, ready.image, onRetryImage)
            PoseDetailInfo(ready.pose, locale, ready.locked)
            Spacer(modifier = Modifier.height(Spacing.md))
        }
        PoseDetailBottomBar(locked = ready.locked, onShoot = onShoot, onUnlock = onUnlock)
    }
}

/** The wide layout for an unfolded foldable (spec poses, KB-7): the image on the left, the text on the right. */
@Composable
internal fun WideDetailLayout(
    ready: PoseDetailUiState.Ready,
    locale: Locale,
    scrollState: ScrollState,
    onRetryImage: () -> Unit,
    onShoot: () -> Unit,
    onUnlock: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize().padding(start = Spacing.md, top = Spacing.md)) {
        // The columns are 315 and 326 wide in the design (the right one with the right margin), as 315 : 326.
        Column(modifier = Modifier.weight(IMAGE_COLUMN_WEIGHT)) {
            PoseDetailImage(ready.pose.categoryId, ready.image, onRetryImage, imageHeight = WideImageHeight)
        }
        Spacer(modifier = Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(TEXT_COLUMN_WEIGHT)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(end = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                PoseDetailInfo(ready.pose, locale, ready.locked)
                Spacer(modifier = Modifier.height(Spacing.md))
            }
            PoseDetailBottomBar(locked = ready.locked, onShoot = onShoot, onUnlock = onUnlock)
        }
    }
}

@Composable
private fun framingLabel(framing: Framing): String = stringResource(
    when (framing) {
        Framing.FULL -> R.string.poses_framing_full
        Framing.HALF -> R.string.poses_framing_half
        Framing.CLOSE -> R.string.poses_framing_close
    },
)
