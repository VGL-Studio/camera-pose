package com.vglstudio.camera.pose.line.guide.feature.camera.capture

import com.vglstudio.camera.pose.line.guide.feature.camera.model.Lens
import com.vglstudio.camera.pose.line.guide.feature.camera.model.TimerSetting
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The photo just taken: a JPEG file in the app cache, not a bitmap in memory. */
data class CapturedPhoto(val file: File, val lens: Lens, val timer: TimerSetting, val capturedAtMillis: Long)

/**
 * Hands the photo just taken to the review screen (T-016). Lives as long as the process. [clear] only drops the
 * reference; CaptureFileStore deletes the file on the next capture.
 */
@Singleton
class CapturedPhotoHolder @Inject constructor() {
    private val photo = MutableStateFlow<CapturedPhoto?>(null)
    val latest: StateFlow<CapturedPhoto?> = photo.asStateFlow()

    fun put(photo: CapturedPhoto) {
        this.photo.value = photo
    }

    fun clear() {
        photo.value = null
    }
}
