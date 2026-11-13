package com.vglstudio.camera.pose.line.guide.feature.camera.review

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
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
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.feature.camera.review.ReviewUiState.Ready
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
@Config(qualifiers = "vi-w673dp-h841dp-xxhdpi")
class ReviewScreenUnfoldedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()
    private var screenState by mutableStateOf<ReviewUiState>(ReviewUiState.Loading)

    /** The screen in [uiState]; with [adBar], in that bar instead of the default one of the screen (plan T-094). */
    private fun setScreen(uiState: ReviewUiState, adBar: ReviewAdBar? = null) {
        screenState = uiState
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                if (adBar == null) {
                    ReviewScreen(
                        uiState = screenState,
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
                } else {
                    ReviewScreen(
                        uiState = screenState,
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
                        adBar = adBar,
                    )
                }
            }
        }
    }

    private fun update(change: () -> Unit) {
        composeRule.runOnIdle(change)
        composeRule.waitForIdle()
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
        // In the narrow column the message wraps; the action goes under it, not over it.
        val text = composeRule.onNode(inSaveError("Không lưu được ảnh: bộ nhớ máy đã đầy.")).getUnclippedBoundsInRoot()
        val retry = composeRule.onNode(inSaveError("Thử lại")).getUnclippedBoundsInRoot()
        assertTrue("retry ${retry.top} under the message ${text.bottom}", retry.top >= text.bottom)
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

    // A large screen in landscape follows the device (spec camera, KB-11; plan T-056): every action and "Lưu ảnh" stay
    // inside the window and the photo keeps 3:4. The last two frames are the first two minus the system bars.
    private fun assertSaveInsideWindow() {
        setScreen(Ready(PATH, SaveStatus.NOT_SAVED))
        val root = bounds(ReviewTestTags.SCREEN)
        val tags = listOf(
            ReviewTestTags.PHOTO,
            ReviewTestTags.RETAKE,
            ReviewTestTags.SHARE,
            ReviewTestTags.NEXT_POSE,
            ReviewTestTags.SAVE,
            ReviewTestTags.CLOSE,
        )
        tags.forEach { tag ->
            val box = bounds(tag)
            assertTrue("$tag bottom ${box.bottom} inside the window ${root.bottom}", box.bottom <= root.bottom)
            assertTrue("$tag right ${box.right} inside the window ${root.right}", box.right <= root.right)
        }
        val photo = bounds(ReviewTestTags.PHOTO)
        val ratio = photo.height.value / photo.width.value
        assertTrue("photo height / width $ratio is 3:4", abs(ratio - 4f / 3f) <= 0.02f)
    }

    @Test
    @Config(qualifiers = "vi-w841dp-h673dp-land-xxhdpi")
    fun landscape_saveInsideWindow_w841h673() = assertSaveInsideWindow()

    @Test
    @Config(qualifiers = "vi-w960dp-h600dp-land-xxhdpi")
    fun landscape_saveInsideWindow_w960h600() = assertSaveInsideWindow()

    @Test
    @Config(qualifiers = "vi-w841dp-h593dp-land-xxhdpi")
    fun landscape_saveInsideWindow_w841h593() = assertSaveInsideWindow()

    @Test
    @Config(qualifiers = "vi-w960dp-h520dp-land-xxhdpi")
    fun landscape_saveInsideWindow_w960h520() = assertSaveInsideWindow()

    // The bottom ad bar (plan T-094; spec camera, KB-11 criterion 3, KB-9 criterion 5): a fake 80 dp slot in the bar of
    // the framework (fakeAdBar). Numbers from section 4.1 of the plan, measured from the top of the test frame.

    private val actionTags = listOf(ReviewTestTags.RETAKE, ReviewTestTags.SHARE, ReviewTestTags.NEXT_POSE)

    private fun assertAbove(label: String, box: DpRect, slot: DpRect) =
        assertTrue("$label bottom ${box.bottom} above the slot ${slot.top}", box.bottom.value <= slot.top.value + 0.5f)

    private fun assertActionsAndSaveAbove(slot: DpRect) {
        (actionTags + ReviewTestTags.SAVE).forEach { assertAbove(it, bounds(it), slot) }
        assertNear("save height", 56f, bounds(ReviewTestTags.SAVE).height, 0.5f)
    }

    @Test
    fun adBar_slotFullWidth_photoKept_saveAbove() {
        var withSlot by mutableStateOf(false)
        setScreen(Ready(PATH, SaveStatus.NOT_SAVED), adBar = fakeAdBar(withSlot = { withSlot }))
        val photoBefore = bounds(ReviewTestTags.PHOTO)
        val saveBefore = bounds(ReviewTestTags.SAVE)

        update { withSlot = true }

        val slot = bounds(FAKE_AD_SLOT)
        assertNear("slot width", 641f, slot.width, 0.5f)
        assertNear("slot left", 16f, slot.left, 0.5f)
        assertNear("slot top", 745f, slot.top, 0.5f)
        val photo = bounds(ReviewTestTags.PHOTO)
        assertNear("photo width", 450f, photo.width, 1f)
        assertNear("photo height", 600f, photo.height, 1f)
        assertNear("photo height kept", photoBefore.height.value, photo.height, 0.5f)
        assertActionsAndSaveAbove(slot)
        val save = bounds(ReviewTestTags.SAVE)
        assertNear("save bottom", 725f, save.bottom, 0.5f)
        assertNear("save moved up", 96f, saveBefore.bottom - save.bottom, 0.5f)
        assertTrue("save ${save.left} right of the photo ${photo.right}", save.left >= photo.right)
    }

    @Test
    @Config(qualifiers = "vi-w960dp-h520dp-land-xxhdpi")
    fun landscape_adBar_saveInsideWindow() {
        setScreen(Ready(PATH, SaveStatus.NOT_SAVED), adBar = fakeAdBar())
        val slot = bounds(FAKE_AD_SLOT)
        assertNear("slot top", 424f, slot.top, 0.5f)
        val save = bounds(ReviewTestTags.SAVE)
        assertNear("save top", 348f, save.top, 0.5f)
        assertNear("save bottom", 404f, save.bottom, 0.5f)
        val photo = bounds(ReviewTestTags.PHOTO)
        assertNear("photo width", 255f, photo.width, 1f)
        assertNear("photo height", 340f, photo.height, 1f)
        assertEquals("photo width / height", 0.75f, photo.width.value / photo.height.value, 0.01f)
        assertActionsAndSaveAbove(slot)
    }

    @Test
    @Config(qualifiers = "vi-w960dp-h520dp-land-xxhdpi")
    fun landscape_adBar_saveError_compactColumn() {
        setScreen(Ready(PATH, SaveStatus.PERMISSION_BLOCKED), adBar = fakeAdBar())
        assertCompactColumn(
            "PERMISSION_BLOCKED",
            "Cho phép lưu ảnh trong Cài đặt máy để lưu vào thư viện.",
            "Mở cài đặt",
            "Lưu ảnh",
            137f,
        )
        assertEquals(listOf("save"), calls)

        calls.clear()
        update { screenState = Ready(PATH, SaveStatus.FAILED_STORAGE_FULL) }
        assertCompactColumn("FAILED_STORAGE_FULL", "Không lưu được ảnh: bộ nhớ máy đã đầy.", "Thử lại", "Lưu ảnh", 137f)
        assertEquals(listOf("save"), calls)
    }

    @Test
    @Config(qualifiers = "pt-rBR-w960dp-h520dp-land-xxhdpi")
    fun landscape_adBar_saveError_compactColumn_portuguese() {
        setScreen(Ready(PATH, SaveStatus.PERMISSION_BLOCKED), adBar = fakeAdBar())
        assertCompactColumn(
            "pt-BR PERMISSION_BLOCKED",
            "Permita salvar fotos nas Configurações do aparelho para salvar na galeria.",
            "Abrir configurações",
            "Salvar foto",
            160.7f,
        )
        assertEquals(listOf("save"), calls)
    }

    /**
     * Too little room in the right column for the stacked actions, the error and the main button: the actions side by
     * side (72 high), the error with its action ([actionText]) on the same line, the main button whole (56) and above
     * the slot. Taps
     * the main button once.
     */
    private fun assertCompactColumn(
        state: String,
        message: String,
        actionText: String,
        saveText: String,
        saveWidth: Float,
    ) {
        val slot = bounds(FAKE_AD_SLOT)
        assertNear("$state slot top", 424f, slot.top, 0.5f)
        val save = bounds(ReviewTestTags.SAVE)
        val error = bounds(ReviewTestTags.SAVE_ERROR)
        assertNear("$state save top", 348f, save.top, 0.5f)
        assertNear("$state save bottom", 404f, save.bottom, 0.5f)
        assertNear("$state save width", saveWidth, save.width, 1f)
        assertTrue(
            "$state save ${save.top} 16 under the error ${error.bottom}",
            save.top.value >= error.bottom.value + 15.5f,
        )
        assertActionsAndSaveAbove(slot)
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(saveText, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val label = layouts.single()
        assertFalse("$state label overflows its height", label.didOverflowHeight)
        // didOverflowWidth of this action is true even in the unchanged layout (the paragraph takes the whole width of
        // its constraints): the drawn line must fit in the size of the text instead, not cut nor ellipsized.
        val lineWidth = label.getLineRight(0) - label.getLineLeft(0)
        assertTrue("$state label line $lineWidth wider than ${label.size.width}", lineWidth <= label.size.width + 0.5f)
        assertFalse("$state label ellipsized", label.isLineEllipsized(0))
        assertEquals("$state label lines", 1, label.lineCount)
        // The actions side by side, as on a phone.
        val actions = bounds(ReviewTestTags.ACTIONS)
        assertNear("$state actions top", 64f, actions.top, 0.5f)
        assertNear("$state actions bottom", 136f, actions.bottom, 0.5f)
        actionTags.forEach { assertNear("$state $it top", 64f, bounds(it).top, 0.5f) }
        // The error on one line, its action beside the message.
        assertNear("$state error top", 284f, error.top, 1f)
        assertNear("$state error bottom", 332f, error.bottom, 1f)
        val messageLayouts = mutableListOf<TextLayoutResult>()
        composeRule.onNode(inSaveError(message), useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(messageLayouts) }
        assertEquals("$state message lines", 1, messageLayouts.single().lineCount)
        val text = composeRule.onNode(inSaveError(message), useUnmergedTree = true).getUnclippedBoundsInRoot()
        val action = composeRule.onNode(inSaveError(actionText), useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("$state action ${action.top} on the line of the message ${text.bottom}", action.top < text.bottom)
        assertTrue("$state action ${action.left} right of the message ${text.right}", action.left >= text.right)
        composeRule.onNodeWithTag(ReviewTestTags.SAVE).performClick()
    }

    @Test
    @Config(qualifiers = "vi-w841dp-h593dp-land-xxhdpi")
    fun landscape_adBar_thresholdBoundary() {
        // Column 413 high with the slot: under the threshold with an error (420), over it without (264).
        setScreen(Ready(PATH, SaveStatus.PERMISSION_BLOCKED), adBar = fakeAdBar())
        val actions = bounds(ReviewTestTags.ACTIONS)
        assertNear("PERMISSION_BLOCKED actions top", 64f, actions.top, 0.5f)
        assertNear("PERMISSION_BLOCKED actions height", 72f, actions.height, 0.5f)
        actionTags.forEach { assertNear("PERMISSION_BLOCKED $it top", 64f, bounds(it).top, 0.5f) }
        assertSaveAt("PERMISSION_BLOCKED", 421f, 477f)

        update { screenState = Ready(PATH, SaveStatus.NOT_SAVED) }

        val stacked = bounds(ReviewTestTags.ACTIONS)
        assertNear("NOT_SAVED actions top", 64f, stacked.top, 0.5f)
        assertNear("NOT_SAVED actions bottom", 256f, stacked.bottom, 0.5f)
        val tops = actionTags.map { bounds(it).top.value }
        assertEquals("NOT_SAVED actions stacked: $tops", 3, tops.toSet().size)
        assertSaveAt("NOT_SAVED", 421f, 477f)
    }

    private fun assertSaveAt(state: String, top: Float, bottom: Float) {
        val save = bounds(ReviewTestTags.SAVE)
        assertNear("$state save top", top, save.top, 0.5f)
        assertNear("$state save bottom", bottom, save.bottom, 0.5f)
        assertActionsAndSaveAbove(bounds(FAKE_AD_SLOT))
    }

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
@Config(qualifiers = "vi-w673dp-h841dp-xxhdpi")
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

    // Named like the design image (plan T-094): the slot spans the bottom, the main button above it.
    @Test
    fun ad_dark_unfolded() {
        captureRoboImage("src/test/screenshots/ReviewScreen_ad_dark_unfolded.png") {
            Screen(Ready(PATH, SaveStatus.NOT_SAVED), adBar = screenshotAdBar())
        }
    }

    private companion object {
        const val PATH = "/cache/captures/capture_1.jpg"
    }
}

@Composable
private fun Screen(uiState: ReviewUiState, adBar: ReviewAdBar? = null) {
    AppTheme(darkTheme = true) {
        if (adBar == null) {
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
        } else {
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
                adBar = adBar,
            )
        }
    }
}
