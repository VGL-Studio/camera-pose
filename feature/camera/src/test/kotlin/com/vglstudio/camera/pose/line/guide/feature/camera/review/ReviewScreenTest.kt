package com.vglstudio.camera.pose.line.guide.feature.camera.review

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PrimaryButtonTestTags
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
 * The stateless review screen on the JVM (spec camera, KB-6). Text follows the text table of the design
 * (docs/features/camera/design/README.md, section 2) and plan T-016, section 8.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class ReviewScreenTest {
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

    @Test
    fun default_showsTexts() {
        setScreen(Ready(PATH, SaveStatus.NOT_SAVED))
        listOf("Xem lại", "Chụp lại", "Chia sẻ", "Tư thế tiếp", "Lưu vào thư viện").forEach {
            composeRule.onNodeWithText(it).assertExists()
        }
        composeRule.onNodeWithContentDescription("Đóng").assertHasClickAction()
        composeRule.onNodeWithTag(ReviewTestTags.PHOTO).assertExists()
    }

    @Test
    fun default_buttonsCallBack() {
        setScreen(Ready(PATH, SaveStatus.NOT_SAVED))
        composeRule.onNodeWithText("Lưu vào thư viện").performClick()
        composeRule.onNodeWithText("Chụp lại").performClick()
        composeRule.onNodeWithText("Chia sẻ").performClick()
        composeRule.onNodeWithText("Tư thế tiếp").performClick()
        composeRule.onNodeWithContentDescription("Đóng").performClick()
        assertEquals(listOf("save", "retake", "share", "nextPose", "close"), calls)
    }

    @Test
    fun saving_showsSavingAndDisablesOthers() {
        // The spinner never ends: keep the frame clock still (L-029).
        composeRule.mainClock.autoAdvance = false
        setScreen(Ready(PATH, SaveStatus.SAVING))
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithText("Đang lưu…").assertExists()
        composeRule.onNodeWithTag(PrimaryButtonTestTags.SPINNER, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(ReviewTestTags.RETAKE).assertIsNotEnabled()
        composeRule.onNodeWithTag(ReviewTestTags.SHARE).assertIsNotEnabled()
        composeRule.onNodeWithTag(ReviewTestTags.NEXT_POSE).assertIsNotEnabled()
        composeRule.onNodeWithTag(ReviewTestTags.CLOSE).assertIsNotEnabled()

        composeRule.onNodeWithText("Chụp lại").performClick()
        assertTrue(calls.isEmpty())
    }

    @Test
    fun saved_saveButtonDisabled() {
        setScreen(Ready(PATH, SaveStatus.SAVED))
        composeRule.onNodeWithTag(ReviewTestTags.SAVE).assertIsNotEnabled()
        composeRule.onNodeWithTag(ReviewTestTags.RETAKE).assertIsEnabled()
        composeRule.onNodeWithTag(ReviewTestTags.SHARE).assertIsEnabled()
        composeRule.onNodeWithTag(ReviewTestTags.NEXT_POSE).assertIsEnabled()
    }

    @Test
    fun saveErrorFull_showsMessageAndRetry() {
        setScreen(Ready(PATH, SaveStatus.FAILED_STORAGE_FULL))
        composeRule.onNode(inSaveError("Không lưu được ảnh: bộ nhớ máy đã đầy.")).assertExists()
        composeRule.onNode(inSaveError("Thử lại")).performClick()
        assertEquals(listOf("retry"), calls)
        composeRule.onNodeWithTag(ReviewTestTags.PHOTO).assertExists()
    }

    @Test
    fun saveErrorOther_showsGenericMessage() {
        setScreen(Ready(PATH, SaveStatus.FAILED))
        composeRule.onNode(inSaveError("Không lưu được ảnh.")).assertExists()
        composeRule.onNode(inSaveError("Thử lại")).assertExists()
    }

    @Test
    fun permissionDenied_showsMessageAndRetry() {
        setScreen(Ready(PATH, SaveStatus.PERMISSION_DENIED))
        composeRule.onNode(inSaveError("Cần quyền lưu ảnh để lưu vào thư viện.")).assertExists()
        composeRule.onNode(inSaveError("Thử lại")).performClick()
        assertEquals(listOf("retry"), calls)
    }

    @Test
    fun permissionBlocked_showsMessageAndOpensSettings() {
        setScreen(Ready(PATH, SaveStatus.PERMISSION_BLOCKED))
        composeRule.onNode(inSaveError("Cho phép lưu ảnh trong Cài đặt máy để lưu vào thư viện.")).assertExists()
        composeRule.onNode(inSaveError("Mở cài đặt")).performClick()
        assertEquals(listOf("settings"), calls)
        composeRule.onNodeWithTag(ReviewTestTags.SAVE).assertIsEnabled()
    }

    @Test
    fun discard_showsDialog_andCallsBack() {
        setScreen(Ready(PATH, SaveStatus.NOT_SAVED, pendingExit = ExitAction.RETAKE))
        composeRule.onNode(inDialog("Bỏ ảnh này?")).assertExists()
        composeRule.onNode(inDialog("Ảnh chưa được lưu vào thư viện.")).assertExists()
        composeRule.onNode(inDialog("Bỏ ảnh")).performClick()
        composeRule.onNode(inDialog("Lưu ảnh")).performClick()
        assertEquals(listOf("discard", "discardSave"), calls)
    }

    @Test
    fun discard_scrimTapDismisses() {
        setScreen(Ready(PATH, SaveStatus.NOT_SAVED, pendingExit = ExitAction.RETAKE))
        composeRule.onRoot().performTouchInput { click(Offset(10.dp.toPx(), 10.dp.toPx())) }
        assertEquals(listOf("dismiss"), calls)
    }

    @Test
    fun discard_hidesContentFromAccessibility() {
        setScreen(Ready(PATH, SaveStatus.NOT_SAVED, pendingExit = ExitAction.RETAKE))
        composeRule.onNodeWithTag(ReviewTestTags.DISCARD_DIALOG).assertExists()
        assertTrue(composeRule.onAllNodesWithText("Chụp lại").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("Lưu vào thư viện").fetchSemanticsNodes().isEmpty())
    }

    // Short phone frame (L-028): the error message, three actions and the main button all fit.

    @Test
    @Config(qualifiers = SHORT_SCREEN)
    fun small_saveError_primaryButtonVisible() {
        setScreen(Ready(PATH, SaveStatus.FAILED_STORAGE_FULL))
        assertFitsOnShortScreen()
    }

    @Test
    @Config(qualifiers = SHORT_SCREEN)
    fun small_permissionBlocked_primaryButtonVisible() {
        setScreen(Ready(PATH, SaveStatus.PERMISSION_BLOCKED))
        assertFitsOnShortScreen()
    }

    private fun assertFitsOnShortScreen() {
        val root = composeRule.onRoot().getUnclippedBoundsInRoot()
        val save = composeRule.onNodeWithTag(ReviewTestTags.SAVE).getUnclippedBoundsInRoot()
        val error = composeRule.onNodeWithTag(ReviewTestTags.SAVE_ERROR).getUnclippedBoundsInRoot()
        val actions = composeRule.onNodeWithTag(ReviewTestTags.ACTIONS).getUnclippedBoundsInRoot()
        val photo = composeRule.onNodeWithTag(ReviewTestTags.PHOTO).getUnclippedBoundsInRoot()

        assertTrue("save bottom ${save.bottom} within ${root.bottom}", save.bottom <= root.bottom)
        assertTrue("save height ${save.bottom - save.top}", save.bottom - save.top >= 56.dp)
        assertTrue("error ${error.bottom} above save ${save.top}", error.bottom <= save.top)
        assertTrue("error ${error.top} below actions ${actions.bottom}", error.top >= actions.bottom)
        assertTrue("photo ${photo.bottom} above actions ${actions.top}", photo.bottom <= actions.top)
        val density = composeRule.density.density
        val widthPx = (photo.right - photo.left).value * density
        val heightPx = (photo.bottom - photo.top).value * density
        assertTrue("photo $widthPx x $heightPx px", heightPx > 0f && abs(widthPx - heightPx * 0.75f) <= 1f)
    }

    private fun inSaveError(text: String) = hasText(text) and hasAnyAncestor(hasTestTag(ReviewTestTags.SAVE_ERROR))

    private fun inDialog(text: String) = hasText(text) and hasAnyAncestor(hasTestTag(ReviewTestTags.DISCARD_DIALOG))

    private companion object {
        const val PATH = "/cache/captures/capture_1.jpg"
        const val SHORT_SCREEN = "w360dp-h640dp-xxhdpi"
    }
}
