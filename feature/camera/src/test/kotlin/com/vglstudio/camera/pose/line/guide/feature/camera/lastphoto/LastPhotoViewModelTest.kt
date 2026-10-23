package com.vglstudio.camera.pose.line.guide.feature.camera.lastphoto

import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import com.vglstudio.camera.pose.line.guide.feature.camera.MainDispatcherRule
import com.vglstudio.camera.pose.line.guide.feature.camera.review.FakePhotoDecoder
import com.vglstudio.camera.pose.line.guide.feature.camera.review.FakePhotoStore
import com.vglstudio.camera.pose.line.guide.feature.camera.review.FakeSavedPhotosRepository
import com.vglstudio.camera.pose.line.guide.feature.camera.review.NextPoseRequests
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LastPhotoViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeSavedPhotosRepository()
    private val photoStore = FakePhotoStore()
    private val requests = NextPoseRequests()
    private val logger = RecordingAppLogger()

    private fun viewModel() = LastPhotoViewModel(repository, photoStore, requests, logger, FakePhotoDecoder())

    @Test
    fun saved_andExists_exposesUri() = runTest {
        repository.last.value = URI
        photoStore.existing += URI
        val viewModel = viewModel()

        viewModel.onScreenStarted()
        runCurrent()

        assertEquals(URI, viewModel.lastPhoto.value)
    }

    @Test
    fun saved_butDeleted_isNull() = runTest {
        repository.last.value = URI
        val viewModel = viewModel()

        viewModel.onScreenStarted()
        runCurrent()

        assertNull(viewModel.lastPhoto.value)
        assertTrue(logger.entries.any { it.tag == "Camera" && it.message.contains("Last photo missing") })
    }

    @Test
    fun nothingSaved_isNull() = runTest {
        val viewModel = viewModel()

        viewModel.onScreenStarted()
        runCurrent()

        assertNull(viewModel.lastPhoto.value)
        assertTrue(logger.entries.none { it.message.contains("Last photo missing") })
        // Control: the same view model shows a saved photo once there is one.
        repository.last.value = URI
        photoStore.existing += URI
        viewModel.onScreenStarted()
        runCurrent()
        assertEquals(URI, viewModel.lastPhoto.value)
    }

    @Test
    fun startedAgain_afterSave_updates() = runTest {
        val viewModel = viewModel()
        viewModel.onScreenStarted()
        runCurrent()
        assertNull(viewModel.lastPhoto.value)

        repository.last.value = URI
        photoStore.existing += URI
        viewModel.onScreenStarted()
        runCurrent()

        assertEquals(URI, viewModel.lastPhoto.value)
    }

    @Test
    fun nextPoseRequested_consumedOnce() = runTest {
        val viewModel = viewModel()
        requests.request()

        assertTrue(viewModel.consumeNextPoseRequest())
        assertFalse(viewModel.consumeNextPoseRequest())
        assertEquals(1, logger.entries.count { it.tag == "Camera" && it.message.contains("Next pose requested") })
    }

    @Test
    fun noNextPoseRequest_isFalse() = runTest {
        val viewModel = viewModel()
        assertFalse(viewModel.consumeNextPoseRequest())
        assertTrue(logger.entries.isEmpty())
        // Control: a request is then seen, so false came from the missing request.
        requests.request()
        assertTrue(viewModel.consumeNextPoseRequest())
    }

    private companion object {
        const val URI = "content://media/external/images/media/7"
    }
}
