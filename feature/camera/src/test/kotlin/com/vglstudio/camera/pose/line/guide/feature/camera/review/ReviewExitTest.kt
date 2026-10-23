package com.vglstudio.camera.pose.line.guide.feature.camera.review

import org.junit.Assert.assertEquals
import org.junit.Test

/** The order in which the review screen is left (spec camera, KB-9; plan T-018). */
class ReviewExitTest {
    private val calls = mutableListOf<String>()

    @Test
    fun saved_asksAdFirst_thenGoesBackAfterAd() {
        leaveReview(savedThisVisit = true, showAd = { calls += "ad" }, goBack = { calls += "back" })
        assertEquals(listOf("ad"), calls)
        afterReviewAd(shown = true, goBack = { calls += "back" })
        assertEquals(listOf("ad", "back"), calls)
    }

    @Test
    fun adNotShown_stillGoesBack() {
        afterReviewAd(shown = false, goBack = { calls += "back" })
        assertEquals(listOf("back"), calls)
    }

    @Test
    fun notSaved_goesBackWithoutAd() {
        leaveReview(savedThisVisit = false, showAd = { calls += "ad" }, goBack = { calls += "back" })
        assertEquals(listOf("back"), calls)
    }
}
