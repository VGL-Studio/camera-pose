package com.vglstudio.camera.pose.line.guide.feature.poses.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.vglstudio.camera.pose.line.guide.core.analytics.Analytics
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlags
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.core.network.NetworkStatus
import com.vglstudio.camera.pose.line.guide.feature.poses.data.PoseImageRepository
import com.vglstudio.camera.pose.line.guide.feature.poses.data.PosesLibrarySource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Empty frame (plan T-022, step 3): every function exists, the state stays Loading. */
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

    fun onToggleSave() = Unit

    fun onSaveFailedShown() = Unit

    fun onRetryImage() = Unit

    /** The id of the pose to shoot with, or null when the screen is not ready. */
    fun onShoot(): String? = null

    internal companion object {
        const val LOG_TAG = "Poses"
        const val KEY_POSE_ID = "poseId"
        const val KEY_OPENED_LOGGED = "detail_opened_logged"
    }
}
