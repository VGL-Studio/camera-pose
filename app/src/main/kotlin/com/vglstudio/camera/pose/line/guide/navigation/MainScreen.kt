package com.vglstudio.camera.pose.line.guide.navigation

/** The screens the app can open after loading (spec loading, section 2). */
internal enum class MainScreen { ONBOARDING, CAMERA, SAMPLE }

/** A screen is available when its destination is registered in [AppNavHost] and its flag is on. */
internal data class MainScreenInputs(
    val onboardingAvailable: Boolean,
    val onboardingDone: Boolean,
    val cameraAvailable: Boolean,
)

internal fun chooseMainScreen(inputs: MainScreenInputs): MainScreen = MainScreen.SAMPLE
