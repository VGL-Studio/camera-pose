package com.vglstudio.camera.pose.line.guide.core.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The AdMob impression Tenjin gets for a paid impression (plan T-099, step 3; spec ads, question 21 a): the four
 * fields the event of :core:ads has; the zero-value fallback, a blank currency and an impression of MAX are not sent.
 */
class TenjinEventsTest {
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
