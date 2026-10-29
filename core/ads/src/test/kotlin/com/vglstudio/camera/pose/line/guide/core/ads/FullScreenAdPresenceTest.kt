package com.vglstudio.camera.pose.line.guide.core.ads

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Whether a full-screen ad is on screen; the network popup waits for it to close (ADR-0024, plan T-017). */
class FullScreenAdPresenceTest {
    @Test
    fun startsHidden() {
        // Control: a presence that was told an ad is showing reads true, so the flow is live.
        val shown = FullScreenAdPresence().apply { onShowing() }
        assertTrue(shown.showing.value)

        assertFalse(FullScreenAdPresence().showing.value)
    }

    @Test
    fun onShowing_setsTrue() {
        val presence = FullScreenAdPresence()
        presence.onShowing()
        assertTrue(presence.showing.value)
    }

    @Test
    fun onClosed_setsFalse() {
        val presence = FullScreenAdPresence()
        presence.onShowing()
        assertTrue(presence.showing.value)
        presence.onClosed()
        assertFalse(presence.showing.value)
    }

    @Test
    fun closedTwice_staysFalse() {
        val presence = FullScreenAdPresence()
        presence.onShowing()
        // Control: the ad is reported as showing before it is closed twice.
        assertTrue(presence.showing.value)
        presence.onClosed()
        presence.onClosed()
        assertFalse(presence.showing.value)
    }

    @Test
    fun showingFlow_emitsShowAndClose() = runTest {
        val presence = FullScreenAdPresence()
        presence.showing.test {
            assertEquals(false, awaitItem())
            presence.onShowing()
            assertEquals(true, awaitItem())
            presence.onClosed()
            assertEquals(false, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun secondShowWhileShowing_isDropped() {
        val presence = FullScreenAdPresence()
        assertTrue(presence.tryBegin(AdPlacement.GALLERY_INTERSTITIAL, hostActivityId = 1, now = 0))
        assertFalse(presence.tryBegin(AdPlacement.GALLERY_NATIVE_FULL_SCREEN, hostActivityId = 1, now = 1_000))
        presence.onClosed()
        assertTrue(presence.tryBegin(AdPlacement.GALLERY_NATIVE_FULL_SCREEN, hostActivityId = 1, now = 2_000))
    }

    @Test
    fun showingFlag_expiresAfterTenMinutes() {
        val presence = FullScreenAdPresence()
        assertTrue(presence.tryBegin(AdPlacement.GALLERY_INTERSTITIAL, hostActivityId = 1, now = 0))
        assertFalse(presence.tryBegin(AdPlacement.GALLERY_INTERSTITIAL, hostActivityId = 1, now = 599_999))
        assertTrue(presence.tryBegin(AdPlacement.GALLERY_INTERSTITIAL, hostActivityId = 2, now = 600_000))
        assertEquals(FullScreenAdPresence.Shown(AdPlacement.GALLERY_INTERSTITIAL, 2), presence.current)
    }

    @Test
    fun current_followsTheAdOnScreen() {
        val presence = FullScreenAdPresence()
        presence.tryBegin(AdPlacement.GALLERY_INTERSTITIAL, hostActivityId = 7, now = 0)
        assertEquals(FullScreenAdPresence.Shown(AdPlacement.GALLERY_INTERSTITIAL, 7), presence.current)
        assertTrue(presence.showing.value)
        presence.onClosed()
        assertEquals(null, presence.current)
        assertFalse(presence.showing.value)
    }
}
