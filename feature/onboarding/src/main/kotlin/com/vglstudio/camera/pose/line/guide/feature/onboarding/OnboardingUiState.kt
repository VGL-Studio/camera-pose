package com.vglstudio.camera.pose.line.guide.feature.onboarding

/** Everything the onboarding screen can show (spec onboarding, KB-1 and KB-2). */
sealed interface OnboardingUiState {
    /** Waiting for the user to tap "Turn on camera". */
    data object Ready : OnboardingUiState

    /** The system permission prompt is open. */
    data object RequestingPermission : OnboardingUiState

    /** Going to the camera, with or without the permission. */
    data object Finished : OnboardingUiState
}

/** One-off events from the view model to the route. */
sealed interface OnboardingEvent {
    /** The user tapped "Turn on camera": :app marks onboarding done. */
    data object Started : OnboardingEvent

    /** The route launches the system permission prompt. */
    data object RequestPermission : OnboardingEvent

    /** Go to the camera, granted or not. */
    data object Finished : OnboardingEvent
}
