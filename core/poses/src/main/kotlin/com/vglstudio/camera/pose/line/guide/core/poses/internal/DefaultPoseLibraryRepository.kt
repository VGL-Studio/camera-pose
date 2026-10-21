package com.vglstudio.camera.pose.line.guide.core.poses.internal

import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.core.poses.PoseCatalogRepository
import com.vglstudio.camera.pose.line.guide.core.poses.PoseHistoryRepository
import com.vglstudio.camera.pose.line.guide.core.poses.PoseLibraryRepository
import com.vglstudio.camera.pose.line.guide.core.poses.model.LibraryState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

internal class DefaultPoseLibraryRepository(
    private val catalogRepository: PoseCatalogRepository,
    private val history: PoseHistoryRepository,
    private val logger: AppLogger,
) : PoseLibraryRepository {
    override val library: Flow<LibraryState> = emptyFlow()

    override suspend fun retry() = Unit
}
