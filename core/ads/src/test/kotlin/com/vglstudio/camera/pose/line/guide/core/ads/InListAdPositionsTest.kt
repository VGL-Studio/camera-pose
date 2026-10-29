package com.vglstudio.camera.pose.line.guide.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Where in-list native ads go: after every N items, never first, never last (plan T-011, section 4). */
class InListAdPositionsTest {
    @Test
    fun designExample_sevenItemsEveryThree_adsAfterItems3And6() {
        // Combined list: item 1, 2, 3, ad, item 4, 5, 6, ad, item 7.
        assertEquals(listOf(3, 7), inListAdPositions(itemCount = 7, every = 3))
    }

    @Test
    fun defaultEvery_twentyItems_adsAfterItems8And16() {
        assertEquals(listOf(8, 17), inListAdPositions(itemCount = 20, every = 8))
    }

    @Test
    fun neverFirst_neverLast_neverAdjacent() {
        for (itemCount in 0..60) {
            for (every in 1..10) {
                val positions = inListAdPositions(itemCount, every)
                val case = "itemCount=$itemCount every=$every positions=$positions"
                // Control: the expected number of ads, so an empty result cannot pass by accident.
                val expectedCount = if (itemCount == 0) 0 else (itemCount - 1) / every
                assertEquals(case, expectedCount, positions.size)
                assertFalse(case, 0 in positions)
                assertFalse(case, itemCount + positions.size - 1 in positions)
                positions.zipWithNext().forEach { (first, second) -> assertEquals(case, every + 1, second - first) }
            }
        }
    }

    @Test
    fun fewerItemsThanEvery_noAds() {
        assertEquals(emptyList<Int>(), inListAdPositions(itemCount = 7, every = 8))
        // Control: one more item than N gives an ad, so the empty result above comes from the item count.
        assertEquals(listOf(8), inListAdPositions(itemCount = 9, every = 8))
    }

    @Test
    fun zeroItems_noAds() {
        assertEquals(emptyList<Int>(), inListAdPositions(itemCount = 0, every = 3))
        // Control: the same N with items gives an ad.
        assertEquals(listOf(3), inListAdPositions(itemCount = 4, every = 3))
    }

    @Test
    fun exactlyEveryItems_noTrailingAd() {
        assertEquals(emptyList<Int>(), inListAdPositions(itemCount = 8, every = 8))
        assertEquals(listOf(8), inListAdPositions(itemCount = 9, every = 8))
    }

    @Test
    fun nonPositiveEvery_noAds() {
        assertEquals(emptyList<Int>(), inListAdPositions(itemCount = 20, every = 0))
        assertEquals(emptyList<Int>(), inListAdPositions(itemCount = 20, every = -1))
        // Control: a positive N with the same items gives ads.
        assertTrue(inListAdPositions(itemCount = 20, every = 1).isNotEmpty())
    }

    @Test
    fun everyOne_adBetweenEveryTwoItems() {
        assertEquals(listOf(1, 3), inListAdPositions(itemCount = 3, every = 1))
    }

    @Test
    fun gridAdEvery_roundsUpToWholeRows() {
        assertEquals(4, gridAdEvery(every = 3, columns = 2))
        assertEquals(8, gridAdEvery(every = 8, columns = 2))
        assertEquals(9, gridAdEvery(every = 8, columns = 3))
        assertEquals(5, gridAdEvery(every = 5, columns = 1))
        assertEquals(5, gridAdEvery(every = 5, columns = 0))
    }

    @Test
    fun inListEvery_nullWhenAdsNotAllowedOrBadValue() {
        assertNull(inListEvery(adsAllowed = false, everyCount = 8))
        assertNull(inListEvery(adsAllowed = true, everyCount = 0))
        assertNull(inListEvery(adsAllowed = true, everyCount = -3))
        assertNull(inListEvery(adsAllowed = true, everyCount = Int.MAX_VALUE + 1L))
        assertEquals(8, inListEvery(adsAllowed = true, everyCount = 8))
    }
}
