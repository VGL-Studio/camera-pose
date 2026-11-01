package com.vglstudio.camera.pose.line.guide.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vglstudio.camera.pose.line.guide.core.designsystem.R
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Radius
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Stroke
import kotlin.math.roundToInt

/** Test tag of the save button of a [PoseCard] (its 48 dp touch area). */
const val POSE_CARD_SAVE_TAG = "pose_card_save"

/** Test tag of the 36 dp circle of the save button. */
const val POSE_CARD_SAVE_CIRCLE_TAG = "pose_card_save_circle"

/** Test tag of the image area of a [PoseCard] or a [PoseCardSkeleton]. */
const val POSE_CARD_IMAGE_TAG = "pose_card_image"

// Image area of the design: 162 x 210 (poses design README, section 3).
private const val IMAGE_ASPECT_RATIO = 162f / 210f
private val SaveTouchSize = 48.dp
private val SaveCircleSize = 36.dp
private val SaveIconSize = 20.dp

// Design: the 48 dp touch area of the save button is 2 dp right of and 2 dp below the top right corner of the image.
private val SaveOffset = 2.dp

// Skeleton text bars (design `loading`).
private val NameBarWidth = 120.dp
private val NameBarHeight = 16.dp
private val MetaBarWidth = 80.dp
private val MetaBarHeight = 12.dp

/**
 * Pose card of the library (sticker.md, section 4; poses design README, section 3): an image on the category color
 * with a save button in its top right corner, then the name (at most two lines) and a second line. The card takes the
 * width of its grid cell; the image keeps the 162 : 210 ratio of the design. [onClick] makes the whole card tappable.
 * Screen readers hear the save button as a toggle button named [saveLabel].
 */
@Composable
fun PoseCard(
    name: String,
    meta: String,
    image: Painter,
    imageBackground: Color,
    saved: Boolean,
    saveLabel: String,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    CardFrame(borderColor = colors.outline, modifier = modifier, onClick = onClick) {
        Box {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(IMAGE_ASPECT_RATIO)
                    .testTag(POSE_CARD_IMAGE_TAG)
                    .clip(RoundedCornerShape(Radius.image))
                    .background(imageBackground)
                    .border(Stroke.sticker, colors.outline, RoundedCornerShape(Radius.image)),
            ) {
                Image(
                    painter = image,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.matchParentSize(),
                )
            }
            SaveButton(
                saved = saved,
                label = saveLabel,
                onClick = onSaveClick,
                modifier = Modifier.align(Alignment.TopEnd).offset(x = SaveOffset, y = SaveOffset),
            )
        }
        CardText {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Placeholder of a [PoseCard] whose image is not there yet: the same size as a card with a one-line name, so the grid
 * does not move when the image arrives. [onClick] (for example to load a failed image again) is announced with
 * [onClickLabel]; otherwise screen readers skip it.
 */
@Composable
fun PoseCardSkeleton(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, onClickLabel: String? = null) {
    val colors = MaterialTheme.colorScheme
    val fill = colors.surfaceContainerHighest
    CardFrame(
        borderColor = colors.outlineVariant,
        modifier = modifier,
        onClick = onClick,
        onClickLabel = onClickLabel,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(IMAGE_ASPECT_RATIO)
                .testTag(POSE_CARD_IMAGE_TAG)
                .background(fill, RoundedCornerShape(Radius.image)),
        )
        // Invisible lines in the styles of the card hold the height of its text; the bars sit on them.
        CardText(modifier = Modifier.clearAndSetSemantics {}) {
            SkeletonLine(MaterialTheme.typography.titleMedium) {
                SkeletonBar(width = NameBarWidth, height = NameBarHeight, color = fill)
            }
            SkeletonLine(MaterialTheme.typography.bodySmall) {
                SkeletonBar(width = MetaBarWidth, height = MetaBarHeight, color = fill)
            }
        }
    }
}

@Composable
private fun CardFrame(
    borderColor: Color,
    modifier: Modifier,
    onClick: (() -> Unit)?,
    onClickLabel: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(Radius.card)
    Column(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(Stroke.sticker, borderColor, shape)
            .then(
                if (onClick != null) Modifier.clickable(onClickLabel = onClickLabel, onClick = onClick) else Modifier,
            )
            // The border is drawn inside the card: the content starts after it (design: 2 + 8).
            .padding(Stroke.sticker + Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        content = content,
    )
}

@Composable
private fun CardText(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        // The design spaces name and second line by 2 dp, outside the scale: half of xs (plan T-021, question 6).
        modifier = modifier.fillMaxWidth().padding(start = Spacing.xs, end = Spacing.xs, bottom = Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs / 2),
        content = content,
    )
}

@Composable
private fun SkeletonLine(style: TextStyle, bar: @Composable () -> Unit) {
    Box(contentAlignment = Alignment.CenterStart) {
        Text(text = " ", style = style, maxLines = 1, modifier = Modifier.alpha(0f))
        bar()
    }
}

@Composable
private fun SkeletonBar(width: Dp, height: Dp, color: Color) {
    Box(modifier = Modifier.size(width = width, height = height).background(color, RoundedCornerShape(Radius.full)))
}

@Composable
private fun SaveButton(saved: Boolean, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(SaveTouchSize)
            .testTag(POSE_CARD_SAVE_TAG)
            .toggleable(value = saved, role = Role.Button, onValueChange = { onClick() })
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(SaveCircleSize)
                .testTag(POSE_CARD_SAVE_CIRCLE_TAG)
                .background(if (saved) colors.secondaryContainer else colors.surface, CircleShape)
                .border(Stroke.sticker, colors.outline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(if (saved) R.drawable.ic_bookmark_added else R.drawable.ic_bookmark),
                contentDescription = null,
                tint = if (saved) colors.onSecondaryContainer else colors.onSurface,
                modifier = Modifier.size(SaveIconSize),
            )
        }
    }
}

/** Sizes of [PoseCard] that other components match. */
object PoseCardDefaults {
    /**
     * Height of a [PoseCard] [width] wide whose name takes one line, at the current font scale: the height of a
     * [PoseCardSkeleton]. A grid cell next to the cards (an ad card, plan T-080) takes it so its row stays even.
     */
    @Composable
    fun oneLineHeight(width: Dp): Dp {
        val density = LocalDensity.current
        val measurer = rememberTextMeasurer()
        val typography = MaterialTheme.typography
        fun lineHeight(style: TextStyle): Int = measurer.measure(" ", style, maxLines = 1).size.height
        // In pixels, as the layout of the card rounds them: frame 2 + 8 on each side, image 162 : 210, gap 8, name
        // and second line 2 apart, 4 below.
        return with(density) {
            val frame = (Stroke.sticker + Spacing.sm).roundToPx()
            val image = ((width.roundToPx() - 2 * frame) / IMAGE_ASPECT_RATIO).roundToInt()
            val text = lineHeight(typography.titleMedium) + (Spacing.xs / 2).roundToPx() +
                lineHeight(typography.bodySmall) + Spacing.xs.roundToPx()
            (2 * frame + image + Spacing.sm.roundToPx() + text).toDp()
        }
    }
}
