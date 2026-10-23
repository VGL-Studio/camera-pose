package com.vglstudio.camera.pose.line.guide.navigation

import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import com.vglstudio.camera.pose.line.guide.feature.camera.navigation.CameraDestination
import com.vglstudio.camera.pose.line.guide.feature.poses.navigation.PoseDetailDestination

/**
 * "Chụp với tư thế này" (spec poses, KB-4): replaces the camera entry with a camera that has [poseId], which the
 * camera reads when it opens (T-014). The library and the detail leave the back stack, so Back on the camera closes
 * the app as before. Runs only while the detail is on top: a second tap does not open a second camera.
 */
internal fun NavController.openCameraWithPose(poseId: String) {
    if (currentDestination?.hasRoute<PoseDetailDestination>() != true) return
    // The permission was asked already when the camera that is there asked it (spec camera, KB-7).
    val askPermission = try {
        getBackStackEntry<CameraDestination>().toRoute<CameraDestination>().askPermission
    } catch (_: IllegalArgumentException) {
        // No camera entry on the back stack.
        true
    }
    navigate(CameraDestination(askPermission = askPermission, poseId = poseId)) {
        popUpTo<CameraDestination> { inclusive = true }
        launchSingleTop = true
    }
}
