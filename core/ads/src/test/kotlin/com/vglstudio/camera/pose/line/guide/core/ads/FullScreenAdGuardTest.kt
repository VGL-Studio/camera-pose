package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.ads.AdIntervalGroup.Companion.DEFAULT
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The `ads_interval` marks of full-screen ads (docs/ops/ads-business-rules.md, section 4): one mark per interval
 * group, written when an interstitial closes, in memory only, read with a monotonic clock (plan T-022).
 */
class FullScreenAdGuardTest {
    private val guard = FullScreenAdGuard()
    private val sounds =
        AdPlacementDeclaration("inter_sounds", AdFormat.INTERSTITIAL, AdIntervalGroup.named("sounds", 30))

    @Test
    fun newGuard_hasNoMark() {
        guard.record(DEFAULT, 10_000)
        assertFalse(guard.allows(DEFAULT, 45_000, 20_000))
        // A new process starts with a new guard, with no mark (I-03).
        assertTrue(FullScreenAdGuard().allows(DEFAULT, 45_000, 20_000))
    }

    @Test
    fun firstInterOfProcess_isAllowed() {
        assertTrue(guard.allows(DEFAULT, 45_000, 0))
        // Control (L-055): once a mark exists, the same check blocks.
        guard.record(DEFAULT, 0)
        assertFalse(guard.allows(DEFAULT, 45_000, 1))
    }

    @Test
    fun manyIntersSpacedByInterval_allAllowed() {
        var now = 0L
        repeat(10) { turn ->
            assertTrue("turn $turn", guard.allows(DEFAULT, 45_000, now))
            guard.record(DEFAULT, now)
            now += 45_000
        }
        // Control: the 11th turn, 1 ms too soon, is blocked.
        assertFalse(guard.allows(DEFAULT, 45_000, now - 45_000 + 44_999))
    }

    @Test
    fun interval_blocksUntilElapsed_boundaryPasses() {
        guard.record(DEFAULT, 0)
        assertFalse(guard.allows(DEFAULT, 45_000, 44_999))
        assertTrue(guard.allows(DEFAULT, 45_000, 45_000))
    }

    @Test
    fun markIsCloseTime_notShowTime() {
        // Shown at 0, closed at 30 000: only the close writes the mark (I-02, I-05).
        guard.record(DEFAULT, 30_000)
        assertFalse(guard.allows(DEFAULT, 45_000, 60_000))
        assertTrue(guard.allows(DEFAULT, 45_000, 75_000))
    }

    @Test
    fun pushesIntervalMark_onlyInterFamily() {
        assertTrue(inter.pushesIntervalMark())
        assertTrue(nfs.pushesIntervalMark())
        assertFalse(open.pushesIntervalMark())
        assertFalse(rw.pushesIntervalMark())
        assertFalse(inter.copy(recordInterval = false).pushesIntervalMark())
    }

    @Test
    fun customGroupInter_doesNotMoveDefaultMark() {
        guard.record(sounds.intervalGroup, 0)
        assertTrue(guard.allows(DEFAULT, 45_000, 1))
        assertFalse(guard.allows(sounds.intervalGroup, 30_000, 1))
    }

    @Test
    fun defaultInter_doesNotBlockCustomGroup() {
        guard.record(DEFAULT, 0)
        assertTrue(guard.allows(sounds.intervalGroup, 30_000, 1))
        assertFalse(guard.allows(DEFAULT, 45_000, 1))
    }

    @Test
    fun ignoreInterval_skipsTheGate() {
        assertTrue(inter.readsInterval())
        assertFalse(inter.copy(ignoreInterval = true).readsInterval())
    }

    @Test
    fun clockBehindMark_isTreatedAsPassed() {
        guard.record(DEFAULT, 100_000)
        assertTrue(guard.allows(DEFAULT, 45_000, 50_000))
        // Control: a clock just after the mark is blocked.
        assertFalse(guard.allows(DEFAULT, 45_000, 100_001))
    }

    @Test
    fun quietPeriodRule_isGone() {
        val guardMethods = FullScreenAdGuard::class.java.declaredMethods.map { it.name }
        // Control: the methods of the guard are read by name.
        assertTrue(guardMethods.contains("record"))
        assertFalse(guardMethods.contains("onUserInteraction"))
        assertFalse(AdsController::class.java.methods.any { it.name == "onUserInteraction" })
    }

    private companion object {
        val inter = AdPlacementDeclaration("inter_x", AdFormat.INTERSTITIAL)
        val open = AdPlacementDeclaration("open_x", AdFormat.APP_OPEN)
        val rw = AdPlacementDeclaration("rw_x", AdFormat.REWARDED)
        val nfs = AdPlacementDeclaration("nfs_x", AdFormat.NATIVE_FULL_SCREEN)
    }
}
