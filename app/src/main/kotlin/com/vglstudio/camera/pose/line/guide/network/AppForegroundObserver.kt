package com.vglstudio.camera.pose.line.guide.network

import androidx.activity.ComponentActivity

internal fun ComponentActivity.observeAppForeground(
    onStarted: () -> Unit,
    onStopped: (changingConfigurations: Boolean) -> Unit,
) {
}
