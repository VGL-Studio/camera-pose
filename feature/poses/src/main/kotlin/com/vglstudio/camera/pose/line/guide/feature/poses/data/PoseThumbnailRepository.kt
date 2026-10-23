package com.vglstudio.camera.pose.line.guide.feature.poses.data

import androidx.compose.ui.graphics.ImageBitmap
import com.vglstudio.camera.pose.line.guide.core.poses.PoseLoadFailure
import com.vglstudio.camera.pose.line.guide.core.poses.model.Pose

sealed interface ThumbnailResult {
    data class Ready(val image: ImageBitmap) : ThumbnailResult

    data class Failed(val failure: PoseLoadFailure) : ThumbnailResult
}

/** Loads the thumbnail of a pose card: from the device when it is there, else from R2 (ADR-0015). */
interface PoseThumbnailRepository {
    suspend fun load(pose: Pose): ThumbnailResult
}
