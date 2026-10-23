package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The form of [rememberShowInterstitial] that reports the result (plan T-018). Without a controller in tests. */
@RunWith(AndroidJUnit4::class)
class ShowInterstitialTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun withoutAdsController_reportsNotShownOnce() {
        val results = mutableListOf<Boolean>()
        var action: () -> Unit = {}
        composeRule.setContent {
            action = rememberShowInterstitial(AdPlacement.REVIEW_INTERSTITIAL) { results += it }
        }
        composeRule.waitForIdle()
        assertTrue(results.isEmpty())
        composeRule.runOnUiThread { action() }
        composeRule.waitForIdle()
        assertEquals(listOf(false), results)
    }

    @Test
    fun bannerPlacement_isRejected() {
        val error = runCatching {
            composeRule.setContent { rememberShowInterstitial(AdPlacement.SAMPLE_BOTTOM_BANNER) {} }
            composeRule.waitForIdle()
        }.exceptionOrNull()
        assertTrue(
            "expected IllegalArgumentException, got $error",
            generateSequence(error) {
                it.cause
            }.any { it is IllegalArgumentException },
        )
    }
}
