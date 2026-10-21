package com.vglstudio.camera.pose.line.guide.feature.loading.navigation

import androidx.compose.animation.ExitTransition
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.vglstudio.camera.pose.line.guide.feature.loading.LoadingRoute
import kotlinx.serialization.Serializable

/** Navigation destination of the loading feature. It is the start destination of the app. */
@Serializable
data object LoadingDestination

/** [onFinished] is called once, when everything is ready (or the feature is off). */
fun NavGraphBuilder.loadingScreen(onFinished: () -> Unit) {
    // No fade-out: loading leaves at once, so the start screen is not covered by a fading loading screen.
    composable<LoadingDestination>(exitTransition = { ExitTransition.None }) {
        LoadingRoute(onFinished = onFinished)
    }
}
