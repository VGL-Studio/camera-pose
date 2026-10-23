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
 * The large image file of :core:poses (on the device, or from R2 within 15 seconds; ADR-1001), decoded on
 * [dispatcher]. A file that cannot be decoded is a failure of reason OTHER.
 */
internal class DefaultPoseImageRepository @Inject constructor(
    private val catalogRepository: PoseCatalogRepository,
    private val decoder: PoseImageDecoder,
    @PosesIoDispatcher private val dispatcher: CoroutineDispatcher,
) : PoseImageRepository {
    override suspend fun load(pose: Pose): PoseImageResult =
        when (val result = catalogRepository.loadFile(pose, PoseFile.IMAGE)) {
            is PoseFileResult.Failed -> PoseImageResult.Failed(result.failure)
            is PoseFileResult.Ready -> {
                val image = withContext(dispatcher) { decoder.decode(result.file, IMAGE_WIDTH_PX) }
                if (image != null) {
                    PoseImageResult.Ready(image)
                } else {
                    PoseImageResult.Failed(PoseLoadFailure(PoseContent.IMAGE, LoadFailureReason.OTHER))
                }
            }
        }

    private companion object {
        // The image area is 379 dp wide at most on a phone: 1137 px at xxhdpi. preview_1200 keeps its size.
        const val IMAGE_WIDTH_PX = 1137
    }
}
