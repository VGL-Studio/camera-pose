package com.vglstudio.camera.pose.line.guide.feature.camera

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The rules of the full-screen ad cover and the report of RESUMED (spec camera, section 5; plan T-097, section 4). */
@RunWith(AndroidJUnit4::class)
class FullScreenAdCoverTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun adCoversCamera_truthTable() {
        val bools = listOf(false, true)
        for (adShowing in bools) {
            for (resumed in bools) {
                for (pausedSinceAdShown in bools) {
                    // Covered only while the ad shows, unless the screen paused and resumed since it began.
                    val expected = adShowing && !(resumed && pausedSinceAdShown)
                    assertEquals(
                        "adShowing=$adShowing resumed=$resumed pausedSinceAdShown=$pausedSinceAdShown",
                        expected,
                        adCoversCamera(adShowing, resumed, pausedSinceAdShown),
                    )
                }
            }
        }
    }

    @Test
    fun cameraMayOpen_truthTable() {
        assertEquals(false, cameraMayOpen(started = false, covered = false))
        assertEquals(false, cameraMayOpen(started = false, covered = true))
        assertEquals(true, cameraMayOpen(started = true, covered = false))
        assertEquals(false, cameraMayOpen(started = true, covered = true))
    }

    @Test
    fun resumeEffect_reportsPauseAndResume() {
        val records = mutableListOf<String>()
        composeRule.setContent {
            CameraResumeEffect(onResumed = { records += "resumed" }, onPaused = { records += "paused" })
        }
        composeRule.waitForIdle()
        val scenario = composeRule.activityRule.scenario
        scenario.moveToState(Lifecycle.State.STARTED)
        composeRule.waitForIdle()
        // Out to the background and back.
        scenario.moveToState(Lifecycle.State.CREATED)
        composeRule.waitForIdle()
        scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()
        assertEquals(listOf("resumed", "paused", "resumed"), records)
    }
}
