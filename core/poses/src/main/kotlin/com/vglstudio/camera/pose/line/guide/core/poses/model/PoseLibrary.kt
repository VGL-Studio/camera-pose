package com.vglstudio.camera.pose.line.guide.core.poses.model

import com.vglstudio.camera.pose.line.guide.core.poses.PoseLoadFailure
import com.vglstudio.camera.pose.line.guide.core.poses.internal.buildPoseLibrary

data class SavedPose(val id: String, val savedAtMillis: Long)

data class UsedPose(val id: String, val usedAtMillis: Long)

sealed interface PoseFilter {
    data object All : PoseFilter

    data object Saved : PoseFilter

    data class Category(val id: String) : PoseFilter
}

sealed interface LibraryState {
    data object Loading : LibraryState

    data class Ready(val library: PoseLibrary) : LibraryState

    data class Failed(val failure: PoseLoadFailure) : LibraryState
}

/**
 * The poses a person can browse: the catalog with the saved and recently used poses applied. Lists are computed
 * once, when the library is built.
 */
class PoseLibrary internal constructor(
    /** Categories with at least one pose, in catalog order. */
    val categories: List<PoseCategory>,
    /** Every valid pose of the catalog. */
    val totalPoses: Int,
    /** Saved poses that are still in the catalog. */
    val savedIds: Set<String>,
    /** The first pose of the catalog: what the camera uses the very first time. */
    val defaultPose: Pose?,
    /** The most recently used pose that is still in the catalog. */
    val lastUsedPose: Pose?,
    private val all: List<Pose>,
    private val saved: List<Pose>,
    private val byCategory: Map<String, List<Pose>>,
) {
    fun posesFor(filter: PoseFilter): List<Pose> = when (filter) {
        PoseFilter.All -> all
        PoseFilter.Saved -> saved
        is PoseFilter.Category -> byCategory[filter.id].orEmpty()
    }

    companion object {
        /** The library of [catalog] with [saved] and [recent] applied: the same ordering the repository uses. */
        fun from(
            catalog: PoseCatalog,
            saved: List<SavedPose> = emptyList(),
            recent: List<UsedPose> = emptyList(),
        ): PoseLibrary = buildPoseLibrary(catalog, saved, recent)
    }
}
