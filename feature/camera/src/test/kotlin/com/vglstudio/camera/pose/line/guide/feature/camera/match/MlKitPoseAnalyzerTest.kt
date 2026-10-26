package com.vglstudio.camera.pose.line.guide.feature.camera.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import com.vglstudio.camera.pose.line.guide.feature.camera.MainDispatcherRule
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.ActivePoseHolder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The detector is shared by every camera screen; one screen that leaves must not turn off or close the detection of
 * another one (FB-20261026-01, spec camera KB-5; plan T-058, section 4). ML Kit is replaced by a mock detector.
 */
@RunWith(AndroidJUnit4::class)
class MlKitPoseAnalyzerTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val detector = mockk<PoseDetector>(relaxed = true)
    private val analyzer = MlKitPoseAnalyzer(RecordingAppLogger())

    // Two camera screens: the stale one (leaving) and the new one.
    private val a = Any()
    private val b = Any()

    @Before
    fun setUp() {
        mockkStatic(PoseDetection::class)
        every { PoseDetection.getClient(any()) } returns detector
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun releaseByStaleOwner_keepsDetectorOfActiveOwner() {
        analyzer.setActive(a, true)
        analyzer.setActive(b, true)

        analyzer.release(a)

        verify(exactly = 0) { detector.close() }
        assertTrue(analyzer.running)
    }

    @Test
    fun staleActivatesLast_releaseByStale_keepsRunning() {
        // The order seen when both view models run again for the pose of the detail: the stale one can be the last.
        analyzer.setActive(b, true)
        analyzer.setActive(a, true)

        analyzer.release(a)

        verify(exactly = 0) { detector.close() }
        assertTrue(analyzer.running)
    }

    @Test
    fun setInactiveByOneOwner_keepsRunningForTheOther() {
        analyzer.setActive(a, true)
        analyzer.setActive(b, true)

        analyzer.setActive(a, false)
        assertTrue(analyzer.running)

        analyzer.setActive(b, false)
        assertFalse(analyzer.running)
        verify(exactly = 0) { detector.close() }
    }

    // Counter-checks (L-035): the last owner still closes the detector.

    @Test
    fun releaseByLastOwner_closesDetector() {
        analyzer.setActive(b, true)

        analyzer.release(b)

        verify(exactly = 1) { detector.close() }
        assertFalse(analyzer.running)
    }

    @Test
    fun releaseAfterAllInactive_closesDetector() {
        analyzer.setActive(a, true)
        analyzer.setActive(a, false)

        analyzer.release(a)

        verify(exactly = 1) { detector.close() }
        assertFalse(analyzer.running)
    }

    @Test
    fun activeAfterRelease_makesANewDetector() {
        val c = Any()
        analyzer.setActive(b, true)
        analyzer.release(b)

        analyzer.setActive(c, true)

        verify(exactly = 2) { PoseDetection.getClient(any()) }
        assertTrue(analyzer.running)
    }

    // The real path of two camera screens: two view models on one analyzer, as after "Chụp với tư thế này".

    @Test
    fun twoMatchViewModels_clearOfStale_keepsMatching_staleCreatedFirst() {
        twoMatchViewModels(staleFirst = true)
    }

    @Test
    fun twoMatchViewModels_clearOfStale_keepsMatching_newCreatedFirst() {
        twoMatchViewModels(staleFirst = false)
    }

    private fun twoMatchViewModels(staleFirst: Boolean) {
        val activePose = ActivePoseHolder()
        activePose.set(poseWithKeypoints())
        val staleStore = ViewModelStore()
        val newStore = ViewModelStore()
        val holder = PoseMatchHolder()
        if (staleFirst) {
            matchViewModel(staleStore, activePose, holder)
            matchViewModel(newStore, activePose, holder)
        } else {
            matchViewModel(newStore, activePose, holder)
            matchViewModel(staleStore, activePose, holder)
        }
        // The new screen takes the pose of the detail: both view models run again.
        activePose.set(poseWithKeypoints(id = "w02"))
        assertTrue(analyzer.running)

        // The transition ends: the stale screen is destroyed.
        staleStore.clear()
        assertTrue("the stale screen turned off the matching of the new one", analyzer.running)
        verify(exactly = 0) { detector.close() }

        // Control: the new screen leaves too, and the detector closes once.
        newStore.clear()
        verify(exactly = 1) { detector.close() }
        assertFalse(analyzer.running)
    }

    private fun matchViewModel(store: ViewModelStore, activePose: ActivePoseHolder, holder: PoseMatchHolder) {
        val created = PoseMatchViewModel(
            frameSource = analyzer,
            activePose = activePose,
            featureFlags = FakeFeatureFlags(true),
            settings = FakePoseMatchingSettings(),
            matchHolder = holder,
            logger = RecordingAppLogger(),
        )
        ViewModelProvider(
            store,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = created as T
            },
        )[PoseMatchViewModel::class.java]
    }
}
