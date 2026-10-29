package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.config.RemoteKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Keys of the ads module (plan T-019): names, types and defaults of docs/ops/ads-business-rules.md, section 10.
 * The defaults are business decisions of the product owner (ADR-0014): change them only with a new decision.
 */
class AdsSettingsTest {
    private val rw = AdPlacementDeclaration(code = "rw_get_item", format = AdFormat.REWARDED)
    private val onboarding = AdPlacementDeclaration(code = "native_onboarding", format = AdFormat.NATIVE_STEP)
    private val sounds = AdPlacementDeclaration(
        code = "inter_sounds",
        format = AdFormat.INTERSTITIAL,
        intervalGroup = AdIntervalGroup.named("sounds", 30),
    )
    private val list = AdPlacementDeclaration(code = "list_x", format = AdFormat.NATIVE_IN_COLLECTION, nativeEvery = 6)
    private val export = AdPlacementDeclaration(code = "rw_export", format = AdFormat.REWARDED)

    private fun assertKey(key: RemoteKey<*>, name: String, type: RemoteKey.Type, default: Any) {
        assertEquals(name, key.name)
        assertEquals(type, key.type)
        assertEquals(default, key.default)
    }

    /** Keys derived from the placements of the app: is_show_<code>, native_every_<code>, ads_interval_<group>. */
    private fun isDerived(name: String) = AdPlacement.entries.any { name == "is_show_${it.key}" } ||
        name.startsWith("native_every_") ||
        name.startsWith("ads_interval_")

    @Test
    fun globalKeys_matchAdsKitTable() {
        // Copied by hand from docs/ops/ads-business-rules.md, section 10.1 (keys owned by ads).
        val expected = setOf(
            Triple("is_show_ads", RemoteKey.Type.BOOLEAN, true),
            Triple("show_open_ads", RemoteKey.Type.BOOLEAN, false),
            Triple("show_open_ads_first_open", RemoteKey.Type.BOOLEAN, false),
            Triple("open_ad_type", RemoteKey.Type.STRING, "aoa"),
            Triple("show_resum_ads", RemoteKey.Type.BOOLEAN, false),
            Triple("resume_ad_type", RemoteKey.Type.STRING, "aoa"),
            Triple("ads_interval", RemoteKey.Type.LONG, 45L),
            Triple("is_show_native_after_inter", RemoteKey.Type.BOOLEAN, false),
            Triple("is_show_native_bottom", RemoteKey.Type.BOOLEAN, false),
            Triple("is_show_native_collap", RemoteKey.Type.BOOLEAN, true),
            Triple("time_expand_collap_ad", RemoteKey.Type.LONG, 30L),
            Triple("is_enable_consent_shortcut", RemoteKey.Type.BOOLEAN, false),
            Triple("is_log_ad_fullscreen_events", RemoteKey.Type.BOOLEAN, false),
            Triple("is_log_ad_slot_events", RemoteKey.Type.BOOLEAN, false),
        )
        val global = AdsSettings.all().filterNot { isDerived(it.name) }
            .map { Triple(it.name, it.type, it.default) }
        assertEquals(14, expected.size)
        assertEquals(expected, global.toSet())
        assertEquals(global.size, global.toSet().size)
    }

    @Test
    fun resumeKey_keepsAdsKitTypo() {
        assertEquals("show_resum_ads", AdsSettings.SHOW_RESUM_ADS.name)
        // Control: the key is really one of the module's keys, so the check below looks at the new list.
        assertTrue(AdsSettings.SHOW_RESUM_ADS in AdsSettings.all())
        assertFalse(AdsSettings.all().any { it.name == "show_resume_ads" })
    }

    @Test
    fun keysAdsKitDropped_areNotDeclared() {
        val names = AdsSettings.all().map { it.name }
        // Control: the list is the new one (the master switch is there), so an empty or old list cannot pass.
        assertTrue(names.toString(), "is_show_ads" in names)
        listOf("time_reload_collap_ad", "resume_ads_interval", "time_reload_bottom_ad").forEach {
            assertFalse("$it must not be declared", it in names)
        }
    }

