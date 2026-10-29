package com.vglstudio.camera.pose.line.guide.core.review

import android.app.Activity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlag
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [rememberRequestInAppReview]: nothing without the provider (tests, previews), the request with it; the ads that show
 * by themselves are held from the request until the review flow is done (plan T-074, pass 2), through the hold given
 * to the internal overload, as :core:ads gives it in the app.
 */
@RunWith(AndroidJUnit4::class)
class RequestInAppReviewTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun withoutProvider_doesNothing() {
        var action: (Int) -> Unit = {}
        composeRule.setContent { action = rememberRequestInAppReview() }
        composeRule.waitForIdle()
        composeRule.runOnUiThread { action(3) }
        composeRule.waitForIdle()
    }

    @Test
    fun withProvider_asksThroughInAppReview() {
        val prompt = FakeReviewPrompt()
        val review = DefaultInAppReview(
            prompt = prompt,
            flags = FakeFeatureFlags(setOf(FeatureFlag.IN_APP_REVIEW)),
            settings = FakeRemoteSettings(),
            history = FakeReviewHistory(),
            clock = { 1_000_000L },
            logger = RecordingAppLogger(),
            scope = CoroutineScope(Job() + Dispatchers.Main.immediate),
        )
        var action: (Int) -> Unit = {}
        composeRule.setContent {
            CompositionLocalProvider(LocalInAppReview provides review) { action = rememberRequestInAppReview() }
        }
        composeRule.waitForIdle()
        composeRule.runOnUiThread { action(3) }
        composeRule.waitForIdle()
        assertEquals(1, prompt.calls)
    }

    /** Holds started and released by the request, as [rememberStartAdHold] of :core:ads does in the app. */
    private class RecordingHold {
        var started = 0
        var released = 0
        val active get() = started - released

        val start: () -> (() -> Unit) = {
            started++
            var done = false
            val release: () -> Unit = {
                if (!done) {
                    done = true
                    released++
                }
            }
            release
        }
    }

    /** Keeps the onDone of each request, so the test decides when the review flow is done. */
    private class PendingInAppReview : InAppReview {
        val pending = mutableListOf<() -> Unit>()

        override fun requestIfDue(activity: Activity, savedPhotos: Int, onDone: () -> Unit) {
            pending += onDone
        }
    }

    @Test
    fun request_holdsAdsUntilDone() {
        val hold = RecordingHold()
        val review = PendingInAppReview()
        var action: (Int) -> Unit = {}
        composeRule.setContent {
            CompositionLocalProvider(LocalInAppReview provides review) {
                action = rememberRequestInAppReview(startHold = hold.start)
            }
        }
        composeRule.waitForIdle()
        // Control: composing alone holds nothing.
        assertEquals(0, hold.started)

        composeRule.runOnUiThread { action(3) }
        assertEquals(1, review.pending.size)
        assertEquals("held while the review flow runs", 1, hold.active)

        composeRule.runOnUiThread { review.pending.single()() }
        assertEquals(0, hold.active)
        assertEquals(1, hold.started)
    }

    @Test
    fun request_releasesWhenTheReviewFails() {
        val hold = RecordingHold()
        val prompt = FakeReviewPrompt().apply { error = IllegalStateException("no Play") }
        val logger = RecordingAppLogger()
        val review = DefaultInAppReview(
            prompt = prompt,
            flags = FakeFeatureFlags(setOf(FeatureFlag.IN_APP_REVIEW)),
            settings = FakeRemoteSettings(),
            history = FakeReviewHistory(),
            clock = { 1_000_000L },
            logger = logger,
            scope = CoroutineScope(Job() + Dispatchers.Main.immediate),
        )
        var action: (Int) -> Unit = {}
        composeRule.setContent {
            CompositionLocalProvider(LocalInAppReview provides review) {
                action = rememberRequestInAppReview(startHold = hold.start)
            }
        }
        composeRule.waitForIdle()

        composeRule.runOnUiThread { action(3) }
        composeRule.waitForIdle()

        // Control: the review was really tried, and failed.
        assertTrue(logger.entries.any { it.message == "Review failed" })
        assertEquals(1, hold.started)
        assertEquals(0, hold.active)
    }

    @Test
    fun withoutProvider_holdsNothing() {
        val hold = RecordingHold()
        var action: (Int) -> Unit = {}
        composeRule.setContent { action = rememberRequestInAppReview(startHold = hold.start) }
        composeRule.waitForIdle()
        composeRule.runOnUiThread { action(3) }
        composeRule.waitForIdle()
        assertEquals(0, hold.started)
    }
}
