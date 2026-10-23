package com.vglstudio.camera.pose.line.guide.feature.camera.camera

import androidx.camera.core.CameraControl
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger

/**
 * Sets the zoom ratio on [control]. The returned future is not awaited: the result is not needed (plan T-031).
 * A call that throws only stops zoom from working; it is logged, never thrown to the caller.
 */
internal fun applyZoom(control: CameraControl, ratio: Float, logger: AppLogger) {
    runCatching { control.setZoomRatio(ratio) }
        .onFailure { logger.warn(LOG_TAG, "Zoom could not be set", it) }
}

private const val LOG_TAG = "Camera"
