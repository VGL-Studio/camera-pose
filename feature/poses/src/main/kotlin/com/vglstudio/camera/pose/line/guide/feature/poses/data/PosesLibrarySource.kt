package com.vglstudio.camera.pose.line.guide.feature.poses.data

import com.vglstudio.camera.pose.line.guide.core.poses.PoseLoadFailure
import com.vglstudio.camera.pose.line.guide.core.poses.model.Pose
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseCategory
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseFilter
import kotlinx.coroutines.flow.Flow

/**
 * The library as the screen needs it: a copy of PoseLibrary of :core:poses, which tests of this module cannot build
 * (plan T-021). The order of every list is the order of :core:poses (saved, recently used, then catalog order).
 */
data class LibraryContent(
    /** Categories with at least one pose, in catalog order. */
    val categories: List<PoseCategory>,
    /** Every valid pose of the catalog. */
    val totalPoses: Int,
    /** Saved poses that are still in the catalog. */
    val savedIds: Set<String>,
    /** Premium poses not unlocked yet; empty while premium poses are off (spec poses, KB-8). */
    val lockedIds: Set<String> = emptySet(),
    private val lists: Map<PoseFilter, List<Pose>>,
) {
    /** The poses of [filter]; empty for a filter that has no list. */
    fun posesFor(filter: PoseFilter): List<Pose> = lists[filter].orEmpty()
}

sealed interface LibraryResult {
    data object Loading : LibraryResult

    data class Ready(val content: LibraryContent) : LibraryResult

    data class Failed(val failure: PoseLoadFailure) : LibraryResult
}

/** The pose library and the saved poses (spec poses, KB-1, KB-3, KB-5). */
interface PosesLibrarySource {
    /** Starts loading the catalog when nothing has loaded it yet. */
    val library: Flow<LibraryResult>

    suspend fun retry()

    /** Throws [java.io.IOException] when the saved list cannot be written. */
    suspend fun setSaved(poseId: String, saved: Boolean)

    /**
     * Unlocks [poseId] for good (spec poses, KB-8). False when premium poses are off, so nothing was written and no
     * unlock is logged. Throws [java.io.IOException] when the list cannot be written.
     */
    suspend fun unlock(poseId: String): Boolean
}
