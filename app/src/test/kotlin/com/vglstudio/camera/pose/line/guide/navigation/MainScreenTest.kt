package com.vglstudio.camera.pose.line.guide.navigation

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
        assertEquals(MainScreen.SAMPLE, MainScreenChooser().choose())
    }
}
