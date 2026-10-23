package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The consent entry point drawn by a feature (spec settings, KB-5; plan T-025, section 4). Without an ads controller
 * (tests, previews) the row is not drawn at all. Cases with a controller need the real consent SDK: checked by hand.
 */
@RunWith(AndroidJUnit4::class)
class PrivacyOptionsButtonTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun withoutAdsController_rendersNothing() {
        var contentCalls = 0
        composeRule.setContent {
            PrivacyOptionsButton { _ ->
                contentCalls++
                Text("Privacy row")
            }
        }
        composeRule.waitForIdle()

        assertEquals(0, contentCalls)
        assertTrue(composeRule.onAllNodesWithText("Privacy row").fetchSemanticsNodes().isEmpty())
    }
}
