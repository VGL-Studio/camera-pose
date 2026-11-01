package com.vglstudio.camera.pose.line.guide.di

import com.vglstudio.camera.pose.line.guide.BuildConfig
import com.vglstudio.camera.pose.line.guide.core.ads.AdsDebugOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The staging build takes the developer's test devices from its build field and logs every network of a load (plan
 * T-058, KB-28). The value is not checked to be empty: on a developer's machine with the key, it is not.
 */
class StagingAdsModuleTest {
    @Test
    fun adsDebugOptions_readsBuildConfig_andLogsNetworks() {
        val options = StagingAdsModule.adsDebugOptions()

        assertEquals(AdsDebugOptions.staging(BuildConfig.ADS_TEST_DEVICE_IDS), options)
        assertTrue(options.logNetworkResponses)
        // The filter of the build file ran: only letters, digits and commas reach the build field.
        assertTrue(BuildConfig.ADS_TEST_DEVICE_IDS, BuildConfig.ADS_TEST_DEVICE_IDS.matches(Regex("[0-9A-Za-z,]*")))
    }
}
