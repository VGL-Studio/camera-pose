package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow

/**
 * Shows the native ad of [placement] (small or medium) in a screen. Features call this, never an ads SDK directly.
 * It takes no space until ads are allowed (consent given, SDK started, placement switched on remotely). While the ad
 * loads, a placeholder keeps its height; when no ad could be loaded, the space shrinks to 0 dp (D-02), or
 * is kept empty when [keepSpaceWhenEmpty] (a placement next to an app button, so the button does not move). The ad is
 * destroyed when the slot leaves the screen. A medium ad comes from the pool of its placement (plan T-028): the slot
 * takes it when it comes on screen and gives it back when it leaves, and records its opportunity (P-07).
 * [onAdAvailabilityChange] tells the app whether the slot has an ad (D-02): true when an ad arrives, false when none
 * could be loaded, or when ads stop being allowed after an ad was shown. Screenshots pass a [NativeAdCard] instead,
 * since tests have no ads controller.
 */
@Composable
fun NativeAdSlot(
    placement: AdPlacement,
    modifier: Modifier = Modifier,
    keepSpaceWhenEmpty: Boolean = false,
    onAdAvailabilityChange: (Boolean) -> Unit = {},
) {
    val size = requireNativeFormat(placement.format)
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
        // A cold flow: nothing loads until the gate opens and the host collects it.
        states = remember(placement) { ads.nativeAd(placement) },
        size = size,
        keepSpaceWhenEmpty = keepSpaceWhenEmpty,
        onAdAvailabilityChange = onAdAvailabilityChange,
        modifier = modifier,
        adContent = { BoundNativeAd(it, size, ctaLarge = placement.ctaLarge) },
        // A pooled slot is one cell of its pool: same opportunity as a cell of a collection (P-07, L-068).
        onPoolOpportunity = if (placement.format.usesPool) {
            { result -> ads.recordPoolCellOpportunity(placement, result) }
        } else {
            null
        },
        ctaLarge = placement.ctaLarge,
        placeholderWhenEmpty = false,
    )
}

/** A native slot is drawn only with consent, the placement switched on remotely, and an ad unit (KB-1). */
internal fun nativeSlotAllowed(canRequestAds: Boolean, enabled: Boolean, unitId: String?): Boolean =
    canRequestAds && enabled && unitId != null

/**
 * Draws the slot while [allowed], nothing otherwise, and reports availability to the app: true on an ad, false on
 * no ad, never while loading; and false when [allowed] turns false after true was reported (L-061).
 * [onTakesSpaceChange] gets whether the slot takes space now, at each state: true while it loads (the placeholder
 * shows then; [placeholderWhenEmpty] only chooses what fills the kept space of a pool cell with no ad), while
 * the ad shows, and with no ad when [keepSpaceWhenEmpty] keeps the space; false once it draws nothing (no ad without
 * [keepSpaceWhenEmpty], or no longer [allowed]). [placeholderWhileLoading] false (the native slot of a bottom bar
 * coming back after the collapsible layer when it had shrunk to 0, D-07, plan T-090): nothing drawn while it loads,
 * and no space taken until its ad arrives. A bottom bar keeps exactly that space for the content (D-04). With
 * [onPoolOpportunity] the slot is a cell of a pool ([NativeAdCellHost]): it records the opportunity of P-07, seen
 * while [viewed]; [cardHeight] is the height of its card placeholders (the caller passes it to the card it draws).
 */
