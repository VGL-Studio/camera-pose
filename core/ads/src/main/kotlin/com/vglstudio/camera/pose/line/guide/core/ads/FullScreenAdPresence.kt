package com.vglstudio.camera.pose.line.guide.core.ads

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether a full-screen ad (interstitial or native full screen) is on screen. The network popup (ADR-0024, T-017)
 * waits for it to close; a second full-screen ad is dropped while it is true (F-01). The flag is lowered by the real
 * callbacks, stuck recovery and forced completion; after [SHOWING_CAP_MILLIS] a new turn may start anyway (F-02).
 * Main thread only.
 */
internal class FullScreenAdPresence {
    private val _showing = MutableStateFlow(false)
    private var startedAt = 0L

    val showing: StateFlow<Boolean> = _showing.asStateFlow()

    /** The ad on screen, or null. */
    var current: Shown? = null
        private set

    /** True while an ad shows and its cap of 10 minutes has not passed at [now]. */
    fun isBusy(now: Long): Boolean = _showing.value && now - startedAt < SHOWING_CAP_MILLIS

    /** Starts a full-screen ad of [placement] on [hostActivityId]; false while another one shows (F-01, F-02). */
    fun tryBegin(placement: AdPlacement, hostActivityId: Int, now: Long): Boolean {
        if (isBusy(now)) return false
        begin(Shown(placement, hostActivityId), now)
        return true
    }

    /** One full-screen ad on screen: its placement and the identity hash of the activity that showed it. */
    data class Shown(val placement: AdPlacement, val hostActivityId: Int)

    /** A full-screen ad is about to be shown, with no placement or host (kept for the tests of T-013). */
    fun onShowing() {
        begin(null, 0)
    }

    /** The full-screen ad was closed or failed to show. Calling it again changes nothing. */
    fun onClosed() {
        current = null
        _showing.value = false
    }

    private fun begin(shown: Shown?, now: Long) {
        current = shown
        startedAt = now
        _showing.value = true
    }

    companion object {
        /** F-02: a user who tapped the ad may read in the browser that long. */
        const val SHOWING_CAP_MILLIS = 600_000L
    }
}
