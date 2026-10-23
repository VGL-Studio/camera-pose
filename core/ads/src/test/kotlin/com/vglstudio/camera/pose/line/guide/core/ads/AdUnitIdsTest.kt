package com.vglstudio.camera.pose.line.guide.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdUnitIdsTest {
    @Test
    fun configuredUnits_areParsedPerPlacementKey() {
        val config = AdPlacement.entries.joinToString(separator = " ; ") { " ${it.key} = unit/${it.key} " } +
            "; other_key=unit/2;"
        val ids = ConfiguredAdUnitIds(config)
        AdPlacement.entries.forEach { assertEquals("unit/${it.key}", ids.of(it)) }
        assertEquals("unit/poses_banner", ids.of(AdPlacement.POSES_BANNER))
        assertTrue(ids.missing.isEmpty())
    }

    @Test
    fun posesPlacements_areBannersWithSpecKeys() {
        assertEquals("poses_banner", AdPlacement.POSES_BANNER.key)
        assertEquals("poses_detail_banner", AdPlacement.POSES_DETAIL_BANNER.key)
        assertEquals(AdFormat.BANNER, AdPlacement.POSES_BANNER.format)
        assertEquals(AdFormat.BANNER, AdPlacement.POSES_DETAIL_BANNER.format)
        assertEquals("ads_poses_banner_enabled", AdsSettings.placementEnabledKey(AdPlacement.POSES_BANNER))
        assertEquals(
            "ads_poses_detail_banner_enabled",
            AdsSettings.placementEnabledKey(AdPlacement.POSES_DETAIL_BANNER),
        )
    }

    @Test
    fun placementWithoutUnit_isReportedMissingAndShowsNoAd() {
        val ids = ConfiguredAdUnitIds("")
        assertEquals(AdPlacement.entries.toList(), ids.missing)
        assertNull(ids.of(AdPlacement.SAMPLE_BOTTOM_BANNER))
    }

    @Test(expected = IllegalArgumentException::class)
    fun malformedEntry_isRejected() {
        ConfiguredAdUnitIds("sample_bottom_banner")
    }

    @Test
    fun googleTestUnits_existForEveryPlacement() {
        AdPlacement.entries.forEach { placement ->
            assertTrue(GoogleTestAdUnitIds.of(placement).startsWith("ca-app-pub-3940256099942544/"))
        }
    }

    @Test
    fun everyPlacementKey_isUniqueAndUsableInRemoteConfig() {
        val keys = AdPlacement.entries.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
        keys.forEach { assertTrue("Key '$it' must be lower_snake_case", Regex("^[a-z0-9_]+$").matches(it)) }
    }

    @Test
    fun nonPersonalizedAds_onlyInRegulatedRegionsWithoutRemoteSwitch() {
        assertTrue(requestsNonPersonalizedAds(inRegulatedRegion = true, personalizedInRegulatedRegions = false))
        assertFalse(requestsNonPersonalizedAds(inRegulatedRegion = true, personalizedInRegulatedRegions = true))
        assertFalse(requestsNonPersonalizedAds(inRegulatedRegion = false, personalizedInRegulatedRegions = false))
    }
}
