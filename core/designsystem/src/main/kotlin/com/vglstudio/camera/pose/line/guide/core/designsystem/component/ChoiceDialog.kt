package com.vglstudio.camera.pose.line.guide.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Radius
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Stroke
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.appColors

private val CardMaxWidth = 331.dp
private val ActionHeight = 48.dp
private val RadioRowHeight = 48.dp
private val RadioSize = 24.dp
private val RadioDotSize = 10.dp
private const val SCRIM_ALPHA = 0.5f

/**
 * A picker of one option among [options] (design settings, section 3; plan T-025, question 2): a card centered on a
 * scrim that covers the whole screen. The scrim is drawn here, as the network dialog, so screenshots show it; a tap on
 * it calls [onDismiss], as the [dismissLabel] button does. The options scroll when the screen is too short.
 * [dismissLabel] is given by the caller, so this module has no translated text.
 */
@Composable
fun ChoiceDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    dismissLabel: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(Radius.sheet)
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.appColors.cameraScrim.copy(alpha = SCRIM_ALPHA))
            .pointerInput(Unit) { detectTapGestures { currentOnDismiss() } }
            .testTag(ChoiceDialogTestTags.SCREEN),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                // Shown edge to edge in its own window: keep the card clear of the system bars.
                .safeDrawingPadding()
                .padding(Spacing.md)
                .widthIn(max = CardMaxWidth)
                .fillMaxWidth()
                .background(colors.surface, shape)
                .border(Stroke.sticker, colors.outline, shape)
                // A tap on the card, away from the options, closes nothing.
                .pointerInput(Unit) { detectTapGestures {} }
                // TalkBack announces the picker by its title when it opens.
                .semantics { paneTitle = title }
                .testTag(ChoiceDialogTestTags.CARD)
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .selectableGroup(),
            ) {
                options.forEachIndexed { index, option ->
                    RadioRow(label = option, selected = index == selectedIndex, onClick = { onSelect(index) })
                }
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.End)
                    .heightIn(min = ActionHeight)
                    .testTag(ChoiceDialogTestTags.DISMISS),
                contentPadding = PaddingValues(horizontal = Spacing.md),
                // TextButton would draw its label in primary; the design uses onSurface.
                colors = ButtonDefaults.textButtonColors(contentColor = colors.onSurface),
            ) {
                Text(text = dismissLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/**
 * One option of a [ChoiceDialog]: a radio and its label, the whole row selectable. The radio is drawn here (plan
 * T-025, question 2): yellow fill with a dark dot when selected, as the design, which RadioButton cannot draw.
 */
@Composable
fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(RadioRowHeight)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(RadioSize)
                .background(if (selected) colors.secondaryContainer else colors.surface, CircleShape)
                .border(Stroke.sticker, colors.outline, CircleShape)
                .testTag(ChoiceDialogTestTags.RADIO),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(Modifier.size(RadioDotSize).background(colors.onSecondaryContainer, CircleShape))
            }
        }
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
    }
}

object ChoiceDialogTestTags {
    const val SCREEN = "choice_dialog"
    const val CARD = "choice_dialog_card"
    const val DISMISS = "choice_dialog_dismiss"
    const val RADIO = "radio_row_radio"
}
