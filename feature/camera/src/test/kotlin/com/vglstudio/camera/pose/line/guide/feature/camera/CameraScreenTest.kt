package com.vglstudio.camera.pose.line.guide.feature.camera

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.feature.camera.CameraUiState.Ready
import com.vglstudio.camera.pose.line.guide.feature.camera.model.FlashMode
import com.vglstudio.camera.pose.line.guide.feature.camera.model.Lens
import com.vglstudio.camera.pose.line.guide.feature.camera.model.TimerSetting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The stateless camera screen on the JVM (spec camera, KB-4 to KB-8). Text follows the text table of the design
 * (docs/features/camera/design/README.md, section 2).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "vi-w411dp-h891dp-xxhdpi")
class CameraScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()

    private fun setScreen(
        uiState: CameraUiState,
        onOpenPoses: (() -> Unit)? = { calls += "poses" },
        onOpenSettings: (() -> Unit)? = { calls += "settings" },
    ) {
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                CameraScreen(
                    uiState = uiState,
                    onFlashClick = { calls += "flash" },
                    onTimerClick = { calls += "timer" },
                    onGridClick = { calls += "grid" },
                    onFlipClick = { calls += "flip" },
                    onZoomSelect = { calls += "zoom $it" },
                    onShutterClick = { calls += "shutter" },
                    onCancelCountdown = { calls += "cancel $it" },
                    onRetry = { calls += "retry" },
                    onOpenAppSettings = { calls += "appSettings" },
                    onOpenPoses = onOpenPoses,
                    onOpenSettings = onOpenSettings,
                )
            }
        }
    }

    private fun hasDescription(text: String) = SemanticsMatcher.expectValue(
        SemanticsProperties.ContentDescription,
        listOf(text),
    )

    @Test
    fun starting_showsSpinnerText_andDisablesShutter() {
        setScreen(CameraUiState.Starting)
        composeRule.onNodeWithText("Đang mở camera…").assertExists()
        composeRule.onNodeWithTag(CameraTestTags.SHUTTER).assertIsNotEnabled()
        listOf(CameraTestTags.FLASH, CameraTestTags.TIMER, CameraTestTags.GRID_BUTTON, CameraTestTags.ZOOM).forEach {
            composeRule.onNodeWithTag(it).assertDoesNotExist()
        }
    }

    @Test
    fun permissionDenied_showsMessage_andOpensSettings() {
        setScreen(CameraUiState.PermissionDenied)
        composeRule.onNodeWithText("Chưa có quyền dùng camera").assertExists()
        composeRule.onNodeWithText("Cho phép camera trong Cài đặt máy để chụp theo tư thế.").assertExists()
        composeRule.onNodeWithText("Mở cài đặt").performClick()
        assertEquals(listOf("appSettings"), calls)
        composeRule.onNodeWithTag(CameraTestTags.SHUTTER).assertIsNotEnabled()
    }

    @Test
    fun error_showsMessage_andRetries() {
        setScreen(CameraUiState.Error)
        composeRule.onNodeWithText("Không mở được camera").assertExists()
        composeRule.onNodeWithText("Camera có thể đang được ứng dụng khác dùng. Đóng ứng dụng đó rồi thử lại.")
            .assertExists()
        composeRule.onNodeWithText("Thử lại").performClick()
        assertEquals(listOf("retry"), calls)
    }

    @Test
    fun flash_showsModeAndClicks() {
        setScreen(READY.copy(flash = FlashMode.AUTO))
        composeRule.onNodeWithTag(CameraTestTags.FLASH).assert(hasDescription("Flash: tự động")).performClick()
        assertEquals(listOf("flash"), calls)
    }

    @Test
    fun flash_unavailable_isDisabled() {
        setScreen(READY.copy(flashAvailable = false))
        composeRule.onNodeWithTag(CameraTestTags.FLASH).assertIsNotEnabled()
    }

    @Test
    fun timer_active_showsSecondsLabel() {
        var state by mutableStateOf<CameraUiState>(READY.copy(timer = TimerSetting.THREE))
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                CameraScreen(
                    uiState = state,
                    onFlashClick = {},
                    onTimerClick = {},
                    onGridClick = {},
                    onFlipClick = {},
                    onZoomSelect = {},
                    onShutterClick = {},
                    onCancelCountdown = {},
                    onRetry = {},
                    onOpenAppSettings = {},
                    onOpenPoses = null,
                    onOpenSettings = null,
                )
            }
        }
        composeRule.onNodeWithText("3s").assertExists()
        composeRule.onNodeWithTag(CameraTestTags.TIMER).assertIsSelected()

        state = READY.copy(timer = TimerSetting.TEN)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("10s").assertExists()

        state = READY.copy(timer = TimerSetting.OFF)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("3s").assertDoesNotExist()
        composeRule.onNodeWithText("10s").assertDoesNotExist()
        composeRule.onNodeWithTag(CameraTestTags.TIMER).assertIsNotSelected()
    }

    @Test
    fun grid_visibleOnlyWhenOn() {
        var state by mutableStateOf<CameraUiState>(READY.copy(gridVisible = true))
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                CameraScreen(
                    uiState = state,
                    onFlashClick = {},
                    onTimerClick = {},
                    onGridClick = {},
                    onFlipClick = {},
                    onZoomSelect = {},
                    onShutterClick = {},
                    onCancelCountdown = {},
                    onRetry = {},
                    onOpenAppSettings = {},
                    onOpenPoses = null,
                    onOpenSettings = null,
                )
            }
        }
        composeRule.onNodeWithTag(CameraTestTags.GRID).assertExists()
        composeRule.onNodeWithTag(CameraTestTags.GRID_BUTTON).assertIsSelected()

        state = READY.copy(gridVisible = false)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(CameraTestTags.GRID).assertDoesNotExist()
    }

    @Test
    fun zoom_showsLevels_andSelects() {
        setScreen(READY.copy(zoomLevels = listOf(0.5f, 1f, 2f), zoom = 1f))
        composeRule.onNodeWithText("0.5").assertExists()
        composeRule.onNodeWithText("1x").assertExists()
        composeRule.onNodeWithText("2").performClick()
        assertEquals(listOf("zoom 2.0"), calls)
    }

    @Test
    fun zoom_withoutUltraWide_hides05() {
        setScreen(READY.copy(zoomLevels = listOf(1f, 2f), zoom = 1f))
        composeRule.onNodeWithText("0.5").assertDoesNotExist()
        composeRule.onNodeWithText("1x").assertExists()
    }

    @Test
    fun zoom_singleLevel_hidden() {
        setScreen(READY.copy(zoomLevels = listOf(1f), zoom = 1f))
        composeRule.onNodeWithTag(CameraTestTags.ZOOM).assertDoesNotExist()
    }

    @Test
    fun flip_clicks() {
        setScreen(READY)
        composeRule.onNodeWithTag(CameraTestTags.FLIP).performClick()
        assertEquals(listOf("flip"), calls)
    }

    @Test
    fun countdown_showsNumber_andShutterCancels() {
        setScreen(READY.copy(timer = TimerSetting.THREE, countdown = 3))
        composeRule.onNodeWithTag(CameraTestTags.COUNTDOWN, useUnmergedTree = true).assertTextEquals("3")
        composeRule.onNodeWithTag(CameraTestTags.SHUTTER).assert(hasDescription("Hủy đếm ngược")).performClick()
        assertEquals(listOf("cancel BUTTON"), calls)
    }

    // KB-6 (plan T-061): switching the camera stays possible while counting down; the view model stops the count.
    @Test
    fun countdown_flipCancelsThenFlips() {
        setScreen(READY.copy(timer = TimerSetting.THREE, countdown = 3))
        composeRule.waitForIdle()
        calls.clear()
        composeRule.onNodeWithTag(CameraTestTags.FLIP).assertIsEnabled().performClick()
        assertEquals(listOf("flip"), calls)
    }

    @Test
    fun ready_shutterClicks() {
        setScreen(READY)
        composeRule.onNodeWithTag(CameraTestTags.SHUTTER).assert(hasDescription("Chụp ảnh")).assertIsEnabled()
            .performClick()
        assertEquals(listOf("shutter"), calls)
    }

    @Test
    fun lastPhoto_isEmptyAndNotClickable() {
        setScreen(READY)
        composeRule.onNodeWithTag(CameraTestTags.LAST_PHOTO).assertExists().assertHasNoClickAction()
    }

    @Test
    fun topButtons_hiddenWithoutCallbacks() {
        setScreen(READY, onOpenPoses = null, onOpenSettings = null)
        composeRule.onNodeWithTag(CameraTestTags.POSES).assertDoesNotExist()
        composeRule.onNodeWithTag(CameraTestTags.SETTINGS).assertDoesNotExist()
    }

    @Test
    fun topButtons_shownWithCallbacks_andClick() {
        setScreen(READY)
        composeRule.onNodeWithTag(CameraTestTags.POSES).performClick()
        composeRule.onNodeWithTag(CameraTestTags.SETTINGS).performClick()
        assertEquals(listOf("poses", "settings"), calls)
    }

    @Test
    @Config(qualifiers = SHORT_SCREEN)
    fun small_countdown_shutterVisibleAndLargeEnough() {
        setScreen(READY.copy(timer = TimerSetting.THREE, gridVisible = true, countdown = 3))
        val rootHeight = composeRule.onRoot().getUnclippedBoundsInRoot().height
        val shutter = composeRule.onNodeWithTag(CameraTestTags.SHUTTER).getUnclippedBoundsInRoot()
        assertTrue("shutter bottom ${shutter.bottom} is below the screen $rootHeight", shutter.bottom <= rootHeight)
        composeRule.onNodeWithTag(CameraTestTags.SHUTTER).assertHeightIsEqualTo(76.dp)
        composeRule.onNodeWithTag(CameraTestTags.FLIP).assertHeightIsEqualTo(48.dp)
        val preview = composeRule.onNodeWithTag(CameraTestTags.PREVIEW).getUnclippedBoundsInRoot()
        assertTrue(preview.height.value > 0f)
        // 3:4, within one pixel (xxhdpi: 1 px is 1/3 dp).
        assertEquals(0.75f, preview.width.value / preview.height.value, 1f / 3f / preview.height.value)
    }

    @Test
    @Config(qualifiers = SHORT_SCREEN)
    fun small_permissionDenied_buttonVisible() {
        setScreen(CameraUiState.PermissionDenied)
        val preview = composeRule.onNodeWithTag(CameraTestTags.PREVIEW).getUnclippedBoundsInRoot()
        val button = composeRule.onNodeWithText("Mở cài đặt").getUnclippedBoundsInRoot()
        assertTrue("button $button is outside the preview $preview", button.top >= preview.top)
        assertTrue("button $button is outside the preview $preview", button.bottom <= preview.bottom)
        assertTrue(button.left >= preview.left && button.right <= preview.right)
    }

    // Portuguese is usually the longest of the release languages (plan T-050): the button still fits the frame.
    @Test
    @Config(qualifiers = "pt-rBR-w360dp-h640dp-xxhdpi")
    fun smallPortuguese_permissionDenied_buttonVisible() {
        setScreen(CameraUiState.PermissionDenied)
        val action = ApplicationProvider.getApplicationContext<Context>().getString(R.string.camera_permission_action)
        val preview = composeRule.onNodeWithTag(CameraTestTags.PREVIEW).getUnclippedBoundsInRoot()
        val button = composeRule.onNodeWithText(action).getUnclippedBoundsInRoot()
        assertTrue("button $button is outside the preview $preview", button.top >= preview.top)
        assertTrue("button $button is outside the preview $preview", button.bottom <= preview.bottom)
        assertTrue(button.left >= preview.left && button.right <= preview.right)
    }

    private companion object {
        const val SHORT_SCREEN = "vi-w360dp-h640dp-xxhdpi"

        val READY = Ready(
            flash = FlashMode.OFF,
            flashAvailable = true,
            timer = TimerSetting.OFF,
            gridVisible = false,
            lens = Lens.BACK,
            zoomLevels = listOf(1f, 2f),
            zoom = 1f,
        )
    }
}
