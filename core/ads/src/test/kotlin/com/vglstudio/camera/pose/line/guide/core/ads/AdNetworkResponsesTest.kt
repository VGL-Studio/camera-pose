package com.vglstudio.camera.pose.line.guide.core.ads

import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdapterResponseInfo
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.ResponseInfo
import com.vglstudio.camera.pose.line.guide.core.logging.LogEntry
import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The per-network log of a load at staging (plan T-058, KB-28, answer 40): one line per network the SDK tried, in its
 * order, with the adapter class, the latency and the error code; never an ad unit, a device id or a message of the
 * SDK (ads-business-rules.md, section 8).
 */
@RunWith(AndroidJUnit4::class)
class AdNetworkResponsesTest {
    @Test
    fun lines_onePerNetwork_inSdkOrder() {
        val responses = listOf(
            NetworkResponse(ADMOB, 120, 3),
            NetworkResponse(APPLOVIN, 80, null),
            NetworkResponse(FACEBOOK, 40, null),
        )

        val lines = networkResponseLines("gallery_interstitial", responses, loadedAdapterClassName = APPLOVIN)

        assertEquals(
            listOf(
                "Network gallery_interstitial: $ADMOB, 120 ms, error 3",
                "Network gallery_interstitial: $APPLOVIN, 80 ms, loaded",
                "Network gallery_interstitial: $FACEBOOK, 40 ms, no error",
            ),
            lines,
        )
    }

    @Test
    fun lines_noNetwork_noneTried() {
        assertEquals(
            listOf("Network gallery_interstitial: none tried"),
            networkResponseLines("gallery_interstitial", emptyList(), loadedAdapterClassName = null),
        )
        assertEquals(emptyList<NetworkResponse>(), networkResponsesOf(null))
    }

    @Test
    fun networkResponsesOf_readsOnlyClassLatencyAndCode() {
        val fake = FakeResponseInfo()

        val responses = networkResponsesOf(fake.info)

        assertEquals(listOf(NetworkResponse(ADMOB, 120, 3), NetworkResponse(APPLOVIN, 80, null)), responses)
        fake.adapters.forEach { adapter ->
            verify(exactly = 0) { adapter.credentials }
            verify(exactly = 0) { adapter.adSourceInstanceName }
        }
        verify(exactly = 0) { fake.error.message }
    }

    @Test
    fun log_enabled_writesLines_disabled_writesNothing() {
        val logger = RecordingAppLogger()
        val log = AdNetworkLog(true, logger)

        log.failed("gallery_interstitial", sdkError())
        assertEquals(listOf(LogEntry(LogLevel.INFO, "Ads", "Network gallery_interstitial: none tried")), logger.entries)

        log.loaded("k", FakeResponseInfo().info)
        assertEquals(
            listOf(
                LogEntry(LogLevel.INFO, "Ads", "Network gallery_interstitial: none tried"),
                LogEntry(LogLevel.INFO, "Ads", "Network k: $ADMOB, 120 ms, error 3"),
                LogEntry(LogLevel.INFO, "Ads", "Network k: $APPLOVIN, 80 ms, loaded"),
            ),
            logger.entries,
        )
        assertTrue(logger.entries.toString(), logger.entries.none { "ca-app-pub" in it.message })
        assertTrue(logger.entries.toString(), logger.entries.none { "No fill" in it.message })

        // Control: the same calls write nothing and read nothing when the log is off (production), nor by default.
        val offLogger = RecordingAppLogger()
        val offInfo = FakeResponseInfo()
        AdNetworkLog(false, offLogger).apply {
            failed("gallery_interstitial", sdkError())
            loaded("k", offInfo.info)
        }
        assertEquals(emptyList<LogEntry>(), offLogger.entries)
        val defaultInfo = FakeResponseInfo()
        AdNetworkLog.OFF.loaded("k", defaultInfo.info)
        listOf(offInfo, defaultInfo).forEach { fake -> verify(exactly = 0) { fake.info.adapterResponses } }
    }

    private fun sdkError() = LoadAdError(3, "No fill for $UNIT", "com.google.android.gms.ads", null, null)

    /** A response of two networks: AdMob failed with code 3 (its message has the ad unit), AppLovin loaded. */
    private class FakeResponseInfo {
        val error = mockk<AdError> {
            every { code } returns 3
            every { message } returns "No fill for $UNIT"
        }
        private val credentials = Bundle().apply { putString("parameter", UNIT) }
        private val admob = mockk<AdapterResponseInfo> {
            every { adapterClassName } returns ADMOB
            every { latencyMillis } returns 120
            every { adError } returns error
            every { this@mockk.credentials } returns this@FakeResponseInfo.credentials
            every { adSourceInstanceName } returns UNIT
        }
        private val applovin = mockk<AdapterResponseInfo> {
            every { adapterClassName } returns APPLOVIN
            every { latencyMillis } returns 80
            every { adError } returns null
            every { this@mockk.credentials } returns this@FakeResponseInfo.credentials
            every { adSourceInstanceName } returns UNIT
        }
        val adapters = listOf(admob, applovin)
        val info = mockk<ResponseInfo> {
            every { adapterResponses } returns adapters
            every { loadedAdapterResponseInfo } returns applovin
        }
    }

    private companion object {
        /** Google's test ad unit, as an ad unit inside a message or the credentials of a network. */
        const val UNIT = "ca-app-pub-3940256099942544/1033173712"
        const val ADMOB = "com.google.ads.mediation.admob.AdMobAdapter"
        const val APPLOVIN = "com.google.ads.mediation.applovin.AppLovinMediationAdapter"
        const val FACEBOOK = "com.google.ads.mediation.facebook.FacebookMediationAdapter"
    }
}
