package com.vglstudio.camera.pose.line.guide.core.review

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlag
import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

/** The order of the steps of a review request (spec camera, KB-10; plan T-019). The scope is not used by [ask]. */
@RunWith(AndroidJUnit4::class)
class InAppReviewTest {
    private val day = 86_400_000L
    private val prompt = FakeReviewPrompt()
    private val flags = FakeFeatureFlags(setOf(FeatureFlag.IN_APP_REVIEW))
    private val settings = FakeRemoteSettings()
    private val history = FakeReviewHistory()
    private val logger = RecordingAppLogger()
    private var now = 1_000_000L
    private val activity: Activity = Robolectric.buildActivity(Activity::class.java).get()

    private fun review(scope: CoroutineScope = CoroutineScope(kotlinx.coroutines.Job())) = DefaultInAppReview(
        prompt = prompt,
        flags = flags,
        settings = settings,
        history = history,
        clock = { now },
        logger = logger,
        scope = scope,
    )

    private fun hasLog(text: String, level: LogLevel? = null) = logger.entries.any {
        it.tag == "InAppReview" &&
            it.message.contains(text) &&
            (level == null || it.level == level)
    }

    @Test
    fun dueAndFlagOn_asksAndRecordsTime() = runTest {
        assertTrue(review().ask(activity, 3))
        assertEquals(1, prompt.calls)
        assertEquals(1_000_000L, history.lastAskedAt())
        assertTrue(hasLog("Review asked"))
    }

    @Test
    fun secondAskWithin30Days_isSkipped() = runTest {
        val review = review()
        assertTrue(review.ask(activity, 3))
        now += 29 * day
        assertFalse(review.ask(activity, 3))
        assertEquals(1, prompt.calls)
        assertTrue(hasLog("Review skipped: not_due"))
        now += day
        assertTrue(review.ask(activity, 3))
        assertEquals(2, prompt.calls)
    }

    @Test
    fun flagOff_neverAsks() = runTest {
        flags.enabled.value = emptySet()
        val review = review()
        assertFalse(review.ask(activity, 10))
        assertEquals(0, prompt.calls)
        assertNull(history.lastAskedAt())
        assertTrue(hasLog("Review skipped: flag_off"))
        flags.enabled.value = setOf(FeatureFlag.IN_APP_REVIEW)
        assertTrue(review.ask(activity, 10))
        assertEquals(1, prompt.calls)
    }

    @Test
    fun remoteMinSaved_isUsed() = runTest {
        settings.longs.value = mapOf("in_app_review_min_saved_photos" to 5L)
        val review = review()
        assertFalse(review.ask(activity, 3))
        assertEquals(0, prompt.calls)
        assertTrue(review.ask(activity, 5))
        assertEquals(1, prompt.calls)
    }

    @Test
    fun remoteMinDays_isUsed() = runTest {
        settings.longs.value = mapOf("in_app_review_min_days_between" to 7L)
        val review = review()
        assertTrue(review.ask(activity, 3))
        now += 6 * day
        assertFalse(review.ask(activity, 3))
        now += day
        assertTrue(review.ask(activity, 3))
        assertEquals(2, prompt.calls)
    }

    @Test
    fun promptFails_isLoggedAndNotRecorded() = runTest {
        prompt.error = RuntimeException("no play services")
        val review = review()
        assertFalse(review.ask(activity, 3))
        assertNull(history.lastAskedAt())
        assertTrue(hasLog("Review failed", LogLevel.WARN))
        // The failed request does not count as asked: the next one goes through.
        prompt.error = null
        assertTrue(review.ask(activity, 3))
        assertEquals(1_000_000L, history.lastAskedAt())
    }
}
