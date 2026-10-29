package com.vglstudio.camera.pose.line.guide.core.ads

/** Retry waits of the PARTNER rules (docs/ops/ads-business-rules.md, F-05, F-06, O-05, section 11). */
internal object AdRetryPolicy {
    private const val FULL_SCREEN_STEP_MILLIS = 2_000L
    private const val FULL_SCREEN_MAX_MILLIS = 10_000L
    private const val NATIVE_FIRST_MILLIS = 2_000L
    private const val NATIVE_MAX_MILLIS = 60_000L
    private const val NATIVE_MAX_FAILURES = 10
    private const val NATIVE_NO_AD_MILLIS = 5_000L
    private const val RELOAD_AFTER_CLOSE_MILLIS = 2_000L

    /** F-05, B-02: min(n x 2 s, 10 s) for the n-th failure in a row; n below 1 counts as 1; never gives up. */
    fun fullScreenDelayMillis(failuresInARow: Int): Long {
        // Capped before multiplying, so a very large n cannot overflow.
        val steps = failuresInARow.coerceIn(1, (FULL_SCREEN_MAX_MILLIS / FULL_SCREEN_STEP_MILLIS).toInt())
        return steps * FULL_SCREEN_STEP_MILLIS
    }

    /** O-05: holding an ad, 2 s doubled up to 60 s (n at most 10); holding none, always 5 s. */
    fun nativeDelayMillis(failuresInARow: Int, holdsAd: Boolean): Long {
        if (!holdsAd) return NATIVE_NO_AD_MILLIS
        val n = failuresInARow.coerceIn(1, NATIVE_MAX_FAILURES)
        return (NATIVE_FIRST_MILLIS shl (n - 1)).coerceAtMost(NATIVE_MAX_MILLIS)
    }

    /**
     * F-06: interstitial and rewarded wait 2 s after closing or failing to show; app open and native full screen
     * reload at once (O-01). Any other format is an [IllegalArgumentException].
     */
    fun reloadAfterCloseMillis(format: AdFormat): Long = when (format) {
        AdFormat.INTERSTITIAL, AdFormat.REWARDED -> RELOAD_AFTER_CLOSE_MILLIS
        AdFormat.APP_OPEN, AdFormat.NATIVE_FULL_SCREEN -> 0L
        else -> throw IllegalArgumentException("$format is not a full-screen format")
    }
}
