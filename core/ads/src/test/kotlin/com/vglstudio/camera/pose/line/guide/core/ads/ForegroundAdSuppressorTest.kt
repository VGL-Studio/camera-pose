package com.vglstudio.camera.pose.line.guide.core.ads

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** G-01: a counter, not a flag; one source releasing never clears another one's hold. */
class ForegroundAdSuppressorTest {
    @Test
    fun countsSources_neverNegative() {
        val suppressor = ForegroundAdSuppressor()
        assertFalse(suppressor.suppressed.value)
        val first = suppressor.acquire("consent_form")
        val second = suppressor.acquire("privacy_form")
        assertTrue(suppressor.suppressed.value)
        first.release()
        assertTrue(suppressor.suppressed.value)
        second.release()
        assertFalse(suppressor.suppressed.value)
        // Releasing again does nothing: a new hold suppresses again.
        first.release()
        second.release()
        suppressor.acquire("again")
        assertTrue(suppressor.suppressed.value)
    }
}
