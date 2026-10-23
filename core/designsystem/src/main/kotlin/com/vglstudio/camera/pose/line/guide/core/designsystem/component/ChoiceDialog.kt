package com.vglstudio.camera.pose.line.guide.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Skeleton for the tests written first (plan T-025, section 4). */
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
    Box(modifier)
}

/** Skeleton for the tests written first (plan T-025, section 4). */
@Composable
fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier)
}

object ChoiceDialogTestTags {
    const val SCREEN = "choice_dialog"
    const val CARD = "choice_dialog_card"
    const val DISMISS = "choice_dialog_dismiss"
    const val RADIO = "radio_row_radio"
}
