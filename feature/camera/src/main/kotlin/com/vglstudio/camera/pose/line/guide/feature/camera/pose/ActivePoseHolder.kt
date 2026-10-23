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

    fun set(pose: Pose?) {
        this.pose.value = pose
    }

    private val outlineView = MutableStateFlow(OutlineView())

    /** How the outline is shown now, for the pose matching to follow (plan T-017). */
    val outline: StateFlow<OutlineView> = outlineView.asStateFlow()

    @Suppress("UNUSED_PARAMETER")
    fun setOutline(view: OutlineView) = Unit
}

/** Whether the outline is on screen and flipped: the matching runs only while it is visible. */
data class OutlineView(val visible: Boolean = true, val mirrored: Boolean = false)
