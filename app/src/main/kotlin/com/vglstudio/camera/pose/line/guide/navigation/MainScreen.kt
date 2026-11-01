package com.vglstudio.camera.pose.line.guide.navigation

/** The screens the app can open after loading (spec loading, section 2). */
internal enum class MainScreen { ONBOARDING, CAMERA }

/** Onboarding is available when its destination is registered in [AppNavHost] and its flag is on. */
internal data class MainScreenInputs(val onboardingAvailable: Boolean, val onboardingDone: Boolean)

/**
 * Onboarding the first time, then the camera (spec loading, section 2). The camera is the screen of every other case:
 * the app has no other main screen since the sample screen left it (plan T-081, ADR-1008).
 */
internal fun chooseMainScreen(inputs: MainScreenInputs): MainScreen = when {
    inputs.onboardingAvailable && !inputs.onboardingDone -> MainScreen.ONBOARDING
    else -> MainScreen.CAMERA
}
