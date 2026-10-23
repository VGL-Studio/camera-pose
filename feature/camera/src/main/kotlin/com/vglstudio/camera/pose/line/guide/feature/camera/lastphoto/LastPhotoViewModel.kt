package com.vglstudio.camera.pose.line.guide.feature.camera.lastphoto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The last photo slot of the camera (spec camera, KB-6): the latest photo saved by the app while it is still in the
 * gallery, read again each time the camera starts (so also after a save on the review screen). Also hands the
 * "Tư thế tiếp" request of the review screen to the camera. Kept apart from CameraViewModel (plan T-016).
 */
@HiltViewModel
class LastPhotoViewModel @Inject constructor(
    private val repository: SavedPhotosRepository,
    private val photoStore: PhotoStore,
    private val nextPoseRequests: NextPoseRequests,
    private val logger: AppLogger,
    internal val decoder: PhotoDecoder,
) : ViewModel() {
    private val state = MutableStateFlow<String?>(null)

    /** Content uri of the latest saved photo, null when none or when it was deleted from the gallery. */
    val lastPhoto: StateFlow<String?> = state.asStateFlow()

    /** True once after the review screen asked for the next pose. */
    fun consumeNextPoseRequest(): Boolean {
        val requested = nextPoseRequests.consume()
        if (requested) logger.info(LOG_TAG, "Next pose requested")
        return requested
    }

    fun onScreenStarted() {
        viewModelScope.launch {
            val uri = repository.lastSavedPhoto.first()
            val present = uri != null && photoStore.exists(uri)
            if (uri != null && !present) logger.info(LOG_TAG, "Last photo missing")
            state.value = if (present) uri else null
        }
    }

    private companion object {
        const val LOG_TAG = "Camera"
    }
}
