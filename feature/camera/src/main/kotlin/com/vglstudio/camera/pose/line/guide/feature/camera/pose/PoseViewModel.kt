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
import com.vglstudio.camera.pose.line.guide.core.poses.PremiumPoseRepository
import com.vglstudio.camera.pose.line.guide.core.poses.model.LibraryState
import com.vglstudio.camera.pose.line.guide.core.poses.model.Pose
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseFile
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseFilter
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseLibrary
import com.vglstudio.camera.pose.line.guide.core.poses.model.PremiumAccess
import com.vglstudio.camera.pose.line.guide.feature.camera.data.OverlaySettings
import com.vglstudio.camera.pose.line.guide.feature.camera.data.PoseOverlaySettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
 * Premium poses not unlocked yet follow the premium access of :core:poses for the life of the screen: their cells are
 * locked, a tap holds the pose for the unlock dialog, and only a granted unlock selects it (spec poses, KB-8; T-086).
 */
@HiltViewModel
class PoseViewModel @Inject constructor(
    private val libraryRepository: PoseLibraryRepository,
    private val catalogRepository: PoseCatalogRepository,
    private val historyRepository: PoseHistoryRepository,
    private val premiumRepository: PremiumPoseRepository,
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

    // How the outline is adjusted (spec camera, KB-3). Only [outlineSettings] is kept; the position is not.
    private var outlineSettings = OverlaySettings()
    private var offsetX = 0f
    private var offsetY = 0f
    private var panelOpen = false
    private var lockHintVisible = false
    private var lockHintJob: Job? = null
    private var gestureMoved = false
    private var gestureScaled = false
    private var gestureHintShown = false
    private var gestureZoom = 1f

    /** Nothing is locked until the first value; it is read before the first pose is chosen (onLibraryReady). */
    private var access = PremiumAccess(enabled = false, unlockedIds = emptySet())

    /** An unlock is being written: a second grant meanwhile is ignored. */
    private var unlocking = false

    init {
        viewModelScope.launch {
            premiumRepository.access.collect {
                access = it
                publish()
            }
        }
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
        // A locked cell calls onLockedPoseSelect; this guard keeps a locked pose from being selected anyway.
        if (access.isLocked(pose)) return
        if (selected?.id == id) return
        select(pose, Source.STRIP)
    }

    /** A tap on the cell of a locked pose (spec poses, KB-8): holds it for the unlock, keeps the selected pose. */
    fun onLockedPoseSelect(id: String) {
        val pose = posesById[id] ?: return
        if (!access.isLocked(pose)) {
            // Unlocked meanwhile (in the detail, or the flag went off): a plain tap.
            onPoseSelect(id)
            return
        }
        if (thumbnails[id] == PoseImage.Failed) loadThumbnail(pose)
        savedStateHandle[KEY_UNLOCK_POSE_ID] = id
        publish()
        logger.info(LOG_TAG, "Locked pose tapped")
    }

    /** The rewarded action of a locked cell ran (KB-8): unlocks, logs, then selects the pose as a tap on a cell. */
    fun onUnlockGranted() {
        val id = savedStateHandle.get<String>(KEY_UNLOCK_POSE_ID) ?: return
        val pose = posesById[id] ?: return
        if (unlocking) return
        if (!access.isLocked(pose)) {
            // Usable already (flag off, unlocked elsewhere): no write, no event (KB-8, criterion 8).
            savedStateHandle[KEY_UNLOCK_POSE_ID] = null
            select(pose, Source.STRIP)
            return
        }
        unlocking = true
        viewModelScope.launch {
            try {
                if (premiumRepository.unlock(id)) {
                    analytics.log(
                        AnalyticsEvent(
                            EVENT_PREMIUM_POSE_UNLOCKED,
                            mapOf("pose_id" to pose.id, "category" to pose.categoryId, "source" to SOURCE_CAMERA),
                        ),
                    )
                    logger.info(LOG_TAG, "Premium pose unlocked")
                } else {
                    // Premium poses were switched off meanwhile: nothing written, the pose is usable (T-085).
                    logger.info(LOG_TAG, "Unlock skipped: premium off")
                }
                savedStateHandle[KEY_UNLOCK_POSE_ID] = null
                // Selected at once, before the new access arrives: select does not check the lock.
                select(pose, Source.STRIP)
            } catch (e: IOException) {
                // Question 7: the pose stays locked and the current pose stays; the cell keeps its badge.
                savedStateHandle[KEY_UNLOCK_POSE_ID] = null
                logger.warn(LOG_TAG, "Premium pose could not be unlocked", e)
                publish()
            } finally {
                unlocking = false
            }
        }
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

    /**
     * A drag or pinch step on the frame: [panX] and [panY] are parts of the frame, [zoom] a factor. Nothing is logged
     * or kept here, the gesture end does that. While locked it changes nothing and shows the lock hint.
     */
    fun onOverlayTransform(panX: Float, panY: Float, zoom: Float) {
        if (!canAdjust() || outlineSettings.hidden) return
        if (outlineSettings.locked) {
            if (!gestureHintShown) {
                gestureHintShown = true
                showLockHint()
            }
            return
        }
        val moved = panX != 0f || panY != 0f
        if (!moved && zoom == 1f) return
        offsetX = clampOffset(offsetX + panX)
        offsetY = clampOffset(offsetY + panY)
        // Two fingers that only drag still jitter a little in distance: the size changes (and is logged) only once
        // the gesture has pinched by more than ZOOM_SLOP in all.
        gestureZoom *= zoom
        if (gestureScaled) {
            outlineSettings = outlineSettings.copy(scale = clampScale(outlineSettings.scale * zoom))
        } else if (abs(gestureZoom - 1f) > ZOOM_SLOP) {
            gestureScaled = true
            outlineSettings = outlineSettings.copy(scale = clampScale(outlineSettings.scale * gestureZoom))
        }
        gestureMoved = gestureMoved || moved
        publish()
    }

    /** The last finger lifted: one `overlay_adjusted` for the gesture. A pinch is kept; a move is not. */
    fun onOverlayGestureEnd() {
        val scaled = gestureScaled
        val moved = gestureMoved
        gestureScaled = false
        gestureMoved = false
        gestureHintShown = false
        gestureZoom = 1f
        if (!canAdjust()) return
        if (scaled) {
            logAdjusted("size")
            saveOutlineSettings()
        } else if (moved) {
            logAdjusted("move")
        }
    }

    fun onOpacityChange(value: Float) {
        if (!canAdjust()) return
        outlineSettings = outlineSettings.copy(opacity = clampOpacity(value))
        publish()
    }

    fun onOpacityChangeFinished() {
        if (!canAdjust()) return
        logAdjusted("opacity")
        saveOutlineSettings()
    }

    fun onScaleChange(value: Float) {
        if (!canAdjust()) return
        outlineSettings = outlineSettings.copy(scale = clampScale(value))
        publish()
    }

    fun onScaleChangeFinished() {
        if (!canAdjust()) return
        logAdjusted("size")
        saveOutlineSettings()
    }

    /** The adjust panel does not open while locked: the button that opens it is dimmed. */
    fun onTogglePanel() {
        if (!canAdjust() || outlineSettings.locked) return
        panelOpen = !panelOpen
        publish()
    }

    fun onClosePanel() {
        if (!panelOpen) return
        panelOpen = false
        publish()
    }

    fun onToggleMirror() {
        if (!canAdjust() || outlineSettings.locked) return
        outlineSettings = outlineSettings.copy(mirrored = !outlineSettings.mirrored)
        publish()
        logAdjusted("flip")
        saveOutlineSettings()
    }

    /** Locking closes the panel (its button is dimmed now) and shows the hint; unlocking hides the hint. */
    fun onToggleLock() {
        if (!canAdjust()) return
        val locked = !outlineSettings.locked
        outlineSettings = outlineSettings.copy(locked = locked)
        if (locked) {
            panelOpen = false
            showLockHint()
        } else {
            hideLockHint()
        }
        publish()
        logAdjusted("lock")
        saveOutlineSettings()
    }

    fun onToggleHidden() {
        if (!canAdjust()) return
        outlineSettings = outlineSettings.copy(hidden = !outlineSettings.hidden)
        publish()
        logAdjusted("hide")
        saveOutlineSettings()
    }

    /** Back to the middle, 100%, the default opacity and not mirrored. Lock and hide stay. */
    fun onResetOverlay() {
        if (!canAdjust() || outlineSettings.locked) return
        offsetX = 0f
        offsetY = 0f
        outlineSettings = outlineSettings.copy(opacity = DEFAULT_OPACITY, scale = 1f, mirrored = false)
        publish()
        logAdjusted("reset")
        saveOutlineSettings()
    }

    /** There is nothing to adjust without a pose (plan T-015, section 2). */
    private fun canAdjust(): Boolean = library != null && selected != null

    private fun logAdjusted(type: String) {
        analytics.log(AnalyticsEvent(EVENT_OVERLAY_ADJUSTED, mapOf("type" to type)))
        logger.info(LOG_TAG, "Overlay adjusted: $type")
    }

    private fun saveOutlineSettings() {
        val settings = outlineSettings
        viewModelScope.launch {
            try {
                overlaySettingsRepository.setOverlay(settings)
            } catch (e: IOException) {
                logger.warn(LOG_TAG, "Overlay settings could not be saved", e)
            }
        }
    }

    private fun showLockHint() {
        lockHintJob?.cancel()
        lockHintVisible = true
        publish()
        lockHintJob = viewModelScope.launch {
            delay(LOCK_HINT_MILLIS)
            lockHintVisible = false
            publish()
        }
    }

    private fun hideLockHint() {
        lockHintJob?.cancel()
        lockHintVisible = false
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
     * after the last one, or when the selected pose is not in the strip, the first one. Locked premium poses are
     * skipped; with no other pose that is not locked, nothing changes (plan T-086, question 8).
     */
    fun onNextPose() {
        val strip = strip()
        if (strip.isEmpty()) return
        val index = strip.indexOfFirst { it.id == selected?.id }
        // From the pose after the selected one, round the strip once. With nothing locked, a strip of only the selected
        // pose selects it again, as before T-086.
        val order = (1..strip.size).map { strip[(index + it).mod(strip.size)] }
        val next = order.firstOrNull { it.id != selected?.id && !access.isLocked(it) }
            ?: order.last().takeIf { strip.none(access::isLocked) }
            ?: return
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
        outlineSettings = overlaySettingsRepository.overlay.first()
        // The locks before the first pose: a pose used last time that is now locked is not opened (camera KB-1, 9).
        access = premiumRepository.access.first()
        library = ready
        posesById = ready.posesFor(PoseFilter.All).associateBy { it.id }
        logger.info(LOG_TAG, "Pose library ready: ${posesById.size} poses, ${ready.categories.size} categories")
        val requested = savedStateHandle.get<String>(KEY_POSE_ID)?.let { posesById[it] }
        // The detail sends a pose only once it is unlocked, so a locked one here is a safeguard: onRequestedPose below
        // logs it and keeps the pose chosen here.
        val first = requested?.takeUnless(access::isLocked)
            ?: ready.lastUsedPose?.takeUnless(access::isLocked)
            ?: ready.defaultPose
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
        if (access.isLocked(pose)) {
            logger.warn(LOG_TAG, "Requested pose is locked")
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
        activePose.setOutline(OutlineView(visible = !outlineSettings.hidden, mirrored = outlineSettings.mirrored))
        val library = library ?: return
        state.value = PoseUiState.Ready(
            categories = library.categories.map { CategoryChip(it.id, it.name.resolve()) },
            selectedCategoryId = categoryId,
            strip = strip().map {
                PoseThumbItem(it.id, it.name.resolve(), thumbnails[it.id] ?: PoseImage.Loading, access.isLocked(it))
            },
            selected = selected?.let { SelectedPose(it.id, it.steps.resolve(), stepIndex, outline) },
            stepsHidden = stepsHidden,
            scrollRequest = scrollRequest,
            overlay = OverlayState(
                settings = outlineSettings,
                offsetX = offsetX,
                offsetY = offsetY,
                panelOpen = panelOpen,
                lockHintVisible = lockHintVisible,
            ),
            unlockPrompt = savedStateHandle.get<String>(KEY_UNLOCK_POSE_ID)
                ?.let { posesById[it] }
                ?.let { UnlockPrompt(it.id, it.name.resolve()) },
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
        const val EVENT_OVERLAY_ADJUSTED = "overlay_adjusted"
        const val EVENT_PREMIUM_POSE_UNLOCKED = "premium_pose_unlocked"
        const val SOURCE_CAMERA = "camera"
        const val KEY_UNLOCK_POSE_ID = "unlockPoseId"
        const val LOCK_HINT_MILLIS = 2_000L
        const val ZOOM_SLOP = 0.03f
        const val THUMBNAIL_PX = 256
        const val OUTLINE_PX = 1080
    }
}
