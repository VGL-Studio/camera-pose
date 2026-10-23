package com.vglstudio.camera.pose.line.guide.feature.poses.data

import com.vglstudio.camera.pose.line.guide.core.poses.LoadFailureReason
import com.vglstudio.camera.pose.line.guide.core.poses.PoseCatalogRepository
import com.vglstudio.camera.pose.line.guide.core.poses.PoseContent
import com.vglstudio.camera.pose.line.guide.core.poses.PoseLoadFailure
import com.vglstudio.camera.pose.line.guide.core.poses.model.Pose
import com.vglstudio.camera.pose.line.guide.feature.poses.di.PosesIoDispatcher
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher

// Step 3 skeleton (plan T-021).
internal class DefaultPoseThumbnailRepository @Inject constructor(
    private val catalogRepository: PoseCatalogRepository,
    private val decoder: PoseImageDecoder,
    @PosesIoDispatcher private val dispatcher: CoroutineDispatcher,
) : PoseThumbnailRepository {
    override suspend fun load(pose: Pose): ThumbnailResult =
        ThumbnailResult.Failed(PoseLoadFailure(PoseContent.THUMBNAIL, LoadFailureReason.OTHER))
}
