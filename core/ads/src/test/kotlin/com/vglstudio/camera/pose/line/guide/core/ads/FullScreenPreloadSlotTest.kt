package com.vglstudio.camera.pose.line.guide.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A full-screen placement in the preload plan (S-22, F-04). */
class FullScreenPreloadSlotTest {
    private val loads = mutableListOf<AdPlacement>()
    private var enabled = true
    private var state: FullScreenLoadState = FullScreenLoadState.Idle

    private val slot = FullScreenPreloadSlot(
        placement = AdPlacement.GALLERY_INTERSTITIAL,
        enabled = { enabled },
        state = { state },
        load = { loads += it },
    )

    @Test
    fun disabled_neverLoads_andIsSettled() {
        enabled = false
        state = FullScreenLoadState.Loading

        slot.loadIfNeeded()

        assertEquals(emptyList<AdPlacement>(), loads)
        assertTrue(slot.settled())
        assertEquals(AdFormat.INTERSTITIAL, slot.format)

        // Control (L-055): the same loading state with the placement on is not settled.
        enabled = true
        assertFalse(slot.settled())
    }

    @Test
    fun enabled_loadsAndSettlesByState() {
        slot.loadIfNeeded()
        assertEquals(listOf(AdPlacement.GALLERY_INTERSTITIAL), loads)

        state = FullScreenLoadState.Loading
        assertFalse(slot.settled())
        state = FullScreenLoadState.Ready
        assertTrue(slot.settled())
        state = FullScreenLoadState.WaitingRetry(dueAtMillis = 2_000)
        assertTrue(slot.settled())
        state = FullScreenLoadState.Idle
        assertTrue(slot.settled())
    }

    @Test
    fun rewardedSlot_loadsThroughLoadFullScreenAdIfNeeded() {
        var rewardedOn = true
        val rewardedLoads = mutableListOf<AdPlacement>()
        val rewarded = FullScreenPreloadSlot(
            placement = AdPlacement.GALLERY_RW_ACTION,
            enabled = { rewardedOn },
            state = { FullScreenLoadState.Idle },
            // In AdsController this is loadFullScreenAdIfNeeded, which reaches RewardedTurns.loadIfNeeded.
            load = { rewardedLoads += it },
        )

        assertEquals(AdFormat.REWARDED, rewarded.format)
        rewarded.loadIfNeeded()
        assertEquals(listOf(AdPlacement.GALLERY_RW_ACTION), rewardedLoads)

        // Control: switched off, nothing is loaded.
        rewardedOn = false
        rewarded.loadIfNeeded()
        assertEquals(listOf(AdPlacement.GALLERY_RW_ACTION), rewardedLoads)
    }
}
