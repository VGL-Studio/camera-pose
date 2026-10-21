package com.vglstudio.camera.pose.line.guide.core.poses.internal

/** Current time in milliseconds. A separate type so tests can fix the time. */
internal fun interface PosesClock {
    fun nowMillis(): Long
}
