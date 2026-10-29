package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** The break gate of :app, provided by MainActivity; null in tests and previews (no limit then). */
val LocalInterstitialBreakGate = staticCompositionLocalOf<InterstitialBreakGate?> { null }

/**
 * Shows the interstitial of [placement] at a natural break if its break limits (InterstitialBreaks) and then the
 * full-screen gate of :core:ads allow it. [onFinished] runs once per call, on the main thread: with false at once
 * when skipped (limits, gate, no ad, no controller), with true after the ad has closed.
 */
@Composable
fun rememberShowInterstitial(placement: AdPlacement, onFinished: (shown: Boolean) -> Unit): () -> Unit =
    rememberBreakInterstitial(placement, onFinished) { ads, activity, shownPlacement, onClosed ->
        ads.showInterstitial(activity, shownPlacement, onClosed)
    }

/** [rememberShowInterstitial] with the call to :core:ads given as [show], for tests. */
@Composable
internal fun rememberBreakInterstitial(
    placement: AdPlacement,
    onFinished: (shown: Boolean) -> Unit,
    show: suspend (ads: AdsController, activity: Activity, placement: AdPlacement, onClosed: () -> Unit) -> Boolean,
): () -> Unit {
    require(placement.format == AdFormat.INTERSTITIAL && !placement.appWide) {
        "${placement.key} is not an interstitial placement of a screen"
    }
    val ads = LocalAdsController.current
    val activity = LocalActivity.current
    val gate = LocalInterstitialBreakGate.current
    val scope = rememberCoroutineScope()
    val currentOnFinished by rememberUpdatedState(onFinished)
    val currentShow by rememberUpdatedState(show)
    return remember(ads, activity, gate, placement) {
        {
            if (ads == null || activity == null) {
                currentOnFinished(false)
            } else {
                scope.launch(Dispatchers.Main.immediate) {
                    val shown = runBreakInterstitial(placement, gate) { onClosed ->
                        currentShow(ads, activity, placement, onClosed)
                    }
                    currentOnFinished(shown)
                }
            }
        }
    }
}

/**
 * One request at a natural break: [gate] counts the break and may skip it; else [show] asks :core:ads, which calls
 * its `onClosed` once in every case. A shown ad moves the limits ([InterstitialBreakGate.onShown]) and the request
 * ends when the ad has closed. Returns whether the ad was shown. No [gate] (tests, previews): no limit.
 */
internal suspend fun runBreakInterstitial(
    placement: AdPlacement,
    gate: InterstitialBreakGate?,
    show: suspend (onClosed: () -> Unit) -> Boolean,
): Boolean {
    val ticket = gate?.onBreak(placement)
    if (gate != null && ticket == null) return false
    val closed = CompletableDeferred<Unit>()
    val shown = show { closed.complete(Unit) }
    if (shown) {
        ticket?.let { gate?.onShown(it) }
        closed.await()
    }
    return shown
}
