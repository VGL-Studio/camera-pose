package com.vglstudio.camera.pose.line.guide.core.ads

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "Ads"

/**
 * Ads of one in-list placement on one screen. Create it with [rememberInListAds], outside the lazy list. A native
 * small row keeps its ad for the life of the screen: scrolling a row away and back shows the same ad, never a new
 * request (ads-guide.md, section 6); every such ad is destroyed when the screen leaves the composition. A native small
 * row that failed tries again 5 s later while it is drawn, only while the in-screen window is open (foreground, no
 * full-screen ad over the app, KB-27) and the consent gate is open right at the send (O-05, plan T-059). A row of a
 * pooled format (medium, card: plan T-028) is a cell of the pool ([pooledCell]): it takes an ad when it comes on
 * screen and gives it back when it leaves (P-01, P-01a), so a row scrolled back may show another ad.
 */
@Stable
class InListAds internal constructor(
    val placement: AdPlacement,
    every: State<Int?>,
    private val rowSource: InListRowSource?,
    private val scope: CoroutineScope,
    private val logger: AppLogger?,
    /** The states of one new cell of the pool of [placement]; null for a format with no pool or without ads. */
    internal val pooledCell: (() -> Flow<NativeAdState>)? = null,
    /** Records the opportunity of a pooled row (P-07). */
    internal val onPoolOpportunity: (result: String) -> Unit = {},
    /** Whether the pool of [placement] is exhausted now (P-05); null without a pool. */
    internal val poolExhausted: (() -> Boolean)? = null,
) {
    private val everyState = every

    /**
     * Positions of the ad rows among [itemCount] items with the current N ([inListAdPositions]), empty while no ad row
     * may be added. For a feature that lays out ads itself, such as the pages of a pager.
     */
    fun positions(itemCount: Int): List<Int> = every?.let { inListAdPositions(itemCount, it) } ?: emptyList()

    // Touched on the main thread only (composition and the screen's coroutine scope).
    private val slots = mutableMapOf<Int, MutableState<NativeAdState>>()
    private val loads = mutableMapOf<Int, Job>()
    private val shown = mutableMapOf<Int, MutableStateFlow<Boolean>>()
    private var destroyed = false

    // Grid cells of a card (KB-6, criterion 5; plan T-080): no empty cell while the pool is exhausted. Declared before
    // `init`, which may call release() at once.
    private val poolEmpty = mutableStateOf(false)
    private val holding = mutableStateMapOf<Int, Boolean>()
    private var poolWatch: Job? = null

    /** Current N, or null while no ad row may be added. */
    val every: Int? get() = everyState.value

    /** A one-cell grid ad of [slot] is laid out: the pool may still give an ad, or the cell holds one. */
    internal fun cellShown(slot: Int): Boolean = !poolEmpty.value || holding[slot] == true

    /**
     * A pooled cell of [slot] has a new state. A cell with no ad while the pool is exhausted marks the pool empty: the
     * cells that hold no ad leave the grid, and the pool is asked again every [PoolCellPolicy.EXHAUSTED_RETRY_MILLIS]
     * until it may give an ad again (P-05).
     */
    internal fun onCellState(slot: Int, state: NativeAdState) {
        if (state is NativeAdState.Loaded) holding[slot] = true else holding.remove(slot)
        if (state == NativeAdState.Unavailable && poolExhausted?.invoke() == true) {
            poolEmpty.value = true
            if (poolWatch?.isActive != true) {
                poolWatch = scope.launch {
                    while (poolEmpty.value) {
                        delay(PoolCellPolicy.EXHAUSTED_RETRY_MILLIS)
                        if (poolExhausted.invoke() != true) poolEmpty.value = false
                    }
                }
            }
        }
    }

    /** The pooled cell of [slot] left the composition: its ad went back to the pool. */
    internal fun onCellLeft(slot: Int) {
        holding.remove(slot)
    }

    init {
        // Consent withdrawn or placement switched off while the screen is open: the rows go away, so do their ads.
        // Slots load again if ads are allowed again later.
        scope.launch {
            snapshotFlow { everyState.value }.collect { if (it == null) release() }
        }
    }

    /**
     * The ad of the [index]-th ad row of the list: its states ([InListRowSource]) are collected from the first call
     * on, and every later call returns the same state. Called while the row is drawn only.
     */
    internal fun slot(index: Int): State<NativeAdState> {
        slots[index]?.let { return it }
        if (destroyed || rowSource == null) return mutableStateOf(NativeAdState.Unavailable)
        val state = mutableStateOf<NativeAdState>(NativeAdState.Loading)
        val rowShown = MutableStateFlow(true)
        slots[index] = state
        shown[index] = rowShown
        loads[index] = scope.launch {
            rowSource.states(placement, index, rowShown).collect { result ->
                if (slots[index] !== state) return@collect
                state.value = result
                val outcome = when (result) {
                    is NativeAdState.Loaded -> "LOADED"
                    NativeAdState.Unavailable -> "UNAVAILABLE"
                    NativeAdState.Loading -> return@collect
                }
                logger?.info(TAG, "in-list ${placement.key} slot $index: $outcome")
            }
        }
        return state
    }

    /**
     * The row of slot [index] came into the composition ([isShown] true) or left it. A row that left keeps its flow:
     * a request in flight goes on and its ad is kept; a retry due meanwhile waits until the row is drawn again.
     */
    internal fun onRowShown(index: Int, isShown: Boolean) {
        shown[index]?.value = isShown
    }

    /** Destroys every loaded ad and cancels the loads still running. Called when the screen is left. */
    internal fun destroyAll() {
        destroyed = true
        release()
    }

    /**
     * Cancels the state flows of the rows, which destroy the ads they hold and the ads arriving late; the rows still
     * in the composition draw nothing. Later calls to [slot] load again.
     */
    private fun release() {
        loads.values.forEach { it.cancel() }
        loads.clear()
        slots.values.forEach { it.value = NativeAdState.Unavailable }
        slots.clear()
        shown.clear()
        poolWatch?.cancel()
        poolWatch = null
        poolEmpty.value = false
        holding.clear()
    }
}

