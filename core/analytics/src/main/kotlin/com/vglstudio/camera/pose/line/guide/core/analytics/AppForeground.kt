package com.vglstudio.camera.pose.line.guide.core.analytics

/** Calls a listener each time the whole process comes back to foreground (not the first time). */
internal interface AppForeground {
    fun onEachReturn(listener: () -> Unit)
}
