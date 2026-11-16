package com.vglstudio.camera.pose.line.guide.core.ads

import com.applovin.mediation.MaxNetworkResponseInfo.AdLoadState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What :core:ads reads of a MAX ad and its waterfall (plan T-072, KB-31 criterion 7, KB-28 criterion 3): revenue in
 * dollars, the network, the adapter of the network that loaded, and one attempt per network tried; never the
 * credentials of a network or a message (the fakes throw [AssertionError] on those getters).
 */
class MaxAdInfoTest {
    @Test
    fun adInfo_readsRevenueNetworkPrecisionUnit() {
        val info = maxAdInfoOf(FakeMaxAd(adUnitId = UNIT, revenue = 0.0123, networkName = "AppLovin"))

        assertEquals(UNIT, info.adUnitId)
        assertEquals(0.0123, info.revenueUsd, 0.0)
        assertEquals("AppLovin", info.networkName)
        assertEquals("exact", info.revenuePrecision)
    }

    @Test
    fun adInfo_adapterOfLoadedNetwork() {
        val info = maxAdInfoOf(FakeMaxAd(waterfall = threeNetworks()))

        assertEquals(GOOGLE_ADAPTER, info.adapterClassName)
        assertEquals(3, info.networks.size)
        // Control: a waterfall with no loaded network gives no adapter.
        val failed = FakeMaxWaterfall(listOf(response(MINTEGRAL_ADAPTER, "Mintegral", AdLoadState.FAILED_TO_LOAD)))
        assertNull(maxAdInfoOf(FakeMaxAd(waterfall = failed)).adapterClassName)
    }

    @Test
    fun adInfo_nullAd_isEmpty() {
        val empty = maxAdInfoOf(null)
        assertEquals("", empty.adUnitId)
        assertEquals(0.0, empty.revenueUsd, 0.0)
        assertNull(empty.networkName)
        assertNull(empty.revenuePrecision)
        assertNull(empty.adapterClassName)
        assertTrue(empty.networks.isEmpty())
        // Control: an ad is read.
        assertEquals(0.0123, maxAdInfoOf(FakeMaxAd(revenue = 0.0123)).revenueUsd, 0.0)
    }

    @Test
    fun adInfo_getterThrows_fieldEmpty_noCrash() {
        val info = maxAdInfoOf(
            FakeMaxAd(
                adUnitId = UNIT,
                revenue = 0.0123,
                networkName = "AppLovin",
                waterfall = threeNetworks(),
                throwing = setOf("getRevenue", "getRevenuePrecision", "getWaterfall"),
            ),
        )

        assertEquals(0.0, info.revenueUsd, 0.0)
        assertNull(info.revenuePrecision)
        assertNull(info.adapterClassName)
        assertTrue(info.networks.isEmpty())
        // The getters that work are still read.
        assertEquals(UNIT, info.adUnitId)
        assertEquals("AppLovin", info.networkName)

        val noUnit = maxAdInfoOf(FakeMaxAd(revenue = 0.5, throwing = setOf("getAdUnitId", "getNetworkName")))
        assertEquals("", noUnit.adUnitId)
        assertNull(noUnit.networkName)
        assertEquals(0.5, noUnit.revenueUsd, 0.0)
    }

    @Test
    fun revenueValue_negativeNaNInfinite_zero_elseAsIs() {
        assertEquals(0.0, maxRevenueValue(-1.0), 0.0)
        assertEquals(0.0, maxRevenueValue(Double.NaN), 0.0)
        assertEquals(0.0, maxRevenueValue(Double.POSITIVE_INFINITY), 0.0)
        assertEquals(0.0, maxRevenueValue(Double.NEGATIVE_INFINITY), 0.0)
        // Dollars as is: never divided by a million.
        assertEquals(0.0123, maxRevenueValue(0.0123), 0.0)
        assertEquals(0.0, maxRevenueValue(0.0), 0.0)
        assertEquals(12.5, maxRevenueValue(12.5), 0.0)
        assertEquals(0.0, maxAdInfoOf(FakeMaxAd(revenue = -0.2)).revenueUsd, 0.0)
    }

    @Test
    fun attempts_inSdkOrder_nameLatencyResultCode() {
        assertEquals(
            listOf(
                MaxNetworkAttempt("AppLovin", 80, MaxNetworkResult.FAILED, -5208),
                MaxNetworkAttempt("Google", 120, MaxNetworkResult.LOADED, null),
                MaxNetworkAttempt("Mintegral", 0, MaxNetworkResult.NOT_TRIED, null),
            ),
            maxNetworkAttemptsOf(threeNetworks()),
        )
    }

