package com.vglstudio.camera.pose.line.guide.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The placements of this app in the AdsKit framework (spec ads, questions 2 and 4; spec camera and poses, section 5):
 * declared with their spec format, switched by `is_show_<code>`, and no sample banner.
 */
class AdPlacementAppTest {
    @Test
    fun appPlacements_declaredWithSpecFormats() {
        val expected = mapOf(
            "poses_banner" to AdFormat.BANNER,
            "poses_detail_banner" to AdFormat.BANNER,
            "review_interstitial" to AdFormat.INTERSTITIAL,
        )
        val byKey = AdPlacement.entries.associateBy { it.key }
        expected.forEach { (key, format) ->
            val placement = byKey[key]
            assertTrue("$key is not declared", placement != null)
            assertEquals(key, format, placement!!.format)
            assertFalse("$key must not be app-wide", placement.appWide)
            assertFalse("$key must not be staging only", placement.stagingOnly)
            assertEquals("is_show_$key", AdsSettings.placementEnabled(placement).name)
            assertEquals(false, AdsSettings.placementEnabled(placement).default)
        }
        assertEquals(AdPlacement.POSES_BANNER, byKey["poses_banner"])
        assertEquals(AdPlacement.POSES_DETAIL_BANNER, byKey["poses_detail_banner"])
        assertEquals(AdPlacement.REVIEW_INTERSTITIAL, byKey["review_interstitial"])
    }

    @Test
    fun noSampleBottomBanner() {
        // Control: the sample placement of the framework is there, so the list read is the real one.
        assertTrue(AdPlacement.entries.any { it.key == "sample_native_small" })
        assertTrue(AdPlacement.entries.none { it.key == "sample_bottom_banner" })
    }
}
