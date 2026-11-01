package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** The impression flag of an SDK ad (P-01a, plan T-028), set by the listener of its load. */
@RunWith(AndroidJUnit4::class)
class NativeAdLoadListenerTest {
    private val events = mockk<NativeAdEvents>(relaxed = true)

    @Test
    fun onAdImpression_marksLoadedAd_andForwardsEvent() {
        val listener = NativeAdLoadListener(events) {}
        val ad = SdkLoadedNativeAd(mockk<NativeAd>(relaxed = true), events)
        listener.loaded = ad
        assertFalse(ad.impressionRecorded)

        // Control: a click does not set the flag.
        listener.onAdClicked()
        assertFalse(ad.impressionRecorded)

        listener.onAdImpression()

        assertTrue(ad.impressionRecorded)
        verify(exactly = 1) { events.onImpression() }
        verify(exactly = 1) { events.onClicked() }
    }

    @Test
    fun onAdFailedToLoad_handsErrorToResponseLog_beforeOnFailed() {
        // Plan T-058, KB-28: the error of the load reaches the per-network log before the failure is handled.
        val order = mutableListOf<String>()
        val listener = NativeAdLoadListener(
            events,
            onFailed = { order += "failed:$it" },
            onLoadFailedResponse = { order += "response:${it.code}" },
        )

        listener.onAdFailedToLoad(LoadAdError(3, "No fill.", "com.google.android.gms.ads", null, null))

        assertEquals(listOf("response:3", "failed:3"), order)
        verify(exactly = 1) { events.onFailed(3, any()) }
    }

    @Test
    fun impressionBeforeLoaded_doesNotCrash() {
        val listener = NativeAdLoadListener(events) {}

        listener.onAdImpression()

        verify(exactly = 1) { events.onImpression() }
    }
}
