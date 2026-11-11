package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Draws the native full-screen turn of the running app over the current screen, in a full-screen dialog: the combo
 * after an interstitial and the standalone turn (HD-05, plan T-026), with the countdown of `time_close_native`; the screen below keeps
 * its state, and closing the ad returns to it (KB-5, KB-16). :app places it once, inside its theme, after the
 * navigation host, at the root of the UI of every activity that may call a full-screen ad. It registers with the
 * controller while it is in a composition: with no host the combo does not open and a standalone native full-screen
 * turn is skipped (K-09). It draws nothing in tests and previews (no [LocalAdsController]).
 */
@Composable
fun FullScreenAdHost() {
    val ads = LocalAdsController.current ?: return
    DisposableEffect(ads) {
        val detach = ads.attachFullScreenHost()
        onDispose { detach() }
    }
    NativeFullScreenOverlayHost(ads.nativeFullScreenView, ads::closeNativeFullScreen, ads::onNativeFullScreenBack)
}

/** The back button closes the ad; a touch outside means nothing (the dialog covers the whole window). */
internal fun nativeFullScreenDialogProperties(): DialogProperties = DialogProperties(
    dismissOnBackPress = true,
    dismissOnClickOutside = false,
    usePlatformDefaultWidth = false,
    decorFitsSystemWindows = false,
)

/**
 * A dialog over the whole window. It is its own window with its own back dispatcher, so the back button reaches
 * [onClose], not the activity below.
 */
@Composable
internal fun NativeFullScreenAdDialog(onClose: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = nativeFullScreenDialogProperties()) {
        Box(modifier = Modifier.fillMaxSize()) { content() }
    }
}
