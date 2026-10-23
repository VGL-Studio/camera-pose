package com.vglstudio.camera.pose.line.guide.feature.poses.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.vglstudio.camera.pose.line.guide.feature.poses.PosesRoute
import kotlinx.serialization.Serializable

/** Navigation destination of the pose library (spec poses, KB-1). */
@Serializable
data object PosesDestination

/**
 * [onOpenSettings] null hides the settings button (until T-025 wires it); [onOpenPose] null leaves cards without a
 * tap action (until T-022 wires the pose detail).
 */
fun NavGraphBuilder.posesScreen(
    onBack: () -> Unit,
    onOpenSettings: (() -> Unit)? = null,
    onOpenPose: ((String) -> Unit)? = null,
) {
    composable<PosesDestination> {
        PosesRoute(onBack = onBack, onOpenSettings = onOpenSettings, onOpenPose = onOpenPose)
    }
}
