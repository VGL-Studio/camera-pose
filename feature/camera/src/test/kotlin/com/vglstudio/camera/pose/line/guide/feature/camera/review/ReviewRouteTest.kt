package com.vglstudio.camera.pose.line.guide.feature.camera.review

import android.app.Activity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import com.vglstudio.camera.pose.line.guide.core.review.InAppReview
import com.vglstudio.camera.pose.line.guide.core.review.LocalInAppReview
import com.vglstudio.camera.pose.line.guide.feature.camera.camera.CaptureFileStore
import com.vglstudio.camera.pose.line.guide.feature.camera.capture.CapturedPhotoHolder
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * [ReviewRoute] without an ads controller (tests): leaving the screen reaches the camera exactly once, after a save
 * and after discarding the photo (spec camera, KB-9; plan T-018, section 4).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "vi-w411dp-h891dp-xxhdpi")
class ReviewRouteTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var file: File
    private var leaves = 0
    private val repository = FakeSavedPhotosRepository()
    private val inAppReview = FakeInAppReview()

    /** Records the saved count of each request and how many times the screen had been left by then. */
    private inner class FakeInAppReview : InAppReview {
        val requests = mutableListOf<Int>()
        val leavesAtRequest = mutableListOf<Int>()

        override fun requestIfDue(activity: Activity, savedPhotos: Int) {
            requests += savedPhotos
            leavesAtRequest += leaves
        }
    }

    @Before
    fun setUp() {
        file = folder.newFile("capture_1.jpg").apply { writeText("jpeg") }
    }

    private fun viewModel(savedState: SavedStateHandle) = ReviewViewModel(
        holder = CapturedPhotoHolder(),
        fileStore = CaptureFileStore(folder.root),
        photoStore = FakePhotoStore(),
        storagePermission = FakeStoragePermission(),
        repository = repository,
        nextPoseRequests = NextPoseRequests(),
        analytics = FakeReviewAnalytics(),
        logger = RecordingAppLogger(),
        savedStateHandle = savedState,
        decoder = FakePhotoDecoder(),
    )

    private fun setRoute(savedState: SavedStateHandle) {
        val vm = viewModel(savedState)
        composeRule.setContent { ReviewRoute(onLeave = { leaves++ }, viewModel = vm) }
        composeRule.waitForIdle()
    }

    @Test
    fun savedThenRetake_leavesOnce() {
        setRoute(
            SavedStateHandle(
                mapOf("review_saved_uri" to "content://media/external/images/media/7", "review_path" to file.path),
            ),
        )
        composeRule.onNodeWithText("Chụp lại").performClick()
        composeRule.waitForIdle()
        assertEquals(1, leaves)
    }

    @Test
    fun discardThenLeave_leavesOnce() {
        setRoute(SavedStateHandle(mapOf("review_path" to file.path)))
        composeRule.onNodeWithText("Chụp lại").performClick()
        composeRule.waitForIdle()
        assertEquals(0, leaves)
        composeRule.onNodeWithText("Bỏ ảnh").performClick()
        composeRule.waitForIdle()
        assertEquals(1, leaves)
    }

    // Asking for a store review (plan T-019, version 2)

    private fun setRouteWithReview(savedState: SavedStateHandle) {
        val vm = viewModel(savedState)
        composeRule.setContent {
            CompositionLocalProvider(LocalInAppReview provides inAppReview) {
                ReviewRoute(onLeave = { leaves++ }, viewModel = vm)
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun savedThenRetake_asksReviewOnceAfterLeaving() {
        repository.count.value = 3
        setRouteWithReview(
            SavedStateHandle(
                mapOf("review_saved_uri" to "content://media/external/images/media/7", "review_path" to file.path),
            ),
        )
        composeRule.onNodeWithText("Chụp lại").performClick()
        composeRule.waitForIdle()
        assertEquals(1, leaves)
        assertEquals(listOf(3), inAppReview.requests)
        assertEquals(listOf(1), inAppReview.leavesAtRequest)
    }

    @Test
    fun discardThenLeave_doesNotAskReview() {
        repository.count.value = 3
        setRouteWithReview(SavedStateHandle(mapOf("review_path" to file.path)))
        composeRule.onNodeWithText("Chụp lại").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Bỏ ảnh").performClick()
        composeRule.waitForIdle()
        assertEquals(1, leaves)
        assertTrue(inAppReview.requests.isEmpty())
    }
}
