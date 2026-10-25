package com.vglstudio.camera.pose.line.guide.feature.camera

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.feature.camera.CameraUiState.Ready
import com.vglstudio.camera.pose.line.guide.feature.camera.model.FlashMode
import com.vglstudio.camera.pose.line.guide.feature.camera.model.Lens
import com.vglstudio.camera.pose.line.guide.feature.camera.model.TimerSetting
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.CategoryChip
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.PoseActions
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.PoseImage
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.PoseThumbItem
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.PoseUiState
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.SelectedPose
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.StripScroll
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The pose part of the camera screen on the JVM (spec camera, KB-1, KB-2, KB-4, KB-7; plan T-014). Text follows the
 * text table of the design (README, section 2), the spec (section 7) and the plan (questions 8 and 11).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "vi-w411dp-h891dp-xxhdpi")
class CameraScreenPoseTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()

    private val actions = PoseActions(
        onCategorySelect = { calls += "category $it" },
        onPoseSelect = { calls += "pose $it" },
        onThumbShown = { calls += "shown $it" },
        onPreviousStep = { calls += "previous" },
        onNextStep = { calls += "next" },
        onHideSteps = { calls += "hide" },
        onShowSteps = { calls += "show" },
        onRetryOutline = { calls += "retryOutline" },
        onRetryLibrary = { calls += "retryLibrary" },
    )

    private var cameraState by mutableStateOf<CameraUiState>(READY)
    private var poseState by mutableStateOf<PoseUiState>(PoseUiState.Loading)

    private fun setScreen(camera: CameraUiState = READY, pose: PoseUiState = poseReady()) {
        cameraState = camera
        poseState = pose
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

    // KB-1: chips

    @Test
    fun chips_showAllFirst_thenCategories_andSelect() {
        setScreen()
        val all = composeRule.onNodeWithText("Tất cả").getUnclippedBoundsInRoot()
        val women = composeRule.onNodeWithText("Nữ").getUnclippedBoundsInRoot()
        val men = composeRule.onNodeWithText("Nam").getUnclippedBoundsInRoot()
        assertTrue(all.left < women.left && women.left < men.left)
        composeRule.onNodeWithText("Tất cả").assertIsSelected()
        composeRule.onNodeWithText("Nữ").assertIsNotSelected()

        composeRule.onNodeWithText("Nam").performClick()
        composeRule.onNodeWithText("Tất cả").performClick()
        assertEquals(listOf("category men", "category null"), calls.filter { it.startsWith("category") })
    }

    // KB-1: strip

    @Test
    fun strip_showsThumbs_selectedMarked_andSelects() {
        setScreen()
        composeRule.onNodeWithTag(CameraTestTags.pose("w02")).assertIsSelected()
        composeRule.onNodeWithTag(CameraTestTags.pose("w01")).assertIsNotSelected().performClick()
        assertTrue(calls.contains("pose w01"))
        assertEquals(
            STRIP_IDS.map { "shown $it" }.toSet(),
            calls.filter { it.startsWith("shown") }.toSet(),
        )
    }

    @Test
    fun failedThumb_hasRetryDescription_andTapSelects() {
        setScreen(pose = poseReady(strip = STRIP_IDS.map { item(it, if (it == "w01") PoseImage.Failed else IMAGE) }))
        composeRule.onNodeWithTag(CameraTestTags.pose("w01"))
            .assert(hasContentDescription("Pose w01, chưa tải được ảnh, chạm để thử lại"))
            .performClick()
        assertTrue(calls.contains("pose w01"))
    }

    @Test
    fun scrollRequest_scrollsStripToPose() {
        val many = (0 until 20).map { item("p%02d".format(it)) }
        setScreen(pose = poseReady(strip = many, selected = null, scrollRequest = StripScroll(index = 15, key = 1)))
        composeRule.onNodeWithTag(CameraTestTags.pose("p15")).assertIsDisplayed()
    }

    // KB-1: outline and no pose

    @Test
    fun outline_shownWhenCameraReadyAndLoaded() {
        setScreen()
        assertTrue(exists(CameraTestTags.OUTLINE))

        poseState = poseReady(selected = selected(outline = PoseImage.Loading))
        composeRule.waitForIdle()
        assertTrue(!exists(CameraTestTags.OUTLINE))
        assertTrue(exists(CameraTestTags.STEP_CARD))
    }

    @Test
    fun noPose_showsHint_withoutOutlineOrSteps() {
        setScreen(pose = poseReady(selected = null))
        composeRule.onNodeWithText("Chọn một tư thế ở dải bên dưới").assertExists()
        assertTrue(exists(CameraTestTags.NO_POSE_HINT))
        assertTrue(!exists(CameraTestTags.OUTLINE))
        assertTrue(!exists(CameraTestTags.STEP_CARD))
        assertTrue(!exists(CameraTestTags.SHOW_STEPS))
    }

    // KB-7, KB-8: the picker works in every camera state, the overlay only when ready

    @Test
    fun permissionDenied_chipsAndStripStillWork() {
        setScreen(camera = CameraUiState.PermissionDenied)
        composeRule.onNodeWithText("Nam").performClick()
        composeRule.onNodeWithTag(CameraTestTags.pose("w01")).performClick()
        assertTrue(calls.containsAll(listOf("category men", "pose w01")))
        assertTrue(!exists(CameraTestTags.OUTLINE))
        assertTrue(!exists(CameraTestTags.STEP_CARD))
    }

    @Test
    fun notReady_hidesOverlay_keepsPicker() {
        setScreen(camera = CameraUiState.Starting)
        listOf(CameraUiState.Starting, CameraUiState.Error).forEach { state ->
            cameraState = state
            composeRule.waitForIdle()
            assertTrue("$state", exists(CameraTestTags.CHIPS) && exists(CameraTestTags.STRIP))
            assertTrue("$state", !exists(CameraTestTags.OUTLINE) && !exists(CameraTestTags.STEP_CARD))
        }
    }

    // KB-4: switching camera keeps the pose

    @Test
    fun flipLens_keepsPoseOverlay() {
        setScreen()
        assertTrue(exists(CameraTestTags.OUTLINE))
        cameraState = READY.copy(lens = Lens.FRONT)
        composeRule.waitForIdle()
        assertTrue(exists(CameraTestTags.OUTLINE))
        composeRule.onNodeWithText("Step 2").assertExists()
    }

    // KB-2: step card

    @Test
    fun stepCard_showsCountAndText() {
        setScreen()
        composeRule.onNodeWithText("2/4").assertExists()
        composeRule.onNodeWithText("Step 2").assertExists()

        poseState = poseReady(selected = selected(stepIndex = 0))
        composeRule.waitForIdle()
        composeRule.onNodeWithText("1/4").assertExists()
        composeRule.onNodeWithText("Step 1").assertExists()

        poseState = poseReady(selected = selected(stepIndex = 3))
        composeRule.waitForIdle()
        composeRule.onNodeWithText("4/4").assertExists()
        composeRule.onNodeWithText("Step 4").assertExists()
    }

    @Test
    fun stepArrows_clickAndDisableAtEnds() {
        setScreen(pose = poseReady(selected = selected(stepIndex = 0)))
        composeRule.onNodeWithContentDescription("Bước trước").assertIsNotEnabled().performClick()
        composeRule.onNodeWithContentDescription("Bước sau").performClick()
        assertEquals(listOf("next"), calls.filter { it == "next" || it == "previous" })

        poseState = poseReady(selected = selected(stepIndex = 3))
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Bước sau").assertIsNotEnabled().performClick()
        composeRule.onNodeWithContentDescription("Bước trước").performClick()
        assertEquals(listOf("next", "previous"), calls.filter { it == "next" || it == "previous" })
    }

    @Test
    fun swipeUpOnStepCard_hides() {
        setScreen()
        composeRule.onNodeWithTag(CameraTestTags.STEP_CARD).performTouchInput { swipeUp() }
        assertEquals(1, calls.count { it == "hide" })
    }

    @Test
    fun smallDragUp_doesNotHide() {
        setScreen()
        composeRule.onNodeWithTag(CameraTestTags.STEP_CARD).performTouchInput {
            down(center)
            moveBy(Offset(0f, -10.dp.toPx()))
            up()
        }
        assertEquals(0, calls.count { it == "hide" })
    }

    @Test
    fun stepCard_hasHideAccessibilityAction() {
        setScreen()
        val actions = composeRule.onNodeWithTag(CameraTestTags.STEP_CARD).fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]
        val hide = actions.first { it.label == "Ẩn thẻ bước" }
        composeRule.runOnIdle { hide.action() }
        assertEquals(1, calls.count { it == "hide" })
    }

    @Test
    fun stepsHidden_showsStepsButton_andShowsCard() {
        setScreen(pose = poseReady(stepsHidden = true))
        assertTrue(!exists(CameraTestTags.STEP_CARD))
        composeRule.onNodeWithText("Bước").assertExists()
        composeRule.onNodeWithTag(CameraTestTags.SHOW_STEPS).assertHeightIsEqualTo(48.dp).performClick()
        assertEquals(1, calls.count { it == "show" })
    }

    // Spec section 7 and plan question 8: load errors

    @Test
    fun outlineFailed_stepCardShowsRetry() {
        setScreen(pose = poseReady(selected = selected(outline = PoseImage.Failed)))
        composeRule.onNodeWithText("Chưa tải được nét viền").assertExists()
        composeRule.onNodeWithContentDescription("Bước trước").assertDoesNotExist()
        composeRule.onNodeWithText("Thử lại").performClick()
        assertEquals(1, calls.count { it == "retryOutline" })
        assertTrue(!exists(CameraTestTags.OUTLINE))
    }

    @Test
    fun posesUnavailable_showsRetry() {
        setScreen(pose = PoseUiState.Unavailable)
        composeRule.onNodeWithText("Chưa tải được danh sách tư thế.").assertExists()
        composeRule.onNodeWithText("Thử lại").performClick()
        assertEquals(1, calls.count { it == "retryLibrary" })
    }

    // Short phone frame (L-028)

    @Test
    @Config(qualifiers = SHORT_SCREEN)
    fun small_default_stepCardInsidePreview_andPickerFullSize() {
        setScreen()
        val preview = composeRule.onNodeWithTag(CameraTestTags.PREVIEW).getUnclippedBoundsInRoot()
        val card = composeRule.onNodeWithTag(CameraTestTags.STEP_CARD).getUnclippedBoundsInRoot()
        assertTrue("card ${card.bottom}, preview ${preview.bottom}", card.bottom <= preview.bottom)
        val text = composeRule.onNodeWithText("Step 2").getUnclippedBoundsInRoot()
        assertTrue("text width ${text.width}", text.width >= 100.dp)
        listOf("Bước trước", "Bước sau").forEach {
            composeRule.onNodeWithContentDescription(it).assertWidthIsEqualTo(40.dp).assertHeightIsEqualTo(48.dp)
        }
        composeRule.onNodeWithTag(CameraTestTags.CHIPS).assertHeightIsEqualTo(52.dp)
        composeRule.onNodeWithTag(CameraTestTags.STRIP).assertHeightIsEqualTo(80.dp)
        val root = composeRule.onRoot().getUnclippedBoundsInRoot()
        val shutter = composeRule.onNodeWithTag(CameraTestTags.SHUTTER).getUnclippedBoundsInRoot()
        assertTrue(
            "shutter ${shutter.bottom}, root ${root.bottom}",
            shutter.top >= root.top && shutter.bottom <= root.bottom,
        )
    }

    @Test
    @Config(qualifiers = SHORT_SCREEN)
    fun small_noPose_hintInsidePreview() {
        setScreen(pose = poseReady(selected = null))
        val preview = composeRule.onNodeWithTag(CameraTestTags.PREVIEW).getUnclippedBoundsInRoot()
        val hint = composeRule.onNodeWithTag(CameraTestTags.NO_POSE_HINT).getUnclippedBoundsInRoot()
        assertTrue(
            "hint $hint, preview $preview",
            hint.left >= preview.left &&
                hint.right <= preview.right &&
                hint.top >= preview.top &&
                hint.bottom <= preview.bottom,
        )
    }

    private companion object {
        const val SHORT_SCREEN = "vi-w360dp-h640dp-xxhdpi"
        val STRIP_IDS = listOf("w01", "m01", "w02", "m02", "w03")
        val IMAGE = PoseImage.Ready(ImageBitmap(1, 1))

        val READY = Ready(
            flash = FlashMode.OFF,
            flashAvailable = true,
            timer = TimerSetting.OFF,
            gridVisible = false,
            lens = Lens.BACK,
            zoomLevels = listOf(1f, 2f),
            zoom = 1f,
        )

        fun item(id: String, image: PoseImage = IMAGE) = PoseThumbItem(id = id, label = "Pose $id", image = image)

        fun selected(stepIndex: Int = 1, outline: PoseImage = IMAGE) = SelectedPose(
            id = "w02",
            steps = listOf("Step 1", "Step 2", "Step 3", "Step 4"),
            stepIndex = stepIndex,
            outline = outline,
        )

        fun poseReady(
            strip: List<PoseThumbItem> = STRIP_IDS.map { item(it) },
            selected: SelectedPose? = selected(),
            stepsHidden: Boolean = false,
            scrollRequest: StripScroll? = null,
        ) = PoseUiState.Ready(
            categories = listOf(CategoryChip("women", "Nữ"), CategoryChip("men", "Nam")),
            selectedCategoryId = null,
            strip = strip,
            selected = selected,
            stepsHidden = stepsHidden,
            scrollRequest = scrollRequest,
        )
    }
}
