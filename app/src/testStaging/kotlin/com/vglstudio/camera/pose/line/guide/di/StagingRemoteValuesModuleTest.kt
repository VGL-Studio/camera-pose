package com.vglstudio.camera.pose.line.guide.di

import com.vglstudio.camera.pose.line.guide.core.ads.AdPlacement
import com.vglstudio.camera.pose.line.guide.core.ads.AdsSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Local values of staging builds for the ad keys (spec ads, question 6; plan T-074, section 8, question 2): the three
 * placements of the app are shown and the two ad logs are on; no other placement of the framework is switched on.
 */
class StagingRemoteValuesModuleTest {
    private val values = StagingRemoteValuesModule.appPlacementsShown() + StagingRemoteValuesModule.adLogsOn()
    private val byName = values.associate { it.key.name to it.value }

    @Test
    fun localValues_areExactlyTheAppSet() {
        val expected = mapOf<String, Any>(
            "is_show_poses_banner" to true,
            "is_show_poses_detail_banner" to true,
            "is_show_review_interstitial" to true,
            "is_log_ad_fullscreen_events" to true,
            "is_log_ad_slot_events" to true,
        )
        assertEquals(expected, byName)
        // Each key has one value only.
        assertEquals(values.size, byName.size)
    }

    @Test
    fun noGalleryNorSamplePlacement_isShown() {
        val notOfTheApp = AdPlacement.entries.filter { it.stagingOnly || it.key.startsWith("sample_") }
        // Control: the framework does declare such placements, and their switch keys have the name read below.
        assertTrue(notOfTheApp.isNotEmpty())
        assertEquals("is_show_sample_native_small", AdsSettings.placementEnabled(AdPlacement.SAMPLE_NATIVE_SMALL).name)
        notOfTheApp.forEach { placement ->
            val name = AdsSettings.placementEnabled(placement).name
            assertTrue("$name must not be set on staging", name !in byName)
        }
    }
}
