package com.vglstudio.camera.pose.line.guide.feature.poses.data

import com.vglstudio.camera.pose.line.guide.core.poses.LoadFailureReason
import com.vglstudio.camera.pose.line.guide.core.poses.PoseCatalogRepository
import com.vglstudio.camera.pose.line.guide.core.poses.PoseContent
import com.vglstudio.camera.pose.line.guide.core.poses.PoseLoadFailure
import com.vglstudio.camera.pose.line.guide.core.poses.model.Pose
import com.vglstudio.camera.pose.line.guide.feature.poses.di.PosesIoDispatcher
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher

/** Empty frame (plan T-022, step 3): always fails. */
internal class DefaultPoseImageRepository @Inject constructor(
    private val catalogRepository: PoseCatalogRepository,
    private val decoder: PoseImageDecoder,
    @PosesIoDispatcher private val dispatcher: CoroutineDispatcher,
) : PoseImageRepository {
    override suspend fun load(pose: Pose): PoseImageResult =
        PoseImageResult.Failed(PoseLoadFailure(PoseContent.IMAGE, LoadFailureReason.OTHER))
}
