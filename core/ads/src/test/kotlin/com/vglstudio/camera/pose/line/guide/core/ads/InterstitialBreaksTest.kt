package com.vglstudio.camera.pose.line.guide.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The break limits of a full-screen placement and the consent, flag and network checks (spec camera, section 5). */
class InterstitialBreaksTest {
    private val placement = AdPlacement.REVIEW_INTERSTITIAL
    private val cap = BreakCap(firstBreak = 2, breaksBetween = 3, minIntervalSeconds = 120)
    private val breaks = InterstitialBreaks()

    @Test
    fun allowed_onlyWithConsentFlagAndNoNetworkDialog() {
        assertTrue(interstitialAllowed(canRequestAds = true, placementEnabled = true, networkDialogShown = false))
        assertFalse(interstitialAllowed(canRequestAds = false, placementEnabled = true, networkDialogShown = false))
        assertFalse(interstitialAllowed(canRequestAds = true, placementEnabled = false, networkDialogShown = false))
        assertFalse(interstitialAllowed(canRequestAds = true, placementEnabled = true, networkDialogShown = true))
    }

    @Test
    fun firstBreak_isSkipped_secondAllowed() {
        val first = breaks.onBreak(placement)
        assertEquals(1, first)
        assertFalse(breaks.allows(placement, first, 0, cap))
        val second = breaks.onBreak(placement)
        assertEquals(2, second)
        assertTrue(breaks.allows(placement, second, 0, cap))
    }

    @Test
    fun afterShown_nextAllowedThreeBreaksLater() {
        breaks.onShown(placement, breakNumber = 2, nowMillis = 0)
        val tenMinutes = 600_000L
        assertFalse(breaks.allows(placement, 3, tenMinutes, cap))
        assertFalse(breaks.allows(placement, 4, tenMinutes, cap))
        assertTrue(breaks.allows(placement, 5, tenMinutes, cap))
    }

    @Test
    fun afterShown_waitsMinInterval() {
        breaks.onShown(placement, breakNumber = 2, nowMillis = 0)
        assertFalse(breaks.allows(placement, 5, 119_999, cap))
        assertTrue(breaks.allows(placement, 5, 120_000, cap))
    }

    @Test
    fun placements_countSeparately() {
        assertEquals(1, breaks.onBreak(AdPlacement.REVIEW_INTERSTITIAL))
        assertEquals(1, breaks.onBreak(AdPlacement.SAMPLE_BOTTOM_BANNER))
        assertEquals(2, breaks.onBreak(AdPlacement.REVIEW_INTERSTITIAL))
    }

    @Test
    fun noneCap_allowsEveryBreak() {
        (1..3).forEach { number ->
            val breakNumber = breaks.onBreak(placement)
            assertEquals(number, breakNumber)
            assertTrue(breaks.allows(placement, breakNumber, 0, BreakCap.NONE))
            breaks.onShown(placement, breakNumber, 0)
        }
    }
}
