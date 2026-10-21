package com.vglstudio.camera.pose.line.guide.network

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.NetworkDialogScreen
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.core.network.NetworkDialogState

/**
 * Draws [content] and, while [state] is shown, the network dialog over it (spec network). The dialog is drawn in the
 * same window, not a separate dialog window, so it blocks touches and screenshots show it. While shown, the content
 * is hidden from screen readers and Back calls [onBack] instead of navigating. [darkTheme] lets dark screens
 * (camera, review) show the dark dialog.
 */
@Composable
fun NetworkDialogHost(
    state: NetworkDialogState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val shown = state as? NetworkDialogState.Shown
    Box(modifier = modifier) {
        Box(modifier = if (shown != null) Modifier.semantics { hideFromAccessibility() } else Modifier) {
            content()
        }
        if (shown != null) {
            AppTheme(darkTheme = darkTheme) {
                NetworkDialogScreen(checking = shown.checking, stillOffline = shown.stillOffline, onRetry = onRetry)
            }
            // Composed after the NavHost in content, so it takes Back before navigation does (KB-3).
            BackHandler(onBack = onBack)
        }
    }
}
