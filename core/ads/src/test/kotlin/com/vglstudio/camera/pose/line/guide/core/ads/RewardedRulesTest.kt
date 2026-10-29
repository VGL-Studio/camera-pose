package com.vglstudio.camera.pose.line.guide.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** The pure rules of rewarded ads (MC-05, R-03 to R-05, L-072; plan T-031). */
class RewardedRulesTest {
    private val rwGetItem = AdPlacementDeclaration("rw_get_item", AdFormat.REWARDED)
    private val rwExport = AdPlacementDeclaration(
        "rw_export",
        AdFormat.REWARDED,
        noAdPolicy = RewardedNoAdPolicy.AllowAfter(2),
    )

    @Test
    fun noAdReason_table() {
        // (consentOpen, ready, offline, lastLoadNetworkError) -> reason.
        assertEquals(RewardedNoAdReason.CONSENT_CLOSED, noAdReason(false, true, false, false))
        assertEquals(RewardedNoAdReason.CONSENT_CLOSED, noAdReason(false, false, true, true))
        assertNull(noAdReason(true, true, true, true))
        assertEquals(RewardedNoAdReason.NETWORK, noAdReason(true, false, true, false))
        assertEquals(RewardedNoAdReason.NETWORK, noAdReason(true, false, false, true))
        assertEquals(RewardedNoAdReason.NOT_READY, noAdReason(true, false, false, false))
    }

    @Test
    fun isNetworkLoadError_code2_orAnyCodeOffline() {
        assertTrue(isNetworkLoadError(2, offline = false))
        assertTrue(isNetworkLoadError(3, offline = true))
        assertTrue(isNetworkLoadError(0, offline = true))
        assertFalse(isNetworkLoadError(3, offline = false))
        assertFalse(isNetworkLoadError(0, offline = false))
    }

    @Test
    fun grantsWithoutAd_byPolicy() {
        assertFalse(grantsWithoutAd(RewardedNoAdPolicy.Block, 1))
        assertFalse(grantsWithoutAd(RewardedNoAdPolicy.Block, 10))
        assertTrue(grantsWithoutAd(RewardedNoAdPolicy.Allow, 1))
        // Question 3: the n-th try in a row with no ad runs the action.
        assertFalse(grantsWithoutAd(RewardedNoAdPolicy.AllowAfter(2), 1))
        assertTrue(grantsWithoutAd(RewardedNoAdPolicy.AllowAfter(2), 2))
        assertTrue(grantsWithoutAd(RewardedNoAdPolicy.AllowAfter(2), 3))
        assertTrue(grantsWithoutAd(RewardedNoAdPolicy.AllowAfter(1), 1))
    }

    @Test
    fun rewarded_pushesNoIntervalMark() {
        // Guard (I-02, R-05): kept when REWARDED gets a full-screen slot.
        assertFalse(rwGetItem.pushesIntervalMark())
        assertFalse(rwExport.pushesIntervalMark())
        // Control: an interstitial pushes its mark.
        assertTrue(AdPlacementDeclaration("x", AdFormat.INTERSTITIAL).pushesIntervalMark())
    }

    @Test
    fun loadStep_table() {
        // (needsLoad, enabled, mayRequest) -> step.
        assertEquals(RewardedLoadStep.NOT_NEEDED, rewardedLoadStep(false, true, true))
        assertEquals(RewardedLoadStep.SWITCHED_OFF, rewardedLoadStep(true, false, true))
        assertEquals(RewardedLoadStep.SWITCHED_OFF, rewardedLoadStep(true, false, false))
        assertEquals(RewardedLoadStep.GATE_CLOSED, rewardedLoadStep(true, true, false))
        assertEquals(RewardedLoadStep.SEND, rewardedLoadStep(true, true, true))
    }

    @Test
    fun loaderOf_rewardedIsRewarded() {
        assertEquals(FullScreenLoader.REWARDED, fullScreenLoaderOf(AdFormat.REWARDED))
        // Control: the other full-screen slots keep their own loader.
        assertEquals(FullScreenLoader.INTERSTITIAL, fullScreenLoaderOf(AdFormat.INTERSTITIAL))
        assertEquals(FullScreenLoader.NATIVE_FULL_SCREEN, fullScreenLoaderOf(AdFormat.NATIVE_FULL_SCREEN))
        assertEquals(FullScreenLoader.APP_OPEN, fullScreenLoaderOf(AdFormat.APP_OPEN))
        assertThrows(IllegalArgumentException::class.java) { fullScreenLoaderOf(AdFormat.BANNER) }
        // Every format with a full-screen slot has a loader.
        AdFormat.entries.filter { it.hasFullScreenSlot }.forEach { fullScreenLoaderOf(it) }
    }

    @Test
    fun policyCode_roundTrip() {
        assertEquals(RewardedNoAdPolicy.Block, RewardedNoAdPolicy.parse("block"))
        assertEquals(RewardedNoAdPolicy.Allow, RewardedNoAdPolicy.parse("allow"))
        assertEquals(RewardedNoAdPolicy.AllowAfter(3), RewardedNoAdPolicy.parse("allow_after_3"))
        listOf("block", "allow", "allow_after_3").forEach {
            assertEquals(it, RewardedNoAdPolicy.parse(it)?.code)
        }
        listOf("allow_after_0", "allow_after_x", "allow_after_", "", "BLOCK").forEach {
            assertNull(it, RewardedNoAdPolicy.parse(it))
        }
    }

    @Test
    fun rewarded_hasFullScreenSlot_butIsNotFullScreen() {
        assertTrue(AdFormat.REWARDED.hasFullScreenSlot)
        assertFalse(AdFormat.REWARDED.isFullScreen)
        // Control.
        assertFalse(AdFormat.BANNER.hasFullScreenSlot)
        assertFalse(AdFormat.BANNER.isFullScreen)
        assertTrue(AdFormat.APP_OPEN.hasFullScreenSlot)
        assertFalse(AdFormat.APP_OPEN.isFullScreen)
    }
}
