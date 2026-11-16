package com.vglstudio.camera.pose.line.guide.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Retry waits of F-05, F-06 and O-05 at their bounds (plan T-021, section 4). */
class AdRetryPolicyTest {
    @Test
    fun fullScreenDelay_growsBy2sUpTo10s() {
        val expected = mapOf(
            1 to 2_000L,
            2 to 4_000L,
            3 to 6_000L,
            4 to 8_000L,
            5 to 10_000L,
            6 to 10_000L,
            100 to 10_000L,
            Int.MAX_VALUE to 10_000L,
            0 to 2_000L,
            -1 to 2_000L,
        )
        expected.forEach { (n, millis) ->
            assertEquals("n = $n", millis, AdRetryPolicy.fullScreenDelayMillis(n))
        }
    }

    @Test
    fun nativeDelay_holdingAd_doublesUpTo60s() {
        val expected = mapOf(
            1 to 2_000L,
            2 to 4_000L,
            3 to 8_000L,
            4 to 16_000L,
            5 to 32_000L,
            6 to 60_000L,
            10 to 60_000L,
            11 to 60_000L,
            Int.MAX_VALUE to 60_000L,
        )
        expected.forEach { (n, millis) ->
            assertEquals("n = $n", millis, AdRetryPolicy.nativeDelayMillis(n, holdsAd = true))
        }
    }

    @Test
    fun nativeDelay_withoutAd_5_10_20_40_then60s() {
        // O-05 (plan T-103): 5, 10, 20, 40 s, then 60 s; n below 1 counts as 1, a very large n does not overflow.
        val expected = listOf(
            0 to 5_000L,
            1 to 5_000L,
            2 to 10_000L,
            3 to 20_000L,
            4 to 40_000L,
            5 to 60_000L,
            6 to 60_000L,
            100 to 60_000L,
            Int.MAX_VALUE to 60_000L,
        )
        expected.forEach { (n, millis) ->
            assertEquals("n = $n", millis, AdRetryPolicy.nativeDelayMillis(n, holdsAd = false))
        }
        // Control (L-055): the same n holding an ad takes the other branch.
        assertEquals(2_000L, AdRetryPolicy.nativeDelayMillis(1, holdsAd = true))
    }

    @Test
    fun reloadAfterClose() {
        assertEquals(2_000L, AdRetryPolicy.reloadAfterCloseMillis(AdFormat.INTERSTITIAL))
        assertEquals(2_000L, AdRetryPolicy.reloadAfterCloseMillis(AdFormat.REWARDED))
        assertEquals(0L, AdRetryPolicy.reloadAfterCloseMillis(AdFormat.APP_OPEN))
        assertEquals(0L, AdRetryPolicy.reloadAfterCloseMillis(AdFormat.NATIVE_FULL_SCREEN))
        val error = runCatching { AdRetryPolicy.reloadAfterCloseMillis(AdFormat.NATIVE_SMALL) }.exceptionOrNull()
        assertTrue("expected IllegalArgumentException, got $error", error is IllegalArgumentException)
    }

    @Test
    fun maxReloadAfterClose_twoSecondsForEveryMaxFullScreenFormat() {
        // ADR-0045, section 2.4 (plan T-070): every full-screen format of MAX reloads 2 s after closing, app open too.
        assertEquals(2_000L, AdRetryPolicy.maxReloadAfterCloseMillis(AdFormat.INTERSTITIAL))
        assertEquals(2_000L, AdRetryPolicy.maxReloadAfterCloseMillis(AdFormat.REWARDED))
        assertEquals(2_000L, AdRetryPolicy.maxReloadAfterCloseMillis(AdFormat.APP_OPEN))
        val error = runCatching { AdRetryPolicy.maxReloadAfterCloseMillis(AdFormat.BANNER) }.exceptionOrNull()
        assertTrue("expected IllegalArgumentException, got $error", error is IllegalArgumentException)
        // Control: the app open ad of AdMob still reloads at once (F-06).
        assertEquals(0L, AdRetryPolicy.reloadAfterCloseMillis(AdFormat.APP_OPEN))
    }
}
