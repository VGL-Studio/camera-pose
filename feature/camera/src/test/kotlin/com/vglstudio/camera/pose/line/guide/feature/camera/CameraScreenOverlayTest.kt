package com.vglstudio.camera.pose.line.guide.feature.camera

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas as ComposeCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.feature.camera.CameraUiState.Ready
import com.vglstudio.camera.pose.line.guide.feature.camera.data.OverlaySettings
import com.vglstudio.camera.pose.line.guide.feature.camera.model.FlashMode
import com.vglstudio.camera.pose.line.guide.feature.camera.model.Lens
import com.vglstudio.camera.pose.line.guide.feature.camera.model.TimerSetting
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.CategoryChip
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.OverlayState
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.PoseActions
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.PoseImage
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.PoseThumbItem
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.PoseUiState
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.SelectedPose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The outline adjustment on the camera screen, on the JVM (spec camera, KB-3; plan T-015): the drag and pinch gestures,
 * the tool column, the adjust panel and the lock hint. Text follows the text table of the design (README, section 2)
 * and the plan (question 8).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class CameraScreenOverlayTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()
    private val transforms = mutableListOf<Triple<Float, Float, Float>>()
    private val values = mutableListOf<Pair<String, Float>>()

    private val actions = PoseActions(
        onOverlayTransform = { x, y, zoom -> transforms += Triple(x, y, zoom) },
        onOverlayGestureEnd = { calls += "end" },
        onOpacityChange = { values += "opacity" to it },
        onOpacityChangeFinished = { calls += "opacityFinished" },
        onScaleChange = { values += "scale" to it },
        onScaleChangeFinished = { calls += "scaleFinished" },
        onTogglePanel = { calls += "togglePanel" },
        onClosePanel = { calls += "closePanel" },
        onToggleMirror = { calls += "mirror" },
        onToggleLock = { calls += "lock" },
        onToggleHidden = { calls += "hidden" },
        onResetOverlay = { calls += "reset" },
    )

    private var cameraState by mutableStateOf<CameraUiState>(READY)
    private var poseState by mutableStateOf<PoseUiState>(PoseUiState.Loading)

    private fun setScreen(
        overlay: OverlayState = OverlayState(),
        camera: CameraUiState = READY,
        selected: SelectedPose? = sampleSelected(),
    ) {
        cameraState = camera
        poseState = poseReady(overlay, selected)
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                CameraScreen(
                    uiState = cameraState,
                    onFlashClick = {},
                    onTimerClick = {},
                    onGridClick = {},
                    onFlipClick = {},
                    onZoomSelect = {},
                    onShutterClick = {},
                    onCancelCountdown = {},
                    onRetry = {},
                    onOpenAppSettings = {},
                    onOpenPoses = {},
                    onOpenSettings = {},
                    poseState = poseState,
                    poseActions = actions,
                )
            }
        }
    }

    private fun exists(tag: String) = composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    private fun textExists(text: String) =
        composeRule.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun overlayOf(change: (OverlayState) -> OverlayState) {
        val ready = poseState as PoseUiState.Ready
        poseState = ready.copy(overlay = change(ready.overlay))
        composeRule.waitForIdle()
    }

    private fun gestureArea() = composeRule.onNodeWithTag(CameraTestTags.OVERLAY_GESTURES)

    // Drag and pinch

    @Test
    fun dragOnPreview_reportsPartsOfFrame_andEndsOnce() {
        setScreen()
        gestureArea().performTouchInput { swipe(center, center + Offset(width * 0.2f, 0f)) }
        composeRule.waitForIdle()
        assertEquals(0.2f, transforms.sumOf { it.first.toDouble() }.toFloat(), 0.02f)
        assertEquals(0f, transforms.sumOf { it.second.toDouble() }.toFloat(), 0.02f)
        assertEquals(1f, transforms.fold(1f) { product, t -> product * t.third }, 0.02f)
        assertEquals(listOf("end"), calls)
    }

    @Test
    fun pinchOnPreview_reportsZoom() {
        setScreen()
        gestureArea().performTouchInput {
            pinch(
                start0 = center - Offset(50f, 0f),
                end0 = center - Offset(150f, 0f),
                start1 = center + Offset(50f, 0f),
                end1 = center + Offset(150f, 0f),
            )
        }
        composeRule.waitForIdle()
        val zoom = transforms.fold(1f) { product, t -> product * t.third }
        assertTrue("zoom $zoom", zoom > 2.5f)
        assertEquals(0f, transforms.sumOf { it.first.toDouble() }.toFloat(), 0.02f)
        assertEquals(listOf("end"), calls)
    }

    @Test
    fun swipeOnStepCard_doesNotMoveOutline() {
        setScreen()
        // Control: the same kind of swipe on the free part of the frame is reported.
        gestureArea().performTouchInput { swipe(center, center + Offset(width * 0.2f, 0f)) }
        composeRule.waitForIdle()
        assertTrue(transforms.isNotEmpty())
        transforms.clear()
        calls.clear()

        composeRule.onNodeWithTag(CameraTestTags.STEP_CARD).performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        assertTrue("transforms $transforms", transforms.isEmpty())
        assertTrue("calls $calls", calls.isEmpty())
    }

    @Test
    fun tapOnTool_doesNotMoveOutline() {
        setScreen()
        composeRule.onNodeWithTag(CameraTestTags.TOOL_ADJUST).performClick()
        composeRule.onNodeWithTag(CameraTestTags.TOOL_MIRROR).performClick()
        composeRule.waitForIdle()
        assertEquals(listOf("togglePanel", "mirror"), calls)
        assertTrue("transforms $transforms", transforms.isEmpty())
    }

    // The outline follows the state

    @Test
    fun outline_followsOffsetScaleMirrorOpacity() {
        setScreen()
        val density = composeRule.density.density
        val preview = composeRule.onNodeWithTag(CameraTestTags.PREVIEW).fetchSemanticsNode().boundsInRoot
        val base = composeRule.onNodeWithTag(CameraTestTags.OUTLINE).fetchSemanticsNode().boundsInRoot
        assertEquals(preview.center.x, base.center.x, 1f)

        overlayOf { it.copy(settings = it.settings.copy(scale = 0.5f), offsetX = 0.25f) }
        val moved = composeRule.onNodeWithTag(CameraTestTags.OUTLINE).fetchSemanticsNode().boundsInRoot
        assertEquals(0.25f * preview.width, moved.center.x - base.center.x, 2f * density)
        assertEquals(0f, moved.center.y - base.center.y, 2f * density)
        assertEquals(0.5f * base.width, moved.width, 2f * density)
        assertEquals(0.5f * base.height, moved.height, 2f * density)

        overlayOf { it.copy(offsetX = 0f, offsetY = -0.25f) }
        val up = composeRule.onNodeWithTag(CameraTestTags.OUTLINE).fetchSemanticsNode().boundsInRoot
        assertEquals(-0.25f * preview.height, up.center.y - base.center.y, 2f * density)
        assertEquals(0f, up.center.x - base.center.x, 2f * density)
    }

    @Test
    fun outline_mirrored_flipsTheImage() {
        setScreen(selected = sampleSelected(outline = HALF_IMAGE))
        assertTrue("left half is lit", lumaAt(CameraTestTags.OUTLINE, 0.25f) > 200)
        assertTrue("right half is dark", lumaAt(CameraTestTags.OUTLINE, 0.75f) < 100)

        overlayOf { it.copy(settings = it.settings.copy(mirrored = true)) }
        assertTrue("left half is dark", lumaAt(CameraTestTags.OUTLINE, 0.25f) < 100)
        assertTrue("right half is lit", lumaAt(CameraTestTags.OUTLINE, 0.75f) > 200)
    }

    @Test
    fun outline_opacity_fadesTheOutline() {
        setScreen(selected = sampleSelected(outline = HALF_IMAGE))
        val full = lumaAt(CameraTestTags.OUTLINE, 0.25f)
        overlayOf { it.copy(settings = it.settings.copy(opacity = 0.5f)) }
        val half = lumaAt(CameraTestTags.OUTLINE, 0.25f)
        assertTrue("full $full, half $half", full > 200 && half in 100..190)
    }

    @Test
    fun hidden_visibilityToolOff() {
        setScreen()
        assertTrue(exists(CameraTestTags.OUTLINE))
        composeRule.onNodeWithTag(CameraTestTags.TOOL_VISIBILITY)
            .assertIsSelected()
            .assertContentDescriptionEquals("Ẩn tư thế")

        overlayOf { it.copy(settings = it.settings.copy(hidden = true)) }
        composeRule.onNodeWithTag(CameraTestTags.TOOL_VISIBILITY)
            .assertIsNotSelected()
            .assertContentDescriptionEquals("Hiện tư thế")
        assertTrue(!exists(CameraTestTags.OUTLINE))
        // The step card stays.
        assertTrue(exists(CameraTestTags.STEP_CARD))
    }

    // Tool column

    @Test
    fun toolColumn_showsFourTools_withStates() {
        setScreen(OverlayState(settings = OverlaySettings(mirrored = true), panelOpen = true))
        assertTrue(exists(CameraTestTags.OVERLAY_TOOLS))
        composeRule.onNodeWithTag(CameraTestTags.TOOL_ADJUST).assertIsSelected()
            .assertContentDescriptionEquals("Chỉnh lớp phủ")
        composeRule.onNodeWithTag(CameraTestTags.TOOL_MIRROR).assertIsSelected()
            .assertContentDescriptionEquals("Lật nét viền")
        composeRule.onNodeWithTag(CameraTestTags.TOOL_LOCK).assertIsNotSelected()
            .assertContentDescriptionEquals("Khóa lớp phủ")
        composeRule.onNodeWithTag(CameraTestTags.TOOL_VISIBILITY).assertIsSelected()
    }

    @Test
    fun toolColumn_buttonsCallTheirActions() {
        // The panel is closed: when it is open it covers the lower two tools.
        setScreen()
        listOf(
            CameraTestTags.TOOL_ADJUST,
            CameraTestTags.TOOL_MIRROR,
            CameraTestTags.TOOL_LOCK,
            CameraTestTags.TOOL_VISIBILITY,
        ).forEach {
            composeRule.onNodeWithTag(it).assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp).performClick()
        }
        assertEquals(listOf("togglePanel", "mirror", "lock", "hidden"), calls)
    }

    @Test
    fun toolColumn_absentWithoutPose() {
        setScreen()
        // Control: with a pose the column, the gesture area and the panel are there.
        overlayOf { it.copy(panelOpen = true) }
        assertTrue(exists(CameraTestTags.OVERLAY_TOOLS))
        assertTrue(exists(CameraTestTags.OVERLAY_GESTURES))
        assertTrue(exists(CameraTestTags.ADJUST_PANEL))

        poseState = (poseState as PoseUiState.Ready).copy(selected = null)
        composeRule.waitForIdle()
        assertTrue(!exists(CameraTestTags.OVERLAY_TOOLS))
        assertTrue(!exists(CameraTestTags.OVERLAY_GESTURES))
        assertTrue(!exists(CameraTestTags.ADJUST_PANEL))
    }

    @Test
    fun toolColumn_absentWhenCameraNotReady() {
        setScreen()
        assertTrue(exists(CameraTestTags.OVERLAY_TOOLS))
        assertTrue(exists(CameraTestTags.OVERLAY_GESTURES))

        cameraState = CameraUiState.Starting
        composeRule.waitForIdle()
        assertTrue(!exists(CameraTestTags.OVERLAY_TOOLS))
        assertTrue(!exists(CameraTestTags.OVERLAY_GESTURES))
        // The pose picker still works in this state (KB-7).
        assertTrue(exists(CameraTestTags.STRIP))
    }

    // Lock

    @Test
    fun locked_dimsAdjustAndMirror_andShowsHint() {
        setScreen(OverlayState(settings = OverlaySettings(locked = true), lockHintVisible = true))
        composeRule.onNodeWithTag(CameraTestTags.TOOL_ADJUST).assertIsNotEnabled()
        composeRule.onNodeWithTag(CameraTestTags.TOOL_MIRROR).assertIsNotEnabled()
        composeRule.onNodeWithTag(CameraTestTags.TOOL_LOCK).assertIsEnabled().assertIsSelected()
            .assertContentDescriptionEquals("Mở khóa lớp phủ")
        composeRule.onNodeWithTag(CameraTestTags.TOOL_VISIBILITY).assertIsEnabled()
        composeRule.onNodeWithText("Đã khóa lớp phủ").assertExists()
        assertTrue(exists(CameraTestTags.LOCK_HINT))

        // The hint sits above the zoom control, in the frame.
        val hint = composeRule.onNodeWithTag(CameraTestTags.LOCK_HINT).getUnclippedBoundsInRoot()
        val zoom = composeRule.onNodeWithTag(CameraTestTags.ZOOM).getUnclippedBoundsInRoot()
        val preview = composeRule.onNodeWithTag(CameraTestTags.PREVIEW).getUnclippedBoundsInRoot()
        assertEquals(16f, (zoom.top - hint.bottom).value, 1f)
        assertEquals(preview.left.value + preview.width.value / 2, (hint.left + hint.width / 2).value, 1f)

        composeRule.onNodeWithTag(CameraTestTags.TOOL_ADJUST).performClick()
        assertTrue("calls $calls", calls.isEmpty())
        composeRule.onNodeWithTag(CameraTestTags.TOOL_LOCK).performClick()
        assertEquals(listOf("lock"), calls)
    }

    @Test
    fun unlocked_toolsEnabled_noHint() {
        setScreen(OverlayState(settings = OverlaySettings(locked = false), lockHintVisible = false))
        composeRule.onNodeWithTag(CameraTestTags.TOOL_ADJUST).assertIsEnabled()
        composeRule.onNodeWithTag(CameraTestTags.TOOL_MIRROR).assertIsEnabled()
        composeRule.onNodeWithTag(CameraTestTags.TOOL_LOCK).assertIsNotSelected()
            .assertContentDescriptionEquals("Khóa lớp phủ")
        assertTrue(!exists(CameraTestTags.LOCK_HINT))
        assertTrue(!textExists("Đã khóa lớp phủ"))
    }

    @Test
    fun lockHint_followsTheState() {
        setScreen(OverlayState(settings = OverlaySettings(locked = true), lockHintVisible = false))
        assertTrue(!exists(CameraTestTags.LOCK_HINT))
        overlayOf { it.copy(lockHintVisible = true) }
        assertTrue(exists(CameraTestTags.LOCK_HINT))
    }

    // Adjust panel

    @Test
    fun panel_showsSlidersAndActions_andHidesZoom() {
        setScreen(OverlayState(settings = OverlaySettings(opacity = 0.7f), panelOpen = true))
        assertTrue(exists(CameraTestTags.ADJUST_PANEL))
        listOf("Chỉnh lớp phủ", "Độ mờ", "70%", "Cỡ", "100%").forEach { assertTrue("$it", textExists(it)) }
        // The four actions: the label is drawn under each button, and the button is named after it.
        listOf("Lật", "Khóa", "Ẩn", "Đặt lại").forEach {
            assertTrue(it, textExists(it))
            composeRule.onNodeWithContentDescription(it).assertExists()
        }
        assertTrue(!exists(CameraTestTags.ZOOM))

        composeRule.onNodeWithContentDescription("Đặt lại").performClick()
        composeRule.onNodeWithContentDescription("Lật").performClick()
        composeRule.onNodeWithContentDescription("Khóa").performClick()
        composeRule.onNodeWithContentDescription("Ẩn").performClick()
        composeRule.onNodeWithContentDescription("Đóng bảng chỉnh").performClick()
        assertEquals(listOf("reset", "mirror", "lock", "hidden", "closePanel"), calls)
    }

    @Test
    fun panelClosed_showsZoom() {
        setScreen()
        assertTrue(!exists(CameraTestTags.ADJUST_PANEL))
        assertTrue(!textExists("Chỉnh lớp phủ"))
        assertTrue(exists(CameraTestTags.ZOOM))
        overlayOf { it.copy(panelOpen = true) }
        assertTrue(exists(CameraTestTags.ADJUST_PANEL))
        assertTrue(!exists(CameraTestTags.ZOOM))
    }

    @Test
    fun panel_showsTheValuesOfTheState() {
        setScreen(OverlayState(settings = OverlaySettings(opacity = 0.35f, scale = 1.25f), panelOpen = true))
        assertTrue(textExists("35%"))
        assertTrue(textExists("125%"))
        assertTrue(!textExists("100%"))
    }

    @Test
    fun opacitySlider_setProgress_callsChangeThenFinished() {
        setScreen(OverlayState(panelOpen = true))
        composeRule.onNodeWithTag(CameraTestTags.OPACITY_SLIDER)
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.4f) }
        composeRule.waitForIdle()
        assertEquals(listOf("opacity" to 0.4f), values)
        assertEquals(listOf("opacityFinished"), calls)
    }

    @Test
    fun sizeSlider_setProgress_callsChangeThenFinished() {
        setScreen(OverlayState(panelOpen = true))
        composeRule.onNodeWithTag(CameraTestTags.SIZE_SLIDER)
            .performSemanticsAction(SemanticsActions.SetProgress) { it(1.2f) }
        composeRule.waitForIdle()
        assertEquals(listOf("scale" to 1.2f), values)
        assertEquals(listOf("scaleFinished"), calls)
    }

    // Short screen (L-028)

    @Test
    @Config(qualifiers = SHORT_SCREEN)
    fun small_toolColumnInRightMargin_clearOfFrameAndStrip() {
        setScreen()
        val root = composeRule.onRoot().getUnclippedBoundsInRoot()
        val preview = composeRule.onNodeWithTag(CameraTestTags.PREVIEW).getUnclippedBoundsInRoot()
        val tools = composeRule.onNodeWithTag(CameraTestTags.OVERLAY_TOOLS).getUnclippedBoundsInRoot()
        val chips = composeRule.onNodeWithTag(CameraTestTags.CHIPS).getUnclippedBoundsInRoot()
        assertTrue("tools $tools, preview $preview", tools.left >= preview.right)
        assertTrue("tools $tools, root $root", tools.right <= root.right)
        assertTrue("tools $tools, chips $chips", tools.bottom.value <= chips.top.value + 0.5f)
        listOf(
            CameraTestTags.TOOL_ADJUST,
            CameraTestTags.TOOL_MIRROR,
            CameraTestTags.TOOL_LOCK,
            CameraTestTags.TOOL_VISIBILITY,
        ).forEach { composeRule.onNodeWithTag(it).assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp) }
    }

    @Test
    @Config(qualifiers = SHORT_SCREEN)
    fun small_panelInsideFrame_andScrolls() {
        setScreen(OverlayState(panelOpen = true))
        val root = composeRule.onRoot().getUnclippedBoundsInRoot()
        val preview = composeRule.onNodeWithTag(CameraTestTags.PREVIEW).getUnclippedBoundsInRoot()
        val panel = composeRule.onNodeWithTag(CameraTestTags.ADJUST_PANEL).getUnclippedBoundsInRoot()
        assertTrue("panel $panel, preview $preview", panel.left >= preview.left && panel.right <= preview.right)
        assertTrue("panel $panel, preview $preview", panel.top >= preview.top && panel.bottom <= preview.bottom)
        val shutter = composeRule.onNodeWithTag(CameraTestTags.SHUTTER).getUnclippedBoundsInRoot()
        assertTrue("shutter $shutter, root $root", shutter.bottom <= root.bottom)

        composeRule.onNodeWithContentDescription("Đặt lại").performScrollTo().performClick()
        assertEquals(listOf("reset"), calls)
    }

    private fun lumaAt(tag: String, fraction: Float): Int {
        composeRule.waitForIdle()
        val root = (composeRule as AndroidComposeTestRule<*, *>).activity.window.decorView
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        val bounds = composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInWindow
        val pixel = bitmap.getPixel(
            (bounds.left + bounds.width * fraction).toInt(),
            bounds.center.y.toInt(),
        )
        return (Color(pixel).let { (it.red + it.green + it.blue) / 3f } * 255).toInt()
    }

    private companion object {
        const val SHORT_SCREEN = "w360dp-h640dp-xxhdpi"
        val STRIP_IDS = listOf("w01", "m01", "w02", "m02", "w03")
        val IMAGE = PoseImage.Ready(ImageBitmap(1, 1))

        /** White on the left half, transparent on the right, 1:2 like an outline. */
        val HALF_IMAGE = PoseImage.Ready(
            ImageBitmap(100, 200).also { image ->
                ComposeCanvas(image).drawRect(0f, 0f, 50f, 200f, Paint().apply { color = Color.White })
            },
        )

        val READY = Ready(
            flash = FlashMode.OFF,
            flashAvailable = true,
            timer = TimerSetting.OFF,
            gridVisible = false,
            lens = Lens.BACK,
            zoomLevels = listOf(1f, 2f),
            zoom = 1f,
        )

        fun sampleSelected(outline: PoseImage = IMAGE) = SelectedPose(
            id = "w02",
            steps = listOf("Step 1", "Step 2", "Step 3", "Step 4"),
            stepIndex = 1,
            outline = outline,
        )

        fun poseReady(overlay: OverlayState, selected: SelectedPose?) = PoseUiState.Ready(
            categories = listOf(CategoryChip("women", "Nữ"), CategoryChip("men", "Nam")),
            selectedCategoryId = null,
            strip = STRIP_IDS.map { PoseThumbItem(id = it, label = "Pose $it", image = IMAGE) },
            selected = selected,
            stepsHidden = false,
            overlay = overlay,
        )
    }
}
