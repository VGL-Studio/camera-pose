package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The turn of the anchored ads of one screen (MC-12, KB-35 criterion 5, plan T-091): the first allowed member in
 * order holds it until it leaves; a member whose first value is not known yet makes the later ones wait; a holder
 * switched off keeps the turn. The scope logs one warning per wait and one line per new holder.
 */
class AnchoredAdTurnTest {
    private val banner = AdPlacement.GALLERY_BANNER
    private val collapsible = AdPlacement.GALLERY_BANNER_COLLAPSIBLE
    private val bar = AdPlacement.POSES_BOTTOM

    @Test
    fun holder_firstAllowedInOrder() {
        val members = listOf(AnchoredAdMember(1, banner, true), AnchoredAdMember(2, collapsible, true))

        assertEquals(1, anchoredAdHolder(members, holder = null))
    }

    @Test
    fun holder_skipsNotAllowed() {
        val members = listOf(AnchoredAdMember(1, banner, false), AnchoredAdMember(2, collapsible, true))

        assertEquals(2, anchoredAdHolder(members, holder = null))
    }

    @Test
    fun holder_waitsForUnknownBefore() {
        val members = listOf(AnchoredAdMember(1, banner, null), AnchoredAdMember(2, collapsible, true))

        assertNull(anchoredAdHolder(members, holder = null))
        // Control: once the first one is known to be off, the second one takes the turn.
        assertEquals(
            2,
            anchoredAdHolder(
                listOf(AnchoredAdMember(1, banner, false), AnchoredAdMember(2, collapsible, true)),
                holder = null,
            ),
        )
    }

    @Test
    fun holder_keptWhileInMembers_evenWhenOff() {
        val members = listOf(AnchoredAdMember(1, banner, false), AnchoredAdMember(2, collapsible, true))

        assertEquals(1, anchoredAdHolder(members, holder = 1))
    }

    @Test
    fun holder_gone_nextAllowedTakes() {
        val members = listOf(AnchoredAdMember(2, collapsible, true), AnchoredAdMember(3, bar, true))

        assertEquals(2, anchoredAdHolder(members, holder = 1))
    }

    @Test
    fun holder_none_firstThatTurnsOnTakes() {
        assertNull(
            anchoredAdHolder(
                listOf(AnchoredAdMember(1, banner, false), AnchoredAdMember(2, collapsible, false)),
                holder = null,
            ),
        )

        assertEquals(
            2,
            anchoredAdHolder(
                listOf(AnchoredAdMember(1, banner, false), AnchoredAdMember(2, collapsible, true)),
                holder = null,
            ),
        )
    }

    @Test
    fun isAnchoredFormat_bannersOnly() {
        assertTrue(isAnchoredFormat(AdFormat.BANNER))
        assertTrue(isAnchoredFormat(AdFormat.BANNER_COLLAPSIBLE))
        assertFalse(isAnchoredFormat(AdFormat.BANNER_MREC))
        assertFalse(isAnchoredFormat(AdFormat.NATIVE_SMALL))
    }

    @Test
    fun logLines_namePlacements() {
        assertEquals(
            "Anchored ad gallery_banner_collapsible waits: gallery_banner holds the anchored ad of this screen",
            anchoredAdWaitLog(collapsible, banner),
        )
        assertEquals("Anchored ad gallery_banner_collapsible takes the turn", anchoredAdTurnLog(collapsible))
    }

    @Test
    fun scope_waitingLogsOnce() {
        val logger = RecordingAppLogger()
        val scope = AnchoredAdScope(logger)
        val first = scope.join(banner)
        val second = scope.join(collapsible)

        scope.report(first, true)
        scope.report(second, true)
        scope.report(first, true)
        scope.report(second, true)

        assertEquals(first, scope.holder)
        val warnings = logger.entries.filter { it.level == LogLevel.WARN }
        assertEquals(1, warnings.size)
        assertEquals("Ads", warnings.single().tag)
        assertEquals(anchoredAdWaitLog(collapsible, banner), warnings.single().message)

        scope.leave(first)

        assertEquals(second, scope.holder)
        val turns = logger.entries.filter {
            it.level == LogLevel.INFO && it.message == anchoredAdTurnLog(collapsible)
        }
        assertEquals(1, turns.size)
        assertEquals("Ads", turns.single().tag)
        // Control: no new warning once the waiting member holds the turn.
        assertEquals(1, logger.entries.count { it.level == LogLevel.WARN })
    }
}