/** The states of the native small row of slot `slot` of a list; [rowShown] is true while the row is drawn. */
internal fun interface InListRowSource {
    fun states(placement: AdPlacement, slot: Int, rowShown: StateFlow<Boolean>): Flow<NativeAdState>
}

/**
 * The states of one native small row of a list (plan T-059): no refresh, no single-ad source. Each send, the first
 * one and every retry (5 s with no ad, sooner when Internet comes back), goes through [retryingNativeAdStates], which
 * waits for the in-screen [window] (foreground, no full-screen ad over the app, KB-27) and asks [mayRequestNow]; then
 * it waits until the row is drawn ([rowShown]) and asks the window and the gate again right before the send, since
 * that wait may be long. A row scrolled away sends nothing until it is drawn again. A Loaded ad stays until the
 * collection is cancelled, then is destroyed. Every send path of a row goes through this function (L-072).
 */
internal fun inListRowStates(
    load: suspend () -> NativeAdState,
    window: StateFlow<Boolean>,
    rowShown: StateFlow<Boolean>,
    mayRequestNow: () -> Boolean,
    retryResets: StateFlow<Int>,
    logger: AppLogger,
    placementKey: String,
): Flow<NativeAdState> = retryingNativeAdStates(
    load = {
        rowShown.first { it }
        if (awaitSendAllowed(window, mayRequestNow)) load() else null
    },
    foreground = window,
    mayRequestNow = mayRequestNow,
    retryResets = retryResets,
    logger = logger,
    placementKey = placementKey,
    refresh = null,
)

/**
 * Only small, medium and card (`native_in_collection`) native ads go inside a list. Not an exhaustive `when` on
 * purpose: other formats (collapsible, full screen) do not belong in a list and must not have to change this file.
 */
