package com.vglstudio.camera.pose.line.guide.navigation

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** The screen after loading (spec loading, KB-1): onboarding the first time, then the camera. */
class MainScreenTest {
    private fun choose(onboardingAvailable: Boolean, onboardingDone: Boolean, cameraAvailable: Boolean) =
        chooseMainScreen(MainScreenInputs(onboardingAvailable, onboardingDone, cameraAvailable))

    @Test
    fun notOnboardedAndOnboardingAvailable_choosesOnboarding() {
        assertEquals(MainScreen.ONBOARDING, choose(onboardingAvailable = true, onboardingDone = false, cameraAvailable = true))
        assertEquals(MainScreen.ONBOARDING, choose(onboardingAvailable = true, onboardingDone = false, cameraAvailable = false))
    }

    @Test
    fun onboardedAndCameraAvailable_choosesCamera() {
        assertEquals(MainScreen.CAMERA, choose(onboardingAvailable = true, onboardingDone = true, cameraAvailable = true))
    }

    @Test
    fun onboardingUnavailable_cameraAvailable_choosesCamera() {
        assertEquals(MainScreen.CAMERA, choose(onboardingAvailable = false, onboardingDone = false, cameraAvailable = true))
        assertEquals(MainScreen.CAMERA, choose(onboardingAvailable = false, onboardingDone = true, cameraAvailable = true))
    }

    @Test
    fun onboardingAvailableButDone_cameraUnavailable_choosesSample() {
        assertEquals(MainScreen.SAMPLE, choose(onboardingAvailable = true, onboardingDone = true, cameraAvailable = false))
    }

    @Test
    fun nothingAvailable_choosesSample() {
        assertEquals(MainScreen.SAMPLE, choose(onboardingAvailable = false, onboardingDone = false, cameraAvailable = false))
    }

    @Test
    fun chooserBeforeOnboardingAndCamera_choosesSample() = runTest {
        assertEquals(MainScreen.SAMPLE, MainScreenChooser().choose())
    }
}
