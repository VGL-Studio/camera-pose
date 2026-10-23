package com.vglstudio.camera.pose.line.guide.feature.camera.layout

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The width from which the camera and the review screen use the unfolded layout (spec camera, KB-11; plan T-020). */
class WindowLayoutTest {
    @Test
    fun isUnfolded_from600dp() {
        assertFalse(isUnfolded(411.dp))
        assertFalse(isUnfolded(599.9.dp))
        assertTrue(isUnfolded(600.dp))
        assertTrue(isUnfolded(673.dp))
    }

    @Test
    fun threshold_is600dp() {
        assertEquals(600.dp, UNFOLDED_MIN_WIDTH)
    }
}