internal fun requireInListFormat(format: AdFormat) {
    require(format in IN_LIST_FORMATS) { "$format is not an in-list ad format" }
}

private val IN_LIST_FORMATS = setOf(AdFormat.NATIVE_SMALL, AdFormat.NATIVE_MEDIUM, AdFormat.NATIVE_IN_COLLECTION)

/** The size an ad row of [format] is drawn at. */
private fun inListAdSize(format: AdFormat): NativeAdSize =
    if (format == AdFormat.NATIVE_IN_COLLECTION) NativeAdSize.CARD else requireNativeFormat(format)

/**
 * The in-list ads of [placement] (native small, medium or card) for one screen. Call it outside the lazy list, then
 * pass it to [itemsWithInListAds]. Medium and card rows take their ads from the pool of the placement (plan T-028). No row is added until ads are allowed (consent given, SDK started, `is_show_ads` and
 * `is_show_<code>` switched on remotely); N comes from `native_every_<code>` (default [AdPlacement.nativeEvery]) and
 * follows remote changes. A placement that declares no `nativeEvery` adds no row. Without an ads controller (tests,
 * previews) no row is ever added.
 */
@Composable
fun rememberInListAds(placement: AdPlacement): InListAds {
    requireInListFormat(placement.format)
    val controller = LocalAdsController.current
    val every: State<Int?> = if (controller == null) {
        remember { mutableStateOf(null) }
    } else {
        remember(controller, placement) { controller.inListEvery(placement) }
            .collectAsStateWithLifecycle(initialValue = null)
    }
    val scope = rememberCoroutineScope()
    val ads = remember(placement, controller, every, scope) {
        InListAds(
            placement = placement,
            every = every,
            rowSource = controller?.let { ads ->
                InListRowSource { p, slot, rowShown -> ads.inListRowStates(p, slot, rowShown) }
            },
            scope = scope,
            logger = controller?.logger,
            pooledCell = controller?.takeIf { placement.format.usesPool }?.let { ads -> { ads.pooledCell(placement) } },
            onPoolOpportunity = { result -> controller?.recordPoolCellOpportunity(placement, result) },
            poolExhausted = controller?.takeIf { placement.format.usesPool }
                ?.let { ads -> { ads.poolExhausted(placement) } },
        )
    }
    DisposableEffect(ads) {
        onDispose { ads.destroyAll() }
    }
    return ads
}

/**
 * Adds [items] to a LazyColumn (vertical lists only) with a native ad row after every N items of [ads] (never first, never last).
 * The ad rows have their own keys and content type, so they never clash with the keys of [items].
 */
fun <T> LazyListScope.itemsWithInListAds(
    items: List<T>,
    ads: InListAds,
    key: ((item: T) -> Any)? = null,
    contentType: (item: T) -> Any? = { null },
    itemContent: @Composable LazyItemScope.(item: T) -> Unit,
) {
    val every = ads.every
    itemsWithInListAds(
        items = items,
        positions = if (every == null) emptyList() else inListAdPositions(items.size, every),
        placementKey = ads.placement.key,
        key = key,
        contentType = contentType,
        adContent = { slot -> InListAdRow(ads, slot) },
        itemContent = itemContent,
    )
}

/**
 * Like [itemsWithInListAds], with ads taken in turn from [ads]: slot k uses ads[k % ads.size]. N is ads.first().every,
 * so no ad row is added while the first placement is off. Each ad row keeps the key of the placement it uses.
 */
fun <T> LazyListScope.itemsWithInListAds(
    items: List<T>,
    ads: List<InListAds>,
    key: ((item: T) -> Any)? = null,
    contentType: (item: T) -> Any? = { null },
    itemContent: @Composable LazyItemScope.(item: T) -> Unit,
) {
    require(ads.isNotEmpty()) { "At least one in-list placement is needed" }
    val every = ads.first().every
    itemsWithInListAds(
        items = items,
        positions = if (every == null) emptyList() else inListAdPositions(items.size, every),
        placementKeys = ads.map { it.placement.key },
        key = key,
        contentType = contentType,
        adContents = ads.map { placementAds ->
            @Composable { slot: Int -> InListAdRow(placementAds, slot) }
        },
        itemContent = itemContent,
    )
}

