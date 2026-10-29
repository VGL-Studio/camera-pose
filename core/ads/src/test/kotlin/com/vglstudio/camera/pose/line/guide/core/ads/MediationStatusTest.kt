package com.vglstudio.camera.pose.line.guide.core.ads

import com.google.android.gms.ads.initialization.AdapterStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class MediationStatusTest {
    private fun status(state: AdapterStatus.State, latency: Int, description: String) = object : AdapterStatus {
        override fun getInitializationState() = state

        override fun getDescription() = description

        override fun getLatency() = latency
    }

    @Test
    fun statusLines_listOneLinePerAdapterSortedByName() {
        val lines = adapterStatusLines(
            mapOf(
                "com.google.ads.mediation.applovin.AppLovinMediationAdapter" to
                    status(AdapterStatus.State.NOT_READY, 0, "Timeout"),
                "com.google.android.gms.ads.MobileAds" to status(AdapterStatus.State.READY, 120, "Initialized"),
            ).toSortedMap(reverseOrder()),
        )

        assertEquals(
            listOf(
                "Adapter com.google.ads.mediation.applovin.AppLovinMediationAdapter: NOT_READY, 0 ms, Timeout",
                "Adapter com.google.android.gms.ads.MobileAds: READY, 120 ms, Initialized",
            ),
            lines,
        )
    }

    @Test
    fun statusLines_emptyWhenNoAdapter() {
        // Control: one adapter gives one line, so an empty result below comes from the empty map.
        assertEquals(
            1,
            adapterStatusLines(mapOf("Adapter" to status(AdapterStatus.State.READY, 1, "Ok"))).size,
        )

        assertEquals(emptyList<String>(), adapterStatusLines(emptyMap()))
    }
}
