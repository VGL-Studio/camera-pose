package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.analytics.AdFunnelKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The pure mappings of the ad metrics (docs/ops/ads-business-rules.md, sections 8.1 and 8.2; plan T-023). */
class AdTelemetryRulesTest {
    @Test
    fun revenueValue_microsToUnits() {
        assertEquals(0.0123, revenueValue(12_300), 0.0)
        assertEquals(0.0, revenueValue(0), 0.0)
        assertEquals("admob", adSourceOrAdmob(null))
        assertEquals("admob", adSourceOrAdmob(""))
        assertEquals("Pangle", adSourceOrAdmob("Pangle"))
    }

    @Test
    fun funnelKind_coversEveryFormat() {
        // Copied by hand from section 8.3: app open and banners have no funnel.
        val expected = mapOf(
            AdFormat.INTERSTITIAL to AdFunnelKind.INTERS,
            AdFormat.REWARDED to AdFunnelKind.REWARDED,
            AdFormat.NATIVE_FULL_SCREEN to AdFunnelKind.NATIVE,
            AdFormat.NATIVE_COLLAPSIBLE to AdFunnelKind.NATIVE,
            AdFormat.NATIVE_SMALL to AdFunnelKind.NATIVE,
            AdFormat.NATIVE_STEP to AdFunnelKind.NATIVE,
            AdFormat.NATIVE_IN_COLLECTION to AdFunnelKind.NATIVE,
            AdFormat.NATIVE_MEDIUM to AdFunnelKind.NATIVE,
            AdFormat.APP_OPEN to null,
            AdFormat.BANNER to null,
            AdFormat.BANNER_COLLAPSIBLE to null,
            AdFormat.BANNER_MREC to null,
        )
        assertEquals(AdFormat.entries.toSet(), expected.keys)
        expected.forEach { (format, kind) -> assertEquals(format.name, kind, format.funnelKind()) }
    }

    @Test
    fun metricCode_coversEveryFormat() {
        // Copied by hand from section 3.1 of plan T-023 (T-04).
        val expected = mapOf(
            AdFormat.INTERSTITIAL to "INTER",
            AdFormat.REWARDED to "REWARDED",
            AdFormat.APP_OPEN to "APP_OPEN",
            AdFormat.BANNER to "BANNER",
            AdFormat.BANNER_COLLAPSIBLE to "BANNER",
            AdFormat.NATIVE_FULL_SCREEN to "NATIVE_FULL",
            AdFormat.NATIVE_COLLAPSIBLE to "NATIVE_COLLAP",
            AdFormat.NATIVE_SMALL to "NATIVE_SMALL",
            AdFormat.NATIVE_STEP to "NATIVE_ONBOARDING",
            AdFormat.NATIVE_IN_COLLECTION to "NATIVE_CONTENT",
            AdFormat.NATIVE_MEDIUM to "NATIVE_MEDIUM",
            AdFormat.BANNER_MREC to "BANNER_MREC",
        )
        assertEquals(AdFormat.entries.toSet(), expected.keys)
        expected.forEach { (format, code) -> assertEquals(format.name, code, format.metricCode()) }
        assertEquals(
            setOf(
                "INTER", "REWARDED", "APP_OPEN", "BANNER", "NATIVE_FULL", "NATIVE_COLLAP", "NATIVE_SMALL",
                "NATIVE_ONBOARDING", "NATIVE_CONTENT", "NATIVE_MEDIUM", "BANNER_MREC",
            ),
            AdFormat.entries.map { it.metricCode() }.toSet(),
        )
    }

    @Test
    fun metricGroup_fullScreenFour() {
        assertEquals(
            setOf(AdFormat.INTERSTITIAL, AdFormat.REWARDED, AdFormat.APP_OPEN, AdFormat.NATIVE_FULL_SCREEN),
            AdFormat.entries.filter { it.metricGroup() == MetricGroup.FULL_SCREEN }.toSet(),
        )
        assertEquals(MetricGroup.SLOT, AdFormat.NATIVE_SMALL.metricGroup())
        assertEquals(MetricGroup.SLOT, AdFormat.BANNER.metricGroup())
    }

    @Test
    fun loadStateAfterError_table() {
        (0..11).forEach { code ->
            assertEquals("offline $code", "network", loadStateAfterError(code, offline = true, sdkThrottled = true))
            assertEquals("offline $code", "network", loadStateAfterError(code, offline = true, sdkThrottled = false))
        }
        assertEquals("no_fill", loadStateAfterError(3, offline = false, sdkThrottled = false))
        assertEquals("no_fill", loadStateAfterError(9, offline = false, sdkThrottled = false))
        assertEquals("sdk_throttled", loadStateAfterError(1, offline = false, sdkThrottled = true))
        assertEquals("invalid", loadStateAfterError(1, offline = false, sdkThrottled = false))
        listOf(8, 10, 11).forEach { assertEquals("code $it", "invalid", loadStateAfterError(it, false, false)) }
        assertEquals("internal", loadStateAfterError(0, offline = false, sdkThrottled = false))
        assertEquals("unknown", loadStateAfterError(2, offline = false, sdkThrottled = false))
        assertEquals("unknown", loadStateAfterError(7, offline = false, sdkThrottled = false))
    }

