package com.vglstudio.camera.pose.line.guide.core.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Names and params of the Firebase ad metrics (docs/ops/ads-business-rules.md, section 8.2; plan T-023). */
class FirebaseAdMetricsTest {
    @Test
    fun impressionRevenue_paramsAndTypes() {
        val event = AdMetricEvent.ImpressionRevenue(
            adFormat = "INTER",
            adSource = "AppLovin",
            monetizationNetwork = "AppLovin",
            adUnitName = "unit",
            currency = "USD",
            value = 0.0123,
            adPlacement = "gallery_interstitial",
            adapterClassName = "com.x.Adapter",
        )

        assertEquals("ad_impression_placement", event.firebaseName())
        val params = event.firebaseParams()
        assertEquals(
            setOf("ad_format", "ad_platform", "ad_source", "ad_unit_name", "currency", "value", "ad_placement"),
            params.keys,
        )
        assertEquals("INTER", params["ad_format"])
        assertEquals("admob", params["ad_platform"])
        assertEquals("AppLovin", params["ad_source"])
        assertEquals("unit", params["ad_unit_name"])
        assertEquals("USD", params["currency"])
        assertTrue(params["value"] is Double)
        assertEquals(0.0123, params["value"] as Double, 0.0)
        assertEquals("gallery_interstitial", params["ad_placement"])
        assertFalse(params.values.any { it == "com.x.Adapter" })
    }

    @Test
    fun revenue_monetizationNetwork_isNotSentToFirebase() {
        val event = AdMetricEvent.ImpressionRevenue(
            adFormat = "INTER",
            adSource = "admob",
            monetizationNetwork = "RawSourceName",
            adUnitName = "unit",
            currency = "USD",
            value = 0.0,
            adPlacement = "p",
            adapterClassName = null,
        )

        val params = event.firebaseParams()

        assertFalse(params.containsKey("monetization_network"))
        assertFalse(params.values.contains("RawSourceName"))
        // Control: Firebase gets its own network name.
        assertEquals("admob", params["ad_source"])
    }

    @Test
    fun emptyOrMissingParams_areDropped() {
        val withPlacement = revenue(adPlacement = "poses_bottom")
        // Control (L-055): with a placement the key is there.
        assertEquals("poses_bottom", withPlacement.firebaseParams()["ad_placement"])
        assertFalse(revenue(adPlacement = null).firebaseParams().containsKey("ad_placement"))
        assertFalse(revenue(adPlacement = "").firebaseParams().containsKey("ad_placement"))

        val full = AdMetricEvent.LoadSuccess("NATIVE_SMALL", "poses_bottom", loadAttempt = 2, latencyMs = 250)
        assertEquals(2L, full.firebaseParams()["load_attempt"])
        assertEquals(250L, full.firebaseParams()["latency_ms"])
        val bare = AdMetricEvent.LoadSuccess("NATIVE_SMALL", "poses_bottom", loadAttempt = null, null)
        assertFalse(bare.firebaseParams().containsKey("load_attempt"))
        assertFalse(bare.firebaseParams().containsKey("latency_ms"))
        assertEquals("NATIVE_SMALL", bare.firebaseParams()["ad_format"])
    }

    @Test
    fun noEventIsNamedAdImpression() {
        val subclasses = AdMetricEvent::class.sealedSubclasses
        assertEquals(8, subclasses.size)
        val events = listOf(
            revenue(adPlacement = "p"),
            AdMetricEvent.LoadSuccess("INTER", "preload", 1, 10),
            AdMetricEvent.LoadFail("INTER", "preload", 1, 10, "3", "no_fill"),
            AdMetricEvent.Opportunity("INTER", "p", "shown", "ready"),
            AdMetricEvent.ShowFail("INTER", "p", "1", "ad_reused"),
            AdMetricEvent.Clicked("INTER", "p"),
            AdMetricEvent.RewardEarned("REWARDED", "p"),
            AdMetricEvent.RewardDialog("REWARDED", "p", "show"),
        )
        // One event of every subclass.
        assertEquals(subclasses.toSet(), events.map { it::class }.toSet())
        events.forEach {
            assertNotEquals("ad_impression", it.firebaseName())
            assertTrue(it.firebaseName().isNotEmpty())
        }
    }

    @Test
    fun opportunity_params() {
        val event = AdMetricEvent.Opportunity("INTER", "gallery_interstitial", "not_ready", "loading")

        assertEquals("ad_opportunity", event.firebaseName())
        assertEquals(
            mapOf(
                "ad_format" to "INTER",
                "ad_placement" to "gallery_interstitial",
                "opportunity_result" to "not_ready",
                "load_state" to "loading",
            ),
            event.firebaseParams(),
        )
    }

    @Test
    fun showFail_params() {
        val event = AdMetricEvent.ShowFail("INTER", "gallery_interstitial", "3", "app_not_foreground")

        assertEquals("ad_show_fail", event.firebaseName())
        assertEquals(
            mapOf(
                "ad_format" to "INTER",
                "ad_placement" to "gallery_interstitial",
                "show_error_code" to "3",
                "show_fail_reason" to "app_not_foreground",
            ),
            event.firebaseParams(),
        )
    }

    @Test
    fun loadFail_params() {
        val event = AdMetricEvent.LoadFail("NATIVE_SMALL", "poses_bottom", 2, 340, "3", "no_fill")

        assertEquals("ad_load_fail", event.firebaseName())
        val params = event.firebaseParams()
        assertEquals(
            mapOf(
                "ad_format" to "NATIVE_SMALL",
                "ad_placement" to "poses_bottom",
                "load_attempt" to 2L,
                "latency_ms" to 340L,
                "load_error_code" to "3",
                "load_state" to "no_fill",
            ),
            params,
        )
        assertTrue(params["load_attempt"] is Long)
        assertTrue(params["latency_ms"] is Long)
        assertTrue(params["load_error_code"] is String)
        assertEquals("ad_load_success", AdMetricEvent.LoadSuccess("INTER", "preload", 1, 5).firebaseName())
    }

    @Test
    fun clicked_params() {
        val event = AdMetricEvent.Clicked("NATIVE_MEDIUM", "gallery_native_medium")

        assertEquals("ad_clicked", event.firebaseName())
        assertEquals(
            mapOf("ad_format" to "NATIVE_MEDIUM", "ad_placement" to "gallery_native_medium"),
            event.firebaseParams(),
        )
    }

    @Test
    fun rewardEvents_nameAndParams() {
        val earned = AdMetricEvent.RewardEarned("REWARDED", "gallery_rw_action")
        assertEquals("ad_reward_earned", earned.firebaseName())
        assertEquals(mapOf("ad_placement" to "gallery_rw_action"), earned.firebaseParams())

        val dialog = AdMetricEvent.RewardDialog("REWARDED", "gallery_rw_action", "dismiss")
        assertEquals("reward_dialog", dialog.firebaseName())
        // No `ad_format` (section 8.2).
        assertEquals(
            mapOf("ad_placement" to "gallery_rw_action", "dialog_action" to "dismiss"),
            dialog.firebaseParams(),
        )
    }

    private fun revenue(adPlacement: String?) = AdMetricEvent.ImpressionRevenue(
        adFormat = "NATIVE_SMALL",
        adSource = "admob",
        monetizationNetwork = "",
        adUnitName = "unit",
        currency = "USD",
        value = 0.0,
        adPlacement = adPlacement,
        adapterClassName = null,
    )
}
