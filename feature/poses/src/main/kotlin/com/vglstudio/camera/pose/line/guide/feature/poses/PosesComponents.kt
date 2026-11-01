package com.vglstudio.camera.pose.line.guide.feature.poses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PoseCard
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PoseCardSkeleton
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.StickerChip
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.appColors
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseFilter
import com.vglstudio.camera.pose.line.guide.core.poses.model.SubjectType
import java.util.Locale

/** Top bar of the library: back, title, and settings when [onOpenSettings] is set (design README, section 3). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PosesTopBar(onBack: () -> Unit, onOpenSettings: (() -> Unit)?) {
    val colors = MaterialTheme.colorScheme
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.poses_title),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
            )
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
            if (onOpenSettings != null) {
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        painter = painterResource(R.drawable.ic_poses_settings),
                        contentDescription = stringResource(R.string.poses_cd_settings),
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

/** All, Saved, then the categories, scrolling sideways. Leaves room above for the badge of the selected chip. */
@Composable
internal fun PoseChipRow(chips: List<PoseChip>, selected: PoseFilter, onSelect: (PoseFilter) -> Unit) {
    val locale = screenLocale()
    LazyRow(
        // Design: chips 16 below the summary line; the badge sticks out 9 above them, so the room is padding here.
        contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, top = Spacing.md, bottom = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(chips, key = { it.key() }) { chip ->
            val filter = chip.filter()
            StickerChip(
                label = when (chip) {
                    PoseChip.All -> stringResource(R.string.poses_chip_all)
                    PoseChip.Saved -> stringResource(R.string.poses_chip_saved)
                    is PoseChip.Category -> chip.name.resolve(locale)
                },
                selected = filter == selected,
                onClick = { onSelect(filter) },
            )
        }
    }
}

/**
 * One cell of the grid. While composed it needs its thumbnail; leaving the grid releases it. A ready image shows the
 * card, anything else the skeleton of the same size; a failed image loads again on a tap.
 */
@Composable
internal fun PoseGridCell(
    item: PoseCardItem,
    onOpenPose: ((String) -> Unit)?,
    onToggleSave: (String) -> Unit,
    onThumbnailNeeded: (String) -> Unit,
    onThumbnailReleased: (String) -> Unit,
    onRetryThumbnail: (String) -> Unit,
) {
    val currentNeeded by rememberUpdatedState(onThumbnailNeeded)
    val currentReleased by rememberUpdatedState(onThumbnailReleased)
    DisposableEffect(item.id) {
        currentNeeded(item.id)
        onDispose { currentReleased(item.id) }
    }
    val modifier = Modifier.testTag(PosesTestTags.card(item.id))
    when (val thumbnail = item.thumbnail) {
        is ThumbnailState.Ready -> {
            val painter = remember(thumbnail.image) { BitmapPainter(thumbnail.image) }
            // Name and steps in the language of the screen, as the camera counts them (steps.resolve()).
            val locale = screenLocale()
            PoseCard(
                name = item.name.resolve(locale),
                meta = stringResource(
                    R.string.poses_card_meta,
                    subjectLabel(item.subjectType),
                    item.steps.resolve(locale).size,
                ),
                image = painter,
                imageBackground = categoryColor(item.categoryId),
                saved = item.saved,
                saveLabel = stringResource(if (item.saved) R.string.poses_cd_unsave else R.string.poses_cd_save),
                onSaveClick = { onToggleSave(item.id) },
                modifier = modifier,
                onClick = onOpenPose?.let { open -> { open(item.id) } },
                // A locked card still opens the detail (spec poses, question 7).
                premium = item.locked,
            )
        }
        ThumbnailState.Loading -> PoseCardSkeleton(modifier = modifier)
        ThumbnailState.Failed -> PoseCardSkeleton(
            modifier = modifier,
            onClick = { onRetryThumbnail(item.id) },
            onClickLabel = stringResource(R.string.poses_cd_reload_image),
        )
    }
}

/**
 * [content] centred in the space left, and scrolling when that space is too short (L-028): the empty and error
 * messages.
 */
@Composable
internal fun CenteredScrollable(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val minHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = minHeight)
                .padding(horizontal = Spacing.md),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content()
        }
    }
}

/** Image background of a card: the color of its category (plan T-021, question 4); other ids use surfaceVariant. */
@Composable
internal fun categoryColor(categoryId: String): Color {
    val colors = MaterialTheme.appColors
    return when (categoryId) {
        "women" -> colors.categoryWomen
        "men" -> colors.categoryMen
        "couples" -> colors.categoryCouple
        "friends" -> colors.categoryFriends
        "family" -> colors.categoryFamily
        "kids" -> colors.categoryKids
        "travel" -> colors.categoryTravel
        "cafe" -> colors.categoryCafe
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
}

/** First word of the second line of a card: Một người, Cặp đôi, Nhóm. */
@Composable
internal fun subjectLabel(type: SubjectType): String = stringResource(
    when (type) {
        SubjectType.SINGLE -> R.string.poses_subject_single
        SubjectType.COUPLE -> R.string.poses_subject_couple
        SubjectType.GROUP -> R.string.poses_subject_group
    },
)

/** The language of the screen, read here so names follow a language change (plan T-021, KB-1). */
@Composable
internal fun screenLocale(): Locale = LocalConfiguration.current.locales[0]

/** Width from which the screens use the wide layout (an unfolded foldable): the Material "compact" window limit. */
internal val WideLayoutMinWidth: Dp = 600.dp

/** True when the room given to the screen is wide enough for the wide layout (spec poses, KB-7). */
internal fun BoxWithConstraintsScope.isWide(): Boolean = maxWidth >= WideLayoutMinWidth

private fun PoseChip.filter(): PoseFilter = when (this) {
    PoseChip.All -> PoseFilter.All
    PoseChip.Saved -> PoseFilter.Saved
    is PoseChip.Category -> PoseFilter.Category(id)
}

private fun PoseChip.key(): String = when (this) {
    PoseChip.All -> "all"
    PoseChip.Saved -> "saved"
    is PoseChip.Category -> "category:$id"
}
