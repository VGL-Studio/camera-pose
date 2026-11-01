package com.vglstudio.camera.pose.line.guide.core.ads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The pure rules of the native full-screen ad after an interstitial (K-02 to K-08, plan T-026), in virtual time. */
@OptIn(ExperimentalCoroutinesApi::class)
class NativeAfterInterRulesTest {
    @Test
    fun countdownSecondsLeft_roundsUp_andStopsAtZero() {
        val deadline = 5_000L
        val expected = mapOf(0L to 5, 1L to 5, 1_000L to 4, 4_001L to 1, 4_999L to 1, 5_000L to 0, 9_000L to 0)
        expected.forEach { (now, seconds) ->
            assertEquals("now = $now", seconds, countdownSecondsLeft(deadline, now, countdownSeconds = 5L))
        }
        // The upper bound: a clock read before the opening never shows more than the countdown of the turn.
        assertEquals(5, countdownSecondsLeft(deadline, -500L, countdownSeconds = 5L))
        // The longest countdown (30 s).
        assertEquals(30, countdownSecondsLeft(30_000L, 0L, countdownSeconds = 30L))
        assertEquals(1, countdownSecondsLeft(30_000L, 29_001L, countdownSeconds = 30L))
    }

    @Test
    fun nativeCloseCountdownSeconds_acceptsOneToThirty_elseFive() {
        mapOf(1L to 1L, 5L to 5L, 12L to 12L, 30L to 30L).forEach { (remote, expected) ->
            assertEquals("remote = $remote", expected, nativeCloseCountdownSeconds(remote))
        }
        listOf(0L, -1L, 31L, Long.MAX_VALUE, Long.MIN_VALUE).forEach { remote ->
            assertEquals("remote = $remote", 5L, nativeCloseCountdownSeconds(remote))
        }
    }

    @Test
    fun defaultCountdown_isFive_matchesKeyDefault() {
        assertEquals(5L, NATIVE_CLOSE_COUNTDOWN_DEFAULT_SECONDS)
        assertEquals(NATIVE_CLOSE_COUNTDOWN_DEFAULT_SECONDS, AdsSettings.TIME_CLOSE_NATIVE.default)
    }

    @Test
    fun awaitInterGone_returnsAtOnce_whenAlreadyGone() = runTest {
        var asks = 0
        val gone = awaitInterGone {
            asks++
            true
        }
        assertTrue(gone)
        assertEquals(0L, currentTime)
        assertEquals(1, asks)
    }

    @Test
    fun awaitInterGone_polls200ms_untilGone() = runTest {
        var asks = 0
        val gone = awaitInterGone {
            asks++
            currentTime >= 600L
        }
        assertTrue(gone)
        assertEquals(600L, currentTime)
        // Asked at 0, 200, 400 and 600.
        assertEquals(4, asks)
    }

    @Test
    fun awaitInterGone_givesUpAt2s() = runTest {
        val gone = awaitInterGone { false }
        assertFalse(gone)
        assertEquals(2_000L, currentTime)
    }

    @Test
    fun closeEnabled_afterCountdownOrOnFailure() {
        val table = listOf(
            Triple(3, false, false),
            Triple(1, false, false),
            Triple(0, false, true),
            Triple(3, true, true),
        )
        table.forEach { (secondsLeft, loadFailed, enabled) ->
            val view = NativeFullScreenView(ad = null, secondsLeft = secondsLeft, loadFailed = loadFailed)
            assertEquals("closeEnabled ($secondsLeft, $loadFailed)", enabled, view.closeEnabled)
            assertEquals("backCloses ($secondsLeft, $loadFailed)", enabled, view.backCloses)
        }
    }

    @Test
    fun comboOpportunity_shownIsReady_elseNotReadyWithStateAtOpen() {
        assertEquals("shown" to "ready", comboOpportunity(everShown = true, loadStateAtOpen = "idle"))
        assertEquals("not_ready" to "loading", comboOpportunity(everShown = false, loadStateAtOpen = "loading"))
    }

    @Test
    fun loadResults_twoOverlappingLoads_wakeOnlyOwnWaiter() = runTest {
        val results = NativeLoadResults()
        val combo = AdPlacement.NATIVE_AFTER_INTER
        val gallery = AdPlacement.GALLERY_NATIVE_FULL_SCREEN
        val comboWait = async { results.await(combo, results.generation(combo)) }
        val galleryWait = async { results.await(gallery, results.generation(gallery)) }
        runCurrent()
        assertFalse(comboWait.isCompleted)
        assertFalse(galleryWait.isCompleted)

        results.signal(gallery)
        runCurrent()
        assertTrue(galleryWait.isCompleted)
        assertFalse("a result of another placement woke the combo", comboWait.isCompleted)

        results.signal(combo)
        runCurrent()
        assertTrue(comboWait.isCompleted)
    }

    @Test
    fun loadResults_signalBeforeAwait_isNotMissed() = runTest {
        val results = NativeLoadResults()
        val combo = AdPlacement.NATIVE_AFTER_INTER
        val since = results.generation(combo)
        results.signal(combo)
        val wait = async { results.await(combo, since) }
        runCurrent()
        assertTrue(wait.isCompleted)
        // Control: a wait from the new generation does not return until the next signal.
        val next = async { results.await(combo, results.generation(combo)) }
        advanceTimeBy(10_000L)
        runCurrent()
        assertFalse(next.isCompleted)
        next.cancel()
    }
}
