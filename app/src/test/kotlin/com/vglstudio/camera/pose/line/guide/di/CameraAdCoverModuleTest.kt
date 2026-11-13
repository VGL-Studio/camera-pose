package com.vglstudio.camera.pose.line.guide.di

import com.vglstudio.camera.pose.line.guide.core.ads.AdsController
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/** The camera reads the full-screen ad flag of the ads controller (spec camera, section 5; plan T-097). */
class CameraAdCoverModuleTest {
    @Test
    fun fullScreenAdCover_followsAdsControllerFlag() {
        val flag = MutableStateFlow(false)
        // Only the flag is declared: reading anything else of the controller throws.
        val ads = mockk<AdsController> { every { fullScreenAdShowing } returns flag }

        val showing = CameraAdCoverModule.fullScreenAdCover(ads).showing()
        assertSame(flag, showing)
        assertEquals(false, showing.value)
        flag.value = true
        assertEquals(true, showing.value)
    }
}
