package com.vglstudio.camera.pose.line.guide.feature.poses.data

import com.vglstudio.camera.pose.line.guide.core.poses.PoseHistoryRepository
import com.vglstudio.camera.pose.line.guide.core.poses.PoseLibraryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// Step 3 skeleton (plan T-021): the real copy comes with the code commit.
internal class DefaultPosesLibrarySource @Inject constructor(
    private val libraryRepository: PoseLibraryRepository,
    private val historyRepository: PoseHistoryRepository,
) : PosesLibrarySource {
    override val library: Flow<LibraryResult> = flowOf(LibraryResult.Loading)

    override suspend fun retry() = Unit

    override suspend fun setSaved(poseId: String, saved: Boolean) = Unit
}
