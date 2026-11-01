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
 * Smoke test on a real emulator: the app starts, goes through loading, passes onboarding ("Next" up to the last page,
 * then "Turn on camera", plan T-082) and reaches the camera (flags on in staging, T-012, T-013). Runs in CI only. No
 * camera state is checked: the CI emulator may have no camera. A fresh install always shows onboarding; the test also accepts landing on the camera
 * directly, so it does not depend on data left on the emulator.
 * The camera permission is granted before launch: the system permission prompt is another activity, and while it
 * covers the app the test finds no Compose hierarchy at all. With the permission granted, "Turn on camera" goes to the
 * camera without a prompt.
 * Checks only the stable end state: the loading screen is brief and covered by unit and JVM UI tests. A fresh
 * staging install shows the ad consent form over the app, so the start screen is looked up in the semantics tree
 * rather than asserted as displayed (CLAUDE.md, step 10).
 * Loading downloads the sample pose catalog and its starter set, so the test needs those files on R2 staging
 * (plan T-010, section 3.3). The slowest load that still succeeds takes about 47 seconds, hence the 60 second wait.
 */
@RunWith(AndroidJUnit4::class)
class AppLaunchTest {
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
    fun appStarts_passesOnboarding_andReachesCamera() {
        composeRule.waitUntil(timeoutMillis = START_SCREEN_TIMEOUT_MILLIS) {
            exists(ONBOARDING_SCREEN) || exists(CAMERA_SCREEN)
        }
        if (exists(ONBOARDING_SCREEN)) {
            passOnboarding()
            composeRule.waitUntil(timeoutMillis = CAMERA_TIMEOUT_MILLIS) { exists(CAMERA_SCREEN) }
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
        const val START_SCREEN_TIMEOUT_MILLIS = 60_000L
        const val CAMERA_TIMEOUT_MILLIS = 10_000L
        const val ONBOARDING_SCREEN = "onboarding_screen"
        const val NEXT = "onboarding_next"
        const val ENABLE_CAMERA = "onboarding_enable_camera"
        const val ONBOARDING_ROUNDS = 6
        const val ONBOARDING_STEP_TIMEOUT_MILLIS = 10_000L
        const val CAMERA_SCREEN = "camera_screen"
    }
}
