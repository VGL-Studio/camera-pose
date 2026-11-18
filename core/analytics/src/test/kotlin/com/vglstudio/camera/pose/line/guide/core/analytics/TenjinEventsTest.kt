package com.vglstudio.camera.pose.line.guide.core.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The AdMob impression Tenjin gets for a paid impression (plan T-099, step 3; spec ads, question 21 a): the four
 * fields the event of :core:ads has; the zero-value fallback, a blank currency and an impression of MAX are not sent.
 * The ad funnel events (plan T-100; ADR-1010, sections 5 and 6): the AppsFlyer names without `af_`, one switch each.
 */
class TenjinEventsTest {
    @Test
    fun funnelNames_areAfNamesWithoutPrefix() {
        val names = funnelSteps().map { it.toTenjinEvent() }

        assertEquals(
            listOf(
                "inters_ad_eligible",
                "inters_api_called",
                "inters_displayed",
                "rewarded_ad_eligible",
                "rewarded_api_called",
                "rewarded_ad_displayed",
                "native_ad_eligible",
                "native_api_called",
                "native_ad_displayed",
            ),
            names,
        )
        names.forEach { assertFalse(it, it.orEmpty().startsWith("af_")) }
    }

    @Test
    fun interDisplayedTimes_1to20_only() {
        assertEquals("inters_displayed_1_times", AdFunnelEvent.InterDisplayedTimes(1).toTenjinEvent())
        assertEquals("inters_displayed_20_times", AdFunnelEvent.InterDisplayedTimes(20).toTenjinEvent())
        assertNull(AdFunnelEvent.InterDisplayedTimes(0).toTenjinEvent())
        assertNull(AdFunnelEvent.InterDisplayedTimes(21).toTenjinEvent())
        assertNull(AdFunnelEvent.InterDisplayedTimes(-1).toTenjinEvent())
    }

    @Test
    fun everyFunnelEvent_hasItsOwnTenjinSwitch() {
        val events = funnelSteps() + AdFunnelEvent.InterDisplayedTimes(1)
        val switches = events.map { it.tenjinSwitch() }

        assertEquals(
            listOf(
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
        assertEquals(10, switches.map { it.name }.toSet().size)
        val tenjinNames = TenjinSettings.switches().map { it.name }
        val appsFlyerNames = AppsFlyerSettings.all().map { it.name }
        switches.forEach { key ->
            assertTrue(key.name, key.name in tenjinNames)
            assertFalse(key.name, key.name in appsFlyerNames)
        }
        (1..20).forEach { count ->
            assertEquals(
                TenjinSettings.IS_LOG_TENJIN_INTERS_DISPLAYED_N_TIMES,
                AdFunnelEvent.InterDisplayedTimes(count).tenjinSwitch(),
            )
        }
    }

    /** Eligible, loaded, displayed, for each of interstitial, rewarded, native, in that order. */
    private fun funnelSteps(): List<AdFunnelEvent> = AdFunnelKind.entries.flatMap { kind ->
        listOf(
            AdFunnelEvent.Eligible(kind),
            AdFunnelEvent.ApiCalled(kind, "INTER", "ca-app-pub-test/1", "com.google.ads.mediation.Adapter"),
            AdFunnelEvent.Displayed(kind, "INTER", "ca-app-pub-test/1", "com.google.ads.mediation.Adapter"),
        )
    }

    @Test
    fun paidValue_hasFields() {
        assertEquals(
            mapOf(
                "ad_unit_id" to "ca-app-pub-unit",
                "currency_code" to "USD",
                "value_micros" to 12_300L,
                "mediation_adapter_class_name" to "x.Adapter",
            ),
            tenjinAdMobPayload(revenue()),
        )
    }

    @Test
    fun zeroValue_isNull() {
        // Control: the same impression with a value is sent.
        assertNotNull(tenjinAdMobPayload(revenue(value = 0.0123)))
        assertNull(tenjinAdMobPayload(revenue(value = 0.0)))
    }

    @Test
    fun nullAdapter_omitsAdapterField() {
        val payload = tenjinAdMobPayload(revenue(adapterClassName = null))
        assertEquals(setOf("ad_unit_id", "currency_code", "value_micros"), payload?.keys)
        assertFalse("mediation_adapter_class_name" in payload.orEmpty())
    }

    @Test
    fun blankCurrency_isNull() {
        // Control: the same impression with a currency is sent.
        assertNotNull(tenjinAdMobPayload(revenue(currency = "USD")))
        assertNull(tenjinAdMobPayload(revenue(currency = "")))
        assertNull(tenjinAdMobPayload(revenue(currency = " ")))
    }

    @Test
    fun maxPlatform_isNull() {
        // Control: the same impression of AdMob is sent.
        assertNotNull(tenjinAdMobPayload(revenue(platform = AdMetricPlatform.ADMOB)))
        assertNull(tenjinAdMobPayload(revenue(platform = AdMetricPlatform.APPLOVIN)))
    }

    private fun revenue(
        value: Double = 0.0123,
        currency: String = "USD",
        adapterClassName: String? = "x.Adapter",
        platform: AdMetricPlatform = AdMetricPlatform.ADMOB,
    ) = AdMetricEvent.ImpressionRevenue(
        adFormat = "INTER",
        adSource = "admob",
        monetizationNetwork = "",
        adUnitName = "ca-app-pub-unit",
        currency = currency,
        value = value,
        adPlacement = "p",
        adapterClassName = adapterClassName,
        platform = platform,
    )
}
