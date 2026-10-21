package com.vglstudio.camera.pose.line.guide.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/** The network dialog card (spec network). */
@Composable
fun NetworkDialog(checking: Boolean, stillOffline: Boolean, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier)
}

/** The network dialog on its scrim, covering the whole screen. */
@Composable
fun NetworkDialogScreen(checking: Boolean, stillOffline: Boolean, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().testTag(NetworkDialogTestTags.SCREEN))
}

object NetworkDialogTestTags {
    const val SCREEN = "network_dialog"
    const val CARD = "network_dialog_card"
    const val RETRY = "network_dialog_retry"
    const val SPINNER = PrimaryButtonTestTags.SPINNER
    const val STILL_OFFLINE = "network_dialog_still_offline"
}