@Composable
internal fun NativeAdSlotGate(
    allowed: Boolean,
    states: Flow<NativeAdState>,
    size: NativeAdSize,
    keepSpaceWhenEmpty: Boolean,
    onAdAvailabilityChange: (Boolean) -> Unit,
    modifier: Modifier,
    onPoolOpportunity: ((result: String) -> Unit)? = null,
    viewed: Boolean = true,
    ctaLarge: Boolean = false,
    placeholderWhenEmpty: Boolean = true,
    onScreen: Boolean = true,
    cardHeight: Dp? = null,
    onTakesSpaceChange: (Boolean) -> Unit = {},
    placeholderWhileLoading: Boolean = true,
    adContent: @Composable (LoadedNativeAd) -> Unit,
) {
    // `af_native_ad_eligible` once each time the slot comes on screen allowed (plan T-033): a page of a pager kept in
    // composition off screen does not count, and coming back on screen counts again.
    ReportNativeEligible(allowed && onScreen)
    val currentOnChange by rememberUpdatedState(onAdAvailabilityChange)
    val currentOnTakesSpace by rememberUpdatedState(onTakesSpaceChange)
    val reported = remember { AvailabilityReport() }
    if (!allowed) {
        SideEffect {
            if (reported.available) {
                reported.available = false
                currentOnChange(false)
            }
            if (reported.takesSpace) {
                reported.takesSpace = false
                currentOnTakesSpace(false)
            }
        }
        Box(modifier = modifier)
        return
    }
    val onStateChange: (NativeAdState) -> Unit = { state ->
        val available = when (state) {
            NativeAdState.Loading -> null
            is NativeAdState.Loaded -> true
            NativeAdState.Unavailable -> false
        }
        if (available != null) {
            reported.available = available
            currentOnChange(available)
        }
        val takesSpace = (available ?: placeholderWhileLoading) || keepSpaceWhenEmpty
        if (takesSpace != reported.takesSpace) {
            reported.takesSpace = takesSpace
            currentOnTakesSpace(takesSpace)
        }
    }
    if (onPoolOpportunity != null) {
        NativeAdCellHost(
            states = states,
            size = size,
            viewed = viewed,
            keepSpaceWhenEmpty = keepSpaceWhenEmpty,
            onOpportunity = onPoolOpportunity,
            modifier = modifier,
            ctaLarge = ctaLarge,
            placeholderWhenEmpty = placeholderWhenEmpty,
            onStateChange = onStateChange,
            onScreen = onScreen,
            cardHeight = cardHeight,
            adContent = adContent,
        )
        return
    }
    NativeAdSlotHost(
        states = states,
        size = size,
        keepSpaceWhenEmpty = keepSpaceWhenEmpty,
        modifier = modifier,
        onStateChange = onStateChange,
        placeholderWhileLoading = placeholderWhileLoading,
        adContent = adContent,
    )
}

/** The last availability and space reported by [NativeAdSlotGate]; not state, nothing is drawn from it. */
private class AvailabilityReport {
    var available = false
    var takesSpace = false
}

/** The size a native placement is drawn at; other formats are not drawn by [NativeAdSlot]. */
internal fun requireNativeFormat(format: AdFormat): NativeAdSize = when (format) {
    AdFormat.NATIVE_SMALL -> NativeAdSize.SMALL
    AdFormat.NATIVE_MEDIUM -> NativeAdSize.MEDIUM
    // Not exhaustive on purpose: new formats (collapsible, full screen) do not belong in this slot.
    else -> throw IllegalArgumentException("$format is not drawn by NativeAdSlot")
}

/**
 * Collects [states] while in composition (not by lifecycle: the ad loads again only when the screen is entered
 * again). Leaving the composition cancels the collection, which destroys the ad. [onStateChange] gets each new state.
 * [placeholderWhileLoading] false draws nothing while loading.
 */
@Composable
internal fun NativeAdSlotHost(
    states: Flow<NativeAdState>,
    size: NativeAdSize,
    keepSpaceWhenEmpty: Boolean,
    modifier: Modifier,
    onStateChange: (NativeAdState) -> Unit = {},
    placeholderWhileLoading: Boolean = true,
    adContent: @Composable (LoadedNativeAd) -> Unit,
) {
    val state by states.collectAsState(initial = NativeAdState.Loading)
    val currentOnStateChange by rememberUpdatedState(onStateChange)
    // The value of this composition, not the delegate: the effect may run after a newer state arrived.
    val shown = state
    LaunchedEffect(shown) { currentOnStateChange(shown) }
    NativeAdSlotContent(
        shown,
        size,
        keepSpaceWhenEmpty,
        modifier,
        placeholderWhileLoading = placeholderWhileLoading,
        adContent = adContent,
    )
}

/**
 * What the slot draws for [state]; [ctaLarge] sizes the placeholder and the kept space of a medium ad with the large
 * call to action. [contentModifier] wraps the placeholder and the ad, never the empty state. [cardHeight] is the height
 * of the loading placeholder of a card. [placeholderWhileLoading] false draws nothing while loading (plan T-090).
 */
