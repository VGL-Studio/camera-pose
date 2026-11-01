package com.vglstudio.camera.pose.line.guide.feature.poses.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vglstudio.camera.pose.line.guide.core.analytics.Analytics
import com.vglstudio.camera.pose.line.guide.core.analytics.AnalyticsEvent
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlag
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlags
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.core.network.NetworkStatus
import com.vglstudio.camera.pose.line.guide.core.poses.LoadFailureReason
import com.vglstudio.camera.pose.line.guide.core.poses.model.Pose
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseFilter
import com.vglstudio.camera.pose.line.guide.feature.poses.data.LibraryContent
import com.vglstudio.camera.pose.line.guide.feature.poses.data.LibraryResult
import com.vglstudio.camera.pose.line.guide.feature.poses.data.PoseImageRepository
import com.vglstudio.camera.pose.line.guide.feature.poses.data.PoseImageResult
import com.vglstudio.camera.pose.line.guide.feature.poses.data.PosesLibrarySource
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * The pose detail (spec poses, KB-3, KB-4, KB-5; plan T-022, section 3.1). The pose id is the argument of the
 * destination, read from [savedStateHandle]. Logs hold no pose id or name.
 */
@HiltViewModel
class PoseDetailViewModel @Inject constructor(
    private val librarySource: PosesLibrarySource,
    private val imageRepository: PoseImageRepository,
    private val networkStatus: NetworkStatus,
    private val analytics: Analytics,
    private val logger: AppLogger,
    private val featureFlags: FeatureFlags,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val state = MutableStateFlow<PoseDetailUiState>(PoseDetailUiState.Loading)
    val uiState: StateFlow<PoseDetailUiState> = state.asStateFlow()

    private val openCameraChannel = Channel<String>(Channel.BUFFERED)

    /** Each element opens the camera with that pose, once an unlock is done (spec poses, KB-8). */
    val openCamera: Flow<String> = openCameraChannel.receiveAsFlow()

    private val poseId: String = savedStateHandle.get<String>(KEY_POSE_ID).orEmpty()

    /** Null until the flag is known. */
    private var enabled: Boolean? = null
    private var library: LibraryResult = LibraryResult.Loading
    private var content: LibraryContent? = null
    private var pose: Pose? = null
    private var missingLogged = false

    private var image: DetailImageState = DetailImageState.Loading
    private var imageStarted = false
    private var imageJob: Job? = null

    /** Whether the last failure of the image was for lack of network (the network dialog may be over it). */
    private var imageFailedNetwork = false
    private var imageFailed = false

    /** Saved state shown while a write runs, or until the library has the written value. */
    private var savedOverride: Boolean? = null
    private var writing = false
    private var saveFailed = false

    /** An unlock is being written (spec poses, KB-8): a second grant meanwhile is ignored. */
    private var unlocking = false
    private var unlockFailed = false

    init {
        viewModelScope.launch {
            featureFlags.enabled(FeatureFlag.POSES).distinctUntilChanged().collectLatest { on ->
                enabled = on
                publish()
                if (on) librarySource.library.collect(::onLibrary)
            }
        }
        viewModelScope.launch { networkStatus.dialogShown.collect { publish() } }
        viewModelScope.launch { networkStatus.recovered.collect { onNetworkRecovered() } }
    }

    fun onToggleSave() {
        val content = content ?: return
        val pose = pose ?: return
        if (writing) return
        val saved = !(savedOverride ?: (pose.id in content.savedIds))
        writing = true
        savedOverride = saved
        publish()
        viewModelScope.launch {
            try {
                librarySource.setSaved(pose.id, saved)
                analytics.log(
                    AnalyticsEvent(EVENT_POSE_SAVED, mapOf("pose_id" to pose.id, "saved" to saved.toString())),
                )
                logger.info(LOG_TAG, "Pose saved: $saved")
                // Keep showing the new value until the library has it.
                if (this@PoseDetailViewModel.content?.let { (pose.id in it.savedIds) == saved } != false) {
                    savedOverride = null
                }
            } catch (e: IOException) {
                savedOverride = null
                saveFailed = true
                logger.warn(LOG_TAG, "Pose could not be saved", e)
            } finally {
                writing = false
                publish()
            }
        }
    }

    fun onSaveFailedShown() {
        if (!saveFailed) return
        saveFailed = false
        publish()
    }

    /** The rewarded action of the unlock button ran (KB-8): unlocks, logs, then opens the camera with the pose. */
    fun onUnlockGranted() {
        val ready = state.value as? PoseDetailUiState.Ready ?: return
        if (unlocking) return
        val pose = ready.pose
        if (!ready.locked) {
            // Already usable (flag off, or unlocked elsewhere): no write, no event (KB-8, criterion 8).
            logSelected(pose)
            openCameraChannel.trySend(pose.id)
            return
        }
        unlocking = true
        viewModelScope.launch {
            try {
                if (librarySource.unlock(pose.id)) {
                    analytics.log(
                        AnalyticsEvent(
                            EVENT_PREMIUM_POSE_UNLOCKED,
                            mapOf("pose_id" to pose.id, "category" to pose.categoryId, "source" to SOURCE_DETAIL),
                        ),
                    )
                    logger.info(LOG_TAG, "Premium pose unlocked")
                } else {
                    // Premium poses were switched off meanwhile: nothing written, the pose is usable (T-085).
                    logger.info(LOG_TAG, "Unlock skipped: premium off")
                }
                // pose_selected, source detail, as "Shoot with this pose" (question 6).
                logSelected(pose)
                openCameraChannel.send(pose.id)
            } catch (e: IOException) {
                // Question 7: the pose stays locked; the screen tells the user, who can watch an ad again.
                unlockFailed = true
                logger.warn(LOG_TAG, "Premium pose could not be unlocked", e)
                publish()
            } finally {
                unlocking = false
            }
        }
    }

    fun onUnlockFailedShown() {
        if (!unlockFailed) return
        unlockFailed = false
        publish()
    }

    fun onRetryImage() {
        if (pose == null) return
        logger.info(LOG_TAG, "Retrying image")
        loadImage()
    }

    /** The id of the pose to shoot with, or null when the screen is not ready. */
    fun onShoot(): String? {
        val ready = state.value as? PoseDetailUiState.Ready ?: return null
        logSelected(ready.pose)
        return ready.pose.id
    }

    /** `pose_selected` with source `detail`: the pose goes to the camera. */
    private fun logSelected(pose: PoseDetail) {
        analytics.log(
            AnalyticsEvent(
                EVENT_POSE_SELECTED,
                mapOf("pose_id" to pose.id, "category" to pose.categoryId, "source" to SOURCE_DETAIL),
            ),
        )
        logger.info(LOG_TAG, "Shoot with pose")
    }

    private fun onLibrary(result: LibraryResult) {
        library = result
        when (result) {
            LibraryResult.Loading -> Unit
            is LibraryResult.Failed -> {
                content = null
                pose = null
                logger.warn(LOG_TAG, "Pose detail: library failed")
            }
            is LibraryResult.Ready -> {
                content = result.content
                val found = result.content.posesFor(PoseFilter.All).firstOrNull { it.id == poseId }
                if (found == null) {
                    if (!missingLogged) {
                        logger.warn(LOG_TAG, "Pose detail: pose not in catalog")
                        missingLogged = true
                    }
                    pose = null
                } else {
                    pose = found
                    // Drop the override once the library agrees with it.
                    if (!writing && savedOverride == (found.id in result.content.savedIds)) savedOverride = null
                    onPoseReady(found)
                }
            }
        }
        publish()
    }

    private fun onPoseReady(found: Pose) {
        if (!imageStarted) {
            imageStarted = true
            loadImage()
        }
        if (savedStateHandle.get<Boolean>(KEY_OPENED_LOGGED) != true) {
            savedStateHandle[KEY_OPENED_LOGGED] = true
            analytics.log(
                AnalyticsEvent(EVENT_POSE_DETAIL_OPENED, mapOf("pose_id" to found.id, "category" to found.categoryId)),
            )
            logger.info(LOG_TAG, "Pose detail opened")
        }
    }

    private fun loadImage() {
        val pose = pose ?: return
        imageJob?.cancel()
        image = DetailImageState.Loading
        imageFailed = false
        imageFailedNetwork = false
        publish()
        imageJob = viewModelScope.launch {
            when (val result = imageRepository.load(pose)) {
                is PoseImageResult.Ready -> image = DetailImageState.Ready(result.image)
                is PoseImageResult.Failed -> {
                    val reason = result.failure.reason
                    analytics.log(
                        AnalyticsEvent(
                            EVENT_CONTENT_LOAD_FAILED,
                            mapOf("content" to CONTENT_IMAGE, "reason" to reason.analyticsValue),
                        ),
                    )
                    logger.warn(LOG_TAG, "Image failed: ${reason.analyticsValue}")
                    imageFailed = true
                    imageFailedNetwork = reason == LoadFailureReason.NETWORK
                }
            }
            publish()
        }
    }

    private fun onNetworkRecovered() {
        if (pose == null || !imageFailed) return
        logger.info(LOG_TAG, "Reloading image after the network came back")
        loadImage()
    }

    private fun publish() {
        state.value = when (enabled) {
            null -> PoseDetailUiState.Loading
            false -> PoseDetailUiState.Disabled
            true -> when (library) {
                LibraryResult.Loading -> PoseDetailUiState.Loading
                is LibraryResult.Failed -> PoseDetailUiState.Missing
                is LibraryResult.Ready -> ready()
            }
        }
    }

    private fun ready(): PoseDetailUiState {
        val pose = pose ?: return PoseDetailUiState.Missing
        val content = content ?: return PoseDetailUiState.Missing
        return PoseDetailUiState.Ready(
            pose = PoseDetail(
                id = pose.id,
                categoryId = pose.categoryId,
                name = pose.name,
                categoryName = content.categories.firstOrNull { it.id == pose.categoryId }?.name,
                subjectType = pose.subjectType,
                framing = pose.framing,
                steps = pose.steps,
                mistakes = pose.mistakes,
            ),
            saved = savedOverride ?: (pose.id in content.savedIds),
            image = shownImage(),
            saveFailed = saveFailed,
            locked = pose.id in content.lockedIds,
            unlockFailed = unlockFailed,
        )
    }

    /** A failure for lack of network under the network dialog keeps the spinner (spec poses, KB-5). */
    private fun shownImage(): DetailImageState = when {
        !imageFailed -> image
        imageFailedNetwork && networkStatus.dialogShown.value -> DetailImageState.Loading
        else -> DetailImageState.Failed
    }

    internal companion object {
        const val LOG_TAG = "Poses"
        const val KEY_POSE_ID = "poseId"
        const val KEY_OPENED_LOGGED = "detail_opened_logged"
        const val EVENT_POSE_DETAIL_OPENED = "pose_detail_opened"
        const val EVENT_POSE_SELECTED = "pose_selected"
        const val EVENT_POSE_SAVED = "pose_saved"
        const val EVENT_PREMIUM_POSE_UNLOCKED = "premium_pose_unlocked"
        const val SOURCE_DETAIL = "detail"
        const val EVENT_CONTENT_LOAD_FAILED = "content_load_failed"
        const val CONTENT_IMAGE = "image"
    }
}
