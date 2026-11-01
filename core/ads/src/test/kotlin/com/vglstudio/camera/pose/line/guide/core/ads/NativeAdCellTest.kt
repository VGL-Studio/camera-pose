package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The native cell of a grid, a list or a pager page (plan T-028, MC-07, KB-21): card or medium by format, one ad per
 * cell, hidden or a static placeholder when the pool is exhausted, and the opportunity of P-07 (viewed only once the
 * page of a pager settled). A cell with no ad polls its pool for ever, so no test waits for idle while one does: a
 * finite fake flow, or a stopped clock moved by a fixed time (L-048).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class NativeAdCellTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val logger = RecordingAppLogger()

    /** The headline texts drawn inside the cell tagged [cellTag]. */
    private fun headlinesIn(cellTag: String): List<String> = composeRule
        .onAllNodes(
            hasAnyAncestor(hasTestTag(NativeAdTestTags.HEADLINE)) and hasAnyAncestor(hasTestTag(cellTag)),
            useUnmergedTree = true,
        )
        .fetchSemanticsNodes()
        .flatMap { node -> node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } }

    @Test
    fun twoMediumCells_showDifferentAds() {
        val shared = immediatePool(numberedAds(3), capacity = 3, logger = logger).also { it.refill() }
        val single = immediatePool(
            listOf(FakeLoadedNativeAd(sampleNativeAdAssets.copy(headline = "Solo 1"))),
            capacity = 1,
            logger = logger,
        ).also { it.refill() }
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            AppTheme {
                Column {
                    listOf("cell_a" to shared, "cell_b" to shared, "cell_c" to single, "cell_d" to single)
                        .forEach { (tag, pool) ->
                            NativeAdCellHost(
                                states = remember { pooledCellStates(pool, PLACEMENT, logger, PLACEMENT.key) },
                                size = NativeAdSize.MEDIUM,
                                viewed = true,
                                keepSpaceWhenEmpty = false,
                                onOpportunity = {},
                                modifier = Modifier.width(CARD_WIDTH).testTag(tag),
                                adContent = { NativeAdCard(it.assets, NativeAdSize.MEDIUM) },
                            )
                        }
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(1_000)

        val a = headlinesIn("cell_a")
        val b = headlinesIn("cell_b")
        assertEquals(1, a.size)
        assertEquals(1, b.size)
        assertTrue("$a", a.single() in setOf("Ad 1", "Ad 2", "Ad 3"))
        assertTrue("$b", b.single() in setOf("Ad 1", "Ad 2", "Ad 3"))
        assertNotEquals(a, b)
        // Control: a pool of one ad fills one cell; the other shows no ad rather than the same one.
        assertEquals(listOf("Solo 1"), headlinesIn("cell_c"))
        composeRule.onAllNodes(
            hasTestTag(NativeAdTestTags.HEADLINE) and hasAnyAncestor(hasTestTag("cell_d")),
            useUnmergedTree = true,
        ).assertCountEquals(0)
    }

    @Test
    fun exhausted_takesNoSpace() {
        val states = MutableStateFlow<NativeAdState>(NativeAdState.Loading)
        composeRule.setContent {
            AppTheme {
                NativeAdCellHost(
                    states = states,
                    size = NativeAdSize.MEDIUM,
                    viewed = true,
                    keepSpaceWhenEmpty = false,
                    onOpportunity = {},
                    modifier = Modifier.width(CARD_WIDTH).testTag(CELL),
                    adContent = { NativeAdCard(it.assets, NativeAdSize.MEDIUM) },
                )
            }
        }
        // Control: while loading the cell has the height of the card.
        composeRule.onNodeWithTag(CELL).assertHeightIsEqualTo(410.dp)

        states.value = NativeAdState.Unavailable
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(CELL).assertHeightIsEqualTo(0.dp)
    }

    @Test
    fun exhausted_keepSpaceDeclared_showsStaticPlaceholder() {
        val seen = mutableListOf<NativeAdState>()
        composeRule.setContent {
            AppTheme {
                NativeAdCellHost(
                    states = remember { flowOf(NativeAdState.Loading, NativeAdState.Unavailable) },
                    size = NativeAdSize.MEDIUM,
                    viewed = true,
                    keepSpaceWhenEmpty = true,
                    onOpportunity = {},
                    modifier = Modifier.width(CARD_WIDTH).testTag(CELL),
                    onStateChange = { seen += it },
                    adContent = { NativeAdCard(it.assets, NativeAdSize.MEDIUM) },
                )
            }
        }
        composeRule.waitForIdle()

        assertEquals(NativeAdState.Unavailable, seen.last())
        composeRule.onNodeWithTag(CELL).assertHeightIsEqualTo(410.dp)
        composeRule.onAllNodes(
            hasTestTag(NativeAdTestTags.PLACEHOLDER) and hasAnyAncestor(hasTestTag(CELL)),
            useUnmergedTree = true,
        ).assertCountEquals(1)
    }

    @Test
    fun inCollection_drawsCard_medium_drawsMedium() {
        val card = requireCellFormat(AdFormat.NATIVE_IN_COLLECTION)
        val medium = requireCellFormat(AdFormat.NATIVE_MEDIUM)
        assertEquals(NativeAdSize.CARD, card)
        assertEquals(NativeAdSize.MEDIUM, medium)
        composeRule.setContent {
            AppTheme {
                Column {
                    listOf(
                        Triple("card", card, 180.dp),
                        Triple("medium", medium, CARD_WIDTH),
                    ).forEach { (tag, size, w) ->
                        NativeAdCellHost(
                            states = remember { flowOf(NativeAdState.Loaded(FakeLoadedNativeAd())) },
                            size = size,
                            viewed = true,
                            keepSpaceWhenEmpty = false,
                            onOpportunity = {},
                            modifier = Modifier.width(w).testTag(tag),
                            adContent = { NativeAdCard(it.assets, size) },
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("card").assertHeightIsEqualTo(300.dp)
        composeRule.onNodeWithTag("medium").assertHeightIsEqualTo(410.dp)
    }

    @Test
    fun otherFormat_isRejected() {
        assertThrows(IllegalArgumentException::class.java) { requireCellFormat(AdFormat.NATIVE_SMALL) }
        assertThrows(IllegalArgumentException::class.java) { requireCellFormat(AdFormat.BANNER) }
        val error = runCatching {
            composeRule.setContent { AppTheme { NativeAdCell(AdPlacement.POSES_BOTTOM) } }
            composeRule.waitForIdle()
        }.exceptionOrNull()
        assertTrue("expected IllegalArgumentException, got $error", error is IllegalArgumentException)
    }

    /** A pager of [pages] pages, each a cell of [statesOf], with the opportunities of each page in [records]. */
    private fun setPager(
        pages: Int,
        statesOf: (Int) -> Flow<NativeAdState>,
        records: List<MutableList<String>>,
        onPager: (PagerState, CoroutineScope) -> Unit,
    ) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            AppTheme {
                val state = rememberPagerState(pageCount = { pages })
                val scope = rememberCoroutineScope()
                onPager(state, scope)
                HorizontalPager(state = state, beyondViewportPageCount = 1, modifier = Modifier.fillMaxSize()) { page ->
                    Box(Modifier.fillMaxSize()) {
                        NativeAdCellHost(
                            states = remember { statesOf(page) },
                            size = NativeAdSize.MEDIUM,
                            viewed = state.isPageSettled(page),
                            keepSpaceWhenEmpty = false,
                            onOpportunity = { records[page] += it },
                            onScreen = state.isPageVisible(page),
                            modifier = Modifier.width(CARD_WIDTH),
                            adContent = { NativeAdCard(it.assets, NativeAdSize.MEDIUM) },
                        )
                    }
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(FRAME)
    }

    private fun scrollTo(state: PagerState, scope: CoroutineScope, page: Int) {
        composeRule.runOnUiThread { scope.launch { state.animateScrollToPage(page) } }
    }

    @Test
    fun pager_recordsShownOnlyAfterSettle_notReadyOnDispose() {
        val records = List(PAGER_PAGES) { mutableListOf<String>() }
        lateinit var pager: PagerState
        lateinit var scope: CoroutineScope
        setPager(
            pages = PAGER_PAGES,
            statesOf = { page ->
                if (page == 2) flowOf(NativeAdState.Loading) else flowOf(NativeAdState.Loaded(FakeLoadedNativeAd()))
            },
            records = records,
            onPager = { state, s ->
                pager = state
                scope = s
            },
        )
        assertEquals(listOf("shown"), records[0])

        scrollTo(pager, scope, 1)
        composeRule.mainClock.advanceTimeBy(100)
        assertEquals(emptyList<String>(), records[1])
        composeRule.mainClock.advanceTimeBy(SETTLE)
        assertEquals(1, pager.settledPage)
        assertEquals(listOf("shown"), records[1])

        // Control: page 2 was built next to page 1 but never settled; leaving now records nothing.
        scrollTo(pager, scope, 0)
        composeRule.mainClock.advanceTimeBy(SETTLE)
        assertEquals(0, pager.settledPage)
        assertEquals(emptyList<String>(), records[2])

        scrollTo(pager, scope, 2)
        composeRule.mainClock.advanceTimeBy(SETTLE)
        assertEquals(2, pager.settledPage)
        assertEquals(emptyList<String>(), records[2])
        scrollTo(pager, scope, 0)
        composeRule.mainClock.advanceTimeBy(SETTLE)

        assertEquals(listOf("not_ready"), records[2])
        assertEquals(listOf("shown"), records[1])
    }

    @Test
    fun pagerPage_viewedOnlyWhenSettled() {
        var pager: PagerState? = null
        var scope: CoroutineScope? = null
        val viewed = mutableMapOf<Int, Boolean>()
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            AppTheme {
                val state = rememberPagerState(pageCount = { 3 })
                pager = state
                scope = rememberCoroutineScope()
                HorizontalPager(state = state, beyondViewportPageCount = 1, modifier = Modifier.fillMaxSize()) { page ->
                    viewed[page] = state.isPageSettled(page)
                    Box(Modifier.fillMaxSize())
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(FRAME)
        assertEquals(true, viewed[0])
        assertEquals(false, viewed[1])

        scrollTo(pager!!, scope!!, 1)
        composeRule.mainClock.advanceTimeBy(100)
        assertTrue(pager!!.isScrollInProgress)
        assertEquals(false, pager!!.isPageSettled(1))

        composeRule.mainClock.advanceTimeBy(SETTLE)
        assertEquals(true, pager!!.isPageSettled(1))
        assertEquals(false, pager!!.isPageSettled(0))
    }

    @Test
    fun mediumSlot_recordsShownOnce_notReadyOnDispose() {
        val loaded = mutableListOf<String>()
        val loading = mutableListOf<String>()
        var shown by mutableStateOf(true)
        composeRule.setContent {
            AppTheme {
                if (shown) {
                    Column {
                        listOf(
                            loaded to flowOf<NativeAdState>(NativeAdState.Loaded(FakeLoadedNativeAd())),
                            loading to flowOf<NativeAdState>(NativeAdState.Loading),
                        ).forEach { (record, flow) ->
                            NativeAdSlotGate(
                                allowed = true,
                                states = remember { flow },
                                size = NativeAdSize.MEDIUM,
                                keepSpaceWhenEmpty = false,
                                onAdAvailabilityChange = {},
                                modifier = Modifier.width(CARD_WIDTH),
                                adContent = { NativeAdCard(it.assets, NativeAdSize.MEDIUM) },
                                onPoolOpportunity = { record += it },
                            )
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
        assertEquals(listOf("shown"), loaded)
        assertEquals(emptyList<String>(), loading)

        shown = false
        composeRule.waitForIdle()

        assertEquals(listOf("shown"), loaded)
        assertEquals(listOf("not_ready"), loading)
    }

    private companion object {
        val CARD_WIDTH = 379.dp
        const val CELL = "cell"
        val PLACEMENT = AdPlacement.GALLERY_NATIVE_MEDIUM
        const val FRAME = 100L
        const val SETTLE = 3_000L

        /** The pager of plan T-028, section 4: three pages, one built beyond the viewport. */
        const val PAGER_PAGES = 3
    }
}
