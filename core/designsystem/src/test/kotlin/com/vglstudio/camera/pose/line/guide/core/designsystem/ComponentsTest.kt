package com.vglstudio.camera.pose.line.guide.core.designsystem

import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.BrandLogo
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PrimaryButton
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.ProgressBar
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.SecondaryButton
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/**
 * UI behaviour of the shared components, run on the JVM with Robolectric (no emulator needed).
 * Native graphics give real text sizes and real pixels, so sizes and colors measured here are the ones drawn on a
 * device.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ComponentsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun primaryButton_withIcon_showsIconAndText_andClicks() {
        var clicks = 0
        composeRule.setContent {
            AppTheme {
                Column {
                    PrimaryButton(
                        text = "With icon",
                        onClick = { clicks++ },
                        modifier = Modifier.testTag(WITH_ICON),
                        icon = painterResource(R.drawable.brand_mark),
                    )
                    PrimaryButton(text = "No icon", onClick = {}, modifier = Modifier.testTag(NO_ICON))
                }
            }
        }

        // The icon (20 dp) and its gap (8 dp) sit before the text, so the text starts 28 dp further in.
        val withIconOffset = textOffset(WITH_ICON, "With icon")
        val noIconOffset = textOffset(NO_ICON, "No icon")
        assertEquals(28f, (withIconOffset - noIconOffset).value, 0.5f)

        composeRule.onNodeWithText("With icon").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun primaryButton_height_is56dp() {
        composeRule.setContent {
            AppTheme { PrimaryButton(text = "Button", onClick = {}, modifier = Modifier.testTag(NO_ICON)) }
        }
        composeRule.onNodeWithTag(NO_ICON).assertHeightIsEqualTo(56.dp)
    }

    @Test
    fun primaryButton_fill_isSecondary_light() {
        composeRule.setContent {
            AppTheme(darkTheme = false) { PrimaryButton(text = "Button", onClick = {}, modifier = Modifier.testTag(NO_ICON)) }
        }
        assertEquals(Color(0xFF111111).toArgb(), pixelAt(NO_ICON, 10.dp, 28.dp).toArgb())
    }

    @Test
    fun primaryButton_fill_isSecondary_dark() {
        composeRule.setContent {
            AppTheme(darkTheme = true) { PrimaryButton(text = "Button", onClick = {}, modifier = Modifier.testTag(NO_ICON)) }
        }
        assertEquals(Color(0xFFFFFFFF).toArgb(), pixelAt(NO_ICON, 10.dp, 28.dp).toArgb())
    }

    @Test
    fun primaryButton_drawsHardShadow() {
        composeRule.setContent {
            AppTheme(darkTheme = false) {
                Box(Modifier.testTag(NO_ICON).padding(8.dp)) { PrimaryButton(text = "Button", onClick = {}) }
            }
        }
        val bounds = composeRule.onNodeWithTag(NO_ICON).getUnclippedBoundsInRoot()
        // Middle of the button horizontally, 2 dp below its bottom edge (the box has an 8 dp margin, the button is 56).
        val shadow = pixelAt(NO_ICON, (bounds.right - bounds.left) / 2, 8.dp + 56.dp + 2.dp)
        assertEquals(Color(0xFFFFC83D).toArgb(), shadow.toArgb())
    }

    @Test
    fun secondaryButton_height_is56dp_andClicks() {
        var clicks = 0
        composeRule.setContent {
            AppTheme {
                SecondaryButton(text = "Secondary", onClick = { clicks++ }, modifier = Modifier.testTag(SECONDARY))
            }
        }
        composeRule.onNodeWithTag(SECONDARY).assertHeightIsEqualTo(56.dp)
        composeRule.onNodeWithText("Secondary").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun secondaryButton_fill_isSurface_dark() {
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                SecondaryButton(text = "Secondary", onClick = {}, modifier = Modifier.testTag(SECONDARY))
            }
        }
        assertEquals(Color(0xFF1E1E1E).toArgb(), pixelAt(SECONDARY, 10.dp, 28.dp).toArgb())
    }

    @Test
    fun secondaryButton_hasNoShadow() {
        composeRule.setContent {
            AppTheme(darkTheme = false) {
                Box(Modifier.testTag(SECONDARY).padding(8.dp)) { SecondaryButton(text = "Secondary", onClick = {}) }
            }
        }
        val bounds = composeRule.onNodeWithTag(SECONDARY).getUnclippedBoundsInRoot()
        val below = pixelAt(SECONDARY, (bounds.right - bounds.left) / 2, 8.dp + 56.dp + 2.dp)
        assertNotEquals(Color(0xFFFFC83D).toArgb(), below.toArgb())
    }

    @Test
    fun progressBar_reportsProgress() {
        composeRule.setContent {
            AppTheme { ProgressBar(progress = { 0.68f }, modifier = Modifier.testTag(PROGRESS)) }
        }
        composeRule.onNodeWithTag(PROGRESS).assertRangeInfoEquals(ProgressBarRangeInfo(0.68f, 0f..1f))
    }

    @Test
    fun progressBar_height_is8dp() {
        // The indicator widens its own semantics bounds for accessibility, so measure the laid-out size through a
        // wrapping box instead.
        composeRule.setContent {
            AppTheme { Box(modifier = Modifier.testTag(PROGRESS)) { ProgressBar(progress = { 0.68f }) } }
        }
        composeRule.onNodeWithTag(PROGRESS).assertHeightIsEqualTo(8.dp)
    }

    @Test
    fun progressBar_defaultFill_isSecondaryContainer() {
        composeRule.setContent {
            AppTheme(darkTheme = false) {
                Box(Modifier.width(200.dp).testTag(PROGRESS)) { ProgressBar(progress = { 0.68f }) }
            }
        }
        assertEquals(Color(0xFFFFC83D).toArgb(), pixelAt(PROGRESS, 40.dp, 4.dp).toArgb())
    }

    @Test
    fun brandLogo_is112dpSquare() {
        composeRule.setContent {
            AppTheme { BrandLogo(modifier = Modifier.testTag(LOGO)) }
        }
        composeRule.onNodeWithTag(LOGO).assertWidthIsEqualTo(112.dp).assertHeightIsEqualTo(112.dp)
    }

    @Test
    fun brandLogo_fill_isSecondaryContainer() {
        composeRule.setContent {
            AppTheme(darkTheme = false) { BrandLogo(modifier = Modifier.testTag(LOGO)) }
        }
        assertEquals(Color(0xFFFFC83D).toArgb(), pixelAt(LOGO, 56.dp, 8.dp).toArgb())
    }

    /**
     * Color of the pixel at ([x], [y]) inside the node; dp are converted with the density of the test. The node is
     * captured with Roborazzi in record mode to a scratch file under build/ (not a reference image) and read back,
     * because captureToImage times out in Robolectric native graphics.
     */
    private fun pixelAt(tag: String, x: Dp, y: Dp): Color {
        val path = "build/tmp/pixel/$tag.png"
        composeRule.onNodeWithTag(tag).captureRoboImage(
            filePath = path,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record),
        )
        val bitmap = checkNotNull(BitmapFactory.decodeFile(path)) { "No image written to $path" }
        with(composeRule.density) { return Color(bitmap.getPixel(x.roundToPx(), y.roundToPx())) }
    }

    private fun textOffset(buttonTag: String, text: String) =
        composeRule.onNodeWithText(text, useUnmergedTree = true).getUnclippedBoundsInRoot().left -
            composeRule.onNodeWithTag(buttonTag).getUnclippedBoundsInRoot().left

    private companion object {
        const val WITH_ICON = "withIcon"
        const val NO_ICON = "noIcon"
        const val SECONDARY = "secondary"
        const val PROGRESS = "progress"
        const val LOGO = "logo"
    }
}
