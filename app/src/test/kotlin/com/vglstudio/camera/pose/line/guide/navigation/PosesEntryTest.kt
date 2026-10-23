package com.vglstudio.camera.pose.line.guide.navigation

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The library button of the camera follows feature_poses_enabled (spec poses, KB-1; plan T-021). */
class PosesEntryTest {
    @Test
    fun flagOff_noOpener() {
        assertNull(posesOpener(enabled = false) {})
    }

    @Test
    fun flagOn_openerOpensLibrary() {
        var opened = false
        val opener = posesOpener(enabled = true) { opened = true }
        assertNotNull(opener)
        opener!!.invoke()
        assertTrue(opened)
    }
}
