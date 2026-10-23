package com.vglstudio.camera.pose.line.guide.core.designsystem

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.ChoiceDialog
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.ChoiceDialogTestTags
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.RadioRow
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.SectionHeader
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.SettingsRow
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.SettingsRowTestTags
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.SettingsRowTrailing
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The settings components of the design system (plan T-025, section 4; design settings, section 3): SettingsRow in
 * its four kinds, SectionHeader, ChoiceDialog and RadioRow. Native graphics give real sizes and real pixels.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class SettingsComponentsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val icon = ColorPainter(Color.Black)

    @Test
    fun settingsRow_isAtLeast64dpTall() {
        composeRule.setContent {
            AppTheme {
                Column(Modifier.width(411.dp)) {
                    SettingsRow(icon, "Theme", SettingsRowTrailing.Chevron {}, Modifier.testTag(ROW_1), value = "Dark")
                    SettingsRow(icon, "Shutter sound", SettingsRowTrailing.Switch(true) {}, Modifier.testTag(ROW_2))
                    SettingsRow(icon, "Privacy policy", SettingsRowTrailing.Link {}, Modifier.testTag(ROW_3))
                    SettingsRow(icon, "Version", SettingsRowTrailing.None, Modifier.testTag(ROW_4), value = "1.0.0")
                }
            }
        }
        listOf(ROW_1, ROW_2, ROW_3, ROW_4).forEach { tag ->
            val bounds = composeRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()
            assertTrue("$tag is ${bounds.bottom - bounds.top}", bounds.bottom - bounds.top >= 64.dp)
        }
    }

    @Test
    fun settingsRow_chevron_clickable() {
        var clicks = 0
        composeRule.setContent {
            AppTheme {
                SettingsRow(icon, "Theme", SettingsRowTrailing.Chevron { clicks++ }, Modifier.testTag(ROW_1), "Dark")
            }
        }
        composeRule.onNodeWithTag(SettingsRowTestTags.CHEVRON, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(ROW_1).assertHasClickAction()
        composeRule.onNodeWithText("Theme", useUnmergedTree = true).performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun settingsRow_link_clickable() {
        var clicks = 0
        composeRule.setContent {
            AppTheme {
                SettingsRow(icon, "Privacy policy", SettingsRowTrailing.Link { clicks++ }, Modifier.testTag(ROW_1))
            }
        }
        composeRule.onNodeWithTag(SettingsRowTestTags.LINK, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(ROW_1).assertHasClickAction()
        composeRule.onNodeWithText("Privacy policy", useUnmergedTree = true).performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun settingsRow_switch_togglesWholeRow() {
        val changes = mutableListOf<Boolean>()
        composeRule.setContent {
            AppTheme {
                SettingsRow(
                    icon,
                    "Shutter sound",
                    SettingsRowTrailing.Switch(checked = false) { changes += it },
                    Modifier.testTag(ROW_1),
                )
            }
        }
        composeRule.onNodeWithTag(SettingsRowTestTags.SWITCH, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(ROW_1).assertIsOff()
        // A tap on the text, not on the switch, toggles it too.
        composeRule.onNodeWithText("Shutter sound", useUnmergedTree = true).performClick()
        assertEquals(listOf(true), changes)
    }

    @Test
    fun settingsRow_info_notClickable() {
        composeRule.setContent {
            AppTheme {
                SettingsRow(icon, "Version", SettingsRowTrailing.None, Modifier.testTag(ROW_1), value = "1.0.0")
            }
        }
        composeRule.onNodeWithTag(ROW_1).assertHasNoClickAction()
        // Title and value are read together, as one node.
        composeRule.onNodeWithTag(ROW_1).assert(hasText("Version")).assert(hasText("1.0.0"))
        listOf(SettingsRowTestTags.CHEVRON, SettingsRowTestTags.LINK, SettingsRowTestTags.SWITCH).forEach {
            composeRule.onNodeWithTag(it, useUnmergedTree = true).assertDoesNotExist()
        }
    }

    @Test
    fun settingsRow_value_showsBelowTitle() {
        composeRule.setContent {
            AppTheme { SettingsRow(icon, "Theme", SettingsRowTrailing.Chevron {}, Modifier.testTag(ROW_1), "Dark") }
        }
        val title = composeRule.onNodeWithText("Theme", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val value = composeRule.onNodeWithText("Dark", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(title.bottom, value.top, 1f)
        assertEquals(title.left, value.left, 1f)
    }

    @Test
    fun settingsRow_iconColor_isOnSurfaceVariant() {
        composeRule.setContent {
            AppTheme(darkTheme = false) {
                Column(Modifier.width(411.dp)) {
                    SettingsRow(icon, "Theme", SettingsRowTrailing.Chevron {}, Modifier.testTag(ROW_1), "Dark")
                }
            }
        }
        val bounds = composeRule.onNodeWithTag(ROW_1).getUnclippedBoundsInRoot()
        // The icon is 24 dp at 16 dp from the start, centered vertically.
        val color = pixelAt(ROW_1, 16.dp + 12.dp, (bounds.bottom - bounds.top) / 2)
        assertEquals(Color(0xFF5A5650).toArgb(), color.toArgb())
    }

    @Test
    fun sectionHeader_is52dpTall_andIsHeading() {
        composeRule.setContent {
            AppTheme { SectionHeader(title = "General", modifier = Modifier.testTag(HEADER)) }
        }
        composeRule.onNodeWithTag(HEADER).assertHeightIsEqualTo(52.dp)
        composeRule.onNodeWithText("General", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(HEADER).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    fun choiceDialog_showsTitleOptionsAndDismiss() {
        setDialog(selected = 0)
        composeRule.onNodeWithText("Theme").assertExists()
        OPTIONS.forEach { composeRule.onNodeWithText(it).assertExists() }
        composeRule.onNodeWithText("Cancel").assertExists()
        composeRule.onNodeWithTag(ChoiceDialogTestTags.CARD).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Theme"),
        )
        composeRule.onNodeWithText("System default").assertIsSelected()
        composeRule.onNodeWithText("Dark").assertIsNotSelected()
    }

    @Test
    fun choiceDialog_selectsAndDismisses() {
        val selected = mutableListOf<Int>()
        var dismissed = 0
        setDialog(selected = 0, onSelect = { selected += it }, onDismiss = { dismissed++ })

        composeRule.onNodeWithText("Dark").performClick()
        assertEquals(listOf(2), selected)
        composeRule.onNodeWithText("Cancel").performClick()
        assertEquals(1, dismissed)
        // A tap on the scrim outside the card closes the dialog too.
        composeRule.onNodeWithTag(ChoiceDialogTestTags.SCREEN).performTouchInput { click(centerLeft.copy(x = 5f)) }
        assertEquals(2, dismissed)
        // A tap inside the card, away from the options, does not.
        composeRule.onNodeWithTag(ChoiceDialogTestTags.CARD).performTouchInput { click(topCenter.copy(y = 4f)) }
        assertEquals(2, dismissed)
    }

    @Test
    fun choiceDialog_cardWidth_isAtMost331dp() {
        setDialog(selected = 0)
        val card = composeRule.onNodeWithTag(ChoiceDialogTestTags.CARD).getUnclippedBoundsInRoot()
        assertTrue("card is ${card.width}", card.width <= 331.dp)
        assertTrue("card is ${card.width}", card.width >= 300.dp)
    }

    @Test
    fun radioRow_is48dpTall_andSelectable() {
        var clicks = 0
        composeRule.setContent {
            AppTheme {
                Column {
                    RadioRow("Light", selected = true, onClick = { clicks++ }, modifier = Modifier.testTag(ROW_1))
                    RadioRow("Dark", selected = false, onClick = {}, modifier = Modifier.testTag(ROW_2))
                }
            }
        }
        composeRule.onNodeWithTag(ROW_1).assertHeightIsEqualTo(48.dp).assertIsSelected()
        composeRule.onNodeWithTag(ROW_2).assertIsNotSelected()
        composeRule.onNodeWithTag(ROW_1).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton),
        )
        composeRule.onNodeWithText("Light", useUnmergedTree = true).performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun radioRow_selectedFill_isSecondaryContainer() {
        composeRule.setContent {
            AppTheme(darkTheme = false) {
                Column {
                    RadioRow("Light", selected = true, onClick = {}, modifier = Modifier.testTag(ROW_1))
                    RadioRow("Dark", selected = false, onClick = {}, modifier = Modifier.testTag(ROW_2))
                }
            }
        }
        // The radio is 24 dp at the start of the 48 dp row: its center is (12, 24); 8 dp right of it is the fill.
        assertEquals(Color(0xFFFFC83D).toArgb(), pixelAt(ROW_1, 12.dp + 8.dp, 24.dp).toArgb())
        assertEquals(Color(0xFFFFFFFF).toArgb(), pixelAt(ROW_2, 12.dp + 8.dp, 24.dp).toArgb())
    }

    @Test
    fun choiceDialog_scrim_isHalfBlack() {
        composeRule.setContent {
            AppTheme(darkTheme = false) {
                Box(Modifier.fillMaxSize().background(Color.White).testTag(BACKGROUND)) {
                    ChoiceDialog(
                        title = "Theme",
                        options = OPTIONS,
                        selectedIndex = 0,
                        onSelect = {},
                        dismissLabel = "Cancel",
                        onDismiss = {},
                    )
                }
            }
        }
        val color = pixelAt(BACKGROUND, 5.dp, 5.dp)
        listOf(color.red, color.green, color.blue).forEach { channel ->
            assertEquals(0x7F / 255f, channel, 2 / 255f)
        }
    }

    private fun setDialog(selected: Int, onSelect: (Int) -> Unit = {}, onDismiss: () -> Unit = {}) {
        composeRule.setContent {
            AppTheme {
                ChoiceDialog(
                    title = "Theme",
                    options = OPTIONS,
                    selectedIndex = selected,
                    onSelect = onSelect,
                    dismissLabel = "Cancel",
                    onDismiss = onDismiss,
                )
            }
        }
    }

    /** Color of the pixel at ([x], [y]) inside the node, read from the drawn root view (as ComponentsTest). */
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
        const val ROW_1 = "row1"
        const val ROW_2 = "row2"
        const val ROW_3 = "row3"
        const val ROW_4 = "row4"
        const val HEADER = "header"
        const val BACKGROUND = "background"
        val OPTIONS = listOf("System default", "Light", "Dark")
    }
}
