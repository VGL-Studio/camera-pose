package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.ads.nativead.NativeAd
import io.mockk.mockk
import io.mockk.verify
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
    fun impressionBeforeLoaded_doesNotCrash() {
        val listener = NativeAdLoadListener(events) {}

        listener.onAdImpression()

        verify(exactly = 1) { events.onImpression() }
    }
}
