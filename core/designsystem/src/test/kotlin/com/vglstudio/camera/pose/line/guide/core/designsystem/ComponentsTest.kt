package com.vglstudio.camera.pose.line.guide.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.BrandLogo
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PrimaryButton
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.ProgressBar
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/**
 * UI behaviour of the shared components, run on the JVM with Robolectric (no emulator needed).
 * Native graphics give real text sizes, so sizes measured here are the ones drawn on a device.
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

        // The icon (18 dp) and its gap (8 dp) sit before the text, so the text starts 26 dp further in.
        val withIconOffset = textOffset(WITH_ICON, "With icon")
        val noIconOffset = textOffset(NO_ICON, "No icon")
        assertEquals(26f, (withIconOffset - noIconOffset).value, 0.5f)

        composeRule.onNodeWithText("With icon").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun primaryButton_height_isAtLeast52dp() {
        composeRule.setContent {
            AppTheme { PrimaryButton(text = "Button", onClick = {}, modifier = Modifier.testTag(NO_ICON)) }
        }
        composeRule.onNodeWithTag(NO_ICON).assertHeightIsAtLeast(52.dp)
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
    fun brandLogo_is132dpSquare() {
        composeRule.setContent {
            AppTheme { BrandLogo(modifier = Modifier.testTag(LOGO)) }
        }
        composeRule.onNodeWithTag(LOGO).assertWidthIsEqualTo(132.dp).assertHeightIsEqualTo(132.dp)
    }

    private fun textOffset(buttonTag: String, text: String) =
        composeRule.onNodeWithText(text, useUnmergedTree = true).getUnclippedBoundsInRoot().left -
            composeRule.onNodeWithTag(buttonTag).getUnclippedBoundsInRoot().left

    private companion object {
        const val WITH_ICON = "withIcon"
        const val NO_ICON = "noIcon"
        const val PROGRESS = "progress"
        const val LOGO = "logo"
    }
}
