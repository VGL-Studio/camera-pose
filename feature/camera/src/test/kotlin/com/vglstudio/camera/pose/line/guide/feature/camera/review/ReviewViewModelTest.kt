package com.vglstudio.camera.pose.line.guide.feature.camera.review

import android.system.ErrnoException
import android.system.OsConstants
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.analytics.AnalyticsEvent
import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import com.vglstudio.camera.pose.line.guide.feature.camera.MainDispatcherRule
import com.vglstudio.camera.pose.line.guide.feature.camera.camera.CaptureFileStore
import com.vglstudio.camera.pose.line.guide.feature.camera.capture.CapturedPhoto
import com.vglstudio.camera.pose.line.guide.feature.camera.capture.CapturedPhotoHolder
import com.vglstudio.camera.pose.line.guide.feature.camera.model.Lens
import com.vglstudio.camera.pose.line.guide.feature.camera.model.TimerSetting
import com.vglstudio.camera.pose.line.guide.feature.camera.photo.StorageFullException
import com.vglstudio.camera.pose.line.guide.feature.camera.review.ReviewEvent.Leave
import com.vglstudio.camera.pose.line.guide.feature.camera.review.ReviewEvent.OpenAppSettings
import com.vglstudio.camera.pose.line.guide.feature.camera.review.ReviewEvent.RequestStoragePermission
import com.vglstudio.camera.pose.line.guide.feature.camera.review.ReviewEvent.Share
import com.vglstudio.camera.pose.line.guide.feature.camera.review.ReviewUiState.Ready
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

