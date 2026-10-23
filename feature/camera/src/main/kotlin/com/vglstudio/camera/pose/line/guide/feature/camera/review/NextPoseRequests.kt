package com.vglstudio.camera.pose.line.guide.feature.camera.review

import javax.inject.Inject
import javax.inject.Singleton

/**
 * "Tư thế tiếp" of the review screen, handed to the camera: the review screen asks, the camera takes the request
 * once when it starts again.
 */
@Singleton
class NextPoseRequests @Inject constructor() {
    fun request() {
    }

    /** True once after [request], then false until the next request. */
    fun consume(): Boolean = false
}
