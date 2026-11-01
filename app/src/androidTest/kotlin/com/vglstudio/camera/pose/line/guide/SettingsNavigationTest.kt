package com.vglstudio.camera.pose.line.guide

import android.Manifest
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

/**
 * On a real emulator: the gear of the camera opens settings and Back returns to the camera (spec settings, KB-1;
 * plan T-025, section 4). Flags are on in staging. Runs in CI only, and checks only stable screens after a bounded
 * wait. As AppLaunchTest, the camera permission is granted before launch and screens are looked up in the semantics
 * tree, because a fresh staging install shows the ad consent form over the app (CLAUDE.md, step 10).
 */
@RunWith(AndroidJUnit4::class)
class SettingsNavigationTest {
    @get:Rule(order = 0)
    val cameraPermission = TestRule { base, _ ->
        object : Statement() {
            override fun evaluate() {
                val instrumentation = InstrumentationRegistry.getInstrumentation()
                instrumentation.uiAutomation.grantRuntimePermission(
                    instrumentation.targetContext.packageName,
                    Manifest.permission.CAMERA,
                )
                base.evaluate()
            }
        }
    }

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun cameraGear_opensSettings_backReturns() {
        composeRule.waitUntil(timeoutMillis = START_SCREEN_TIMEOUT_MILLIS) {
            exists(ONBOARDING_SCREEN) || exists(CAMERA_SCREEN)
        }
        if (exists(ONBOARDING_SCREEN)) {
            passOnboarding()
        }
        composeRule.waitUntil(timeoutMillis = SCREEN_TIMEOUT_MILLIS) { exists(CAMERA_SETTINGS) }

        composeRule.onNodeWithTag(CAMERA_SETTINGS).performClick()
        composeRule.waitUntil(timeoutMillis = SCREEN_TIMEOUT_MILLIS) { exists(SETTINGS_SCREEN) }

        // System Back (Espresso is not on the test classpath): the activity's back dispatcher, as the Back key.
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitUntil(timeoutMillis = SCREEN_TIMEOUT_MILLIS) {
            exists(CAMERA_SCREEN) && !exists(SETTINGS_SCREEN)
        }
    }

    private fun exists(tag: String): Boolean = composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    /**
     * Taps "Next" up to the last page of the onboarding flow, then "Turn on camera" (plan T-082). At most
     * [ONBOARDING_ROUNDS] rounds, enough for four pages and one spare wait; each round waits a bounded time for an
     * enabled "Next" (T-083 dims it while the ad of page 1 loads) or for "Turn on camera".
     */
    private fun passOnboarding() {
        repeat(ONBOARDING_ROUNDS) {
            composeRule.waitUntil(timeoutMillis = ONBOARDING_STEP_TIMEOUT_MILLIS) {
                existsEnabled(NEXT) || exists(ENABLE_CAMERA)
            }
            if (exists(ENABLE_CAMERA)) {
                composeRule.onNodeWithTag(ENABLE_CAMERA).performClick()
                return
            }
            composeRule.onNodeWithTag(NEXT).performClick()
        }
    }

    private fun existsEnabled(tag: String): Boolean =
        composeRule.onAllNodes(hasTestTag(tag) and isEnabled()).fetchSemanticsNodes().isNotEmpty()

    private companion object {
        // As AppLaunchTest: loading downloads the starter set of poses from R2 staging.
        const val START_SCREEN_TIMEOUT_MILLIS = 60_000L
        const val SCREEN_TIMEOUT_MILLIS = 10_000L
        const val ONBOARDING_SCREEN = "onboarding_screen"
        const val NEXT = "onboarding_next"
        const val ENABLE_CAMERA = "onboarding_enable_camera"
        const val ONBOARDING_ROUNDS = 6
        const val ONBOARDING_STEP_TIMEOUT_MILLIS = 10_000L
        const val CAMERA_SCREEN = "camera_screen"
        const val CAMERA_SETTINGS = "camera_settings"
        const val SETTINGS_SCREEN = "settings_screen"
    }
}
