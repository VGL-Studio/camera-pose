package com.vglstudio.camera.pose.line.guide.network

import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Reports to [onStarted] each start of [this] activity and to [onStopped] each stop, with whether the stop is a
 * configuration change (plan T-077). Registered in onCreate, so a recreated activity reports too.
 */
internal fun ComponentActivity.observeAppForeground(
    onStarted: () -> Unit,
    onStopped: (changingConfigurations: Boolean) -> Unit,
) {
    lifecycle.addObserver(
        object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = onStarted()

            override fun onStop(owner: LifecycleOwner) = onStopped(isChangingConfigurations)
        },
    )
}
