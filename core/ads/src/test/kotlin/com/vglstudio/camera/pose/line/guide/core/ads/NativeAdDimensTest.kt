package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The heights of the native ads (KB-32 criterion 3, design T-064, plans T-065, T-109, T-110): the collapsible layer
 * follows the window, linear between two marks, the floor and the ceiling kept outside them, rounded to the nearest
 * whole dp like the numbers of the design; its total by the short edge, its media area what the content of the small
 * native leaves. The small native no longer follows the window (106 dp at font scale 1.0, plan T-109): its height is
 * tested in [NativeAdFontScaleTest.smallNativeHeight_is106dp_atFontScale1_wholeDp]. Medium no longer follows the
 * window either (plan T-110): 250 dp, or its content block and 120 dp of media when that is more, whole dp.
 */
class NativeAdDimensTest {
    private fun assertDp(what: String, expected: Dp, actual: Dp) = assertEquals(what, expected.value, actual.value, 0f)

    /** Plan T-109, section 3.5: the total of the open layer, 250 dp to 400 dp by the short edge (271 dp at 360). */
    @Test
    fun collapsibleTotalHeight_interpolatesAndClamps() {
        mapOf(300 to 250, 320 to 250, 360 to 271, 411 to 299, 600 to 400, 673 to 400).forEach { (width, height) ->
            assertDp("layer total at $width dp", height.dp, NativeAdDimens.collapsibleTotalHeight(width.dp))
        }
    }

    /**
     * Plan T-110, section 3.5 (KB-32 criterion 3, question 66): medium is 250 dp, or its content block and 120 dp of
     * media when that is more; one function, whole dp.
     */
    @Test
    fun mediumHeight_is250_orContentPlus120() {
        // Control at the threshold: 130 dp of content still gives 250 dp, 131 dp gives 251 dp.
        assertTrue(
            "130 and 131 dp of content give different cards",
            NativeAdDimens.mediumHeight(130.dp) != NativeAdDimens.mediumHeight(131.dp),
        )
        mapOf(106 to 250, 118 to 250, 130 to 250, 131 to 251, 180 to 300).forEach { (content, height) ->
            assertDp("medium over $content dp of content", height.dp, NativeAdDimens.mediumHeight(content.dp))
        }
    }

    /**
     * KB-2, KB-32 (plan T-110, section 3.5): with little room the media shrinks first, never below 120 dp; only then
     * does the card grow.
     */
    @Test
    fun mediumMedia_shrinksTo120_beforeCardGrows() {
        // Control at the threshold: 130 and 131 dp of content both leave 120 dp of media, in a 250 and a 251 dp card.
        assertDp("media over 130 dp", 120.dp, NativeAdDimens.mediumMediaHeight(130.dp))
        assertDp("media over 131 dp", 120.dp, NativeAdDimens.mediumMediaHeight(131.dp))
        assertDp("card over 130 dp", 250.dp, NativeAdDimens.mediumHeight(130.dp))
        assertDp("card over 131 dp", 251.dp, NativeAdDimens.mediumHeight(131.dp))
        mapOf(106 to 144, 118 to 132, 130 to 120, 131 to 120, 180 to 120).forEach { (content, media) ->
            assertDp("media over $content dp of content", media.dp, NativeAdDimens.mediumMediaHeight(content.dp))
        }
        (75..200).forEach { content ->
            val card = NativeAdDimens.mediumHeight(content.dp)
            val media = NativeAdDimens.mediumMediaHeight(content.dp)
            assertFalse("media over $content dp is below 120 dp", media < 120.dp)
            if (card > 250.dp) assertDp("media of a grown card over $content dp", 120.dp, media)
        }
    }

