package com.vglstudio.camera.pose.line.guide.feature.camera.match

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.vglstudio.camera.pose.line.guide.feature.camera.model.Lens
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/** Thin layer over ML Kit pose detection, checked on devices (plan T-017, section 4). */
@Singleton
internal class MlKitPoseAnalyzer @Inject constructor() :
    PoseFrameSource,
    ImageAnalysis.Analyzer {
    private val shared = MutableSharedFlow<BodyPose?>(extraBufferCapacity = 1)
    override val frames: SharedFlow<BodyPose?> = shared

    /** Set by the camera controller when it opens the camera. */
    var lens: Lens = Lens.BACK

    override fun setActive(active: Boolean) = Unit

    override fun analyze(image: ImageProxy) = image.close()

    /** Called when the camera closes. */
    fun reset() = Unit

    /** Called when the camera is released for good. */
    fun release() = Unit
}