    @Test
    fun consentShortcut_isOffByDefault() {
        assertKey(AdsSettings.IS_ENABLE_CONSENT_SHORTCUT, "is_enable_consent_shortcut", RemoteKey.Type.BOOLEAN, false)
        // Control: the key is listed, so the table of keys shows this default.
        assertTrue(AdsSettings.IS_ENABLE_CONSENT_SHORTCUT in AdsSettings.all())
    }

    @Test
    fun nativeCollap_isOnByDefault() {
        assertKey(AdsSettings.IS_SHOW_NATIVE_COLLAP, "is_show_native_collap", RemoteKey.Type.BOOLEAN, true)
        assertTrue(AdsSettings.IS_SHOW_NATIVE_COLLAP in AdsSettings.all())
    }

    @Test
    fun masterSwitch_isOnByDefault() {
        assertKey(AdsSettings.IS_SHOW_ADS, "is_show_ads", RemoteKey.Type.BOOLEAN, true)
        assertTrue(AdsSettings.IS_SHOW_ADS in AdsSettings.all())
    }

    @Test
    fun keysNamedByTemplate_areMarked() {
        // The official AdsKit key table, DG:171-181, copied by hand (not ADSKIT_KEY_NAMES).
        val adsKitNames = setOf(
            "show_open_ads", "show_open_ads_first_open", "open_ad_type", "show_resum_ads", "resume_ad_type",
            "ads_interval", "is_show_native_after_inter", "is_show_native_bottom", "time_reload_collap_ad",
            "is_show_rw_get_item", "is_show_native_onboarding",
        )
        assertEquals(11, adsKitNames.size)
        val keys = AdsSettings.all()
        // Control: both kinds are in the list, so both checks below run.
        assertTrue(keys.any { it.name in adsKitNames })
        assertTrue(keys.any { it.name !in adsKitNames })
        keys.forEach { key ->
            if (key.name in adsKitNames) {
                assertFalse(key.name, key.description.contains("not an AdsKit key"))
            } else {
                assertTrue(
                    "${key.name}: ${key.description}",
                    key.description.contains("not an AdsKit key") || key.description.contains("Derived key"),
                )
            }
        }
    }

    @Test
    fun wave2Keys_areGone() {
        val old = setOf(
            "ads_enabled",
            "ads_personalized_regulated_regions_enabled",
            "ads_interstitial_min_interval_seconds",
            "ads_first_session_grace_seconds",
            "ads_interstitial_max_per_session_count",
            "ads_inlist_every_count",
            "ads_native_load_timeout_seconds",
        )
        val names = AdsSettings.all().map { it.name }
        assertTrue("No keys listed", names.isNotEmpty())
        names.forEach { name ->
            assertFalse("$name is a wave 2 key", name in old)
            assertFalse("$name is a wave 2 placement key", Regex("^ads_.+_enabled$").matches(name))
        }
    }

    @Test
    fun fakePlacements_deriveEveryKeyWithoutCoreChanges() {
        val keys = AdsSettings.keysFor(listOf(rw, onboarding, sounds, list, export)).associateBy { it.name }
        fun key(name: String) = keys[name] ?: error("$name is missing from ${keys.keys}")
        listOf(
            "is_show_rw_get_item",
            "is_show_native_onboarding",
            "is_show_inter_sounds",
            "is_show_list_x",
            "is_show_rw_export",
        ).forEach { name -> assertKey(key(name), name, RemoteKey.Type.BOOLEAN, false) }
        assertKey(key("ads_interval_sounds"), "ads_interval_sounds", RemoteKey.Type.LONG, 30L)
        assertKey(key("native_every_list_x"), "native_every_list_x", RemoteKey.Type.LONG, 6L)
    }

