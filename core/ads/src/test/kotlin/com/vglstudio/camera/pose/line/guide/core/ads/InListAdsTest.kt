package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
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
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
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
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.analytics.AdFunnelEvent
import com.vglstudio.camera.pose.line.guide.core.analytics.RecordingAdFunnel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
        placement = AdPlacement.POSES_BOTTOM,
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
            AdsTestTheme {
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
            AdsTestTheme {
                LazyColumn(modifier = Modifier.fillMaxSize().testTag(LIST)) {
                    itemsWithInListAds(
                        items = (1..7).toList(),
                        positions = inListAdPositions(itemCount = 7, every = 3),
                        placementKey = AdPlacement.POSES_BOTTOM.key,
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
            AdsTestTheme {
                val scope = rememberCoroutineScope()
                val ads = remember {
                    InListAds(
                        placement = AdPlacement.POSES_BOTTOM,
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
            AdsTestTheme {
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
            AdsTestTheme {
                val ads = rememberInListAds(AdPlacement.POSES_BOTTOM)
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
            AdsTestTheme {
                val scope = rememberCoroutineScope()
                val ads = remember {
                    InListAds(
                        placement = AdPlacement.POSES_BOTTOM,
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
            AdsTestTheme {
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
            AdsTestTheme {
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
            AdsTestTheme {
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
                AdsTestTheme {
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
        poolExhausted: (() -> Boolean)? = null,
        poolHasFreeAd: (() -> Boolean)? = null,
        logger: RecordingAppLogger = RecordingAppLogger(),
    ) {
        composeRule.setContent {
            AdsTestTheme {
                val scope = rememberCoroutineScope()
                val ads = remember {
                    InListAds(
                        placement = AdPlacement.GALLERY_INLIST_MEDIUM,
                        every = derivedStateOf(every),
                        rowSource = loadedRows,
                        scope = scope,
                        logger = logger,
                        pooledCell = cell,
                        onPoolOpportunity = onOpportunity,
                        poolExhausted = poolExhausted,
                        poolHasFreeAd = poolHasFreeAd,
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
            AdsTestTheme {
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

    /** Cells of a scripted fake pool: the k-th call of [cell] returns script(k); [calls] counts them. */
    private class ScriptedCells(private val script: (call: Int) -> Flow<NativeAdState>) {
        var calls = 0
            private set

        fun cell(): Flow<NativeAdState> = script(calls++)
    }

    /** A cell holding one fake ad, "Pool ad <call + 1>", given back into [released] when the cell leaves. */
    private fun loadedCell(call: Int, released: MutableList<FakeLoadedNativeAd>): Flow<NativeAdState> = flow {
        val ad = FakeLoadedNativeAd(sampleNativeAdAssets.copy(headline = "Pool ad ${call + 1}"))
        try {
            emit(NativeAdState.Loaded(ad))
            awaitCancellation()
        } finally {
            released += ad
        }
    }

    /** A cell that stays loading. */
    private fun loadingCell(): Flow<NativeAdState> = flow {
        emit(NativeAdState.Loading)
        awaitCancellation()
    }

    /**
     * [itemCount] items of 120 dp in a grid of [columns] columns through the public grid function, with the pooled
     * cells of [cell] for [placement] every [every]. Without [controller] no `af_native_ad_eligible` is recorded.
     */
    private fun setPooledGrid(
        itemCount: Int,
        every: () -> Int?,
        cell: () -> Flow<NativeAdState>,
        exhausted: () -> Boolean,
        hasFreeAd: () -> Boolean = { false },
        placement: AdPlacement = AdPlacement.GALLERY_IN_COLLECTION,
        columns: Int = 2,
        adCellHeight: (@Composable (cellWidth: Dp) -> Dp)? = null,
        keepSpace: Boolean = false,
        onOpportunity: (String) -> Unit = {},
        controller: AdsController? = null,
    ) {
        composeRule.setContent {
            AdsTestTheme {
                CompositionLocalProvider(LocalAdsController provides controller) {
                    val scope = rememberCoroutineScope()
                    val ads = remember {
                        InListAds(
                            placement = placement,
                            every = derivedStateOf(every),
                            rowSource = loadedRows,
                            scope = scope,
                            logger = RecordingAppLogger(),
                            pooledCell = cell,
                            onPoolOpportunity = onOpportunity,
                            poolExhausted = exhausted,
                            poolHasFreeAd = hasFreeAd,
                            keepSpaceWhenPoolEmpty = keepSpace,
                        )
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier.fillMaxSize().testTag(GRID),
                    ) {
                        itemsWithInListAds(
                            items = (1..itemCount).toList(),
                            ads = ads,
                            columns = columns,
                            adCellHeight = adCellHeight,
                        ) {
                            Text("List item $it", Modifier.height(ITEM_HEIGHT).testTag("item_$it"))
                        }
                    }
                }
            }
        }
    }

    private fun countOf(tag: String): Int = composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().size

    private fun headlineIn(cellTag: String, text: String) =
        composeRule.onAllNodes(hasText(text) and hasAnyAncestor(hasTestTag(cellTag)), useUnmergedTree = true)

    private fun eligibleCount(funnel: RecordingAdFunnel): Int = funnel.events.count { it is AdFunnelEvent.Eligible }

    @Test
    fun grid_inCollection_poolExhausted_noEmptyCell() {
        val pool = ScriptedCells { flowOf(NativeAdState.Unavailable) }
        setPooledGrid(itemCount = 7, every = { 3 }, cell = pool::cell, exhausted = { true })
        composeRule.waitForIdle()

        // Control: the cell was built and asked the pool.
        assertTrue(pool.calls >= 1)
        composeRule.onNodeWithTag("inlist_ad_0").assertDoesNotExist()
        // Item 4 moves into the cell beside item 3: no hole in the grid.
        assertDpEquals("item 4 beside item 3", bounds("item_3").top, bounds("item_4").top)
    }

    @Test
    fun grid_inCollection_cellHoldingAd_staysWhenPoolExhausted() {
        val released = mutableListOf<FakeLoadedNativeAd>()
        val pool = ScriptedCells { call ->
            if (call == 0) loadedCell(call, released) else flowOf(NativeAdState.Unavailable)
        }
        setPooledGrid(itemCount = 7, every = { 3 }, cell = pool::cell, exhausted = { true })
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag("inlist_ad_0").assertCountEquals(1)
        headlineIn("inlist_ad_0", "Pool ad 1").assertCountEquals(1)
        composeRule.onNodeWithTag("inlist_ad_1").assertDoesNotExist()
        assertEquals(emptyList<FakeLoadedNativeAd>(), released)
    }

    @Test
    fun grid_inCollection_poolHasAdsAgain_cellReturns() {
        val released = mutableListOf<FakeLoadedNativeAd>()
        val pool = ScriptedCells { call ->
            if (call < 2) flowOf(NativeAdState.Unavailable) else loadedCell(call, released)
        }
        var free = false
        composeRule.mainClock.autoAdvance = false
        setPooledGrid(itemCount = 7, every = { 3 }, cell = pool::cell, exhausted = { true }, hasFreeAd = { free })
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.onNodeWithTag("inlist_ad_0").assertDoesNotExist()

        free = true
        composeRule.mainClock.advanceTimeBy(1_000)
        // Control: not yet the 2 s tick.
        composeRule.onNodeWithTag("inlist_ad_0").assertDoesNotExist()

        composeRule.mainClock.advanceTimeBy(PoolCellPolicy.EXHAUSTED_RETRY_MILLIS + 100 - 1_000)
        composeRule.onAllNodesWithTag("inlist_ad_0").assertCountEquals(1)
        headlineIn("inlist_ad_0", "Pool ad 3").assertCountEquals(1)
        // The cell that came back is a new cell of the pool.
        composeRule.onAllNodes(hasText("Pool ad 1"), useUnmergedTree = true).assertCountEquals(0)
        composeRule.onAllNodes(hasText("Pool ad 2"), useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun grid_inCollection_poolNotExhaustedWithoutAd_cellStaysOut() {
        val funnel = RecordingAdFunnel()
        val controller = testAdsController(funnel = funnel)
        // Every cell that could be built during the rounds gets no ad; a cell built later (the control) stays loading.
        val pool = ScriptedCells { call -> if (call == 0) flowOf(NativeAdState.Unavailable) else loadingCell() }
        var exhausted = true
        var free = false
        val opportunities = mutableListOf<String>()
        composeRule.mainClock.autoAdvance = false
        setPooledGrid(
            itemCount = 5,
            every = { 3 },
            cell = pool::cell,
            exhausted = { exhausted },
            hasFreeAd = { free },
            onOpportunity = { opportunities += it },
            controller = controller,
        )
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.onNodeWithTag("inlist_ad_0").assertDoesNotExist()
        assertEquals(1, eligibleCount(funnel))
        assertEquals(listOf("not_ready"), opportunities)
        val calls = pool.calls

        repeat(3) { round ->
            // A request in flight: the pool is not exhausted, yet it has no ad.
            exhausted = false
            composeRule.mainClock.advanceTimeBy(PoolCellPolicy.EXHAUSTED_RETRY_MILLIS + 100)
            assertStillOut(round, "in flight", funnel, opportunities, pool, calls)
            // The request failed: exhausted again, waiting for the retry.
            exhausted = true
            composeRule.mainClock.advanceTimeBy(5_000)
            assertStillOut(round, "failed", funnel, opportunities, pool, calls)
        }

        // Control: once the pool has a free ad, the cell comes back.
        free = true
        composeRule.mainClock.advanceTimeBy(PoolCellPolicy.EXHAUSTED_RETRY_MILLIS + 100)
        composeRule.onAllNodesWithTag("inlist_ad_0").assertCountEquals(1)
        assertEquals(2, eligibleCount(funnel))
    }

    private fun assertStillOut(
        round: Int,
        step: String,
        funnel: RecordingAdFunnel,
        opportunities: List<String>,
        pool: ScriptedCells,
        calls: Int,
    ) {
        val at = "round $round, $step"
        assertEquals(at, 0, countOf("inlist_ad_0"))
        assertDpEquals(at, bounds("item_3").top, bounds("item_4").top)
        assertEquals(at, 1, eligibleCount(funnel))
        assertEquals(at, listOf("not_ready"), opportunities)
        assertEquals(at, calls, pool.calls)
    }

    @Test
    fun grid_inCollection_gateClosedAndOpened_cellsReturnAtOnce() {
        val released = mutableListOf<FakeLoadedNativeAd>()
        val pool = ScriptedCells { call ->
            when (call) {
                0 -> loadedCell(call, released)
                1 -> flowOf(NativeAdState.Unavailable)
                else -> loadingCell()
            }
        }
        var every by mutableStateOf<Int?>(3)
        composeRule.mainClock.autoAdvance = false
        setPooledGrid(itemCount = 7, every = { every }, cell = pool::cell, exhausted = { true })
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.onAllNodesWithTag("inlist_ad_0").assertCountEquals(1)
        composeRule.onNodeWithTag("inlist_ad_1").assertDoesNotExist()

        // Placement switched off or consent withdrawn.
        every = null
        Snapshot.sendApplyNotifications()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onAllNodes(adRows()).assertCountEquals(0)
        assertEquals(listOf("Pool ad 1"), released.map { it.assets.headline })

        // Back on while the pool is still exhausted: every cell is there at once, with no 2 s wait.
        every = 3
        Snapshot.sendApplyNotifications()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onAllNodesWithTag("inlist_ad_0").assertCountEquals(1)
        composeRule.onAllNodesWithTag("inlist_ad_1").assertCountEquals(1)
    }

    @Test
    fun grid_inCollection_outInBackground_returnsOnlyAfterResume() {
        val released = mutableListOf<FakeLoadedNativeAd>()
        var inBackground = false
        val pool = ScriptedCells { call ->
            if (inBackground) loadedCell(call, released) else flowOf(NativeAdState.Unavailable)
        }
        var free = false
        setPooledGrid(itemCount = 5, every = { 3 }, cell = pool::cell, exhausted = { true }, hasFreeAd = { free })
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("inlist_ad_0").assertDoesNotExist()
        val callsBefore = pool.calls

        @Suppress("UNCHECKED_CAST")
        val rule = composeRule as AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity>
        val scenario = rule.activityRule.scenario
        inBackground = true
        scenario.moveToState(Lifecycle.State.CREATED)
        free = true
        composeRule.mainClock.advanceTimeBy(PoolCellPolicy.EXHAUSTED_RETRY_MILLIS * 2)
        // No cell was built in background, so no ad was taken.
        assertEquals(callsBefore, pool.calls)

        scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag("inlist_ad_0").assertCountEquals(1)
        assertTrue(pool.calls > callsBefore)
    }

    @Test
    fun grid_fullWidthRow_poolExhausted_keepsZeroHeightRow() {
        val pool = ScriptedCells { flowOf(NativeAdState.Unavailable) }
        setPooledGrid(
            itemCount = 7,
            every = { 3 },
            cell = pool::cell,
            exhausted = { true },
            placement = AdPlacement.GALLERY_INLIST_MEDIUM,
        )
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag("inlist_ad_0").assertCountEquals(1)
        assertDpEquals("full-width row height", 0.dp, bounds("inlist_ad_0").height)
    }

    @Test
    fun lazyColumn_pooledRow_poolExhausted_keepsZeroHeightRow() {
        setPooledList(
            itemCount = 7,
            every = { 3 },
            cell = { flowOf(NativeAdState.Unavailable) },
            poolExhausted = { true },
        )
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag("inlist_ad_0").assertCountEquals(1)
        assertDpEquals("list row height", 0.dp, bounds("inlist_ad_0").height)
    }

    @Test
    fun lazyColumn_pooledRow_poolExhausted_noPoolWatch_noOutOfGridLog() {
        val logger = RecordingAppLogger()
        var freeAdChecks = 0
        composeRule.mainClock.autoAdvance = false
        setPooledList(
            itemCount = 7,
            every = { 3 },
            cell = { flowOf(NativeAdState.Unavailable) },
            poolExhausted = { true },
            poolHasFreeAd = {
                freeAdChecks++
                false
            },
            logger = logger,
        )
        composeRule.mainClock.advanceTimeBy(PoolCellPolicy.EXHAUSTED_RETRY_MILLIS * 2 + 100)

        // A list row never leaves the list (P-06): it starts no watch of the pool and logs no "out of the grid".
        composeRule.onAllNodesWithTag("inlist_ad_0").assertCountEquals(1)
        assertEquals("free-ad checks of a list", 0, freeAdChecks)
        assertTrue(
            "no out-of-the-grid line for a list: ${logger.entries}",
            logger.entries.none { it.message.contains("out of the grid") },
        )
    }

    @Test
    fun grid_inCollection_keepSpace_keepsStaticPlaceholder() {
        val pool = ScriptedCells { flowOf(NativeAdState.Unavailable) }
        setPooledGrid(
            itemCount = 7,
            every = { 3 },
            cell = pool::cell,
            exhausted = { true },
            adCellHeight = { it * 1.5f },
            keepSpace = true,
        )
        composeRule.waitForIdle()

        // Control: the cell was built and asked the pool.
        assertTrue(pool.calls >= 1)
        val cellWidth = bounds(GRID).width / 2
        composeRule.onAllNodesWithTag("inlist_ad_0").assertCountEquals(1)
        val cell = bounds("inlist_ad_0")
        assertDpEquals("cell beside item 3", bounds("item_3").top, cell.top)
        val placeholders = composeRule.onAllNodes(
            hasTestTag(NativeAdTestTags.PLACEHOLDER) and hasAnyAncestor(hasTestTag("inlist_ad_0")),
            useUnmergedTree = true,
        )
        placeholders.assertCountEquals(1)
        assertDpEquals("placeholder height", cellWidth * 1.5f, placeholders[0].getBoundsInRoot().height)
        assertTrue("item 4 on the next row", bounds("item_4").top >= cell.bottom - TOLERANCE_DP.dp)
    }

    @Test
    fun grid_inCollection_cellTakesAppHeight() {
        val released = mutableListOf<FakeLoadedNativeAd>()
        val pool = ScriptedCells { call -> if (call == 0) loadedCell(call, released) else loadingCell() }
        setPooledGrid(
            itemCount = 7,
            every = { 3 },
            cell = pool::cell,
            exhausted = { false },
            adCellHeight = { it * 1.5f },
        )
        composeRule.waitForIdle()

        val expected = bounds(GRID).width / 2 * 1.5f
        val card = composeRule
            .onNode(hasTestTag(NativeAdTestTags.CARD) and hasAnyAncestor(hasTestTag("inlist_ad_0")))
            .getBoundsInRoot()
        val first = bounds("inlist_ad_0")
        assertDpEquals("card height", expected, card.height)
        assertDpEquals("cell height", card.height, first.height)
        assertDpEquals("cell beside item 3", bounds("item_3").top, first.top)

        val placeholder = composeRule
            .onNode(
                hasTestTag(NativeAdTestTags.PLACEHOLDER) and hasAnyAncestor(hasTestTag("inlist_ad_1")),
                useUnmergedTree = true,
            )
            .getBoundsInRoot()
        assertDpEquals("placeholder height", expected, placeholder.height)
        assertDpEquals("loading cell height", placeholder.height, bounds("inlist_ad_1").height)
    }

    @Test
    fun grid_inCollection_noHeight_keeps300_noOwnPadding() {
        val released = mutableListOf<FakeLoadedNativeAd>()
        val pool = ScriptedCells { call -> loadedCell(call, released) }
        setPooledGrid(itemCount = 7, every = { 3 }, cell = pool::cell, exhausted = { false })
        composeRule.waitForIdle()

        val card = composeRule
            .onNode(hasTestTag(NativeAdTestTags.CARD) and hasAnyAncestor(hasTestTag("inlist_ad_0")))
            .getBoundsInRoot()
        assertDpEquals("card height", 300.dp, card.height)
        assertDpEquals("cell height", 300.dp, bounds("inlist_ad_0").height)
        assertDpEquals("card beside item 3", bounds("item_3").top, card.top)
    }

    @Test
    fun fixedAds_gridPositions_oneCellAndFullWidth() {
        var format by mutableStateOf(AdFormat.NATIVE_IN_COLLECTION)
        var every by mutableStateOf(8)
        var itemCount by mutableStateOf(12)
        var columns by mutableStateOf(2)
        composeRule.setContent {
            AdsTestTheme {
                LazyVerticalGrid(columns = GridCells.Fixed(columns), modifier = Modifier.fillMaxSize().testTag(GRID)) {
                    itemsWithFixedAds(
                        items = (1..itemCount).toList(),
                        format = format,
                        every = every,
                        columns = columns,
                        key = { it },
                        adContent = { slot ->
                            Text("Fixed ad $slot", Modifier.fillMaxWidth().testTag("fixed_ad_$slot"))
                        },
                    ) {
                        Text("List item $it", Modifier.height(ITEM_HEIGHT).testTag("item_$it"))
                    }
                }
            }
        }
        composeRule.waitForIdle()

        // Two columns: one card after item 8, in the left cell beside item 9.
        val grid = bounds(GRID)
        composeRule.onAllNodes(adRows("fixed_ad_")).assertCountEquals(1)
        var ad = bounds("fixed_ad_0")
        assertDpEquals("ad beside item 9", bounds("item_9").top, ad.top)
        assertDpEquals("ad in the left column", grid.left, ad.left)
        assertTrue("ad one cell wide", ad.right <= grid.left + grid.width / 2 + TOLERANCE_DP.dp)

        // Three columns: the card takes the third cell of the row of items 7 and 8; item 9 opens the next row.
        columns = 3
        composeRule.waitForIdle()
        ad = bounds("fixed_ad_0")
        assertDpEquals("ad beside item 7", bounds("item_7").top, ad.top)
        assertDpEquals("ad beside item 8", bounds("item_8").top, ad.top)
        assertTrue("ad in the third column", ad.left >= grid.left + grid.width * 2 / 3 - TOLERANCE_DP.dp)
        assertDpEquals("item 9 in the left column", grid.left, bounds("item_9").left)
        assertTrue("item 9 on the next row", bounds("item_9").top >= bounds("item_7").bottom - TOLERANCE_DP.dp)

        // Medium spans the whole grid, N rounded up to whole rows, as in the real grid function.
        format = AdFormat.NATIVE_MEDIUM
        every = 3
        itemCount = 7
        columns = 2
        composeRule.waitForIdle()
        composeRule.onAllNodes(adRows("fixed_ad_")).assertCountEquals(1)
        ad = bounds("fixed_ad_0")
        assertTrue("ad below item 4", ad.top >= bounds("item_4").bottom - TOLERANCE_DP.dp)
        assertTrue("ad above item 5", ad.bottom <= bounds("item_5").top + TOLERANCE_DP.dp)
        assertDpEquals("ad width", grid.width, ad.width)
    }

    @Test
    fun grid_inCollection_poolCycle_reportsOncePerEntry() {
        val funnel = RecordingAdFunnel()
        val controller = testAdsController(funnel = funnel)
        val first = MutableStateFlow<NativeAdState>(NativeAdState.Loading)
        val pool = ScriptedCells { call -> if (call == 0) first else loadingCell() }
        var exhausted = false
        var free = false
        val opportunities = mutableListOf<String>()
        composeRule.mainClock.autoAdvance = false
        setPooledGrid(
            itemCount = 5,
            every = { 3 },
            cell = pool::cell,
            exhausted = { exhausted },
            hasFreeAd = { free },
            onOpportunity = { opportunities += it },
            controller = controller,
        )
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.onAllNodesWithTag("inlist_ad_0").assertCountEquals(1)
        assertEquals(1, eligibleCount(funnel))

        exhausted = true
        first.value = NativeAdState.Unavailable
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.onNodeWithTag("inlist_ad_0").assertDoesNotExist()
        assertEquals(listOf("not_ready"), opportunities)

        // The pool stays exhausted: nothing more is recorded.
        composeRule.mainClock.advanceTimeBy(PoolCellPolicy.EXHAUSTED_RETRY_MILLIS + 100)
        composeRule.onNodeWithTag("inlist_ad_0").assertDoesNotExist()
        assertEquals(1, eligibleCount(funnel))

        // A free ad: the cell comes back, a new entry.
        free = true
        composeRule.mainClock.advanceTimeBy(PoolCellPolicy.EXHAUSTED_RETRY_MILLIS + 100)
        composeRule.onAllNodesWithTag("inlist_ad_0").assertCountEquals(1)
        assertEquals(2, eligibleCount(funnel))
        assertEquals(listOf("not_ready"), opportunities)
    }

    @Test
    fun grid_inCollection_oneFreeAd_oneCellReturnsPerTick() {
        val funnel = RecordingAdFunnel()
        val controller = testAdsController(funnel = funnel)
        val released = mutableListOf<FakeLoadedNativeAd>()
        val pool = ScriptedCells { call ->
            if (call < 2) flowOf(NativeAdState.Unavailable) else loadedCell(call, released)
        }
        var free = false
        composeRule.mainClock.autoAdvance = false
        setPooledGrid(
            itemCount = 7,
            every = { 3 },
            cell = pool::cell,
            exhausted = { true },
            hasFreeAd = { free },
            controller = controller,
        )
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.onNodeWithTag("inlist_ad_0").assertDoesNotExist()
        composeRule.onNodeWithTag("inlist_ad_1").assertDoesNotExist()
        assertEquals(2, eligibleCount(funnel))

        free = true
        composeRule.mainClock.advanceTimeBy(PoolCellPolicy.EXHAUSTED_RETRY_MILLIS + 100)
        composeRule.onAllNodesWithTag("inlist_ad_0").assertCountEquals(1)
        headlineIn("inlist_ad_0", "Pool ad 3").assertCountEquals(1)
        composeRule.onNodeWithTag("inlist_ad_1").assertDoesNotExist()
        assertEquals(3, eligibleCount(funnel))

        // The free ad was taken: the other cell stays out.
        free = false
        composeRule.mainClock.advanceTimeBy(PoolCellPolicy.EXHAUSTED_RETRY_MILLIS + 100)
        composeRule.onNodeWithTag("inlist_ad_1").assertDoesNotExist()
        assertEquals(3, eligibleCount(funnel))

        free = true
        composeRule.mainClock.advanceTimeBy(PoolCellPolicy.EXHAUSTED_RETRY_MILLIS + 100)
        composeRule.onAllNodesWithTag("inlist_ad_1").assertCountEquals(1)
        assertEquals(4, eligibleCount(funnel))
    }

    @Test
    fun grid_inCollection_twoCycles_watchRestarts() {
        val funnel = RecordingAdFunnel()
        val controller = testAdsController(funnel = funnel)
        val released = mutableListOf<FakeLoadedNativeAd>()
        val second = MutableStateFlow<NativeAdState>(
            NativeAdState.Loaded(FakeLoadedNativeAd(sampleNativeAdAssets.copy(headline = "Pool ad 2"))),
        )
        val pool = ScriptedCells { call ->
            when (call) {
                0 -> flowOf(NativeAdState.Unavailable)
                1 -> second
                else -> loadedCell(call, released)
            }
        }
        var free = false
        composeRule.mainClock.autoAdvance = false
        setPooledGrid(
            itemCount = 5,
            every = { 3 },
            cell = pool::cell,
            exhausted = { true },
            hasFreeAd = { free },
            controller = controller,
        )
        // Cycle 1.
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.onNodeWithTag("inlist_ad_0").assertDoesNotExist()
        free = true
        composeRule.mainClock.advanceTimeBy(PoolCellPolicy.EXHAUSTED_RETRY_MILLIS + 100)
        headlineIn("inlist_ad_0", "Pool ad 2").assertCountEquals(1)
        // No cell is out any more: the watch ends.
        composeRule.mainClock.advanceTimeBy(PoolCellPolicy.EXHAUSTED_RETRY_MILLIS + 100)

        // Cycle 2: the cell loses its ad while the pool is exhausted.
        free = false
        second.value = NativeAdState.Unavailable
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.onNodeWithTag("inlist_ad_0").assertDoesNotExist()
        composeRule.mainClock.advanceTimeBy(PoolCellPolicy.EXHAUSTED_RETRY_MILLIS + 100)
        composeRule.onNodeWithTag("inlist_ad_0").assertDoesNotExist()
        free = true
        composeRule.mainClock.advanceTimeBy(PoolCellPolicy.EXHAUSTED_RETRY_MILLIS + 100)
        composeRule.onAllNodesWithTag("inlist_ad_0").assertCountEquals(1)
        headlineIn("inlist_ad_0", "Pool ad 3").assertCountEquals(1)
        assertEquals(3, eligibleCount(funnel))
    }

    @Test
    fun rememberInListAds_pooledPlacement_wiresControllerSignals() {
        val settings = FakeRemoteSettings()
        val loader = FakeNativeAdLoader()
        val controller = testAdsController(flags = DefaultAdsFlags(settings))
        controller.nativeLoaderForTest = loader
        settings.set(AdsSettings.placementEnabled(AdPlacement.GALLERY_IN_COLLECTION), true)
        controller.markSdkReadyForTest()
        controller.openConsentGateForTest()
        controller.setAppForegroundForTest(true)
        var pooled: InListAds? = null
        var small: InListAds? = null
        composeRule.setContent {
            CompositionLocalProvider(LocalAdsController provides controller) {
                pooled = rememberInListAds(AdPlacement.GALLERY_IN_COLLECTION)
                small = rememberInListAds(AdPlacement.POSES_BOTTOM)
            }
        }
        composeRule.waitForIdle()

        // Control: a format with no pool gets neither signal.
        assertNull(small!!.poolExhausted)
        assertNull(small!!.poolHasFreeAd)
        val ads = pooled!!
        assertNotNull(ads.poolExhausted)
        assertNotNull(ads.poolHasFreeAd)
        // No pool yet.
        assertTrue(ads.poolExhausted!!())
        assertFalse(ads.poolHasFreeAd!!())

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        try {
            val cell = scope.launch { controller.pooledCell(AdPlacement.GALLERY_IN_COLLECTION).collect {} }
            composeRule.waitForIdle()
            assertEquals(1, loader.requests.size)
            // In flight: not exhausted, still no free ad.
            assertFalse(ads.poolExhausted!!())
            assertFalse(ads.poolHasFreeAd!!())

            cell.cancel()
            loader.deliver(FakeLoadedNativeAd())
            composeRule.waitForIdle()
            assertTrue(ads.poolHasFreeAd!!())
        } finally {
            scope.cancel()
        }
    }

    private companion object {
        const val LIST = "list"
        const val CONTROL_LIST = "control_list"
        const val GRID = "grid"
        const val AD_TAG_PREFIX = "inlist_ad_"
        const val TOLERANCE_DP = 0.5f

        /** The height of an app item in the grids of plan T-097, section 4. */
        val ITEM_HEIGHT = 120.dp

        /** The last index of 30 items with an ad row after every 3: 30 items and 9 ad rows. */
        const val LAST_INDEX_OF_30 = 38

        /** The last index of 40 items with an ad row after every 3 (40 items and 13 ad rows), and its last slot. */
        const val LAST_INDEX_OF_40 = 52
        const val LAST_SLOT_OF_40 = 12

        /** The third ad row of a list with an ad after every 3 items (inListAdPositions). */
        const val INDEX_OF_AD_ROW_2 = 11
    }
}
