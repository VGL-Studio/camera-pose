package com.vglstudio.camera.pose.line.guide.core.review

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlag
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** [rememberRequestInAppReview]: nothing without the provider (tests, previews), the request with it. */
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
}