/**
 * Adds [items] with an ad row at each of [positions], the k-th one drawn by adContents[k % size] with the key of
 * placementKeys[k % size]. [placementKeys] and [adContents] have the same size, at least one.
 */
internal fun <T> LazyListScope.itemsWithInListAds(
    items: List<T>,
    positions: List<Int>,
    placementKeys: List<String>,
    key: ((item: T) -> Any)? = null,
    contentType: (item: T) -> Any? = { null },
    adContents: List<@Composable (slot: Int) -> Unit>,
    itemContent: @Composable LazyItemScope.(item: T) -> Unit,
) {
    require(adContents.isNotEmpty() && placementKeys.size == adContents.size) {
        "Each in-list placement needs its key and its ad content"
    }
    forEachInListRun(items.size, positions) { run ->
        when (run) {
            is InListRun.Items -> items(
                count = run.end - run.start,
                key = key?.let { itemKey -> { index -> itemKey(items[run.start + index]) } },
                contentType = { index -> contentType(items[run.start + index]) },
            ) { index -> itemContent(items[run.start + index]) }
            is InListRun.Ad -> {
                val turn = run.slot % adContents.size
                item(key = InListAdKey(placementKeys[turn], run.slot), contentType = InListAdContentType) {
                    adContents[turn](run.slot)
                }
            }
        }
    }
}

/** Adds [items] with an ad row at each of [positions] (indexes in the combined list), drawn by [adContent]. */
internal fun <T> LazyListScope.itemsWithInListAds(
    items: List<T>,
    positions: List<Int>,
    placementKey: String,
    key: ((item: T) -> Any)? = null,
    contentType: (item: T) -> Any? = { null },
    adContent: @Composable (slot: Int) -> Unit,
    itemContent: @Composable LazyItemScope.(item: T) -> Unit,
) {
    forEachInListRun(items.size, positions) { run ->
        when (run) {
            is InListRun.Items -> items(
                count = run.end - run.start,
                key = key?.let { itemKey -> { index -> itemKey(items[run.start + index]) } },
                contentType = { index -> contentType(items[run.start + index]) },
            ) { index -> itemContent(items[run.start + index]) }
            is InListRun.Ad -> item(key = InListAdKey(placementKey, run.slot), contentType = InListAdContentType) {
                adContent(run.slot)
            }
        }
    }
}

/**
 * Adds [items] to a LazyVerticalGrid (vertical grids only) of [columns] columns, with an ad after every N items of
 * [ads]: a card ([AdFormat.NATIVE_IN_COLLECTION]) takes one cell, N counted in items; any other format is a
 * full-width row, N rounded up to whole rows ([gridAdEvery]). The grid does not tell its column count while the list
 * is built, so the caller passes it. A card has no space of its own above or below (the grid spaces its rows) and is
 * [adCellHeight] high for the width of its cell, so it matches the cards of the app (300 dp without it). While the
 * pool is exhausted, a card cell that holds no ad leaves the grid and the items after it move up: the grid has no
 * empty cell (plan T-080); it comes back once the pool may give an ad again. A full-width row stays, 0 dp high.
 */
