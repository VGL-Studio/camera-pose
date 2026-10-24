package com.vglstudio.camera.pose.line.guide.core.config

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Numbers of the pose matching, set remotely without a new release (spec camera, KB-5; plan T-017). */
interface PoseMatchingSettings {
    /** The score from which the pose counts as matched, 1 to 100. */
    val scoreThreshold: Flow<Int>

    /** The share of confidence, in percent (1 to 99), from which a landmark counts as in the frame. */
    val minInFramePercent: Flow<Int>
}

/** Reads the numbers from Remote Config; a value outside its range (a typing mistake there) is replaced by the default. */
internal class DefaultPoseMatchingSettings @Inject constructor(private val remoteSettings: RemoteSettings) :
    PoseMatchingSettings {
    override val scoreThreshold: Flow<Int> = remoteSettings
        .long(THRESHOLD_KEY, DEFAULT_THRESHOLD.toLong())
        .map { value -> if (value in 1L..MAX_THRESHOLD) value.toInt() else DEFAULT_THRESHOLD }

    override val minInFramePercent: Flow<Int> = remoteSettings
        .long(MIN_IN_FRAME_KEY, DEFAULT_MIN_IN_FRAME_PERCENT.toLong())
        .map { value -> if (value in 1L..MAX_MIN_IN_FRAME) value.toInt() else DEFAULT_MIN_IN_FRAME_PERCENT }

    private companion object {
        const val THRESHOLD_KEY = "pose_match_threshold"
        const val MIN_IN_FRAME_KEY = "pose_match_min_in_frame_percent"
        const val DEFAULT_THRESHOLD = 80
        const val DEFAULT_MIN_IN_FRAME_PERCENT = 50
        const val MAX_THRESHOLD = 100L
        const val MAX_MIN_IN_FRAME = 99L
    }
}
