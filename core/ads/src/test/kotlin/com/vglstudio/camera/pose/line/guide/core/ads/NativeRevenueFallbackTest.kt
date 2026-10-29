package com.vglstudio.camera.pose.line.guide.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-02: a native ad with no paid event 2 s after its impression logs one zero-value impression. */
class NativeRevenueFallbackTest {
    private val fallback = NativeRevenueFallback()

    @Test
    fun noPaidEventIn2s_logsZeroOnce() {
        assertEquals(3_000L, fallback.onImpression(1_000))
        assertFalse(fallback.onDue(2_999))
        assertTrue(fallback.onDue(3_000))
        assertFalse(fallback.onDue(4_000))
    }

    @Test
    fun paidEventFirst_cancelsFallback() {
        fallback.onImpression(1_000)

        assertTrue(fallback.onPaidEvent())
        assertFalse(fallback.onDue(3_000))
    }

    @Test
    fun paidEventAfterFallback_isNotLoggedAgain() {
        fallback.onImpression(1_000)

        assertTrue(fallback.onDue(3_000))
        assertFalse(fallback.onPaidEvent())
    }
}
