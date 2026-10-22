package com.vglstudio.camera.pose.line.guide.navigation

import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlag
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlags
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** The screen after loading (spec loading, KB-1): onboarding the first time, then the camera. */
class MainScreenTest {
    private fun choose(onboarding: Boolean, done: Boolean, camera: Boolean) = chooseMainScreen(
        MainScreenInputs(onboardingAvailable = onboarding, onboardingDone = done, cameraAvailable = camera),
    )

    @Test
    fun notOnboardedAndOnboardingAvailable_choosesOnboarding() {
        assertEquals(MainScreen.ONBOARDING, choose(onboarding = true, done = false, camera = true))
        assertEquals(MainScreen.ONBOARDING, choose(onboarding = true, done = false, camera = false))
    }

    @Test
    fun onboardedAndCameraAvailable_choosesCamera() {
        assertEquals(MainScreen.CAMERA, choose(onboarding = true, done = true, camera = true))
    }

    @Test
    fun onboardingUnavailable_cameraAvailable_choosesCamera() {
        assertEquals(MainScreen.CAMERA, choose(onboarding = false, done = false, camera = true))
        assertEquals(MainScreen.CAMERA, choose(onboarding = false, done = true, camera = true))
    }

    @Test
    fun onboardingAvailableButDone_cameraUnavailable_choosesSample() {
        assertEquals(MainScreen.SAMPLE, choose(onboarding = true, done = true, camera = false))
    }

    @Test
    fun nothingAvailable_choosesSample() {
        assertEquals(MainScreen.SAMPLE, choose(onboarding = false, done = false, camera = false))
    }

    @Test
    fun chooserBeforeOnboardingAndCamera_choosesSample() = runTest {
        assertEquals(MainScreen.SAMPLE, MainScreenChooser(FakeFeatureFlags(emptySet())).choose())
    }

    @Test
    fun chooser_cameraFlagOn_choosesCamera() = runTest {
        assertEquals(MainScreen.CAMERA, MainScreenChooser(FakeFeatureFlags(setOf(FeatureFlag.CAMERA))).choose())
    }

    @Test
    fun chooser_cameraFlagOff_choosesSample() = runTest {
        assertEquals(MainScreen.SAMPLE, MainScreenChooser(FakeFeatureFlags(emptySet())).choose())
    }
}

private class FakeFeatureFlags(private val enabled: Set<FeatureFlag>) : FeatureFlags {
    override fun enabled(flag: FeatureFlag): Flow<Boolean> = flowOf(flag in enabled)
}
