package com.vglstudio.camera.pose.line.guide.feature.poses

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.vglstudio.camera.pose.line.guide.core.analytics.Analytics
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlags
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.core.network.NetworkStatus
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseFilter
import com.vglstudio.camera.pose.line.guide.feature.poses.data.PoseThumbnailRepository
import com.vglstudio.camera.pose.line.guide.feature.poses.data.PosesLibrarySource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Step 3 skeleton (plan T-021): every action does nothing and the state stays Loading.
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

    fun onSelectFilter(filter: PoseFilter) = Unit

    fun onShowAll() = Unit

    fun onToggleSave(id: String) = Unit

    fun onSaveFailedShown() = Unit

    fun onRetry() = Unit

    fun onThumbnailNeeded(id: String) = Unit

    fun onThumbnailReleased(id: String) = Unit

    fun onRetryThumbnail(id: String) = Unit

    internal companion object {
        const val LOG_TAG = "Poses"
    }
}
