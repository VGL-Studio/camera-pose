package com.vglstudio.camera.pose.line.guide.feature.camera.pose

import com.vglstudio.camera.pose.line.guide.core.poses.model.Pose
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The pose the camera shows now; null when there is none. Written by PoseViewModel, read at capture (and by T-017). */
@Singleton
class ActivePoseHolder @Inject constructor() {
    private val pose = MutableStateFlow<Pose?>(null)
    val current: StateFlow<Pose?> = pose.asStateFlow()

    @Suppress("UNUSED_PARAMETER")
    fun set(pose: Pose?) {
    }
}
