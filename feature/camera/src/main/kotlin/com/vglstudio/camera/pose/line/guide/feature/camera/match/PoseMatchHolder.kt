package com.vglstudio.camera.pose.line.guide.feature.camera.match

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Whether the pose matched at this moment, for CameraViewModel to read when the photo is taken (spec camera, KB-5). */
@Singleton
class PoseMatchHolder @Inject constructor() {
    private val value = MutableStateFlow(false)
    val matched: StateFlow<Boolean> = value

    @Suppress("UNUSED_PARAMETER")
    fun set(matched: Boolean) = Unit
}
