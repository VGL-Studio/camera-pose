package com.vglstudio.camera.pose.line.guide.feature.poses.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.vglstudio.camera.pose.line.guide.feature.poses.detail.PoseDetailRoute
import kotlinx.serialization.Serializable

/** Navigation destination of the pose detail (spec poses, KB-4). [poseId] is read by the view model. */
@Serializable
data class PoseDetailDestination(val poseId: String)

/** [onShootWithPose] opens the camera with the pose; :app wires it (ADR-0001: features never open each other). */
fun NavGraphBuilder.poseDetailScreen(onBack: () -> Unit, onShootWithPose: (String) -> Unit) {
    composable<PoseDetailDestination> {
        PoseDetailRoute(onBack = onBack, onShootWithPose = onShootWithPose)
    }
}
