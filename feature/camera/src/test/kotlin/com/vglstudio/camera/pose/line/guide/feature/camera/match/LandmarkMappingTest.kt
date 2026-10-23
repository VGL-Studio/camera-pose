package com.vglstudio.camera.pose.line.guide.feature.camera.match

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The change from the pixels ML Kit returns to the frame the user sees (plan T-017, version 2). The analysed image is
 * 640 x 480 as the sensor gives it; a rotation of 90 or 270 degrees makes it 480 x 640 upright.
 */
class LandmarkMappingTest {
    private fun map(
        type: Int,
        x: Float,
        y: Float,
        rotation: Int,
        mirrored: Boolean = false,
        inFrame: Float = 0.9f,
    ): BodyPose = toBodyPose(listOf(RawLandmark(type, x, y, inFrame)), 640, 480, rotation, mirrored)

    private fun BodyPose.point(type: Int): Pair<Float, Float> {
        val landmark = landmarks[type]
        assertNotNull("landmark $type in $landmarks", landmark)
        return landmark!!.x to landmark.y
    }

    private fun assertPoint(expectedX: Float, expectedY: Float, pose: BodyPose, type: Int = 0) {
        val (x, y) = pose.point(type)
        assertEquals("x", expectedX, x, TOLERANCE)
        assertEquals("y", expectedY, y, TOLERANCE)
    }

    @Test
    fun rotation0_normalisesByWidthAndHeight() {
        assertPoint(0.5f, 0.25f, map(0, 320f, 120f, rotation = 0))
    }

    @Test
    fun rotation90_swapsWidthAndHeight() {
        assertPoint(0.25f, 0.75f, map(0, 120f, 480f, rotation = 90))
    }

    @Test
    fun rotation270_swapsWidthAndHeight() {
        assertPoint(0.25f, 0.75f, map(0, 120f, 480f, rotation = 270))
    }

    @Test
    fun rotation180_keepsSize() {
        assertPoint(0.5f, 0.25f, map(0, 320f, 120f, rotation = 180))
    }

    @Test
    fun frontCamera_mirrorsX() {
        val pose = map(0, 120f, 480f, rotation = 270, mirrored = true)
        assertPoint(0.75f, 0.75f, pose)
        assertTrue(pose.mirrored)
        // The same point from the back camera is not flipped and not marked.
        val back = map(0, 120f, 480f, rotation = 270)
        assertPoint(0.25f, 0.75f, back)
        assertFalse(back.mirrored)
    }

    @Test
    fun outOfImage_isClamped() {
        assertPoint(0f, 1f, map(0, -10f, 1000f, rotation = 0))
        assertPoint(1f, 0f, map(0, 900f, -5f, rotation = 0))
        // The valid point is in the result, so clamping did not drop it.
        assertPoint(0.5f, 0.25f, map(0, 320f, 120f, rotation = 0))
    }

    @Test
    fun unknownType_isDropped() {
        val pose = toBodyPose(
            listOf(
                RawLandmark(0, 320f, 120f, 0.9f),
                RawLandmark(33, 320f, 120f, 0.9f),
                RawLandmark(-1, 320f, 120f, 0.9f),
                RawLandmark(32, 64f, 48f, 0.9f),
            ),
            imageWidth = 640,
            imageHeight = 480,
            rotationDegrees = 0,
            mirrored = false,
        )
        assertEquals(setOf(0, 32), pose.landmarks.keys)
        assertNull(pose.landmarks[33])
        assertPoint(0.1f, 0.1f, pose, type = 32)
    }

    @Test
    fun inFrame_isKept() {
        assertEquals(0.7f, map(11, 320f, 120f, rotation = 0, inFrame = 0.7f).landmarks.getValue(11).inFrame, TOLERANCE)
    }

    private companion object {
        const val TOLERANCE = 1e-4f
    }
}
