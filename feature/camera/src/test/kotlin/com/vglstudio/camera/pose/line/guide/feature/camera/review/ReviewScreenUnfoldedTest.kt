package com.vglstudio.camera.pose.line.guide.feature.camera.review

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.feature.camera.review.ReviewUiState.Ready
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The review screen on an unfolded phone, 673 x 841 dp (spec camera, KB-11; plan T-020): the photo on the left, the
 * three actions stacked and the main button "Lưu ảnh" in the column on the right. Numbers from
 * design/context/ReviewScreen_default_dark_unfolded.md.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w673dp-h841dp-xxhdpi")
class ReviewScreenUnfoldedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()

    private fun setScreen(uiState: ReviewUiState) {
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                ReviewScreen(
                    uiState = uiState,
                    image = null,
                    onCloseClick = { calls += "close" },
                    onSaveClick = { calls += "save" },
                    onRetrySave = { calls += "retry" },
                    onOpenAppSettings = { calls += "settings" },
                    onRetakeClick = { calls += "retake" },
                    onShareClick = { calls += "share" },
                    onNextPoseClick = { calls += "nextPose" },
                    onDiscardConfirm = { calls += "discard" },
                    onDiscardSave = { calls += "discardSave" },
                    onDismissDialog = { calls += "dismiss" },
                )
            }
        }
    }

    private fun bounds(tag: String) = composeRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()

    private fun assertNear(label: String, expected: Float, actual: Dp, tolerance: Float) {
        assertTrue("$label: expected $expected dp, got ${actual.value} dp", abs(actual.value - expected) <= tolerance)
    }

    @Test
    fun photoLeft_actionsAndSaveRight() {
        setScreen(Ready(PATH, SaveStatus.NOT_SAVED))
        val photo = bounds(ReviewTestTags.PHOTO)
        assertNear("photo left", 16f, photo.left, 4f)
        assertNear("photo width", 450f, photo.width, 4f)
        assertNear("photo height", 600f, photo.height, 4f)

        val actions = listOf(ReviewTestTags.RETAKE, ReviewTestTags.SHARE, ReviewTestTags.NEXT_POSE).map { bounds(it) }
        actions.forEach { assertTrue("action ${it.left} right of the photo ${photo.right}", it.left >= photo.right) }
        actions.zipWithNext().forEach { (upper, lower) ->
            assertNear("gap between actions", 24f, lower.top - upper.bottom, 2f)
        }

        val save = bounds(ReviewTestTags.SAVE)
        val root = bounds(ReviewTestTags.SCREEN)
        assertTrue("save ${save.left} right of the photo ${photo.right}", save.left >= photo.right)
        assertNear("save bottom gap", 20f, root.bottom - save.bottom, 2f)
        composeRule.onNodeWithText("Lưu ảnh").assertExists()
        assertTrue(composeRule.onAllNodesWithText("Lưu vào thư viện").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun saving_showsSavingText() {
        // The spinner never ends: keep the frame clock still (L-029).
        composeRule.mainClock.autoAdvance = false
        setScreen(Ready(PATH, SaveStatus.SAVING))
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithText("Đang lưu…").assertExists()
        assertTrue(bounds(ReviewTestTags.SAVE).left >= bounds(ReviewTestTags.PHOTO).right)
        assertTrue(composeRule.onAllNodesWithText("Lưu ảnh").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun unfoldedReview_callsTheSameCallbacks() {
        setScreen(Ready(PATH, SaveStatus.NOT_SAVED))
        composeRule.onNodeWithText("Chụp lại").performClick()
        composeRule.onNodeWithText("Chia sẻ").performClick()
        composeRule.onNodeWithText("Tư thế tiếp").performClick()
        composeRule.onNodeWithText("Lưu ảnh").performClick()
        composeRule.onNodeWithContentDescription("Đóng").performClick()
        assertEquals(listOf("retake", "share", "nextPose", "save", "close"), calls)
    }

    @Test
    fun saveError_isInRightColumn_aboveSave_andRetries() {
        setScreen(Ready(PATH, SaveStatus.FAILED_STORAGE_FULL))
        val photo = bounds(ReviewTestTags.PHOTO)
        val error = bounds(ReviewTestTags.SAVE_ERROR)
        val save = bounds(ReviewTestTags.SAVE)
        assertTrue("error ${error.left} right of the photo ${photo.right}", error.left >= photo.right)
        assertTrue("error ${error.bottom} above save ${save.top}", error.bottom <= save.top)
        composeRule.onNode(inSaveError("Không lưu được ảnh: bộ nhớ máy đã đầy.")).assertExists()
        composeRule.onNode(inSaveError("Thử lại")).performClick()
        assertEquals(listOf("retry"), calls)
    }

    @Test
    fun discardDialog_isCentered_andHidesContent() {
        setScreen(Ready(PATH, SaveStatus.NOT_SAVED, pendingExit = ExitAction.RETAKE))
        val root = bounds(ReviewTestTags.SCREEN)
        val dialog = bounds(ReviewTestTags.DISCARD_DIALOG)
        assertNear("dialog centre x", (root.left.value + root.right.value) / 2, (dialog.left + dialog.right) / 2, 4f)
        assertNear("dialog centre y", (root.top.value + root.bottom.value) / 2, (dialog.top + dialog.bottom) / 2, 4f)
        assertTrue(composeRule.onAllNodesWithText("Chụp lại").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("Chia sẻ").fetchSemanticsNodes().isEmpty())
        composeRule.onNode(inDialog("Bỏ ảnh này?")).assertExists()
        composeRule.onNode(inDialog("Lưu ảnh")).performClick()
        assertEquals(listOf("discardSave"), calls)
    }

    private fun inSaveError(text: String) = hasText(text) and hasAnyAncestor(hasTestTag(ReviewTestTags.SAVE_ERROR))

    private fun inDialog(text: String) = hasText(text) and hasAnyAncestor(hasTestTag(ReviewTestTags.DISCARD_DIALOG))

    private companion object {
        const val PATH = "/cache/captures/capture_1.jpg"
    }
}

/**
 * Screenshot tests (Roborazzi) of the review screen on an unfolded phone. default_dark_unfolded is named like a design
 * image in docs/features/camera/design/screens/ and compared by scripts/design-compare.sh (ADR-0020);
 * save-error_unfolded has no design image: the most content in the right column. In a class of its own, without a
 * compose rule: the rule opens one more window and Roborazzi would then capture the whole screen.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w673dp-h841dp-xxhdpi")
class ReviewScreenUnfoldedScreenshotTest {
    @Test
    fun default_dark_unfolded() {
        captureRoboImage("src/test/screenshots/ReviewScreen_default_dark_unfolded.png") {
            Screen(Ready(PATH, SaveStatus.NOT_SAVED))
        }
    }

    @Test
    fun saveError_unfolded() {
        captureRoboImage("src/test/screenshots/ReviewScreen_save-error_unfolded.png") {
            Screen(Ready(PATH, SaveStatus.FAILED_STORAGE_FULL))
        }
    }

    private companion object {
        const val PATH = "/cache/captures/capture_1.jpg"
    }
}

@Composable
private fun Screen(uiState: ReviewUiState) {
    AppTheme(darkTheme = true) {
        ReviewScreen(
            uiState = uiState,
            image = null,
            onCloseClick = {},
            onSaveClick = {},
            onRetrySave = {},
            onOpenAppSettings = {},
            onRetakeClick = {},
            onShareClick = {},
            onNextPoseClick = {},
            onDiscardConfirm = {},
            onDiscardSave = {},
            onDismissDialog = {},
        )
    }
}
