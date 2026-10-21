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
}
