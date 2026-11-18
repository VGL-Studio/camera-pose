package com.vglstudio.camera.pose.line.guide.core.analytics

import com.vglstudio.camera.pose.line.guide.core.config.RemoteKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Switches of the Tenjin events (plan T-099, step 4; plan T-100, step 3; spec ads, question 21 c; ADR-0046). */
class TenjinSettingsTest {
    @Test
    fun keys_areBooleanSwitches_onByDefault() {
        val keys = TenjinSettings.all()
        // Control: the keys of T-099 and T-100 are there, in order, so the checks below look at real keys.
        assertEquals(
            listOf(
                "is_log_tenjin_ad_revenue",
                "is_log_tenjin_inters_ad_eligible",
                "is_log_tenjin_inters_api_called",
                "is_log_tenjin_inters_displayed",
                "is_log_tenjin_rewarded_ad_eligible",
                "is_log_tenjin_rewarded_api_called",
                "is_log_tenjin_rewarded_ad_displayed",
                "is_log_tenjin_native_ad_eligible",
                "is_log_tenjin_native_api_called",
                "is_log_tenjin_native_ad_displayed",
                "is_log_tenjin_inters_displayed_n_times",
            ),
            keys.map { it.name },
        )
        keys.forEach { key ->
            assertTrue(key.name, key.name.startsWith("is_log_tenjin_"))
            assertEquals(key.name, RemoteKey.Type.BOOLEAN, key.type)
            assertEquals(key.name, true, key.default)
            assertTrue("${key.name}: ${key.description}", "Tenjin" in key.description)
            assertTrue(
                "${key.name}: ${key.description}",
                key.description.endsWith("Name set by the app, not an AdsKit key."),
            )
        }
    }

    @Test
    fun switches_matchAll() {
        val switches = TenjinSettings.switches()
        // Control: the switches are not empty.
        assertEquals(
            listOf(
                TenjinSettings.IS_LOG_TENJIN_AD_REVENUE,
                TenjinSettings.IS_LOG_TENJIN_INTERS_AD_ELIGIBLE,
                TenjinSettings.IS_LOG_TENJIN_INTERS_API_CALLED,
                TenjinSettings.IS_LOG_TENJIN_INTERS_DISPLAYED,
                TenjinSettings.IS_LOG_TENJIN_REWARDED_AD_ELIGIBLE,
                TenjinSettings.IS_LOG_TENJIN_REWARDED_API_CALLED,
                TenjinSettings.IS_LOG_TENJIN_REWARDED_AD_DISPLAYED,
                TenjinSettings.IS_LOG_TENJIN_NATIVE_AD_ELIGIBLE,
                TenjinSettings.IS_LOG_TENJIN_NATIVE_API_CALLED,
                TenjinSettings.IS_LOG_TENJIN_NATIVE_AD_DISPLAYED,
                TenjinSettings.IS_LOG_TENJIN_INTERS_DISPLAYED_N_TIMES,
            ),
            switches,
        )
        assertEquals(TenjinSettings.all().map { it.name }, switches.map { it.name })
    }

    @Test
    fun revenueKey_descriptionUnchanged() {
        // Guard: the key of T-099 keeps its description word for word (its row of the key table does not change).
        assertEquals(
            "Sends the revenue of every paid AdMob impression to Tenjin. Tenjin event switch (ADR-1010). " +
                "Name set by the app, not an AdsKit key.",
            TenjinSettings.IS_LOG_TENJIN_AD_REVENUE.description,
        )
    }
}
