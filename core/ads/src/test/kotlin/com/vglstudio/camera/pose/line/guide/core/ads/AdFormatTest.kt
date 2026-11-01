package com.vglstudio.camera.pose.line.guide.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class AdFormatTest {
    @Test
    fun fullScreenFormats_areInterstitialAndNativeFullScreen() {
        val fullScreen = AdFormat.entries.filter { it.isFullScreen }
        assertEquals(listOf(AdFormat.INTERSTITIAL, AdFormat.NATIVE_FULL_SCREEN), fullScreen)
        AdFormat.entries.filterNot { it in fullScreen }.forEach { format ->
            assertEquals("$format must not be full screen", false, format.isFullScreen)
        }
    }

    @Test
    fun codes_areTheClosedListOfMc02() {
        // docs/ops/ads-business-rules.md, MC-02, copied by hand.
        val mc02 = setOf(
            "app_open",
            "interstitial",
            "rewarded",
            "banner",
            "banner_collapsible",
            "native_small",
            "native_collap",
            "native_fullscreen",
            "native_step",
            "native_medium",
            "native_in_collection",
            "banner_mrec",
        )
        val codes = AdFormat.entries.map { it.code }
        assertEquals(mc02, codes.toSet())
        assertEquals(codes.size, codes.toSet().size)
    }

    @Test
    fun bannerMrec_codeAndNotFullScreen() {
        // Control: a pooled format has a capacity, so the null below is the banner's own answer (plan T-062).
        assertEquals(3, AdFormat.NATIVE_MEDIUM.poolCapacity())
        assertEquals("banner_mrec", AdFormat.BANNER_MREC.code)
        assertFalse(AdFormat.BANNER_MREC.isFullScreen)
        assertFalse(AdFormat.BANNER_MREC.hasFullScreenSlot)
        assertNull(AdFormat.BANNER_MREC.poolCapacity())
        assertNull(AdFormat.BANNER_MREC.nativeLoadSize())
    }
}
