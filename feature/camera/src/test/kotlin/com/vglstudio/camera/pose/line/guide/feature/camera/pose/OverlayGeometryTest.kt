package com.vglstudio.camera.pose.line.guide.feature.camera.pose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The pure functions of the outline adjustment (spec camera, KB-3; plan T-015). */
class OverlayGeometryTest {
    @Test
    fun clampOffset_keepsCentreInFrame() {
        assertEquals(-0.5f, clampOffset(-0.7f), 0f)
        assertEquals(-0.5f, clampOffset(-0.5f), 0f)
        assertEquals(0f, clampOffset(0f), 0f)
        assertEquals(0.5f, clampOffset(0.5f), 0f)
        assertEquals(0.5f, clampOffset(0.7f), 0f)
    }

    @Test
    fun clampScale_isFortyToOneSixtyPercent() {
        assertEquals(0.4f, clampScale(0.1f), 0f)
        assertEquals(0.4f, clampScale(0.4f), 0f)
        assertEquals(1.2f, clampScale(1.2f), 0f)
        assertEquals(1.6f, clampScale(1.6f), 0f)
        assertEquals(1.6f, clampScale(2.5f), 0f)
    }

    @Test
    fun clampOpacity_isTwentyToHundredPercent() {
        assertEquals(0.2f, clampOpacity(0f), 0f)
        assertEquals(0.2f, clampOpacity(0.2f), 0f)
        assertEquals(0.7f, clampOpacity(0.7f), 0f)
        assertEquals(1f, clampOpacity(1f), 0f)
        assertEquals(1f, clampOpacity(4f), 0f)
    }

    @Test
    fun percent_roundsToWholePercent() {
        assertEquals(70, percent(0.704f))
        assertEquals(71, percent(0.706f))
        assertEquals(100, percent(1.0f))
        assertEquals(20, percent(0.2f))
        assertEquals(160, percent(1.6f))
    }

    private fun place(containerWidth: Float, frameWidth: Float, frameHeight: Float, stepBottom: Float) =
        toolColumnPlacement(containerWidth, frameWidth, frameHeight, stepBottom)

    private fun assertInFrame(placement: ToolColumnPlacement, x: Float, y: Float, overflow: Float = 0f) {
        assertFalse("inside the frame: $placement", placement.outsideFrame)
        assertEquals("x of $placement", x, placement.x, 0.01f)
        assertEquals("y of $placement", y, placement.y, 0.01f)
        assertEquals("overflow of $placement", overflow, placement.overflow, 0.01f)
    }

    private fun assertOutside(placement: ToolColumnPlacement, x: Float, y: Float) {
        assertTrue("in the margin: $placement", placement.outsideFrame)
        assertEquals("x of $placement", x, placement.x, 0.01f)
        assertEquals("y of $placement", y, placement.y, 0.01f)
        assertEquals("overflow of $placement", 0f, placement.overflow, 0.01f)
    }

    @Test
    fun toolColumnPlacement_byFrame() {
        // Standard 411 x 891: container and frame 411 x 548, one-row step card (bottom 84): the design (347, 150).
        assertInFrame(place(411f, 411f, 548f, 84f), x = 347f, y = 150f)
        // 360 x 640: container 360 x 296, frame 222 x 296, two-row step card (bottom 140): margin 69, in the margin.
        assertOutside(place(360f, 222f, 296f, 140f), x = 301.5f, y = 80f)
        // 411 x 731: container 411 x 387, frame 290 x 387: margin 60.5 is too small, the column fits in the frame.
        assertInFrame(place(411f, 290f, 387f, 140f), x = 286.5f, y = 150f)
        // 360 x 690: container 360 x 346, frame 259 x 346: margin 50.5 from 48 up, in the margin at the frame edge.
        assertOutside(place(360f, 259f, 346f, 140f), x = 310.75f, y = 130f)
        // 360 x 720: container 360 x 376, frame 282 x 376: margin 39, the column is in the frame and overflows 4.
        assertInFrame(place(360f, 282f, 376f, 140f), x = 257f, y = 148f, overflow = 4f)
    }
}
