package com.vglstudio.camera.pose.line.guide.navigation

import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlag
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlags
import com.vglstudio.camera.pose.line.guide.core.logging.LogEntry
import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import com.vglstudio.camera.pose.line.guide.onboarding.OnboardingStatus
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        val chooser =
            MainScreenChooser(FakeFeatureFlags(emptySet()), FakeOnboardingStatus(done = false), RecordingAppLogger())
        assertEquals(MainScreen.SAMPLE, chooser.choose())
    }

    @Test
    fun chooser_cameraFlagOn_choosesCamera() = runTest {
        val chooser = MainScreenChooser(
            FakeFeatureFlags(setOf(FeatureFlag.CAMERA)),
            FakeOnboardingStatus(done = false),
            RecordingAppLogger(),
        )
        assertEquals(MainScreen.CAMERA, chooser.choose())
    }

    @Test
    fun chooser_cameraFlagOff_choosesSample() = runTest {
        val chooser =
            MainScreenChooser(FakeFeatureFlags(emptySet()), FakeOnboardingStatus(done = false), RecordingAppLogger())
        assertEquals(MainScreen.SAMPLE, chooser.choose())
    }

    // T-012: onboarding flag, camera flag and "onboarding done".

    @Test
    fun chooser_onboardingAndCameraOn_notDone_choosesOnboarding() = runTest {
        val status = FakeOnboardingStatus(done = false)
        assertEquals(MainScreen.ONBOARDING, chooser(BOTH_FLAGS, status).choose())
    }

    @Test
    fun chooser_onboardingDone_choosesCamera() = runTest {
        val status = FakeOnboardingStatus(done = true)
        assertEquals(MainScreen.CAMERA, chooser(BOTH_FLAGS, status).choose())
        assertEquals(1, status.reads)
    }

    // Passes on the skeleton too; kept as a guard (plan T-012, section 4).
    @Test
    fun chooser_onboardingFlagOff_choosesCamera_withoutReadingStatus() = runTest {
        val status = FakeOnboardingStatus(done = false)
        assertEquals(MainScreen.CAMERA, chooser(setOf(FeatureFlag.CAMERA), status).choose())
        assertEquals(0, status.reads)
    }

    // Passes on the skeleton too; kept as a guard (plan T-012, section 4, question 2).
    @Test
    fun chooser_onboardingOnCameraOff_choosesSample() = runTest {
        val status = FakeOnboardingStatus(done = false)
        assertEquals(MainScreen.SAMPLE, chooser(setOf(FeatureFlag.ONBOARDING), status).choose())
        assertEquals(0, status.reads)
    }

    @Test
    fun chooser_statusReadFails_showsOnboarding_andLogsWarning() = runTest {
        val logger = RecordingAppLogger()
        val status = FakeOnboardingStatus(done = true, readError = IOException("disk read failed"))
        assertEquals(MainScreen.ONBOARDING, chooser(BOTH_FLAGS, status, logger).choose())
        val warning = logger.entries.single { it.level == LogLevel.WARN }
        assertEquals("Navigation", warning.tag)
        assertTrue(warning.error is IOException)
    }

    @Test
    fun chooser_logsChosenScreen() = runTest {
        val logger = RecordingAppLogger()
        chooser(BOTH_FLAGS, FakeOnboardingStatus(done = false), logger).choose()
        assertTrue(logger.entries.contains(LogEntry(LogLevel.INFO, "Navigation", "Main screen: ONBOARDING")))
    }

    private fun chooser(
        flags: Set<FeatureFlag>,
        status: OnboardingStatus = FakeOnboardingStatus(done = false),
        logger: RecordingAppLogger = RecordingAppLogger(),
    ) = MainScreenChooser(FakeFeatureFlags(flags), status, logger)

    private companion object {
        val BOTH_FLAGS = setOf(FeatureFlag.ONBOARDING, FeatureFlag.CAMERA)
    }
}

private class FakeFeatureFlags(private val enabled: Set<FeatureFlag>) : FeatureFlags {
    override fun enabled(flag: FeatureFlag): Flow<Boolean> = flowOf(flag in enabled)
}

/** Counts reads and marks; [readError] makes every read fail. */
private class FakeOnboardingStatus(private var done: Boolean, private val readError: IOException? = null) :
    OnboardingStatus {
    var reads = 0
    var marks = 0

    override suspend fun isDone(): Boolean {
        reads++
        readError?.let { throw it }
        return done
    }

    override suspend fun markDone() {
        marks++
        done = true
    }
}
