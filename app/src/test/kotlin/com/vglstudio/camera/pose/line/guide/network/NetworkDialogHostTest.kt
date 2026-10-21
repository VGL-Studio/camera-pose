package com.vglstudio.camera.pose.line.guide.network

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.NetworkDialogTestTags
import com.vglstudio.camera.pose.line.guide.core.network.NetworkDialogState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The shell that draws the network dialog over every screen (spec network, KB-1 and KB-3). A plain Application, so
 * neither Hilt nor Firebase starts.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class)
class NetworkDialogHostTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun setContentButton(state: NetworkDialogState, onClick: () -> Unit) {
        composeRule.setContent {
            NetworkDialogHost(state = state, onRetry = {}, onBack = {}) {
                Button(onClick = onClick, modifier = Modifier.testTag(CONTENT)) { Text("Content") }
            }
        }
    }

    @Test
    fun shown_blocksContentClicks_andHidesItFromAccessibility() {
        var clicks = 0
        setContentButton(SHOWN) { clicks++ }

        composeRule.onNodeWithTag(CONTENT).performClick()

        assertEquals(0, clicks)
        composeRule.onNodeWithTag(NetworkDialogTestTags.SCREEN).assertExists()
        composeRule.onNodeWithTag(CONTENT, useUnmergedTree = true)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.HideFromAccessibility))
        // Still in the semantics tree, so AppLaunchTest finds the screen behind the dialog.
        assertEquals(1, composeRule.onAllNodesWithTag(CONTENT).fetchSemanticsNodes().size)
    }

    @Test
    fun hidden_contentWorksNormally() {
        var clicks = 0
        setContentButton(NetworkDialogState.Hidden) { clicks++ }

        composeRule.onNodeWithTag(CONTENT).performClick()

        assertEquals(1, clicks)
        composeRule.onAllNodesWithTag(NetworkDialogTestTags.SCREEN).assertCountEquals(0)
    }

    @Test
    fun back_whileShown_callsOnBack_andKeepsDestination() {
        var backs = 0
        setNavContent(SHOWN) { backs++ }

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        assertEquals(1, backs)
        composeRule.onNodeWithTag(SECOND).assertExists()
        composeRule.onNodeWithTag(NetworkDialogTestTags.SCREEN).assertExists()
    }

    @Test
    fun back_whileHidden_navigatesBack() {
        var backs = 0
        setNavContent(NetworkDialogState.Hidden) { backs++ }

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        assertEquals(0, backs)
        composeRule.onNodeWithTag(FIRST).assertExists()
        assertTrue(composeRule.onAllNodesWithTag(SECOND).fetchSemanticsNodes().isEmpty())
    }

    /** A two-destination NavHost already on its second destination. */
    private fun setNavContent(state: NetworkDialogState, onBack: () -> Unit) {
        lateinit var navController: NavHostController
        composeRule.setContent {
            navController = rememberNavController()
            NetworkDialogHost(state = state, onRetry = {}, onBack = onBack) {
                NavHost(navController = navController, startDestination = FIRST) {
                    composable(FIRST) { Text("First", modifier = Modifier.testTag(FIRST)) }
                    composable(SECOND) { Text("Second", modifier = Modifier.testTag(SECOND)) }
                }
            }
        }
        composeRule.runOnIdle { navController.navigate(SECOND) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(SECOND).assertExists()
    }

    private companion object {
        val SHOWN = NetworkDialogState.Shown(checking = false, stillOffline = false)
        const val CONTENT = "content"
        const val FIRST = "first"
        const val SECOND = "second"
    }
}
