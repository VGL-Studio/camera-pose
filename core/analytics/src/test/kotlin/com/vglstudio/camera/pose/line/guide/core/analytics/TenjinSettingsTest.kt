package com.vglstudio.camera.pose.line.guide.core.analytics

import com.vglstudio.camera.pose.line.guide.core.config.RemoteKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Switches of the Tenjin events (plan T-099, step 4; spec ads, question 21 c; ADR-0046). */
class TenjinSettingsTest {
    @Test
    fun keys_areBooleanSwitches_onByDefault() {
        val keys = TenjinSettings.all()
        // Control: the key of this ticket is there, so the checks below look at real keys.
        assertEquals(listOf("is_log_tenjin_ad_revenue"), keys.map { it.name })
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
        assertEquals(listOf(TenjinSettings.IS_LOG_TENJIN_AD_REVENUE), switches)
        assertEquals(TenjinSettings.all().map { it.name }, switches.map { it.name })
    }
}
