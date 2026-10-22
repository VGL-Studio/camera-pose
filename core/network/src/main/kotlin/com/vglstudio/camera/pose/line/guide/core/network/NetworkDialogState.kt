package com.vglstudio.camera.pose.line.guide.core.network

/** What the app shell draws over every screen after loading. */
sealed interface NetworkDialogState {
    data object Hidden : NetworkDialogState

    /**
     * The dialog is shown. [checking] while "Retry" checks the network again; [stillOffline] after a retry that
     * found no network.
     */
    data class Shown(val checking: Boolean, val stillOffline: Boolean) : NetworkDialogState
}