    @Test
    fun maxLoadStateAfterError_table() {
        // Plan T-071, question 2: the codes of com.applovin.mediation.MaxErrorCode, read with javap (13.6.4).
        assertEquals("no_fill", maxLoadStateAfterError(204, offline = false))
        listOf(-1000, -1001, -1009).forEach { assertEquals("code $it", "network", maxLoadStateAfterError(it, false)) }
        assertEquals("internal", maxLoadStateAfterError(-1, offline = false))
        listOf(-5001, -5603, 0, 3, -9000).forEach {
            assertEquals("code $it", "unknown", maxLoadStateAfterError(it, offline = false))
        }
        assertEquals("network", maxLoadStateAfterError(204, offline = true))
        assertEquals("network", maxLoadStateAfterError(-1, offline = true))
        // Control: a code of AdMob keeps its meaning for AdMob only.
        assertEquals("no_fill", loadStateAfterError(3, offline = false, sdkThrottled = false))
        assertEquals("unknown", maxLoadStateAfterError(3, offline = false))
    }

    @Test
    fun loadStateAtDecision_snapshot() {
        assertEquals("ready", loadStateAtDecision(FullScreenLoadState.Ready, lastErrorLabel = "no_fill"))
        assertEquals("loading", loadStateAtDecision(FullScreenLoadState.Loading, lastErrorLabel = "no_fill"))
        assertEquals("no_fill", loadStateAtDecision(FullScreenLoadState.Idle, lastErrorLabel = "no_fill"))
        assertEquals("idle", loadStateAtDecision(FullScreenLoadState.Idle, lastErrorLabel = null))
        val waiting = FullScreenLoadState.WaitingRetry(5_000)
        assertEquals("network", loadStateAtDecision(waiting, lastErrorLabel = "network"))
    }

    @Test
    fun showFailReason_table() {
        assertEquals("internal", showFailReasonOf(0))
        assertEquals("ad_reused", showFailReasonOf(1))
        assertEquals("not_ready", showFailReasonOf(2))
        assertEquals("app_not_foreground", showFailReasonOf(3))
        assertEquals("mediation_show_error", showFailReasonOf(4))
        assertEquals("unknown", showFailReasonOf(9))
    }

    @Test
    fun opportunityResult_ofDecision() {
        assertEquals("shown", FullScreenShowDecision.SHOW.opportunityResult())
        assertEquals("busy", FullScreenShowDecision.BUSY.opportunityResult())
        assertEquals("not_ready", FullScreenShowDecision.NOT_READY.opportunityResult())
        assertEquals("throttled", FullScreenShowDecision.THROTTLED.opportunityResult())
        assertNull(FullScreenShowDecision.CONSENT_CLOSED.opportunityResult())
        assertNull(FullScreenShowDecision.DISABLED.opportunityResult())
    }

    @Test
    fun loadPlacement_table() {
        val screen = AdPlacement.POSES_BOTTOM
        listOf(AdFormat.INTERSTITIAL, AdFormat.NATIVE_FULL_SCREEN, AdFormat.APP_OPEN, AdFormat.REWARDED).forEach {
            assertEquals(it.name, "preload", loadPlacementOf(it, screen))
            assertEquals(it.name, "preload", loadPlacementOf(it, null))
        }
        assertEquals("poses_bottom", loadPlacementOf(AdFormat.NATIVE_SMALL, screen))
        assertEquals("preload", loadPlacementOf(AdFormat.NATIVE_SMALL, null))
        assertEquals("poses_bottom", loadPlacementOf(AdFormat.BANNER, screen))
        assertNull(loadPlacementOf(AdFormat.BANNER, null))
    }

    @Test
    fun loadPlacement_pooledFormats_arePool() {
        // PL-02: a load of a pool is always `pool`, even with a placement of a screen.
        listOf(AdFormat.NATIVE_MEDIUM, AdFormat.NATIVE_STEP, AdFormat.NATIVE_IN_COLLECTION).forEach {
            assertEquals(it.name, "pool", loadPlacementOf(it, AdPlacement.GALLERY_NATIVE_MEDIUM))
            assertEquals(it.name, "pool", loadPlacementOf(it, null))
        }
        // Control: a single native small ad keeps the placement of its screen.
        assertEquals("poses_bottom", loadPlacementOf(AdFormat.NATIVE_SMALL, AdPlacement.POSES_BOTTOM))
    }

    @Test
    fun bannerMrec_slotGroup_loadPlacementOfScreen() {
        // Control: a pooled format loads as `pool`, whatever the screen.
        assertEquals("pool", loadPlacementOf(AdFormat.NATIVE_MEDIUM, AdPlacement.GALLERY_NATIVE_MEDIUM))
        assertEquals(MetricGroup.SLOT, AdFormat.BANNER_MREC.metricGroup())
        assertNull(AdFormat.BANNER_MREC.funnelKind())
        assertEquals("gallery_banner_mrec", loadPlacementOf(AdFormat.BANNER_MREC, AdPlacement.GALLERY_BANNER_MREC))
        assertNull(loadPlacementOf(AdFormat.BANNER_MREC, null))
    }
}
