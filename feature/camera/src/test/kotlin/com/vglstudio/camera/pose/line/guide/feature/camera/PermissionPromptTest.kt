package com.vglstudio.camera.pose.line.guide.feature.camera

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The five rows of the table in plan T-026, section 3.1. */
class PermissionPromptTest {
    @Test
    fun granted_isShown() {
        assertTrue(permissionDialogShown(granted = true, rationaleBefore = false, rationaleAfter = false))
    }

    @Test
    fun firstDenial_isShown() {
        assertTrue(permissionDialogShown(granted = false, rationaleBefore = false, rationaleAfter = true))
    }

    @Test
    fun secondDenial_isShown() {
        assertTrue(permissionDialogShown(granted = false, rationaleBefore = true, rationaleAfter = false))
    }

    @Test
    fun permanentDenial_isNotShown() {
        assertFalse(permissionDialogShown(granted = false, rationaleBefore = false, rationaleAfter = false))
    }

    @Test
    fun deniedWithBothRationales_isShown() {
        assertTrue(permissionDialogShown(granted = false, rationaleBefore = true, rationaleAfter = true))
    }
}
