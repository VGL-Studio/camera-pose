package com.vglstudio.camera.pose.line.guide.core.analytics

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Names, params and switches of the AppsFlyer events (docs/ops/ads-business-rules.md, section 8.3; plan T-033). */
class AppsFlyerEventsTest {
    @Test
    fun revenue_hasTheEightParamsOfSection83() {
        val event = revenue(
            adSource = "Meta Audience Network",
            monetizationNetwork = "Meta Audience Network",
            adPlacement = "home",
        )

        val af = afRevenue(event, "VN")

        assertEquals("af_ad_revenue", af.name)
        val params = af.params!!
        assertEquals(
            mapOf(
                "monetization_network" to "Meta Audience Network",
                "mediation_network" to "admob",
                "af_currency" to "USD",
                "af_revenue" to 0.0123,
                "country" to "VN",
                "ad_placement" to "home",
                "ad_unit" to "unit",
                "ad_type" to "INTER",
            ),
            params,
        )
        // In the order of section 8.3, and no other key (no adapter class).
        assertEquals(
            listOf(
                "monetization_network",
                "mediation_network",
                "af_currency",
                "af_revenue",
                "country",
                "ad_placement",
                "ad_unit",
                "ad_type",
            ),
            params.keys.toList(),
        )
        assertTrue(params["af_revenue"] is Double)
        params.filterKeys { it != "af_revenue" }.values.forEach { assertTrue("$it", it is String) }
        assertFalse(params.values.contains("x.Adapter"))
    }

    @Test
    fun revenue_noNetworkNoPlacement_areEmptyStrings() {
        val event = revenue(adSource = "admob", monetizationNetwork = "", adPlacement = null)

        val params = afRevenue(event, "VN").params!!

        assertEquals("", params["monetization_network"])
        assertEquals("", params["ad_placement"])
        assertTrue(params.containsKey("ad_placement"))
        // Control: Firebase leaves the placement out and names the network admob for the same event.
        val firebase = event.firebaseParams()
        assertFalse(firebase.containsKey("ad_placement"))
        assertEquals("admob", firebase["ad_source"])
    }

    @Test
    fun noEventIsNamedAfRevenue() {
        val subclasses = AdFunnelEvent::class.sealedSubclasses
        assertEquals(4, subclasses.size)
        val events = AdFunnelKind.entries.flatMap { kind ->
            listOf(
                AdFunnelEvent.Eligible(kind),
                AdFunnelEvent.ApiCalled(kind, "INTER", "unit", "x.Adapter"),
                AdFunnelEvent.Displayed(kind, "INTER", "unit", "x.Adapter"),
            )
        } + (0..21).map { AdFunnelEvent.InterDisplayedTimes(it) }
        // One event of every subclass.
        assertEquals(subclasses.toSet(), events.map { it::class }.toSet())
        val names = events.mapNotNull { it.toAfEvent()?.name } + afRevenue(revenue(), "VN").name
        // Control: 9 funnel events, 20 counts and the revenue.
        assertEquals(30, names.size)
        names.forEach {
            assertNotEquals("af_revenue", it)
            assertTrue(it, it.startsWith("af_"))
        }
    }

    @Test
    fun everyEvent_hasItsOwnSwitch() {
        val funnel = AdFunnelKind.entries.flatMap { kind ->
            listOf(
                AdFunnelEvent.Eligible(kind),
                AdFunnelEvent.ApiCalled(kind, "INTER", "unit", ""),
                AdFunnelEvent.Displayed(kind, "INTER", "unit", ""),
            )
        } + AdFunnelEvent.InterDisplayedTimes(1)
        assertEquals(10, funnel.size)

        val switches = funnel.map { it.switch() } + REVENUE_SWITCH

        assertEquals(11, switches.map { it.name }.toSet().size)
        assertEquals(AppsFlyerSettings.all().map { it.name }.toSet(), switches.map { it.name }.toSet())
        // Each event has the switch named after it.
        funnel.forEach { event ->
            val af = event.toAfEvent()!!
            val expected = if (event is AdFunnelEvent.InterDisplayedTimes) {
                "is_log_af_inters_displayed_n_times"
            } else {
                "is_log_${af.name}"
            }
            assertEquals(af.name, expected, event.switch().name)
        }
        assertEquals("is_log_af_ad_revenue", REVENUE_SWITCH.name)
    }

