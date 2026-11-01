package com.vglstudio.camera.pose.line.guide.feature.poses.data

import com.vglstudio.camera.pose.line.guide.core.poses.PoseHistoryRepository
import com.vglstudio.camera.pose.line.guide.core.poses.PoseLibraryRepository
import com.vglstudio.camera.pose.line.guide.core.poses.PremiumPoseRepository
import com.vglstudio.camera.pose.line.guide.core.poses.model.LibraryState
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseFilter
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseLibrary
import com.vglstudio.camera.pose.line.guide.core.poses.model.PremiumAccess
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * The library of :core:poses, copied list by list into [LibraryContent]. The order is the one of :core:poses; nothing
 * is sorted again here (plan T-021, section 3.1). The locked poses follow the premium access of :core:poses (plan
 * T-086).
 */
internal class DefaultPosesLibrarySource @Inject constructor(
    private val libraryRepository: PoseLibraryRepository,
    private val historyRepository: PoseHistoryRepository,
    private val premiumRepository: PremiumPoseRepository,
) : PosesLibrarySource {
    // The locks follow the flag and the unlocked list of :core:poses (T-085): a new access gives a new content.
    override val library: Flow<LibraryResult> =
        combine(libraryRepository.library, premiumRepository.access) { state, access ->
            when (state) {
                LibraryState.Loading -> LibraryResult.Loading
                is LibraryState.Failed -> LibraryResult.Failed(state.failure)
                is LibraryState.Ready -> LibraryResult.Ready(state.library.toContent(access))
            }
        }

    override suspend fun retry() = libraryRepository.retry()

    override suspend fun setSaved(poseId: String, saved: Boolean) = historyRepository.setSaved(poseId, saved)

    override suspend fun unlock(poseId: String): Boolean = premiumRepository.unlock(poseId)

    private fun PoseLibrary.toContent(access: PremiumAccess): LibraryContent {
        val filters = listOf(PoseFilter.All, PoseFilter.Saved) + categories.map { PoseFilter.Category(it.id) }
        return LibraryContent(
            categories = categories,
            totalPoses = totalPoses,
            savedIds = savedIds,
            lockedIds = posesFor(PoseFilter.All).filter(access::isLocked).mapTo(mutableSetOf()) { it.id },
            lists = filters.associateWith { posesFor(it) },
        )
    }
}
