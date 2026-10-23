package com.vglstudio.camera.pose.line.guide.feature.poses.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.semantics.ProgressBarRangeInfo
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
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
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

    private fun setScreen(initial: PoseDetailUiState) {
        state = initial
        composeRule.setContent {
            AppTheme(darkTheme = false) {
                PoseDetailScreen(
                    uiState = state,
                    onBack = { backs++ },
                    onToggleSave = { toggles++ },
                    onRetryImage = { retries++ },
                    onShoot = { shoots++ },
                    onSaveFailedShown = { saveFailedShown++ },
                )
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
        composeRule.onNodeWithText("Một người · Women · Toàn thân · 5 bước").assertExists()
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
        val IMAGE = ImageBitmap(379, 380)
        val STEPS = listOf(
            "Đứng chéo 45° so với máy, chân trước hơi khuỵu.",
            "Tay trái chống hông, khuỷu tay mở ra sau.",
            "Tay phải thả lỏng dọc thân.",
            "Xoay vai về phía máy, cằm hơi hạ.",
        )
        val FIVE_STEPS = STEPS + "Nhìn qua vai về phía ống kính, mắt mở tự nhiên và thả lỏng cổ."
        const val LONG_MISTAKE = "Vai trái bị che mất khi hai người đứng sát nhau và cùng xoay về một phía."
        const val MISTAKE = "Rút vai lên sát tai làm cổ ngắn lại. Thả vai xuống trước khi chụp."
    }
}
