package com.vglstudio.camera.pose.line.guide.feature.camera.camera

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.UseCase
import androidx.camera.core.ZoomState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import com.vglstudio.camera.pose.line.guide.feature.camera.MainDispatcherRule
import com.vglstudio.camera.pose.line.guide.feature.camera.match.BodyPose
import com.vglstudio.camera.pose.line.guide.feature.camera.match.FakeFeatureFlags
import com.vglstudio.camera.pose.line.guide.feature.camera.match.MlKitPoseAnalyzer
import com.vglstudio.camera.pose.line.guide.feature.camera.model.FlashMode
import com.vglstudio.camera.pose.line.guide.feature.camera.model.Lens
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import java.io.File
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Two camera screens live at once while the navigation moves from the pose detail to a new camera: the stale screen
 * must not unbind the camera of the new one (FB-20261026-01, spec camera KB-1, KB-6; plan T-058, section 4). The
 * process-wide ProcessCameraProvider is a fake that keeps the set of bound use cases; the tests check that set.
 */
@RunWith(AndroidJUnit4::class)
class CameraXControllerTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val provider = mockk<ProcessCameraProvider>(relaxed = true)
    private val analyzer = MlKitPoseAnalyzer(RecordingAppLogger())

    /** The use cases bound now, across every controller. */
    private val bound = mutableSetOf<UseCase>()

    /** Every successful bind, in order: the owner and its use cases. */
    private val binds = mutableListOf<Pair<LifecycleOwner, List<UseCase>>>()

    /** When true, binding the analysis use case fails, as on a phone that cannot run three use cases. */
    private var analysisNotSupported = false

    @Before
    fun setUp() {
        mockkStatic("androidx.camera.lifecycle.ProcessCameraProviderExtKt")
        coEvery { ProcessCameraProvider.awaitInstance(any()) } returns provider
        every {
            provider.bindToLifecycle(any<LifecycleOwner>(), any<CameraSelector>(), *anyVararg<UseCase>())
        } answers {
            val owner = firstArg<LifecycleOwner>()
            val useCases = useCasesOf(args[2])
            if (analysisNotSupported && useCases.any { it is ImageAnalysis }) {
                throw IllegalArgumentException("No supported surface combination")
            }
            bound += useCases
            binds += owner to useCases
            mockk<Camera>(relaxed = true).also {
                every { it.cameraInfo.zoomState } returns MutableLiveData<ZoomState>()
            }
        }
        every { provider.unbind(*anyVararg<UseCase>()) } answers { bound -= useCasesOf(args[0]).toSet() }
        every { provider.unbindAll() } answers { bound.clear() }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun useCasesOf(arg: Any?): List<UseCase> = (arg as Array<*>).map { it as UseCase }

    private fun controller(flagOn: Boolean, logger: RecordingAppLogger = RecordingAppLogger()) = CameraXController(
        context = context,
        fileStore = CaptureFileStore(File(context.cacheDir, "captures")),
        clock = { 0L },
        logger = logger,
        poseAnalyzer = analyzer,
        featureFlags = FakeFeatureFlags(flagOn),
    )

    /** The use cases of the last bind of [owner]. */
    private fun lastBindOf(owner: LifecycleOwner): List<UseCase> = binds.last { it.first === owner }.second

    @Test
    fun releaseOfStaleController_keepsUseCasesOfTheNewOneBound_flagOff() = runTest {
        releaseOfStaleController_keepsUseCasesOfTheNewOneBound(flagOn = false, expectedUseCases = 2)
    }

    @Test
    fun releaseOfStaleController_keepsUseCasesOfTheNewOneBound_flagOn() = runTest {
        releaseOfStaleController_keepsUseCasesOfTheNewOneBound(flagOn = true, expectedUseCases = 3)
    }

    private suspend fun releaseOfStaleController_keepsUseCasesOfTheNewOneBound(flagOn: Boolean, expectedUseCases: Int) {
        val stale = controller(flagOn)
        val fresh = controller(flagOn)
        stale.open(Lens.BACK, FlashMode.OFF)
        // The library opens over the camera: the stale screen stops and closes its camera.
        stale.close()
        // "Chụp với tư thế này": the new camera screen opens before the stale one is destroyed.
        fresh.open(Lens.BACK, FlashMode.OFF)
        val freshUseCases = lastBindOf(fresh)
        assertEquals(expectedUseCases, freshUseCases.size)
        assertEquals(flagOn, freshUseCases.any { it is ImageAnalysis })

        stale.release()

        assertTrue("the stale controller unbound the use cases of the new one", bound.containsAll(freshUseCases))
        assertEquals(freshUseCases.toSet(), bound)
    }

    // Counter-checks (L-035): a controller still unbinds what it bound itself.

    @Test
    fun close_unbindsItsOwnUseCases() = runTest {
        val fresh = controller(flagOn = true)
        fresh.open(Lens.BACK, FlashMode.OFF)
        val first = lastBindOf(fresh)
        assertEquals(first.toSet(), bound)

        fresh.close()
        assertTrue(bound.none { it in first })

        fresh.open(Lens.BACK, FlashMode.OFF)
        val second = lastBindOf(fresh)
        assertEquals(3, second.size)
        assertEquals(second.toSet(), bound)
    }

    @Test
    fun reopen_unbindsThePreviousUseCasesOfTheSameController() = runTest {
        val fresh = controller(flagOn = false)
        fresh.open(Lens.BACK, FlashMode.OFF)
        val back = lastBindOf(fresh)

        fresh.open(Lens.FRONT, FlashMode.OFF)
        val front = lastBindOf(fresh)

        assertTrue(bound.none { it in back })
        assertEquals(front.toSet(), bound)
    }

    @Test
    fun analysisNotSupported_bindsPreviewAndCapture_keepsOthers() = runTest {
        val other = controller(flagOn = false)
        other.open(Lens.BACK, FlashMode.OFF)
        val otherUseCases = lastBindOf(other)
        analysisNotSupported = true
        val fresh = controller(flagOn = true)

        fresh.open(Lens.BACK, FlashMode.OFF)

        val freshUseCases = lastBindOf(fresh)
        assertEquals(2, freshUseCases.size)
        assertFalse(bound.any { it is ImageAnalysis })
        assertEquals((otherUseCases + freshUseCases).toSet(), bound)
        // Secondary, the way it is done: no controller unbinds the whole process.
        verify(exactly = 0) { provider.unbindAll() }
    }

    @Test
    fun releaseAfterClose_emitsNoFrame() = runTest {
        val frames = mutableListOf<BodyPose?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { analyzer.frames.collect { frames += it } }
        val stale = controller(flagOn = true)
        stale.open(Lens.BACK, FlashMode.OFF)
        stale.close()
        runCurrent()
        assertEquals(listOf<BodyPose?>(null), frames)

        // The screen was already stopped: destroying it must not tell the collectors (the new screen) again.
        stale.release()
        runCurrent()

        assertEquals(listOf<BodyPose?>(null), frames)
    }

    // Spec camera, KB-4 (plan T-072): a lens list the device cannot give is unknown, and the caught error is logged
    // (ADR-0019).

    @Test
    fun lensQueryFails_logsWarn_andLensesAreUnknown() = runTest {
        every { provider.hasCamera(any()) } throws IllegalStateException("camera info unavailable")
        val logger = RecordingAppLogger()

        val lenses = controller(flagOn = false, logger = logger).availableLenses()

        assertEquals(emptySet<Lens>(), lenses)
        val warnings = logger.entries.filter { it.level == LogLevel.WARN }
        assertEquals(warnings.toString(), 1, warnings.size)
        assertEquals("Camera", warnings.single().tag)
        assertTrue(warnings.single().error is IllegalStateException)
    }

    @Test
    fun providerFails_logsWarn_andLensesAreUnknown() = runTest {
        coEvery { ProcessCameraProvider.awaitInstance(any()) } throws IllegalStateException("no camera service")
        val logger = RecordingAppLogger()

        val lenses = controller(flagOn = false, logger = logger).availableLenses()

        assertEquals(emptySet<Lens>(), lenses)
        val warnings = logger.entries.filter { it.level == LogLevel.WARN }
        assertEquals(warnings.toString(), 1, warnings.size)
        assertEquals("Camera", warnings.single().tag)
        assertTrue(warnings.single().error is IllegalStateException)
    }

    // Counter-check (L-035): a lens list the device gives logs no warning.
    @Test
    fun lensQueryOk_noWarn() = runTest {
        every { provider.hasCamera(any()) } returns true
        val logger = RecordingAppLogger()

        val lenses = controller(flagOn = false, logger = logger).availableLenses()

        assertEquals(setOf(Lens.BACK, Lens.FRONT), lenses)
        assertEquals(emptyList<Any>(), logger.entries.filter { it.level == LogLevel.WARN })
    }
}
