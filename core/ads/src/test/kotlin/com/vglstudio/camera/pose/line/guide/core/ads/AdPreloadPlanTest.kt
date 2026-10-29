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

        assertEquals(AdPlacement.entries.map { it.format }.toSet(), plan.groups.flatten().toSet())
        assertEquals(plan.groups.sumOf { it.size }, plan.groups.flatten().toSet().size)
    }

    @Test
    fun fromPlacements_hasRewardedInSecondGroup() {
        val groups = AdPreloadPlan.fromPlacements().groups

        assertEquals(2, groups.size)
        assertTrue(AdFormat.REWARDED in groups[1])
        assertTrue(AdFormat.REWARDED !in groups[0])
        // Control (L-055): without the rewarded placement of the gallery, no group has REWARDED.
        val withoutRewarded = AdPreloadPlan.fromPlacements(AdPlacement.entries - AdPlacement.GALLERY_RW_ACTION)
        assertTrue(AdFormat.REWARDED !in withoutRewarded.groups.flatten())
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
