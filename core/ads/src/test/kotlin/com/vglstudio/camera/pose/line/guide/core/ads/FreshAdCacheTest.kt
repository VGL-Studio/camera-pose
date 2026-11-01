package com.vglstudio.camera.pose.line.guide.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Preloaded native full-screen ads are shown only within an hour of loading (plan T-013, question 4). */
class FreshAdCacheTest {
    private val discarded = mutableListOf<String>()
    private val cache = FreshAdCache<String>(onDiscard = { discarded += it })
    private val key = AdPlacement.POSES_BOTTOM

    @Test
    fun emptyCache_hasNothingFresh() {
        assertFalse(cache.hasFresh(key, nowMillis = 0))
        assertNull(cache.take(key, nowMillis = 0))
        assertFalse(cache.contains(key))

        // Control: once an ad is put, the same reads see it.
        cache.put(key, "ad", nowMillis = 0)
        assertTrue(cache.contains(key))
        assertTrue(cache.hasFresh(key, nowMillis = 0))
        assertEquals("ad", cache.take(key, nowMillis = 0))
    }

    @Test
    fun adWithinOneHour_isTaken() {
        cache.put(key, "ad", nowMillis = 0)
        assertTrue(cache.hasFresh(key, nowMillis = 3_599_999))
        assertEquals("ad", cache.take(key, nowMillis = 3_599_999))
        assertNull(cache.take(key, nowMillis = 3_599_999))
        assertTrue(discarded.isEmpty())
    }

    @Test
    fun adOlderThanOneHour_isDiscarded() {
        cache.put(key, "ad", nowMillis = 0)
        assertFalse(cache.hasFresh(key, nowMillis = 3_600_000))
        assertNull(cache.take(key, nowMillis = 3_600_000))
        assertEquals(listOf("ad"), discarded)
        assertFalse(cache.contains(key))
    }

    @Test
    fun nativeFullScreenCache_keepsAt54m59s_dropsAt55m() {
        val gone = mutableListOf<String>()
        val native = nativeFullScreenCache<String>(onDiscard = { gone += it })
        native.put(key, "kept", nowMillis = 0)
        assertTrue(native.hasFresh(key, nowMillis = 55 * 60_000L - 1))
        assertEquals("kept", native.take(key, nowMillis = 55 * 60_000L - 1))

        native.put(key, "old", nowMillis = 0)
        assertFalse(native.hasFresh(key, nowMillis = 55 * 60_000L))
        assertNull(native.take(key, nowMillis = 55 * 60_000L))
        assertEquals(listOf("old"), gone)
    }

    @Test
    fun defaultCache_stillKeepsAt59m() {
        // Control of nativeFullScreenCache_keepsAt54m59s_dropsAt55m: the constructor default keeps an hour.
        cache.put(key, "ad", nowMillis = 0)
        assertTrue(cache.hasFresh(key, nowMillis = 59 * 60_000L))
        assertEquals("ad", cache.take(key, nowMillis = 59 * 60_000L))
        assertTrue(discarded.isEmpty())
    }

    @Test
    fun put_replacesAndDiscardsOlderAd() {
        cache.put(key, "first", nowMillis = 0)
        cache.put(key, "second", nowMillis = 10)
        assertEquals(listOf("first"), discarded)
        assertEquals("second", cache.take(key, nowMillis = 20))
    }
}
