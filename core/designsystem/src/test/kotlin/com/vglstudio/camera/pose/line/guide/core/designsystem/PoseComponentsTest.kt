package com.vglstudio.camera.pose.line.guide.core.designsystem

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.POSE_THUMB_RETRY_TAG
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PREMIUM_BADGE_TAG
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PoseThumb
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PremiumBadge
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PremiumBadgeSize
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.SELECTED_BADGE_TAG
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.SelectedBadge
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.StepCard
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.StepCardArrows
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.StickerChip
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The four pose components (docs/design-system/sticker.md, section 4; camera design README, section 3; plan T-014). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class PoseComponentsTest {
    @get:Rule
    val composeRule = createComposeRule()

    // StickerChip

    @Test
    fun stickerChip_height36_touch48() {
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                Box(Modifier.padding(24.dp).testTag(WRAPPER)) {
                    StickerChip(label = "Nữ", selected = false, onClick = {})
                }
            }
        }
        composeRule.onNodeWithTag(WRAPPER).assertHeightIsEqualTo(36.dp)
        val touch = composeRule.onNodeWithText("Nữ").fetchSemanticsNode().touchBoundsInRoot
        with(composeRule.density) { assertEquals(48.dp.toPx(), touch.height, 1f) }
    }

    @Test
    fun stickerChip_selected_isYellowWithBadge() {
        composeRule.setContent {
            Column(Modifier.padding(24.dp)) {
                AppTheme(darkTheme = true) {
                    StickerChip(label = "Tất cả", selected = true, onClick = {}, modifier = Modifier.testTag(SELECTED))
                    StickerChip(
                        label = "Nữ",
                        selected = false,
                        onClick = {},
                        modifier = Modifier.padding(top = 24.dp).testTag(CHIP),
                    )
                }
                AppTheme(darkTheme = false) {
                    StickerChip(
                        label = "Nam",
                        selected = false,
                        onClick = {},
                        modifier = Modifier.padding(top = 24.dp).testTag(LIGHT),
                    )
                }
            }
        }
        assertEquals(YELLOW, pixelAt(SELECTED, 8.dp, 18.dp))
        assertEquals(Color(0xFF1E1E1E).toArgb(), pixelAt(CHIP, 8.dp, 18.dp))
        assertEquals(Color(0xFFFFFFFF).toArgb(), pixelAt(LIGHT, 8.dp, 18.dp))
        // Only the selected chip has the badge.
        assertEquals(
            1,
            composeRule.onAllNodesWithTag(SELECTED_BADGE_TAG, useUnmergedTree = true).fetchSemanticsNodes().size,
        )
    }

    @Test
    fun stickerChip_click_andSelectedSemantics() {
        var clicks = 0
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                Column {
                    StickerChip(label = "Tất cả", selected = true, onClick = {})
                    StickerChip(label = "Nữ", selected = false, onClick = { clicks++ })
                }
            }
        }
        composeRule.onNodeWithText("Tất cả").assertIsSelected()
        composeRule.onNodeWithText("Nữ").assertIsNotSelected().performClick()
        assertEquals(1, clicks)
    }

    // Disabled while the camera counts down (plan T-061): read as disabled, a tap does nothing.
    @Test
    fun stickerChip_disabled_notSelectable() {
        var disabledClicks = 0
        var enabledClicks = 0
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                Column(Modifier.padding(24.dp)) {
                    StickerChip(label = "Nữ", selected = false, onClick = { disabledClicks++ }, enabled = false)
                    StickerChip(
                        label = "Nam",
                        selected = false,
                        onClick = { enabledClicks++ },
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
            }
        }
        composeRule.onNodeWithText("Nữ").assertIsNotEnabled().performTouchInput { click() }
        assertEquals(0, disabledClicks)
        // Control: the same chip enabled (the default) answers the tap once.
        composeRule.onNodeWithText("Nam").assertIsEnabled().performTouchInput { click() }
        assertEquals(1, enabledClicks)
    }

    // SelectedBadge

    @Test
    fun selectedBadge_is22dpYellow() {
        composeRule.setContent {
            AppTheme(darkTheme = true) { Box(Modifier.padding(24.dp)) { SelectedBadge() } }
        }
        composeRule.onNodeWithTag(SELECTED_BADGE_TAG, useUnmergedTree = true)
            .assertWidthIsEqualTo(22.dp)
            .assertHeightIsEqualTo(22.dp)
        assertEquals(YELLOW, pixelAt(SELECTED_BADGE_TAG, 11.dp, 4.dp, useUnmergedTree = true))
    }

    // PoseThumb

    @Test
    fun poseThumb_is64dp() {
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                PoseThumb(
                    image = null,
                    selected = false,
                    onClick = {},
                    contentDescription = "Pose w01",
                    modifier = Modifier.testTag(THUMB),
                )
            }
        }
        composeRule.onNodeWithTag(THUMB).assertWidthIsEqualTo(64.dp).assertHeightIsEqualTo(64.dp)
    }

    @Test
    fun poseThumb_selectedBorder_isYellow3dp() {
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                Column(Modifier.padding(24.dp)) {
                    PoseThumb(
                        image = null,
                        selected = true,
                        onClick = {},
                        contentDescription = "Pose w01",
                        modifier = Modifier.testTag(SELECTED),
                    )
                    PoseThumb(
                        image = null,
                        selected = false,
                        onClick = {},
                        contentDescription = "Pose w02",
                        modifier = Modifier.padding(top = 24.dp).testTag(THUMB),
                    )
                }
            }
        }
        assertEquals(YELLOW, pixelAt(SELECTED, 1.5.dp, 32.dp))
        assertEquals(YELLOW, pixelAt(SELECTED, 2.5.dp, 32.dp))
        assertEquals(Color(0xFFFFFFFF).toArgb(), pixelAt(THUMB, 1.5.dp, 32.dp))
        // The normal border is 2 dp: past it is the fill.
        assertEquals(SURFACE_VARIANT, pixelAt(THUMB, 2.5.dp, 32.dp))
    }

    @Test
    fun poseThumb_failed_showsRetryIcon() {
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                Column {
                    PoseThumb(image = null, selected = false, onClick = {}, contentDescription = "a", failed = true)
                    PoseThumb(image = null, selected = false, onClick = {}, contentDescription = "b")
                }
            }
        }
        assertEquals(
            1,
            composeRule.onAllNodesWithTag(POSE_THUMB_RETRY_TAG, useUnmergedTree = true).fetchSemanticsNodes().size,
        )
    }

    @Test
    fun poseThumb_loading_isEmptyFill() {
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                PoseThumb(
                    image = null,
                    selected = false,
                    onClick = {},
                    contentDescription = "Pose w01",
                    modifier = Modifier.padding(24.dp).testTag(THUMB),
                )
            }
        }
        assertEquals(SURFACE_VARIANT, pixelAt(THUMB, 32.dp, 32.dp))
    }

    @Test
    fun poseThumb_click_andSelectedSemantics() {
        var clicks = 0
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                Column {
                    PoseThumb(image = null, selected = true, onClick = {}, contentDescription = "Pose w01")
                    PoseThumb(image = null, selected = false, onClick = { clicks++ }, contentDescription = "Pose w02")
                }
            }
        }
        composeRule.onNodeWithContentDescription("Pose w01").assertIsSelected()
        composeRule.onNodeWithContentDescription("Pose w02").assertIsNotSelected().performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun poseThumb_disabled_notSelectable() {
        var disabledClicks = 0
        var enabledClicks = 0
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                Column(Modifier.padding(24.dp)) {
                    PoseThumb(
                        image = null,
                        selected = false,
                        onClick = { disabledClicks++ },
                        contentDescription = "Pose w01",
                        enabled = false,
                    )
                    PoseThumb(
                        image = null,
                        selected = false,
                        onClick = { enabledClicks++ },
                        contentDescription = "Pose w02",
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
            }
        }
        composeRule.onNodeWithContentDescription("Pose w01").assertIsNotEnabled().performTouchInput { click() }
        assertEquals(0, disabledClicks)
        // Control: the same cell enabled (the default) answers the tap once.
        composeRule.onNodeWithContentDescription("Pose w02").assertIsEnabled().performTouchInput { click() }
        assertEquals(1, enabledClicks)
    }

    // Premium cell (spec poses, KB-8; camera design, CameraScreen_premium_dark; plan T-086)

    @Test
    fun thumb_premium_drawsBadge_22dpOverTopLeft() {
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                Column(Modifier.padding(24.dp)) {
                    PoseThumb(
                        image = null,
                        selected = false,
                        onClick = {},
                        contentDescription = "Pose w01",
                        modifier = Modifier.testTag(THUMB),
                        premium = true,
                    )
                    PoseThumb(
                        image = null,
                        selected = false,
                        onClick = {},
                        contentDescription = "Pose w02",
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
            }
        }
        val badges = composeRule.onAllNodesWithTag(PREMIUM_BADGE_TAG, useUnmergedTree = true).fetchSemanticsNodes()
        assertEquals(1, badges.size)
        val badge = composeRule.onNodeWithTag(PREMIUM_BADGE_TAG, useUnmergedTree = true)
        badge.assertWidthIsEqualTo(22.dp).assertHeightIsEqualTo(22.dp)
        val bounds = badge.getUnclippedBoundsInRoot()
        val cell = composeRule.onNodeWithTag(THUMB).getUnclippedBoundsInRoot()
        // Design: the 22 dp badge at (-7, -7) of the 64 dp cell, mirroring the selected badge.
        assertEquals(-7f, (bounds.left - cell.left).value, 0.5f)
        assertEquals(-7f, (bounds.top - cell.top).value, 0.5f)
    }

    @Test
    fun thumb_premium_readsDescriptionOnce() {
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                Column(Modifier.padding(24.dp)) {
                    PoseThumb(
                        image = null,
                        selected = false,
                        onClick = {},
                        contentDescription = PREMIUM_CELL,
                        modifier = Modifier.testTag(THUMB),
                        premium = true,
                    )
                    // Control: a selectable cell with a badge that has a label of its own reads two descriptions.
                    Box(
                        Modifier
                            .padding(top = 24.dp)
                            .size(64.dp)
                            .semantics { contentDescription = PREMIUM_CELL }
                            .selectable(selected = false, onClick = {})
                            .testTag(CARD),
                    ) {
                        PremiumBadge(PremiumBadgeSize.Thumb, contentDescription = "Premium pose")
                    }
                }
            }
        }
        composeRule.onNodeWithTag(THUMB).assertContentDescriptionEquals(PREMIUM_CELL)
        composeRule.onNodeWithTag(CARD).assertContentDescriptionEquals(PREMIUM_CELL, "Premium pose")
    }

    // StepCard

    @Test
    fun stepCard_wide_oneRow() {
        setStepCard(width = 379.dp)
        val text = composeRule.onNodeWithText(STEP_TEXT).getUnclippedBoundsInRoot()
        val next = composeRule.onNodeWithContentDescription(NEXT).getUnclippedBoundsInRoot()
        assertTrue("arrow top ${next.top}, text bottom ${text.bottom}", next.top < text.bottom)
    }

    @Test
    fun stepCard_narrow_twoRows() {
        setStepCard(width = 190.dp)
        val text = composeRule.onNodeWithText(STEP_TEXT).getUnclippedBoundsInRoot()
        val next = composeRule.onNodeWithContentDescription(NEXT).getUnclippedBoundsInRoot()
        assertTrue("arrow top ${next.top}, text bottom ${text.bottom}", next.top >= text.bottom)
        assertTrue("text width ${text.width}", text.width >= 100.dp)
    }

    @Test
    fun stepCardArrows_disabled_notClickable() {
        var previous = 0
        var next = 0
        setStepCard(width = 379.dp, previousEnabled = false, onPrevious = { previous++ }, onNext = { next++ })
        composeRule.onNodeWithContentDescription(PREVIOUS)
            .assertWidthIsEqualTo(40.dp)
            .assertHeightIsEqualTo(48.dp)
            .assertIsNotEnabled()
            .performClick()
        composeRule.onNodeWithContentDescription(NEXT).assertIsEnabled().performClick()
        assertEquals(0, previous)
        assertEquals(1, next)
    }

    @Test
    fun stepCard_badge_isYellowPill() {
        setStepCard(width = 379.dp)
        composeRule.onNodeWithText("2/4").assertExists()
        // Left of the label, inside the pill's padding: yellow. Between the card border and the pill: white card.
        assertEquals(YELLOW, pixelAt("2/4", (-3).dp, 10.dp, byText = true))
        assertEquals(Color(0xFFFFFFFF).toArgb(), pixelAt(CARD, 8.dp, 32.dp))
    }

    @Test
    fun stepCard_trailingSlot_replacesArrows() {
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                Box(Modifier.width(379.dp)) {
                    StepCard(text = STEP_TEXT, trailing = { Text("Retry") })
                }
            }
        }
        composeRule.onNodeWithText(STEP_TEXT).assertExists()
        composeRule.onNodeWithText("Retry").assertExists()
        composeRule.onNodeWithContentDescription(NEXT).assertDoesNotExist()
    }

    private fun setStepCard(
        width: Dp,
        previousEnabled: Boolean = true,
        onPrevious: () -> Unit = {},
        onNext: () -> Unit = {},
    ) {
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                Box(Modifier.width(width)) {
                    StepCard(
                        text = STEP_TEXT,
                        badge = "2/4",
                        modifier = Modifier.testTag(CARD),
                        trailing = {
                            StepCardArrows(
                                onPrevious = onPrevious,
                                onNext = onNext,
                                previousEnabled = previousEnabled,
                                nextEnabled = true,
                                previousDescription = PREVIOUS,
                                nextDescription = NEXT,
                            )
                        },
                    )
                }
            }
        }
    }

    /** Draws the root view into a bitmap: captureToImage times out on Robolectric (as CameraComponentsTest, L-006). */
    private fun pixelAt(key: String, x: Dp, y: Dp, useUnmergedTree: Boolean = false, byText: Boolean = false): Int {
        composeRule.waitForIdle()
        val root = (composeRule as AndroidComposeTestRule<*, *>).activity.window.decorView
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        val node = if (byText) {
            composeRule.onNodeWithText(key, useUnmergedTree = true)
        } else {
            composeRule.onNodeWithTag(key, useUnmergedTree = useUnmergedTree)
        }
        val bounds = node.fetchSemanticsNode().boundsInWindow
        with(composeRule.density) {
            return bitmap.getPixel(bounds.left.toInt() + x.roundToPx(), bounds.top.toInt() + y.roundToPx())
        }
    }

    private companion object {
        const val WRAPPER = "wrapper"
        const val SELECTED = "selected"
        const val CHIP = "chip"
        const val LIGHT = "light"
        const val THUMB = "thumb"
        const val CARD = "card"
        const val PREMIUM_CELL = "Lean, premium, tap to unlock"
        const val STEP_TEXT = "Turn your shoulders 45 degrees to the camera."
        const val PREVIOUS = "Previous step"
        const val NEXT = "Next step"
        val YELLOW = Color(0xFFFFC83D).toArgb()
        val SURFACE_VARIANT = Color(0xFF2A2A2A).toArgb()
    }
}
