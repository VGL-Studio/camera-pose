package com.vglstudio.camera.pose.line.guide.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The defaults are business decisions of the product owner (ADR-0014): change them only with a new decision. */
class AdsSettingsTest {
    @Test
    fun placementsAreOffByDefault() {
        assertFalse(AdsSettings.PLACEMENT_ENABLED_DEFAULT)
    }

    @Test
    fun personalizedAdsInRegulatedRegionsAreOnByDefault() {
        assertTrue(AdsSettings.PERSONALIZED_IN_REGULATED_REGIONS_DEFAULT)
    }

    @Test
    fun interstitialMinIntervalIs45SecondsByDefault() {
        assertEquals(45L, AdsSettings.INTERSTITIAL_MIN_INTERVAL_SECONDS_DEFAULT)
    }

    @Test
    fun reviewInterstitialCapDefaults() {
        assertEquals(BreakCap(2, 3, 120), AdsSettings.breakCapDefaults(AdPlacement.REVIEW_INTERSTITIAL))
        assertEquals(BreakCap.NONE, AdsSettings.breakCapDefaults(AdPlacement.SAMPLE_BOTTOM_BANNER))
        val placement = AdPlacement.REVIEW_INTERSTITIAL
        assertEquals("ads_review_interstitial_first_break", AdsSettings.placementFirstBreakKey(placement))
        assertEquals("ads_review_interstitial_breaks_between", AdsSettings.placementBreaksBetweenKey(placement))
        assertEquals("ads_review_interstitial_min_interval_seconds", AdsSettings.placementMinIntervalKey(placement))
    }
}
