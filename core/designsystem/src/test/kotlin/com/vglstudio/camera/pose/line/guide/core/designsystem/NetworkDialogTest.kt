package com.vglstudio.camera.pose.line.guide.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.NetworkDialogScreen
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.NetworkDialogTestTags
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The network dialog on the JVM (spec network, KB-1 and KB-2; design network, text table and sizes). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "vi-w411dp-h891dp-xxhdpi")
class NetworkDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setDialog(checking: Boolean = false, stillOffline: Boolean = false, onRetry: () -> Unit = {}) {
        composeRule.setContent {
            AppTheme { NetworkDialogScreen(checking = checking, stillOffline = stillOffline, onRetry = onRetry) }
        }
    }

    @Test
    fun default_showsTextOfDesignTable() {
        setDialog()

        composeRule.onNodeWithText(TITLE).assertExists()
        composeRule.onNodeWithText(MESSAGE).assertExists()
        composeRule.onNodeWithText(RETRY, useUnmergedTree = true).assertExists()
        composeRule.onAllNodesWithText(STILL_OFFLINE).assertCountEquals(0)
    }

    @Test
    fun card_is331dpWide_andRetryAtLeast48dp() {
        setDialog()

        composeRule.onNodeWithTag(NetworkDialogTestTags.CARD).assertWidthIsEqualTo(331.dp)
        val retry = composeRule.onNodeWithTag(NetworkDialogTestTags.RETRY).getUnclippedBoundsInRoot()
        assertTrue("height ${retry.height}", retry.height >= 48.dp)
        val expectedWidth = 331.dp - Spacing.lg * 2
        assertEquals(expectedWidth.value, retry.width.value, 0.5f)
    }

    @Test
    fun screen_consumesTouches() {
        var behind = 0
        var retries = 0
        composeRule.setContent {
            AppTheme {
                Box(Modifier.fillMaxSize()) {
                    Button(onClick = { behind++ }, modifier = Modifier.testTag(BEHIND)) { Text("Behind") }
                    NetworkDialogScreen(checking = false, stillOffline = false, onRetry = { retries++ })
                }
            }
        }

        composeRule.onNodeWithTag(BEHIND).performClick()
        composeRule.onNodeWithTag(NetworkDialogTestTags.SCREEN).performTouchInput { click(topLeft + Offset(10f, 10f)) }

        assertEquals(0, behind)
        assertEquals(0, retries)
        composeRule.onNodeWithTag(NetworkDialogTestTags.CARD).assertExists()
    }

    @Test
    fun retry_click_callsOnRetry() {
        var retries = 0
        setDialog(onRetry = { retries++ })

        composeRule.onNodeWithTag(NetworkDialogTestTags.RETRY).performClick()

        assertEquals(1, retries)
    }

    @Test
    fun checking_showsSpinner_andIgnoresClicks() {
        var retries = 0
        setDialog(checking = true, onRetry = { retries++ })

        composeRule.onNodeWithTag(NetworkDialogTestTags.SPINNER, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(NetworkDialogTestTags.RETRY).performClick()

        assertEquals(0, retries)
    }

    @Test
    fun stillOffline_showsMessageBelowButton() {
        setDialog(stillOffline = true)

        val message = composeRule.onNodeWithText(STILL_OFFLINE).getUnclippedBoundsInRoot()
        val retry = composeRule.onNodeWithTag(NetworkDialogTestTags.RETRY).getUnclippedBoundsInRoot()
        assertTrue("message top ${message.top}, retry bottom ${retry.bottom}", message.top >= retry.bottom)
    }

    @Test
    @Config(qualifiers = "vi-w360dp-h640dp-xxhdpi")
    fun smallScreen_cardFitsAndRetryVisible() {
        setDialog(stillOffline = true)

        val screen = composeRule.onRoot().getUnclippedBoundsInRoot()
        val card = composeRule.onNodeWithTag(NetworkDialogTestTags.CARD).getUnclippedBoundsInRoot()
        assertTrue("card $card in $screen", card.left >= screen.left && card.right <= screen.right)
        assertTrue("card $card in $screen", card.top >= screen.top && card.bottom <= screen.bottom)
        composeRule.onNodeWithTag(NetworkDialogTestTags.RETRY).assertIsDisplayed()
    }

    private companion object {
        const val TITLE = "Mất kết nối mạng"
        const val MESSAGE = "App cần mạng để hoạt động. Kiểm tra kết nối Internet rồi thử lại."
        const val RETRY = "Thử lại"
        const val STILL_OFFLINE = "Vẫn chưa có mạng"
        const val BEHIND = "behind"
    }
}
