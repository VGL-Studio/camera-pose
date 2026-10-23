package com.vglstudio.camera.pose.line.guide.core.review

/** Wall-clock time: the 30 days must survive a restart of the device. */
internal fun interface ReviewClock {
    fun nowMillis(): Long
}
