package com.vglstudio.camera.pose.line.guide.core.ads

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** S-18: only the staging options can reset consent. */
class ConsentDebugOptionsTest {
    @Test
    fun onlyStagingAllowsReset() {
        assertTrue(ConsentDebugOptions.EEA_TEST_DEVICE.allowConsentReset)
        assertFalse(ConsentDebugOptions.NONE.allowConsentReset)
    }
}
