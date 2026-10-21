package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.ads.FullScreenAdGuard.Decision
import org.junit.Assert.assertEquals
import org.junit.Test

class FullScreenAdGuardTest {
    private val guard = FullScreenAdGuard(quietPeriodMillis = 1_000)

    @Test
    fun firstAd_withoutInteraction_isShown() {
        assertEquals(Decision.Show, guard.decide(now = 10_000, minIntervalMillis = 120_000))
    }

    @Test
    fun secondAd_withinMinInterval_isSkipped() {
        guard.onShown(now = 10_000)
        assertEquals(Decision.TooSoon, guard.decide(now = 129_999, minIntervalMillis = 120_000))
    }

    @Test
    fun secondAd_afterMinInterval_isShown() {
        guard.onShown(now = 10_000)
        assertEquals(Decision.Show, guard.decide(now = 130_000, minIntervalMillis = 120_000))
    }

    @Test
    fun whileUserInteracts_waitsForTheRestOfTheQuietPeriod() {
        guard.onUserInteraction(now = 10_000)
        assertEquals(Decision.Wait(700), guard.decide(now = 10_300, minIntervalMillis = 120_000))
    }

    @Test
    fun afterQuietPeriod_isShown() {
        guard.onUserInteraction(now = 10_000)
        assertEquals(Decision.Show, guard.decide(now = 11_000, minIntervalMillis = 120_000))
    }

    @Test
    fun frequencyCap_winsOverWaiting() {
        guard.onShown(now = 10_000)
        guard.onUserInteraction(now = 20_000)
        assertEquals(Decision.TooSoon, guard.decide(now = 20_100, minIntervalMillis = 120_000))
    }
}
