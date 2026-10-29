package com.vglstudio.camera.pose.line.guide.core.analytics

import com.vglstudio.camera.pose.line.guide.core.config.RemoteKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Switches of the AppsFlyer events (docs/ops/ads-business-rules.md, section 10.1; plan T-019). */
class AppsFlyerSettingsTest {
    // Copied by hand from section 10.1.
    private val names = setOf(
        "is_log_af_ad_revenue",
        "is_log_af_inters_ad_eligible",
        "is_log_af_inters_api_called",
        "is_log_af_inters_displayed",
        "is_log_af_rewarded_ad_eligible",
        "is_log_af_rewarded_api_called",
        "is_log_af_rewarded_ad_displayed",
        "is_log_af_native_ad_eligible",
        "is_log_af_native_api_called",
        "is_log_af_native_ad_displayed",
        "is_log_af_inters_displayed_n_times",
    )

    @Test
    fun everyKey_isOnByDefault() {
        val keys = AppsFlyerSettings.all()
        assertEquals(11, names.size)
        assertEquals(names, keys.map { it.name }.toSet())
        assertEquals(keys.size, keys.map { it.name }.toSet().size)
        keys.forEach { key ->
            assertEquals(key.name, RemoteKey.Type.BOOLEAN, key.type)
            assertEquals(key.name, true, key.default)
        }
    }

    @Test
    fun switches_areAllKeys_inOrder() {
        val switches = AppsFlyerSettings.switches()
        // Control: the 11 keys are there.
        assertEquals(11, switches.size)
        assertEquals(AppsFlyerSettings.all().map { it.name }, switches.map { it.name })
    }

    @Test
    fun everyKey_isMarkedAsTemplateName() {
        val keys = AppsFlyerSettings.all()
        // Control: the 11 keys are there, so the check below looks at real keys.
        assertEquals(11, keys.size)
        // None of them is in the official AdsKit key table (DG:171-181).
        keys.forEach { assertTrue("${it.name}: ${it.description}", it.description.contains("not an AdsKit key")) }
    }
}
