package com.vglstudio.camera.pose.line.guide.core.analytics

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The `is_log_af_*` switches, followed as a flow (ADR-0003; docs/ops/ads-business-rules.md, section 10.1). */
@OptIn(ExperimentalCoroutinesApi::class)
class AppsFlyerSwitchesTest {
    private val settings = FakeRemoteSettings()

    @Test
    fun default_isOn_beforeRemoteValue() = runTest(UnconfinedTestDispatcher()) {
        val switches = AppsFlyerSwitches(settings, backgroundScope)

        AppsFlyerSettings.switches().forEach { assertTrue(it.name, switches.isOn(it)) }
        assertEquals(11, AppsFlyerSettings.switches().size)
    }

    @Test
    fun followsRemoteChanges() = runTest(UnconfinedTestDispatcher()) {
        val switches = AppsFlyerSwitches(settings, backgroundScope)
        val key = AppsFlyerSettings.IS_LOG_AF_INTERS_DISPLAYED
        assertTrue(switches.isOn(key))

        settings.set(key, false)
        assertFalse(switches.isOn(key))
        // Control (L-055): another key is still on.
        assertTrue(switches.isOn(AppsFlyerSettings.IS_LOG_AF_INTERS_API_CALLED))
        assertTrue(switches.isOn(AppsFlyerSettings.IS_LOG_AF_AD_REVENUE))

        settings.set(key, true)
        assertTrue(switches.isOn(key))
        // Every switch is followed, read from Remote Config.
        assertEquals(AppsFlyerSettings.all().map { it.name }.toSet(), settings.readNames)
    }
}
