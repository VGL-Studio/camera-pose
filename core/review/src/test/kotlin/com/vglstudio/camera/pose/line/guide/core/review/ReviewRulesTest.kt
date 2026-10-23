package com.vglstudio.camera.pose.line.guide.core.review

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The rule of KB-10 (spec camera; plan T-019): enough saved photos and enough days since the last request. */
class ReviewRulesTest {
    private val now = 1_800_000_000_000L
    private val day = 86_400_000L

    @Test
    fun threeSaved_isDue_twoIsNot() {
        assertTrue(isReviewDue(3, 3, null, now, 30))
        assertFalse(isReviewDue(2, 3, null, now, 30))
    }

    @Test
    fun thirtyDaysAfterLastAsk_isDue() {
        assertTrue(isReviewDue(3, 3, now - 30 * day, now, 30))
        assertFalse(isReviewDue(3, 3, now - 30 * day + 1, now, 30))
    }

    @Test
    fun neverAsked_isDue() {
        assertTrue(isReviewDue(10, 3, null, now, 30))
    }

    @Test
    fun lastAskInTheFuture_isDue() {
        assertTrue(isReviewDue(3, 3, now + 1_000, now, 30))
    }

    @Test
    fun defaults() {
        assertEquals(3L, ReviewSettings.MIN_SAVED_PHOTOS_DEFAULT)
        assertEquals(30L, ReviewSettings.MIN_DAYS_BETWEEN_DEFAULT)
        assertEquals("in_app_review_min_saved_photos", ReviewSettings.MIN_SAVED_PHOTOS)
        assertEquals("in_app_review_min_days_between", ReviewSettings.MIN_DAYS_BETWEEN)
    }
}