@Composable
internal fun NativeAdSlotContent(
    state: NativeAdState,
    size: NativeAdSize,
    keepSpaceWhenEmpty: Boolean,
    modifier: Modifier,
    ctaLarge: Boolean = false,
    contentModifier: Modifier = Modifier,
    cardHeight: Dp? = null,
    placeholderWhileLoading: Boolean = true,
    adContent: @Composable (LoadedNativeAd) -> Unit,
) {
    Box(modifier = modifier) {
        when (state) {
            NativeAdState.Loading ->
                if (placeholderWhileLoading) {
                    Box(contentModifier) { NativeAdPlaceholder(size, ctaLarge = ctaLarge, cardHeight = cardHeight) }
                }
            // A new NativeAdView for each ad: the binder registers a view once, so a swapped ad is bound only
            // in a fresh frame (refresh in place, plan T-040).
            is NativeAdState.Loaded -> Box(contentModifier) { key(state.ad) { adContent(state.ad) } }
            NativeAdState.Unavailable ->
                if (keepSpaceWhenEmpty) {
                    // Small and the step card follow the font scale, like the card and its placeholder (plans
                    // T-045, T-109); a card of this app takes the height of its grid cell (plan T-080).
                    val height = if (size == NativeAdSize.CARD && cardHeight != null) {
                        cardHeight
                    } else {
                        nativeAdHeight(size, ctaLarge)
                    }
                    Spacer(Modifier.fillMaxWidth().height(height))
                }
        }
    }
}

/**
 * The native slot of a bottom bar ([BottomAdBarScaffold], D-02): like [NativeAdSlot] for native small (gate,
 * placeholder as high as the slot, [rememberSmallNativeHeight]: 106 dp at the default font scale, nothing with no ad), reporting whether it takes space
 * ([onTakesSpaceChange]: placeholder or ad, D-04), with its first refresh [firstRefreshExtraMillis] later
 * than the interval of `time_reload_native_small` (section 5.7). The stream is built once per placement: a new extra
 * (the layer turned on or off) applies from the next slot, without destroying the ad or loading again.
 * [placeholderWhileLoading] false: no placeholder while it loads, and no space taken until its ad arrives (the bar
 * builds the slot so when it comes back after the collapsible layer having shrunk to 0, D-07, plan T-090).
 */
@Composable
internal fun BottomNativeSlot(
    placement: AdPlacement,
    firstRefreshExtraMillis: Long,
    onTakesSpaceChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    placeholderWhileLoading: Boolean = true,
) {
    val ads = LocalAdsController.current
    if (ads == null) {
        Box(modifier = modifier)
        return
    }
    val canRequestAds by ads.canRequestAds.collectAsStateWithLifecycle()
    val enabled by remember(placement) { ads.isEnabled(placement) }.collectAsStateWithLifecycle(initialValue = false)
    val unitId = remember(placement) { ads.unitId(placement, AdFormat.NATIVE_SMALL) }
    key(placement) {
        BottomNativeSlotHost(
            allowed = nativeSlotAllowed(canRequestAds, enabled, unitId),
            newStates = { extra -> ads.nativeAd(placement, AdFormat.NATIVE_SMALL, extra) },
            firstRefreshExtraMillis = firstRefreshExtraMillis,
            onAdAvailabilityChange = {},
            modifier = modifier,
            onTakesSpaceChange = onTakesSpaceChange,
            placeholderWhileLoading = placeholderWhileLoading,
        )
    }
}

/** [BottomNativeSlot] without an ads controller: [newStates] builds the stream once, with the extra given then. */
@Composable
internal fun BottomNativeSlotHost(
    allowed: Boolean,
    newStates: (firstRefreshExtraMillis: Long) -> Flow<NativeAdState>,
    firstRefreshExtraMillis: Long,
    onAdAvailabilityChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onTakesSpaceChange: (Boolean) -> Unit = {},
    placeholderWhileLoading: Boolean = true,
    adContent: @Composable (LoadedNativeAd) -> Unit = { BoundNativeAd(it, NativeAdSize.SMALL) },
) {
    // A cold flow: nothing loads until the gate opens and the host collects it.
    val states = remember { newStates(firstRefreshExtraMillis) }
    NativeAdSlotGate(
        allowed = allowed,
        states = states,
        size = NativeAdSize.SMALL,
        keepSpaceWhenEmpty = false,
        onAdAvailabilityChange = onAdAvailabilityChange,
        modifier = modifier,
        onTakesSpaceChange = onTakesSpaceChange,
        placeholderWhileLoading = placeholderWhileLoading,
        adContent = adContent,
    )
}
