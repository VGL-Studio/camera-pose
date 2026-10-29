package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow

/**
 * The native ad of one cell of a grid, a list or a pager page (MC-07, KB-21): a card ([AdFormat.NATIVE_IN_COLLECTION])
 * or a medium ad ([AdFormat.NATIVE_MEDIUM]), one ad per cell, taken from the pool of the placement while the cell is
 * on screen and given back when it leaves (P-01, P-01a). Like [NativeAdSlot], it takes no space until ads are allowed.
 * While the pool is exhausted the cell takes no space, or shows a static placeholder when the placement declares
 * [AdPlacement.keepSpaceWhenPoolEmpty]; it keeps asking, so an ad that comes later is shown (P-06). In a pager, pass
 * [viewed] = `pagerState.isPageSettled(page)`: the cell counts as seen only once its page settled (P-07), and
 * [onScreen] = `pagerState.isPageVisible(page)`: a pager keeps the pages it built, so a page that scrolled away gives
 * its ad back only through it (P-01); coming back, it claims again.
 * [onAdAvailabilityChange] works as for [NativeAdSlot]. Other formats are an [IllegalArgumentException].
 */
@Composable
fun NativeAdCell(
    placement: AdPlacement,
    modifier: Modifier = Modifier,
    viewed: Boolean = true,
    onAdAvailabilityChange: (Boolean) -> Unit = {},
    onScreen: Boolean = true,
) {
    val size = requireCellFormat(placement.format)
    val ads = LocalAdsController.current
    if (ads == null) {
        Box(modifier = modifier)
        return
    }
    val canRequestAds by ads.canRequestAds.collectAsStateWithLifecycle()
    val enabled by remember(placement) { ads.isEnabled(placement) }.collectAsStateWithLifecycle(initialValue = false)
    val unitId = remember(placement) { ads.unitId(placement) }
    NativeAdSlotGate(
        allowed = nativeSlotAllowed(canRequestAds, enabled, unitId),
        // A cold flow: nothing is claimed until the gate opens and the host collects it.
        states = remember(placement) { ads.pooledCell(placement) },
        size = size,
        keepSpaceWhenEmpty = placement.keepSpaceWhenPoolEmpty,
        onAdAvailabilityChange = onAdAvailabilityChange,
        modifier = modifier,
        adContent = { BoundNativeAd(it, size, ctaLarge = placement.ctaLarge) },
        onPoolOpportunity = { ads.recordPoolCellOpportunity(placement, it) },
        viewed = viewed,
        ctaLarge = placement.ctaLarge,
        onScreen = onScreen,
    )
}

/** The size a cell of [format] is drawn at: card or medium; other formats do not go in a cell. */
internal fun requireCellFormat(format: AdFormat): NativeAdSize = when (format) {
    AdFormat.NATIVE_IN_COLLECTION -> NativeAdSize.CARD
    AdFormat.NATIVE_MEDIUM -> NativeAdSize.MEDIUM
    // Not exhaustive on purpose: other formats do not belong in a cell.
    else -> throw IllegalArgumentException("$format is not drawn by NativeAdCell")
}

/**
 * One cell of a pool, testable without [AdsController] (like [NativeAdSlotHost]): collects [states] while in
 * composition and [onScreen], draws them with [NativeAdSlotContent], and reports the opportunity of P-07 through one
 * [PoolCellOpportunity]: each change of the state or of [viewed] is passed to it; leaving the composition, or the
 * screen ([onScreen] turning false), ends it and gives the ad back (P-01, P-01a). Back on screen it is a new cell that
 * claims again. A pager keeps the pages it built after they scroll away, so its pages pass [onScreen] =
 * `pagerState.isPageVisible(page)`; a lazy list or grid disposes its rows, and other cells keep the default.
 * With [keepSpaceWhenEmpty], no ad keeps the cell's height: a static placeholder when [placeholderWhenEmpty] (a cell
 * of a collection), an empty space otherwise (a slot next to an app button). [contentModifier] wraps the placeholder
 * and the ad only, so a cell with no ad takes no space at all.
 */
@Composable
internal fun NativeAdCellHost(
    states: Flow<NativeAdState>,
    size: NativeAdSize,
    viewed: Boolean,
    keepSpaceWhenEmpty: Boolean,
    onOpportunity: (result: String) -> Unit,
    modifier: Modifier = Modifier,
    ctaLarge: Boolean = false,
    placeholderWhenEmpty: Boolean = true,
    contentModifier: Modifier = Modifier,
    onStateChange: (NativeAdState) -> Unit = {},
    onScreen: Boolean = true,
    adContent: @Composable (LoadedNativeAd) -> Unit,
) {
    if (!onScreen) {
        // Off screen: the cell below leaves the composition, so its collection ends and its ad goes back to the pool.
        Box(modifier = modifier)
        return
    }
    NativeAdCellOnScreen(
        states = states,
        size = size,
        viewed = viewed,
        keepSpaceWhenEmpty = keepSpaceWhenEmpty,
        onOpportunity = onOpportunity,
        modifier = modifier,
        ctaLarge = ctaLarge,
        placeholderWhenEmpty = placeholderWhenEmpty,
        contentModifier = contentModifier,
        onStateChange = onStateChange,
        adContent = adContent,
    )
}

/** The cell of [NativeAdCellHost] while it is on screen. */
@Composable
private fun NativeAdCellOnScreen(
    states: Flow<NativeAdState>,
    size: NativeAdSize,
    viewed: Boolean,
    keepSpaceWhenEmpty: Boolean,
    onOpportunity: (result: String) -> Unit,
    modifier: Modifier,
    ctaLarge: Boolean,
    placeholderWhenEmpty: Boolean,
    contentModifier: Modifier,
    onStateChange: (NativeAdState) -> Unit,
    adContent: @Composable (LoadedNativeAd) -> Unit,
) {
    val state by states.collectAsState(initial = NativeAdState.Loading)
    val currentOnOpportunity by rememberUpdatedState(onOpportunity)
    val currentOnStateChange by rememberUpdatedState(onStateChange)
    val opportunity = remember { PoolCellOpportunity { currentOnOpportunity(it) } }
    // The value of this composition, not the delegate: the effect may run after a newer state arrived.
    val shown = state
    LaunchedEffect(shown) { currentOnStateChange(shown) }
    LaunchedEffect(shown, viewed) { opportunity.onState(hasAd = shown is NativeAdState.Loaded, viewed = viewed) }
    DisposableEffect(opportunity) {
        onDispose { opportunity.onLeave() }
    }
    if (shown == NativeAdState.Unavailable && keepSpaceWhenEmpty && placeholderWhenEmpty) {
        Box(modifier = modifier) {
            Box(modifier = contentModifier) { NativeAdPlaceholder(size, animated = false, ctaLarge = ctaLarge) }
        }
        return
    }
    NativeAdSlotContent(
        state = shown,
        size = size,
        keepSpaceWhenEmpty = keepSpaceWhenEmpty,
        modifier = modifier,
        ctaLarge = ctaLarge,
        contentModifier = contentModifier,
        adContent = adContent,
    )
}

/** P-07: the page of a pager counts as seen once it settled: no scroll in progress and it is the settled page. */
fun PagerState.isPageSettled(page: Int): Boolean = !isScrollInProgress && settledPage == page

/**
 * Whether [page] is on screen now, at least in part. A pager keeps the pages it built after they scroll away, so a
 * [NativeAdCell] of a page passes it as `onScreen`: a page that leaves the screen gives its ad back (P-01).
 */
fun PagerState.isPageVisible(page: Int): Boolean = layoutInfo.visiblePagesInfo.any { it.index == page }
