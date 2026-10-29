package com.vglstudio.camera.pose.line.guide.core.ads

import org.junit.Assert.assertEquals
import org.junit.Test

/** The media of the full native ad shrinks first when the text grows, never below its floor (plan T-044, KB-2). */
class FullMediaHeightTest {
    @Test
    fun fontScale1_keepsDesignMedia() {
        assertEquals(200, fullMediaHeight(inner = 392, others = 192, floor = 120))
    }

    @Test
    fun largerText_shrinksMedia() {
        assertEquals(134, fullMediaHeight(inner = 392, others = 258, floor = 120))
    }

    @Test
    fun belowFloor_keepsFloor() {
        assertEquals(120, fullMediaHeight(inner = 392, others = 300, floor = 120))
    }

    @Test
    fun noRoomLeft_keepsFloor() {
        assertEquals(120, fullMediaHeight(inner = 392, others = 392, floor = 120))
    }
}