    @Test
    fun funnelNamesAndParams() {
        // Copied by hand from section 8.3: the displayed event of interstitials has no `_ad_`.
        val expected = listOf(
            Triple(AdFunnelKind.INTERS, "eligible", "af_inters_ad_eligible"),
            Triple(AdFunnelKind.INTERS, "api", "af_inters_api_called"),
            Triple(AdFunnelKind.INTERS, "displayed", "af_inters_displayed"),
            Triple(AdFunnelKind.REWARDED, "eligible", "af_rewarded_ad_eligible"),
            Triple(AdFunnelKind.REWARDED, "api", "af_rewarded_api_called"),
            Triple(AdFunnelKind.REWARDED, "displayed", "af_rewarded_ad_displayed"),
            Triple(AdFunnelKind.NATIVE, "eligible", "af_native_ad_eligible"),
            Triple(AdFunnelKind.NATIVE, "api", "af_native_api_called"),
            Triple(AdFunnelKind.NATIVE, "displayed", "af_native_ad_displayed"),
        )
        expected.forEach { (kind, step, name) ->
            val event = when (step) {
                "eligible" -> AdFunnelEvent.Eligible(kind)
                "api" -> AdFunnelEvent.ApiCalled(kind, "NATIVE_MEDIUM", "unit", "x.Adapter")
                else -> AdFunnelEvent.Displayed(kind, "NATIVE_MEDIUM", "unit", "x.Adapter")
            }
            val af = event.toAfEvent()!!
            assertEquals(name, af.name)
            if (step == "eligible") {
                assertNull(name, af.params)
            } else {
                assertEquals(
                    name,
                    mapOf("ad_format" to "NATIVE_MEDIUM", "ad_unit_id" to "unit", "ad_source" to "x.Adapter"),
                    af.params,
                )
            }
        }
    }

    @Test
    fun country_simThenNetworkThenLocale_upperCase() {
        val gb = Locale("en", "GB")
        assertEquals("VN", countryCode("vn", "us", gb))
        assertEquals("US", countryCode("", "us", gb))
        assertEquals("US", countryCode("  ", "us", gb))
        assertEquals("US", countryCode(null, "us", gb))
        assertEquals("GB", countryCode("", "", gb))
        assertEquals("GB", countryCode(null, null, gb))
        assertEquals("", countryCode(null, null, Locale("en")))
    }

    @Test
    fun interDisplayedTimes_1to20only() {
        assertEquals("af_inters_displayed_1_times", AdFunnelEvent.InterDisplayedTimes(1).toAfEvent()!!.name)
        assertEquals("af_inters_displayed_20_times", AdFunnelEvent.InterDisplayedTimes(20).toAfEvent()!!.name)
        assertNull(AdFunnelEvent.InterDisplayedTimes(1).toAfEvent()!!.params)
        assertNull(AdFunnelEvent.InterDisplayedTimes(0).toAfEvent())
        assertNull(AdFunnelEvent.InterDisplayedTimes(21).toAfEvent())
    }

    private fun revenue(
        adSource: String = "AppLovin",
        monetizationNetwork: String = "AppLovin",
        adPlacement: String? = "home",
    ) = AdMetricEvent.ImpressionRevenue(
        adFormat = "INTER",
        adSource = adSource,
        monetizationNetwork = monetizationNetwork,
        adUnitName = "unit",
        currency = "USD",
        value = 0.0123,
        adPlacement = adPlacement,
        adapterClassName = "x.Adapter",
    )
}
