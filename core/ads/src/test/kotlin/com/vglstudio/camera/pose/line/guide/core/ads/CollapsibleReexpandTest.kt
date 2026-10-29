package com.vglstudio.camera.pose.line.guide.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** CL-03: a collapsed layer opens again `time_expand_collap_ad` seconds after the tap, only with an ad (plan T-030). */
class CollapsibleReexpandTest {
    @Test
    fun notCollapsed_neverReexpands() {
        assertNull(collapsibleReexpandAtMillis(collapsedAtMillis = null, expandAfterSeconds = 30))
    }

    @Test
    fun thirtySeconds_reexpandsThirtySecondsAfterTheMark() {
        assertEquals(1_000L + 30_000L, collapsibleReexpandAtMillis(collapsedAtMillis = 1_000, expandAfterSeconds = 30))
        // A mark at 0 is a real mark, not "not collapsed".
        assertEquals(30_000L, collapsibleReexpandAtMillis(collapsedAtMillis = 0, expandAfterSeconds = 30))
    }

    @Test
    fun zeroOrNegativeSeconds_neverReexpands() {
        assertNull(collapsibleReexpandAtMillis(collapsedAtMillis = 1_000, expandAfterSeconds = 0))
        assertNull(collapsibleReexpandAtMillis(collapsedAtMillis = 1_000, expandAfterSeconds = -5))
        // Control: the same mark with 1 second does reexpand, so null above comes from the seconds.
        assertEquals(2_000L, collapsibleReexpandAtMillis(collapsedAtMillis = 1_000, expandAfterSeconds = 1))
    }

    @Test
    fun shouldReexpand_beforeDue_atDue_withoutAd() {
        // L-055: three cases side by side, so each false is told apart from the true.
        assertFalse(shouldReexpand(nowMillis = 29_999, reexpandAtMillis = 30_000, hasAd = true))
        assertTrue(shouldReexpand(nowMillis = 30_000, reexpandAtMillis = 30_000, hasAd = true))
        assertFalse(shouldReexpand(nowMillis = 30_000, reexpandAtMillis = 30_000, hasAd = false))
        assertTrue(shouldReexpand(nowMillis = 90_000, reexpandAtMillis = 30_000, hasAd = true))
        assertFalse(shouldReexpand(nowMillis = 90_000, reexpandAtMillis = null, hasAd = true))
    }
}
