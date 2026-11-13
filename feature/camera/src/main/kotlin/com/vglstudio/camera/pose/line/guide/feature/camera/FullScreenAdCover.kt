package com.vglstudio.camera.pose.line.guide.feature.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.flow.StateFlow

/**
 * True while a full-screen ad of the app is showing (spec camera, section 5; plan T-097). A StateFlow, so the camera
 * has the value as soon as it is built. Provided by `:app` from the ads controller.
 */
fun interface FullScreenAdCover {
    fun showing(): StateFlow<Boolean>
}

/**
 * Whether the ad covers the camera. A real SDK ad pauses the screen when its activity comes up, and the screen resumes
 * only once that activity is gone; so a flag still on after the screen paused and resumed since the ad began is stuck,
 * and the camera ignores it. Started, then the flag, then RESUMED without a pause is still covered: the screen can
 * reach RESUMED after the ad began and pause only when the ad activity comes up (plan T-097, section 3.1).
 */
fun adCoversCamera(adShowing: Boolean, resumed: Boolean, pausedSinceAdShown: Boolean): Boolean =
    adShowing && !(resumed && pausedSinceAdShown)

/** The camera opens only while the screen is started and no full-screen ad covers it. */
fun cameraMayOpen(started: Boolean, covered: Boolean): Boolean = started && !covered

/** Reports each RESUMED of the screen and each pause (also when the effect leaves), with the latest callbacks. */
@Composable
fun CameraResumeEffect(onResumed: () -> Unit, onPaused: () -> Unit) {
    val currentOnResumed by rememberUpdatedState(onResumed)
    val currentOnPaused by rememberUpdatedState(onPaused)
    LifecycleResumeEffect(Unit) {
        currentOnResumed()
        onPauseOrDispose { currentOnPaused() }
    }
}
