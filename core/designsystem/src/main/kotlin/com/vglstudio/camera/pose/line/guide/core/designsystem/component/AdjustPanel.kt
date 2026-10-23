package com.vglstudio.camera.pose.line.guide.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.vglstudio.camera.pose.line.guide.core.designsystem.R
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Radius
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Stroke
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.appColors

// Sizes from the camera design (README, section 3; context/CameraScreen_adjust_dark.md).
private val HeaderIconSize = 24.dp
private val CloseTouchSize = 48.dp
private val SliderLabelHeight = 20.dp
private val SliderTrackBoxHeight = 28.dp
private val SliderTouchHeight = 48.dp
private val TrackHeight = 8.dp
private val ThumbSize = 24.dp
private val ActionWidth = 48.dp
private const val DISABLED_ALPHA = 0.38f

/**
 * The sheet that adjusts the pose outline on the camera (sticker.md, section 4; camera design README, section 3):
 * white, with a dark sticker border, [title] and a close button, then [content] (sliders and actions) 16 dp apart.
 * The caller sets the width and, on a short screen, the height and the scrolling.
 */
@Composable
fun AdjustPanel(
    title: String,
    onClose: () -> Unit,
    closeDescription: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val appColors = MaterialTheme.appColors
    val shape = RoundedCornerShape(Radius.sheet)
    Column(
        modifier = modifier
            .clip(shape)
            .background(appColors.cameraControl, shape)
            .border(Stroke.sticker, appColors.cameraOnControl, shape)
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = appColors.cameraOnControl)
            // The icon takes 24 dp of the header; its button is 48 dp around it (the header keeps the design height).
            Box(modifier = Modifier.size(HeaderIconSize), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .requiredSize(CloseTouchSize)
                        .clip(CircleShape)
                        .semantics { contentDescription = closeDescription }
                        .clickable(role = Role.Button, onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = null,
                        tint = appColors.cameraOnControl,
                        modifier = Modifier.size(HeaderIconSize),
                    )
                }
            }
        }
        content()
    }
}

/**
 * A labelled slider, 52 dp high: [label] on the left and [valueText] on the right, then the 28 dp track box. The track
 * is 8 dp, the part run yellow with a thin border, the thumb a white 24 dp disc with the sticker border. The slider
 * itself is 48 dp high, taller than the box. Screen readers hear [label] and [valueText] as the state; the texts drawn
 * above the track are not read again.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustSlider(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appColors = MaterialTheme.appColors
    // One node for screen readers: Slider keeps its own touch handling, but its semantics are not merged into a
    // parent, so the label, the value and the progress actions are set here.
    Column(
        modifier = modifier.fillMaxWidth().clearAndSetSemantics {
            contentDescription = label
            stateDescription = valueText
            progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(valueRange), valueRange)
            setProgress { target ->
                onValueChange(target.coerceIn(valueRange.start, valueRange.endInclusive))
                onValueChangeFinished()
                true
            }
        },
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(SliderLabelHeight),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = appColors.cameraOnControl)
            Text(text = valueText, style = MaterialTheme.typography.labelLarge, color = appColors.cameraOnControl)
        }
        Box(
            modifier = Modifier.fillMaxWidth().height(SliderTrackBoxHeight),
            contentAlignment = Alignment.Center,
        ) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.requiredHeight(SliderTouchHeight),
                valueRange = valueRange,
                onValueChangeFinished = onValueChangeFinished,
                thumb = { AdjustThumb() },
                track = { state -> AdjustTrack(state) },
            )
        }
    }
}

@Composable
private fun AdjustThumb() {
    val appColors = MaterialTheme.appColors
    Box(
        modifier = Modifier
            .size(ThumbSize)
            .background(appColors.cameraControl, CircleShape)
            .border(Stroke.sticker, appColors.cameraOnControl, CircleShape),
    )
}

/**
 * The track reaches both ends of the slider, under the thumb at its ends. [Slider] keeps the thumb centre inside the
 * track inset by half the thumb, so the track is laid out that much wider than [Slider] gives it, and the run part
 * ends at the thumb centre.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdjustTrack(state: SliderState) {
    val appColors = MaterialTheme.appColors
    val colors = MaterialTheme.colorScheme
    val range = state.valueRange
    val fraction = ((state.value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
    val shape = RoundedCornerShape(Radius.full)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(TrackHeight)
            .layout { measurable, constraints ->
                val overhang = ThumbSize.roundToPx()
                val width = constraints.maxWidth + overhang
                val placeable = measurable.measure(Constraints.fixed(width, constraints.maxHeight))
                layout(constraints.maxWidth, placeable.height) { placeable.place(-overhang / 2, 0) }
            },
    ) {
        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight().background(appColors.cameraOnSurfaceVariant, shape))
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .layout { measurable, constraints ->
                    // The run part: from the left end to the thumb centre.
                    val half = ThumbSize.roundToPx() / 2
                    val width = half + ((constraints.maxWidth - 2 * half) * fraction).toInt()
                    val placeable = measurable.measure(Constraints.fixed(width, constraints.maxHeight))
                    layout(width, placeable.height) { placeable.place(0, 0) }
                }
                .background(colors.secondaryContainer, shape)
                .border(Stroke.thin, appColors.cameraOnControl, shape),
        )
    }
}

/**
 * A round 48 dp button with [label] under it, 48 x 68 dp together. Screen readers hear [label] as the name of the
 * button (the drawn label is not read again); [active] marks it selected.
 */
@Composable
fun AdjustAction(
    icon: Painter,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    enabled: Boolean = true,
) {
    val appColors = MaterialTheme.appColors
    Column(
        modifier = modifier.width(ActionWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        CameraIconButton(
            icon = icon,
            onClick = onClick,
            contentDescription = label,
            active = active,
            enabled = enabled,
        )
        // The drawn label is not a second stop for screen readers: the button above already has its name.
        Box(modifier = Modifier.clearAndSetSemantics {}) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = appColors.cameraOnControl.copy(alpha = if (enabled) 1f else DISABLED_ALPHA),
                softWrap = false,
                modifier = Modifier.wrapContentWidth(unbounded = true),
            )
        }
    }
}
