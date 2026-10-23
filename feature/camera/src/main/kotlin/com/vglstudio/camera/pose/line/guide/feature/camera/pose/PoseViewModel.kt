package com.vglstudio.camera.pose.line.guide.feature.camera.pose

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vglstudio.camera.pose.line.guide.core.analytics.Analytics
import com.vglstudio.camera.pose.line.guide.core.analytics.AnalyticsEvent
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.core.network.NetworkStatus
import com.vglstudio.camera.pose.line.guide.core.poses.LoadFailureReason
import com.vglstudio.camera.pose.line.guide.core.poses.PoseCatalogRepository
import com.vglstudio.camera.pose.line.guide.core.poses.PoseFileResult
import com.vglstudio.camera.pose.line.guide.core.poses.PoseHistoryRepository
import com.vglstudio.camera.pose.line.guide.core.poses.PoseLibraryRepository
import com.vglstudio.camera.pose.line.guide.core.poses.model.LibraryState
import com.vglstudio.camera.pose.line.guide.core.poses.model.Pose
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseFile
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseFilter
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseLibrary
import com.vglstudio.camera.pose.line.guide.feature.camera.data.PoseOverlaySettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The pose part of the camera (spec camera, KB-1, KB-2), next to CameraViewModel on the same destination, so the
 * chips and the strip work in every camera state (KB-7). The library is taken once, when it is first ready: the strip
 * keeps that order for the life of the screen, also after a pose is marked as used (plan T-014, question 2). Only
 * choices of the user count as used and log `pose_selected`; the pose chosen on opening does not (question 13).
 */
