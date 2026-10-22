package com.vglstudio.camera.pose.line.guide.network

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.NetworkDialogScreen
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.core.network.NetworkDialogState

/**
 * Draws [content] and, while [state] is shown, the network dialog over it (spec network). The dialog is drawn in its
 * own full-screen dialog window: as the top window it keeps touches and screen readers off the content behind it,
 * and system Back goes to its own dispatcher, where Back calls [onBack] instead of navigating. [darkTheme] lets dark
 * screens (camera, review) show the dark dialog.
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
    Box(modifier = modifier) {
        content()
    }
    if (state is NetworkDialogState.Shown) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
            ),
        ) {
            AppTheme(darkTheme = darkTheme) {
                NetworkDialogScreen(checking = state.checking, stillOffline = state.stillOffline, onRetry = onRetry)
            }
            BackHandler(onBack = onBack)
        }
    }
}
