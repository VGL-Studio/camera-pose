package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The three ways features block the ads that show by themselves (G-03), inside the app's root [AdsHostContent]. */
@RunWith(AndroidJUnit4::class)
class AdSuppressionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val suppressor = ForegroundAdSuppressor()
    private val suppression = ResumeSuppression(suppressor, RecordingAppLogger())

    @Test
    fun holdThenRelease_andReleaseOnDispose() {
        var shown by mutableStateOf(true)
        var handle: AdSuppression? = null
        composeRule.setContent {
            AdsHostContent(suppression, suppressor) {
                if (shown) handle = rememberAdSuppression()
            }
        }
        composeRule.waitForIdle()
        val adSuppression = requireNotNull(handle)
        composeRule.runOnUiThread { adSuppression.hold() }
        assertTrue(suppressor.suppressed.value)
        composeRule.runOnUiThread {
            adSuppression.release()
            adSuppression.release()
        }
        assertFalse(suppressor.suppressed.value)
        // Never below zero: another source's hold still counts.
        val other = composeRule.runOnUiThread { suppressor.acquire("other") }
        assertTrue(suppressor.suppressed.value)
        composeRule.runOnUiThread { other.release() }

        // Safety net: leaving the composition releases a hold that is still on.
        composeRule.runOnUiThread { adSuppression.hold() }
        assertTrue(suppressor.suppressed.value)
        shown = false
        composeRule.waitForIdle()
        assertFalse(suppressor.suppressed.value)
    }

    @Test
    fun suppressResumeAds_followsActive() {
        var active by mutableStateOf(true)
        composeRule.setContent {
            AdsHostContent(suppression, suppressor) { SuppressResumeAds(active) }
        }
        composeRule.waitForIdle()
        assertTrue(suppressor.suppressed.value)
        active = false
        composeRule.waitForIdle()
        assertFalse(suppressor.suppressed.value)
    }

    @Test
    fun markLeavingApp_blocksUntilNextResume() {
        var markLeaving: () -> Unit = {}
        composeRule.setContent {
            AdsHostContent(suppression, suppressor) { markLeaving = rememberMarkLeavingApp() }
        }
        composeRule.waitForIdle()
        assertFalse(suppression.resumeAdsSuppressed.value)
        composeRule.runOnUiThread { markLeaving() }
        composeRule.waitForIdle()
        assertTrue(suppression.resumeAdsSuppressed.value)
        composeRule.runOnUiThread { suppression.onActivityResumed() }
        assertFalse(suppression.resumeAdsSuppressed.value)
    }
}
