package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Text
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** In-list native ads in a LazyColumn and a LazyVerticalGrid: order, label, spacing, keys (plan T-011, section 4). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class InListAdsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val badge: String
        get() = ApplicationProvider.getApplicationContext<android.content.Context>()
            .getString(R.string.ads_native_badge)

    private fun bounds(tag: String): DpRect = composeRule.onNodeWithTag(tag).getBoundsInRoot()

    private fun assertDpEquals(message: String, expected: Dp, actual: Dp) {
        assertEquals(message, expected.value, actual.value, TOLERANCE_DP)
    }

    private fun adRows(prefix: String = AD_TAG_PREFIX) = SemanticsMatcher("test tag starts with $prefix") {
        it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true
    }

    private fun inListAds(every: Int?, rows: InListRowSource, scope: CoroutineScope) = InListAds(
        placement = AdPlacement.SAMPLE_NATIVE_SMALL,
        every = mutableStateOf(every),
        rowSource = rows,
        scope = scope,
        logger = RecordingAppLogger(),
    )

    /** Rows through the real [inListRowStates], the window and the gate open, each loading with [load]. */
    private fun rowsLoading(load: suspend () -> NativeAdState) = InListRowSource { _, _, shown ->
        inListRowStates(
            load = load,
            window = MutableStateFlow(true),
            rowShown = shown,
            mayRequestNow = { true },
            retryResets = MutableStateFlow(0),
            logger = RecordingAppLogger(),
            placementKey = "test",
        )
    }

    private val loadedRows = rowsLoading { NativeAdState.Loaded(FakeLoadedNativeAd(sampleNativeAdAssets)) }

    /** Seven items through the public function, with the real ad rows of [rows], every 3 items. */
    private fun setPublicListContent(rows: InListRowSource) {
        composeRule.setContent {
            AppTheme {
                val scope = rememberCoroutineScope()
                val ads = remember { inListAds(every = 3, rows = rows, scope = scope) }
                LazyColumn(modifier = Modifier.fillMaxSize().testTag(LIST)) {
                    itemsWithInListAds(items = (1..7).toList(), ads = ads) {
                        Text("List item $it", Modifier.testTag("item_$it"))
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun lazyColumn_sevenItemsEveryThree_adRowsAfterItems3And6() {
        composeRule.setContent {
            AppTheme {
                LazyColumn(modifier = Modifier.fillMaxSize().testTag(LIST)) {
                    itemsWithInListAds(
                        items = (1..7).toList(),
                        positions = inListAdPositions(itemCount = 7, every = 3),
                        placementKey = AdPlacement.SAMPLE_NATIVE_SMALL.key,
                        adContent = { slot ->
                            NativeAdCard(sampleNativeAdAssets, NativeAdSize.SMALL, Modifier.testTag("inlist_ad_$slot"))
                        },
                    ) {
                        Text("List item $it", Modifier.testTag("item_$it"))
                    }
                }
            }
        }

        val firstAd = bounds("inlist_ad_0")
        assertTrue(firstAd.top >= bounds("item_3").bottom)
        assertTrue(firstAd.bottom <= bounds("item_4").top)

        composeRule.onNodeWithTag(LIST).performScrollToNode(hasTestTag("inlist_ad_1"))
        val secondAd = bounds("inlist_ad_1")
        assertTrue(secondAd.top >= bounds("item_6").bottom)
        assertTrue(secondAd.bottom <= bounds("item_7").top)

        composeRule.onAllNodes(adRows()).assertCountEquals(2)
    }

    @Test
    fun lazyColumn_fewerItemsThanEvery_noAdRow() {
        var every by mutableStateOf<Int?>(3)
        composeRule.setContent {
            AppTheme {
                val scope = rememberCoroutineScope()
                val ads = remember {
                    InListAds(
                        placement = AdPlacement.SAMPLE_NATIVE_SMALL,
                        every = derivedStateOf { every },
                        rowSource = loadedRows,
                        scope = scope,
                        logger = RecordingAppLogger(),
                    )
                }
                LazyColumn(modifier = Modifier.fillMaxSize().testTag(LIST)) {
                    itemsWithInListAds(items = (1..7).toList(), ads = ads) {
                        Text("List item $it", Modifier.testTag("item_$it"))
                    }
                }
            }
        }
        // Control: with N = 3 the same seven items get an ad row, so its absence below comes from N = 8.
        composeRule.onAllNodesWithTag("inlist_ad_0").assertCountEquals(1)

        every = 8
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag("inlist_ad_0").assertCountEquals(0)
        composeRule.onAllNodesWithText(badge).assertCountEquals(0)
    }

    @Test
    fun adRow_showsAdLabel() {
        setPublicListContent(loadedRows)
        composeRule.onNode(hasText(badge) and hasAnyAncestor(hasTestTag("inlist_ad_0"))).assertExists()
    }

    @Test
    fun adRow_isSixteenDpFromItemsAboveAndBelow() {
        setPublicListContent(loadedRows)

        val card = composeRule
            .onNode(hasTestTag(NativeAdTestTags.CARD) and hasAnyAncestor(hasTestTag("inlist_ad_0")))
            .getBoundsInRoot()
        assertDpEquals("gap above the ad", 16.dp, card.top - bounds("item_3").bottom)
        assertDpEquals("gap below the ad", 16.dp, bounds("item_4").top - card.bottom)
    }

    @Test
    fun unavailableSlot_takesNoHeight() {
        setPublicListContent(InListRowSource { _, _, _ -> flowOf(NativeAdState.Unavailable) })

        // Control: the ad row is in the list, between item 3 and item 4, so the 0 dp below is the row collapsing.
        composeRule.onAllNodesWithTag("inlist_ad_0").assertCountEquals(1)
        assertDpEquals("ad row height", 0.dp, bounds("inlist_ad_0").height)
        assertDpEquals("gap between item 3 and item 4", 0.dp, bounds("item_4").top - bounds("item_3").bottom)
    }

    @Test
    fun loadingSlot_showsPlaceholderOfSmallHeight() {
        setPublicListContent(
            InListRowSource { _, _, _ ->
                flow {
                    emit(NativeAdState.Loading)
                    awaitCancellation()
                }
            },
        )

        assertDpEquals("ad row height", 80.dp + 16.dp * 2, bounds("inlist_ad_0").height)
        composeRule
            .onNode(hasTestTag(NativeAdTestTags.PLACEHOLDER) and hasAnyAncestor(hasTestTag("inlist_ad_0")))
            .assertExists()
    }

    @Test
    fun lazyGrid_twoColumnsEveryThree_adSpansFullWidthAfterItem4() {
        composeRule.setContent {
            AppTheme {
                val scope = rememberCoroutineScope()
                val ads = remember { inListAds(every = 3, rows = loadedRows, scope = scope) }
                LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize().testTag(GRID)) {
                    itemsWithInListAds(items = (1..7).toList(), ads = ads, columns = 2) {
                        Text("List item $it", Modifier.testTag("item_$it"))
                    }
                }
            }
        }
        composeRule.waitForIdle()

        val ad = bounds("inlist_ad_0")
        assertTrue(ad.top >= bounds("item_3").bottom)
        assertTrue(ad.top >= bounds("item_4").bottom)
        assertTrue(ad.bottom <= bounds("item_5").top)
        assertDpEquals("ad row width", bounds(GRID).width, ad.width)
    }

    @Test
    fun publicApi_withoutAdsController_addsNoAdRow() {
        var adsWithoutController: InListAds? = null
        composeRule.setContent {
            AppTheme {
                val ads = rememberInListAds(AdPlacement.SAMPLE_NATIVE_SMALL)
                adsWithoutController = ads
                val scope = rememberCoroutineScope()
                val allowedAds = remember { inListAds(every = 3, rows = loadedRows, scope = scope) }
                Row(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(modifier = Modifier.weight(1f).testTag(LIST)) {
                        itemsWithInListAds(items = (1..20).toList(), ads = ads) {
                            Text("List item $it", Modifier.testTag("item_$it"))
                        }
                    }
                    // Control: the same items with ads allowed get ad rows.
                    LazyColumn(modifier = Modifier.weight(1f).testTag(CONTROL_LIST)) {
                        itemsWithInListAds(items = (1..20).toList(), ads = allowedAds) {
                            Text("List item $it", Modifier.testTag("control_item_$it"))
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNode(hasTestTag("inlist_ad_0") and hasAnyAncestor(hasTestTag(CONTROL_LIST))).assertExists()

        assertNull(adsWithoutController!!.every)
        composeRule.onAllNodes(adRows("item_") and hasAnyAncestor(hasTestTag(LIST))).assertCountEquals(20)
        composeRule.onAllNodes(adRows() and hasAnyAncestor(hasTestTag(LIST))).assertCountEquals(0)
        composeRule.onAllNodes(hasText(badge) and hasAnyAncestor(hasTestTag(LIST))).assertCountEquals(0)
    }

    @Test
    fun loadedSlots_destroyedWhenEveryBecomesNull_andLoadAgainWhenBack() {
        var every by mutableStateOf<Int?>(3)
        val loaded = mutableListOf<FakeLoadedNativeAd>()
        val rows = rowsLoading {
            NativeAdState.Loaded(FakeLoadedNativeAd(sampleNativeAdAssets).also { loaded += it })
        }
        composeRule.setContent {
            AppTheme {
                val scope = rememberCoroutineScope()
                val ads = remember {
                    InListAds(
                        placement = AdPlacement.SAMPLE_NATIVE_SMALL,
                        every = derivedStateOf { every },
                        rowSource = rows,
                        scope = scope,
                        logger = RecordingAppLogger(),
                    )
                }
                LazyColumn(modifier = Modifier.fillMaxSize().testTag(LIST)) {
                    itemsWithInListAds(items = (1..7).toList(), ads = ads) {
                        Text("List item $it", Modifier.testTag("item_$it"))
                    }
                }
            }
        }
        composeRule.waitForIdle()
        // Control: two ads are loaded and none is destroyed while their rows are shown.
        assertEquals(listOf(0, 0), loaded.map { it.destroyCount })

        // Consent withdrawn or placement switched off while the screen is open.
        every = null
        composeRule.waitForIdle()
        composeRule.onAllNodes(adRows()).assertCountEquals(0)
        assertEquals(listOf(1, 1), loaded.map { it.destroyCount })

        // Allowed again: the rows come back with fresh ads.
        every = 3
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag("inlist_ad_0").assertCountEquals(1)
        assertEquals(4, loaded.size)
        assertEquals(listOf(0, 0), loaded.drop(2).map { it.destroyCount })
    }

    @Test
    fun adRowKeys_doNotClashWithItemKeys() {
        composeRule.setContent {
            AppTheme {
                val scope = rememberCoroutineScope()
                val ads = remember { inListAds(every = 3, rows = loadedRows, scope = scope) }
                LazyColumn(modifier = Modifier.fillMaxSize().testTag(LIST)) {
                    itemsWithInListAds(items = (0..19).toList(), ads = ads, key = { it }) {
                        Text("List item $it", Modifier.testTag("item_$it"))
                    }
                }
            }
        }
        composeRule.waitForIdle()

        // Control: ad rows were added next to items keyed 0, 1, 2..., which would clash with a slot-number key.
        composeRule.onAllNodesWithTag("inlist_ad_0").assertCountEquals(1)
        composeRule.onAllNodesWithTag("inlist_ad_1").assertCountEquals(1)
    }

    @Test
    fun alternatingPlacements_takeTurns() {
        composeRule.setContent {
            AppTheme {
                LazyColumn(modifier = Modifier.fillMaxSize().testTag(LIST)) {
                    itemsWithInListAds(
                        items = (1..7).toList(),
                        positions = inListAdPositions(itemCount = 7, every = 3),
                        placementKeys = listOf("a", "b"),
                        adContents = listOf(
                            { slot -> Text("ad a $slot", Modifier.testTag("ad_a")) },
                            { slot -> Text("ad b $slot", Modifier.testTag("ad_b")) },
                        ),
                    ) {
                        Text("List item $it", Modifier.testTag("item_$it"))
                    }
                }
            }
        }

        // Every row fits on the 891 dp screen: read the order from top to bottom.
        val order = listOf("item_1", "item_2", "item_3", "ad_a", "item_4", "item_5", "item_6", "ad_b", "item_7")
        order.forEach { composeRule.onAllNodesWithTag(it).assertCountEquals(1) }
        val tops = order.map { bounds(it).top.value }
        assertEquals("rows out of order: $order -> $tops", tops.sorted(), tops)
        // Control: the first slot is not the second placement, so the turn really changes.
        composeRule.onAllNodesWithText("ad a 0").assertCountEquals(1)
        composeRule.onAllNodesWithText("ad b 1").assertCountEquals(1)
    }

    @Test
    fun failedRow_showsAdAfterRetry() {
        val retryResets = MutableStateFlow(0)
        val secondAnswer = CompletableDeferred<Unit>()
        val calls = mutableMapOf<Int, Int>()
        val rows = InListRowSource { _, slot, shown ->
            inListRowStates(
                load = {
                    val call = calls.getOrElse(slot) { 0 }
                    calls[slot] = call + 1
                    if (call == 0) {
                        NativeAdState.Unavailable
                    } else {
                        secondAnswer.await()
                        NativeAdState.Loaded(FakeLoadedNativeAd(sampleNativeAdAssets))
                    }
                },
                window = MutableStateFlow(true),
                rowShown = shown,
                mayRequestNow = { true },
                retryResets = retryResets,
                logger = RecordingAppLogger(),
                placementKey = "test",
            )
        }
        setPublicListContent(rows)

        // Internet back: the row tries again at once, without waiting for the 5 s of the clock.
        retryResets.value += 1
        composeRule.waitForIdle()
        // Control: before the ad of the second try comes, the failed row takes no height.
        assertDpEquals("ad row height before the ad", 0.dp, bounds("inlist_ad_0").height)

        secondAnswer.complete(Unit)
        composeRule.waitForIdle()
        composeRule.onNode(hasText(badge) and hasAnyAncestor(hasTestTag("inlist_ad_0"))).assertExists()
        assertDpEquals("ad row height", 80.dp + 16.dp * 2, bounds("inlist_ad_0").height)
    }

    @Test
    fun adRow_scrolledAway_marksHidden_backMarksShown() {
        val shownOf = mutableMapOf<Int, StateFlow<Boolean>>()
        val rows = InListRowSource { _, slot, shown ->
            shownOf[slot] = shown
            flow {
                emit(NativeAdState.Loading)
                awaitCancellation()
            }
        }
        composeRule.setContent {
            AppTheme {
                val scope = rememberCoroutineScope()
                val ads = remember { inListAds(every = 3, rows = rows, scope = scope) }
                LazyColumn(modifier = Modifier.fillMaxSize().testTag(LIST)) {
                    itemsWithInListAds(items = (1..40).toList(), ads = ads) {
                        Text("List item $it", Modifier.testTag("item_$it"))
                    }
                }
            }
        }
        composeRule.waitForIdle()
        // Control: the first ad row is drawn before any scroll.
        assertTrue(shownOf.getValue(0).value)

        composeRule.onNodeWithTag(LIST).performScrollToIndex(LAST_INDEX_OF_40)
        composeRule.waitForIdle()
        assertFalse("row 0 scrolled away", shownOf.getValue(0).value)
        assertTrue("last row on screen", shownOf.getValue(LAST_SLOT_OF_40).value)

        composeRule.onNodeWithTag(LIST).performScrollToIndex(0)
        composeRule.waitForIdle()
        assertTrue("row 0 back on screen", shownOf.getValue(0).value)
    }

    @Test
    fun emptyAds_isRejected() {
        val error = runCatching {
            composeRule.setContent {
                AppTheme {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        itemsWithInListAds(items = (1..7).toList(), ads = emptyList()) {
                            Text("List item $it")
                        }
                    }
                }
            }
            composeRule.waitForIdle()
        }.exceptionOrNull()
        assertTrue("expected IllegalArgumentException, got $error", error is IllegalArgumentException)
    }

    /** Cells of a fake pool: each call hands a new ad (or [loadingOnly] calls stay loading) and records releases. */
    private class FakePooledCells(private val loadingOnly: Set<Int> = emptySet()) {
        val handed = mutableListOf<FakeLoadedNativeAd>()
        val released = mutableListOf<FakeLoadedNativeAd>()
        private var calls = 0

        fun cell(): Flow<NativeAdState> {
            val call = calls++
            if (call in loadingOnly) return flowOf(NativeAdState.Loading)
            return flow {
                val ad = FakeLoadedNativeAd(sampleNativeAdAssets.copy(headline = "Pool ad ${call + 1}"))
                handed += ad
                try {
                    emit(NativeAdState.Loaded(ad))
                    awaitCancellation()
                } finally {
                    released += ad
                }
            }
        }
    }

    /** [itemCount] items every [every] with the pooled medium rows of [cell], N in [every]. */
    private fun setPooledList(
        itemCount: Int,
        every: () -> Int?,
        cell: () -> Flow<NativeAdState>,
        onOpportunity: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            AppTheme {
                val scope = rememberCoroutineScope()
                val ads = remember {
                    InListAds(
                        placement = AdPlacement.GALLERY_INLIST_MEDIUM,
                        every = derivedStateOf(every),
                        rowSource = loadedRows,
                        scope = scope,
                        logger = RecordingAppLogger(),
                        pooledCell = cell,
                        onPoolOpportunity = onOpportunity,
                    )
                }
                LazyColumn(modifier = Modifier.fillMaxSize().testTag(LIST)) {
                    itemsWithInListAds(items = (1..itemCount).toList(), ads = ads) {
                        Text("List item $it", Modifier.testTag("item_$it"))
                    }
                }
            }
        }
    }

    @Test
    fun pooledRow_scrolledAway_releasesAd() {
        val pool = FakePooledCells()
        setPooledList(itemCount = 30, every = { 3 }, cell = pool::cell)
        composeRule.waitForIdle()
        val first = pool.handed.first()
        assertEquals(emptyList<FakeLoadedNativeAd>(), pool.released)

        composeRule.onNodeWithTag(LIST).performScrollToNode(hasText("List item 30"))
        composeRule.waitForIdle()

        assertEquals(1, pool.released.count { it === first })
        // The row gives the ad back to the pool; the pool, not the row, decides whether to destroy it.
        assertEquals(0, first.destroyCount)
    }

    @Test
    fun pooledRow_impressedAd_notShownInAnotherRow() {
        val ads = numberedAds(30)
        val pool = immediatePool(ads, capacity = 5, logger = RecordingAppLogger()).also { it.refill() }
        composeRule.mainClock.autoAdvance = false
        setPooledList(
            itemCount = 30,
            every = { 3 },
            cell = {
                pooledCellStates(pool, AdPlacement.GALLERY_INLIST_MEDIUM, RecordingAppLogger(), "gallery_inlist_medium")
            },
        )
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.onNode(hasText("Ad 1") and hasAnyAncestor(hasTestTag("inlist_ad_0")), useUnmergedTree = true)
            .assertExists()
        ads.first().impressionRecorded = true

        composeRule.onNodeWithTag(LIST).performScrollToIndex(LAST_INDEX_OF_30)
        composeRule.mainClock.advanceTimeBy(1_000)
        // An instant scroll: an animated one never ends while the clock is stopped (L-048).
        composeRule.onNodeWithTag(LIST).performScrollToIndex(INDEX_OF_AD_ROW_2)
        composeRule.mainClock.advanceTimeBy(1_000)

        composeRule.onAllNodesWithTag("inlist_ad_2").assertCountEquals(1)
        composeRule.onAllNodes(hasText("Ad 1"), useUnmergedTree = true).assertCountEquals(0)
        assertEquals(1, ads.first().destroyCount)
        // Control: the row shows an ad of the pool.
        composeRule.onAllNodes(
            hasTestTag(NativeAdTestTags.HEADLINE) and hasAnyAncestor(hasTestTag("inlist_ad_2")),
            useUnmergedTree = true,
        ).assertCountEquals(1)
    }

    @Test
    fun pooledRow_recordsShownOnce_notReadyOnDispose() {
        val pool = FakePooledCells(loadingOnly = setOf(1))
        val results = mutableListOf<String>()
        var every by mutableStateOf<Int?>(3)
        setPooledList(itemCount = 7, every = { every }, cell = pool::cell, onOpportunity = { results += it })
        composeRule.waitForIdle()
        assertEquals(listOf("shown"), results)

        every = null
        composeRule.waitForIdle()

        assertEquals(listOf("shown", "not_ready"), results)
    }

    @Test
    fun pooledRows_everyBecomesNull_releaseToPool() {
        val pool = FakePooledCells()
        var every by mutableStateOf<Int?>(3)
        setPooledList(itemCount = 7, every = { every }, cell = pool::cell)
        composeRule.waitForIdle()
        assertEquals(2, pool.handed.size)
        assertEquals(emptyList<FakeLoadedNativeAd>(), pool.released)

        every = null
        composeRule.waitForIdle()
        composeRule.onAllNodes(adRows()).assertCountEquals(0)
        assertEquals(pool.handed.toSet(), pool.released.toSet())
        assertEquals(2, pool.released.size)

        // Control: allowed again, the rows take ads again.
        every = 3
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag("inlist_ad_0").assertCountEquals(1)
        assertEquals(4, pool.handed.size)
    }

    @Test
    fun grid_inCollection_adTakesOneCell_everyNItems() {
        val positions = gridInListPositions(AdFormat.NATIVE_IN_COLLECTION, itemCount = 7, every = 3, columns = 2)
        assertEquals(listOf(3, 7), positions)
        // Control: a medium row is rounded to whole rows and spans the grid.
        assertEquals(listOf(4), gridInListPositions(AdFormat.NATIVE_MEDIUM, itemCount = 7, every = 3, columns = 2))
        assertTrue(gridAdSpansOneCell(AdFormat.NATIVE_IN_COLLECTION))
        assertFalse(gridAdSpansOneCell(AdFormat.NATIVE_MEDIUM))
        composeRule.setContent {
            AppTheme {
                LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize().testTag(GRID)) {
                    itemsWithInListAds(
                        items = (1..7).toList(),
                        positions = positions,
                        placementKey = "cards",
                        adSpansOneCell = gridAdSpansOneCell(AdFormat.NATIVE_IN_COLLECTION),
                        adContent = { slot -> Text("Ad cell $slot", Modifier.testTag("inlist_ad_$slot")) },
                    ) {
                        Text("List item $it", Modifier.testTag("item_$it"))
                    }
                }
            }
        }
        composeRule.waitForIdle()

        val grid = bounds(GRID)
        val first = bounds("inlist_ad_0")
        assertDpEquals("first ad beside item 3", bounds("item_3").top, first.top)
        assertTrue("first ad in the second column", first.left >= grid.left + grid.width / 2 - TOLERANCE_DP.dp)
        val second = bounds("inlist_ad_1")
        assertDpEquals("second ad beside item 6", bounds("item_6").top, second.top)
        assertTrue("second ad in the second column", second.left >= grid.left + grid.width / 2 - TOLERANCE_DP.dp)
        assertTrue("item 7 below the second ad", bounds("item_7").top >= second.bottom)
    }

    @Test
    fun positions_followEvery() {
        var every by mutableStateOf<Int?>(3)
        val ads = InListAds(
            placement = AdPlacement.GALLERY_INLIST_MEDIUM,
            every = derivedStateOf { every },
            rowSource = loadedRows,
            scope = CoroutineScope(Dispatchers.Unconfined),
            logger = RecordingAppLogger(),
        )
        assertEquals(listOf(3, 7), ads.positions(itemCount = 7))
        assertEquals(emptyList<Int>(), ads.positions(itemCount = 3))

        every = null
        assertEquals(emptyList<Int>(), ads.positions(itemCount = 7))
    }

    private companion object {
        const val LIST = "list"
        const val CONTROL_LIST = "control_list"
        const val GRID = "grid"
        const val AD_TAG_PREFIX = "inlist_ad_"
        const val TOLERANCE_DP = 0.5f

        /** The last index of 30 items with an ad row after every 3: 30 items and 9 ad rows. */
        const val LAST_INDEX_OF_30 = 38

        /** The last index of 40 items with an ad row after every 3 (40 items and 13 ad rows), and its last slot. */
        const val LAST_INDEX_OF_40 = 52
        const val LAST_SLOT_OF_40 = 12

        /** The third ad row of a list with an ad after every 3 items (inListAdPositions). */
        const val INDEX_OF_AD_ROW_2 = 11
    }
}
