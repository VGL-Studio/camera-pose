package com.vglstudio.camera.pose.line.guide.core.analytics.di

import com.vglstudio.camera.pose.line.guide.core.analytics.AdFunnelEvent
import com.vglstudio.camera.pose.line.guide.core.analytics.AdFunnelKind
import com.vglstudio.camera.pose.line.guide.core.analytics.AppsFlyerConfig
import com.vglstudio.camera.pose.line.guide.core.analytics.AppsFlyerSwitches
import com.vglstudio.camera.pose.line.guide.core.analytics.AppsFlyerTracker
import com.vglstudio.camera.pose.line.guide.core.analytics.FakeAppForeground
import com.vglstudio.camera.pose.line.guide.core.analytics.FakeAppsFlyerSink
import com.vglstudio.camera.pose.line.guide.core.analytics.FakeRemoteSettings
import com.vglstudio.camera.pose.line.guide.core.analytics.FakeTenjinSink
import com.vglstudio.camera.pose.line.guide.core.analytics.FixedCountry
import com.vglstudio.camera.pose.line.guide.core.analytics.TenjinConfig
import com.vglstudio.camera.pose.line.guide.core.analytics.TenjinSwitches
import com.vglstudio.camera.pose.line.guide.core.analytics.TenjinTracker
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** The ad funnel of the app reaches AppsFlyer, then Tenjin (plan T-100, step 6). */
@OptIn(ExperimentalCoroutinesApi::class)
class AdMetricsModuleTest {
    @Test
    fun adFunnel_reachesAppsFlyerAndTenjin() = runTest(UnconfinedTestDispatcher()) {
        val settings = FakeRemoteSettings()
        val logger = RecordingAppLogger()
        val appsFlyerSink = FakeAppsFlyerSink()
        val appsFlyer = AppsFlyerTracker(
            config = AppsFlyerConfig("DEV_KEY"),
            sink = appsFlyerSink,
            switches = AppsFlyerSwitches(settings, backgroundScope),
            country = FixedCountry("VN"),
            logger = logger,
        ).apply {
            prepare()
            start()
        }
        val tenjinSink = FakeTenjinSink()
        val tenjin = TenjinTracker(
            config = TenjinConfig("SDK_KEY"),
            sink = tenjinSink,
            switches = TenjinSwitches(settings, backgroundScope),
            foreground = FakeAppForeground(),
            logger = logger,
        ).apply {
            prepare()
            onConsentKnown()
            start()
        }

        AdMetricsModule.adFunnel(appsFlyer, tenjin, logger).log(
            AdFunnelEvent.Displayed(AdFunnelKind.NATIVE, "NATIVE_SMALL", "ca-app-pub-test/1", "x.Adapter"),
        )

        assertEquals(listOf("af_native_ad_displayed"), appsFlyerSink.events.map { it.name })
        assertEquals(listOf("native_ad_displayed"), tenjinSink.events)
    }
}
