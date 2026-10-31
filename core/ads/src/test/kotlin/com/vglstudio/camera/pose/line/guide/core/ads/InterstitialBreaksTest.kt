package com.vglstudio.camera.pose.line.guide.core.ads

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The break limits of review_interstitial (spec camera, section 5; spec ads, questions 1 and 2): the first break that
 * may show an ad and the breaks between two ads, counted in memory, read from two remote keys. No time rule: the only
 * time gate is `ads_interval` of :core:ads (plan T-074, section 3.1).
 */
class InterstitialBreaksTest {
    private val placement = AdPlacement.REVIEW_INTERSTITIAL
    private val cap = BreakCap(firstBreak = 2, breaksBetween = 3)
    private val breaks = InterstitialBreaks()

    private val settings = FakeRemoteSettings()
    private val gate = DefaultInterstitialBreakGate(settings)

    @Test
    fun firstBreak_isSkipped_secondAllowed() {
        val first = breaks.onBreak(placement)
        assertEquals(1, first)
        assertFalse(breaks.allows(placement, first, cap))
        val second = breaks.onBreak(placement)
        assertEquals(2, second)
        assertTrue(breaks.allows(placement, second, cap))
    }

    @Test
    fun afterShown_nextAllowedThreeBreaksLater() {
        breaks.onShown(placement, breakNumber = 2)
        assertFalse(breaks.allows(placement, 3, cap))
        assertFalse(breaks.allows(placement, 4, cap))
        assertTrue(breaks.allows(placement, 5, cap))
    }

    @Test
    fun placements_countSeparately() {
        assertEquals(1, breaks.onBreak(AdPlacement.REVIEW_INTERSTITIAL))
        assertEquals(1, breaks.onBreak(AdPlacement.POSES_BOTTOM))
        assertEquals(2, breaks.onBreak(AdPlacement.REVIEW_INTERSTITIAL))
    }

    @Test
    fun noneCap_allowsEveryBreak() {
        (1..3).forEach { number ->
            val breakNumber = breaks.onBreak(placement)
            assertEquals(number, breakNumber)
            assertTrue(breaks.allows(placement, breakNumber, BreakCap.NONE))
            breaks.onShown(placement, breakNumber)
        }
    }

    @Test
    fun gate_readsBothKeys_firstBreakSkipped_secondAllowed() = runTest {
        settings.set(InterstitialBreakSettings.REVIEW_FIRST_BREAK, 2L)
        settings.set(InterstitialBreakSettings.REVIEW_BREAKS_BETWEEN, 3L)
        assertNull(gate.onBreak(placement))
        val second = ticketOf(gate.onBreak(placement))
        assertEquals(2, second.breakNumber)
        assertEquals(placement, second.placement)
        gate.onShown(second)
        assertNull(gate.onBreak(placement))
        assertNull(gate.onBreak(placement))
        assertEquals(5, ticketOf(gate.onBreak(placement)).breakNumber)
        assertTrue(
            settings.readNames.toString(),
            settings.readNames.containsAll(
                setOf("ads_review_interstitial_first_break", "ads_review_interstitial_breaks_between"),
            ),
        )
    }

    @Test
    fun gate_firstBreakOne_firstBreakHasTicket() = runTest {
        // Control of the test above: the first break comes from the key, not from a fixed number.
        settings.set(InterstitialBreakSettings.REVIEW_FIRST_BREAK, 1L)
        assertEquals(1, ticketOf(gate.onBreak(placement)).breakNumber)
    }

    @Test
    fun settings_defaultsAreTwoAndThree() {
        assertEquals(2L, InterstitialBreakSettings.REVIEW_FIRST_BREAK.default)
        assertEquals(3L, InterstitialBreakSettings.REVIEW_BREAKS_BETWEEN.default)
        assertEquals("ads_review_interstitial_first_break", InterstitialBreakSettings.REVIEW_FIRST_BREAK.name)
        assertEquals("ads_review_interstitial_breaks_between", InterstitialBreakSettings.REVIEW_BREAKS_BETWEEN.name)
        assertEquals(
            InterstitialBreakSettings.REVIEW_FIRST_BREAK to InterstitialBreakSettings.REVIEW_BREAKS_BETWEEN,
            InterstitialBreakSettings.capOf(AdPlacement.REVIEW_INTERSTITIAL),
        )
        assertEquals(
            listOf(InterstitialBreakSettings.REVIEW_FIRST_BREAK, InterstitialBreakSettings.REVIEW_BREAKS_BETWEEN),
            InterstitialBreakSettings.all(),
        )
        // Control: a placement without limits has no keys.
        assertNull(InterstitialBreakSettings.capOf(AdPlacement.POSES_BOTTOM))
    }

    @Test
    fun gate_defaults_skipFirst_allowSecond() = runTest {
        // No remote value: the defaults 2 and 3 apply.
        assertNull(gate.onBreak(placement))
        assertNotNull(gate.onBreak(placement))
    }

    @Test
    fun noTimeRule_secondAllowedRightAfterFirstShown() = runTest {
        // Shown at break 2; break 5 is allowed at once, with no clock moving (the 120 s rule is gone).
        assertNull(gate.onBreak(placement))
        gate.onShown(ticketOf(gate.onBreak(placement)))
        assertNull(gate.onBreak(placement))
        assertNull(gate.onBreak(placement))
        assertNotNull(gate.onBreak(placement))
    }

    @Test
    fun onShown_movesTheLastShownBreak() = runTest {
        settings.set(InterstitialBreakSettings.REVIEW_FIRST_BREAK, 1L)
        val first = ticketOf(gate.onBreak(placement))
        // Control: without onShown every break after the first is allowed.
        assertNotNull(gate.onBreak(placement))
        gate.onShown(first)
        // Break 3 is two after the shown break 1: not allowed (3 between).
        assertNull(gate.onBreak(placement))
        assertNotNull(gate.onBreak(placement))
    }

    @Test
    fun gate_placementWithoutCap_allowsEveryBreak() = runTest {
        val other = AdPlacement.GALLERY_INTERSTITIAL
        repeat(3) { index ->
            val ticket = ticketOf(gate.onBreak(other))
            assertEquals(index + 1, ticket.breakNumber)
            gate.onShown(ticket)
        }
    }

    /** The ticket, failing the test when the break got none. */
    private fun ticketOf(ticket: BreakTicket?): BreakTicket {
        assertNotNull("expected a ticket", ticket)
        return ticket!!
    }
}
