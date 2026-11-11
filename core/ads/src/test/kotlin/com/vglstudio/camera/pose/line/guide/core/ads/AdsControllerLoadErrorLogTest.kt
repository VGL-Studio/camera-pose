package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The real [AdsController] builds its [LoadErrorLog] from [AdsDebugOptions] (plan T-093, KB-28): the masked SDK message
 * at staging, the code only in production.
 */
@RunWith(AndroidJUnit4::class)
class AdsControllerLoadErrorLogTest {
    @Test
    fun staging_logsMessage() {
        val controller = testAdsController(debugOptions = AdsDebugOptions.staging(""))

        assertEquals("3 \"No ad for <ad unit>.\"", controller.loadErrorLog.text(3, ADMOB_UNIT_MESSAGE))
    }

    @Test
    fun production_codeOnly() {
        val controller = testAdsController()

        assertEquals("3", controller.loadErrorLog.text(3, ADMOB_UNIT_MESSAGE))
    }
}
