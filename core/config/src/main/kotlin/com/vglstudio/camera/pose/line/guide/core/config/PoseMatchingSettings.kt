package com.vglstudio.camera.pose.line.guide.core.config

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Numbers of the pose matching, set remotely without a new release (spec camera, KB-5; plan T-017). */
interface PoseMatchingSettings {
    /** The score from which the pose counts as matched, 1 to 100. */
    val scoreThreshold: Flow<Int>

    /** The share of confidence, in percent (1 to 99), from which a landmark counts as in the frame. */
    val minInFramePercent: Flow<Int>
}

internal class DefaultPoseMatchingSettings @Inject constructor(private val remoteSettings: RemoteSettings) :
    PoseMatchingSettings {
    override val scoreThreshold: Flow<Int> = flowOf(DEFAULT_THRESHOLD)

    override val minInFramePercent: Flow<Int> = flowOf(DEFAULT_MIN_IN_FRAME_PERCENT)

    private companion object {
        const val DEFAULT_THRESHOLD = 80
        const val DEFAULT_MIN_IN_FRAME_PERCENT = 50
    }
}
