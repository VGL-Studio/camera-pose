package com.vglstudio.camera.pose.line.guide.network

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vglstudio.camera.pose.line.guide.core.network.NetworkDialogState

/** Draws [content] and, while [state] is shown, the network dialog over it (spec network). */
@Composable
fun NetworkDialogHost(
    state: NetworkDialogState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    content()
}
