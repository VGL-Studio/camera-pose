package com.vglstudio.camera.pose.line.guide.feature.camera

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.feature.camera.CameraScreenScreenshotTest.Companion.DEFAULT
import com.vglstudio.camera.pose.line.guide.feature.camera.CameraScreenScreenshotTest.Companion.LOCKED
import com.vglstudio.camera.pose.line.guide.feature.camera.CameraScreenScreenshotTest.Companion.samplePoses
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.PoseActions
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.PoseThumbItem
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.PoseUiState
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.SelectedPose
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The camera screen on an unfolded phone, 673 x 841 dp (spec camera, KB-11; plan T-020): the 3:4 frame on the left, the
 * capture controls in a column on the right, the chips and the strip under the frame. Numbers from
 * design/context/CameraScreen_default_dark_unfolded.md.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w673dp-h841dp-xxhdpi")
class CameraScreenUnfoldedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()

    private val actions = PoseActions(
        onCategorySelect = { calls += "category $it" },
        onPoseSelect = { calls += "pose $it" },
    )

    @Composable
    private fun Screen(
        poseState: PoseUiState = samplePoses(),
        lastPhoto: ImageBitmap? = null,
        onLastPhotoClick: (() -> Unit)? = { calls += "lastPhoto" },
    ) {
        AppTheme(darkTheme = true) {
            CameraScreen(
                uiState = DEFAULT,
                onFlashClick = {},
                onTimerClick = {},
                onGridClick = {},
                onFlipClick = { calls += "flip" },
                onZoomSelect = { calls += "zoom $it" },
                onShutterClick = { calls += "shutter" },
                onCancelCountdown = {},
                onRetry = {},
                onOpenAppSettings = {},
                onOpenPoses = { calls += "poses" },
                onOpenSettings = { calls += "settings" },
                poseState = poseState,
                poseActions = actions,
                lastPhoto = lastPhoto,
                onLastPhotoClick = onLastPhotoClick,
            )
        }
    }

    private fun bounds(tag: String) = composeRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()

    private fun assertNear(label: String, expected: Float, actual: Dp, tolerance: Float) {
        assertTrue("$label: expected $expected dp, got ${actual.value} dp", abs(actual.value - expected) <= tolerance)
    }

    @Test
    fun preview_isLeft_3by4_600tall() {
        composeRule.setContent { Screen() }
        val preview = bounds(CameraTestTags.PREVIEW)
        assertNear("preview left", 16f, preview.left, 4f)
        assertNear("preview top", 72f, preview.top, 4f)
        assertNear("preview width", 450f, preview.width, 4f)
        assertNear("preview height", 600f, preview.height, 4f)
    }

    @Test
    fun captureControls_areInRightColumn() {
        composeRule.setContent { Screen() }
        val preview = bounds(CameraTestTags.PREVIEW)
        val tags = listOf(CameraTestTags.FLIP, CameraTestTags.SHUTTER, CameraTestTags.LAST_PHOTO, CameraTestTags.ZOOM)
        val boxes = tags.map { bounds(it) }
        boxes.forEachIndexed { i, box ->
            assertTrue("${tags[i]} left ${box.left} right of the preview ${preview.right}", box.left > preview.right)
        }
        val centres = boxes.map { (it.left.value + it.right.value) / 2 }
        centres.forEach { assertTrue("centres $centres", abs(it - centres.first()) <= 2f) }
        // Top to bottom: flip, shutter, last photo, zoom.
        boxes.zipWithNext().forEachIndexed { i, (upper, lower) ->
            assertTrue("${tags[i]} above ${tags[i + 1]}", upper.bottom <= lower.top)
        }
        val shutter = boxes[1]
        val shutterMiddle = (shutter.top.value + shutter.bottom.value) / 2
        assertNear("shutter middle", (preview.top.value + preview.bottom.value) / 2, shutterMiddle.dp, 4f)
        // The zoom control is no longer in the frame.
        val zoom = boxes[3]
        assertTrue("zoom is outside the preview", zoom.left >= preview.right)
    }

    @Test
    fun topBarOverPreview_pickerUnderIt() {
        composeRule.setContent { Screen() }
        val preview = bounds(CameraTestTags.PREVIEW)
        val poses = bounds(CameraTestTags.POSES)
        val settings = bounds(CameraTestTags.SETTINGS)
        assertNear("library button left", 16f, poses.left, 4f)
        assertNear("settings button right", preview.right.value, settings.right, 4f)

        val root = bounds(CameraTestTags.SCREEN)
        val chips = bounds(CameraTestTags.CHIPS)
        val strip = bounds(CameraTestTags.STRIP)
        assertTrue("chips ${chips.top} under the preview ${preview.bottom}", chips.top >= preview.bottom - 1.dp)
        assertTrue("strip ${strip.top} under the chips ${chips.bottom}", strip.top >= chips.bottom - 1.dp)
        listOf(chips, strip).forEach {
            assertNear("picker left", 0f, it.left, 1f)
            assertNear("picker right", root.right.value, it.right, 1f)
        }
    }

    @Test
    fun unfoldedControls_callTheSameCallbacks() {
        composeRule.setContent { Screen() }
        // The controls tapped here are the ones of the right column, not of the phone layout.
        assertTrue(bounds(CameraTestTags.SHUTTER).left > bounds(CameraTestTags.PREVIEW).right)
        composeRule.onNodeWithTag(CameraTestTags.SHUTTER).performClick()
        composeRule.onNodeWithTag(CameraTestTags.FLIP).performClick()
        composeRule.onNodeWithTag(CameraTestTags.LAST_PHOTO).performClick()
        composeRule.onNodeWithText("2").performClick()
        composeRule.onNodeWithText("Nam").performClick()
        composeRule.onNodeWithTag(CameraTestTags.pose("p3")).performClick()
        assertEquals(listOf("shutter", "flip", "lastPhoto", "zoom 2.0", "category men", "pose p3"), calls)
    }

    @Test
    fun overlayToolsAndHints_stayInsideFrame() {
        composeRule.setContent { Screen(samplePoses(overlay = LOCKED)) }
        val preview = bounds(CameraTestTags.PREVIEW)
        val tools = bounds(CameraTestTags.OVERLAY_TOOLS)
        assertTrue("tools inside the preview", tools.left >= preview.left && tools.right <= preview.right)
        assertTrue("tools inside the preview", tools.top >= preview.top && tools.bottom <= preview.bottom)
        assertNear("tools right gap", 16f, preview.right - tools.right, 2f)
        val hint = bounds(CameraTestTags.LOCK_HINT)
        assertNear("hint bottom gap", 16f, preview.bottom - hint.bottom, 2f)
    }

    private fun manyPoses(): PoseUiState.Ready {
        val base = samplePoses()
        val image = base.strip.first().image
        val strip = (0 until 20).map { PoseThumbItem("p%02d".format(it), "Pose p%02d".format(it), image) }
        return base.copy(
            strip = strip,
            selected = SelectedPose("p15", listOf("Step 1"), 0, base.selected!!.outline),
        )
    }

    private fun exists(tag: String) = composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    @Test
    @Config(qualifiers = "w673dp-h891dp-xxhdpi")
    fun switchingLayout_keepsStripScrollAndPose() {
        var width by mutableStateOf(411.dp)
        composeRule.setContent {
            Box(Modifier.width(width).fillMaxHeight()) { Screen(manyPoses()) }
        }
        composeRule.onNodeWithTag(CameraTestTags.STRIP).performScrollToIndex(15)
        composeRule.onNodeWithTag(CameraTestTags.pose("p15")).assertIsSelected()
        assertTrue("phone layout: the shutter is under the strip", isPhoneLayout())

        width = 673.dp
        composeRule.waitForIdle()
        assertTrue("unfolded layout: the shutter is right of the frame", !isPhoneLayout())
        composeRule.onNodeWithTag(CameraTestTags.pose("p15")).assertIsSelected()

        width = 411.dp
        composeRule.waitForIdle()
        assertTrue("phone layout again", isPhoneLayout())
        composeRule.onNodeWithTag(CameraTestTags.pose("p15")).assertIsSelected()
    }

    private fun isPhoneLayout(): Boolean = bounds(CameraTestTags.SHUTTER).top >= bounds(CameraTestTags.STRIP).bottom

    @Test
    fun stateRestoration_keepsStripScroll() {
        val tester = StateRestorationTester(composeRule)
        tester.setContent { Screen(manyPoses()) }
        composeRule.onNodeWithTag(CameraTestTags.STRIP).performScrollToIndex(15)
        assertTrue(exists(CameraTestTags.pose("p15")))
        tester.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()
        assertTrue("cell 15 is still on screen", exists(CameraTestTags.pose("p15")))
    }
}