@HiltViewModel
class PoseViewModel @Inject constructor(
    private val libraryRepository: PoseLibraryRepository,
    private val catalogRepository: PoseCatalogRepository,
    private val historyRepository: PoseHistoryRepository,
    private val overlaySettingsRepository: PoseOverlaySettingsRepository,
    private val decoder: PoseImageDecoder,
    private val activePose: ActivePoseHolder,
    private val networkStatus: NetworkStatus,
    private val analytics: Analytics,
    private val logger: AppLogger,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val state = MutableStateFlow<PoseUiState>(PoseUiState.Loading)
    val uiState: StateFlow<PoseUiState> = state.asStateFlow()

    /** Null until the library is first ready. */
    private var library: PoseLibrary? = null
    private var posesById: Map<String, Pose> = emptyMap()
    private var categoryId: String? = null
    private var selected: Pose? = null
    private var stepIndex = 0
    private var outline: PoseImage = PoseImage.Loading
    private var outlineJob: Job? = null
    private val thumbnails = mutableMapOf<String, PoseImage>()
    private var stepsHidden = false
    private var scrollRequest: StripScroll? = null
    private var scrollKey = 0

    init {
        viewModelScope.launch { libraryRepository.library.collect(::onLibraryState) }
        viewModelScope.launch {
            // T-022 opens the camera with a pose, as a new destination or by setting the key on this one.
            savedStateHandle.getStateFlow<String?>(KEY_POSE_ID, null).collect { id ->
                if (id != null && library != null) onRequestedPose(id)
            }
        }
        viewModelScope.launch { networkStatus.recovered.collect { onNetworkRecovered() } }
    }

    /** Null: "Tất cả". The selected pose stays; the strip goes back to its start. */
    fun onCategorySelect(id: String?) {
        val library = library ?: return
        if (id == categoryId) return
        if (id != null && library.categories.none { it.id == id }) return
        categoryId = id
        scrollRequest = StripScroll(index = 0, key = ++scrollKey)
        publish()
        logger.info(LOG_TAG, if (id == null) "Category: all" else "Category: $id")
    }

    /** A tap on a cell: selects its pose; a cell that failed to load also loads its thumbnail again (question 5). */
    fun onPoseSelect(id: String) {
        val pose = posesById[id] ?: return
        if (thumbnails[id] == PoseImage.Failed) loadThumbnail(pose)
        if (selected?.id == id) return
        select(pose, Source.STRIP)
    }

    /** A cell was put on screen: loads its thumbnail once. A failed one loads again only on a tap or network back. */
    fun onThumbShown(id: String) {
        if (id in thumbnails) return
        val pose = posesById[id] ?: return
        loadThumbnail(pose)
    }

    fun onPreviousStep() {
        if (selected == null || stepIndex == 0) return
        stepIndex--
        publish()
    }

    fun onNextStep() {
        val steps = selected?.steps?.resolve() ?: return
        if (stepIndex >= steps.lastIndex) return
        stepIndex++
        publish()
    }

    fun onHideSteps() = setStepsHidden(true)

    fun onShowSteps() = setStepsHidden(false)

    fun onRetryOutline() {
        val pose = selected ?: return
        if (outline != PoseImage.Failed) return
        logger.info(LOG_TAG, "Retrying pose outline")
        loadOutline(pose)
    }

    fun onRetryLibrary() {
        logger.info(LOG_TAG, "Retrying pose library")
        viewModelScope.launch { libraryRepository.retry() }
    }

    /**
     * "Tư thế tiếp" of the review screen (T-016): the pose after the selected one in the strip of the selected chip;
     * after the last one, or when the selected pose is not in the strip, the first one.
     */
    fun onNextPose() {
        val strip = strip()
        if (strip.isEmpty()) return
        val index = strip.indexOfFirst { it.id == selected?.id }
        val next = if (index < 0 || index == strip.lastIndex) strip.first() else strip[index + 1]
        select(next, Source.NEXT)
    }

    private suspend fun onLibraryState(libraryState: LibraryState) {
        if (library != null) return
        when (libraryState) {
            LibraryState.Loading -> state.value = PoseUiState.Loading
            is LibraryState.Failed -> {
                logger.warn(LOG_TAG, "Pose library unavailable: ${libraryState.failure.reason.analyticsValue}")
                state.value = PoseUiState.Unavailable
            }
            is LibraryState.Ready -> onLibraryReady(libraryState.library)
        }
    }

    private suspend fun onLibraryReady(ready: PoseLibrary) {
        stepsHidden = overlaySettingsRepository.stepsHidden.first()
        library = ready
        posesById = ready.posesFor(PoseFilter.All).associateBy { it.id }
        logger.info(LOG_TAG, "Pose library ready: ${posesById.size} poses, ${ready.categories.size} categories")
        val requested = savedStateHandle.get<String>(KEY_POSE_ID)?.let { posesById[it] }
        val first = requested ?: ready.lastUsedPose ?: ready.defaultPose
        if (first != null) select(first, Source.AUTO) else publish()
        savedStateHandle.get<String>(KEY_POSE_ID)?.let(::onRequestedPose)
    }

    private fun onRequestedPose(id: String) {
        savedStateHandle[KEY_POSE_ID] = null
        val pose = posesById[id]
        if (pose == null) {
            logger.warn(LOG_TAG, "Requested pose is not in the catalog")
            return
        }
        select(pose, Source.DETAIL)
    }

    private fun select(pose: Pose, source: Source) {
        if (selected?.id != pose.id) {
            selected = pose
            stepIndex = 0
            activePose.set(pose)
            loadOutline(pose)
        }
        if (source == Source.STRIP || source == Source.NEXT) {
            analytics.log(
                AnalyticsEvent(
                    EVENT_POSE_SELECTED,
                    mapOf("pose_id" to pose.id, "category" to pose.categoryId, "source" to source.analyticsValue),
                ),
            )
        }
        if (source != Source.AUTO) markUsed(pose)
        if (source == Source.DETAIL || strip().none { it.id == pose.id }) categoryId = null
        val index = strip().indexOfFirst { it.id == pose.id }
        if (index >= 0) scrollRequest = StripScroll(index = index, key = ++scrollKey)
        publish()
        logger.info(LOG_TAG, "Pose selected: ${source.analyticsValue}")
    }

    private fun markUsed(pose: Pose) {
        viewModelScope.launch {
            try {
                historyRepository.markUsed(pose.id)
            } catch (e: IOException) {
                logger.warn(LOG_TAG, "Used pose could not be saved", e)
            }
        }
    }

    /** Only the newest load writes the outline: a load for a pose no longer selected is cancelled. */
    private fun loadOutline(pose: Pose) {
        outlineJob?.cancel()
        outline = PoseImage.Loading
        publish()
        outlineJob = viewModelScope.launch {
            val image = loadImage(pose, PoseFile.OUTLINE, OUTLINE_PX)
            if (selected?.id != pose.id) return@launch
            outline = image
            publish()
        }
    }

    private fun loadThumbnail(pose: Pose) {
        thumbnails[pose.id] = PoseImage.Loading
        publish()
        viewModelScope.launch {
            thumbnails[pose.id] = loadImage(pose, PoseFile.THUMBNAIL, THUMBNAIL_PX)
            publish()
        }
    }

    private suspend fun loadImage(pose: Pose, file: PoseFile, targetPx: Int): PoseImage {
        val reason = when (val result = catalogRepository.loadFile(pose, file)) {
            is PoseFileResult.Ready -> {
                val bitmap = decoder.decode(result.file, targetPx)
                if (bitmap != null) return PoseImage.Ready(bitmap)
                LoadFailureReason.OTHER
            }
            is PoseFileResult.Failed -> result.failure.reason
        }
        val content = if (file == PoseFile.OUTLINE) "outline" else "thumbnail"
        analytics.log(
            AnalyticsEvent(EVENT_CONTENT_LOAD_FAILED, mapOf("content" to content, "reason" to reason.analyticsValue)),
        )
        logger.warn(LOG_TAG, "Pose $content failed: ${reason.analyticsValue}")
        return PoseImage.Failed
    }

    private fun onNetworkRecovered() {
        if (library == null) {
            if (state.value == PoseUiState.Unavailable) onRetryLibrary()
            return
        }
        val failedThumbnails = thumbnails.filterValues { it == PoseImage.Failed }.keys
        val outlineFailed = outline == PoseImage.Failed
        if (failedThumbnails.isEmpty() && !outlineFailed) return
        logger.info(LOG_TAG, "Reloading pose content after the network came back")
        failedThumbnails.forEach { id -> posesById[id]?.let(::loadThumbnail) }
        if (outlineFailed) selected?.let(::loadOutline)
    }

    private fun setStepsHidden(hidden: Boolean) {
        if (stepsHidden == hidden || library == null) return
        stepsHidden = hidden
        publish()
        logger.info(LOG_TAG, if (hidden) "Steps hidden" else "Steps shown")
        viewModelScope.launch {
            try {
                overlaySettingsRepository.setStepsHidden(hidden)
            } catch (e: IOException) {
                logger.warn(LOG_TAG, "Step card choice could not be saved", e)
            }
        }
    }

    private fun strip(): List<Pose> {
        val library = library ?: return emptyList()
        return library.posesFor(categoryId?.let { PoseFilter.Category(it) } ?: PoseFilter.All)
    }

    private fun publish() {
        val library = library ?: return
        state.value = PoseUiState.Ready(
            categories = library.categories.map { CategoryChip(it.id, it.name.resolve()) },
            selectedCategoryId = categoryId,
            strip = strip().map { PoseThumbItem(it.id, it.name.resolve(), thumbnails[it.id] ?: PoseImage.Loading) },
            selected = selected?.let { SelectedPose(it.id, it.steps.resolve(), stepIndex, outline) },
            stepsHidden = stepsHidden,
            scrollRequest = scrollRequest,
        )
    }

    private enum class Source(val analyticsValue: String) {
        AUTO("auto"),
        STRIP("strip"),
        DETAIL("detail"),
        NEXT("next"),
    }

    internal companion object {
        const val LOG_TAG = "Camera"
        const val KEY_POSE_ID = "poseId"
        const val EVENT_POSE_SELECTED = "pose_selected"
        const val EVENT_CONTENT_LOAD_FAILED = "content_load_failed"
        const val THUMBNAIL_PX = 256
        const val OUTLINE_PX = 1080
    }
}
