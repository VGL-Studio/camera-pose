package com.vglstudio.camera.pose.line.guide.feature.poses.data

import com.vglstudio.camera.pose.line.guide.core.poses.PoseHistoryRepository
import com.vglstudio.camera.pose.line.guide.core.poses.PoseLibraryRepository
import com.vglstudio.camera.pose.line.guide.core.poses.PremiumPoseRepository
import com.vglstudio.camera.pose.line.guide.core.poses.model.LibraryState
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseFilter
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseLibrary
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The library of :core:poses, copied list by list into [LibraryContent]. The order is the one of :core:poses; nothing
 * is sorted again here (plan T-021, section 3.1).
 */
internal class DefaultPosesLibrarySource @Inject constructor(
    private val libraryRepository: PoseLibraryRepository,
    private val historyRepository: PoseHistoryRepository,
    private val premiumRepository: PremiumPoseRepository,
) : PosesLibrarySource {
    override val library: Flow<LibraryResult> = libraryRepository.library.map { state ->
        when (state) {
            LibraryState.Loading -> LibraryResult.Loading
            is LibraryState.Failed -> LibraryResult.Failed(state.failure)
            is LibraryState.Ready -> LibraryResult.Ready(state.library.toContent())
        }
    }

    override suspend fun retry() = libraryRepository.retry()

    override suspend fun setSaved(poseId: String, saved: Boolean) = historyRepository.setSaved(poseId, saved)

    // Empty frame of step 3 (plan T-086, section 4).
    override suspend fun unlock(poseId: String): Boolean = false

    private fun PoseLibrary.toContent(): LibraryContent {
        val filters = listOf(PoseFilter.All, PoseFilter.Saved) + categories.map { PoseFilter.Category(it.id) }
        return LibraryContent(
            categories = categories,
            totalPoses = totalPoses,
            savedIds = savedIds,
            lists = filters.associateWith { posesFor(it) },
        )
    }
}
