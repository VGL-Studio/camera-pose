package com.vglstudio.camera.pose.line.guide.core.ads

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether a full-screen ad (interstitial or native full screen) is on screen. The network popup (ADR-0024, T-017)
 * waits for it to close; a second full-screen ad is dropped while it is true (F-01). The flag is lowered by the real
 * callbacks, stuck recovery and forced completion; after [SHOWING_CAP_MILLIS] a new turn may start anyway (F-02).
 *
 * [showing] is the published flag, and [onPublished] gets each new value it takes; the in-screen ads read it (KB-27,
 * plan T-054). Inside [publishAfter] the inner state ([isBusy], [current], [showingNow]) moves at once while the
 * published flag waits for the end of the block, so an interstitial handing over to its native ad shows no gap.
 * Main thread only.
 */
internal class FullScreenAdPresence(private val onPublished: (Boolean) -> Unit = {}) {
    private val _showing = MutableStateFlow(false)
    private var startedAt = 0L

    /** The inner state: true while an ad is on screen, even inside a [publishAfter] block not yet published. */
    private var inner = false

    /** Depth of [publishAfter] blocks; only the outermost one publishes. */
    private var holding = 0

    val showing: StateFlow<Boolean> = _showing.asStateFlow()

    /** The inner state, which moves at once inside [publishAfter] too (read by the consent round, plan T-054). */
    val showingNow: Boolean get() = inner

    /** The ad on screen, or null. */
    var current: Shown? = null
        private set

    /** True while an ad shows and its cap of 10 minutes has not passed at [now]. */
    fun isBusy(now: Long): Boolean = inner && now - startedAt < SHOWING_CAP_MILLIS

    /** Starts a full-screen ad of [placement] on [hostActivityId]; false while another one shows (F-01, F-02). */
    fun tryBegin(placement: AdPlacement, hostActivityId: Int, now: Long): Boolean {
        if (isBusy(now)) return false
        begin(Shown(placement, hostActivityId), now)
        return true
    }

    /**
     * One full-screen ad on screen: its placement and the identity hash of the activity that showed it. [awaitingHost]
     * is true while its host was destroyed for a configuration change and the new activity is not resumed yet (plan
     * T-055); it lives in the turn, so every close and every new turn clears it.
     */
    data class Shown(val placement: AdPlacement, val hostActivityId: Int, val awaitingHost: Boolean = false)

    /**
     * The host of the ad on screen is recreated for a configuration change (KB-26, criterion 4): the turn waits for
     * the next resumed activity of the app. Does nothing when no ad shows.
     */
    fun handOver() {
        current = current?.copy(awaitingHost = true)
    }

    /**
     * Gives the waiting turn to [activityId], the activity resumed now, and returns true; false, changing nothing,
     * when no turn waits. The start of the turn stays, so the cap of F-02 counts from the real show.
     */
    fun rehostIfAwaiting(activityId: Int): Boolean {
        val shown = current
        if (shown == null || !shown.awaitingHost) return false
        current = shown.copy(hostActivityId = activityId, awaitingHost = false)
        return true
    }

    /** A full-screen ad is about to be shown, with no placement or host (kept for the tests of T-013). */
    fun onShowing() {
        begin(null, 0)
    }

    /** The full-screen ad was closed or failed to show. Calling it again changes nothing. */
    fun onClosed() {
        current = null
        inner = false
        publish()
    }

    /**
     * Runs [block], then publishes [showing] once with the last inner value, also when [block] throws (the error goes
     * on: a flag stuck at true would block every in-screen ad). A close then a begin inside the block publishes no
     * false. In nested blocks only the outermost end publishes.
     */
    fun <T> publishAfter(block: () -> T): T {
        holding++
        try {
            return block()
        } finally {
            holding--
            publish()
        }
    }

    private fun begin(shown: Shown?, now: Long) {
        current = shown
        startedAt = now
        inner = true
        publish()
    }

    private fun publish() {
        if (holding > 0 || _showing.value == inner) return
        _showing.value = inner
        onPublished(inner)
    }

    companion object {
        /** F-02: a user who tapped the ad may read in the browser that long. */
        const val SHOWING_CAP_MILLIS = 600_000L
    }
}
