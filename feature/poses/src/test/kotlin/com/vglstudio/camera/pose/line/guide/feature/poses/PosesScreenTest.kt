package com.vglstudio.camera.pose.line.guide.feature.poses

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
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
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.ads.AdPlacement
import com.vglstudio.camera.pose.line.guide.core.ads.BottomAdBarDefaults
import com.vglstudio.camera.pose.line.guide.core.ads.BottomAdBarLayout
import com.vglstudio.camera.pose.line.guide.core.ads.BottomAdBarScaffold
import com.vglstudio.camera.pose.line.guide.core.ads.rememberInListAds
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.POSE_CARD_IMAGE_TAG
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PREMIUM_BADGE_TAG
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PoseCardDefaults
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
        bar: (@Composable (screen: @Composable (PaddingValues) -> Unit) -> Unit)? = null,
        width: (() -> Dp)? = null,
        gridAds: @Composable () -> PosesGridAds = { PosesGridAds.None },
        probe: @Composable () -> Unit = {},
    ) {
        state = initial
        composeRule.setContent {
            AppTheme(darkTheme = false) {
                val content = @Composable { contentPadding: PaddingValues ->
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
                        contentPadding = contentPadding,
                        gridAds = gridAds(),
                    )
                }
                probe()
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
        card("x01").assert(hasText("Solo · 5 steps"))
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

    // KB-7: unfolded foldable

    @Test
    @Config(qualifiers = UNFOLDED)
    fun wide_hasThreeColumns() {
        setScreen(ready(cards = manyCards()))
        val cards = listOf("p01", "p02", "p03", "p04").map { card(it).getUnclippedBoundsInRoot() }
        val (first, second, third, fourth) = cards
        assertEquals(first.top, second.top)
        assertEquals(first.top, third.top)
        assertTrue("cards go to the right: $cards", first.left < second.left && second.left < third.left)
        assertTrue("fourth card under the first: $cards", fourth.top > first.bottom)
        assertEquals(first.left, fourth.left)
        cards.take(3).forEach { assertEquals("card width of $it", 203f, it.width.value, 1f) }
    }

    // A large screen in landscape follows the device (spec poses, KB-7; spec camera, KB-11; plan T-056).
    @Test
    @Config(qualifiers = "vi-w841dp-h673dp-land-xxhdpi")
    fun landscape_hasThreeColumns() {
        setScreen(ready(cards = manyCards()))
        val cards = listOf("p01", "p02", "p03", "p04").map { card(it).getUnclippedBoundsInRoot() }
        val (first, second, third, fourth) = cards
        assertEquals(first.top, second.top)
        assertEquals(first.top, third.top)
        assertTrue("cards go to the right: $cards", first.left < second.left && second.left < third.left)
        assertTrue("fourth card under the first: $cards", fourth.top > first.bottom)
        assertEquals(first.left, fourth.left)
    }

    @Test
    @Config(qualifiers = UNFOLDED)
    fun wideLoading_hasThreeColumns() {
        // Control: on a phone width the skeleton grid has two columns, so the check below can fail.
        var width by mutableStateOf(411.dp)
        setScreen(PosesUiState.Loading, width = { width })
        assertSkeletonColumns(2)
        width = 673.dp
        composeRule.waitForIdle()
        assertSkeletonColumns(3)
    }

    private fun assertSkeletonColumns(columns: Int) {
        val images = composeRule.onAllNodesWithTag(POSE_CARD_IMAGE_TAG, useUnmergedTree = true)
        // Tall skeleton cards: only the rows that fit are composed, at least the first row and one card of the next.
        val shown = images.fetchSemanticsNodes().size
        assertTrue("skeletons shown: $shown", shown > columns)
        val bounds = (0 until shown).map { images[it].getUnclippedBoundsInRoot() }
        assertEquals("first row: $bounds", columns, bounds.count { it.top == bounds[0].top })
        assertTrue("next row is lower: $bounds", bounds[columns].top > bounds[0].bottom)
        assertEquals(bounds[0].left, bounds[columns].left)
    }

    @Test
    @Config(qualifiers = UNFOLDED)
    fun width599_isTwoColumns_width600_isThreeColumns() {
        var width by mutableStateOf(599.dp)
        setScreen(ready(cards = manyCards()), width = { width })
        assertColumns(2, "p01")
        width = 600.dp
        composeRule.waitForIdle()
        assertColumns(3, "p01")
        width = 599.dp
        composeRule.waitForIdle()
        assertColumns(2, "p01")
    }

    @Test
    @Config(qualifiers = UNFOLDED)
    fun widthChange_keepsScrollAndSelectedChip() {
        var width by mutableStateOf(411.dp)
        val gridState = LazyGridState()
        setScreen(
            // Thirty cards: enough lines in three columns to keep the scroll position without clamping.
            ready(selected = PoseFilter.Category("men"), cards = manyCards(count = 30)),
            gridState = gridState,
            width = { width },
        )
        composeRule.onNodeWithText("Nam").assertIsSelected()
        composeRule.onNodeWithTag(PosesTestTags.GRID).performScrollToIndex(6)
        composeRule.runOnIdle { assertEquals(6, gridState.firstVisibleItemIndex) }
        assertColumns(2, "p07")
        width = 673.dp
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertEquals(6, gridState.firstVisibleItemIndex) }
        assertColumns(3, "p07")
        composeRule.onNodeWithText("Nam").assertIsSelected()
        width = 411.dp
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertEquals(6, gridState.firstVisibleItemIndex) }
        assertColumns(2, "p07")
        composeRule.onNodeWithText("Nam").assertIsSelected()
    }

    /** The row of [first] holds [columns] cards: the cards after it share its top, and the one after the row is lower. */
    private fun assertColumns(columns: Int, first: String) {
        val start = first.removePrefix("p").toInt()
        val bounds = (start..start + columns).map { card("p%02d".format(it)).getUnclippedBoundsInRoot() }
        val row = bounds.take(columns)
        assertEquals("one row of $columns cards: $bounds", 1, row.map { it.top }.toSet().size)
        assertTrue("cards go to the right: $bounds", row.zipWithNext().all { (a, b) -> a.right <= b.left })
        assertTrue("next card in a lower row: $bounds", bounds[columns].top > row[0].bottom)
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

    // KB-8: premium poses (plan T-086)

    @Test
    fun lockedCard_showsPremiumBadge_andOpensDetail() {
        setScreen(ready(cards = fixtureCards().map { if (it.id == "w03") it.copy(locked = true) else it }))
        val badges = composeRule.onAllNodesWithTag(PREMIUM_BADGE_TAG, useUnmergedTree = true)
        badges.assertCountEquals(1)
        composeRule.onNode(
            hasTestTag(PREMIUM_BADGE_TAG) and hasAnyAncestor(hasTestTag(PosesTestTags.card("w03"))),
            useUnmergedTree = true,
        ).assertExists()
        // A tap on a locked card still opens the detail (spec poses, question 7).
        card("w03").performClick()
        assertEquals(listOf("w03"), opened)
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

    // KB-6: native bottom bar (plan T-079, section 4). The bar of the framework with a fake 80 dp slot.

    @Test
    fun bottomBar_lastCardScrollsAboveSlot() {
        var passPadding by mutableStateOf(false)
        setScreen(ready(cards = manyCards()), bar = fakeBar(passPadding = { passPadding }))
        // Control: the same bar, its padding not given to the screen: the last card ends behind the slot.
        scrollToLastCard()
        val slotBefore = composeRule.onNodeWithTag(SLOT).getUnclippedBoundsInRoot()
        val lastBefore = card("p12").getUnclippedBoundsInRoot()
        assertTrue(
            "control: last card ${lastBefore.bottom} behind slot ${slotBefore.top}",
            lastBefore.bottom > slotBefore.top,
        )
        passPadding = true
        composeRule.waitForIdle()
        scrollToLastCard()
        val root = composeRule.onRoot().getUnclippedBoundsInRoot()
        val slot = composeRule.onNodeWithTag(SLOT).getUnclippedBoundsInRoot()
        val last = card("p12").getUnclippedBoundsInRoot()
        assertEquals("slot height", SLOT_HEIGHT, slot.height.value, 0.5f)
        assertEquals("slot 16 above the bottom", 16f, (root.bottom - slot.bottom).value, 0.5f)
        assertTrue("last card ${last.bottom} above slot ${slot.top}", last.bottom.value <= slot.top.value + 0.5f)
    }

    @Test
    @Config(qualifiers = "vi-w360dp-h640dp-xxhdpi")
    fun smallScreen_bottomBar_onErrorAndSavedEmpty_actionAboveSlot() {
        var passPadding by mutableStateOf(false)
        setScreen(PosesUiState.Error(LoadFailureReason.OTHER), bar = fakeBar(passPadding = { passPadding }))
        listOf(
            PosesUiState.Error(LoadFailureReason.OTHER) to "Thử lại",
            ready(selected = PoseFilter.Saved, cards = emptyList()) to "Xem tất cả tư thế",
        ).forEach { (screenState, action) ->
            state = screenState
            passPadding = false
            composeRule.waitForIdle()
            val centerWithout = centerY(action)
            passPadding = true
            composeRule.waitForIdle()
            val slot = composeRule.onNodeWithTag(SLOT).assertExists().getUnclippedBoundsInRoot()
            val button = composeRule.onNodeWithText(action).getUnclippedBoundsInRoot()
            assertTrue("$action ${button.bottom} above slot ${slot.top}", button.bottom.value <= slot.top.value + 0.5f)
            assertEquals("$action moves up half of 96", 48f, centerWithout - centerY(action), 1f)
            // Control: without the padding the button is back where it was.
            passPadding = false
            composeRule.waitForIdle()
            assertEquals("control: $action without padding", 0f, centerWithout - centerY(action), 1f)
        }
    }

    @Test
    fun saveFailedSnackbar_aboveSlot() {
        composeRule.mainClock.autoAdvance = false
        setScreen(ready(saveFailed = true), bar = fakeBar())
        composeRule.mainClock.advanceTimeBy(500)
        val slot = composeRule.onNodeWithTag(SLOT).getUnclippedBoundsInRoot()
        val snackbar = composeRule.onNodeWithText("Chưa lưu được tư thế. Thử lại sau.").getUnclippedBoundsInRoot()
        assertTrue("snackbar ${snackbar.bottom} above slot ${slot.top}", snackbar.bottom.value <= slot.top.value + 0.5f)
    }

    @Test
    fun realBottomBar_withoutAds_takesNoSpace() {
        var real by mutableStateOf(true)
        val fake = fakeBar()
        setScreen(
            ready(cards = manyCards()),
            bar = { screen ->
                if (real) BottomAdBarScaffold(AdPlacement.POSES_BOTTOM) { screen(it) } else fake(screen)
            },
        )
        scrollToLastCard()
        val root = composeRule.onRoot().getUnclippedBoundsInRoot()
        composeRule.onNodeWithTag(REAL_SLOT).assertDoesNotExist()
        assertEquals("last cards 16 above the bottom", 16f, root.bottom.value - lastRowBottom(), 0.5f)
        // Control: the same screen in a bar with a slot keeps 16 + 96 under the last card.
        real = false
        composeRule.waitForIdle()
        scrollToLastCard()
        assertEquals("control: with a slot", 112f, root.bottom.value - lastRowBottom(), 0.5f)
    }

    // Ad cells of the grid (spec poses KB-6, criteria 5 and 6; plan T-080, section 4)

    /** A fixed ad cell after every 8 poses: a box of the height the screen gives it. */
    private val fixedGridAds = PosesGridAds.Fixed(every = 8) { modifier, cardHeight ->
        Box(modifier.fillMaxWidth().height(cardHeight))
    }

    private fun adCell() = composeRule.onNodeWithTag(PosesTestTags.AD_CELL)

    /**
     * Shows [cards] with the fixed ad cells, scrolls the cell into view, and returns the height of a one-line card as
     * wide as the cell, computed in the composition of the screen ([PoseCardDefaults.oneLineHeight]).
     */
    private fun showFixedAdsAndOneLineHeight(cards: List<PoseCardItem> = manyCards()): Float {
        var probeWidth by mutableStateOf<Dp?>(null)
        var oneLine: Dp? = null
        setScreen(
            ready(cards = cards),
            gridAds = { fixedGridAds },
            probe = { probeWidth?.let { oneLine = PoseCardDefaults.oneLineHeight(it) } },
        )
        composeRule.onNodeWithTag(PosesTestTags.GRID).performScrollToIndex(AD_INDEX)
        composeRule.waitForIdle()
        probeWidth = adCell().getUnclippedBoundsInRoot().width
        composeRule.waitForIdle()
        return oneLine!!.value
    }

    @Test
    fun fixedGridAds_adCellAfterEighthPose() {
        val oneLine = showFixedAdsAndOneLineHeight()

        composeRule.onAllNodesWithTag(PosesTestTags.AD_CELL).assertCountEquals(1)
        val ad = adCell().getUnclippedBoundsInRoot()
        val seventh = card("p07").getUnclippedBoundsInRoot()
        assertEquals("ad in the left column", seventh.left.value, ad.left.value, 0.5f)
        assertEquals("ad beside card 9", card("p09").getUnclippedBoundsInRoot().top.value, ad.top.value, 0.5f)
        assertEquals("ad height, a one-line card of its width", oneLine, ad.height.value, 0.5f)
        // Same column: card 7 has the same width (the two columns can differ by a pixel).
        assertEquals("ad height, card 7 above it", seventh.height.value, ad.height.value, 0.5f)
    }

    @Test
    @Config(qualifiers = "vi-w360dp-h640dp-xxhdpi")
    fun fixedGridAds_small_cellHeightIsOneLineCardOfItsWidth() {
        val oneLine = showFixedAdsAndOneLineHeight()

        val ad = adCell().getUnclippedBoundsInRoot()
        assertEquals("ad height, a one-line card of its width", oneLine, ad.height.value, 0.5f)
        val seventh = card("p07").getUnclippedBoundsInRoot()
        assertEquals("ad in the column of card 7", seventh.left.value, ad.left.value, 0.5f)
        assertEquals("ad height, card 7 above it", seventh.height.value, ad.height.value, 0.5f)
    }

    @Test
    @Config(qualifiers = UNFOLDED)
    fun fixedGridAds_unfolded_cellHeightIsOneLineCardOfItsWidth() {
        val oneLine = showFixedAdsAndOneLineHeight()

        val ad = adCell().getUnclippedBoundsInRoot()
        assertEquals("ad height, a one-line card of its width", oneLine, ad.height.value, 0.5f)
        val sixth = card("p06").getUnclippedBoundsInRoot()
        assertEquals("ad in the column of card 6", sixth.left.value, ad.left.value, 0.5f)
        assertEquals("ad height, card 6 above it", sixth.height.value, ad.height.value, 0.5f)
    }

    @Test
    @Config(qualifiers = UNFOLDED)
    fun fixedGridAds_unfolded_threeColumns() {
        setScreen(ready(cards = manyCards()), gridAds = { fixedGridAds })
        composeRule.onNodeWithTag(PosesTestTags.GRID).performScrollToIndex(AD_INDEX)
        composeRule.waitForIdle()

        val ad = adCell().getUnclippedBoundsInRoot()
        val seventh = card("p07").getUnclippedBoundsInRoot()
        val eighth = card("p08").getUnclippedBoundsInRoot()
        assertEquals("ad in the row of card 7", seventh.top.value, ad.top.value, 0.5f)
        assertEquals("ad in the row of card 8", eighth.top.value, ad.top.value, 0.5f)
        assertTrue("ad in the third column: $ad, $eighth", ad.left >= eighth.right)
        assertEquals(
            "ad in the column of card 6",
            card("p06").getUnclippedBoundsInRoot().left.value,
            ad.left.value,
            0.5f,
        )
        val ninth = card("p09").getUnclippedBoundsInRoot()
        assertTrue("card 9 starts the next row", ninth.top > ad.top)
        assertEquals("card 9 in the first column", seventh.left.value, ninth.left.value, 0.5f)
    }

    @Test
    fun fixedGridAds_savedAndGroupFilters_haveCell() {
        setScreen(
            ready(selected = PoseFilter.Category("women"), cards = manyCards(count = 9)),
            gridAds = { fixedGridAds },
        )
        listOf(PoseFilter.Category("women"), PoseFilter.Saved).forEach { filter ->
            state = ready(selected = filter, cards = manyCards(count = 9))
            composeRule.waitForIdle()
            composeRule.onNodeWithTag(PosesTestTags.GRID).performScrollToIndex(AD_INDEX)
            composeRule.waitForIdle()
            composeRule.onAllNodesWithTag(PosesTestTags.AD_CELL).assertCountEquals(1)
            val ad = adCell().getUnclippedBoundsInRoot()
            assertEquals(
                "$filter: ad beside card 9",
                card("p09").getUnclippedBoundsInRoot().top.value,
                ad.top.value,
                0.5f,
            )
            assertTrue("$filter: ad after card 8", ad.top > card("p08").getUnclippedBoundsInRoot().top)
        }
    }

    @Test
    fun noGridAds_noAdCell() {
        setScreen(ready(cards = manyCards()))
        composeRule.onNodeWithTag(PosesTestTags.GRID).performScrollToIndex(AD_INDEX)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(PosesTestTags.AD_CELL).assertDoesNotExist()
        assertEquals(card("p09").getUnclippedBoundsInRoot().top, card("p10").getUnclippedBoundsInRoot().top)
    }

    @Test
    fun posesScreen_liveGridAdsWithoutController_noAdCell() {
        setScreen(
            ready(cards = manyCards()),
            gridAds = { PosesGridAds.Live(rememberInListAds(AdPlacement.POSES_IN_COLLECTION)) },
        )
        composeRule.onNodeWithTag(PosesTestTags.GRID).performScrollToIndex(AD_INDEX)
        composeRule.waitForIdle()

        // No ads controller (tests, previews): the placement is never allowed, so no cell.
        composeRule.onNodeWithTag(PosesTestTags.AD_CELL).assertDoesNotExist()
        composeRule.onNodeWithTag("inlist_ad_0").assertDoesNotExist()
        assertEquals(card("p09").getUnclippedBoundsInRoot().top, card("p10").getUnclippedBoundsInRoot().top)
    }

    private fun scrollToLastCard() {
        composeRule.onNodeWithTag(PosesTestTags.GRID).performScrollToIndex(11)
        composeRule.waitForIdle()
    }

    /**
     * The bottom of the last row, p11 and p12: the right card can be a pixel shorter, as the two columns share an odd
     * number of pixels.
     */
    private fun lastRowBottom(): Float = listOf("p11", "p12").maxOf { card(it).getUnclippedBoundsInRoot().bottom.value }

    private fun centerY(text: String): Float {
        val bounds = composeRule.onNodeWithText(text).getUnclippedBoundsInRoot()
        return (bounds.top.value + bounds.bottom.value) / 2
    }

    // Small screens

    private companion object {
        const val UNFOLDED = "vi-w673dp-h841dp-xxhdpi"

        /** The index of the ad cell after card 8 in the grid (cards 1 to 8, then the cell). */
        const val AD_INDEX = 8
        const val SLOT = "fake_ad_slot"
        const val SLOT_HEIGHT = 80f

        // The tag of the real slot of the framework (BottomAdBarTestTags, internal to :core:ads).
        const val REAL_SLOT = "bottom_ad_bar_slot"

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
        fun manyCards(thumbnail: ThumbnailState = ThumbnailState.Ready(IMAGE), count: Int = 12) = (1..count).map {
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

/**
 * The bottom bar of the framework with a fake 80 dp slot (plan T-079, section 4): the slot takes the modifier the bar
 * gives, so it is as wide as the screen less 2 x 16. [passPadding] false keeps the bar but gives the screen no padding.
 */
private fun fakeBar(
    passPadding: () -> Boolean = { true },
): @Composable (screen: @Composable (PaddingValues) -> Unit) -> Unit = { screen ->
    BottomAdBarLayout(
        reservation = BottomAdBarDefaults.nativeReservation,
        slot = { Box(it.height(80.dp).testTag("fake_ad_slot")) },
        layer = null,
    ) { padding -> screen(if (passPadding()) padding else PaddingValues()) }
}
