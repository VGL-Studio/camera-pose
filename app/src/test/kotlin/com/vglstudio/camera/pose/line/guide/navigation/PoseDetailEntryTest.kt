package com.vglstudio.camera.pose.line.guide.navigation

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.feature.camera.navigation.CameraDestination
import com.vglstudio.camera.pose.line.guide.feature.poses.navigation.PoseDetailDestination
import com.vglstudio.camera.pose.line.guide.feature.poses.navigation.PosesDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * "Chụp với tư thế này" replaces the camera entry with a camera that has the pose id (spec poses, KB-4; plan T-022,
 * section 3.1). A real NavHost with plain screens for the three destinations; a plain Application, so neither Hilt nor
 * Firebase starts.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class)
class PoseDetailEntryTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var navController: NavHostController? = null

    private fun setNavHost() {
        composeRule.setContent {
            val controller = rememberNavController().also { navController = it }
            NavHost(navController = controller, startDestination = CameraDestination(askPermission = false)) {
                composable<CameraDestination> { Text("camera") }
                composable<PosesDestination> { Text("poses") }
                composable<PoseDetailDestination> { Text("detail") }
            }
        }
        composeRule.waitForIdle()
    }

    private fun navigate(destination: Any) {
        composeRule.runOnUiThread { navController!!.navigate(destination) }
        composeRule.waitForIdle()
    }

    private fun backStackRoutes(): List<String> = navController!!.currentBackStack.value
        .map { it.destination }
        .mapNotNull { destination ->
            when {
                destination.hasRoute<CameraDestination>() -> "camera"
                destination.hasRoute<PosesDestination>() -> "poses"
                destination.hasRoute<PoseDetailDestination>() -> "detail"
                else -> null
            }
        }

    @Test
    fun openCameraWithPose_replacesCameraWithPoseId_andDropsLibraryAndDetail() {
        setNavHost()
        navigate(PosesDestination)
        navigate(PoseDetailDestination("w01"))
        assertEquals(listOf("camera", "poses", "detail"), backStackRoutes())

        composeRule.runOnUiThread { navController!!.openCameraWithPose("w01") }
        composeRule.waitForIdle()

        val current = navController!!.currentBackStackEntry
        assertNotNull(current)
        // The permission flag of the camera that was there is kept; the pose id is the one of the detail.
        assertEquals(CameraDestination(askPermission = false, poseId = "w01"), current!!.toRoute<CameraDestination>())
        // One camera, no library and no detail: Back on the camera closes the app as before.
        assertEquals(listOf("camera"), backStackRoutes())
    }

    @Test
    fun notOnDetail_doesNothing() {
        setNavHost()
        val before = navController!!.currentBackStackEntry
        composeRule.runOnUiThread { navController!!.openCameraWithPose("w01") }
        composeRule.waitForIdle()
        assertEquals(listOf("camera"), backStackRoutes())
        val current = navController!!.currentBackStackEntry
        assertEquals(before?.id, current?.id)
        assertEquals(CameraDestination(askPermission = false, poseId = null), current!!.toRoute<CameraDestination>())

        // On the library it does nothing either.
        navigate(PosesDestination)
        val library = navController!!.currentBackStackEntry
        composeRule.runOnUiThread { navController!!.openCameraWithPose("w01") }
        composeRule.waitForIdle()
        assertEquals(listOf("camera", "poses"), backStackRoutes())
        assertEquals(library?.id, navController!!.currentBackStackEntry?.id)

        // Control: on the detail the same call opens the camera, so the checks above can fail.
        navigate(PoseDetailDestination("w01"))
        composeRule.runOnUiThread { navController!!.openCameraWithPose("w01") }
        composeRule.waitForIdle()
        assertEquals(listOf("camera"), backStackRoutes())
    }
}
