package com.vglstudio.camera.pose.line.guide.feature.camera.match

import kotlinx.coroutines.flow.SharedFlow

/** One landmark in frame space: x, y from 0 to 1 as the user sees the preview (mirrored for the front camera). */
data class BodyLandmark(val x: Float, val y: Float, val inFrame: Float)

/** Landmarks by MediaPipe index (0 nose to 32 right foot index); missing when ML Kit did not return it. */
data class BodyPose(val landmarks: Map<Int, BodyLandmark>, val mirrored: Boolean)

/**
 * Frames analysed by ML Kit, one emission per analysed frame; null when the camera closed. A SharedFlow, not a
 * StateFlow: frames without a person are equal to each other and must not be merged (plan T-017, version 2).
 */
interface PoseFrameSource {
    val frames: SharedFlow<BodyPose?>

    /** Runs detection only while active; otherwise each frame is closed at once. */
    fun setActive(active: Boolean)
}
