package com.vglstudio.camera.pose.line.guide.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The slot of app open ads (A-07 to A-09, plan T-024). */
class AppOpenRulesTest {
    private val discarded = mutableListOf<String>()
    private val cache = FreshAdCache<String>(APP_OPEN_MAX_AGE_MILLIS, onDiscard = { discarded += it })
    private val key = AdPlacement.OPEN_APP

    @Test
    fun appOpen_expiresAtOneHour() {
        cache.put(key, "a", nowMillis = 0)
        assertTrue(cache.hasFresh(key, nowMillis = 3_599_999))
        assertFalse(cache.hasFresh(key, nowMillis = 3_600_000))
        assertNull(cache.take(key, nowMillis = 3_600_000))
        assertEquals(listOf("a"), discarded)
    }

    @Test
    fun lockOfOneSecondAfterClose() {
        assertTrue(appOpenLockPassed(lastClosedAtMillis = null, nowMillis = 0))
        assertFalse(appOpenLockPassed(lastClosedAtMillis = 10_000, nowMillis = 10_999))
        assertTrue(appOpenLockPassed(lastClosedAtMillis = 10_000, nowMillis = 11_000))
        // The clock reads before the mark (AR-01): the lock counts as passed.
        assertTrue(appOpenLockPassed(lastClosedAtMillis = 10_000, nowMillis = 9_000))
    }

    @Test
    fun adLoadedWhileShowing_isKept() {
        cache.put(key, "a", nowMillis = 0)
        // The show takes the ad out of the slot (A-09).
        assertEquals("a", cache.take(key, nowMillis = 0))
        // A new ad loads while the first one is on screen; the close removes nothing.
        cache.put(key, "b", nowMillis = 5_000)
        assertTrue(cache.hasFresh(key, nowMillis = 6_000))
        assertEquals("b", cache.take(key, nowMillis = 6_000))
        assertTrue(discarded.isEmpty())
    }
}
