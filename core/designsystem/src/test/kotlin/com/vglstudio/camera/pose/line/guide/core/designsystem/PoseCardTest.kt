package com.vglstudio.camera.pose.line.guide.core.designsystem

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.POSE_CARD_IMAGE_TAG
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.POSE_CARD_SAVE_CIRCLE_TAG
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.POSE_CARD_SAVE_TAG
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PREMIUM_BADGE_TAG
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PoseCard
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PoseCardDefaults
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PoseCardSkeleton
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The pose card and its skeleton (sticker.md, section 4; poses design README, section 3; plan T-021). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class PoseCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var saveClicks = 0
    private var cardClicks = 0

    private fun setCard(
        name: String = NAME,
        saved: Boolean = false,
        onClick: (() -> Unit)? = null,
        dark: Boolean = false,
    ) {
        composeRule.setContent {
            AppTheme(darkTheme = dark) {
                PoseCard(
                    name = name,
                    meta = META,
                    image = ColorPainter(Color.Transparent),
                    imageBackground = CATEGORY,
                    saved = saved,
                    saveLabel = if (saved) "Bỏ lưu tư thế" else "Lưu tư thế",
                    onSaveClick = { saveClicks++ },
                    modifier = Modifier.padding(16.dp).width(CARD_WIDTH).testTag(CARD),
                    onClick = onClick,
                )
            }
        }
    }

    @Test
    fun showsNameAndMeta() {
        setCard()
        composeRule.onNodeWithText(NAME).assertExists()
        composeRule.onNodeWithText(META).assertExists()
    }

    @Test
    fun saveButton_touchTarget48_circle36() {
        setCard()
        composeRule.onNodeWithTag(POSE_CARD_SAVE_TAG, useUnmergedTree = true)
            .assertWidthIsEqualTo(48.dp)
            .assertHeightIsEqualTo(48.dp)
        composeRule.onNodeWithTag(POSE_CARD_SAVE_CIRCLE_TAG, useUnmergedTree = true)
            .assertWidthIsEqualTo(36.dp)
            .assertHeightIsEqualTo(36.dp)
    }

    @Test
    fun saved_buttonFillIsSecondaryContainer_unsavedIsSurface() {
        composeRule.setContent {
            AppTheme(darkTheme = false) {
                Column {
                    PoseCard(
                        name = NAME,
                        meta = META,
                        image = ColorPainter(Color.Transparent),
                        imageBackground = CATEGORY,
                        saved = true,
                        saveLabel = "saved",
                        onSaveClick = {},
                        modifier = Modifier.width(CARD_WIDTH),
                    )
                    PoseCard(
                        name = NAME,
                        meta = META,
                        image = ColorPainter(Color.Transparent),
                        imageBackground = CATEGORY,
                        saved = false,
                        saveLabel = "unsaved",
                        onSaveClick = {},
                        modifier = Modifier.width(CARD_WIDTH),
                    )
                }
            }
        }
        // Inside the 2 dp border, away from the icon in the middle.
        assertEquals(Color(0xFFFFC83D).toArgb(), circlePixel("saved", 18.dp, 5.dp))
        assertEquals(Color(0xFFFFFFFF).toArgb(), circlePixel("unsaved", 18.dp, 5.dp))
    }

    @Test
    fun saveClick_callsOnSaveClick_only() {
        setCard(onClick = { cardClicks++ })
        composeRule.onNodeWithContentDescription("Lưu tư thế").performClick()
        assertEquals(1, saveClicks)
        assertEquals(0, cardClicks)
        composeRule.onNodeWithTag(CARD).performClick()
        assertEquals(1, cardClicks)
        assertEquals(1, saveClicks)
    }

    @Test
    fun saveButton_hasButtonRoleAndLabel() {
        setCard(saved = false)
        composeRule.onNodeWithContentDescription("Lưu tư thế")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Off))
    }

    @Test
    fun imageArea_hasDesignAspectRatio() {
        setCard()
        val bounds = composeRule.onNodeWithTag(POSE_CARD_IMAGE_TAG, useUnmergedTree = true).getUnclippedBoundsInRoot()
        // 182 wide card: 2 dp border and 8 dp padding each side leave 162 dp, as on the design; 162 : 210.
        assertEquals(162f, bounds.width.value, 0.5f)
        assertEquals(210f, bounds.height.value, 0.5f)
    }

    @Test
    fun longName_wrapsToAtMostTwoLines() {
        setCard(name = LONG_NAME)
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(LONG_NAME, useUnmergedTree = true).fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult].action!!(layouts)
        assertEquals(2, layouts.first().lineCount)
        assertTrue(layouts.first().hasVisualOverflow)
    }

    @Test
    fun skeleton_sameSizeAsOneLineCard() {
        composeRule.setContent {
            AppTheme(darkTheme = false) {
                Column {
                    PoseCard(
                        name = NAME,
                        meta = META,
                        image = ColorPainter(Color.Transparent),
                        imageBackground = CATEGORY,
                        saved = false,
                        saveLabel = "Lưu tư thế",
                        onSaveClick = {},
                        modifier = Modifier.width(CARD_WIDTH).testTag(CARD),
                    )
                    PoseCardSkeleton(modifier = Modifier.width(CARD_WIDTH).testTag(SKELETON))
                }
            }
        }
        val card = composeRule.onNodeWithTag(CARD).getUnclippedBoundsInRoot()
        val skeleton = composeRule.onNodeWithTag(SKELETON).getUnclippedBoundsInRoot()
        // Design: 2 + 8 + 210 + 8 + 46 (24 + 2 + 16 + 4) + 8 + 2.
        assertEquals(284f, card.height.value, 0.5f)
        assertEquals(card.height.value, skeleton.height.value, 0.01f)
        assertEquals(card.width.value, skeleton.width.value, 0.01f)
    }

    @Test
    fun oneLineHeight_matchesSkeletonAndOneLineCard() {
        var width by mutableStateOf(156.dp)
        var fontScale by mutableStateOf(1f)
        var expected = 0.dp
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                AppTheme(darkTheme = false) {
                    expected = PoseCardDefaults.oneLineHeight(width)
                    Column {
                        PoseCard(
                            name = SHORT_NAME,
                            meta = META,
                            image = ColorPainter(Color.Transparent),
                            imageBackground = CATEGORY,
                            saved = false,
                            saveLabel = "Lưu tư thế",
                            onSaveClick = {},
                            modifier = Modifier.width(width).testTag(CARD),
                        )
                        PoseCardSkeleton(modifier = Modifier.width(width).testTag(SKELETON))
                    }
                }
            }
        }
        listOf(1f, 2f).forEach { scale ->
            listOf(156.dp, 182.dp, 203.dp).forEach { cardWidth ->
                fontScale = scale
                width = cardWidth
                composeRule.waitForIdle()
                val card = composeRule.onNodeWithTag(CARD).getUnclippedBoundsInRoot()
                val skeleton = composeRule.onNodeWithTag(SKELETON).getUnclippedBoundsInRoot()
                val at = "width $cardWidth, font scale $scale"
                assertEquals("card width, $at", cardWidth.value, card.width.value, 0.5f)
                assertEquals("skeleton, $at", skeleton.height.value, expected.value, 0.5f)
                assertEquals("one-line card, $at", card.height.value, expected.value, 0.5f)
                if (scale == 1f && cardWidth == 182.dp) assertEquals("design height", 284f, expected.value, 0.5f)
            }
        }
    }

    @Test
    fun skeleton_withoutOnClick_notClickable() {
        composeRule.setContent {
            AppTheme { PoseCardSkeleton(modifier = Modifier.width(CARD_WIDTH).testTag(SKELETON)) }
        }
        composeRule.onNodeWithTag(SKELETON).assertHasNoClickAction()
    }

    @Test
    fun skeleton_withOnClick_callsIt() {
        var clicks = 0
        composeRule.setContent {
            AppTheme {
                PoseCardSkeleton(
                    modifier = Modifier.width(CARD_WIDTH).testTag(SKELETON),
                    onClick = { clicks++ },
                    onClickLabel = "Tải lại ảnh",
                )
            }
        }
        composeRule.onNodeWithTag(SKELETON).performClick()
        assertEquals(1, clicks)
    }

    // Premium poses (spec poses, KB-8; plan T-086)

    @Test
    fun premium_drawsBadge_32dpAtTopLeft_withDescription() {
        var premium by mutableStateOf(true)
        composeRule.setContent {
            AppTheme(darkTheme = false) {
                PoseCard(
                    name = NAME,
                    meta = META,
                    image = ColorPainter(Color.Transparent),
                    imageBackground = CATEGORY,
                    saved = false,
                    saveLabel = "Lưu tư thế",
                    onSaveClick = {},
                    modifier = Modifier.padding(16.dp).width(CARD_WIDTH),
                    premium = premium,
                )
            }
        }
        val badge = composeRule.onNodeWithTag(PREMIUM_BADGE_TAG, useUnmergedTree = true)
        badge.assertWidthIsEqualTo(32.dp).assertHeightIsEqualTo(32.dp)
        badge.assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(PREMIUM_LABEL)))
        val bounds = badge.getUnclippedBoundsInRoot()
        val image = composeRule.onNodeWithTag(POSE_CARD_IMAGE_TAG, useUnmergedTree = true).getUnclippedBoundsInRoot()
        // Design: the badge sits at (6, 10) in the image.
        assertEquals(6f, (bounds.left - image.left).value, 0.5f)
        assertEquals(10f, (bounds.top - image.top).value, 0.5f)
        // Its centre is as far from the left edge of the image as the centre of the save circle from the right edge.
        val circle = composeRule.onNodeWithTag(POSE_CARD_SAVE_CIRCLE_TAG, useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val badgeCentre = (bounds.left + bounds.right) / 2 - image.left
        val circleCentre = image.right - (circle.left + circle.right) / 2
        assertEquals(circleCentre.value, badgeCentre.value, 0.5f)
        // Control: not premium, no badge.
        premium = false
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(PREMIUM_BADGE_TAG, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun premium_mergedNode_readsBadgeOnce() {
        composeRule.setContent {
            AppTheme(darkTheme = false) {
                PoseCard(
                    name = NAME,
                    meta = META,
                    image = ColorPainter(Color.Transparent),
                    imageBackground = CATEGORY,
                    saved = false,
                    saveLabel = "Lưu tư thế",
                    onSaveClick = {},
                    modifier = Modifier.padding(16.dp).width(CARD_WIDTH).testTag(CARD),
                    onClick = {},
                    premium = true,
                )
            }
        }
        // L-004: the name and the second line are text; the badge adds its label once to the merged card.
        composeRule.onNodeWithTag(CARD).assertContentDescriptionEquals(PREMIUM_LABEL)
        // The badge is also a node of its own, which the device scenario selects.
        composeRule.onNodeWithTag(PREMIUM_BADGE_TAG, useUnmergedTree = true).assertExists()
    }

    @Test
    fun premiumBadge_colors_light_and_dark() {
        composeRule.setContent {
            Column {
                listOf(false, true).forEach { dark ->
                    AppTheme(darkTheme = dark) {
                        PoseCard(
                            name = NAME,
                            meta = META,
                            image = ColorPainter(Color.Transparent),
                            imageBackground = CATEGORY,
                            saved = false,
                            saveLabel = if (dark) "dark" else "light",
                            onSaveClick = {},
                            modifier = Modifier.width(CARD_WIDTH),
                            premium = true,
                        )
                    }
                }
            }
        }
        // The ring is tertiaryContainer: 4 dp inside its left edge, at its middle height, away from the icon.
        assertEquals(Color(0xFFFFD6E2).toArgb(), badgePixel(index = 0))
        assertEquals(Color(0xFF6B1D35).toArgb(), badgePixel(index = 1))
    }

    private fun badgePixel(index: Int): Int {
        composeRule.waitForIdle()
        val root = (composeRule as AndroidComposeTestRule<*, *>).activity.window.decorView
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        val bounds = composeRule.onAllNodesWithTag(PREMIUM_BADGE_TAG, useUnmergedTree = true)[index]
            .fetchSemanticsNode().boundsInWindow
        with(composeRule.density) {
            val left = bounds.left + 4.dp.toPx()
            val top = (bounds.top + bounds.bottom) / 2
            return bitmap.getPixel(left.toInt(), top.toInt())
        }
    }

    private fun circlePixel(saveLabel: String, x: Dp, y: Dp): Int {
        composeRule.waitForIdle()
        val root = (composeRule as AndroidComposeTestRule<*, *>).activity.window.decorView
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        // The circle is centred in the 48 dp touch area that carries the label.
        val bounds = composeRule.onNodeWithContentDescription(saveLabel, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInWindow
        with(composeRule.density) {
            val left = bounds.left + 6.dp.toPx() + x.toPx()
            val top = bounds.top + 6.dp.toPx() + y.toPx()
            return bitmap.getPixel(left.toInt(), top.toInt())
        }
    }

    private companion object {
        const val CARD = "card"
        const val SKELETON = "skeleton"
        const val NAME = "Tựa lan can"

        // One line at 156 dp, even at font scale 2.0.
        const val SHORT_NAME = "Tựa cửa"
        const val META = "Một người · 4 bước"
        const val LONG_NAME = "Tựa lan can nhìn về phía hoàng hôn trên bờ biển vào một buổi chiều có gió nhẹ"
        val CARD_WIDTH = 182.dp
        val CATEGORY = Color(0xFF4D8DFF)
        const val PREMIUM_LABEL = "Premium pose"
    }
}
