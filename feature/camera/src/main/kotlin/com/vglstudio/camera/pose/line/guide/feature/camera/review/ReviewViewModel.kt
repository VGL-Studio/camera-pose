package com.vglstudio.camera.pose.line.guide.feature.camera.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vglstudio.camera.pose.line.guide.core.analytics.Analytics
import com.vglstudio.camera.pose.line.guide.core.analytics.AnalyticsEvent
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.feature.camera.camera.CaptureFileStore
import com.vglstudio.camera.pose.line.guide.feature.camera.capture.CapturedPhotoHolder
import com.vglstudio.camera.pose.line.guide.feature.camera.data.SavedPhotosRepository
import com.vglstudio.camera.pose.line.guide.feature.camera.photo.PhotoDecoder
import com.vglstudio.camera.pose.line.guide.feature.camera.photo.PhotoStore
import com.vglstudio.camera.pose.line.guide.feature.camera.photo.StoragePermission
import com.vglstudio.camera.pose.line.guide.feature.camera.photo.isStorageFull
import com.vglstudio.camera.pose.line.guide.feature.camera.review.ReviewUiState.Ready
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The review screen (spec camera, KB-6): the photo just taken (a cache file handed over by [CapturedPhotoHolder]) or,
 * from the last photo slot of the camera, the latest saved photo. Saving goes through [PhotoStore]; on Android 9 and
 * lower it first asks for the storage permission (plan T-016, version 2). Every way out goes through [leave], which
 * deletes the cache file and emits [ReviewEvent.Leave] (T-018 and T-019 build on it). The photo itself is never kept
 * here: the screen decodes it with [decoder].
 */
