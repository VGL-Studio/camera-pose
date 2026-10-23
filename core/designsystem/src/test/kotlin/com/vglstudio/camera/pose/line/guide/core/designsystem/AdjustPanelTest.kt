package com.vglstudio.camera.pose.line.guide.core.designsystem

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.AdjustAction
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.AdjustPanel
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.AdjustSlider
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.CameraIconButton
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The adjust panel of the camera: the sheet, its slider and its action (camera design README, section 3; plan T-015).
 * Measured on the pixels of the drawn window (L-006).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class AdjustPanelTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val changes = mutableListOf<Float>()
    private var finished = 0

    @Test
    fun adjustPanel_whiteSheet_radius24_closeButton48() {
        var closes = 0
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                Box(modifier = Modifier.background(Color.Red)) {
                    AdjustPanel(
                        title = "Title",
                        onClose = { closes++ },
                        closeDescription = "Close",
                        modifier = Modifier.width(300.dp).testTag(PANEL),
                    ) {
                        Text(text = "Body")
                    }
                }
            }
        }
        composeRule.onNodeWithText("Title").assertExists()
        composeRule.onNodeWithText("Body").assertExists()
        // Inside the padding: white. In the 2 dp border: dark. Outside the corner of radius 24: what is behind.
        assertEquals(WHITE, pixelAt(PANEL, 150.dp, 8.dp))
        assertEquals(INK, pixelAt(PANEL, 150.dp, 1.dp))
        assertEquals(RED, pixelAt(PANEL, 3.dp, 3.dp))

        composeRule.onNodeWithContentDescription("Close").assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp)
        composeRule.onNodeWithContentDescription("Close").performClick()
        assertEquals(1, closes)
    }

    private fun setSlider(value: Float = 0.5f, valueText: String = "70%") {
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                Box(modifier = Modifier.width(343.dp).background(BACKGROUND_COLOR)) {
                    AdjustSlider(
                        label = "Độ mờ",
                        valueText = valueText,
                        value = value,
                        valueRange = 0f..1f,
                        onValueChange = { changes += it },
                        onValueChangeFinished = { finished++ },
                        modifier = Modifier.testTag(SLIDER),
                    )
                }
            }
        }
    }

    @Test
    fun adjustSlider_trackAndThumbSizes() {
        setSlider(value = 0.5f)
        composeRule.onNodeWithTag(SLIDER).assertHeightIsEqualTo(52.dp)
        // The track box is 28 dp, 24 dp under the top (20 label, 4 gap); its middle is at 38 dp.
        assertEquals(ACTIVE, pixelAt(SLIDER, 86.dp, 38.dp))
        assertEquals(INACTIVE, pixelAt(SLIDER, 257.dp, 38.dp))
        assertEquals(WHITE, pixelAt(SLIDER, 171.dp, 38.dp))
        // Heights in pixels (xxhdpi: 3 px per dp): the unrun track is 8 dp, the run part 8 dp, the thumb 24 dp.
        val density = composeRule.density.density
        assertEquals((8 * density).toInt(), rowsMatching(SLIDER, 257.dp) { it == INACTIVE })
        assertEquals((8 * density).toInt(), rowsMatching(SLIDER, 86.dp) { it != BACKGROUND })
        assertEquals((24 * density).toInt(), rowsMatching(SLIDER, 171.dp) { it != BACKGROUND })
    }

    @Test
    fun adjustSlider_touchHeight48() {
        setSlider()
        composeRule.onNodeWithTag(
            SLIDER,
        ).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assertHeightIsAtLeast(48.dp)

        // Real touches 22 dp above and 22 dp below the middle of the track (38 dp), both outside the 28 dp track box
        // and the 24 dp thumb, still move the thumb: the touch area is 48 dp inside the bounds of the component.
        composeRule.onNodeWithTag(SLIDER).performTouchInput { click(Offset(width * 0.25f, 16.dp.toPx())) }
        composeRule.waitForIdle()
        assertTrue("above: $changes", changes.isNotEmpty())
        assertEquals(0.25f, changes.last(), 0.05f)
        changes.clear()
        composeRule.onNodeWithTag(SLIDER).performTouchInput { click(Offset(width * 0.75f, 60.dp.toPx())) }
        composeRule.waitForIdle()
        assertTrue("below: $changes", changes.isNotEmpty())
        assertEquals(0.75f, changes.last(), 0.05f)
    }

    @Test
    fun adjustSlider_setProgress_finishedOnce() {
        setSlider(value = 0.5f)
        composeRule.onNodeWithTag(SLIDER).performSemanticsAction(SemanticsActions.SetProgress) { it(0.25f) }
        composeRule.waitForIdle()
        assertEquals(listOf(0.25f), changes)
        assertEquals(1, finished)
    }

    @Test
    fun adjustSlider_readsLabelAndValue() {
        setSlider(valueText = "70%")
        composeRule.onNodeWithTag(SLIDER).assertContentDescriptionEquals("Độ mờ")
        val config = composeRule.onNodeWithTag(SLIDER).fetchSemanticsNode().config
        assertEquals("70%", config[SemanticsProperties.StateDescription])
        // The texts are drawn too.
        composeRule.onNodeWithText("Độ mờ", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("70%", useUnmergedTree = true).assertExists()
    }

    @Test
    fun adjustAction_48button_label_selectedWhenActive() {
        var clicks = 0
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                Row(modifier = Modifier.background(BACKGROUND_COLOR)) {
                    AdjustAction(
                        icon = painterResource(R.drawable.ic_refresh),
                        label = "Lật",
                        onClick = { clicks++ },
                        modifier = Modifier.testTag(ACTION_ON),
                        active = true,
                    )
                    AdjustAction(
                        icon = painterResource(R.drawable.ic_refresh),
                        label = "Khóa",
                        onClick = {},
                        modifier = Modifier.testTag(ACTION_OFF),
                    )
                }
            }
        }
        composeRule.onNodeWithTag(ACTION_ON).assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(68.dp)
        composeRule.onNodeWithContentDescription("Lật").assertIsSelected()
        composeRule.onNodeWithContentDescription("Khóa").assertIsNotSelected()
        composeRule.onNodeWithText("Khóa", useUnmergedTree = true).assertExists()
        assertEquals(ACTIVE, pixelAt(ACTION_ON, 10.dp, 24.dp))
        assertEquals(WHITE, pixelAt(ACTION_OFF, 10.dp, 24.dp))
        composeRule.onNodeWithContentDescription("Lật").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun adjustAction_disabled_notClickable() {
        var disabledClicks = 0
        var enabledClicks = 0
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                Row {
                    AdjustAction(
                        icon = painterResource(R.drawable.ic_refresh),
                        label = "Off",
                        onClick = { disabledClicks++ },
                        enabled = false,
                    )
                    AdjustAction(
                        icon = painterResource(R.drawable.ic_refresh),
                        label = "On",
                        onClick = { enabledClicks++ },
                    )
                }
            }
        }
        composeRule.onNodeWithContentDescription("Off").assertIsNotEnabled().performClick()
        composeRule.onNodeWithContentDescription("On").performClick()
        assertEquals(0, disabledClicks)
        assertEquals(1, enabledClicks)
    }

    @Test
    fun cameraIconButton_notToggleable_hasNoSelected() {
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                Row {
                    CameraIconButton(
                        icon = painterResource(R.drawable.ic_refresh),
                        onClick = {},
                        contentDescription = "Plain",
                        toggleable = false,
                    )
                    CameraIconButton(
                        icon = painterResource(R.drawable.ic_refresh),
                        onClick = {},
                        contentDescription = "Switch",
                    )
                }
            }
        }
        composeRule.onNodeWithContentDescription("Plain")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Selected))
        composeRule.onNodeWithContentDescription("Switch")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected))
    }

    /** Draws the root view into a bitmap: captureToImage times out on Robolectric (as ComponentsTest, T-007). */
    private fun drawWindow(): Bitmap {
        composeRule.waitForIdle()
        val root = (composeRule as AndroidComposeTestRule<*, *>).activity.window.decorView
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        return bitmap
    }

    private fun pixelAt(tag: String, x: Dp, y: Dp): Int {
        val bitmap = drawWindow()
        val bounds = composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInWindow
        with(composeRule.density) {
            return bitmap.getPixel(bounds.left.toInt() + x.roundToPx(), bounds.top.toInt() + y.roundToPx())
        }
    }

    /** How many pixel rows of the track box (24 dp to 52 dp under the top of the slider) at [x] satisfy [matches]. */
    private fun rowsMatching(tag: String, x: Dp, matches: (Int) -> Boolean): Int {
        val bitmap = drawWindow()
        val bounds = composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInWindow
        with(composeRule.density) {
            val column = bounds.left.toInt() + x.roundToPx()
            val top = bounds.top.toInt() + 24.dp.roundToPx()
            val bottom = bounds.top.toInt() + 52.dp.roundToPx()
            return (top until bottom).count { matches(bitmap.getPixel(column, it)) }
        }
    }

    private companion object {
        const val PANEL = "panel"
        const val SLIDER = "slider"
        const val ACTION_ON = "action_on"
        const val ACTION_OFF = "action_off"

        val BACKGROUND_COLOR = Color(0xFF0A0A0B)
        val BACKGROUND = 0xFF0A0A0B.toInt()
        val WHITE = 0xFFFFFFFF.toInt()
        val INK = 0xFF111111.toInt()
        val RED = 0xFFFF0000.toInt()
        val ACTIVE = 0xFFFFC83D.toInt()
        val INACTIVE = 0xFFB5B0A8.toInt()
    }
}
