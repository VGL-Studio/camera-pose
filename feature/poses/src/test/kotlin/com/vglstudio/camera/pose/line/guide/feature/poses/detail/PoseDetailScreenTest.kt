package com.vglstudio.camera.pose.line.guide.feature.poses.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.ads.AdPlacement
import com.vglstudio.camera.pose.line.guide.core.ads.BottomAdBarDefaults
import com.vglstudio.camera.pose.line.guide.core.ads.BottomAdBarLayout
import com.vglstudio.camera.pose.line.guide.core.ads.BottomAdBarScaffold
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.core.poses.model.Framing
import com.vglstudio.camera.pose.line.guide.core.poses.model.LocalizedList
import com.vglstudio.camera.pose.line.guide.core.poses.model.LocalizedText
import com.vglstudio.camera.pose.line.guide.core.poses.model.SubjectType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The pose detail screen (spec poses, KB-3, KB-4, KB-5; plan T-022, section 4). Native graphics: sizes are measured and
 * real bitmaps are drawn. The screen language is Vietnamese, as the text table.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "vi-w411dp-h891dp-xxhdpi")
class PoseDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var backs = 0
    private var toggles = 0
    private var retries = 0
    private var shoots = 0
    private var saveFailedShown = 0

    private var state by mutableStateOf<PoseDetailUiState>(PoseDetailUiState.Loading)

    private fun setScreen(
        initial: PoseDetailUiState,
        bar: (@Composable (screen: @Composable (PaddingValues) -> Unit) -> Unit)? = null,
        width: (() -> Dp)? = null,
    ) {
        state = initial
        composeRule.setContent {
            AppTheme(darkTheme = false) {
                val content = @Composable { contentPadding: PaddingValues ->
                    PoseDetailScreen(
                        uiState = state,
                        onBack = { backs++ },
                        onToggleSave = { toggles++ },
                        onRetryImage = { retries++ },
                        onShoot = { shoots++ },
                        onSaveFailedShown = { saveFailedShown++ },
                        contentPadding = contentPadding,
                    )
                }
                // The screen alone, or inside the bottom bar that gives it its padding (plan T-079, section 4).
                val screen = @Composable { if (bar == null) content(PaddingValues()) else bar(content) }
                if (width == null) {
                    screen()
                } else {
                    // The screen gets that width of the window, from the left edge (KB-7).
                    Box(Modifier.fillMaxSize()) {
                        Box(Modifier.align(Alignment.TopStart).fillMaxHeight().requiredWidth(width())) { screen() }
                    }
                }
            }
        }
    }

    // Flag and catalog

    @Test
    fun disabled_callsOnBackOnce() {
        setScreen(PoseDetailUiState.Disabled)
        composeRule.waitForIdle()
        state = PoseDetailUiState.Disabled
        composeRule.waitForIdle()
        assertEquals(1, backs)
        composeRule.onNodeWithTag(PoseDetailTestTags.SCREEN).assertExists()
    }

    @Test
    fun missing_callsOnBackOnce() {
        setScreen(PoseDetailUiState.Missing)
        composeRule.waitForIdle()
        state = PoseDetailUiState.Missing
        composeRule.waitForIdle()
        assertEquals(1, backs)
        composeRule.onNodeWithTag(PoseDetailTestTags.SCREEN).assertExists()
    }

    @Test
    fun loading_hasBackButton_butNoSaveButtonNorBottomBar() {
        setScreen(PoseDetailUiState.Loading)
        composeRule.onNodeWithContentDescription("Quay lại").assertExists()
        composeRule.onNodeWithContentDescription("Lưu tư thế").assertDoesNotExist()
        composeRule.onNodeWithTag(PoseDetailTestTags.BOTTOM_BAR).assertDoesNotExist()
    }

    // KB-4: content

    @Test
    fun ready_showsTitleMetaStepsMistakesAndButton() {
        setScreen(ready())
        composeRule.onNodeWithText("Tựa lan can").assertExists()
        composeRule.onNodeWithText("Một người · Nữ · Toàn thân · 4 bước").assertExists()
        composeRule.onNodeWithText("Các bước").assertExists()
        STEPS.forEachIndexed { index, step ->
            composeRule.onNodeWithText("${index + 1}", useUnmergedTree = true).assertExists()
            composeRule.onNodeWithText(step).assertExists()
        }
        composeRule.onNodeWithText("Lỗi hay gặp").assertExists()
        composeRule.onNodeWithText(MISTAKE).assertExists()
        composeRule.onNodeWithText("Chụp với tư thế này").assertExists()
        composeRule.onNodeWithTag(PoseDetailTestTags.BOTTOM_BAR).assertExists()
    }

    @Test
    fun steps_areNumberedInOrder() {
        setScreen(ready())
        val numbers = STEPS.indices.map {
            composeRule.onNodeWithText("${it + 1}", useUnmergedTree = true).getUnclippedBoundsInRoot().top
        }
        assertEquals(numbers.sorted(), numbers)
        assertEquals(STEPS.size, numbers.toSet().size)
        // Each text is on the line of its number.
        STEPS.forEachIndexed { index, step ->
            val text = composeRule.onNodeWithText(step).getUnclippedBoundsInRoot()
            val number = composeRule.onNodeWithText("${index + 1}", useUnmergedTree = true).getUnclippedBoundsInRoot()
            assertTrue("step ${index + 1}: ${number.left} < ${text.left}", number.right <= text.left)
            assertTrue(text.top < number.bottom && number.top < text.bottom)
        }
    }

    @Test
    fun couple_halfBody_meta() {
        setScreen(ready(detail = detail(subject = SubjectType.COUPLE, framing = Framing.HALF, steps = FIVE_STEPS)))
        composeRule.onNodeWithText("Cặp đôi · Nữ · Nửa người · 5 bước").assertExists()
    }

    @Test
    fun closeUp_group_meta() {
        setScreen(ready(detail = detail(subject = SubjectType.GROUP, framing = Framing.CLOSE)))
        composeRule.onNodeWithText("Nhóm · Nữ · Cận mặt · 4 bước").assertExists()
    }

    @Test
    fun noCategoryName_leavesTheGroupOut() {
        setScreen(ready(detail = detail(categoryName = null)))
        composeRule.onNodeWithText("Một người · Toàn thân · 4 bước").assertExists()
    }

    @Test
    fun noMistakes_hidesMistakesBlock() {
        setScreen(ready())
        // Control: with a mistake the block is there, so the check below can fail.
        composeRule.onNodeWithText("Lỗi hay gặp").assertExists()
        composeRule.onNodeWithText(STEPS[0]).assertExists()
        state = ready(detail = detail(mistakes = emptyList()))
        composeRule.waitForIdle()
        composeRule.onNodeWithText(STEPS[0]).assertExists()
        composeRule.onNodeWithText("Lỗi hay gặp").assertDoesNotExist()
        composeRule.onNodeWithText(MISTAKE).assertDoesNotExist()
    }

    @Test
    fun severalMistakes_eachInItsOwnText() {
        setScreen(ready(detail = detail(mistakes = listOf(MISTAKE, "Vai trái bị che mất."))))
        composeRule.onAllNodesWithText("Lỗi hay gặp").assertCountEquals(1)
        composeRule.onNodeWithText(MISTAKE).assertExists()
        composeRule.onNodeWithText("Vai trái bị che mất.").assertExists()
    }

    @Test
    @Config(qualifiers = "en-w411dp-h891dp-xxhdpi")
    fun meta_countsStepsInScreenLanguage() {
        // The catalog has 3 steps in Vietnamese and 5 in English: the screen counts the ones it shows.
        val pose = detail(
            name = LocalizedText(mapOf("vi" to "Tựa lan can", "en" to "Lean on the rail")),
            categoryName = LocalizedText(mapOf("vi" to "Nữ", "en" to "Women")),
            stepTexts = LocalizedList(
                mapOf("vi" to STEPS.take(3), "en" to (1..5).map { "English step $it" }),
            ),
        )
        setScreen(ready(detail = pose))
        composeRule.onNodeWithText("Lean on the rail").assertExists()
        composeRule.onNodeWithText("Solo · Women · Full body · 5 steps").assertExists()
        composeRule.onNodeWithText("English step 5").assertExists()
        composeRule.onNodeWithText(STEPS[0]).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "vi-w360dp-h640dp-xxhdpi")
    fun smallScreen_bottomBarAlwaysVisible_lastMistakeScrollsAboveIt() {
        val long = detail(
            steps = FIVE_STEPS,
            mistakes = listOf(MISTAKE, LONG_MISTAKE),
        )
        setScreen(ready(detail = long))
        val screenHeight = composeRule.onRoot().getUnclippedBoundsInRoot().height
        val lastMistake = composeRule.onNodeWithText(LONG_MISTAKE)
        val bar = composeRule.onNodeWithTag(PoseDetailTestTags.BOTTOM_BAR)
        val barBefore = bar.getUnclippedBoundsInRoot()
        assertTrue("bar inside the screen", barBefore.top.value >= 0f && barBefore.bottom <= screenHeight)
        // The content is longer than the room above the bar: the last line starts below the bar before scrolling.
        assertTrue(
            "content longer than the screen",
            lastMistake.getUnclippedBoundsInRoot().bottom > barBefore.top,
        )
        lastMistake.performScrollTo()
        composeRule.waitForIdle()
        val barAfter = bar.getUnclippedBoundsInRoot()
        assertEquals(barBefore.top, barAfter.top)
        assertTrue("bar inside the screen", barAfter.bottom <= screenHeight)
        val last = lastMistake.getUnclippedBoundsInRoot()
        assertTrue(
            "last mistake ${last.bottom} above bar ${barAfter.top}",
            last.bottom.value <= barAfter.top.value + 0.01f,
        )
    }

    // KB-4: buttons

    @Test
    fun shootClick_callsOnShoot() {
        setScreen(ready())
        composeRule.onNodeWithText("Chụp với tư thế này").performClick()
        assertEquals(1, shoots)
    }

    @Test
    fun back_callsOnBack() {
        setScreen(ready())
        composeRule.onNodeWithContentDescription("Quay lại").performClick()
        assertEquals(1, backs)
    }

    // KB-3

    @Test
    fun saveButton_describedBySavedState_andCallsOnToggleSave() {
        setScreen(ready(saved = false))
        composeRule.onNodeWithContentDescription("Bỏ lưu tư thế").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Lưu tư thế").performClick()
        assertEquals(1, toggles)
        state = ready(saved = true)
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Lưu tư thế").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Bỏ lưu tư thế").performClick()
        assertEquals(2, toggles)
    }

    @Test
    fun saveFailed_showsSnackbar_thenCallsShown() {
        composeRule.mainClock.autoAdvance = false
        setScreen(ready(saveFailed = true))
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.onNodeWithText("Chưa lưu được tư thế. Thử lại sau.").assertExists()
        assertEquals(0, saveFailedShown)
        composeRule.mainClock.advanceTimeBy(10_000)
        composeRule.mainClock.advanceTimeBy(1_000)
        assertEquals(1, saveFailedShown)
    }

    // KB-5

    @Test
    fun imageLoading_showsProgress() {
        composeRule.mainClock.autoAdvance = false
        setScreen(ready(image = DetailImageState.Loading))
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNode(
            hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate) and
                hasAnyAncestor(hasTestTag(PoseDetailTestTags.IMAGE)),
        ).assertExists()
        composeRule.onNodeWithText("Không tải được ảnh").assertDoesNotExist()
        // The rest of the screen does not wait for the image.
        composeRule.onNodeWithText(STEPS[0]).assertExists()
        composeRule.onNodeWithText("Chụp với tư thế này").assertExists()
    }

    @Test
    fun imageReady_hasNoProgressNorMessage() {
        setScreen(ready(image = DetailImageState.Ready(IMAGE)))
        composeRule.onNodeWithTag(PoseDetailTestTags.IMAGE).assertExists()
        composeRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertDoesNotExist()
        composeRule.onNodeWithText("Không tải được ảnh").assertDoesNotExist()
    }

    @Test
    fun imageFailed_showsMessageAndRetry_stepsStillShown() {
        setScreen(ready(image = DetailImageState.Failed))
        composeRule.onNodeWithText("Không tải được ảnh").assertExists()
        composeRule.onNodeWithText("Đã có lỗi khi tải ảnh. Thử lại sau ít phút.").assertExists()
        composeRule.onNodeWithText("Thử lại").performClick()
        assertEquals(1, retries)
        composeRule.onNodeWithText(STEPS[0]).assertExists()
        composeRule.onNodeWithText("Chụp với tư thế này").assertExists()
    }

    // KB-6: native bottom bar (plan T-079, section 4). The bar of the framework with a fake 80 dp slot.

    @Test
    fun bottomBar_slotBelowButtonBar_buttonNotCovered() {
        setScreen(ready(), bar = fakeBar())
        val root = composeRule.onRoot().getUnclippedBoundsInRoot()
        val slot = composeRule.onNodeWithTag(SLOT).getUnclippedBoundsInRoot()
        val bar = composeRule.onNodeWithTag(PoseDetailTestTags.BOTTOM_BAR).getUnclippedBoundsInRoot()
        val button = composeRule.onNodeWithText("Chụp với tư thế này").getUnclippedBoundsInRoot()
        assertEquals("slot height", 80f, slot.height.value, 0.5f)
        assertEquals("slot 16 above the bottom", 16f, (root.bottom - slot.bottom).value, 0.5f)
        // Design: the button bar ends at y = 795, where the slot starts (699 + 96).
        assertEquals("slot starts where the bar ends", 0f, (slot.top - bar.bottom).value, 0.5f)
        assertTrue("button $button inside the screen $root", button.top >= root.top && button.bottom <= root.bottom)
        assertTrue("button ${button.bottom} above slot ${slot.top}", button.bottom.value <= slot.top.value + 0.5f)
        composeRule.onNodeWithText("Chụp với tư thế này").performClick()
        assertEquals(1, shoots)
    }

    // Portuguese has the longest main button of the Latin-script languages (plan T-051): it fits on the short screen.
    @Test
    @Config(qualifiers = "pt-rBR-w360dp-h640dp-xxhdpi")
    fun smallScreen_portuguese_buttonFullyVisible() {
        setScreen(ready(detail = detail(steps = FIVE_STEPS, mistakes = listOf(MISTAKE, LONG_MISTAKE))))
        val root = composeRule.onRoot().getUnclippedBoundsInRoot()
        val button = composeRule.onNodeWithText("Fotografar com esta pose").getUnclippedBoundsInRoot()
        assertTrue("button $button inside the screen $root", button.top >= root.top && button.bottom <= root.bottom)
        assertTrue("button $button inside the screen $root", button.left >= root.left && button.right <= root.right)
    }

    // The Portuguese main button stays on one line on the short screen (review note of T-051; plan T-055): a button
    // that wraps still fits the screen, so the line count of its text is read.
    @Test
    @Config(qualifiers = "pt-rBR-w360dp-h640dp-xxhdpi")
    fun smallScreen_portuguese_buttonOneLine() {
        setScreen(ready(detail = detail(steps = FIVE_STEPS, mistakes = listOf(MISTAKE, LONG_MISTAKE))))
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText("Fotografar com esta pose", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals("lines", 1, layouts.single().lineCount)
    }

    @Test
    @Config(qualifiers = "vi-w360dp-h640dp-xxhdpi")
    fun smallScreen_withBottomBar_buttonFullyVisible() {
        val long = detail(steps = FIVE_STEPS, mistakes = listOf(MISTAKE, LONG_MISTAKE))
        var withSlot by mutableStateOf(false)
        setScreen(ready(detail = long), bar = fakeBar(withSlot = { withSlot }))
        val heightWithout = composeRule.onNodeWithText("Chụp với tư thế này").getUnclippedBoundsInRoot().height
        withSlot = true
        composeRule.waitForIdle()
        val root = composeRule.onRoot().getUnclippedBoundsInRoot()
        val slot = composeRule.onNodeWithTag(SLOT).getUnclippedBoundsInRoot()
        val button = composeRule.onNodeWithText("Chụp với tư thế này").getUnclippedBoundsInRoot()
        assertTrue("button $button inside the screen $root", button.top >= root.top && button.bottom <= root.bottom)
        assertTrue("button ${button.bottom} above slot ${slot.top}", button.bottom.value <= slot.top.value + 0.5f)
        assertEquals("button height kept", heightWithout.value, button.height.value, 0.5f)
        val lastMistake = composeRule.onNodeWithText(LONG_MISTAKE)
        lastMistake.performScrollTo()
        composeRule.waitForIdle()
        val bar = composeRule.onNodeWithTag(PoseDetailTestTags.BOTTOM_BAR).getUnclippedBoundsInRoot()
        val last = lastMistake.getUnclippedBoundsInRoot()
        assertTrue("last mistake ${last.bottom} above bar ${bar.top}", last.bottom.value <= bar.top.value + 0.5f)
    }

    @Test
    fun realBottomBar_withoutAds_takesNoSpace() {
        var real by mutableStateOf(true)
        val fake = fakeBar()
        setScreen(
            ready(),
            bar = { screen ->
                if (real) BottomAdBarScaffold(AdPlacement.POSE_DETAIL_BOTTOM) { screen(it) } else fake(screen)
            },
        )
        val root = composeRule.onRoot().getUnclippedBoundsInRoot()
        composeRule.onNodeWithTag(REAL_SLOT).assertDoesNotExist()
        val bar = composeRule.onNodeWithTag(PoseDetailTestTags.BOTTOM_BAR).getUnclippedBoundsInRoot()
        assertEquals("bar reaches the bottom of the screen", root.bottom.value, bar.bottom.value, 0.5f)
        // Control: the same screen in a bar with a slot keeps 96 under the button bar.
        real = false
        composeRule.waitForIdle()
        val barWithSlot = composeRule.onNodeWithTag(PoseDetailTestTags.BOTTOM_BAR).getUnclippedBoundsInRoot()
        assertEquals("control: with a slot", 96f, (root.bottom - barWithSlot.bottom).value, 0.5f)
    }

    // KB-7: unfolded foldable

    @Test
    @Config(qualifiers = UNFOLDED)
    fun wide_imageLeft_infoRight_bottomBarInRightColumn() {
        // Control: the same data at a phone width has the text under the image, so the checks below can fail.
        var width by mutableStateOf(411.dp)
        setScreen(ready(), width = { width })
        val phoneMeta = composeRule.onNodeWithText(META).getUnclippedBoundsInRoot()
        val phoneImage = composeRule.onNodeWithTag(PoseDetailTestTags.IMAGE).getUnclippedBoundsInRoot()
        assertTrue("text under the image on a phone: $phoneMeta $phoneImage", phoneMeta.top >= phoneImage.bottom)
        width = 673.dp
        composeRule.waitForIdle()
        val root = composeRule.onRoot().getUnclippedBoundsInRoot()
        val image = composeRule.onNodeWithTag(PoseDetailTestTags.IMAGE).getUnclippedBoundsInRoot()
        val meta = composeRule.onNodeWithText(META).getUnclippedBoundsInRoot()
        val bar = composeRule.onNodeWithTag(PoseDetailTestTags.BOTTOM_BAR).getUnclippedBoundsInRoot()
        val button = composeRule.onNodeWithText("Chụp với tư thế này").getUnclippedBoundsInRoot()
        assertEquals("image width", 315f, image.width.value, 1f)
        assertEquals("image height", 440f, image.height.value, 1f)
        assertEquals("image left", 16f, image.left.value, 1f)
        assertEquals("image top", 80f, image.top.value, 1f)
        assertEquals("meta left", 347f, meta.left.value, 1f)
        assertTrue("meta beside the image: $meta $image", meta.top < image.bottom && meta.left >= image.right)
        assertEquals("bar left", 347f, bar.left.value, 1f)
        assertEquals("bar right", root.right.value, bar.right.value, 0.5f)
        assertEquals("bar bottom", root.bottom.value, bar.bottom.value, 0.5f)
        assertTrue("button in the right column: $button $bar", button.left >= bar.left && button.right <= bar.right)
        composeRule.onNodeWithText("Chụp với tư thế này").performClick()
        assertEquals(1, shoots)
    }

    @Test
    @Config(qualifiers = UNFOLDED)
    fun wide_infoScrolls_bottomBarStaysVisible() {
        setScreen(ready(detail = longDetail()), width = { 673.dp })
        val root = composeRule.onRoot().getUnclippedBoundsInRoot()
        val bar = composeRule.onNodeWithTag(PoseDetailTestTags.BOTTOM_BAR)
        val barBounds = bar.getUnclippedBoundsInRoot()
        val lastMistake = composeRule.onNodeWithText(LONG_MISTAKE)
        // The text is longer than the room above the bar, so it has to scroll.
        assertTrue(
            "last line below the bar before scrolling",
            lastMistake.getUnclippedBoundsInRoot().bottom > barBounds.top,
        )
        // The text sits in the right column.
        assertEquals(347f, composeRule.onNodeWithText(LONG_META).getUnclippedBoundsInRoot().left.value, 1f)
        lastMistake.performScrollTo()
        composeRule.waitForIdle()
        val last = lastMistake.getUnclippedBoundsInRoot()
        val after = bar.getUnclippedBoundsInRoot()
        assertEquals("bar does not move", barBounds.top.value, after.top.value, 0.5f)
        assertTrue("bar inside the screen", after.bottom <= root.bottom)
        assertTrue("last mistake ${last.bottom} above bar ${after.top}", last.bottom.value <= after.top.value + 0.5f)
    }

    // A large screen in landscape follows the device (spec camera, KB-11; plan T-056): the bar with "Chụp" stays at the
    // bottom of the window, right of the image.
    @Test
    @Config(qualifiers = "vi-w841dp-h673dp-land-xxhdpi")
    fun landscape_bottomBarVisible() {
        setScreen(ready(detail = longDetail()), width = { 841.dp })
        val root = composeRule.onRoot().getUnclippedBoundsInRoot()
        val image = composeRule.onNodeWithTag(PoseDetailTestTags.IMAGE).getUnclippedBoundsInRoot()
        val bar = composeRule.onNodeWithTag(PoseDetailTestTags.BOTTOM_BAR).getUnclippedBoundsInRoot()
        assertEquals("bar at the bottom of the window", root.bottom.value, bar.bottom.value, 0.5f)
        assertTrue("bar top ${bar.top} not above the image top ${image.top}", bar.top >= image.top)
        assertTrue("image ${image.right} left of the bar ${bar.left}", image.right <= bar.left)
    }

    @Test
    @Config(qualifiers = UNFOLDED)
    fun wide_bottomBar_slotFullWidth_buttonAbove() {
        setScreen(ready(), bar = fakeBar(), width = { 673.dp })
        val slot = composeRule.onNodeWithTag(SLOT).getUnclippedBoundsInRoot()
        val bar = composeRule.onNodeWithTag(PoseDetailTestTags.BOTTOM_BAR).getUnclippedBoundsInRoot()
        assertEquals("slot across the screen", 673f - 32f, slot.width.value, 0.5f)
        assertEquals("slot 16 from the left", 16f, slot.left.value, 0.5f)
        assertEquals("bar in the right column", 347f, bar.left.value, 1f)
        assertTrue("bar ${bar.bottom} above slot ${slot.top}", bar.bottom.value <= slot.top.value + 0.5f)
    }

    @Test
    @Config(qualifiers = UNFOLDED_LOW)
    fun widthChange_keepsPoseAndScroll() {
        var width by mutableStateOf(673.dp)
        setScreen(ready(detail = longDetail()), width = { width })
        val wideMetaTop = composeRule.onNodeWithText(LONG_META).getUnclippedBoundsInRoot().top
        assertEquals("wide layout: text in the right column", 347f, metaLeft(), 1f)
        width = 411.dp
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Tựa lan can").assertExists()
        assertTrue("phone layout: text under the image", metaLeft() < 100f)
        composeRule.onNodeWithText(LONG_MISTAKE).performScrollTo()
        composeRule.waitForIdle()
        assertTrue("scrolled: image is above the screen", imageTop() < 0f)
        width = 673.dp
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Tựa lan can").assertExists()
        assertEquals("wide layout again: text in the right column", 347f, metaLeft(), 1f)
        val metaTop = composeRule.onNodeWithText(LONG_META).getUnclippedBoundsInRoot().top
        assertTrue("scroll kept: text moved up from $wideMetaTop to $metaTop", metaTop.value < wideMetaTop.value - 1f)
        width = 411.dp
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Tựa lan can").assertExists()
        assertTrue("scroll kept after folding again", imageTop() < 0f)
    }

    private fun longDetail() = detail(steps = MANY_STEPS, mistakes = listOf(MISTAKE, LONG_MISTAKE))

    private fun metaLeft() = composeRule.onNodeWithText(LONG_META).getUnclippedBoundsInRoot().left.value

    private fun imageTop() = composeRule.onNodeWithTag(PoseDetailTestTags.IMAGE).getUnclippedBoundsInRoot().top.value

    @Test
    @Config(qualifiers = UNFOLDED)
    fun wide_imageFailed_retryInLeftColumn() {
        setScreen(ready(image = DetailImageState.Failed), width = { 673.dp })
        val image = composeRule.onNodeWithTag(PoseDetailTestTags.IMAGE).getUnclippedBoundsInRoot()
        assertEquals("image area width", 315f, image.width.value, 1f)
        listOf("Không tải được ảnh", "Thử lại").forEach { text ->
            val bounds = composeRule.onNodeWithText(text).getUnclippedBoundsInRoot()
            assertTrue(
                "$text $bounds inside the image area $image",
                bounds.left >= image.left &&
                    bounds.right <= image.right &&
                    bounds.top >= image.top &&
                    bounds.bottom <= image.bottom,
            )
        }
        composeRule.onNodeWithText("Thử lại").performClick()
        assertEquals(1, retries)
        composeRule.onNodeWithText(STEPS[0]).assertExists()
    }

    private fun ready(
        detail: PoseDetail = detail(),
        saved: Boolean = false,
        image: DetailImageState = DetailImageState.Ready(IMAGE),
        saveFailed: Boolean = false,
    ) = PoseDetailUiState.Ready(pose = detail, saved = saved, image = image, saveFailed = saveFailed)

    private fun detail(
        subject: SubjectType = SubjectType.SINGLE,
        framing: Framing = Framing.FULL,
        steps: List<String> = STEPS,
        mistakes: List<String> = listOf(MISTAKE),
        name: LocalizedText = LocalizedText(mapOf("vi" to "Tựa lan can")),
        categoryName: LocalizedText? = LocalizedText(mapOf("vi" to "Nữ")),
        stepTexts: LocalizedList = LocalizedList(mapOf("vi" to steps)),
    ) = PoseDetail(
        id = "w01",
        categoryId = "women",
        name = name,
        categoryName = categoryName,
        subjectType = subject,
        framing = framing,
        steps = stepTexts,
        mistakes = LocalizedList(if (mistakes.isEmpty()) emptyMap() else mapOf("vi" to mistakes)),
    )

    private companion object {
        const val UNFOLDED = "vi-w673dp-h841dp-xxhdpi"
        const val UNFOLDED_LOW = "vi-w673dp-h641dp-xxhdpi"
        const val META = "Một người · Nữ · Toàn thân · 4 bước"
        const val LONG_META = "Một người · Nữ · Toàn thân · 12 bước"
        const val SLOT = "fake_ad_slot"

        // The tag of the real slot of the framework (BottomAdBarTestTags, internal to :core:ads).
        const val REAL_SLOT = "bottom_ad_bar_slot"

        val IMAGE = ImageBitmap(379, 380)
        val STEPS = listOf(
            "Đứng chéo 45° so với máy, chân trước hơi khuỵu.",
            "Tay trái chống hông, khuỷu tay mở ra sau.",
            "Tay phải thả lỏng dọc thân.",
            "Xoay vai về phía máy, cằm hơi hạ.",
        )
        val MANY_STEPS = STEPS + (5..12).map { "Bước $it: giữ tư thế, thả lỏng vai và hít thở đều trong vài giây." }
        val FIVE_STEPS = STEPS + "Nhìn qua vai về phía ống kính, mắt mở tự nhiên và thả lỏng cổ."
        const val LONG_MISTAKE = "Vai trái bị che mất khi hai người đứng sát nhau và cùng xoay về một phía."
        const val MISTAKE = "Rút vai lên sát tai làm cổ ngắn lại. Thả vai xuống trước khi chụp."
    }
}

/**
 * The bottom bar of the framework with a fake 80 dp slot (plan T-079, section 4): the slot takes the modifier the bar
 * gives, so it is as wide as the screen less 2 x 16. [withSlot] false is a bar without an ad: no slot, nothing kept.
 */
private fun fakeBar(
    withSlot: () -> Boolean = { true },
): @Composable (screen: @Composable (PaddingValues) -> Unit) -> Unit = { screen ->
    val shown = withSlot()
    BottomAdBarLayout(
        reservation = if (shown) BottomAdBarDefaults.nativeReservation else 0.dp,
        slot = if (shown) {
            { Box(it.height(80.dp).testTag("fake_ad_slot")) }
        } else {
            null
        },
        layer = null,
        content = screen,
    )
}