@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val holder: CapturedPhotoHolder,
    private val fileStore: CaptureFileStore,
    private val photoStore: PhotoStore,
    private val storagePermission: StoragePermission,
    private val repository: SavedPhotosRepository,
    private val nextPoseRequests: NextPoseRequests,
    private val analytics: Analytics,
    private val logger: AppLogger,
    private val savedStateHandle: SavedStateHandle,
    internal val decoder: PhotoDecoder,
) : ViewModel() {
    private val state = MutableStateFlow<ReviewUiState>(ReviewUiState.Loading)
    val uiState: StateFlow<ReviewUiState> = state.asStateFlow()

    /** Photos saved since install, for the review request (plan T-019). */
    val savedCount: StateFlow<Int> =
        repository.savedCount.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private val eventChannel = Channel<ReviewEvent>(Channel.BUFFERED)
    val events: Flow<ReviewEvent> = eventChannel.receiveAsFlow()

    /** Opened from the last photo slot: the photo is already in the gallery, there is no cache file. */
    private val openedFromGallery: Boolean = savedStateHandle.get<String>(KEY_SAVED_PHOTO_URI) != null

    /** True once this visit has saved the photo. */
    private var savedThisVisit: Boolean = savedStateHandle.get<String>(KEY_NEW_SAVED_URI) != null
    private var left = false

    private val awaitingPermission: Boolean get() = savedStateHandle.get<Boolean>(KEY_AWAITING_PERMISSION) == true

    /** "Lưu ảnh" of the dialog: where to go once the save succeeds. Dropped when the save fails or is refused. */
    private var exitAfterSave: ExitAction?
        get() = savedStateHandle.get<String>(KEY_EXIT_AFTER_SAVE)?.let { name ->
            ExitAction.entries.firstOrNull { it.name == name }
        }
        set(value) {
            savedStateHandle[KEY_EXIT_AFTER_SAVE] = value?.name
        }

    init {
        val savedPhotoUri = savedStateHandle.get<String>(KEY_SAVED_PHOTO_URI)
        if (savedPhotoUri != null) openSaved(savedPhotoUri) else openCaptured()
    }

    private fun openCaptured() {
        // The holder is empty after the process was killed: use what the previous instance kept.
        val photo = holder.latest.value
        if (photo != null) {
            savedStateHandle[KEY_PATH] = photo.file.path
            savedStateHandle[KEY_POSE_ID] = photo.poseId
            savedStateHandle[KEY_TAKEN_AT] = photo.capturedAtMillis
        }
        val path = savedStateHandle.get<String>(KEY_PATH)
        if (path == null || !File(path).exists()) {
            logger.warn(LOG_TAG, "Review photo missing")
            left = true
            eventChannel.trySend(ReviewEvent.Leave(savedThisVisit = false))
            return
        }
        state.value = Ready(path, if (savedThisVisit) SaveStatus.SAVED else SaveStatus.NOT_SAVED)
        logger.info(LOG_TAG, "Review opened: captured")
    }

    private fun openSaved(uri: String) {
        viewModelScope.launch {
            if (photoStore.exists(uri)) {
                state.value = Ready(uri, SaveStatus.SAVED)
                logger.info(LOG_TAG, "Review opened: last saved")
            } else {
                logger.warn(LOG_TAG, "Last saved photo missing")
                left = true
                eventChannel.send(ReviewEvent.Leave(savedThisVisit = false))
            }
        }
    }

    fun onSaveClick() {
        requestSave()
    }

    fun onRetrySave() {
        requestSave()
    }

    /** Result of the storage permission dialog (Android 9 and lower); [blocked] when it will not show again. */
    fun onStoragePermissionResult(granted: Boolean, blocked: Boolean) {
        // Read from the saved state, so a result after the activity was recreated is still handled.
        if (!awaitingPermission) return
        savedStateHandle.remove<Boolean>(KEY_AWAITING_PERMISSION)
        if (granted) {
            logger.info(LOG_TAG, "Storage permission granted")
            save()
            return
        }
        exitAfterSave = null
        val ready = ready() ?: return
        if (blocked) {
            state.value = ready.copy(save = SaveStatus.PERMISSION_BLOCKED, pendingExit = null)
            logger.info(LOG_TAG, "Storage permission blocked")
        } else {
            state.value = ready.copy(save = SaveStatus.PERMISSION_DENIED, pendingExit = null)
            logger.info(LOG_TAG, "Storage permission denied")
        }
    }

    fun onOpenAppSettings() {
        if (ready()?.save != SaveStatus.PERMISSION_BLOCKED) return
        logger.info(LOG_TAG, "Opening app settings for storage")
        eventChannel.trySend(ReviewEvent.OpenAppSettings)
    }

    /**
     * Each time the screen starts, also back from the system settings: a permission granted there clears the message.
     * Saving waits for a tap (plan T-016, question 12).
     */
    fun onScreenStarted() {
        val ready = ready() ?: return
        val refused = ready.save == SaveStatus.PERMISSION_DENIED || ready.save == SaveStatus.PERMISSION_BLOCKED
        if (refused && !storagePermission.needsRequest()) {
            state.value = ready.copy(save = SaveStatus.NOT_SAVED)
            logger.info(LOG_TAG, "Storage permission granted in settings")
        }
    }

    fun onRetakeClick() = askToLeave(ExitAction.RETAKE)

    fun onNextPoseClick() = askToLeave(ExitAction.NEXT_POSE)

    /** The close button and the system Back button. */
    fun onCloseClick() = askToLeave(ExitAction.CLOSE)

    /** "Bỏ ảnh". */
    fun onDiscardConfirm() {
        val action = ready()?.pendingExit ?: return
        logger.info(LOG_TAG, "Photo discarded")
        leave(action)
    }

    /** "Lưu ảnh": closes the dialog, saves, then goes where the user asked. */
    fun onDiscardSave() {
        val ready = ready() ?: return
        val action = ready.pendingExit ?: return
        state.value = ready.copy(pendingExit = null)
        exitAfterSave = action
        if (!requestSave()) exitAfterSave = null
    }

    /** A tap outside the dialog, or Back while it shows. */
    fun onDismissDialog() {
        val ready = ready() ?: return
        if (ready.pendingExit != null) state.value = ready.copy(pendingExit = null)
    }

    fun onShareClick() {
        val ready = ready() ?: return
        if (ready.save == SaveStatus.SAVING || left) return
        val location = savedStateHandle.get<String>(KEY_NEW_SAVED_URI) ?: ready.location
        logger.info(LOG_TAG, "Share opened")
        eventChannel.trySend(ReviewEvent.Share(location))
    }

    /** The share sheet could not open (no app takes it). */
    internal fun onShareFailed(error: Throwable) {
        logger.warn(LOG_TAG, "Share unavailable", error)
    }

    /** Starts a save or asks for the permission first. False when nothing could start. */
    private fun requestSave(): Boolean {
        val ready = ready() ?: return false
        if (left || awaitingPermission || ready.save !in SAVABLE) return false
        if (storagePermission.needsRequest()) {
            savedStateHandle[KEY_AWAITING_PERMISSION] = true
            logger.info(LOG_TAG, "Asking for the storage permission")
            eventChannel.trySend(ReviewEvent.RequestStoragePermission)
            return true
        }
        save()
        return true
    }

    private fun save() {
        val ready = ready() ?: return
        state.value = ready.copy(save = SaveStatus.SAVING, pendingExit = null)
        viewModelScope.launch {
            val file = File(ready.location)
            if (!file.exists()) {
                logger.warn(LOG_TAG, "Photo file missing at save")
                failSave(SaveStatus.FAILED)
                return@launch
            }
            val uri = try {
                photoStore.save(file, savedStateHandle.get<Long>(KEY_TAKEN_AT) ?: System.currentTimeMillis())
            } catch (e: IOException) {
                val full = isStorageFull(e)
                logger.warn(LOG_TAG, if (full) "Save failed: storage full" else "Save failed", e)
                failSave(if (full) SaveStatus.FAILED_STORAGE_FULL else SaveStatus.FAILED)
                return@launch
            }
            savedStateHandle[KEY_NEW_SAVED_URI] = uri
            savedThisVisit = true
            state.value = ready.copy(save = SaveStatus.SAVED, pendingExit = null)
            val poseId = savedStateHandle.get<String>(KEY_POSE_ID).orEmpty()
            analytics.log(AnalyticsEvent(EVENT_PHOTO_SAVED, mapOf("pose_id" to poseId)))
            logger.info(LOG_TAG, "Photo saved")
            try {
                // Leaving right after the save clears this view model: the slot and the count must still be stored.
                withContext(NonCancellable) { repository.recordSaved(uri) }
            } catch (e: IOException) {
                // The photo is in the gallery; only the slot and the count miss it.
                logger.warn(LOG_TAG, "Saved photo not recorded", e)
            }
            exitAfterSave?.let(::leave)
        }
    }

    private fun failSave(status: SaveStatus) {
        exitAfterSave = null
        val ready = ready() ?: return
        state.value = ready.copy(save = status, pendingExit = null)
    }

    private fun askToLeave(action: ExitAction) {
        val ready = ready() ?: return
        if (left || ready.save == SaveStatus.SAVING || awaitingPermission) return
        if (ready.save == SaveStatus.SAVED) {
            leave(action)
            return
        }
        state.value = ready.copy(pendingExit = action)
        logger.info(LOG_TAG, "Discard asked")
    }

    /** The only way out of the review screen. */
    private fun leave(action: ExitAction) {
        if (left) return
        left = true
        exitAfterSave = null
        val ready = ready()
        if (!openedFromGallery && ready != null) {
            fileStore.delete(File(ready.location))
            holder.clear()
        }
        if (ready != null) state.value = ready.copy(pendingExit = null)
        if (action == ExitAction.NEXT_POSE) nextPoseRequests.request()
        logger.info(LOG_TAG, "Review left: ${action.logValue}")
        eventChannel.trySend(ReviewEvent.Leave(savedThisVisit))
    }

    private fun ready(): Ready? = state.value as? Ready

    private companion object {
        const val LOG_TAG = "Review"
        const val EVENT_PHOTO_SAVED = "photo_saved"

        /** Argument of ReviewDestination: set when opened from the last photo slot. */
        const val KEY_SAVED_PHOTO_URI = "savedPhotoUri"
        const val KEY_PATH = "review_path"
        const val KEY_POSE_ID = "review_pose_id"
        const val KEY_TAKEN_AT = "review_taken_at"
        const val KEY_NEW_SAVED_URI = "review_saved_uri"
        const val KEY_AWAITING_PERMISSION = "awaiting_storage_permission"
        const val KEY_EXIT_AFTER_SAVE = "review_exit_after_save"

        val SAVABLE = setOf(
            SaveStatus.NOT_SAVED,
            SaveStatus.FAILED_STORAGE_FULL,
            SaveStatus.FAILED,
            SaveStatus.PERMISSION_DENIED,
        )
    }
}
