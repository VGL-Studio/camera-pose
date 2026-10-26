package com.vglstudio.camera.pose.line.guide.feature.camera

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.percentOffset
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.feature.camera.CameraScreenScreenshotTest.Companion.ADJUSTING
import com.vglstudio.camera.pose.line.guide.feature.camera.CameraScreenScreenshotTest.Companion.DEFAULT
import com.vglstudio.camera.pose.line.guide.feature.camera.CameraScreenScreenshotTest.Companion.SHORT_SCREEN
import com.vglstudio.camera.pose.line.guide.feature.camera.CameraScreenScreenshotTest.Companion.samplePoses
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.PoseActions
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.PoseUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The camera screen while counting down, phone layout (spec camera, KB-6, question 16; plan T-061): the buttons that
 * change the photo are faded and locked, a tap on the frame stops the count, a drag or a pinch does nothing, the zoom
 * still works, and leaving the screen stops the count first.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "vi-w411dp-h891dp-xxhdpi")
class CameraScreenCountdownTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()

    private val actions = PoseActions(
        onCategorySelect = { calls += "category $it" },
        onPoseSelect = { calls += "pose $it" },
        onPreviousStep = { calls += "previous" },
        onNextStep = { calls += "next" },
        onHideSteps = { calls += "hide" },
        onShowSteps = { calls += "show" },
        onOverlayTransform = { _, _, _ -> calls += "transform" },
        onOverlayGestureEnd = { calls += "gestureEnd" },
        onTogglePanel = { calls += "togglePanel" },
        onClosePanel = { calls += "closePanel" },
        onOpacityChange = { calls += "adjust" },
        onScaleChange = { calls += "adjust" },
        onToggleMirror = { calls += "mirror" },
        onToggleLock = { calls += "lock" },
        onToggleHidden = { calls += "visibility" },
    )

    private var cameraState by mutableStateOf<CameraUiState>(COUNTING)

    private fun setScreen(
        camera: CameraUiState = COUNTING,
        poseState: PoseUiState = samplePoses(),
        clear: Boolean = true,
    ) {
        cameraState = camera
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                CameraScreen(
                    uiState = cameraState,
                    onFlashClick = { calls += "flash" },
                    onTimerClick = { calls += "timer" },
                    onGridClick = { calls += "grid" },
                    onFlipClick = { calls += "flip" },
                    onZoomSelect = { calls += "zoom $it" },
                    onShutterClick = { calls += "shutter" },
                    onCancelCountdown = { calls += "cancel $it" },
                    onRetry = {},
                    onOpenAppSettings = {},
                    onOpenPoses = { calls += "poses" },
                    onOpenSettings = { calls += "settings" },
                    poseState = poseState,
                    poseActions = actions,
                    onLastPhotoClick = { calls += "lastPhoto" },
                )
            }
        }
        composeRule.waitForIdle()
        if (clear) calls.clear()
    }

    private fun showState(camera: CameraUiState) {
        cameraState = camera
        composeRule.waitForIdle()
        calls.clear()
    }

    private fun tap(tag: String) {
        composeRule.onNodeWithTag(tag).performTouchInput { click() }
        composeRule.waitForIdle()
    }

    // KB-6, new 1: flash, timer and grid are locked; the library and the settings are not.

    @Test
    fun counting_topToolsDisabled() {
        setScreen()
        val locked = listOf(CameraTestTags.FLASH, CameraTestTags.TIMER, CameraTestTags.GRID_BUTTON)
        locked.forEach { composeRule.onNodeWithTag(it).assertIsNotEnabled() }
        locked.forEach { tap(it) }
        assertEquals(emptyList<String>(), calls)
        composeRule.onNodeWithTag(CameraTestTags.POSES).assertIsEnabled()
        composeRule.onNodeWithTag(CameraTestTags.SETTINGS).assertIsEnabled()

        // Control: not counting, the three buttons are enabled.
        showState(DEFAULT)
        locked.forEach { composeRule.onNodeWithTag(it).assertIsEnabled() }
    }

    // KB-6, new 1: faded to 0.38, as on the design; measured on the drawn window (L-006).

    @Test
    fun counting_lockedControlsFaded() {
        setScreen()
        val counting = fadedPoints()
        showState(DEFAULT)
        val idle = fadedPoints()

        assertTrue(
            "flash ${counting.flash} is not below 60% of settings ${counting.settings}",
            counting.flash < counting.settings * 0.6f,
        )
        assertTrue("chip ${counting.chip} not darker than ${idle.chip}", counting.chip < idle.chip)
        assertTrue("thumb ${counting.thumb} not darker than ${idle.thumb}", counting.thumb < idle.thumb)
        // The badge of "Tất cả" sticks out of the chip: faded with the row, not cut off.
        assertTrue("badge red ${counting.badgeRed} while counting", counting.badgeRed in 60..150)
        assertTrue("badge red ${idle.badgeRed} when idle", idle.badgeRed > 200)
    }

    private data class FadedPoints(val flash: Int, val settings: Int, val chip: Int, val thumb: Int, val badgeRed: Int)

    private fun fadedPoints(): FadedPoints {
        val bitmap = windowBitmap()
        // On the white fill of the 48 dp round buttons, left of the icon.
        val flash = lumaAt(bitmap, boundsOf(CameraTestTags.FLASH, byText = false), 8.dp, 24.dp)
        val settings = lumaAt(bitmap, boundsOf(CameraTestTags.SETTINGS, byText = false), 8.dp, 24.dp)
        // On the fill, left of the label of "Nữ" and of the figure of the first cell.
        val chipBounds = boundsOf("Nữ", byText = true)
        val chip = lumaAt(bitmap, chipBounds, 6.dp, px(chipBounds.height / 2))
        val thumb = lumaAt(bitmap, boundsOf(CameraTestTags.pose("p1"), byText = false), 6.dp, 32.dp)
        val all = boundsOf("Tất cả", byText = true)
        val badge = with(composeRule.density) {
            bitmap.getPixel((all.right + 4.dp.toPx()).toInt(), (all.top - 5.dp.toPx()).toInt())
        }
        return FadedPoints(flash, settings, chip, thumb, (Color(badge).red * 255).toInt())
    }

    // KB-6, new 1: the chips and the strip do not answer and do not scroll.

    @Test
    fun counting_chipsAndStripDisabled() {
        setScreen()
        composeRule.onNodeWithText("Nữ").assertIsNotEnabled().performTouchInput { click() }
        tap(CameraTestTags.pose("p3"))
        composeRule.onNodeWithTag(CameraTestTags.pose("p3")).assertIsNotEnabled()
        assertTrue("calls $calls", calls.none { it.startsWith("category") || it.startsWith("pose") })

        val before = composeRule.onNodeWithTag(CameraTestTags.pose("p1")).getUnclippedBoundsInRoot().left
        composeRule.onNodeWithTag(CameraTestTags.STRIP).performTouchInput {
            swipeLeft(startX = right - 20f, endX = right - 20f - 200.dp.toPx())
        }
        composeRule.waitForIdle()
        val after = composeRule.onNodeWithTag(CameraTestTags.pose("p1")).getUnclippedBoundsInRoot().left
        assertEquals("first cell moved", before, after)
    }

    // KB-6, new 1: the tool column and the step arrows are locked.

    @Test
    fun counting_toolsAndArrowsDisabled() {
        setScreen()
        listOf(
            CameraTestTags.TOOL_ADJUST,
            CameraTestTags.TOOL_MIRROR,
            CameraTestTags.TOOL_LOCK,
            CameraTestTags.TOOL_VISIBILITY,
        ).forEach { composeRule.onNodeWithTag(it).assertIsNotEnabled() }
        composeRule.onNodeWithContentDescription(PREVIOUS).assertIsNotEnabled()
        composeRule.onNodeWithContentDescription(NEXT).assertIsNotEnabled()

        // Control: not counting, step 2 of 4, both arrows and the tools can be used.
        showState(DEFAULT)
        composeRule.onNodeWithContentDescription(NEXT).assertIsEnabled()
        composeRule.onNodeWithContentDescription(PREVIOUS).assertIsEnabled()
        composeRule.onNodeWithTag(CameraTestTags.TOOL_ADJUST).assertIsEnabled()
    }

    // KB-6, new 1: on a short screen the tool column may be outside the frame; it is locked too.

    @Test
    @Config(qualifiers = SHORT_SCREEN)
    fun small_counting_toolsDisabled() {
        setScreen()
        composeRule.onNodeWithTag(CameraTestTags.TOOL_ADJUST).assertIsNotEnabled()
    }

    // KB-6, new 1: an open adjust panel closes when the count starts.

    @Test
    fun counting_closesOpenPanel() {
        // Control first: the panel open while not counting stays open.
        setScreen(camera = DEFAULT, poseState = samplePoses(overlay = ADJUSTING), clear = false)
        assertTrue("calls $calls", "closePanel" !in calls)

        cameraState = COUNTING
        composeRule.waitForIdle()
        assertEquals(listOf("closePanel"), calls)
    }

    // KB-6, new 1: a drag or a pinch on the frame does not move the outline and does not stop the count.

    @Test
    fun counting_dragOnFrame_doesNothing() {
        setScreen()
        composeRule.onNodeWithTag(CameraTestTags.OVERLAY_GESTURES).performTouchInput {
            swipe(Offset(width * 0.3f, height * 0.6f), Offset(width * 0.3f, height * 0.8f))
        }
        composeRule.waitForIdle()
        assertEquals(emptyList<String>(), calls)
    }

    @Test
    fun counting_pinchOnFrame_doesNothing() {
        setScreen()
        composeRule.onNodeWithTag(CameraTestTags.PREVIEW).performTouchInput {
            pinch(
                start0 = center - Offset(50f, 0f),
                end0 = center - Offset(150f, 0f),
                start1 = center + Offset(50f, 0f),
                end1 = center + Offset(150f, 0f),
            )
        }
        composeRule.waitForIdle()
        assertEquals(emptyList<String>(), calls)
    }

    // KB-6, new 1: a screen reader cannot hide the step card while counting.

    @Test
    fun counting_stepCardHasNoHideAction() {
        setScreen()
        val counting = composeRule.onNodeWithTag(CameraTestTags.STEP_CARD).fetchSemanticsNode().config
        assertTrue("custom actions while counting", SemanticsActions.CustomActions !in counting)
        assertTrue(
            "the card can be hidden while counting",
            SemanticsActions.OnClick !in counting || SemanticsProperties.Disabled in counting,
        )

        // Control: not counting, there is one way to hide it.
        showState(DEFAULT)
        val idle = composeRule.onNodeWithTag(CameraTestTags.STEP_CARD).fetchSemanticsNode().config
        val byClick = SemanticsActions.OnClick in idle && SemanticsProperties.Disabled !in idle
        val byAction = SemanticsActions.CustomActions in idle &&
            idle[SemanticsActions.CustomActions].any { it.label == HIDE_STEPS }
        assertTrue("no way to hide the card when idle", byClick || byAction)
    }

    // KB-6, new 2: a tap anywhere on the frame but the zoom stops the count.

    @Test
    fun counting_tapFrame_cancels() {
        setScreen()
        composeRule.onNodeWithTag(CameraTestTags.PREVIEW).performTouchInput { click(percentOffset(0.2f, 0.7f)) }
        composeRule.waitForIdle()
        assertEquals(listOf("cancel PREVIEW"), calls)
    }

    @Test
    fun counting_tapStepCard_cancelsWithoutHiding() {
        setScreen()
        composeRule.onNodeWithTag(CameraTestTags.STEP_CARD).performTouchInput {
            click(Offset(width * 0.3f, height / 2f))
        }
        composeRule.waitForIdle()
        assertEquals(listOf("cancel PREVIEW"), calls)
    }

    @Test
    fun counting_tapOutlineOrTool_cancels() {
        setScreen()
        composeRule.onNodeWithTag(CameraTestTags.OUTLINE).performTouchInput { click(percentOffset(0.5f, 0.15f)) }
        composeRule.waitForIdle()
        tap(CameraTestTags.TOOL_ADJUST)
        assertEquals(listOf("cancel PREVIEW", "cancel PREVIEW"), calls)
    }

    // KB-6, new 1 and 2: the zoom stays usable and does not stop the count.

    @Test
    fun counting_zoomStillWorks() {
        setScreen()
        composeRule.onNodeWithText("2").performClick()
        composeRule.waitForIdle()
        assertEquals(listOf("zoom 2.0"), calls)
    }

    // KB-6, new 2: leaving the screen stops the count first, then opens the other screen.

    @Test
    fun counting_leaveCancelsThenOpens() {
        setScreen()
        tap(CameraTestTags.POSES)
        tap(CameraTestTags.SETTINGS)
        tap(CameraTestTags.LAST_PHOTO)
        assertEquals(
            listOf("cancel LEAVE", "poses", "cancel LEAVE", "settings", "cancel LEAVE", "lastPhoto"),
            calls,
        )

        // Control: not counting, nothing to stop.
        showState(DEFAULT)
        tap(CameraTestTags.POSES)
        assertEquals(listOf("poses"), calls)
    }

    private fun boundsOf(key: String, byText: Boolean) = if (byText) {
        composeRule.onNodeWithText(key).fetchSemanticsNode().boundsInWindow
    } else {
        composeRule.onNodeWithTag(key).fetchSemanticsNode().boundsInWindow
    }

    private fun px(value: Float): Dp = with(composeRule.density) { value.toDp() }

    private fun windowBitmap(): Bitmap {
        composeRule.waitForIdle()
        val root = (composeRule as AndroidComposeTestRule<*, *>).activity.window.decorView
        return Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888).also { root.draw(Canvas(it)) }
    }

    private fun lumaAt(bitmap: Bitmap, bounds: Rect, x: Dp, y: Dp): Int {
        val pixel = with(composeRule.density) {
            bitmap.getPixel((bounds.left + x.toPx()).toInt(), (bounds.top + y.toPx()).toInt())
        }
        return (Color(pixel).let { (it.red + it.green + it.blue) / 3f } * 255).toInt()
    }

    private companion object {
        val COUNTING = DEFAULT.copy(countdown = 3)
        const val PREVIOUS = "Bước trước"
        const val NEXT = "Bước sau"
        const val HIDE_STEPS = "Ẩn thẻ bước"
    }
}