    @Test
    fun placementsAreOffByDefault() {
        val all = AdsSettings.all()
        // App-wide placements have no switch of their own (MC-09, question 3 of plan T-024).
        assertTrue(AdPlacement.entries.any { it.appWide })
        AdPlacement.entries.filterNot { it.appWide }.forEach { placement ->
            val key = AdsSettings.placementEnabled(placement)
            assertKey(key, "is_show_${placement.key}", RemoteKey.Type.BOOLEAN, false)
            assertTrue("$placement is missing", key in all)
        }
    }

    @Test
    fun appWidePlacements_haveNoSwitchKey() {
        val names = AdsSettings.all().map { it.name }
        // Control: the switch of a placement that is not app-wide is listed.
        assertTrue(names.toString(), "is_show_sample_native_small" in names)
        assertFalse(names.toString(), "is_show_open_app" in names)
        assertFalse(names.toString(), "is_show_open_app_inter" in names)
    }

    @Test
    fun all_isDerivedFromDeclaredPlacements() {
        // Control: the list is the new one, with the switch of the sample placement.
        assertTrue(AdsSettings.all().any { it.name == "is_show_sample_native_small" })
        assertEquals(AdsSettings.keysFor(AdPlacement.entries.map { it.declaration }), AdsSettings.all())
    }

    @Test
    fun sharedGroup_isOneKey() {
        val other = AdPlacementDeclaration(
            code = "inter_sounds_2",
            format = AdFormat.INTERSTITIAL,
            intervalGroup = AdIntervalGroup.named("sounds", 30),
        )
        val names = AdsSettings.keysFor(listOf(sounds, other)).map { it.name }
        assertEquals(names.toString(), 1, names.count { it == "ads_interval_sounds" })
    }

    @Test
    fun conflictingGroupDefaults_areRejected() {
        val other = AdPlacementDeclaration(
            code = "inter_sounds_2",
            format = AdFormat.INTERSTITIAL,
            intervalGroup = AdIntervalGroup.named("sounds", 40),
        )
        // Control: each placement alone is accepted.
        AdsSettings.keysFor(listOf(sounds))
        AdsSettings.keysFor(listOf(other))
        assertThrows(IllegalArgumentException::class.java) { AdsSettings.keysFor(listOf(sounds, other)) }
    }

    @Test
    fun adsKitCodes_giveAdsKitKeyStrings() {
        val keys = AdsSettings.keysFor(listOf(rw, onboarding, sounds)).associateBy { it.name }
        listOf("is_show_rw_get_item", "is_show_native_onboarding", "ads_interval_sounds").forEach { name ->
            assertTrue("$name is missing from ${keys.keys}", name in keys)
        }
        listOf("is_show_rw_get_item", "is_show_native_onboarding").forEach { name ->
            assertFalse(name, keys.getValue(name).description.contains("not an AdsKit key"))
        }
    }

    @Test
    fun allKeys_containsEveryDeclaredKey() {
        val declared = AdsSettings::class.java.declaredFields
            .filter { RemoteKey::class.java.isAssignableFrom(it.type) }
            .map { field ->
                field.isAccessible = true
                field.get(AdsSettings) as RemoteKey<*>
            }
        // Control: the reflection finds the 14 keys of the table, so an empty list cannot pass by accident.
        assertTrue("Found only ${declared.map { it.name }}", declared.size >= 14)
        val all = AdsSettings.all()
        declared.forEach { key -> assertTrue("$key is missing from AdsSettings.all()", key in all) }
    }

    @Test
    fun allKeys_haveUniqueNames() {
        val names = AdsSettings.all().map { it.name }
        // Control: the 14 keys of the table and one key per placement that is not app-wide are there, so an empty
        // list cannot pass.
        assertTrue("Only $names", names.size >= 14 + AdPlacement.entries.count { !it.appWide })
        assertEquals(names.distinct(), names)
    }
}
