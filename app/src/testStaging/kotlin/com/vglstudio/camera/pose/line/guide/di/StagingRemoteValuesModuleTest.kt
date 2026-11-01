package com.vglstudio.camera.pose.line.guide.di

import com.vglstudio.camera.pose.line.guide.core.ads.AdPlacement
import com.vglstudio.camera.pose.line.guide.core.ads.AdsSettings
import com.vglstudio.camera.pose.line.guide.core.ads.startupAdPlacementFor
import com.vglstudio.camera.pose.line.guide.core.config.RemoteKey
import com.vglstudio.camera.pose.line.guide.core.config.RemoteSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Local values of staging builds for the ad keys (spec ads, questions 6 and 11; plan T-074, section 8, question 2; plan
 * T-079): the placements of the app are shown, the app open and resume ads are on, the two ad logs are on, the bottom
 * bars draw the native slot without the collapsible layer; no other placement of the framework is switched on, and the
 * first open and the combo stay off.
 */
class StagingRemoteValuesModuleTest {
    private val values = StagingRemoteValuesModule.appPlacementsShown() +
        StagingRemoteValuesModule.globalAdSwitchesOn() +
        StagingRemoteValuesModule.globalAdSwitchesOff()
    private val byName = values.associate { it.key.name to it.value }

    /** The values a staging build reads when Firebase has none for any key: the local value, else the default. */
    private val localOnly = object : RemoteSettings {
        // Safe cast: LocalRemoteValue.of ties each local value to the type of its key, as in DefaultRemoteSettings.
        @Suppress("UNCHECKED_CAST")
        override fun <T : Any> value(key: RemoteKey<T>): Flow<T> = flowOf((byName[key.name] as T?) ?: key.default)
    }

    @Test
    fun localValues_areExactlyTheAppSet() {
        val expected = mapOf<String, Any>(
            "is_show_poses_bottom" to true,
            "is_show_pose_detail_bottom" to true,
            "is_show_review_interstitial" to true,
            "is_show_inter_pose_detail" to true,
            // The card of the pose library grid (spec ads, question 11; plan T-080).
            "is_show_poses_in_collection" to true,
            "show_open_ads" to true,
            "show_resum_ads" to true,
            "is_show_native_bottom" to true,
            "is_show_native_collap" to false,
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

    @Test
    fun firstOpenAndCombo_areNotSet() {
        val off = listOf(
            AdsSettings.SHOW_OPEN_ADS_FIRST_OPEN,
            AdsSettings.IS_SHOW_NATIVE_AFTER_INTER,
        )
        // Control: the names are the keys of the spec, and the global switches of this module are set.
        assertEquals(
            listOf("show_open_ads_first_open", "is_show_native_after_inter"),
            off.map { it.name },
        )
        assertTrue(byName.toString(), "show_open_ads" in byName)
        off.forEach { key ->
            assertTrue("${key.name} must not be set on staging", key.name !in byName)
            // Off by default too, so staging leaves them off.
            assertEquals(false, key.default)
        }
    }

    @Test
    fun nativeBottomOn_collapOff() = runBlocking {
        // The bottom bars draw the 80 dp native slot, without the collapsible layer, as on release (spec ads, questions
        // 10 and 11; plan T-079).
        assertEquals(true, localOnly.value(AdsSettings.IS_SHOW_NATIVE_BOTTOM).first())
        assertEquals(false, localOnly.value(AdsSettings.IS_SHOW_NATIVE_COLLAP).first())
        // Control: the layer is on by default in code, so staging has to set it off.
        assertEquals(true, AdsSettings.IS_SHOW_NATIVE_COLLAP.default)
    }

    @Test
    fun everyLocalValue_isADeclaredKey() {
        val declared = AdsSettings.all().map { it.name }.toSet()
        // Control: a global switch is set, so the values below are not only placement switches.
        assertTrue(byName.toString(), "show_open_ads" in byName)
        values.forEach { assertTrue("${it.key.name} is not a declared key", it.key.name in declared) }
        assertEquals(values.size, values.map { it.key.name }.toSet().size)
    }

    @Test
    fun firstOpen_choosesNoStartupAd() = runBlocking {
        // The choice of :core:ads itself (A-01, A-02), with the values of a staging build and no Firebase value.
        assertNull(startupAdPlacementFor(localOnly, firstOpen = true))
        // Control: from the second open the same values choose the app open ad.
        assertEquals(AdPlacement.OPEN_APP, startupAdPlacementFor(localOnly, firstOpen = false))
    }
}
