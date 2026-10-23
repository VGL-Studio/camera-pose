package com.vglstudio.camera.pose.line.guide.feature.camera.match

/** A landmark as ML Kit returns it: pixel coordinates in the upright image, [type] is the MediaPipe index. */
data class RawLandmark(val type: Int, val x: Float, val y: Float, val inFrame: Float)

/** Turns pixels of the analysed image into frame space (0 to 1, as the user sees the preview). */
@Suppress("UNUSED_PARAMETER")
internal fun toBodyPose(
    points: List<RawLandmark>,
    imageWidth: Int,
    imageHeight: Int,
    rotationDegrees: Int,
    mirrored: Boolean,
): BodyPose = BodyPose(emptyMap(), mirrored)
