package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * G-04: the root of the app's UI reads the hold in composition and reports it, so the block of resume ads is lowered
 * one recomposition after the last hold is released (G-02). Without the root, it is never lowered.
 */
@RunWith(AndroidJUnit4::class)
class AdsHostTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val suppressor = ForegroundAdSuppressor()
    private val suppression = ResumeSuppression(suppressor, RecordingAppLogger())

    @Test
    fun shellReportsSuppressionFromComposition() {
        composeRule.setContent { AdsHostContent(suppression, suppressor) { Box {} } }
        composeRule.waitForIdle()
        holdThenRelease()
        assertFalse(suppression.resumeAdsSuppressed.value)
    }

    @Test
    fun shellReportsSuppressionFromComposition_releasedInSameFrame() {
        composeRule.setContent { AdsHostContent(suppression, suppressor) { Box {} } }
        composeRule.waitForIdle()
        composeRule.runOnUiThread { suppressor.acquire("system_screen").release() }
        assertTrue(suppression.resumeAdsSuppressed.value)
        composeRule.waitForIdle()
        assertFalse(suppression.resumeAdsSuppressed.value)
    }

    @Test
    fun withoutShell_suppressionStays() {
        // Control: the same content without AdsHostContent; only the shell lowers the block.
        composeRule.setContent { Box {} }
        composeRule.waitForIdle()
        holdThenRelease()
        assertTrue(suppression.resumeAdsSuppressed.value)
    }

    private fun holdThenRelease() {
        val hold = composeRule.runOnUiThread { suppressor.acquire("system_screen") }
        composeRule.waitForIdle()
        assertTrue(suppression.resumeAdsSuppressed.value)
        composeRule.runOnUiThread { hold.release() }
        composeRule.waitForIdle()
    }
}
