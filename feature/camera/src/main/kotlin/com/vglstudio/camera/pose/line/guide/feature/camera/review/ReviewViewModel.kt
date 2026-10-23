package com.vglstudio.camera.pose.line.guide.feature.camera.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.vglstudio.camera.pose.line.guide.core.analytics.Analytics
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.feature.camera.camera.CaptureFileStore
import com.vglstudio.camera.pose.line.guide.feature.camera.capture.CapturedPhotoHolder
import com.vglstudio.camera.pose.line.guide.feature.camera.data.SavedPhotosRepository
import com.vglstudio.camera.pose.line.guide.feature.camera.photo.PhotoDecoder
import com.vglstudio.camera.pose.line.guide.feature.camera.photo.PhotoStore
import com.vglstudio.camera.pose.line.guide.feature.camera.photo.StoragePermission
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow

/** The review screen (spec camera, KB-6). Skeleton of plan T-016, step 3. */
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

    val events: Flow<ReviewEvent> = emptyFlow()

    fun onSaveClick() {
    }

    fun onRetrySave() {
    }

    fun onStoragePermissionResult(granted: Boolean, blocked: Boolean) {
    }

    fun onOpenAppSettings() {
    }

    fun onScreenStarted() {
    }

    fun onRetakeClick() {
    }

    fun onNextPoseClick() {
    }

    fun onCloseClick() {
    }

    fun onDiscardConfirm() {
    }

    fun onDiscardSave() {
    }

    fun onDismissDialog() {
    }

    fun onShareClick() {
    }
}
