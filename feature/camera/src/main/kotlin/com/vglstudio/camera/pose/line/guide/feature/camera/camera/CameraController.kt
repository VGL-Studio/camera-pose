package com.vglstudio.camera.pose.line.guide.feature.camera.camera

import androidx.camera.core.SurfaceRequest
import com.vglstudio.camera.pose.line.guide.feature.camera.model.FlashMode
import com.vglstudio.camera.pose.line.guide.feature.camera.model.Lens
import java.io.File
import kotlinx.coroutines.flow.StateFlow

/** The camera hardware, behind an interface so the view model and the screen are tested on the JVM. */
interface CameraController {
    /** Surface for the viewfinder; null while no camera is bound. */
    val surfaceRequest: StateFlow<SurfaceRequest?>

    /** Binds preview and capture for [lens]. Throws [CameraOpenException]. */
    suspend fun open(lens: Lens, flash: FlashMode): CameraCapabilities

    fun setFlash(mode: FlashMode)

    fun setZoom(ratio: Float)

    /** Throws [CaptureFailedException]. */
    suspend fun takePicture(): File

    fun close()

    /** Closes the camera for good, when the view model is cleared. */
    fun release() {
        close()
    }
}

data class CameraCapabilities(val hasFlash: Boolean, val minZoomRatio: Float, val maxZoomRatio: Float)

class CameraOpenException(cause: Throwable) : Exception(cause)

class CaptureFailedException(cause: Throwable) : Exception(cause)
