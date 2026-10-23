package com.vglstudio.camera.pose.line.guide.feature.camera.review

import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "Tư thế tiếp" of the review screen, handed to the camera: the review screen asks, the camera takes the request
 * once when it starts again and selects the next pose (PoseViewModel.onNextPose, T-014).
 */
@Singleton
class NextPoseRequests @Inject constructor() {
    private val requested = AtomicBoolean(false)

    fun request() {
        requested.set(true)
    }

    /** True once after [request], then false until the next request. */
    fun consume(): Boolean = requested.getAndSet(false)
}
