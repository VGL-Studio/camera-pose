package com.vglstudio.camera.pose.line.guide.feature.camera

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow

/**
 * True while a full-screen ad of the app is showing (spec camera, section 5; plan T-097). A StateFlow, so the camera
 * has the value as soon as it is built. Provided by `:app` from the ads controller.
 */
fun interface FullScreenAdCover {
    fun showing(): StateFlow<Boolean>
}

/** Skeleton for the tests written first (plan T-097, section 3.2). */
fun adCoversCamera(adShowing: Boolean, resumed: Boolean, pausedSinceAdShown: Boolean): Boolean = false

/** Skeleton for the tests written first (plan T-097, section 3.2). */
fun cameraMayOpen(started: Boolean, covered: Boolean): Boolean = true

/** Skeleton for the tests written first (plan T-097, section 3.2). */
@Composable
fun CameraResumeEffect(onResumed: () -> Unit, onPaused: () -> Unit) {
}
