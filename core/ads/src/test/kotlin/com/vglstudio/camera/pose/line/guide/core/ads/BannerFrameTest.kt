package com.vglstudio.camera.pose.line.guide.core.ads

import org.junit.Assert.assertEquals
import org.junit.Test

/** What a banner slot draws and when its ad view runs (B-01, B-04, B-06). */
class BannerFrameTest {
    @Test
    fun frame_table() {
        val rows = listOf(
            Row(gateOpen = false, enabled = true, unitId = "u", hadAd = false) to BannerFrame.NONE,
            Row(gateOpen = true, enabled = true, unitId = "u", hadAd = false) to BannerFrame.ACTIVE,
            Row(gateOpen = false, enabled = true, unitId = "u", hadAd = true) to BannerFrame.KEPT_EMPTY,
            Row(gateOpen = true, enabled = true, unitId = "u", hadAd = true) to BannerFrame.ACTIVE,
            Row(gateOpen = true, enabled = false, unitId = "u", hadAd = true) to BannerFrame.NONE,
            Row(gateOpen = false, enabled = false, unitId = "u", hadAd = true) to BannerFrame.NONE,
            Row(gateOpen = true, enabled = true, unitId = null, hadAd = false) to BannerFrame.NONE,
            Row(gateOpen = true, enabled = true, unitId = "", hadAd = false) to BannerFrame.NONE,
            Row(gateOpen = true, enabled = true, unitId = "  ", hadAd = false) to BannerFrame.NONE,
        )

        rows.forEach { (row, expected) ->
            assertEquals("$row", expected, bannerFrame(row.gateOpen, row.enabled, row.unitId, row.hadAd))
        }
    }

    @Test
    fun shouldRun_onlyWhenVisibleAndActive() {
        assertEquals(true, bannerShouldRun(visible = true, frame = BannerFrame.ACTIVE))
        assertEquals(false, bannerShouldRun(visible = true, frame = BannerFrame.KEPT_EMPTY))
        assertEquals(false, bannerShouldRun(visible = false, frame = BannerFrame.ACTIVE))
        assertEquals(false, bannerShouldRun(visible = true, frame = BannerFrame.NONE))
    }

    @Test
    fun slotHeight_table() {
        val rows = listOf(
            HeightRow(BannerFrame.NONE, BannerSizeKind.MEDIUM_RECTANGLE, failedBeforeAd = false) to 0,
            HeightRow(BannerFrame.NONE, BannerSizeKind.MEDIUM_RECTANGLE, failedBeforeAd = true) to 0,
            HeightRow(BannerFrame.NONE, BannerSizeKind.ANCHORED, failedBeforeAd = false) to 0,
            HeightRow(BannerFrame.NONE, BannerSizeKind.ANCHORED, failedBeforeAd = true) to 0,
            HeightRow(BannerFrame.ACTIVE, BannerSizeKind.MEDIUM_RECTANGLE, failedBeforeAd = false) to 250,
            HeightRow(BannerFrame.ACTIVE, BannerSizeKind.MEDIUM_RECTANGLE, failedBeforeAd = true) to 0,
            // B-06: a kept frame keeps its height.
            HeightRow(BannerFrame.KEPT_EMPTY, BannerSizeKind.MEDIUM_RECTANGLE, failedBeforeAd = true) to 250,
            // Control (L-055): fixed and collapsible banners never shrink on a failure.
            HeightRow(BannerFrame.ACTIVE, BannerSizeKind.ANCHORED, failedBeforeAd = true) to 250,
            HeightRow(BannerFrame.ACTIVE, BannerSizeKind.LARGE_ANCHORED, failedBeforeAd = true) to 250,
        )

        rows.forEach { (row, expected) ->
            assertEquals("$row", expected, bannerSlotHeightDp(row.frame, row.kind, 250, row.failedBeforeAd))
        }
    }

    private data class HeightRow(val frame: BannerFrame, val kind: BannerSizeKind, val failedBeforeAd: Boolean)

    private data class Row(val gateOpen: Boolean, val enabled: Boolean, val unitId: String?, val hadAd: Boolean)
}
