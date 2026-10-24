package com.vglstudio.camera.pose.line.guide.feature.camera.match

private const val ROTATED_QUARTER = 90
private const val ROTATED_THREE_QUARTERS = 270
private val LANDMARK_TYPES = 0..32

/** A landmark as ML Kit returns it: pixel coordinates in the upright image, [type] is the MediaPipe index. */
data class RawLandmark(val type: Int, val x: Float, val y: Float, val inFrame: Float)

/**
 * Turns the pixels of the analysed image into frame space (0 to 1, as the user sees the preview). The image is as the
 * sensor gives it ([imageWidth] x [imageHeight]); a rotation of 90 or 270 degrees makes the upright image
 * [imageHeight] x [imageWidth]. The front camera shows a mirror, so [mirrored] flips x. Landmarks of an unknown type
 * are dropped; coordinates outside the image are held at its edge.
 */
internal fun toBodyPose(
    points: List<RawLandmark>,
    imageWidth: Int,
    imageHeight: Int,
    rotationDegrees: Int,
    mirrored: Boolean,
): BodyPose {
    val turned = rotationDegrees == ROTATED_QUARTER || rotationDegrees == ROTATED_THREE_QUARTERS
    val width = (if (turned) imageHeight else imageWidth).coerceAtLeast(1).toFloat()
    val height = (if (turned) imageWidth else imageHeight).coerceAtLeast(1).toFloat()
    val landmarks = points.filter { it.type in LANDMARK_TYPES }.associate { point ->
        val x = (point.x / width).coerceIn(0f, 1f)
        point.type to BodyLandmark(
            x = if (mirrored) 1f - x else x,
            y = (point.y / height).coerceIn(0f, 1f),
            inFrame = point.inFrame,
        )
    }
    return BodyPose(landmarks, mirrored)
}
