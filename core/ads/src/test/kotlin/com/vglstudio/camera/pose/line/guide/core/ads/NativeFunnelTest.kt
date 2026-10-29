package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.analytics.AdFunnelEvent
import com.vglstudio.camera.pose.line.guide.core.analytics.AdFunnelKind
import com.vglstudio.camera.pose.line.guide.core.analytics.RecordingAdFunnel
import com.vglstudio.camera.pose.line.guide.core.analytics.RecordingAdMetrics
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/** The native funnel of one load: `api_called` per load, `displayed` once per ad (section 8.3; plan T-033). */
class NativeFunnelTest {
    private val funnel = RecordingAdFunnel()
    private val telemetry = AdTelemetry(
        metrics = RecordingAdMetrics(),
        funnel = funnel,
        lifecycleSwitches = { DEFAULT_FLAGS },
        serving = { false },
        offline = { false },
        counter = LoadAttemptCounter(),
        logger = RecordingAppLogger(),
    )

    @Test
    fun loaded_sendsApiCalled_withAdapter() {
        NativeFunnel(telemetry, AdFormat.NATIVE_SMALL, "unit").onLoaded("x.Adapter")
        NativeFunnel(telemetry, AdFormat.NATIVE_IN_COLLECTION, "unit2").onLoaded(null)

        assertEquals(
            listOf(
                AdFunnelEvent.ApiCalled(AdFunnelKind.NATIVE, "NATIVE_SMALL", "unit", "x.Adapter"),
                AdFunnelEvent.ApiCalled(AdFunnelKind.NATIVE, "NATIVE_CONTENT", "unit2", ""),
            ),
            funnel.events,
        )
    }

    @Test
    fun impressionTwice_sendsDisplayedOnce() {
        val native = NativeFunnel(telemetry, AdFormat.NATIVE_MEDIUM, "unit")
        native.onLoaded("x.Adapter")
        native.onImpression()
        native.onImpression()

        assertEquals(
            listOf(
                AdFunnelEvent.ApiCalled(AdFunnelKind.NATIVE, "NATIVE_MEDIUM", "unit", "x.Adapter"),
                AdFunnelEvent.Displayed(AdFunnelKind.NATIVE, "NATIVE_MEDIUM", "unit", "x.Adapter"),
            ),
            funnel.events,
        )
    }

    @Test
    fun twoAds_eachSendsDisplayed() {
        // Two loads of one placement overlap (L-068): each ad has its own funnel.
        val first = NativeFunnel(telemetry, AdFormat.NATIVE_SMALL, "unit")
        val second = NativeFunnel(telemetry, AdFormat.NATIVE_SMALL, "unit")
        first.onLoaded("a.Adapter")
        second.onLoaded("b.Adapter")
        second.onImpression()
        first.onImpression()
        first.onImpression()

        val displayed = funnel.events.filterIsInstance<AdFunnelEvent.Displayed>()
        assertEquals(listOf("b.Adapter", "a.Adapter"), displayed.map { it.adSource })
    }

    private companion object {
        val DEFAULT_FLAGS: AdsGlobalFlags = runBlocking { DefaultAdsFlags(FakeRemoteSettings()).global.first() }
    }
}