fun <T> LazyGridScope.itemsWithInListAds(
    items: List<T>,
    ads: InListAds,
    columns: Int,
    key: ((item: T) -> Any)? = null,
    contentType: (item: T) -> Any? = { null },
    adCellHeight: (@Composable (cellWidth: Dp) -> Dp)? = null,
    itemContent: @Composable LazyGridItemScope.(item: T) -> Unit,
) {
    val every = ads.every
    val format = ads.placement.format
    val oneCell = gridAdSpansOneCell(format)
    itemsWithInListAds(
        items = items,
        positions = if (every == null) emptyList() else gridInListPositions(format, items.size, every, columns),
        placementKey = ads.placement.key,
        key = key,
        contentType = contentType,
        adSpansOneCell = oneCell,
        adShown = if (oneCell) ads::cellShown else { _ -> true },
        adContent = { slot ->
            if (oneCell) {
                BoxWithConstraints {
                    InListAdRow(
                        ads,
                        slot,
                        contentPadding = PaddingValues(),
                        cardHeight = adCellHeight?.invoke(maxWidth),
                    )
                }
            } else {
                InListAdRow(ads, slot)
            }
        },
        itemContent = itemContent,
    )
}

/**
 * Like the grid [itemsWithInListAds], with a fixed ad for screenshots and previews ([NativeAdCard]): an ad after every
 * [every] items of [items] (never first, never last), one cell for [AdFormat.NATIVE_IN_COLLECTION], else a full-width
 * row. The app shows real ads with [itemsWithInListAds].
 */
fun <T> LazyGridScope.itemsWithFixedAds(
    items: List<T>,
    format: AdFormat,
    every: Int,
    columns: Int,
    key: ((item: T) -> Any)? = null,
    contentType: (item: T) -> Any? = { null },
    adContent: @Composable (slot: Int) -> Unit,
    itemContent: @Composable LazyGridItemScope.(item: T) -> Unit,
) = itemsWithInListAds(
    items = items,
    positions = gridInListPositions(format, items.size, every, columns),
    placementKey = FIXED_ADS_KEY,
    key = key,
    contentType = contentType,
    adSpansOneCell = gridAdSpansOneCell(format),
    adContent = adContent,
    itemContent = itemContent,
)

/** Placement key of the ads of [itemsWithFixedAds]: they belong to no placement. */
private const val FIXED_ADS_KEY = "fixed"

/** A card of a collection takes one cell of a grid (MC-07); other formats span the whole grid width. */
internal fun gridAdSpansOneCell(format: AdFormat): Boolean = format == AdFormat.NATIVE_IN_COLLECTION

/** Ad positions in a grid: every N items for a card in one cell, else N rounded up to whole rows. */
internal fun gridInListPositions(format: AdFormat, itemCount: Int, every: Int, columns: Int): List<Int> =
    if (gridAdSpansOneCell(format)) {
        inListAdPositions(itemCount, every)
    } else {
        inListAdPositions(itemCount, gridAdEvery(every, columns))
    }

/**
 * Grid version of the list function above: each ad spans the whole grid width, or one cell with [adSpansOneCell]. An
 * ad whose [adShown] is false is not laid out; the items after it keep their order.
 */
internal fun <T> LazyGridScope.itemsWithInListAds(
    items: List<T>,
    positions: List<Int>,
    placementKey: String,
    key: ((item: T) -> Any)? = null,
    contentType: (item: T) -> Any? = { null },
    adSpansOneCell: Boolean = false,
    adShown: (slot: Int) -> Boolean = { true },
    adContent: @Composable (slot: Int) -> Unit,
    itemContent: @Composable LazyGridItemScope.(item: T) -> Unit,
) {
    forEachInListRun(items.size, positions) { run ->
        when (run) {
            is InListRun.Items -> items(
                count = run.end - run.start,
                key = key?.let { itemKey -> { index -> itemKey(items[run.start + index]) } },
                contentType = { index -> contentType(items[run.start + index]) },
            ) { index -> itemContent(items[run.start + index]) }
            is InListRun.Ad -> if (adShown(run.slot)) {
                item(
                    key = InListAdKey(placementKey, run.slot),
                    span = { GridItemSpan(if (adSpansOneCell) 1 else maxLineSpan) },
                    contentType = InListAdContentType,
                ) { adContent(run.slot) }
            }
        }
    }
}

/** A stretch of the combined list: app items `[start, end)`, or one ad row. */
private sealed interface InListRun {
    data class Items(val start: Int, val end: Int) : InListRun

