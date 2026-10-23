package com.vglstudio.camera.pose.line.guide.feature.camera.lastphoto

import androidx.lifecycle.ViewModel
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.feature.camera.data.SavedPhotosRepository
import com.vglstudio.camera.pose.line.guide.feature.camera.photo.PhotoDecoder
import com.vglstudio.camera.pose.line.guide.feature.camera.photo.PhotoStore
import com.vglstudio.camera.pose.line.guide.feature.camera.review.NextPoseRequests
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The last photo slot of the camera. Skeleton of plan T-016, step 3. */
@HiltViewModel
class LastPhotoViewModel @Inject constructor(
    private val repository: SavedPhotosRepository,
    private val photoStore: PhotoStore,
    private val nextPoseRequests: NextPoseRequests,
    private val logger: AppLogger,
    internal val decoder: PhotoDecoder,
) : ViewModel() {
    private val state = MutableStateFlow<String?>(null)
    val lastPhoto: StateFlow<String?> = state.asStateFlow()

    fun consumeNextPoseRequest(): Boolean = false

    fun onScreenStarted() {
    }
}
