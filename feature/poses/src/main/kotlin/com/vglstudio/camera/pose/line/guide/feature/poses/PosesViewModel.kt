package com.vglstudio.camera.pose.line.guide.feature.poses

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
import com.vglstudio.camera.pose.line.guide.feature.poses.data.PoseThumbnailRepository
import com.vglstudio.camera.pose.line.guide.feature.poses.data.PosesLibrarySource
import com.vglstudio.camera.pose.line.guide.feature.poses.data.ThumbnailResult
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * The pose library (spec poses, KB-1, KB-2, KB-3, KB-5; plan T-021, section 3.1).
 *
 * The order of the grid is taken from the library when a chip is shown, and kept while it shows: saving a pose on the
 * screen changes its button, not its place (question 3). Thumbnails are loaded only for the cards the grid has
 * composed, and dropped when a card leaves it, so memory holds the images of the cards on screen only.
 */
@HiltViewModel
class PosesViewModel @Inject constructor(
    private val librarySource: PosesLibrarySource,
    private val thumbnailRepository: PoseThumbnailRepository,
    private val networkStatus: NetworkStatus,
    private val analytics: Analytics,
    private val logger: AppLogger,
    private val featureFlags: FeatureFlags,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val state = MutableStateFlow<PosesUiState>(PosesUiState.Loading)
    val uiState: StateFlow<PosesUiState> = state.asStateFlow()

    /** Null until the flag is known. */
    private var enabled: Boolean? = null
    private var library: LibraryResult = LibraryResult.Loading
    private var content: LibraryContent? = null
    private var posesById: Map<String, Pose> = emptyMap()
    private var filter: PoseFilter = decodeFilter(savedStateHandle.get<String>(KEY_FILTER))

    /** The ids of the grid, in the order taken when the chip was shown; null: take it from the library. */
    private var order: List<String>? = null

    /** Saved state shown while a write runs, or until the library has the written value. */
    private val savedOverrides = mutableMapOf<String, Boolean>()
    private val writing = mutableSetOf<String>()
    private var saveFailed = false

    private val needed = mutableSetOf<String>()
    private val thumbnails = mutableMapOf<String, ThumbnailState>()
    private val thumbnailJobs = mutableMapOf<String, Job>()
    private val thumbnailPermits = Semaphore(MAX_THUMBNAIL_LOADS)

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

    fun onSelectFilter(filter: PoseFilter) {
        this.filter = filter
        savedStateHandle[KEY_FILTER] = encodeFilter(filter)
        // A chip shown again takes the order of the library again.
        order = null
        publish()
        val name = when (filter) {
            PoseFilter.All -> "all"
            PoseFilter.Saved -> "saved"
            is PoseFilter.Category -> "category"
        }
        logger.info(LOG_TAG, "Filter: $name")
    }

    /** "Xem tất cả tư thế" of the empty saved list. */
    fun onShowAll() = onSelectFilter(PoseFilter.All)

    fun onToggleSave(id: String) {
        val content = content ?: return
        if (id in writing || id !in posesById) return
        val saved = !(savedOverrides[id] ?: (id in content.savedIds))
        writing += id
        savedOverrides[id] = saved
        publish()
        viewModelScope.launch {
            try {
                librarySource.setSaved(id, saved)
                analytics.log(AnalyticsEvent(EVENT_POSE_SAVED, mapOf("pose_id" to id, "saved" to saved.toString())))
                logger.info(LOG_TAG, "Pose saved: $saved")
                // Keep showing the new value until the library has it.
                if (this@PosesViewModel.content?.let { (id in it.savedIds) == saved } != false) savedOverrides -= id
            } catch (e: IOException) {
                savedOverrides -= id
                saveFailed = true
                logger.warn(LOG_TAG, "Pose could not be saved", e)
            } finally {
                writing -= id
                publish()
            }
        }
    }

    fun onSaveFailedShown() {
        if (!saveFailed) return
        saveFailed = false
        publish()
    }

    fun onRetry() {
        logger.info(LOG_TAG, "Retrying catalog")
        viewModelScope.launch { librarySource.retry() }
    }

    /** A card entered the grid. */
    fun onThumbnailNeeded(id: String) {
        if (!needed.add(id)) return
        loadThumbnail(id)
    }

    /** A card left the grid: its load stops and its image is dropped; the next need loads it again. */
    fun onThumbnailReleased(id: String) {
        if (!needed.remove(id)) return
        thumbnailJobs.remove(id)?.cancel()
        thumbnails -= id
        publish()
    }

    /** A tap on a card whose image failed. */
    fun onRetryThumbnail(id: String) {
        if (thumbnails[id] != ThumbnailState.Failed) return
        logger.info(LOG_TAG, "Retrying thumbnail")
        loadThumbnail(id)
    }

    private fun onLibrary(result: LibraryResult) {
        val previous = library
        library = result
        when (result) {
            LibraryResult.Loading -> Unit
            is LibraryResult.Failed -> if (previous != result) {
                val reason = result.failure.reason
                analytics.log(
                    AnalyticsEvent(
                        EVENT_CONTENT_LOAD_FAILED,
                        mapOf("content" to CONTENT_CATALOG, "reason" to reason.analyticsValue),
                    ),
                )
                logger.warn(LOG_TAG, "Catalog failed: ${reason.analyticsValue}")
            }
            is LibraryResult.Ready -> {
                if (content == null) {
                    logger.info(
                        LOG_TAG,
                        "Library ready: ${result.content.totalPoses} poses, ${result.content.categories.size} groups",
                    )
                }
                content = result.content
                posesById = result.content.posesFor(PoseFilter.All).associateBy { it.id }
                // Drop overrides the library now agrees with.
                savedOverrides.keys.retainAll { id ->
                    id in writing ||
                        (id in result.content.savedIds) != savedOverrides[id]
                }
                // A card that left the catalog is gone from the grid: do not keep its image.
                thumbnails.keys.retainAll { it in posesById }
                needed.retainAll { it in posesById }
            }
        }
        publish()
    }

    private fun loadThumbnail(id: String) {
        val pose = posesById[id] ?: return
        thumbnailJobs.remove(id)?.cancel()
        thumbnails[id] = ThumbnailState.Loading
        publish()
        thumbnailJobs[id] = viewModelScope.launch {
            val result = thumbnailPermits.withPermit { thumbnailRepository.load(pose) }
            thumbnails[id] = when (result) {
                is ThumbnailResult.Ready -> ThumbnailState.Ready(result.image)
                is ThumbnailResult.Failed -> {
                    val reason = result.failure.reason
                    analytics.log(
                        AnalyticsEvent(
                            EVENT_CONTENT_LOAD_FAILED,
                            mapOf("content" to CONTENT_THUMBNAIL, "reason" to reason.analyticsValue),
                        ),
                    )
                    logger.warn(LOG_TAG, "Thumbnail failed: ${reason.analyticsValue}")
                    ThumbnailState.Failed
                }
            }
            thumbnailJobs -= id
            publish()
        }
    }

    private fun onNetworkRecovered() {
        if (library is LibraryResult.Failed) {
            logger.info(LOG_TAG, "Reloading catalog after the network came back")
            viewModelScope.launch { librarySource.retry() }
        }
        val failed = needed.filter { thumbnails[it] == ThumbnailState.Failed }
        if (failed.isEmpty()) return
        logger.info(LOG_TAG, "Reloading ${failed.size} thumbnails after the network came back")
        failed.forEach(::loadThumbnail)
    }

    private fun publish() {
        state.value = when (enabled) {
            null -> PosesUiState.Loading
            false -> PosesUiState.Disabled
            true -> when (val result = library) {
                LibraryResult.Loading -> PosesUiState.Loading
                is LibraryResult.Failed -> {
                    val reason = result.failure.reason
                    // The network dialog shows instead of an error of the screen (spec poses, KB-5).
                    if (reason == LoadFailureReason.NETWORK && networkStatus.dialogShown.value) {
                        PosesUiState.Loading
                    } else {
                        PosesUiState.Error(reason)
                    }
                }
                is LibraryResult.Ready -> ready(result.content)
            }
        }
    }

    private fun ready(content: LibraryContent): PosesUiState.Ready {
        val current = filter
        if (current is PoseFilter.Category && content.categories.none { it.id == current.id }) {
            filter = PoseFilter.All
            savedStateHandle[KEY_FILTER] = encodeFilter(PoseFilter.All)
            order = null
        }
        val fresh = content.posesFor(filter).map { it.id }
        val kept = order?.filter { it in posesById }
        val ids = if (kept == null) fresh else kept + fresh.filterNot { it in kept }
        order = ids
        return PosesUiState.Ready(
            totalPoses = content.totalPoses,
            chips = listOf(PoseChip.All, PoseChip.Saved) + content.categories.map { PoseChip.Category(it.id, it.name) },
            selected = filter,
            cards = ids.mapNotNull { id ->
                posesById[id]?.let { pose ->
                    PoseCardItem(
                        id = pose.id,
                        categoryId = pose.categoryId,
                        name = pose.name,
                        subjectType = pose.subjectType,
                        stepCount = pose.steps.values.values.firstOrNull()?.size ?: 0,
                        saved = savedOverrides[id] ?: (id in content.savedIds),
                        thumbnail = thumbnails[id] ?: ThumbnailState.Loading,
                    )
                }
            },
            saveFailed = saveFailed,
        )
    }

    internal companion object {
        const val LOG_TAG = "Poses"
        const val KEY_FILTER = "poses_filter"
        const val EVENT_POSE_SAVED = "pose_saved"
        const val EVENT_CONTENT_LOAD_FAILED = "content_load_failed"
        const val CONTENT_CATALOG = "catalog"
        const val CONTENT_THUMBNAIL = "thumbnail"
        const val MAX_THUMBNAIL_LOADS = 4
        private const val CATEGORY_PREFIX = "category:"

        fun encodeFilter(filter: PoseFilter): String = when (filter) {
            PoseFilter.All -> "all"
            PoseFilter.Saved -> "saved"
            is PoseFilter.Category -> CATEGORY_PREFIX + filter.id
        }

        fun decodeFilter(value: String?): PoseFilter = when {
            value == "saved" -> PoseFilter.Saved
            value != null &&
                value.startsWith(
                    CATEGORY_PREFIX,
                ) -> PoseFilter.Category(value.removePrefix(CATEGORY_PREFIX))
            else -> PoseFilter.All
        }
    }
}
