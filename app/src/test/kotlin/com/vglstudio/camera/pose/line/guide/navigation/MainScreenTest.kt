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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The screen after loading (spec loading, KB-1): onboarding the first time, then the camera. */
class MainScreenTest {
    private fun choose(onboarding: Boolean, done: Boolean) = chooseMainScreen(
        MainScreenInputs(onboardingAvailable = onboarding, onboardingDone = done),
    )

    @Test
    fun notOnboardedAndOnboardingAvailable_choosesOnboarding() {
        assertEquals(MainScreen.ONBOARDING, choose(onboarding = true, done = false))
    }

    @Test
    fun onboarded_choosesCamera() {
        assertEquals(MainScreen.CAMERA, choose(onboarding = true, done = true))
    }

    @Test
    fun onboardingUnavailable_choosesCamera() {
        assertEquals(MainScreen.CAMERA, choose(onboarding = false, done = false))
        assertEquals(MainScreen.CAMERA, choose(onboarding = false, done = true))
    }

    // The app has no other main screen since the sample screen left it (plan T-081, ADR-1008).
    @Test
    fun everyScreen_isOnboardingOrCamera() {
        assertEquals(listOf(MainScreen.ONBOARDING, MainScreen.CAMERA), MainScreen.entries.toList())
    }

    @Test
    fun chooserWithNoFlag_choosesCamera() = runTest {
        val chooser =
            MainScreenChooser(FakeFeatureFlags(emptySet()), FakeOnboardingStatus(done = false), RecordingAppLogger())
        assertEquals(MainScreen.CAMERA, chooser.choose())
    }

    // A flag other than the onboarding flag being on does not change the main screen (plan T-088, section 3.5;
    // was chooser_cameraFlagOn_choosesCamera).
    @Test
    fun chooser_everyOtherFlagOn_choosesCamera() = runTest {
        val flags = FakeFeatureFlags(FeatureFlag.entries.toSet() - FeatureFlag.ONBOARDING)
        val status = FakeOnboardingStatus(done = false)
        val chooser = MainScreenChooser(flags, status, RecordingAppLogger())
        assertEquals(MainScreen.CAMERA, chooser.choose())
        assertEquals(0, status.reads)
        assertEquals(listOf(FeatureFlag.ONBOARDING), flags.read)
    }

    // Spec camera, section 8 (plan T-088): the camera is always the main screen, so no flag gates it.
    @Test
    fun noCameraFlag() {
        assertTrue(FeatureFlag.entries.isNotEmpty())
        assertFalse(
            "feature_camera_enabled must not come back",
            FeatureFlag.entries.any { it.name == "CAMERA" || it.key == "feature_camera_enabled" },
        )
    }

    // The camera flag is no longer read (plan T-081, question 1): onboarding on and done, camera flag off.
    @Test
    fun chooser_cameraFlagOff_choosesCamera() = runTest {
        val flags = FakeFeatureFlags(setOf(FeatureFlag.ONBOARDING))
        val chooser = MainScreenChooser(flags, FakeOnboardingStatus(done = true), RecordingAppLogger())
        assertEquals(MainScreen.CAMERA, chooser.choose())
        assertEquals(listOf(FeatureFlag.ONBOARDING), flags.read)
    }

    // T-012: onboarding flag and "onboarding done".

    @Test
    fun chooser_onboardingAndCameraOn_notDone_choosesOnboarding() = runTest {
        val status = FakeOnboardingStatus(done = false)
        assertEquals(MainScreen.ONBOARDING, chooser(ONBOARDING_ON, status).choose())
    }

    @Test
    fun chooser_onboardingDone_choosesCamera() = runTest {
        val status = FakeOnboardingStatus(done = true)
        assertEquals(MainScreen.CAMERA, chooser(ONBOARDING_ON, status).choose())
        assertEquals(1, status.reads)
    }

    // Passes on the skeleton too; kept as a guard (plan T-012, section 4).
    @Test
    fun chooser_onboardingFlagOff_choosesCamera_withoutReadingStatus() = runTest {
        val status = FakeOnboardingStatus(done = false)
        assertEquals(MainScreen.CAMERA, chooser(emptySet(), status).choose())
        assertEquals(0, status.reads)
    }

    // Onboarding only needs its own flag (plan T-081, question 1; was both flags in plan T-012, question 2).
    @Test
    fun chooser_onboardingOnCameraOff_choosesOnboarding() = runTest {
        val status = FakeOnboardingStatus(done = false)
        assertEquals(MainScreen.ONBOARDING, chooser(setOf(FeatureFlag.ONBOARDING), status).choose())
        assertEquals(1, status.reads)
    }

    @Test
    fun chooser_statusReadFails_showsOnboarding_andLogsWarning() = runTest {
        val logger = RecordingAppLogger()
        val status = FakeOnboardingStatus(done = true, readError = IOException("disk read failed"))
        assertEquals(MainScreen.ONBOARDING, chooser(ONBOARDING_ON, status, logger).choose())
        val warning = logger.entries.single { it.level == LogLevel.WARN }
        assertEquals("Navigation", warning.tag)
        assertTrue(warning.error is IOException)
    }

    @Test
    fun chooser_logsChosenScreen() = runTest {
        val logger = RecordingAppLogger()
        chooser(ONBOARDING_ON, FakeOnboardingStatus(done = false), logger).choose()
        assertTrue(logger.entries.contains(LogEntry(LogLevel.INFO, "Navigation", "Main screen: ONBOARDING")))
    }

    private fun chooser(
        flags: Set<FeatureFlag>,
        status: OnboardingStatus = FakeOnboardingStatus(done = false),
        logger: RecordingAppLogger = RecordingAppLogger(),
    ) = MainScreenChooser(FakeFeatureFlags(flags), status, logger)

    private companion object {
        val ONBOARDING_ON = setOf(FeatureFlag.ONBOARDING)
    }
}

/** Records every flag read, in order. */
private class FakeFeatureFlags(private val enabled: Set<FeatureFlag>) : FeatureFlags {
    val read = mutableListOf<FeatureFlag>()

    override fun enabled(flag: FeatureFlag): Flow<Boolean> {
        read += flag
        return flowOf(flag in enabled)
    }
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
