package com.vglstudio.camera.pose.line.guide.feature.camera.pose

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.vglstudio.camera.pose.line.guide.core.analytics.Analytics
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.core.network.NetworkStatus
import com.vglstudio.camera.pose.line.guide.core.poses.PoseCatalogRepository
import com.vglstudio.camera.pose.line.guide.core.poses.PoseHistoryRepository
import com.vglstudio.camera.pose.line.guide.core.poses.PoseLibraryRepository
import com.vglstudio.camera.pose.line.guide.feature.camera.data.PoseOverlaySettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The pose part of the camera (spec camera, KB-1, KB-2), next to CameraViewModel on the same destination. */
@Suppress("unused", "UnusedPrivateProperty")
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

    fun onCategorySelect(id: String?) {}

    fun onPoseSelect(id: String) {}

    fun onThumbShown(id: String) {}

    fun onPreviousStep() {}

    fun onNextStep() {}

    fun onHideSteps() {}

    fun onShowSteps() {}

    fun onRetryOutline() {}

    fun onRetryLibrary() {}

    /** "Tư thế tiếp" of the review screen (T-016). */
    fun onNextPose() {}
}
