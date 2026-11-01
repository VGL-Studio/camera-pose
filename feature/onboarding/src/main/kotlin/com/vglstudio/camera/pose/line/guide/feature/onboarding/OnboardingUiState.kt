package com.vglstudio.camera.pose.line.guide.feature.onboarding

/**
 * Everything the onboarding flow can show (spec onboarding, KB-1, KB-2 and KB-5). Every state carries the page list
 * and the current page, because the last page stays on screen while the permission prompt is open and while going
 * to the camera.
 */
sealed interface OnboardingUiState {
    val pages: List<OnboardingPage>
    val index: Int
    val page: OnboardingPage get() = pages[index]
    val isLastPage: Boolean get() = index == pages.lastIndex

    /** The user reads page [index]; on the last page, waits for "Turn on camera". */
    data class Browsing(override val pages: List<OnboardingPage>, override val index: Int) : OnboardingUiState

    /** On the last page, the system permission prompt is open. */
    data class RequestingPermission(override val pages: List<OnboardingPage>) : OnboardingUiState {
        override val index: Int get() = pages.lastIndex
    }

    /** On the last page, going to the camera with or without the permission. */
    data class Finished(override val pages: List<OnboardingPage>) : OnboardingUiState {
        override val index: Int get() = pages.lastIndex
    }
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
