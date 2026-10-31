package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The form of [rememberShowInterstitial] that reports the result (plan T-018), with the break limits of :app in front
 * of the gate of :core:ads (plan T-074, section 3.1, spec ads question 1).
 */
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
            composeRule.setContent { rememberShowInterstitial(AdPlacement.POSES_BOTTOM) {} }
            composeRule.waitForIdle()
        }.exceptionOrNull()
        assertTrue(
            "expected IllegalArgumentException, got $error",
            generateSequence(error) {
                it.cause
            }.any { it is IllegalArgumentException },
        )
    }

    @Test
    fun gateSaysNo_reportsNotShownOnce_withoutCallingController() {
        val gate = RecordingBreakGate(allow = false)
        val withoutController = mutableListOf<Boolean>()
        val withController = mutableListOf<Boolean>()
        var shows = 0
        var actionWithout: () -> Unit = {}
        var actionWith: () -> Unit = {}
        val controller = testAdsController()
        composeRule.setContent {
            CompositionLocalProvider(LocalInterstitialBreakGate provides gate) {
                actionWithout = rememberShowInterstitial(AdPlacement.REVIEW_INTERSTITIAL) { withoutController += it }
                CompositionLocalProvider(LocalAdsController provides controller) {
                    actionWith = rememberBreakInterstitial(
                        placement = AdPlacement.REVIEW_INTERSTITIAL,
                        onFinished = { withController += it },
                        show = { _, _, _, _ ->
                            shows++
                            true
                        },
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.runOnUiThread {
            actionWithout()
            actionWith()
        }
        composeRule.waitForIdle()
        assertEquals(listOf(false), withoutController)
        assertEquals(listOf(false), withController)
        assertEquals(0, shows)
        // The break was counted once, by the action that had a controller.
        assertEquals(listOf(AdPlacement.REVIEW_INTERSTITIAL), gate.breaks)

        // Control: the same action with a gate that allows calls the controller and reports what it says.
        gate.allow = true
        composeRule.runOnUiThread { actionWith() }
        composeRule.waitForIdle()
        assertEquals(1, shows)
    }
}
