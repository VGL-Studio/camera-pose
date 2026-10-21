package com.vglstudio.camera.pose.line.guide.core.poses

import com.vglstudio.camera.pose.line.guide.core.poses.model.SavedPose
import com.vglstudio.camera.pose.line.guide.core.poses.model.UsedPose
import kotlinx.coroutines.flow.Flow

/** Saved poses and recently used poses, kept on the device. */
interface PoseHistoryRepository {
    /** Newest saved first. */
    val saved: Flow<List<SavedPose>>

    /** Most recently used first. */
    val recent: Flow<List<UsedPose>>

    /** Throws [java.io.IOException] when the list cannot be written; the caller tells the user. */
    suspend fun setSaved(poseId: String, saved: Boolean)

    /** Moves the pose to the front of the recent list, which keeps 20 poses. Throws [java.io.IOException]. */
    suspend fun markUsed(poseId: String)
}