    @Test
    fun attempts_nameFallsBackToAdapterThenUnknown() {
        val waterfall = FakeMaxWaterfall(
            listOf(
                response(MINTEGRAL_ADAPTER, "", AdLoadState.FAILED_TO_LOAD, error = FakeMaxError(-5208)),
                response(MINTEGRAL_ADAPTER, null, AdLoadState.AD_LOAD_NOT_ATTEMPTED),
                response("", "", AdLoadState.AD_LOAD_NOT_ATTEMPTED),
                FakeMaxNetworkResponse(null, 5, AdLoadState.AD_LOAD_NOT_ATTEMPTED),
            ),
        )

        assertEquals(
            listOf(MINTEGRAL_ADAPTER, MINTEGRAL_ADAPTER, "unknown", "unknown"),
            maxNetworkAttemptsOf(waterfall).map { it.networkName },
        )
    }

    @Test
    fun attempts_neverReadCredentialsOrMessages() {
        // The fakes throw AssertionError on getCredentials, getMessage, getMediatedNetworkErrorMessage and
        // getAdLoadFailureInfo: reading one fails this test.
        val attempts = maxNetworkAttemptsOf(
            FakeMaxWaterfall(
                listOf(
                    response(APPLOVIN_ADAPTER, "AppLovin", AdLoadState.AD_LOADED),
                    response(MINTEGRAL_ADAPTER, "Mintegral", AdLoadState.FAILED_TO_LOAD, FakeMaxError(-5208)),
                ),
            ),
        )
        // Control: the networks were read.
        assertEquals(listOf("AppLovin", "Mintegral"), attempts.map { it.networkName })
        assertEquals(listOf(null, -5208), attempts.map { it.errorCode })
    }

    @Test
    fun attempts_nullWaterfall_empty() {
        assertTrue(maxNetworkAttemptsOf(null).isEmpty())
        // Control: a waterfall of one network gives one attempt.
        val one = FakeMaxWaterfall(listOf(response(APPLOVIN_ADAPTER, "AppLovin", AdLoadState.AD_LOADED)))
        assertEquals(1, maxNetworkAttemptsOf(one).size)
    }

    @Test
    fun lines_format_loadedErrorNotTried_noneTried() {
        assertEquals(
            listOf(
                "Network preload:interstitial/max: AppLovin, 80 ms, error -5208",
                "Network preload:interstitial/max: Google, 120 ms, loaded",
                "Network preload:interstitial/max: Mintegral, 0 ms, not tried",
            ),
            maxNetworkLines(KEY, maxNetworkAttemptsOf(threeNetworks())),
        )
        assertEquals(listOf("Network $KEY: none tried"), maxNetworkLines(KEY, emptyList()))
    }

    @Test
    fun adInfo_toString_hasNoAdUnit() {
        val text = maxAdInfoOf(FakeMaxAd(adUnitId = UNIT, networkName = "AppLovin", waterfall = threeNetworks()))
            .toString()

        assertFalse(text, UNIT in text)
        // Control: the text says which network served the ad.
        assertTrue(text, "AppLovin" in text)
    }

    @Test
    fun adInfo_loadStateGetterThrows_noCrash_adapterOfLoadedNetwork() {
        val throwing = FakeMaxNetworkResponse(
            FakeMaxNetwork("AppLovin", APPLOVIN_ADAPTER),
            10,
            AdLoadState.AD_LOADED,
            stateThrows = true,
        )
        val waterfall = FakeMaxWaterfall(listOf(throwing, response(GOOGLE_ADAPTER, "Google", AdLoadState.AD_LOADED)))

        val info = maxAdInfoOf(FakeMaxAd(revenue = 0.0123, waterfall = waterfall))

        assertEquals(0.0123, info.revenueUsd, 0.0)
        assertEquals(GOOGLE_ADAPTER, info.adapterClassName)
        assertEquals(2, info.networks.size)
    }

    /** AppLovin failed (-5208), Google loaded, Mintegral not tried: the order of the SDK. */
    private fun threeNetworks() = FakeMaxWaterfall(
        listOf(
            response(APPLOVIN_ADAPTER, "AppLovin", AdLoadState.FAILED_TO_LOAD, FakeMaxError(-5208), latency = 80),
            response(GOOGLE_ADAPTER, "Google", AdLoadState.AD_LOADED, latency = 120),
            response(MINTEGRAL_ADAPTER, "Mintegral", AdLoadState.AD_LOAD_NOT_ATTEMPTED, latency = 0),
        ),
    )

    private fun response(
        adapter: String?,
        name: String?,
        state: AdLoadState,
        error: FakeMaxError? = null,
        latency: Long = 10,
    ) = FakeMaxNetworkResponse(FakeMaxNetwork(name, adapter), latency, state, error)

    private companion object {
        const val UNIT = "FAKE_MAX_UNIT_0123"
        const val KEY = "preload:interstitial/max"
        const val APPLOVIN_ADAPTER = "com.applovin.mediation.adapters.AppLovinMediationAdapter"
        const val GOOGLE_ADAPTER = "com.applovin.mediation.adapters.GoogleMediationAdapter"
        const val MINTEGRAL_ADAPTER = "com.applovin.mediation.adapters.MintegralMediationAdapter"
    }
}
