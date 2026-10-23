package com.vglstudio.camera.pose.line.guide.feature.poses.data

import androidx.compose.ui.graphics.ImageBitmap
import com.vglstudio.camera.pose.line.guide.core.poses.PoseLoadFailure
import com.vglstudio.camera.pose.line.guide.core.poses.model.Pose

sealed interface PoseImageResult {
    data class Ready(val image: ImageBitmap) : PoseImageResult

    data class Failed(val failure: PoseLoadFailure) : PoseImageResult
}

/** Loads the large image of the pose detail: from the device when it is there, else from R2 (ADR-0015). */
interface PoseImageRepository {
    suspend fun load(pose: Pose): PoseImageResult
}
