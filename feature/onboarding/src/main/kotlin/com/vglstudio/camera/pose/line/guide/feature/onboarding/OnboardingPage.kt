package com.vglstudio.camera.pose.line.guide.feature.onboarding

/** A page of the onboarding flow, in display order (spec onboarding, questions 4 and 10). */
enum class OnboardingPage(val logName: String) {
    INTRO("intro"),
    POSES("poses"),

    /** How to use the outline: match it, adjust it, shoot (spec onboarding, question 10). */
    GUIDE("guide"),

    /** The last page: "Turn on camera" and the permission note. */
    CAMERA("camera"),
}

/** The pages of the flow, always four (spec onboarding, question 10). */
internal val OnboardingPages: List<OnboardingPage> = OnboardingPage.entries
