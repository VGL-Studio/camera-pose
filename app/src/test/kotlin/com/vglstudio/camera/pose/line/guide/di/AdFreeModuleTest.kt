package com.vglstudio.camera.pose.line.guide.di

import com.vglstudio.camera.pose.line.guide.core.ads.AdFreeStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test

/** The binding of the ad-free signal (KB-30, question 39 of the ads spec): the template sells no ad removal. */
class AdFreeModuleTest {
    @Test
    fun default_neverAdFree() {
        val status = AdFreeModule.adFreeStatus()
        assertFalse(status.adFree.value)
        assertSame(AdFreeStatus.NONE, status)
    }
}
