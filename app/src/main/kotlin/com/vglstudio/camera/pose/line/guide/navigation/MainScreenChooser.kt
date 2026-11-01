package com.vglstudio.camera.pose.line.guide.navigation

import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlag
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlags
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.onboarding.OnboardingStatus
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Chooses the screen to open when loading is done. Only :app knows every feature (ADR-0001). */
internal class MainScreenChooser @Inject constructor(
    private val featureFlags: FeatureFlags,
    private val onboardingStatus: OnboardingStatus,
    private val logger: AppLogger,
) {
    suspend fun choose(): MainScreen = chooseMainScreen(inputs()).also { screen ->
        logger.info(LOG_TAG, "Main screen: ${screen.name}")
    }

    /**
     * Read once, when loading is done: the choice only matters at that moment (plan T-001, question 3).
     * Loading has just waited for Remote Config (ADR-1001), so the first value is the latest.
     * The camera is always there, so onboarding only needs its own flag; feature_camera_enabled is not read (plan
     * T-081, question 1). The stored status is read only when onboarding is available; a read error shows onboarding
     * rather than skip it (plan T-012, question 3).
     */
    private suspend fun inputs(): MainScreenInputs {
        val onboarding = featureFlags.enabled(FeatureFlag.ONBOARDING).first()
        val onboardingDone = onboarding && readOnboardingDone()
        return MainScreenInputs(onboardingAvailable = onboarding, onboardingDone = onboardingDone)
    }

    private suspend fun readOnboardingDone(): Boolean = try {
        onboardingStatus.isDone()
    } catch (e: IOException) {
        logger.warn(LOG_TAG, "Could not read onboarding status", e)
        false
    }

    private companion object {
        const val LOG_TAG = "Navigation"
    }
}
