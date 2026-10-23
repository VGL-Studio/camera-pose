package com.vglstudio.camera.pose.line.guide.core.ads

/** Limits of one placement counted in natural breaks (plan T-018). */
internal data class BreakCap(val firstBreak: Int, val breaksBetween: Int, val minIntervalSeconds: Long) {
    companion object {
        /** No limit besides the shared guard. */
        val NONE = BreakCap(firstBreak = 1, breaksBetween = 1, minIntervalSeconds = 0)
    }
}

/** Counts the breaks of the session per placement. Pure Kotlin, main thread only. Stub: behavior comes with the code. */
internal class InterstitialBreaks {
    fun onBreak(placement: AdPlacement): Int = 0

    fun allows(placement: AdPlacement, breakNumber: Int, nowMillis: Long, cap: BreakCap): Boolean = false

    fun onShown(placement: AdPlacement, breakNumber: Int, nowMillis: Long) = Unit
}

internal fun interstitialAllowed(canRequestAds: Boolean, placementEnabled: Boolean, networkDialogShown: Boolean) = false