/**
 * The review screen logic (spec camera, KB-6). Runs on Robolectric only so that ErrnoException, an Android class,
 * can be built; nothing else needs Android.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ReviewViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val folder = TemporaryFolder()

    private val holder = CapturedPhotoHolder()
    private val photoStore = FakePhotoStore()
    private val permission = FakeStoragePermission()
    private val repository = FakeSavedPhotosRepository()
    private val nextPoseRequests = NextPoseRequests()
    private val analytics = FakeReviewAnalytics()
    private val logger = RecordingAppLogger()
    private val events = mutableListOf<ReviewEvent>()
    private lateinit var fileStore: CaptureFileStore
    private lateinit var file: File

    @Before
    fun setUp() {
        val dir = folder.newFolder("captures")
        fileStore = CaptureFileStore(dir)
        file = File(dir, "capture_1.jpg").apply { writeText("jpeg") }
        holder.put(CapturedPhoto(file, Lens.BACK, TimerSetting.OFF, 1_000L, poseId = "p1"))
    }

    private fun TestScope.viewModel(savedState: SavedStateHandle = SavedStateHandle()): ReviewViewModel {
        val viewModel = ReviewViewModel(
            holder = holder,
            fileStore = fileStore,
            photoStore = photoStore,
            storagePermission = permission,
            repository = repository,
            nextPoseRequests = nextPoseRequests,
            analytics = analytics,
            logger = logger,
            savedStateHandle = savedState,
            decoder = FakePhotoDecoder(),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.collect { events += it } }
        runCurrent()
        return viewModel
    }

    private fun ReviewViewModel.ready(): Ready = uiState.value as Ready

    private fun hasLog(text: String, level: LogLevel? = null) =
        logger.entries.any { it.tag == "Review" && it.message.contains(text) && (level == null || it.level == level) }

    private val savedEvent = AnalyticsEvent("photo_saved", mapOf("pose_id" to "p1"))

    // Opening

    @Test
    fun captured_opensReady_notSaved() = runTest {
        val viewModel = viewModel()
        assertEquals(Ready(file.path, SaveStatus.NOT_SAVED, null), viewModel.uiState.value)
        assertTrue(hasLog("Review opened: captured"))
        assertTrue(events.isEmpty())
    }

    @Test
    fun photoFileMissing_leavesAtOnce() = runTest {
        file.delete()
        viewModel()
        assertEquals(listOf<ReviewEvent>(Leave(false)), events)
        assertTrue(hasLog("Review photo missing", LogLevel.WARN))
    }

    @Test
    fun holderEmpty_noSavedState_leaves() = runTest {
        holder.clear()
        viewModel()
        assertEquals(listOf<ReviewEvent>(Leave(false)), events)
    }

    @Test
    fun processRecreated_restoresFromSavedState() = runTest {
        val savedState = SavedStateHandle()
        viewModel(savedState)
        holder.clear()

        val recreated = viewModel(savedState)

        assertEquals(Ready(file.path, SaveStatus.NOT_SAVED), recreated.uiState.value)
        assertTrue(events.isEmpty())
    }

    // Saving

    @Test
    fun save_showsSavingWhileWriting() = runTest {
        photoStore.pendingSave = CompletableDeferred()
        val viewModel = viewModel()

        viewModel.onSaveClick()
        runCurrent()
        assertEquals(SaveStatus.SAVING, viewModel.ready().save)

        viewModel.onRetakeClick()
        viewModel.onShareClick()
        viewModel.onCloseClick()
        viewModel.onSaveClick()
        runCurrent()
        assertEquals(Ready(file.path, SaveStatus.SAVING, null), viewModel.uiState.value)
        assertTrue(events.isEmpty())
        assertEquals(1, photoStore.saveCalls.size)
    }

    @Test
    fun save_success_logsEventAndRecords() = runTest {
        val viewModel = viewModel()
        viewModel.onSaveClick()
        runCurrent()

        assertEquals(SaveStatus.SAVED, viewModel.ready().save)
        assertEquals(listOf(file), photoStore.saveCalls)
        assertEquals(listOf(savedEvent), analytics.events)
        assertEquals(listOf("content://media/external/images/media/7"), repository.recorded)
        val saved = logger.entries.single { it.message.contains("Photo saved") }
        assertFalse(saved.message.contains("content://"))
        assertFalse(saved.message.contains("capture_1"))
    }

    @Test
    fun save_leaveDuringRecord_recordStillCompletes() = runTest {
        repository.pendingWrite = CompletableDeferred()
        val store = ViewModelStore()
        val created = viewModel()
        val viewModel = ViewModelProvider(
            store,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = created as T
            },
        )[ReviewViewModel::class.java]
        viewModel.onSaveClick()
        runCurrent()

        // "Chụp lại" right after the save: the screen leaves and its view model is cleared during the write.
        viewModel.onRetakeClick()
        runCurrent()
        assertEquals(listOf<ReviewEvent>(Leave(true)), events)
        store.clear()
        repository.pendingWrite!!.complete(Unit)
        runCurrent()

        assertEquals(listOf("content://media/external/images/media/7"), repository.recorded)
    }

    @Test
    fun save_recordFails_stillSaved() = runTest {
        repository.failWrites = true
        val viewModel = viewModel()
        viewModel.onSaveClick()
        runCurrent()

        assertEquals(SaveStatus.SAVED, viewModel.ready().save)
        assertEquals(listOf(savedEvent), analytics.events)
        assertTrue(logger.entries.any { it.level == LogLevel.WARN && it.error is IOException })
    }

    @Test
    fun save_storageFull_showsError_thenRetrySaves() = runTest {
        photoStore.saveError = StorageFullException()
        val viewModel = viewModel()
        viewModel.onSaveClick()
        runCurrent()

        assertEquals(Ready(file.path, SaveStatus.FAILED_STORAGE_FULL), viewModel.uiState.value)
        assertTrue(analytics.events.isEmpty())
        assertTrue(file.exists())

        photoStore.saveError = null
        viewModel.onRetrySave()
        runCurrent()
        assertEquals(SaveStatus.SAVED, viewModel.ready().save)
        assertEquals(2, photoStore.saveCalls.size)
    }

    @Test
    fun save_otherError_showsGenericError() = runTest {
        photoStore.saveError = IOException("boom")
        val viewModel = viewModel()
        viewModel.onSaveClick()
        runCurrent()

        assertEquals(SaveStatus.FAILED, viewModel.ready().save)
        assertTrue(logger.entries.any { it.level == LogLevel.WARN && it.error is IOException })
    }

    @Test
    fun save_ioErrorWithNoSpace_isStorageFull() = runTest {
        photoStore.saveError = IOException(ErrnoException("write", OsConstants.ENOSPC))
        val viewModel = viewModel()
        viewModel.onSaveClick()
        runCurrent()

        assertEquals(SaveStatus.FAILED_STORAGE_FULL, viewModel.ready().save)
    }

    @Test
    fun save_fileVanished_showsError() = runTest {
        val viewModel = viewModel()
        file.delete()
        viewModel.onSaveClick()
        runCurrent()

        assertEquals(SaveStatus.FAILED, viewModel.ready().save)
        assertTrue(photoStore.saveCalls.isEmpty())
    }

    // Storage permission, Android 9 and lower (plan version 2)

    @Test
    fun save_whenNoPermissionNeeded_savesWithoutAsking() = runTest {
        val viewModel = viewModel()
        viewModel.onSaveClick()
        runCurrent()

        assertFalse(events.contains(RequestStoragePermission))
        assertEquals(1, photoStore.saveCalls.size)
        assertEquals(SaveStatus.SAVED, viewModel.ready().save)
    }

    @Test
    fun save_whenPermissionNeeded_asksFirst() = runTest {
        permission.needsRequest = true
        val viewModel = viewModel()
        viewModel.onSaveClick()
        runCurrent()

        assertEquals(listOf<ReviewEvent>(RequestStoragePermission), events)
        assertTrue(photoStore.saveCalls.isEmpty())
        assertEquals(Ready(file.path, SaveStatus.NOT_SAVED), viewModel.uiState.value)
        assertTrue(hasLog("Asking for the storage permission"))

        viewModel.onSaveClick()
        runCurrent()
        assertEquals(listOf<ReviewEvent>(RequestStoragePermission), events)
    }

    @Test
    fun permissionGranted_savesAtOnce() = runTest {
        permission.needsRequest = true
        val viewModel = viewModel()
        viewModel.onSaveClick()
        runCurrent()

        permission.needsRequest = false
        viewModel.onStoragePermissionResult(granted = true, blocked = false)
        runCurrent()

        assertEquals(SaveStatus.SAVED, viewModel.ready().save)
        assertEquals(listOf(savedEvent), analytics.events)
        assertTrue(hasLog("Storage permission granted"))
    }

    @Test
    fun permissionDenied_showsDenied_retryAsksAgain() = runTest {
        permission.needsRequest = true
        val viewModel = viewModel()
        viewModel.onSaveClick()
        runCurrent()

        viewModel.onStoragePermissionResult(granted = false, blocked = false)
        runCurrent()

        assertEquals(Ready(file.path, SaveStatus.PERMISSION_DENIED), viewModel.uiState.value)
        assertTrue(photoStore.saveCalls.isEmpty())
        assertTrue(file.exists())
        assertTrue(analytics.events.isEmpty())
        assertTrue(hasLog("Storage permission denied"))

        viewModel.onRetrySave()
        runCurrent()
        assertEquals(listOf<ReviewEvent>(RequestStoragePermission, RequestStoragePermission), events)
    }

    @Test
    fun permissionBlocked_showsBlocked_andOpensSettings() = runTest {
        permission.needsRequest = true
        val viewModel = viewModel()
        viewModel.onSaveClick()
        runCurrent()

        viewModel.onStoragePermissionResult(granted = false, blocked = true)
        runCurrent()
        assertEquals(SaveStatus.PERMISSION_BLOCKED, viewModel.ready().save)
        assertTrue(hasLog("Storage permission blocked"))

        viewModel.onRetrySave()
        runCurrent()
        assertEquals(listOf<ReviewEvent>(RequestStoragePermission), events)

        viewModel.onOpenAppSettings()
        runCurrent()
        assertEquals(listOf(RequestStoragePermission, OpenAppSettings), events)
    }

    private fun TestScope.blockedViewModel(): ReviewViewModel {
        permission.needsRequest = true
        val viewModel = viewModel()
        viewModel.onSaveClick()
        runCurrent()
        viewModel.onStoragePermissionResult(granted = false, blocked = true)
        runCurrent()
        assertEquals(SaveStatus.PERMISSION_BLOCKED, viewModel.ready().save)
        return viewModel
    }

    @Test
    fun backFromSettings_granted_clearsMessage_withoutSaving() = runTest {
        val viewModel = blockedViewModel()

        permission.needsRequest = false
        viewModel.onScreenStarted()
        runCurrent()

        assertEquals(Ready(file.path, SaveStatus.NOT_SAVED), viewModel.uiState.value)
        assertTrue(photoStore.saveCalls.isEmpty())
    }

    @Test
    fun backFromSettings_stillDenied_keepsMessage() = runTest {
        val viewModel = blockedViewModel()

        viewModel.onScreenStarted()
        runCurrent()

        assertEquals(SaveStatus.PERMISSION_BLOCKED, viewModel.ready().save)
    }

    @Test
    fun discardSave_permissionDenied_staysOnReview() = runTest {
        permission.needsRequest = true
        val viewModel = viewModel()
        viewModel.onRetakeClick()
        viewModel.onDiscardSave()
        runCurrent()
        assertEquals(listOf<ReviewEvent>(RequestStoragePermission), events)

        viewModel.onStoragePermissionResult(granted = false, blocked = false)
        runCurrent()

        assertEquals(Ready(file.path, SaveStatus.PERMISSION_DENIED, null), viewModel.uiState.value)
        assertTrue(events.none { it is Leave })
        assertTrue(file.exists())
    }

    @Test
    fun discardSave_permissionGranted_savesThenLeaves() = runTest {
        permission.needsRequest = true
        val viewModel = viewModel()
        viewModel.onRetakeClick()
        viewModel.onDiscardSave()
        runCurrent()

        permission.needsRequest = false
        viewModel.onStoragePermissionResult(granted = true, blocked = false)
        runCurrent()

        assertEquals(listOf(RequestStoragePermission, Leave(true)), events)
        assertEquals(listOf(savedEvent), analytics.events)
    }

    @Test
    fun permissionResult_afterRecreation_isHandled() = runTest {
        permission.needsRequest = true
        val savedState = SavedStateHandle()
        val first = viewModel(savedState)
        first.onSaveClick()
        runCurrent()
        assertEquals(listOf<ReviewEvent>(RequestStoragePermission), events)

        val recreated = viewModel(savedState)
        permission.needsRequest = false
        recreated.onStoragePermissionResult(granted = true, blocked = false)
        runCurrent()

        assertEquals(SaveStatus.SAVED, recreated.ready().save)
        assertEquals(1, photoStore.saveCalls.size)
    }

    @Test
    fun permissionResult_whenNotAsked_isIgnored() = runTest {
        val viewModel = viewModel()

        viewModel.onStoragePermissionResult(granted = false, blocked = true)
        viewModel.onStoragePermissionResult(granted = true, blocked = false)
        runCurrent()

        assertEquals(Ready(file.path, SaveStatus.NOT_SAVED), viewModel.uiState.value)
        assertTrue(photoStore.saveCalls.isEmpty())
        assertTrue(events.isEmpty())
    }

    // Leaving

    @Test
    fun retake_afterSave_leavesWithoutDialog() = runTest {
        val viewModel = viewModel()
        viewModel.onSaveClick()
        runCurrent()

        viewModel.onRetakeClick()
        runCurrent()

        assertNull(viewModel.ready().pendingExit)
        assertEquals(listOf<ReviewEvent>(Leave(true)), events)
        assertFalse(file.exists())
        assertNull(holder.latest.value)
        assertTrue(hasLog("Review left: retake"))
    }

    @Test
    fun retake_notSaved_asks() = runTest {
        val viewModel = viewModel()
        viewModel.onRetakeClick()
        assertEquals(ExitAction.RETAKE, viewModel.ready().pendingExit)
        assertTrue(events.isEmpty())
        assertTrue(hasLog("Discard asked"))
    }

    @Test
    fun nextPose_notSaved_asks() = runTest {
        val viewModel = viewModel()
        viewModel.onNextPoseClick()
        assertEquals(ExitAction.NEXT_POSE, viewModel.ready().pendingExit)
        assertTrue(events.isEmpty())
    }

    @Test
    fun close_notSaved_asks() = runTest {
        val viewModel = viewModel()
        viewModel.onCloseClick()
        assertEquals(ExitAction.CLOSE, viewModel.ready().pendingExit)
        assertTrue(events.isEmpty())
    }

    @Test
    fun discardConfirm_leavesWithoutSaving() = runTest {
        val viewModel = viewModel()
        viewModel.onRetakeClick()
        viewModel.onDiscardConfirm()
        runCurrent()

        assertEquals(listOf<ReviewEvent>(Leave(false)), events)
        assertTrue(photoStore.saveCalls.isEmpty())
        assertFalse(file.exists())
        assertTrue(analytics.events.isEmpty())
        assertTrue(hasLog("Photo discarded"))
        assertFalse(nextPoseRequests.consume())
    }

    @Test
    fun discardConfirm_fromNextPose_requestsNextPose() = runTest {
        val viewModel = viewModel()
        viewModel.onNextPoseClick()
        viewModel.onDiscardConfirm()
        runCurrent()

        assertEquals(listOf<ReviewEvent>(Leave(false)), events)
        assertTrue(nextPoseRequests.consume())
    }

    @Test
    fun discardSave_savesThenLeaves() = runTest {
        val viewModel = viewModel()
        viewModel.onNextPoseClick()
        viewModel.onDiscardSave()
        runCurrent()

        assertEquals(listOf(savedEvent), analytics.events)
        assertEquals(listOf<ReviewEvent>(Leave(true)), events)
        assertTrue(nextPoseRequests.consume())
    }

    @Test
    fun discardSave_fails_staysWithError() = runTest {
        photoStore.saveError = StorageFullException()
        val viewModel = viewModel()
        viewModel.onNextPoseClick()
        viewModel.onDiscardSave()
        runCurrent()

        assertEquals(Ready(file.path, SaveStatus.FAILED_STORAGE_FULL, null), viewModel.uiState.value)
        assertTrue(events.none { it is Leave })
        assertFalse(nextPoseRequests.consume())
    }

    @Test
    fun dismissDialog_staysOnReview() = runTest {
        val viewModel = viewModel()
        viewModel.onCloseClick()
        assertEquals(ExitAction.CLOSE, viewModel.ready().pendingExit)

        viewModel.onDismissDialog()
        runCurrent()

        assertEquals(Ready(file.path, SaveStatus.NOT_SAVED, null), viewModel.uiState.value)
        assertTrue(events.isEmpty())
        assertTrue(file.exists())
    }

    @Test
    fun nextPose_afterSave_requestsAndLeaves() = runTest {
        val viewModel = viewModel()
        viewModel.onSaveClick()
        runCurrent()

        viewModel.onNextPoseClick()
        runCurrent()

        assertEquals(listOf<ReviewEvent>(Leave(true)), events)
        assertTrue(nextPoseRequests.consume())
        assertFalse(nextPoseRequests.consume())
    }

    // Sharing

    @Test
    fun share_notSaved_sharesCacheFile() = runTest {
        val viewModel = viewModel()
        viewModel.onShareClick()
        runCurrent()

        assertEquals(listOf<ReviewEvent>(Share(file.path)), events)
        assertEquals(SaveStatus.NOT_SAVED, viewModel.ready().save)
        assertTrue(hasLog("Share opened"))
    }

    @Test
    fun share_afterSave_sharesSavedUri() = runTest {
        val viewModel = viewModel()
        viewModel.onSaveClick()
        runCurrent()

        viewModel.onShareClick()
        runCurrent()

        assertEquals(listOf<ReviewEvent>(Share("content://media/external/images/media/7")), events)
        assertEquals(SaveStatus.SAVED, viewModel.ready().save)
    }

    // Opened from the last photo slot of the camera

    @Test
    fun lastSaved_opensSaved_closeLeavesAtOnce() = runTest {
        val uri = "content://media/external/images/media/3"
        photoStore.existing += uri
        val viewModel = viewModel(SavedStateHandle(mapOf("savedPhotoUri" to uri)))
        assertEquals(Ready(uri, SaveStatus.SAVED), viewModel.uiState.value)
        assertTrue(hasLog("Review opened: last saved"))

        viewModel.onCloseClick()
        runCurrent()

        assertEquals(listOf<ReviewEvent>(Leave(false)), events)
        assertTrue(file.exists())
        assertTrue(analytics.events.isEmpty())
    }

    @Test
    fun lastSaved_deleted_leaves() = runTest {
        viewModel(SavedStateHandle(mapOf("savedPhotoUri" to "content://media/external/images/media/3")))
        assertEquals(listOf<ReviewEvent>(Leave(false)), events)
    }

    // The count for the store review request (plan T-019)

    @Test
    fun savedCount_followsRepository() = runTest {
        repository.count.value = 2
        val viewModel = viewModel()
        assertEquals(2, viewModel.savedCount.value)

        viewModel.onSaveClick()
        runCurrent()

        assertEquals(3, viewModel.savedCount.value)
    }
}