    @Test
    fun heights_areWholeDp() {
        // Control: 411 dp gives 299 dp, not the 298.75 dp of the line itself.
        assertDp("layer total at 411 dp", 299.dp, NativeAdDimens.collapsibleTotalHeight(411.dp))
        // Control: 130.25 dp of content gives a 251 dp medium, not the 250.25 dp of content and media.
        assertDp("medium over 130.25 dp", 251.dp, NativeAdDimens.mediumHeight(130.25.dp))
        (300..700).forEach { size ->
            listOf(
                "layer total" to NativeAdDimens.collapsibleTotalHeight(size.dp),
                "medium" to NativeAdDimens.mediumHeight((size / 4f).dp),
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

    private fun assertLayer(width: Int, height: Int, mediaArea: Int, slot: Int, total: Int, content: Dp = SMALL) {
        val layer = NativeAdDimens.collapsibleLayer(window(width, height), content)
        assertNotNull("layer at $width x $height, content $content", layer)
        layer!!
        assertDp("media area at $width x $height, content $content", mediaArea.dp, layer.mediaArea)
        assertDp("slot at $width x $height, content $content", slot.dp, layer.slot)
        assertDp("layer at $width x $height, content $content", total.dp, layer.height)
    }

    /** Plan T-109, section 3.5: the media area is what the total leaves over the 106 dp content. */
    @Test
    fun collapsibleLayer_contentLeftForMedia() {
        assertLayer(411, 891, mediaArea = 193, slot = 106, total = 299)
        assertLayer(411, 700, mediaArea = 193, slot = 106, total = 299)
        // 299 + 16 = 315 dp, just half of 630 dp.
        assertLayer(411, 630, mediaArea = 193, slot = 106, total = 299)
        assertLayer(360, 640, mediaArea = 165, slot = 106, total = 271)
        assertLayer(673, 841, mediaArea = 294, slot = 106, total = 400)
        // Control: the same width 1 dp below the least window with room has no layer at all.
        assertNull(NativeAdDimens.collapsibleLayer(window(411, 507), SMALL))
    }

    @Test
    fun collapsibleLayer_mediaShrinks_toHalfWindow() {
        // 284 + 16 = 300 dp, half of 600 dp.
        assertLayer(411, 600, mediaArea = 178, slot = 106, total = 284)
        // Half of 629 dp is 314.5 dp: rounded down, never past half.
        assertLayer(411, 629, mediaArea = 192, slot = 106, total = 298)
        assertLayer(411, 508, mediaArea = 132, slot = 106, total = 238)
        assertLayer(411, 509, mediaArea = 132, slot = 106, total = 238)
    }

    @Test
    fun collapsibleLayer_null_whenNoRoom() {
        // Control at the threshold: 508 dp still has room (MediaView 120 dp).
        assertNotNull(NativeAdDimens.collapsibleLayer(window(411, 508), SMALL))
        listOf(
            window(411, 507),
            window(411, 445),
            window(411, 411),
            window(891, 411),
            window(640, 360),
            window(320, 470),
        ).forEach { assertNull("no room at $it", NativeAdDimens.collapsibleLayer(it, SMALL)) }
        // Question 5 of plan T-109: an unfolded foldable turned landscape has room, the media area shrunk to half.
        assertLayer(841, 673, mediaArea = 214, slot = 106, total = 320)
    }

    /**
     * KB-32 criterion 3 (question 66): large text that leaves the media below 120 dp makes the layer taller, within
     * half the window.
     */
    @Test
    fun collapsibleLayer_growsWhenContentLeavesMediaBelow120() {
        assertLayer(411, 891, mediaArea = 132, slot = 167, total = 299, content = 167.dp)
        assertLayer(411, 891, mediaArea = 132, slot = 168, total = 300, content = 168.dp)
        assertLayer(411, 891, mediaArea = 132, slot = 180, total = 312, content = 180.dp)
        assertLayer(411, 700, mediaArea = 132, slot = 180, total = 312, content = 180.dp)
        assertNull("no room at 411 x 600, content 180 dp", NativeAdDimens.collapsibleLayer(window(411, 600), 180.dp))
    }

    @Test
    fun collapsibleLayer_neverPastHalf_mediaAtLeast120() {
        var none = 0
        var shrunk = 0
        for (content in listOf(106.dp, 140.dp, 180.dp)) {
            for (width in 300..700) {
                for (height in 300..1000) {
                    val size = window(width, height)
                    val layer = NativeAdDimens.collapsibleLayer(size, content)
                    if (layer == null) {
                        none++
                        continue
                    }
                    val design = max(NativeAdDimens.collapsibleTotalHeight(size.shortEdge) - content, 132.dp)
                    if (layer.mediaArea < design) shrunk++
                    val case = "$size, content $content"
                    assertTrue("$case: layer and margin past half", (layer.height + 16.dp).value <= height / 2f)
                    assertTrue("$case: media area below 132 dp", layer.mediaArea >= 132.dp)
                    assertTrue("$case: media area above the design", layer.mediaArea <= design)
                    assertDp("$case: slot", content, layer.slot)
                    assertEquals(
                        "$case: whole dp",
                        layer.mediaArea.value.roundToInt().toFloat(),
                        layer.mediaArea.value,
                        0f,
                    )
                }
            }
        }
        // Control: the grid has windows with no room and windows where the media shrinks.
        assertTrue("windows with no room: $none", none > 0)
        assertTrue("windows with the media shrunk: $shrunk", shrunk > 0)
    }

    private companion object {
        /** The content of the small native at font scale 1.0 (plan T-109). */
        val SMALL = 106.dp
    }
}
