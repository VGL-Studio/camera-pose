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

    /** The Remote Config keys of the pose matching, declared once here (ADR-0023). */
    companion object {
        val THRESHOLD: RemoteKey<Long> = RemoteKey.long(
            "pose_match_threshold",
            80,
            "Score, 1 to 100, from which the pose on the camera counts as matched; out of range uses 80.",
        )
        val MIN_IN_FRAME: RemoteKey<Long> = RemoteKey.long(
            "pose_match_min_in_frame_percent",
            50,
            "Confidence in percent, 1 to 99, from which a body point counts as in the frame; out of range uses 50.",
        )

        /** Every key of this file, for the list of the app's keys. */
        val KEYS: List<RemoteKey<*>> = listOf(THRESHOLD, MIN_IN_FRAME)
    }
}

/** Reads the numbers from Remote Config; a value outside its range (a typing mistake there) is replaced by the default. */
internal class DefaultPoseMatchingSettings @Inject constructor(private val remoteSettings: RemoteSettings) :
    PoseMatchingSettings {
    override val scoreThreshold: Flow<Int> = remoteSettings
        .value(PoseMatchingSettings.THRESHOLD)
        .map { value -> if (value in 1L..MAX_THRESHOLD) value.toInt() else DEFAULT_THRESHOLD }

    override val minInFramePercent: Flow<Int> = remoteSettings
        .value(PoseMatchingSettings.MIN_IN_FRAME)
        .map { value -> if (value in 1L..MAX_MIN_IN_FRAME) value.toInt() else DEFAULT_MIN_IN_FRAME_PERCENT }

    private companion object {
        val DEFAULT_THRESHOLD = PoseMatchingSettings.THRESHOLD.default.toInt()
        val DEFAULT_MIN_IN_FRAME_PERCENT = PoseMatchingSettings.MIN_IN_FRAME.default.toInt()
        const val MAX_THRESHOLD = 100L
        const val MAX_MIN_IN_FRAME = 99L
    }
}
