package com.vglstudio.camera.pose.line.guide.navigation

import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlag
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlags
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.onboarding.OnboardingStatus
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Chooses the screen to open when loading is done. Only :app knows every feature (ADR-0001). */
internal class MainScreenChooser @Inject constructor(
    private val featureFlags: FeatureFlags,
    @Suppress("unused") private val onboardingStatus: OnboardingStatus,
    @Suppress("unused") private val logger: AppLogger,
) {
    suspend fun choose(): MainScreen = chooseMainScreen(inputs())

    /**
     * Read once, when loading is done: the choice only matters at that moment (plan T-001, question 3).
     * Loading has just waited for Remote Config (ADR-1001), so the first value is the latest.
     * T-012 reads FeatureFlag.ONBOARDING (first value) and whether onboarding is done.
     */
    private suspend fun inputs() = MainScreenInputs(
        onboardingAvailable = false,
        onboardingDone = false,
        cameraAvailable = featureFlags.enabled(FeatureFlag.CAMERA).first(),
    )
}
