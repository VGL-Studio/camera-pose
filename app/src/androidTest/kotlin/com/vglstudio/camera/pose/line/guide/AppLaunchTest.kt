package com.vglstudio.camera.pose.line.guide

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke test on a real emulator: the app starts, goes through loading and reaches its start screen. Runs in CI only.
 * Checks only the stable end state: the loading screen is brief and covered by unit and JVM UI tests. A fresh
 * staging install shows the ad consent form over the app, so the start screen is looked up in the semantics tree
 * rather than asserted as displayed (CLAUDE.md, step 10).
 * Loading downloads the sample pose catalog and its starter set, so the test needs those files on R2 staging
 * (plan T-010, section 3.3). The slowest load that still succeeds takes about 47 seconds, hence the 60 second wait.
 */
@RunWith(AndroidJUnit4::class)
class AppLaunchTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appStarts_andReachesStartScreen() {
        composeRule.waitUntil(timeoutMillis = START_SCREEN_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithTag("sample_screen").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        const val START_SCREEN_TIMEOUT_MILLIS = 60_000L
    }
}
