package com.vglstudio.camera.pose.line.guide.navigation

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.feature.loading.navigation.LoadingDestination
import com.vglstudio.camera.pose.line.guide.feature.settings.navigation.SettingsDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * ReportPastLoading of AppNavHost when the activity is restored from saved state (plan T-078): the back stack of
 * rememberNavController comes back without composing loading again, and only the restored screen is reported, never a
 * "not past loading" first frame that would hide the network dialog. The host follows AppNavHost (loading popped
 * inclusive); AppNavHost itself needs Hilt for the loading screen, which :app has no test setup for.
 */
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class NavRestoreTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val events = mutableListOf<String>()

    private fun StateRestorationTester.setHost(loadingFinishes: Boolean) = setContent {
        val navController = rememberNavController()
        ReportPastLoading(navController) { events += "pastLoading=$it" }
        NavHost(navController, startDestination = LoadingDestination) {
            composable<LoadingDestination> {
                Text("loading")
                LaunchedEffect(Unit) {
                    events += "loading"
                    if (loadingFinishes) {
                        navController.navigate(SettingsDestination) {
                            popUpTo<LoadingDestination> { inclusive = true }
                        }
                    }
                }
            }
            composable<SettingsDestination> { Text("main") }
        }
    }

    @Test
    fun restoredPastLoading_skipsLoading_reportsOnlyPastLoading() {
        val restoration = StateRestorationTester(composeRule)
        restoration.setHost(loadingFinishes = true)
        composeRule.waitForIdle()
        // Control, the same with or without the fix: the first launch goes through loading, then past it.
        assertTrue("loading" in events)
        assertEquals("pastLoading=true", events.last())

        events.clear()
        restoration.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()

        assertEquals(listOf("pastLoading=true"), events)
    }

    @Test
    fun restoredOnLoading_reportsNotPastLoading() {
        val restoration = StateRestorationTester(composeRule)
        restoration.setHost(loadingFinishes = false)
        composeRule.waitForIdle()
        events.clear()

        restoration.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()

        assertEquals(listOf("loading", "pastLoading=false"), events)
    }
}
