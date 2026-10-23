package com.vglstudio.camera.pose.line.guide.core.ads

/**
 * Limits of one placement counted in natural breaks (plan T-018): the first break that may show an ad, the number
 * of breaks between two ads, and the seconds between them.
 */
internal data class BreakCap(val firstBreak: Int, val breaksBetween: Int, val minIntervalSeconds: Long) {
    companion object {
        /** No limit besides the shared [FullScreenAdGuard]. */
        val NONE = BreakCap(firstBreak = 1, breaksBetween = 1, minIntervalSeconds = 0)
    }
}

/**
 * Counts the natural breaks of the session for each placement and remembers the last ad shown, in memory only.
 * Free of Android, so it runs in unit tests. Times are milliseconds of a monotonic clock. Main thread only.
 */
internal class InterstitialBreaks {
    private class State {
        var breaks = 0
        var lastShownBreak: Int? = null
        var lastShownAt = 0L
    }

    private val states = mutableMapOf<AdPlacement, State>()

    private fun stateOf(placement: AdPlacement) = states.getOrPut(placement) { State() }

    /** Counts one break of [placement], shown or not, and returns its number (the first is 1). */
    fun onBreak(placement: AdPlacement): Int = ++stateOf(placement).breaks

    fun allows(placement: AdPlacement, breakNumber: Int, nowMillis: Long, cap: BreakCap): Boolean {
        if (breakNumber < cap.firstBreak) return false
        val state = stateOf(placement)
        val lastBreak = state.lastShownBreak ?: return true
        return breakNumber - lastBreak >= cap.breaksBetween &&
            nowMillis - state.lastShownAt >= cap.minIntervalSeconds * MILLIS_PER_SECOND
    }

    fun onShown(placement: AdPlacement, breakNumber: Int, nowMillis: Long) {
        stateOf(placement).apply {
            lastShownBreak = breakNumber
            lastShownAt = nowMillis
        }
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
    }
}

/** Consent given, placement switched on remotely and no network dialog on the screen. */
internal fun interstitialAllowed(canRequestAds: Boolean, placementEnabled: Boolean, networkDialogShown: Boolean) =
    canRequestAds && placementEnabled && !networkDialogShown
