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

    // Asking for a store review (plan T-019)

    @Test
    fun adShown_doesNotAskReview() {
        afterReviewAd(shown = true, goBack = { calls += "goBack" }, askReview = { calls += "askReview" })
        assertEquals(listOf("goBack"), calls)
    }

    @Test
    fun adNotShown_goesBackThenAsksReview() {
        afterReviewAd(shown = false, goBack = { calls += "goBack" }, askReview = { calls += "askReview" })
        assertEquals(listOf("goBack", "askReview"), calls)
    }

    @Test
    fun notSaved_goesBackWithoutReview() {
        leaveReview(
            savedThisVisit = false,
            showAd = { calls += "showAd" },
            goBack = { calls += "goBack" },
            askReview = { calls += "askReview" },
        )
        assertEquals(listOf("goBack"), calls)

        // Contrast: a saved visit asks for the ad, then goes back and asks for the review.
        calls.clear()
        val afterAd = leaveAndKeepAfterAd(savedThisVisit = true)
        afterAd()
        assertEquals(listOf("showAd", "goBack", "askReview"), calls)
    }

    @Test
    fun saved_asksAdBeforeReview() {
        leaveReview(
            savedThisVisit = true,
            showAd = { calls += "showAd" },
            goBack = { calls += "goBack" },
            askReview = { calls += "askReview" },
        )
        assertEquals(listOf("showAd"), calls)
    }

    /** Leaves like [ReviewRoute]: the ad callback runs [afterReviewAd] with "not shown". */
    private fun leaveAndKeepAfterAd(savedThisVisit: Boolean): () -> Unit {
        val goBack = { calls += "goBack" }
        val askReview = { calls += "askReview" }
        var afterAd: () -> Unit = {}
        leaveReview(savedThisVisit, showAd = {
            calls += "showAd"
            afterAd = { afterReviewAd(shown = false, goBack = goBack, askReview = askReview) }
        }, goBack = goBack, askReview = askReview)
        return afterAd
    }
}
