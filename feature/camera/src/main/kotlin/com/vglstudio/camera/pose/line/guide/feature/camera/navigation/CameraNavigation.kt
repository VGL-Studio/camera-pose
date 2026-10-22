package com.vglstudio.camera.pose.line.guide.feature.camera.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.vglstudio.camera.pose.line.guide.feature.camera.CameraRoute
import kotlinx.serialization.Serializable

/**
 * Navigation destination of the camera. [askPermission] is false when onboarding has just asked for the permission
 * and the user said no, so the camera does not ask twice in a row (spec camera, KB-7).
 */
@Serializable
data class CameraDestination(val askPermission: Boolean = true)

fun NavGraphBuilder.cameraScreen(
    onPhotoCaptured: () -> Unit,
    onOpenPoses: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
) {
    composable<CameraDestination> {
        CameraRoute(onPhotoCaptured = onPhotoCaptured, onOpenPoses = onOpenPoses, onOpenSettings = onOpenSettings)
    }
}
