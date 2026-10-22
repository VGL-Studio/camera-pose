package com.vglstudio.camera.pose.line.guide.feature.onboarding.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.vglstudio.camera.pose.line.guide.feature.onboarding.OnboardingRoute
import kotlinx.serialization.Serializable

/** Navigation destination of the onboarding screen (spec onboarding). */
@Serializable
data object OnboardingDestination

/**
 * [onStarted]: the user tapped "Turn on camera", so :app marks onboarding done (plan T-010, question 2).
 * [onFinished]: go to the camera, whether the permission was granted or not (spec onboarding, KB-2).
 */
fun NavGraphBuilder.onboardingScreen(onStarted: () -> Unit, onFinished: () -> Unit) {
    composable<OnboardingDestination> {
        OnboardingRoute(onStarted = onStarted, onFinished = onFinished)
    }
}