    data class Ad(val slot: Int) : InListRun
}

/** Splits [itemCount] items around the ad [positions] into runs, in order. Empty item runs are skipped. */
private inline fun forEachInListRun(itemCount: Int, positions: List<Int>, action: (InListRun) -> Unit) {
    var start = 0
    positions.forEachIndexed { slot, position ->
        // Items before this ad: its position minus the ads before it.
        val end = (position - slot).coerceIn(start, itemCount)
        if (end > start) action(InListRun.Items(start, end))
        action(InListRun.Ad(slot))
        start = end
    }
    if (itemCount > start) action(InListRun.Items(start, itemCount))
}

/**
 * Key of an ad row; a type of its own, so it never equals the key of an app item. Parcelable because a lazy list saves
 * its item keys in the Bundle of the screen.
 */
internal data class InListAdKey(val placementKey: String, val slot: Int) : Parcelable {
    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(placementKey)
        dest.writeInt(slot)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<InListAdKey> = object : Parcelable.Creator<InListAdKey> {
            override fun createFromParcel(source: Parcel): InListAdKey =
                InListAdKey(placementKey = source.readString().orEmpty(), slot = source.readInt())

            override fun newArray(size: Int): Array<InListAdKey?> = arrayOfNulls(size)
        }
    }
}

/** Content type of ad rows, so Compose never reuses an item's row for an ad. */
private object InListAdContentType

/** Test tag of the [slot]-th ad row of a list. */
internal fun inListAdTag(slot: Int): String = "inlist_ad_$slot"

/**
 * One ad row, the "In-list ad slot" of the design: [contentPadding] (16 dp above and below) while loading or loaded,
 * nothing (0 dp) when no ad came. A pooled row is a cell of the pool ([NativeAdCellHost]): it takes an ad while in
 * composition, gives it back when it leaves, and records its opportunity (P-07); its states go to
 * [InListAds.onCellState]. [cardHeight] sizes a card ([NativeAdSize.CARD]); other sizes ignore it.
 */
@Composable
internal fun InListAdRow(
    ads: InListAds,
    slot: Int,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(vertical = Spacing.md),
    cardHeight: Dp? = null,
) {
    // `af_native_ad_eligible` once each time the row comes into the list (plan T-033): rows exist only when allowed.
    ReportNativeEligible(true)
    val size = inListAdSize(ads.placement.format)
    val ctaLarge = ads.placement.ctaLarge
    val pooledCell = ads.pooledCell
    if (pooledCell != null) {
        NativeAdCellHost(
            states = remember(ads, slot) { pooledCell() },
            size = size,
            viewed = true,
            keepSpaceWhenEmpty = false,
            onOpportunity = ads.onPoolOpportunity,
            modifier = modifier.fillMaxWidth().testTag(inListAdTag(slot)),
            ctaLarge = ctaLarge,
            contentModifier = Modifier.padding(contentPadding),
            onStateChange = { ads.onCellState(slot, it) },
            cardHeight = cardHeight,
            adContent = { BoundNativeAd(it, size, ctaLarge = ctaLarge, cardHeight = cardHeight) },
        )
        DisposableEffect(ads, slot) {
            onDispose { ads.onCellLeft(slot) }
        }
        return
    }
    val state by remember(ads, slot) { ads.slot(slot) }
    DisposableEffect(ads, slot) {
        ads.onRowShown(slot, true)
        onDispose { ads.onRowShown(slot, false) }
    }
    Box(modifier = modifier.fillMaxWidth().testTag(inListAdTag(slot))) {
        when (val current = state) {
            NativeAdState.Loading ->
                Box(Modifier.padding(contentPadding)) { NativeAdPlaceholder(size, cardHeight = cardHeight) }
            is NativeAdState.Loaded -> Box(Modifier.padding(contentPadding)) {
                BoundNativeAd(current.ad, size, ctaLarge = ctaLarge, cardHeight = cardHeight)
            }
            NativeAdState.Unavailable -> Unit
        }
    }
}
