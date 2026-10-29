package com.vglstudio.camera.pose.line.guide.core.ads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The order of [runBreakInterstitial] (plan T-074, section 4): the break limits first, then the gate of :core:ads
 * (the show), [InterstitialBreakGate.onShown] only for a shown ad, and the end only after the ad has closed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BreakInterstitialTest {
    private val placement = AdPlacement.REVIEW_INTERSTITIAL

    @Test
    fun shown_callsOnShown_thenFinishesTrueAfterClose() = runTest(UnconfinedTestDispatcher()) {
        val gate = RecordingBreakGate(allow = true)
        var close: () -> Unit = {}
        val result = async {
            runBreakInterstitial(placement, gate) { onClosed ->
                close = onClosed
                true
            }
        }
        assertEquals(1, gate.shown.size)
        assertEquals(placement, gate.shown.single().placement)
        // Control: the request is still on while the ad is on screen.
        assertFalse(result.isCompleted)
        close()
        assertTrue(result.await())
    }

    @Test
    fun notShown_finishesFalse_noOnShown() = runTest(UnconfinedTestDispatcher()) {
        val gate = RecordingBreakGate(allow = true)
        var shows = 0
        val shown = runBreakInterstitial(placement, gate) { onClosed ->
            shows++
            // :core:ads calls onFinished at once when it shows nothing.
            onClosed()
            false
        }
        assertFalse(shown)
        // Control: the gate allowed, so the show was asked for.
        assertEquals(1, shows)
        assertTrue(gate.shown.isEmpty())
    }

    @Test
    fun gateSaysNo_neverShows() = runTest(UnconfinedTestDispatcher()) {
        val gate = RecordingBreakGate(allow = false)
        var shows = 0
        val shown = runBreakInterstitial(placement, gate) {
            shows++
            true
        }
        assertFalse(shown)
        assertEquals(0, shows)
        assertEquals(listOf(placement), gate.breaks)
    }

    @Test
    fun noGate_showsWithoutLimit() = runTest(UnconfinedTestDispatcher()) {
        var shows = 0
        val shown = runBreakInterstitial(placement, gate = null) { onClosed ->
            shows++
            onClosed()
            true
        }
        assertTrue(shown)
        assertEquals(1, shows)
    }
}
