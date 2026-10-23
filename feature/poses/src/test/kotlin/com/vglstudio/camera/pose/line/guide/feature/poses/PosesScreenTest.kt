package com.vglstudio.camera.pose.line.guide.feature.poses

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.core.poses.LoadFailureReason
import com.vglstudio.camera.pose.line.guide.core.poses.model.LocalizedList
import com.vglstudio.camera.pose.line.guide.core.poses.model.Pose
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The pose library screen (spec poses, KB-1, KB-2, KB-3, KB-5; plan T-021, section 4). Native graphics: sizes are
 * measured and real bitmaps are drawn. The screen language is Vietnamese, as the text table.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "vi-w411dp-h891dp-xxhdpi")
class PosesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var backs = 0
    private var settingsOpened = 0
    private val opened = mutableListOf<String>()
    private val filters = mutableListOf<PoseFilter>()
    private var showAll = 0
    private val toggles = mutableListOf<String>()
    private var retries = 0
    private val needed = mutableListOf<String>()
    private val released = mutableListOf<String>()
    private val thumbnailRetries = mutableListOf<String>()
    private var saveFailedShown = 0

    private var state by mutableStateOf<PosesUiState>(PosesUiState.Loading)

    private fun setScreen(
        initial: PosesUiState,
        gridState: LazyGridState? = null,
        onOpenSettings: (() -> Unit)? = { settingsOpened++ },
        onOpenPose: ((String) -> Unit)? = { opened += it },
    ) {
        state = initial
        composeRule.setContent {
            AppTheme(darkTheme = false) {
                PosesScreen(
                    uiState = state,
                    gridState = gridState ?: rememberLazyGridState(),
                    onBack = { backs++ },
                    onOpenSettings = onOpenSettings,
                    onOpenPose = onOpenPose,
                    onSelectFilter = { filters += it },
                    onShowAll = { showAll++ },
                    onToggleSave = { toggles += it },
                    onRetry = { retries++ },
                    onThumbnailNeeded = { needed += it },
                    onThumbnailReleased = { released += it },
                    onRetryThumbnail = { thumbnailRetries += it },
                    onSaveFailedShown = { saveFailedShown++ },
                )
            }
        }
    }

    private fun card(id: String) = composeRule.onNodeWithTag(PosesTestTags.card(id))

    private fun saveButtonOf(id: String, description: String) = composeRule.onNode(
        hasContentDescription(description) and hasAnyAncestor(hasTestTag(PosesTestTags.card(id))),
        useUnmergedTree = true,
    )

    // Flag

    @Test
    fun disabled_callsOnBackOnce() {
        setScreen(PosesUiState.Disabled)
        composeRule.waitForIdle()
        state = PosesUiState.Disabled
        composeRule.waitForIdle()
        assertEquals(1, backs)
    }

    // KB-1

    @Test
    fun grid_hasTwoColumns() {
        setScreen(ready())
        val first = card("w01").getUnclippedBoundsInRoot()
        val second = card("w02").getUnclippedBoundsInRoot()
        val third = card("w03").getUnclippedBoundsInRoot()
        assertEquals(first.top, second.top)
        assertTrue("second card is right of the first", second.left > first.right)
        assertTrue("third card is under the first", third.top > first.bottom)
        assertEquals(first.left, third.left)
    }

    @Test
    fun ready_showsTitleSummaryChipsAndCards() {
        setScreen(ready())
        composeRule.onNodeWithText("Thư viện tư thế").assertExists()
        composeRule.onNodeWithText("7 tư thế trong 3 nhóm").assertExists()
        listOf("Tất cả", "Đã lưu", "Nữ", "Nam", "Cặp đôi").forEach {
            composeRule.onNodeWithText(it).assertExists()
        }
        card("w01").assertExists()
    }

    @Test
    fun card_showsNameMetaAndSaveButton() {
        setScreen(ready())
        composeRule.onNodeWithText("Tựa lan can").assertExists()
        // w03 has the same second line: check the one of w01.
        card("w01").assert(hasText("Một người · 4 bước"))
        saveButtonOf("w01", "Lưu tư thế").assertExists()
    }

    @Test
    fun card_couple_showsCoupleLabel() {
        setScreen(ready())
        composeRule.onNodeWithText("Cặp đôi · 5 bước").assertExists()
    }

    @Test
    fun card_group_showsGroupLabel() {
        setScreen(ready())
        composeRule.onNodeWithText("Nhóm · 3 bước").assertExists()
    }

    @Test
    @Config(qualifiers = "en-w411dp-h891dp-xxhdpi")
    fun card_nameUsesScreenLocale() {
        setScreen(ready())
        composeRule.onNodeWithText("Lean on the rail").assertExists()
        composeRule.onNodeWithText("Tựa lan can").assertDoesNotExist()
        composeRule.onNodeWithText("Women").assertExists()
    }

    @Test
    fun card_stepCountUsesScreenLanguage() {
        // English first in the catalog: the Vietnamese screen counts the Vietnamese steps (review of T-021).
        setScreen(ready(cards = listOf(twoLanguageCard(LocalizedList(mapOf("en" to steps(5), "vi" to steps(3)))))))
        card("x01").assert(hasText("Một người · 3 bước"))
    }

    @Test
    @Config(qualifiers = "en-w411dp-h891dp-xxhdpi")
    fun card_stepCountUsesScreenLanguage_english() {
        setScreen(ready(cards = listOf(twoLanguageCard(LocalizedList(mapOf("vi" to steps(3), "en" to steps(5)))))))
        card("x01").assert(hasText("Một người · 5 bước"))
    }

    @Test
    fun back_callsOnBack() {
        setScreen(ready())
        composeRule.onNodeWithContentDescription("Quay lại").performClick()
        assertEquals(1, backs)
    }

    @Test
    fun settings_callsOnOpenSettings() {
        setScreen(ready())
        composeRule.onNodeWithContentDescription("Cài đặt").performClick()
        assertEquals(1, settingsOpened)
    }

    @Test
    fun settings_hiddenWhenNull() {
        setScreen(ready(), onOpenSettings = null)
        composeRule.onNodeWithContentDescription("Cài đặt").assertDoesNotExist()
    }

    @Test
    fun thumbnailLoading_showsSkeleton_thumbnailReady_showsCard() {
        setScreen(ready(thumbnails = mapOf("w01" to ThumbnailState.Loading)))
        card("w01").assertExists()
        composeRule.onNodeWithText("Tựa lan can").assertDoesNotExist()
        state = ready()
        composeRule.onNodeWithText("Tựa lan can").assertExists()
    }

    @Test
    fun skeletonThenImage_keepsCardBounds() {
        setScreen(ready(thumbnails = mapOf("w03" to ThumbnailState.Loading)))
        val skeleton = card("w03").getUnclippedBoundsInRoot()
        state = ready()
        val loaded = card("w03").getUnclippedBoundsInRoot()
        assertEquals(skeleton, loaded)
        assertTrue("card has a size", loaded.bottom - loaded.top > (loaded.right - loaded.left))
    }

    // KB-2

    @Test
    fun selectedChip_isSelected() {
        val men = fixtureCards().filter { it.categoryId == "men" }
        setScreen(ready(selected = PoseFilter.Category("men"), cards = men))
        composeRule.onNodeWithText("Nam").assertIsSelected()
        composeRule.onNodeWithText("Tất cả").assertIsNotSelected()
    }

    @Test
    fun chipClick_callsOnSelectFilter_andScrollsToTop() {
        val gridState = LazyGridState()
        setScreen(ready(cards = manyCards()), gridState = gridState)
        composeRule.onNodeWithTag(PosesTestTags.GRID).performScrollToIndex(6)
        composeRule.runOnIdle { assertEquals(6, gridState.firstVisibleItemIndex) }
        composeRule.onNodeWithText("Nam").performClick()
        composeRule.waitForIdle()
        assertEquals(listOf<PoseFilter>(PoseFilter.Category("men")), filters)
        assertEquals(0, gridState.firstVisibleItemIndex)
    }

    @Test
    fun chipClick_scrollsToTop_alsoWhenFirstCardMovesDownInNewList() {
        // "Nữ" scrolled down; its first card is in the middle of "Tất cả": the grid must still start at the top.
        val women = manyCards()
        val gridState = LazyGridState()
        setScreen(ready(selected = PoseFilter.Category("women"), cards = women), gridState = gridState)
        composeRule.onNodeWithTag(PosesTestTags.GRID).performScrollToIndex(6)
        composeRule.runOnIdle { assertEquals(6, gridState.firstVisibleItemIndex) }
        composeRule.onNodeWithText("Tất cả").performClick()
        composeRule.waitForIdle()
        val men = (1..6).map {
            item(testPose(id = "m%02d".format(it), categoryId = "men"), false, ThumbnailState.Ready(IMAGE))
        }
        state = ready(selected = PoseFilter.All, cards = men + women)
        composeRule.waitForIdle()
        assertEquals(listOf<PoseFilter>(PoseFilter.All), filters)
        assertEquals(0, gridState.firstVisibleItemIndex)
        card("m01").assertExists()
    }

    @Test
    fun scrollPosition_survivesStateRestore() {
        val restorationTester = StateRestorationTester(composeRule)
        var gridState: LazyGridState? = null
        state = ready(cards = manyCards())
        restorationTester.setContent {
            AppTheme(darkTheme = false) {
                // As PosesRoute: the grid state is remembered with saved state.
                val remembered = rememberLazyGridState()
                gridState = remembered
                PosesScreen(
                    uiState = state,
                    gridState = remembered,
                    onBack = {},
                    onOpenSettings = null,
                    onOpenPose = null,
                    onSelectFilter = {},
                    onShowAll = {},
                    onToggleSave = {},
                    onRetry = {},
                    onThumbnailNeeded = {},
                    onThumbnailReleased = {},
                    onRetryThumbnail = {},
                    onSaveFailedShown = {},
                )
            }
        }
        composeRule.onNodeWithTag(PosesTestTags.GRID).performScrollToIndex(6)
        composeRule.runOnIdle { assertEquals(6, gridState!!.firstVisibleItemIndex) }
        gridState = null
        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.runOnIdle { assertEquals(6, gridState!!.firstVisibleItemIndex) }
    }

    // KB-3

    @Test
    fun saveClick_callsOnToggleSave_notOnOpenPose() {
        setScreen(ready())
        saveButtonOf("w01", "Lưu tư thế").performClick()
        assertEquals(listOf("w01"), toggles)
        assertTrue(opened.isEmpty())
    }

    @Test
    fun savedCard_saveButtonDescribedAsUnsave() {
        setScreen(ready())
        saveButtonOf("w02", "Bỏ lưu tư thế").assertExists()
        saveButtonOf("w02", "Lưu tư thế").assertDoesNotExist()
    }

    @Test
    fun savedEmpty_showsTexts_andShowAllCalls() {
        setScreen(ready(selected = PoseFilter.Saved, cards = emptyList()))
        composeRule.onNodeWithText("Chưa lưu tư thế nào").assertExists()
        composeRule.onNodeWithText("Chạm biểu tượng lưu trên thẻ tư thế để giữ lại ở đây.").assertExists()
        composeRule.onNodeWithText("7 tư thế trong 3 nhóm").assertExists()
        composeRule.onNodeWithText("Đã lưu").assertIsSelected()
        composeRule.onNodeWithText("Xem tất cả tư thế").performClick()
        assertEquals(1, showAll)
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
    fun visibleCards_requestThumbnails_andScrolledAwayCardsRelease() {
        setScreen(ready(cards = manyCards(thumbnail = ThumbnailState.Loading)))
        composeRule.waitForIdle()
        assertTrue("needed $needed", needed.containsAll(listOf("p01", "p02", "p03", "p04")))
        assertTrue("needed $needed", "p12" !in needed)
        assertTrue(released.isEmpty())
        composeRule.onNodeWithTag(PosesTestTags.GRID).performScrollToIndex(11)
        composeRule.waitForIdle()
        assertTrue("needed $needed", "p12" in needed)
        assertTrue("released $released", released.containsAll(listOf("p01", "p02")))
    }

    @Test
    fun failedThumbnail_tapSkeleton_callsRetryThumbnail() {
        setScreen(ready(thumbnails = mapOf("w01" to ThumbnailState.Failed)))
        card("w01").performClick()
        assertEquals(listOf("w01"), thumbnailRetries)
        assertTrue(opened.isEmpty())
    }

    @Test
    fun error_showsTitleMessageRetry_andRetryCalls() {
        setScreen(PosesUiState.Error(LoadFailureReason.OTHER))
        composeRule.onNodeWithText("Thư viện tư thế").assertExists()
        composeRule.onNodeWithText("Không tải được thư viện").assertExists()
        composeRule.onNodeWithText("Đã có lỗi khi tải thư viện. Thử lại sau ít phút.").assertExists()
        composeRule.onNodeWithText("Thử lại").performClick()
        assertEquals(1, retries)
    }

    // For T-022

    @Test
    fun cardClick_callsOnOpenPose() {
        setScreen(ready())
        card("w01").performClick()
        assertEquals(listOf("w01"), opened)
    }

    @Test
    fun cardClick_withoutOnOpenPose_doesNothing() {
        setScreen(ready(), onOpenPose = null)
        card("w01").assertExists().assertHasNoClickAction()
    }

    // Short screen (L-028)

    @Test
    @Config(qualifiers = "vi-w360dp-h640dp-xxhdpi")
    fun smallScreen_gridScrollsToLastCard() {
        setScreen(ready())
        card("c02").assertDoesNotExist()
        composeRule.onNodeWithTag(PosesTestTags.GRID).performScrollToIndex(6)
        card("c02").assertExists()
    }

    @Test
    @Config(qualifiers = "vi-w360dp-h640dp-xxhdpi")
    fun smallScreen_savedEmptyButtonFullyVisible() {
        setScreen(ready(selected = PoseFilter.Saved, cards = emptyList()))
        val root = composeRule.onRoot().getUnclippedBoundsInRoot()
        val button = composeRule.onNodeWithText("Xem tất cả tư thế").getUnclippedBoundsInRoot()
        assertTrue("button $button in $root", button.top >= root.top && button.bottom <= root.bottom)
        assertTrue("button $button in $root", button.left >= root.left && button.right <= root.right)
    }

    private companion object {
        val IMAGE = ImageBitmap(162, 210)

        fun item(pose: Pose, saved: Boolean, thumbnail: ThumbnailState) = PoseCardItem(
            id = pose.id,
            categoryId = pose.categoryId,
            name = pose.name,
            subjectType = pose.subjectType,
            steps = pose.steps,
            saved = saved,
            thumbnail = thumbnail,
        )

        /** The fixture catalog in catalog order, w02 saved, every image ready unless set in [thumbnails]. */
        fun fixtureCards(thumbnails: Map<String, ThumbnailState> = emptyMap()) = PosesFixtures.POSES.map { pose ->
            item(pose, saved = pose.id == "w02", thumbnail = thumbnails[pose.id] ?: ThumbnailState.Ready(IMAGE))
        }

        fun steps(count: Int) = (1..count).map { "Step $it" }

        /** One card whose steps differ by language. */
        fun twoLanguageCard(steps: LocalizedList) =
            item(testPose(id = "x01"), saved = false, thumbnail = ThumbnailState.Ready(IMAGE)).copy(steps = steps)

        /** Twelve cards p01 to p12, enough to scroll. */
        fun manyCards(thumbnail: ThumbnailState = ThumbnailState.Ready(IMAGE)) = (1..12).map {
            item(testPose(id = "p%02d".format(it)), saved = false, thumbnail = thumbnail)
        }

        fun ready(
            selected: PoseFilter = PoseFilter.All,
            cards: List<PoseCardItem>? = null,
            thumbnails: Map<String, ThumbnailState> = emptyMap(),
            saveFailed: Boolean = false,
        ) = PosesUiState.Ready(
            totalPoses = 7,
            chips = listOf(PoseChip.All, PoseChip.Saved) + listOf(
                PosesFixtures.WOMEN,
                PosesFixtures.MEN,
                PosesFixtures.COUPLES,
            ).map { PoseChip.Category(it.id, it.name) },
            selected = selected,
            cards = cards ?: fixtureCards(thumbnails),
            saveFailed = saveFailed,
        )
    }
}
