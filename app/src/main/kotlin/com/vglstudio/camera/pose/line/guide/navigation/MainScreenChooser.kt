package com.vglstudio.camera.pose.line.guide.navigation

import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlags
import javax.inject.Inject

/** Chooses the screen to open when loading is done. Only :app knows every feature (ADR-0001). */
internal class MainScreenChooser @Inject constructor(private val featureFlags: FeatureFlags) {
    suspend fun choose(): MainScreen = chooseMainScreen(inputs())

    /**
     * Read once, when loading is done: the choice only matters at that moment (plan T-001, question 3).
     * T-012 reads FeatureFlag.ONBOARDING (first value) and whether onboarding is done; T-013 reads FeatureFlag.CAMERA.
     */
    private suspend fun inputs() = MainScreenInputs(
        onboardingAvailable = false,
        onboardingDone = false,
        cameraAvailable = false,
    )
}
