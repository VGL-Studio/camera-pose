package com.vglstudio.camera.pose.line.guide.feature.poses.data

import com.vglstudio.camera.pose.line.guide.core.poses.LoadFailureReason
import com.vglstudio.camera.pose.line.guide.core.poses.PoseCatalogRepository
import com.vglstudio.camera.pose.line.guide.core.poses.PoseContent
import com.vglstudio.camera.pose.line.guide.core.poses.PoseFileResult
import com.vglstudio.camera.pose.line.guide.core.poses.PoseLoadFailure
import com.vglstudio.camera.pose.line.guide.core.poses.model.Pose
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseFile
import com.vglstudio.camera.pose.line.guide.feature.poses.di.PosesIoDispatcher
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * The thumbnail file of :core:poses (on the device, or from R2 within 15 seconds; ADR-1001), decoded on [dispatcher].
 * A file that cannot be decoded is a failure of reason OTHER.
 */
internal class DefaultPoseThumbnailRepository @Inject constructor(
    private val catalogRepository: PoseCatalogRepository,
    private val decoder: PoseImageDecoder,
    @PosesIoDispatcher private val dispatcher: CoroutineDispatcher,
) : PoseThumbnailRepository {
    override suspend fun load(pose: Pose): ThumbnailResult =
        when (val result = catalogRepository.loadFile(pose, PoseFile.THUMBNAIL)) {
            is PoseFileResult.Failed -> ThumbnailResult.Failed(result.failure)
            is PoseFileResult.Ready -> {
                val image = withContext(dispatcher) { decoder.decode(result.file, THUMBNAIL_WIDTH_PX) }
                if (image != null) {
                    ThumbnailResult.Ready(image)
                } else {
                    ThumbnailResult.Failed(PoseLoadFailure(PoseContent.THUMBNAIL, LoadFailureReason.OTHER))
                }
            }
        }

    private companion object {
        // A card is 162 dp wide at most on a phone: 486 px at xxhdpi. preview_540 keeps its size.
        const val THUMBNAIL_WIDTH_PX = 486
    }
}
