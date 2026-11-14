package com.vglstudio.camera.pose.line.guide.feature.camera.review

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.ads.AdPlacement
import com.vglstudio.camera.pose.line.guide.core.ads.BottomAdBarDefaults
import com.vglstudio.camera.pose.line.guide.core.ads.BottomAdBarLayout
import com.vglstudio.camera.pose.line.guide.core.ads.BottomAdBarScaffold
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PrimaryButtonTestTags
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import com.vglstudio.camera.pose.line.guide.feature.camera.review.ReviewUiState.Ready
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The stateless review screen on the JVM (spec camera, KB-6). Text follows the text table of the design
 * (docs/features/camera/design/README.md, section 2) and plan T-016, section 8.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "vi-w411dp-h891dp-xxhdpi")
class ReviewScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()
    private var inversePrimary = Color.Unspecified
    private var screenState by mutableStateOf<ReviewUiState>(ReviewUiState.Loading)

    /** The screen in [uiState]; with [adBar], in that bar instead of the default one of the screen (plan T-094). */
    private fun setScreen(uiState: ReviewUiState, adBar: ReviewAdBar? = null) {
        screenState = uiState
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                inversePrimary = MaterialTheme.colorScheme.inversePrimary
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

    // The action of the message is drawn in inversePrimary (README, "Xem lại: lưu lỗi"), not the TextButton default.

    @Test
    fun saveError_retryUsesInversePrimary() {
        setScreen(Ready(PATH, SaveStatus.FAILED_STORAGE_FULL))
        assertDrawnIn(inSaveError("Thử lại"), inversePrimary)
    }

    @Test
    fun permissionBlocked_settingsUsesInversePrimary() {
        setScreen(Ready(PATH, SaveStatus.PERMISSION_BLOCKED))
        assertDrawnIn(inSaveError("Mở cài đặt"), inversePrimary)
    }

    /** Draws the root view to a Bitmap (L-006) and looks for [color] among the pixels of the node's text. */
    private fun assertDrawnIn(matcher: SemanticsMatcher, color: Color) {
        composeRule.waitForIdle()
        val root = (composeRule as AndroidComposeTestRule<*, *>).activity.window.decorView
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        val bounds = composeRule.onNode(matcher).fetchSemanticsNode().boundsInWindow
        val pixels = buildSet {
            for (x in bounds.left.toInt() until bounds.right.toInt()) {
                for (y in bounds.top.toInt() until bounds.bottom.toInt()) add(bitmap.getPixel(x, y))
            }
        }
        assertTrue(
            "expected ${Integer.toHexString(color.toArgb())} in the text",
            pixels.contains(color.toArgb()),
        )
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

    // Portuguese is usually the longest of the release languages (plan T-050): the long text still fits.
    @Test
    @Config(qualifiers = SMALL_PORTUGUESE)
    fun smallPortuguese_saveError_primaryButtonVisible() {
        setScreen(Ready(PATH, SaveStatus.FAILED_STORAGE_FULL))
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
        assertTrue(
            "error ${error.top} at least ${Spacing.sm} below actions ${actions.bottom}",
            error.top >= actions.bottom + Spacing.sm,
        )
        assertTrue("photo ${photo.bottom} above actions ${actions.top}", photo.bottom <= actions.top)
        val density = composeRule.density.density
        val widthPx = (photo.right - photo.left).value * density
        val heightPx = (photo.bottom - photo.top).value * density
        assertTrue("photo $widthPx x $heightPx px", heightPx > 0f && abs(widthPx - heightPx * 0.75f) <= 1f)
    }

    // The bottom ad bar of the review screen (plan T-094; spec camera, KB-9 criteria 5 to 9): a fake 80 dp slot in the
    // bar of the framework (fakeAdBar). Numbers from section 4.1 of the plan, measured from the top of the test frame
    // (no status bar, so 24 dp higher than design/context/ReviewScreen_ad_dark.md; the button is 56 high, not 60).

    private fun bounds(tag: String): DpRect = composeRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()

    private fun assertNear(label: String, expected: Float, actual: Dp, tolerance: Float) =
        assertEquals(label, expected, actual.value, tolerance)

    private fun assertAbove(label: String, box: DpRect, slot: DpRect) =
        assertTrue("$label bottom ${box.bottom} above the slot ${slot.top}", box.bottom.value <= slot.top.value + 0.5f)

    private fun assertActionsAndSaveAbove(slot: DpRect) {
        listOf(ReviewTestTags.RETAKE, ReviewTestTags.SHARE, ReviewTestTags.NEXT_POSE, ReviewTestTags.SAVE).forEach {
            assertAbove(it, bounds(it), slot)
        }
        assertNear("save height", 56f, bounds(ReviewTestTags.SAVE).height, 0.5f)
    }

    private fun assertPhoto3to4(photo: DpRect) {
        assertTrue("photo height ${photo.height}", photo.height.value > 0f)
        assertEquals("photo width / height", 0.75f, photo.width.value / photo.height.value, 0.01f)
    }

    @Test
    fun adBar_slotAtBottom_photoKept_saveMovesUp() {
        var withSlot by mutableStateOf(false)
        setScreen(Ready(PATH, SaveStatus.NOT_SAVED), adBar = fakeAdBar(withSlot = { withSlot }))
        val photoBefore = bounds(ReviewTestTags.PHOTO)
        val actionsBefore = bounds(ReviewTestTags.ACTIONS)
        val saveBefore = bounds(ReviewTestTags.SAVE)

        update { withSlot = true }

        val root = composeRule.onRoot().getUnclippedBoundsInRoot()
        val slot = bounds(FAKE_AD_SLOT)
        assertNear("slot height", 80f, slot.height, 0.5f)
        assertNear("slot to the bottom", 16f, root.bottom - slot.bottom, 0.5f)
        assertNear("slot top", 795f, slot.top, 0.5f)
        assertActionsAndSaveAbove(slot)
        val save = bounds(ReviewTestTags.SAVE)
        assertNear("save top", 719f, save.top, 0.5f)
        assertNear("save bottom", 775f, save.bottom, 0.5f)
        assertNear("save to the slot", 20f, slot.top - save.bottom, 0.5f)
        assertNear("save moved up", 96f, saveBefore.top - save.top, 0.5f)
        // The photo keeps 379 x 505 while there is room (as without the slot), only the main button moves up 96.
        val photo = bounds(ReviewTestTags.PHOTO)
        assertNear("photo width", 379f, photo.width, 1f)
        assertNear("photo height", 505.3f, photo.height, 1f)
        assertNear("photo top", 64f, photo.top, 1f)
        assertNear("photo top kept", photoBefore.top.value, photo.top, 0.5f)
        assertNear("photo height kept", photoBefore.height.value, photo.height, 0.5f)
        val actions = bounds(ReviewTestTags.ACTIONS)
        assertNear("actions top", 593.3f, actions.top, 1f)
        assertNear("actions top kept", actionsBefore.top.value, actions.top, 0.5f)
    }

    @Test
    fun adBar_buttonsStillCallBack() {
        setScreen(Ready(PATH, SaveStatus.NOT_SAVED), adBar = fakeAdBar())
        composeRule.onNodeWithTag(FAKE_AD_SLOT).assertExists()
        composeRule.onNodeWithText("Lưu vào thư viện").performClick()
        composeRule.onNodeWithText("Chụp lại").performClick()
        composeRule.onNodeWithText("Chia sẻ").performClick()
        composeRule.onNodeWithText("Tư thế tiếp").performClick()
        assertEquals(listOf("save", "retake", "share", "nextPose"), calls)
    }

    @Test
    fun adBar_savingAndSaveError_slotStays() {
        setScreen(Ready(PATH, SaveStatus.SAVED), adBar = fakeAdBar())
        // Opened from the thumbnail of the last photo: saved, the main button off; laid out like NOT_SAVED.
        assertSlotStays("SAVED")
        assertNear("SAVED save top", 719f, bounds(ReviewTestTags.SAVE).top, 0.5f)
        assertNear("SAVED photo height", 505.3f, bounds(ReviewTestTags.PHOTO).height, 1f)

        update { screenState = Ready(PATH, SaveStatus.FAILED_STORAGE_FULL) }
        assertSlotStays("FAILED_STORAGE_FULL")
        val photo = bounds(ReviewTestTags.PHOTO)
        assertNear("FAILED_STORAGE_FULL photo width", 359.3f, photo.width, 1f)
        assertNear("FAILED_STORAGE_FULL photo height", 479f, photo.height, 1f)
        assertNear("FAILED_STORAGE_FULL error height", 48f, bounds(ReviewTestTags.SAVE_ERROR).height, 1f)

        update { screenState = Ready(PATH, SaveStatus.PERMISSION_BLOCKED) }
        assertSlotStays("PERMISSION_BLOCKED")

        // The spinner never ends: keep the frame clock still (L-029).
        composeRule.mainClock.autoAdvance = false
        composeRule.runOnUiThread {
            screenState = Ready(PATH, SaveStatus.SAVING)
            // With the clock stopped no frame sends the change: send it so the next frame recomposes.
            Snapshot.sendApplyNotifications()
        }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithText("Đang lưu…").assertExists()
        assertSlotStays("SAVING")
    }

    private fun assertSlotStays(state: String) {
        val slot = bounds(FAKE_AD_SLOT)
        assertNear("$state slot top", 795f, slot.top, 0.5f)
        if (composeRule.onAllNodesWithTag(ReviewTestTags.SAVE_ERROR).fetchSemanticsNodes().isNotEmpty()) {
            assertAbove("$state error", bounds(ReviewTestTags.SAVE_ERROR), slot)
        }
        assertActionsAndSaveAbove(slot)
    }

    @Test
    fun adBar_discardDialog_coversSlot() {
        setScreen(Ready(PATH, SaveStatus.NOT_SAVED), adBar = fakeAdBar())
        val node = composeRule.onNodeWithTag(FAKE_AD_SLOT).fetchSemanticsNode()
        val inWindow = node.boundsInWindow.center
        val inRoot = node.boundsInRoot.center
        // Control: without the dialog the middle of the slot is the red of the fake ad.
        val before = drawnPixel(inWindow)
        assertTrue(
            "slot red ${Integer.toHexString(before)}",
            red(before) >= 240 && green(before) <= 15 && blue(before) <= 15,
        )

        update { screenState = Ready(PATH, SaveStatus.NOT_SAVED, pendingExit = ExitAction.RETAKE) }

        val covered = drawnPixel(inWindow)
        assertTrue("scrim over the slot ${Integer.toHexString(covered)}", red(covered) < 200)
        composeRule.onRoot().performTouchInput { click(inRoot) }
        assertEquals(listOf("dismiss"), calls)
    }

    private fun drawnPixel(point: Offset): Int {
        composeRule.waitForIdle()
        val root = (composeRule as AndroidComposeTestRule<*, *>).activity.window.decorView
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        return bitmap.getPixel(point.x.toInt(), point.y.toInt())
    }

    private fun red(pixel: Int) = (pixel shr 16) and 0xFF

    private fun green(pixel: Int) = (pixel shr 8) and 0xFF

    private fun blue(pixel: Int) = pixel and 0xFF

    @Test
    fun adBar_discardDialog_hidesSlotFromAccessibility() {
        setScreen(Ready(PATH, SaveStatus.NOT_SAVED), adBar = fakeAdBar())
        // Control: with the dialog closed the text of the slot is read.
        assertEquals(1, composeRule.onAllNodesWithText(FAKE_AD_TEXT).fetchSemanticsNodes().size)

        update { screenState = Ready(PATH, SaveStatus.NOT_SAVED, pendingExit = ExitAction.RETAKE) }

        composeRule.onNodeWithTag(ReviewTestTags.DISCARD_DIALOG).assertExists()
        assertTrue(composeRule.onAllNodesWithText(FAKE_AD_TEXT).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun adBar_discardDialog_hidesAndroidViewSlot() {
        // The real slot is a view of the ads SDK: screen readers see it through the accessibility node provider of the
        // compose view (plan T-094, section 4.1).
        val activity = (composeRule as AndroidComposeTestRule<*, *>).activity
        val accessibility = activity.getSystemService(AccessibilityManager::class.java)
        shadowOf(accessibility).setEnabled(true)
        shadowOf(accessibility).setTouchExplorationEnabled(true)
        setScreen(
            Ready(PATH, SaveStatus.NOT_SAVED),
            adBar = fakeAdBar(
                slot = { modifier ->
                    AndroidView(
                        factory = { TextView(it).apply { text = "AdViewText" } },
                        modifier = modifier.height(80.dp),
                    )
                },
            ),
        )
        // Control: with the dialog closed the view of the slot is one child of the tree.
        assertEquals(1, accessibleViewChildren(activity.window.decorView))

        update { screenState = Ready(PATH, SaveStatus.NOT_SAVED, pendingExit = ExitAction.RETAKE) }

        assertEquals(0, accessibleViewChildren(activity.window.decorView))
    }

    @Test
    fun realAdBar_withoutAds_takesNoSpace() {
        var real by mutableStateOf(true)
        val fake = fakeAdBar()
        setScreen(
            Ready(PATH, SaveStatus.NOT_SAVED),
            adBar = { barModifier, content ->
                if (real) {
                    BottomAdBarScaffold(
                        AdPlacement.REVIEW_BOTTOM,
                        barModifier,
                        content,
                    )
                } else {
                    fake(barModifier, content)
                }
            },
        )
        val root = composeRule.onRoot().getUnclippedBoundsInRoot()
        composeRule.onAllNodesWithTag(BAR_SLOT).assertCountEquals(0)
        assertNear("save to the bottom", 20f, root.bottom - bounds(ReviewTestTags.SAVE).bottom, 0.5f)

        // Control: the same screen in a bar with a slot keeps 96 under the content.
        update { real = false }
        assertNear("control: save to the bottom", 116f, root.bottom - bounds(ReviewTestTags.SAVE).bottom, 0.5f)
    }

    @Test
    fun adBar_permissionBlocked_photoShrinks_allAbove() {
        setScreen(Ready(PATH, SaveStatus.PERMISSION_BLOCKED), adBar = fakeAdBar())
        val slot = bounds(FAKE_AD_SLOT)
        val photo = bounds(ReviewTestTags.PHOTO)
        assertNear("photo width", 344.3f, photo.width, 1f)
        assertNear("photo height", 459f, photo.height, 1f)
        assertPhoto3to4(photo)
        val error = bounds(ReviewTestTags.SAVE_ERROR)
        assertNear("error height", 68f, error.height, 1f)
        assertAbove("error", error, slot)
        assertActionsAndSaveAbove(slot)
    }

    @Test
    fun adBar_banner_photoAndButtonsAbove() {
        val banner: ReviewAdBar = { barModifier, content ->
            BottomAdBarLayout(
                reservation = 50.dp,
                slotMargin = 0.dp,
                slot = { Box(it.height(50.dp).testTag(FAKE_BANNER)) },
                layer = null,
                modifier = barModifier,
                content = content,
            )
        }
        setScreen(Ready(PATH, SaveStatus.NOT_SAVED), adBar = banner)
        val root = composeRule.onRoot().getUnclippedBoundsInRoot()
        val slot = composeRule.onNodeWithTag(FAKE_BANNER, useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertNear("banner at the bottom", root.bottom.value, slot.bottom, 0.5f)
        assertNear("banner width", 411f, slot.width, 0.5f)
        assertAbove("photo", bounds(ReviewTestTags.PHOTO), slot)
        assertActionsAndSaveAbove(slot)
    }

    @Test
    @Config(qualifiers = SHORT_SCREEN)
    fun small_adBar_photoShrinks_allVisible() {
        var withSlot by mutableStateOf(false)
        setScreen(Ready(PATH, SaveStatus.NOT_SAVED), adBar = fakeAdBar(withSlot = { withSlot }))
        val photoBefore = bounds(ReviewTestTags.PHOTO)
        assertNear("photo width without the slot", 291f, photoBefore.width, 1f)
        assertNear("photo height without the slot", 388f, photoBefore.height, 1f)

        update { withSlot = true }

        val slot = bounds(FAKE_AD_SLOT)
        assertNear("slot top", 544f, slot.top, 0.5f)
        val photo = bounds(ReviewTestTags.PHOTO)
        assertNear("photo width", 219f, photo.width, 1f)
        assertNear("photo height", 292f, photo.height, 1f)
        assertPhoto3to4(photo)
        val save = bounds(ReviewTestTags.SAVE)
        assertNear("save top", 468f, save.top, 0.5f)
        assertNear("save bottom", 524f, save.bottom, 0.5f)
        assertActionsAndSaveAbove(slot)

        update { screenState = Ready(PATH, SaveStatus.PERMISSION_BLOCKED) }

        val blockedPhoto = bounds(ReviewTestTags.PHOTO)
        assertNear("blocked photo width", 156f, blockedPhoto.width, 1f)
        assertNear("blocked photo height", 208f, blockedPhoto.height, 1f)
        assertPhoto3to4(blockedPhoto)
        assertAllVisibleAbove(bounds(FAKE_AD_SLOT), "Mở cài đặt")
    }

    @Test
    @Config(qualifiers = SMALL_PORTUGUESE)
    fun smallPortuguese_adBar_permissionBlocked_allVisible() {
        setScreen(Ready(PATH, SaveStatus.PERMISSION_BLOCKED), adBar = fakeAdBar())
        val photo = bounds(ReviewTestTags.PHOTO)
        assertNear("photo width", 131f, photo.width, 1f)
        assertNear("photo height", 174.7f, photo.height, 1f)
        assertPhoto3to4(photo)
        assertNear("error height", 101.3f, bounds(ReviewTestTags.SAVE_ERROR).height, 1f)
        assertAllVisibleAbove(bounds(FAKE_AD_SLOT), "Abrir configurações")
    }

    /** The message, its [action], the three actions and the main button are on the screen, above [slot]. */
    private fun assertAllVisibleAbove(slot: DpRect, action: String) {
        val root = composeRule.onRoot().getUnclippedBoundsInRoot()
        val error = bounds(ReviewTestTags.SAVE_ERROR)
        val actionButton = composeRule.onNode(inSaveError(action)).getUnclippedBoundsInRoot()
        assertAbove("error", error, slot)
        assertAbove("action $action", actionButton, slot)
        assertActionsAndSaveAbove(slot)
        listOf(ReviewTestTags.PHOTO, ReviewTestTags.SAVE_ERROR, ReviewTestTags.SAVE).forEach {
            val box = bounds(it)
            assertTrue(
                "$it ${box.top} to ${box.bottom} inside the screen",
                box.top >= root.top && box.bottom <= root.bottom,
            )
        }
    }

    private fun inSaveError(text: String) = hasText(text) and hasAnyAncestor(hasTestTag(ReviewTestTags.SAVE_ERROR))

    private fun inDialog(text: String) = hasText(text) and hasAnyAncestor(hasTestTag(ReviewTestTags.DISCARD_DIALOG))

    private companion object {
        const val PATH = "/cache/captures/capture_1.jpg"
        const val SHORT_SCREEN = "vi-w360dp-h640dp-xxhdpi"
        const val SMALL_PORTUGUESE = "pt-rBR-w360dp-h640dp-xxhdpi"
        const val FAKE_BANNER = "fake_banner"
    }
}

/** The `adBar` of [ReviewScreen]: the bottom bar around the screen (plan T-094). */
internal typealias ReviewAdBar = @Composable (Modifier, @Composable (PaddingValues) -> Unit) -> Unit

/** Tag of the fake slot of [fakeAdBar], and the text it shows. */
internal const val FAKE_AD_SLOT = "fake_ad_slot"
internal const val FAKE_AD_TEXT = "Ad"

/** Tag of the slot of the framework's bar (BottomAdBarTestTags.SLOT of :core:ads, internal there). */
internal const val BAR_SLOT = "bottom_ad_bar_slot"

/**
 * The bottom bar of the framework with a fake 80 dp red slot (plan T-094, section 4), or [slot]: it takes the modifier
 * the bar gives, so it is as wide as the screen less 2 x 16. [withSlot] false is a bar without an ad: no slot, nothing
 * kept.
 */
internal fun fakeAdBar(
    withSlot: () -> Boolean = { true },
    slot: @Composable (Modifier) -> Unit = { FakeAdSlot(it) },
): ReviewAdBar = { barModifier, content ->
    val shown = withSlot()
    BottomAdBarLayout(
        reservation = if (shown) BottomAdBarDefaults.nativeReservation else 0.dp,
        slot = if (shown) slot else null,
        layer = null,
        modifier = barModifier,
        content = content,
    )
}

@Composable
private fun FakeAdSlot(modifier: Modifier) {
    Box(modifier = modifier.height(80.dp).background(Color.Red).testTag(FAKE_AD_SLOT)) { Text(FAKE_AD_TEXT) }
}

/**
 * How many real views (not compose nodes) screen readers reach in the compose view under [root]: walks the nodes of
 * its accessibility node provider from the host (-1), the child ids read by reflection (hidden API); a child whose
 * virtual id is -1 is a view (an AndroidView holder).
 */
internal fun accessibleViewChildren(root: View): Int {
    val composeView = findComposeView(root) ?: error("no compose view under $root")
    val provider = composeView.accessibilityNodeProvider ?: error("no accessibility node provider")
    var views = 0
    fun visit(virtualId: Int) {
        val info = provider.createAccessibilityNodeInfo(virtualId) ?: return
        childNodeIds(info).forEach { id ->
            val virtual = (id shr 32).toInt()
            if (virtual == View.NO_ID) views++ else visit(virtual)
        }
    }
    visit(View.NO_ID)
    return views
}

private fun findComposeView(view: View): View? {
    if (view.javaClass.simpleName == "AndroidComposeView") return view
    if (view !is ViewGroup) return null
    return (0 until view.childCount).firstNotNullOfOrNull { findComposeView(view.getChildAt(it)) }
}

private fun childNodeIds(info: AccessibilityNodeInfo): List<Long> {
    val ids = AccessibilityNodeInfo::class.java.getMethod("getChildNodeIds").invoke(info) ?: return emptyList()
    val size = ids.javaClass.getMethod("size").invoke(ids) as Int
    val get = ids.javaClass.getMethod("get", Int::class.javaPrimitiveType)
    return (0 until size).map { get.invoke(ids, it) as Long }
}
