package com.vglstudio.camera.pose.line.guide.feature.camera

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Canvas as ComposeCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.feature.camera.CameraUiState.Ready
import com.vglstudio.camera.pose.line.guide.feature.camera.match.MatchHintText
import com.vglstudio.camera.pose.line.guide.feature.camera.match.MatchUiState
import com.vglstudio.camera.pose.line.guide.feature.camera.model.FlashMode
import com.vglstudio.camera.pose.line.guide.feature.camera.model.Lens
import com.vglstudio.camera.pose.line.guide.feature.camera.model.TimerSetting
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.CategoryChip
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.OverlayState
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.PoseImage
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
 * The pose matching on the camera screen, on the JVM (spec camera, KB-5; plan T-017, section 4): the hint with the
 * score, the outline and the shutter ring in the colour of a match, and where the hint sits. Text follows the text
 * table of the design (README, section 2) and the plan (question 4). Native graphics, so the pixels are real.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class CameraScreenMatchTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var matchState by mutableStateOf<MatchUiState>(MatchUiState.Off)
    private var overlayState by mutableStateOf(OverlayState())

    private fun setScreen(match: MatchUiState, overlay: OverlayState = OverlayState(), longStep: Boolean = false) {
        matchState = match
        overlayState = overlay
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                CameraScreen(
                    uiState = READY,
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
                    poseState = poseReady(overlayState, longStep),
                    matchState = matchState,
                )
            }
        }
    }

    private fun exists(tag: String) = composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    private fun textExists(text: String) =
        composeRule.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    /** The texts of the nodes under the hint, whole tree (not merged). */
    private fun hintTexts(): List<String> {
        val root = composeRule.onNodeWithTag(CameraTestTags.MATCH_HINT, useUnmergedTree = true).fetchSemanticsNode()
        fun collect(node: SemanticsNode): List<String> =
            (node.config.getOrNull(SemanticsProperties.Text)?.map { it.text } ?: emptyList()) +
                node.children.flatMap { collect(it) }
        return collect(root)
    }

    private fun pixelAt(bitmap: Bitmap, x: Float, y: Float): Int = bitmap.getPixel(x.toInt(), y.toInt())

    private fun screenBitmap(): Bitmap {
        composeRule.waitForIdle()
        val root = (composeRule as AndroidComposeTestRule<*, *>).activity.window.decorView
        return Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888).also {
            root.draw(Canvas(it))
        }
    }

    private fun assertColor(expected: Long, actual: Int, what: String) {
        val color = Color(actual)
        val want = Color(expected)
        fun channel(a: Float, b: Float) = abs(a - b) * 255f <= TOLERANCE
        assertTrue(
            "$what: expected #${expected.toString(16)}, got #${actual.toUInt().toString(16)}",
            channel(color.red, want.red) && channel(color.green, want.green) && channel(color.blue, want.blue),
        )
    }

    /** The colour in the middle of the outline (a solid image, so the colour is the one the outline is tinted). */
    private fun outlineColor(): Int {
        val bitmap = screenBitmap()
        val bounds = composeRule.onNodeWithTag(CameraTestTags.OUTLINE).fetchSemanticsNode().boundsInWindow
        return pixelAt(bitmap, bounds.center.x, bounds.center.y)
    }

    /** The colour of the shutter ring, 2 dp inside the left edge of the button. */
    private fun ringColor(): Int {
        val bitmap = screenBitmap()
        val bounds = composeRule.onNodeWithTag(CameraTestTags.SHUTTER).fetchSemanticsNode().boundsInWindow
        val inset = with(composeRule.density) { 2.dp.toPx() }
        return pixelAt(bitmap, bounds.left + inset, bounds.center.y)
    }

    // The states

    @Test
    fun scoring_showsScoreAndHint() {
        setScreen(MatchUiState.Scoring(68, MatchHintText.RAISE_LEFT_ARM))
        assertTrue(textExists("68"))
        assertTrue(textExists("Nâng tay trái cao hơn"))
        composeRule.onNodeWithTag(CameraTestTags.MATCH_HINT)
            .assertContentDescriptionEquals("68 điểm, Nâng tay trái cao hơn")
        // The outline and the shutter keep their colour.
        assertColor(WHITE, outlineColor(), "outline")
        assertColor(WHITE, ringColor(), "shutter ring")
    }

    @Test
    fun scoring_showsTheTextOfEveryHint() {
        val expected = mapOf(
            MatchHintText.RAISE_LEFT_ARM to "Nâng tay trái cao hơn",
            MatchHintText.LOWER_LEFT_ARM to "Hạ tay trái thấp hơn",
            MatchHintText.RAISE_RIGHT_ARM to "Nâng tay phải cao hơn",
            MatchHintText.LOWER_RIGHT_ARM to "Hạ tay phải thấp hơn",
            MatchHintText.BEND_LEFT_ELBOW to "Gập khuỷu tay trái thêm",
            MatchHintText.STRAIGHTEN_LEFT_ARM to "Duỗi thẳng tay trái",
            MatchHintText.BEND_RIGHT_ELBOW to "Gập khuỷu tay phải thêm",
            MatchHintText.STRAIGHTEN_RIGHT_ARM to "Duỗi thẳng tay phải",
            MatchHintText.WIDEN_LEFT_LEG to "Đưa chân trái ra xa hơn",
            MatchHintText.CLOSE_LEFT_LEG to "Khép chân trái vào",
            MatchHintText.WIDEN_RIGHT_LEG to "Đưa chân phải ra xa hơn",
            MatchHintText.CLOSE_RIGHT_LEG to "Khép chân phải vào",
            MatchHintText.BEND_LEFT_KNEE to "Gập gối trái thêm",
            MatchHintText.STRAIGHTEN_LEFT_LEG to "Duỗi thẳng chân trái",
            MatchHintText.BEND_RIGHT_KNEE to "Gập gối phải thêm",
            MatchHintText.STRAIGHTEN_RIGHT_LEG to "Duỗi thẳng chân phải",
        )
        assertEquals(MatchHintText.entries.toSet(), expected.keys)
        setScreen(MatchUiState.Scoring(50, MatchHintText.RAISE_LEFT_ARM))
        expected.forEach { (hint, text) ->
            matchState = MatchUiState.Scoring(50, hint)
            composeRule.waitForIdle()
            assertTrue("$hint: $text", textExists(text))
        }
    }

    @Test
    fun matched_showsMessage_greenOutline_andShutterRing() {
        setScreen(MatchUiState.Matched(92))
        assertTrue(textExists("92"))
        assertTrue(textExists("Đúng dáng rồi, bấm chụp!"))
        assertColor(SUCCESS, outlineColor(), "outline")
        assertColor(SUCCESS, ringColor(), "shutter ring")

        // The control: back to scoring, both turn white again.
        matchState = MatchUiState.Scoring(70, MatchHintText.RAISE_LEFT_ARM)
        composeRule.waitForIdle()
        assertColor(WHITE, outlineColor(), "outline")
        assertColor(WHITE, ringColor(), "shutter ring")
    }

    @Test
    fun noPerson_showsHintWithoutScore() {
        setScreen(MatchUiState.NoPerson)
        assertTrue(textExists("Lùi lại để cả người vào khung"))
        assertTrue(exists(CameraTestTags.MATCH_HINT))
        val texts = hintTexts()
        assertTrue("texts $texts", "Lùi lại để cả người vào khung" in texts)
        assertTrue("texts $texts", texts.none { text -> text.any { it.isDigit() } })
        assertColor(WHITE, outlineColor(), "outline")
    }

    @Test
    fun off_noMatchHint() {
        setScreen(MatchUiState.Off)
        assertTrue(!exists(CameraTestTags.MATCH_HINT))
        // The control: the same screen with a state has it.
        matchState = MatchUiState.NoPerson
        composeRule.waitForIdle()
        assertTrue(exists(CameraTestTags.MATCH_HINT))
    }

    // Where the hint is

    @Test
    fun matchHint_isAboveZoom() {
        setScreen(MatchUiState.Scoring(68, MatchHintText.RAISE_LEFT_ARM))
        val hint = composeRule.onNodeWithTag(CameraTestTags.MATCH_HINT).getUnclippedBoundsInRoot()
        val zoom = composeRule.onNodeWithTag(CameraTestTags.ZOOM).getUnclippedBoundsInRoot()
        val preview = composeRule.onNodeWithTag(CameraTestTags.PREVIEW).getUnclippedBoundsInRoot()
        val gap = zoom.top.value - hint.bottom.value
        assertTrue("gap $gap, hint $hint, zoom $zoom", abs(gap - 16f) <= 2f)
        val hintCentre = (hint.left.value + hint.right.value) / 2
        val previewCentre = (preview.left.value + preview.right.value) / 2
        assertTrue("hint $hint, preview $preview", abs(hintCentre - previewCentre) <= 1f)
        assertEquals(56f, hint.height.value, 1f)
    }

    @Test
    fun lockHintVisible_hidesMatchHint() {
        setScreen(MatchUiState.Scoring(68, MatchHintText.RAISE_LEFT_ARM), OverlayState(lockHintVisible = true))
        assertTrue(exists(CameraTestTags.LOCK_HINT))
        assertTrue(!exists(CameraTestTags.MATCH_HINT))
        overlayState = OverlayState(lockHintVisible = false)
        composeRule.waitForIdle()
        assertTrue(!exists(CameraTestTags.LOCK_HINT))
        assertTrue(exists(CameraTestTags.MATCH_HINT))
    }

    @Test
    fun panelOpen_hidesMatchHint() {
        setScreen(MatchUiState.Scoring(68, MatchHintText.RAISE_LEFT_ARM), OverlayState(panelOpen = true))
        assertTrue(exists(CameraTestTags.ADJUST_PANEL))
        assertTrue(!exists(CameraTestTags.MATCH_HINT))
        overlayState = OverlayState(panelOpen = false)
        composeRule.waitForIdle()
        assertTrue(exists(CameraTestTags.MATCH_HINT))
    }

    // Short screen (L-028)

    @Test
    @Config(qualifiers = SHORT_SCREEN)
    fun small_matchHintInsidePreview_clearOfStepCardAndZoom() {
        setScreen(MatchUiState.Scoring(60, MatchHintText.WIDEN_RIGHT_LEG), longStep = true)
        val preview = composeRule.onNodeWithTag(CameraTestTags.PREVIEW).getUnclippedBoundsInRoot()
        val hint = composeRule.onNodeWithTag(CameraTestTags.MATCH_HINT).getUnclippedBoundsInRoot()
        val step = composeRule.onNodeWithTag(CameraTestTags.STEP_CARD).getUnclippedBoundsInRoot()
        val zoom = composeRule.onNodeWithTag(CameraTestTags.ZOOM).getUnclippedBoundsInRoot()
        assertTrue("hint $hint, preview $preview", hint.left >= preview.left && hint.right <= preview.right)
        assertTrue("hint $hint, preview $preview", hint.top >= preview.top && hint.bottom <= preview.bottom)
        assertTrue("hint $hint, step $step", hint.top >= step.bottom)
        assertTrue("hint $hint, zoom $zoom", hint.bottom <= zoom.top)
        // The text wraps inside the pill and is not cut.
        val text = composeRule.onAllNodesWithText("Đưa chân phải ra xa hơn", useUnmergedTree = true)
            .fetchSemanticsNodes().single().boundsInWindow
        val pill = composeRule.onNodeWithTag(CameraTestTags.MATCH_HINT).fetchSemanticsNode().boundsInWindow
        assertTrue("text $text, pill $pill", text.left >= pill.left && text.right <= pill.right)
        assertTrue("text $text, pill $pill", text.top >= pill.top && text.bottom <= pill.bottom)
    }

    private companion object {
        const val SHORT_SCREEN = "w360dp-h640dp-xxhdpi"
        const val WHITE = 0xFFFFFFFFL
        const val SUCCESS = 0xFF3DDC97L

        /** Per channel, out of 255. */
        const val TOLERANCE = 4f
        val STRIP_IDS = listOf("w01", "m01", "w02")

        /** A solid white outline, so the middle of the outline has the colour it is tinted. */
        val SOLID = PoseImage.Ready(
            ImageBitmap(100, 200).also { image ->
                ComposeCanvas(image).drawRect(0f, 0f, 100f, 200f, Paint().apply { color = Color.White })
            },
        )

        val READY = Ready(
            flash = FlashMode.OFF,
            flashAvailable = true,
            timer = TimerSetting.OFF,
            gridVisible = false,
            lens = Lens.BACK,
            zoomLevels = listOf(0.5f, 1f, 2f),
            zoom = 1f,
        )

        fun poseReady(overlay: OverlayState, longStep: Boolean) = PoseUiState.Ready(
            categories = listOf(CategoryChip("women", "Nữ"), CategoryChip("men", "Nam")),
            selectedCategoryId = null,
            strip = STRIP_IDS.map { PoseThumbItem(id = it, label = "Pose $it", image = SOLID) },
            selected = SelectedPose(
                id = "w02",
                steps = listOf("Step 1", if (longStep) "Xoay vai 45° về phía máy, cằm hơi hạ." else "Step 2"),
                stepIndex = 1,
                outline = SOLID,
            ),
            stepsHidden = false,
            overlay = overlay,
        )
    }
}
