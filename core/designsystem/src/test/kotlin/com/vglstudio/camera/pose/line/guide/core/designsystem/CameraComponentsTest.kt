package com.vglstudio.camera.pose.line.guide.core.designsystem

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.CameraIconButton
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.GridOverlay
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.StateMessage
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.ZoomControl
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The four camera components (docs/design-system/sticker.md, section 4; camera design README, section 3). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class CameraComponentsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun cameraIconButton_is48dpCircle() {
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                CameraIconButton(
                    icon = painterResource(R.drawable.ic_refresh),
                    onClick = {},
                    contentDescription = "Refresh",
                    modifier = Modifier.testTag(BUTTON),
                )
            }
        }
        composeRule.onNodeWithTag(BUTTON).assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp)
    }

    @Test
    fun cameraIconButton_colors() {
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                Box {
                    CameraIconButton(
                        icon = painterResource(R.drawable.ic_refresh),
                        onClick = {},
                        contentDescription = null,
                        modifier = Modifier.testTag(BUTTON),
                    )
                    CameraIconButton(
                        icon = painterResource(R.drawable.ic_refresh),
                        onClick = {},
                        contentDescription = null,
                        modifier = Modifier.padding(start = 100.dp).testTag(ACTIVE),
                        active = true,
                    )
                }
            }
        }
        assertEquals(Color(0xFFFFFFFF).toArgb(), pixelAt(BUTTON, 10.dp, 24.dp).toArgb())
        assertEquals(Color(0xFFFFC83D).toArgb(), pixelAt(ACTIVE, 10.dp, 24.dp).toArgb())
    }

    @Test
    fun cameraIconButton_text_showsLabel() {
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                CameraIconButton(text = "3s", onClick = {}, contentDescription = null, active = true)
            }
        }
        composeRule.onNodeWithText("3s").assertExists()
    }

    @Test
    fun cameraIconButton_disabled_notClickable() {
        var clicks = 0
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                CameraIconButton(
                    icon = painterResource(R.drawable.ic_refresh),
                    onClick = { clicks++ },
                    contentDescription = null,
                    modifier = Modifier.testTag(BUTTON),
                    enabled = false,
                )
            }
        }
        composeRule.onNodeWithTag(BUTTON).assertIsNotEnabled().performClick()
        assertEquals(0, clicks)
    }

    @Test
    fun cameraIconButton_active_isSelected() {
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                CameraIconButton(
                    text = "3s",
                    onClick = {},
                    contentDescription = null,
                    modifier = Modifier.testTag(BUTTON),
                    active = true,
                )
            }
        }
        composeRule.onNodeWithTag(BUTTON).assertIsSelected().assertHasClickAction()
    }

    @Test
    fun gridOverlay_drawsThirds() {
        composeRule.setContent {
            Box(Modifier.testTag(GRID).size(300.dp, 450.dp).background(Color.Black)) {
                GridOverlay(color = Color.White, alpha = 1f)
            }
        }
        val white = Color.White.toArgb()
        val black = Color.Black.toArgb()
        assertEquals(white, pixelAt(GRID, 100.dp, 50.dp).toArgb())
        assertEquals(white, pixelAt(GRID, 200.dp, 50.dp).toArgb())
        assertEquals(white, pixelAt(GRID, 50.dp, 150.dp).toArgb())
        assertEquals(white, pixelAt(GRID, 50.dp, 300.dp).toArgb())
        assertEquals(black, pixelAt(GRID, 50.dp, 50.dp).toArgb())
        assertEquals(black, pixelAt(GRID, 150.dp, 225.dp).toArgb())
    }

    @Test
    fun zoomControl_showsLabels_selects() {
        var selected = -1
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                ZoomControl(levels = listOf("0.5", "1x", "2"), selectedIndex = 1, onSelect = { selected = it })
            }
        }
        composeRule.onNodeWithText("0.5").assertExists()
        composeRule.onNodeWithText("1x").assertIsSelected()
        composeRule.onNodeWithText("2").performClick()
        assertEquals(2, selected)
    }

    @Test
    fun zoomControl_height_is48dp() {
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                ZoomControl(
                    levels = listOf("0.5", "1x", "2"),
                    selectedIndex = 1,
                    onSelect = {},
                    modifier = Modifier.testTag(ZOOM),
                )
            }
        }
        composeRule.onNodeWithTag(ZOOM).assertHeightIsEqualTo(48.dp)
    }

    @Test
    fun zoomControl_selectedFill_isSecondaryContainer() {
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                ZoomControl(
                    levels = listOf("0.5", "1x", "2"),
                    selectedIndex = 0,
                    onSelect = {},
                    modifier = Modifier.testTag(ZOOM),
                )
            }
        }
        // First button: 4 dp padding, 40 dp circle; a point inside the circle, left of the label.
        assertEquals(Color(0xFFFFC83D).toArgb(), pixelAt(ZOOM, 10.dp, 24.dp).toArgb())
    }

    @Test
    fun stateMessage_showsTexts_andAction() {
        var actions = 0
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                StateMessage(
                    icon = painterResource(R.drawable.ic_wifi_off),
                    title = "Title",
                    message = "Message",
                    actionLabel = "Action",
                    actionIcon = painterResource(R.drawable.ic_refresh),
                    onAction = { actions++ },
                )
            }
        }
        composeRule.onNodeWithText("Title").assertExists()
        composeRule.onNodeWithText("Message").assertExists()
        composeRule.onNodeWithText("Action").performClick()
        assertEquals(1, actions)
    }

    @Test
    fun stateMessage_withoutAction_hasNoButton() {
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                StateMessage(icon = painterResource(R.drawable.ic_wifi_off), title = "Title", message = "Message")
            }
        }
        composeRule.onNodeWithText("Title").assertExists()
        composeRule.onNodeWithText("Action").assertDoesNotExist()
    }

    @Test
    fun stateMessage_width_isAtMost331dp() {
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                Box(Modifier.width(411.dp)) {
                    StateMessage(
                        icon = painterResource(R.drawable.ic_wifi_off),
                        title = "Title",
                        message = "A long message that would fill the whole width of the frame if nothing held it.",
                        modifier = Modifier.testTag(MESSAGE),
                    )
                }
            }
        }
        val width = composeRule.onNodeWithTag(MESSAGE).getUnclippedBoundsInRoot().width
        assertTrue("width $width", width.value in 1f..331.5f)
    }

    /** Draws the root view into a bitmap: captureToImage times out on Robolectric (as ComponentsTest, T-007). */
    private fun pixelAt(tag: String, x: Dp, y: Dp): Color {
        composeRule.waitForIdle()
        val root = (composeRule as AndroidComposeTestRule<*, *>).activity.window.decorView
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        val bounds = composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInWindow
        with(composeRule.density) {
            return Color(bitmap.getPixel(bounds.left.toInt() + x.roundToPx(), bounds.top.toInt() + y.roundToPx()))
        }
    }

    private companion object {
        const val BUTTON = "button"
        const val ACTIVE = "active"
        const val GRID = "grid"
        const val ZOOM = "zoom"
        const val MESSAGE = "message"
    }
}
