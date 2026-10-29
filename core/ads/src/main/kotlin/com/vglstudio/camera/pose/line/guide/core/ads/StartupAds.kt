package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** The ad of the loading screen when the app opens (KB-13). Bound to AdsController by Hilt. */
interface StartupAds {
    /** Waits within the limits of A-03 and returns the turn to show, or null; null at once when the key is off. */
    suspend fun awaitStartupAd(): StartupAdTurn?
}

/**
 * One ad to show when the app opens. Only the two placements of the loading screen are accepted, `open_app` and
 * `open_app_inter`: never the combo (`native_combo`) nor the resume ad (`resume`, `resume_inter`, plan T-025).
 */
class StartupAdTurn(val placement: AdPlacement) {
    init {
        require(placement == AdPlacement.OPEN_APP || placement == AdPlacement.OPEN_APP_INTER) {
            "${placement.key} is not a startup placement"
        }
    }
}

/**
 * Returns an action that shows a [StartupAdTurn] through the full-screen gate of :core:ads (A-02, A-05).
 * onFinished(interShown) runs exactly once, on the main thread: at once when nothing shows, else after the ad
 * closes, fails to show, is found stuck or loses its activity. With no ads controller (tests, previews) it runs at
 * once with false.
 */
@Composable
fun rememberShowStartupAd(): (StartupAdTurn, (interShown: Boolean) -> Unit) -> Unit {
    val ads = LocalAdsController.current
    val activity = LocalActivity.current
    return remember(ads, activity) {
        { turn, onFinished ->
            if (ads != null && activity != null) {
                ads.showStartupAd(activity, turn, onFinished)
            } else {
                onFinished(false)
            }
        }
    }
}
