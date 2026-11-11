package com.vglstudio.camera.pose.line.guide.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The default preload plan and the checks of a plan (S-22, HD-02). */
class AdPreloadPlanTest {
    @Test
    fun default_putsAppOpenAndFirstStepFirst() {
        val plan = defaultPreloadPlan(
            listOf(AdFormat.INTERSTITIAL, AdFormat.APP_OPEN, AdFormat.NATIVE_SMALL, AdFormat.NATIVE_STEP),
        )

        assertEquals(
            listOf(
                setOf(AdFormat.APP_OPEN, AdFormat.NATIVE_STEP),
                setOf(AdFormat.INTERSTITIAL, AdFormat.NATIVE_SMALL),
            ),
            plan.groups,
        )
    }

    @Test
    fun default_withoutAppOpenOrStep_isOneGroup() {
        val plan = defaultPreloadPlan(listOf(AdFormat.INTERSTITIAL, AdFormat.NATIVE_FULL_SCREEN, AdFormat.INTERSTITIAL))

        assertEquals(listOf(setOf(AdFormat.INTERSTITIAL, AdFormat.NATIVE_FULL_SCREEN)), plan.groups)
    }

    @Test
    fun fromPlacements_coversEveryTemplateFormat() {
        val plan = AdPreloadPlan.fromPlacements()

        // The formats of the placements, and the collapsible layer of their bottom bars (KB-33).
        assertEquals(
            AdPlacement.entries.map { it.format }.toSet() + AdFormat.NATIVE_COLLAPSIBLE,
            plan.groups.flatten().toSet(),
        )
        assertEquals(plan.groups.sumOf { it.size }, plan.groups.flatten().toSet().size)
    }

    @Test
    fun fromPlacements_hasRewardedInSecondGroup() {
        val groups = AdPreloadPlan.fromPlacements().groups

        assertEquals(2, groups.size)
        assertTrue(AdFormat.REWARDED in groups[1])
        assertTrue(AdFormat.REWARDED !in groups[0])
        // Control (L-055): without the rewarded placements (the gallery's and the app's), no group has REWARDED.
        val withoutRewarded = AdPreloadPlan.fromPlacements(
            AdPlacement.entries - AdPlacement.GALLERY_RW_ACTION - AdPlacement.RW_PREMIUM_POSE,
        )
        assertTrue(AdFormat.REWARDED !in withoutRewarded.groups.flatten())
    }

    @Test
    fun fromPlacements_withBottomBar_hasCollapsibleInSecondGroup() {
        val groups = AdPreloadPlan.fromPlacements().groups

        assertEquals(2, groups.size)
        assertTrue(AdFormat.NATIVE_SMALL in groups[1])
        assertTrue(AdFormat.NATIVE_COLLAPSIBLE in groups[1])
        assertTrue(AdFormat.NATIVE_SMALL !in groups[0])
        assertTrue(AdFormat.NATIVE_COLLAPSIBLE !in groups[0])
        // Control (L-055): with no native small placement, or only one inside a list, no group has the layer.
        val noSmall = AdPreloadPlan.fromPlacements(AdPlacement.entries.filter { it.format != AdFormat.NATIVE_SMALL })
        assertTrue(AdFormat.NATIVE_COLLAPSIBLE !in noSmall.groups.flatten())
        val listOnly = AdPreloadPlan.fromPlacements(listOf(AdPlacement.GALLERY_INLIST_SMALL))
        assertTrue(AdFormat.NATIVE_COLLAPSIBLE !in listOnly.groups.flatten())
    }

    @Test
    fun customPlan_acceptsCollapsibleFirst() {
        // An app may preload the layer of its bottom bar before anything else (HD-02).
        val plan = AdPreloadPlan(listOf(setOf(AdFormat.NATIVE_COLLAPSIBLE), setOf(AdFormat.NATIVE_SMALL)))

        assertEquals(listOf(setOf(AdFormat.NATIVE_COLLAPSIBLE), setOf(AdFormat.NATIVE_SMALL)), plan.groups)
    }

    @Test
    fun fromPlacements_hasNoBannerLayer() {
        // One bottom bar, no banner placement: its banner layer is never preloaded (KB-33, criterion 8).
        val formats = AdPreloadPlan.fromPlacements(listOf(AdPlacement.POSES_BOTTOM)).groups.flatten().toSet()

        assertEquals(setOf(AdFormat.NATIVE_SMALL, AdFormat.NATIVE_COLLAPSIBLE), formats)
        assertTrue(AdFormat.BANNER !in formats)
    }

    @Test
    fun nativeOnly_keepsCollapsible() {
        // S-24, S-25: the refill when an activity comes back fills the stock of the layer too.
        val refill = AdPreloadPlan.fromPlacements(listOf(AdPlacement.POSES_BOTTOM)).nativeOnly()

        assertTrue(AdFormat.NATIVE_COLLAPSIBLE in refill.groups.flatten())
    }

    @Test
    fun templatePlan_hasAppOpenFirst() {
        // HD-02: the app open ad of the loading screen (T-024) is preloaded first, with the pool of the flow steps
        // (`gallery_native_step`, plan T-036).
        assertEquals(setOf(AdFormat.APP_OPEN, AdFormat.NATIVE_STEP), AdPreloadPlan.fromPlacements().groups.first())
    }

    @Test
    fun formatInTwoGroups_isRejected() {
        val error = runCatching {
            AdPreloadPlan(listOf(setOf(AdFormat.INTERSTITIAL), setOf(AdFormat.NATIVE_SMALL, AdFormat.INTERSTITIAL)))
        }.exceptionOrNull()

        assertTrue("expected IllegalArgumentException, got $error", error is IllegalArgumentException)
    }

    @Test
    fun emptyGroups_areDropped() {
        val plan = AdPreloadPlan(listOf(emptySet(), setOf(AdFormat.INTERSTITIAL), emptySet()))

        assertEquals(listOf(setOf(AdFormat.INTERSTITIAL)), plan.groups)
    }
}
