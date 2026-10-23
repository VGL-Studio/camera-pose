package com.vglstudio.camera.pose.line.guide.core.designsystem

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.MatchHint
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The pose matching hint (docs/design-system/sticker.md, section 4; camera design README, section 3; plan T-017): a
 * pill 56 high with a 40 dp circle that holds the score (or an icon) and a short text.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class MatchHintTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setHint(
        text: String = TEXT,
        score: Int? = null,
        matched: Boolean = false,
        icon: ColorPainter? = null,
    ) {
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                MatchHint(
                    text = text,
                    modifier = Modifier.testTag(HINT),
                    score = score,
                    matched = matched,
                    icon = icon,
                )
            }
        }
    }

    private fun screen(): Bitmap {
        composeRule.waitForIdle()
        val root = (composeRule as AndroidComposeTestRule<*, *>).activity.window.decorView
        return Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888).also { root.draw(Canvas(it)) }
    }

    private fun Bitmap.at(x: Dp, y: Dp): Color {
        val bounds = composeRule.onNodeWithTag(HINT).fetchSemanticsNode().boundsInWindow
        with(composeRule.density) {
            return Color(getPixel(bounds.left.toInt() + x.roundToPx(), bounds.top.toInt() + y.roundToPx()))
        }
    }

    private fun isNear(expected: Color, actual: Color): Boolean {
        fun near(a: Float, b: Float) = abs(a - b) * 255f <= TOLERANCE
        return near(expected.red, actual.red) && near(expected.green, actual.green) && near(expected.blue, actual.blue)
    }

    private fun assertColor(expected: Color, actual: Color, what: String) =
        assertTrue("$what: expected $expected, got $actual", isNear(expected, actual))

    @Test
    fun height56_circle40() {
        setHint(score = 68)
        composeRule.onNodeWithTag(HINT).assertHeightIsEqualTo(56.dp)
        // Along the middle line: the circle is 40 dp including its 2 dp border, so 36 dp of yellow, after the left
        // margin of 8 dp (the design's 6 rounded up to the 4 dp grid).
        val bitmap = screen()
        val bounds = composeRule.onNodeWithTag(HINT).fetchSemanticsNode().boundsInWindow
        val density = composeRule.density.density
        val y = bounds.center.y.toInt()
        val yellow = (bounds.left.toInt() until bounds.right.toInt()).filter { x ->
            isNear(YELLOW, Color(bitmap.getPixel(x, y)))
        }
        assertTrue("no yellow in the pill", yellow.isNotEmpty())
        val width = (yellow.last() - yellow.first() + 1) / density
        assertEquals("yellow run $width dp", 36f, width, 1f)
        val start = (yellow.first() - bounds.left.toInt()) / density
        assertEquals("yellow starts at $start dp", 10f, start, 1f)
    }

    @Test
    fun scoring_isWhiteWithYellowCircle() {
        setHint(score = 68)
        val bitmap = screen()
        assertColor(YELLOW, bitmap.at(30.dp, 28.dp), "circle")
        assertColor(Color.White, bitmap.at(5.dp, 28.dp), "pill")
        assertTrue(composeRule.onAllNodesWithText("68", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun matched_isGreenWithWhiteCircle() {
        setHint(score = 92, matched = true)
        val bitmap = screen()
        assertColor(GREEN, bitmap.at(5.dp, 28.dp), "pill")
        assertColor(Color.White, bitmap.at(30.dp, 28.dp), "circle")
        assertTrue(composeRule.onAllNodesWithText("92", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun icon_insteadOfScore() {
        setHint(text = "Lùi lại", score = null, icon = ColorPainter(Color.Red))
        val bitmap = screen()
        // The icon is tinted dark and drawn in the middle of a white circle; no number.
        assertColor(DARK, bitmap.at(32.dp, 28.dp), "icon")
        assertColor(Color.White, bitmap.at(15.dp, 28.dp), "circle")
        val digits = texts().filter { text -> text.any { it.isDigit() } }
        assertTrue("texts $digits", digits.isEmpty())
        assertTrue(texts().contains("Lùi lại"))
    }

    @Test
    fun semantics_readsScoreAndText() {
        setHint(score = 68)
        val texts = texts()
        assertTrue("texts $texts", texts.contains("68") && texts.contains(TEXT))
        // One node for the whole pill: the text and the score are not separate stops for a screen reader.
        val node = composeRule.onNodeWithTag(HINT).fetchSemanticsNode()
        assertTrue(node.config.getOrNull(SemanticsProperties.Text).orEmpty().size >= 2)
    }

    @Test
    fun liveRegion_isPolite() {
        setHint(score = 68)
        val node = composeRule.onNodeWithTag(HINT).fetchSemanticsNode()
        assertEquals(LiveRegionMode.Polite, node.config.getOrNull(SemanticsProperties.LiveRegion))
    }

    private fun texts(): List<String> =
        composeRule.onNodeWithTag(HINT).fetchSemanticsNode().config.getOrNull(SemanticsProperties.Text)
            ?.map { it.text }.orEmpty()

    private companion object {
        const val HINT = "match_hint"
        const val TEXT = "Nâng tay trái cao hơn"
        const val TOLERANCE = 4f
        val YELLOW = Color(0xFFFFC83D)
        val GREEN = Color(0xFF3DDC97)
        val DARK = Color(0xFF111111)
    }
}
