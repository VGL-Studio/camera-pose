package com.vglstudio.camera.pose.line.guide.core.ads

/** Retry waits of the PARTNER rules (docs/ops/ads-business-rules.md, F-05, F-06, O-05, section 11). */
internal object AdRetryPolicy {
    private const val FULL_SCREEN_STEP_MILLIS = 2_000L
    private const val FULL_SCREEN_MAX_MILLIS = 10_000L
    private const val NATIVE_FIRST_MILLIS = 2_000L
    private const val NATIVE_MAX_MILLIS = 60_000L
    private const val NATIVE_MAX_FAILURES = 10
    private const val NATIVE_NO_AD_FIRST_MILLIS = 5_000L
    private const val NATIVE_NO_AD_MAX_FAILURES = 5
    private const val RELOAD_AFTER_CLOSE_MILLIS = 2_000L

    /** F-05, B-02: min(n x 2 s, 10 s) for the n-th failure in a row; n below 1 counts as 1; never gives up. */
    fun fullScreenDelayMillis(failuresInARow: Int): Long {
        // Capped before multiplying, so a very large n cannot overflow.
        val steps = failuresInARow.coerceIn(1, (FULL_SCREEN_MAX_MILLIS / FULL_SCREEN_STEP_MILLIS).toInt())
        return steps * FULL_SCREEN_STEP_MILLIS
    }

    /**
     * O-05: holding an ad, 2 s doubled up to 60 s (n at most 10); holding none, 5, 10, 20, 40, then 60 s for every
     * failure from the fifth (plan T-103). n below 1 counts as 1; n is capped before the shift, so it cannot overflow.
     */
    fun nativeDelayMillis(failuresInARow: Int, holdsAd: Boolean): Long {
        if (!holdsAd) {
            val n = failuresInARow.coerceIn(1, NATIVE_NO_AD_MAX_FAILURES)
            return (NATIVE_NO_AD_FIRST_MILLIS shl (n - 1)).coerceAtMost(NATIVE_MAX_MILLIS)
        }
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

    /**
     * F-06 for the full-screen ads of MAX: 2 s after closing or failing to show, app open included (ADR-0045, section
     * 2.4, plan T-070); any other format is an [IllegalArgumentException].
     */
    fun maxReloadAfterCloseMillis(format: AdFormat): Long = when (format) {
        AdFormat.INTERSTITIAL, AdFormat.REWARDED, AdFormat.APP_OPEN -> RELOAD_AFTER_CLOSE_MILLIS
        else -> throw IllegalArgumentException("$format is not a MAX full-screen format")
    }
}
