package com.vglstudio.camera.pose.line.guide.feature.onboarding

/** A page of the onboarding flow, in display order (spec onboarding, question 4). */
enum class OnboardingPage(val logName: String) {
    INTRO("intro"),
    POSES("poses"),

    /** Only while feature_premium_poses_enabled is on (spec onboarding, KB-5). */
    PREMIUM("premium"),

    /** The last page: "Turn on camera" and the permission note. */
    CAMERA("camera"),
}

/** The pages of the flow: three, or four with the premium page while [premium] is on. */
internal fun onboardingPages(premium: Boolean): List<OnboardingPage> = listOfNotNull(
    OnboardingPage.INTRO,
    OnboardingPage.POSES,
    OnboardingPage.PREMIUM.takeIf { premium },
    OnboardingPage.CAMERA,
)
