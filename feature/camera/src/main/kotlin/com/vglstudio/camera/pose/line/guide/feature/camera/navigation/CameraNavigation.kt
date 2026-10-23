package com.vglstudio.camera.pose.line.guide.feature.camera.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.vglstudio.camera.pose.line.guide.feature.camera.CameraRoute
import kotlinx.serialization.Serializable

/**
 * Navigation destination of the camera. [askPermission] is false when onboarding has just asked for the permission
 * and the user said no, so the camera does not ask twice in a row (spec camera, KB-7). [poseId] opens the camera
 * with that pose ("Chụp với tư thế này" of the pose detail, T-022). [onOpenLastPhoto] receives the content uri of the
 * latest saved photo when its slot is tapped (T-016).
 */
@Serializable
data class CameraDestination(val askPermission: Boolean = true, val poseId: String? = null)

fun NavGraphBuilder.cameraScreen(
    onPhotoCaptured: () -> Unit,
    onOpenPoses: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    onOpenLastPhoto: (String) -> Unit = {},
) {
    composable<CameraDestination> {
        CameraRoute(
            onPhotoCaptured = onPhotoCaptured,
            onOpenPoses = onOpenPoses,
            onOpenSettings = onOpenSettings,
            onOpenLastPhoto = onOpenLastPhoto,
        )
    }
}
