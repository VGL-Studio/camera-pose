package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The three heights of the native ads that follow the screen (KB-32 criterion 3, design T-064, plan T-065): linear
 * between two marks, the floor and the ceiling kept outside them, rounded to the nearest whole dp like the numbers of
 * the design. One function each for the drawn ad, its placeholder and the space kept by the bottom bar.
 */
class NativeAdDimensTest {
    private fun assertDp(what: String, expected: Dp, actual: Dp) = assertEquals(what, expected.value, actual.value, 0f)

    @Test
    fun bottomSlotHeight_interpolatesAndClamps() {
        mapOf(300 to 90, 320 to 90, 360 to 106, 411 to 126, 600 to 200, 673 to 200).forEach { (width, height) ->
            assertDp("bottom slot at $width dp", height.dp, NativeAdDimens.bottomSlotHeight(width.dp))
        }
    }

    @Test
    fun collapsibleMediaAreaHeight_interpolatesAndClamps() {
        mapOf(320 to 160, 360 to 166, 411 to 173, 600 to 200, 673 to 200).forEach { (width, height) ->
            assertDp("media area at $width dp", height.dp, NativeAdDimens.collapsibleMediaAreaHeight(width.dp))
        }
    }

    @Test
    fun mediumHeight_interpolatesAndClamps() {
        mapOf(600 to 320, 640 to 320, 841 to 384, 891 to 400, 1000 to 400).forEach { (screenHeight, height) ->
            assertDp("medium at $screenHeight dp", height.dp, NativeAdDimens.mediumHeight(screenHeight.dp))
        }
    }

    /** KB-2, KB-32, question 7 of the plan: the low frame of medium is a card below 350 dp. */
    @Test
    fun mediumIsCompact_belowCard350dp() {
        // Control: the two screens on each side of the threshold give 349 and 350 dp.
        assertDp("medium at 732 dp", 349.dp, NativeAdDimens.mediumHeight(732.dp))
        assertDp("medium at 733 dp", 350.dp, NativeAdDimens.mediumHeight(733.dp))
        listOf(600, 640, 732).forEach {
            assertTrue("$it dp is the low frame", NativeAdDimens.mediumIsCompact(it.dp))
        }
        listOf(733, 841, 891).forEach {
            assertFalse("$it dp is not the low frame", NativeAdDimens.mediumIsCompact(it.dp))
        }
    }

    @Test
    fun heights_areWholeDp() {
        // Control: 411 dp gives 126 dp, not the 125.75 dp of the line itself.
        assertDp("bottom slot at 411 dp", 126.dp, NativeAdDimens.bottomSlotHeight(411.dp))
        (300..700).forEach { size ->
            listOf(
                "bottom slot" to NativeAdDimens.bottomSlotHeight(size.dp),
                "media area" to NativeAdDimens.collapsibleMediaAreaHeight(size.dp),
                "medium" to NativeAdDimens.mediumHeight(size.dp),
            ).forEach { (what, height) ->
                assertEquals("$what at $size dp", height.value.roundToInt().toFloat(), height.value, 0f)
            }
        }
    }

    // The short edge of the window and the ceiling of the collapsible layer (KB-4, KB-32 criterion 3, 2026-11-16).

    private fun window(width: Int, height: Int) = DpSize(width.dp, height.dp)

    @Test
    fun shortEdge_isSmallerSide() {
        mapOf(
            window(411, 891) to 411,
            window(891, 411) to 411,
            window(411, 445) to 411,
            window(673, 841) to 673,
            window(841, 673) to 673,
            window(360, 360) to 360,
        ).forEach { (size, edge) -> assertDp("short edge of $size", edge.dp, size.shortEdge) }
    }

    private fun assertLayer(width: Int, height: Int, mediaArea: Int, slot: Int, total: Int) {
        val layer = NativeAdDimens.collapsibleLayer(window(width, height))
        assertNotNull("layer at $width x $height", layer)
        layer!!
        assertDp("media area at $width x $height", mediaArea.dp, layer.mediaArea)
        assertDp("slot at $width x $height", slot.dp, layer.slot)
        assertDp("layer at $width x $height", total.dp, layer.height)
    }

    @Test
    fun collapsibleLayer_keepsDesign_whenRoom() {
        assertLayer(411, 891, mediaArea = 173, slot = 126, total = 299)
        assertLayer(411, 700, mediaArea = 173, slot = 126, total = 299)
        // 299 + 16 = 315 dp, just half of 630 dp.
        assertLayer(411, 630, mediaArea = 173, slot = 126, total = 299)
        assertLayer(360, 640, mediaArea = 166, slot = 106, total = 272)
        assertLayer(673, 841, mediaArea = 200, slot = 200, total = 400)
        // Control: the same width 1 dp below the least window with room has no layer at all.
        assertNull(NativeAdDimens.collapsibleLayer(window(411, 547)))
    }

    @Test
    fun collapsibleLayer_mediaShrinks_toHalfWindow() {
        // 284 + 16 = 300 dp, half of 600 dp.
        assertLayer(411, 600, mediaArea = 158, slot = 126, total = 284)
        // Half of 629 dp is 314.5 dp: rounded down, never past half.
        assertLayer(411, 629, mediaArea = 172, slot = 126, total = 298)
        assertLayer(411, 548, mediaArea = 132, slot = 126, total = 258)
        assertLayer(411, 549, mediaArea = 132, slot = 126, total = 258)
    }

    @Test
    fun collapsibleLayer_null_whenNoRoom() {
        // Control at the threshold: 548 dp still has room (MediaView 120 dp).
        assertNotNull(NativeAdDimens.collapsibleLayer(window(411, 548)))
        listOf(
            window(411, 547),
            window(411, 524),
            window(411, 445),
            window(411, 411),
            window(891, 411),
            window(640, 360),
            window(841, 673),
            window(320, 470),
        ).forEach { assertNull("no room at $it", NativeAdDimens.collapsibleLayer(it)) }
    }

    @Test
    fun collapsibleLayer_neverPastHalf_mediaAtLeast120() {
        var none = 0
        var shrunk = 0
        for (width in 300..700) {
            for (height in 300..1000) {
                val size = window(width, height)
                val layer = NativeAdDimens.collapsibleLayer(size)
                if (layer == null) {
                    none++
                    continue
                }
                val design = NativeAdDimens.collapsibleMediaAreaHeight(size.shortEdge)
                if (layer.mediaArea < design) shrunk++
                assertTrue("$size: layer and margin past half", (layer.height + 16.dp).value <= height / 2f)
                assertTrue("$size: media area below 132 dp", layer.mediaArea >= 132.dp)
                assertTrue("$size: media area above the design", layer.mediaArea <= design)
                assertDp("$size: slot", NativeAdDimens.bottomSlotHeight(size.shortEdge), layer.slot)
                assertEquals("$size: whole dp", layer.mediaArea.value.roundToInt().toFloat(), layer.mediaArea.value, 0f)
            }
        }
        // Control: the grid has windows with no room and windows where the media shrinks.
        assertTrue("windows with no room: $none", none > 0)
        assertTrue("windows with the media shrunk: $shrunk", shrunk > 0)
    }
}
