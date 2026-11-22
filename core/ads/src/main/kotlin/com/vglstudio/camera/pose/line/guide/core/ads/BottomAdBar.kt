package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing

/** What the bottom slot of a bottom bar draws (D-01): the native small (height by the font scale), or the banner. */
internal enum class BottomSlotKind { NATIVE, BANNER }

/** The layers of a bottom bar: its slot, and whether the collapsible layer floats above it (D-01, D-03). */
internal data class BottomAdBarLayers(val slot: BottomSlotKind, val collapsible: Boolean)

/**
 * Whether the bar of a placement is built (D-04): consent given and `is_show_<code>` on. A banner that already had an
 * ad keeps its frame when consent is withdrawn (B-06), so its bar stays; a placement switched off never does.
 */
internal fun bottomAdBarAllowed(canRequestAds: Boolean, enabled: Boolean, bannerHadAd: Boolean = false): Boolean =
    enabled && (canRequestAds || bannerHadAd)

/**
 * The layers of the bar (D-01, D-04): none when not [allowed]; the native slot with `is_show_native_bottom`, the
 * banner otherwise; the collapsible layer with `is_show_native_collap` (already false without the native slot).
 */
internal fun bottomAdBarLayers(allowed: Boolean, flags: AdsGlobalFlags): BottomAdBarLayers? {
    if (!allowed) return null
    val slot = if (flags.showNativeBottom) BottomSlotKind.NATIVE else BottomSlotKind.BANNER
    return BottomAdBarLayers(slot, collapsible = slot == BottomSlotKind.NATIVE && flags.showNativeCollap)
}

/**
 * The layers of the bar from consent, `is_show_<code>` and whether the banner slot has an ad (D-01, D-04, B-06).
 * Without consent only a banner that had an ad is kept, never the native slot or the layer: nothing may load then.
 * [collapsibleLayer] is the declaration of the placement: false never gives the layer, whatever
 * `is_show_native_collap` says.
 */
internal fun bottomAdBarLayers(
    canRequestAds: Boolean,
    enabled: Boolean,
    flags: AdsGlobalFlags,
    bannerHadAd: Boolean,
    collapsibleLayer: Boolean,
): BottomAdBarLayers? {
    if (!bottomAdBarAllowed(canRequestAds, enabled, bannerHadAd)) return null
    if (!canRequestAds) return BottomAdBarLayers(BottomSlotKind.BANNER, collapsible = false)
    val layers = bottomAdBarLayers(allowed = true, flags = flags) ?: return null
    return if (collapsibleLayer) layers else layers.copy(collapsible = false)
}

/**
 * The space the content of the screen keeps at the bottom (D-04): the space the slot takes now, ad or not, so the
 * content always ends where the slot starts. The native slot, while it takes space ([nativeTakesSpace]: its
 * placeholder from the first load, or its ad), keeps [nativeSlotHeight] with its bottom margin (the height the slot is
 * drawn with, [rememberSmallNativeHeight]; AR-02, plan T-109); none once it draws nothing (a failure, and the retries after it until an ad loads). The banner slot keeps [bannerHeight], the
 * height its frame takes now ([BannerSlotHost]): the frame height from the moment the bar is allowed (while it loads,
 * with an ad, kept by B-06), 0 after a failure before its first ad until an ad loads. The floating layer keeps none.
 */
internal fun bottomBarReservation(
    slot: BottomSlotKind?,
    nativeTakesSpace: Boolean,
    bannerHeight: Dp,
    nativeSlotHeight: Dp,
): Dp = when (slot) {
    null -> 0.dp
    BottomSlotKind.NATIVE -> if (nativeTakesSpace) nativeSlotHeight + BottomAdBarDimens.SlotMargin else 0.dp
    BottomSlotKind.BANNER -> bannerHeight
}

/**
 * What the first refresh count of the native bottom slot adds to the interval of its key (section 5.7, plan T-087):
 * 500 ms with the collapsible layer above it, so both requests are not sent together when both keys are equal; none
 * without the layer.
 */
internal fun bottomSlotFirstRefreshExtraMillis(collapsible: Boolean): Long =
    if (collapsible) NativeAdPolicy.BOTTOM_REFRESH_DELAY_AFTER_COLLAPSIBLE_MILLIS else 0L

/** Sizes of the bottom bar, from design/context/AdGalleryScreen_collapsible-*.md. */
object BottomAdBarDimens {
    /** Start, end and bottom margin of the native slot and of the collapsible layer; the banner has none. */
    val SlotMargin: Dp = Spacing.md
}

/**
 * Values for screens and tests that draw a bottom bar with fake ads ([BottomAdBarLayout]). Read them in composition,
 * under the app's theme: they follow the font scale (`LocalDensity`) and the type scale (`MaterialTheme`).
 */
object BottomAdBarDefaults {
    /**
     * The space kept for the native slot while it shows its placeholder or its ad: the slot ([nativeSlotHeight]) and
     * its 16 dp margin below (D-04): 122 dp at the default font scale on every window, more with larger text (KB-32,
     * plan T-109). A screen sets its own bottom space with this function, never with a fixed number.
     */
    @Composable
    fun nativeReservation(): Dp = bottomBarReservation(
        BottomSlotKind.NATIVE,
        nativeTakesSpace = true,
        bannerHeight = 0.dp,
        nativeSlotHeight = rememberSmallNativeHeight(),
    )

    /**
     * The height of the native slot (KB-32, question 66): 106 dp at the default font scale, whatever the width of the
     * window; more with larger text, so its texts are never cut (plan T-109).
     */
    @Composable
    fun nativeSlotHeight(): Dp = rememberSmallNativeHeight()

    /**
     * The height of the open collapsible layer on this window at the current font scale: its media area and the small
     * native content below it (KB-4, KB-32); 299 dp at 411 x 891 dp, 284 dp at 411 x 600 dp (the layer with its margin
     * reaches half the window) at the default font scale. Null when the window has no room for the layer (landscape
     * phone, split screen, or a low window with large text): draw no layer then.
     */
    @Composable
    fun collapsibleLayerHeight(): Dp? = rememberCollapsibleLayer()?.height
}

internal object BottomAdBarTestTags {
    const val SLOT = "bottom_ad_bar_slot"
    const val LAYER = "bottom_ad_bar_layer"
}

/**
 * The bottom bar, stateless, for screenshots and previews with fake ads ([BottomAdBarScaffold] draws real ones).
 * [content] fills the screen and keeps [reservation] at the bottom; [slot] is pinned at the bottom, [slotMargin] from
 * the edges (0 for a banner); [layer] floats over the slot, 16 dp from the edges. The slot and the layer take only the
 * size they draw (D-03): a tap above them reaches the content, a tap on the layer does not.
 */
@Composable
fun BottomAdBarLayout(
    reservation: Dp,
    slot: (@Composable (Modifier) -> Unit)?,
    layer: (@Composable (Modifier) -> Unit)?,
    modifier: Modifier = Modifier,
    slotMargin: Dp = BottomAdBarDimens.SlotMargin,
    content: @Composable (PaddingValues) -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Always the first child, so turning the bar on or off never moves the content in the composition.
        content(PaddingValues(bottom = reservation))
        if (slot != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = slotMargin, end = slotMargin, bottom = slotMargin)
                    .testTag(BottomAdBarTestTags.SLOT),
            ) {
                slot(Modifier.fillMaxWidth())
            }
        }
        if (layer != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(
                        start = BottomAdBarDimens.SlotMargin,
                        end = BottomAdBarDimens.SlotMargin,
                        bottom = BottomAdBarDimens.SlotMargin,
                    )
                    .testTag(BottomAdBarTestTags.LAYER)
                    // The layer covers the content below it: a tap on it never reaches the screen.
                    .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
            ) {
                layer(Modifier.fillMaxWidth())
            }
        }
    }
}

/**
 * The bottom bar of the screen showing [placement] (MC-08, D-01 to D-05), around the screen's [content]. Features
 * call this, never an ads SDK directly; [placement] is the bar's own placement, declared as native small, and every
 * layer reports its ads under it, so revenue and clicks go to the screen on show (D-05).
 *
 * - The bottom slot is native small (KB-32, question 66: 106 dp at the default font scale on every window, more
 *   with larger text) with `is_show_native_bottom`, the fixed banner otherwise; with `is_show_native_collap` too, the
 *   collapsible layer floats over the slot, opens at its first ad, collapses on a tap and opens again
 *   `time_expand_collap_ad` seconds later (CL-01 to CL-04). The layer is its media area over a small native content
 *   (299 dp at 411 x 891 dp at the default font scale, taller when large text leaves the media below 120 dp; plan
 *   T-109); with its 16 dp margin it never takes more than half the window height, its media shrinking first, and a
 *   window with no room has no layer, only the slot (CL-05).
 * - The slot and the layer start together: the slot loads at once, never waiting for the layer. While the layer shows
 *   its ad (open), the native slot gives way to it (D-07): its ad is destroyed and nothing is drawn, not even the
 *   placeholder; it loads, refreshes and retries nothing, takes nothing from its stock and reports nothing (MC-14),
 *   while a load it sent before goes on for its stock (the exception of O-03). Collapsed, or the layer gone or out
 *   of ads, the slot comes back as a new display opportunity: its placeholder while it loads (none when it had shrunk
 *   to 0), the ad of its stock first (O-01). The banner slot never gives way.
 * - [content] gets the space the slot takes as bottom padding, never the layer's (D-04), so a taller layer covers more
 *   of the content but moves nothing: from the moment the bar is allowed, while the slot loads (native placeholder,
 *   banner frame) and once it has an ad; 0 after a failure before its first ad, until an ad loads. While the native
 *   slot gives way to the layer the space stays as it was when the layer opened.
 * - A placement declared with `collapsibleLayer = false` never has the floating layer, even with
 *   `is_show_native_collap` on; its slot is as above.
 * - Nothing is drawn, and nothing kept, without consent or with `is_show_<code>` off; tests and previews (no ads
 *   controller) draw only the content. Turning the bar on or off never rebuilds [content].
 * - One anchored ad per screen (MC-12, plan T-091): the bar is an anchored ad, as a fixed or collapsible banner of
 *   [AdSlot]. When the screen has another one, the one that entered the composition first and is allowed runs; the
 *   other builds nothing, keeps no space, reports nothing (MC-14) and logs one warning, until the first one leaves.
 * - In-content ads win over the floating layer (MC-13, plan T-092): on a screen whose [InContentAds] has a placement
 *   on, the bar has its slot only, as with `is_show_native_collap` off, until the screen is left; switched on while
 *   the layer shows, the layer goes at once (its ad destroyed) and the slot comes back as after a collapse (D-07).
 */
@Composable
fun BottomAdBarScaffold(
    placement: AdPlacement,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    require(placement.format == AdFormat.NATIVE_SMALL) {
        "${placement.key} is ${placement.format}: a bottom bar placement is native small"
    }
    val ads = LocalAdsController.current
    val bar = rememberBottomAdBarState(ads, placement)
    BottomAdBarHost(
        layers = bar.choice.layers,
        nativeSlot = { extra, onTakesSpace, slotModifier, placeholderWhileLoading ->
            BottomNativeSlot(placement, extra, onTakesSpace, slotModifier, placeholderWhileLoading)
        },
        bannerSlot = { onAvailability, onHeight, slotModifier ->
            BottomBannerSlot(placement, onAvailability, onHeight, slotModifier)
        },
        layer = { layerModifier, onShowingChange ->
            CollapsibleNativeAdLayer(placement, bar.expandAfterSeconds, layerModifier, onShowingChange)
        },
        modifier = modifier,
        onLayersChange = { ads?.onBottomBarLayers(placement, it) },
        onSlotHasAdChange = bar.choice.onSlotHasAdChange,
        onSlotYieldChange = { ads?.onBottomSlotYield(placement, it) },
        content = content,
    )
}

/** What the bar of [placement] shows now, read from [ads]; nothing without a controller (tests, previews). */
private class BottomAdBarState(val choice: BottomAdBarChoice, val expandAfterSeconds: Long)

@Composable
private fun rememberBottomAdBarState(ads: AdsController?, placement: AdPlacement): BottomAdBarState {
    if (ads == null) return BottomAdBarState(BottomAdBarChoice(layers = null, onSlotHasAdChange = {}), 0)
    val canRequestAds by ads.canRequestAds.collectAsStateWithLifecycle()
    // Null until the first value of `is_show_<code>`: the turn of the screen is not decided before it (MC-12).
    val enabled by remember(placement) { ads.isEnabled(placement) }.collectAsStateWithLifecycle(initialValue = null)
    val flags by ads.globalFlags.collectAsStateWithLifecycle()
    // In-content ads of the screen on (MC-13, plan T-092): no layer; null until known, so the turn waits for it.
    val block = LocalInContentAdBlock.current
    val choice = rememberBottomAdBarLayers(
        canRequestAds,
        enabled == true,
        flags,
        placement.declaration.collapsibleLayer && block == false,
    )
    val allowed = if (enabled == null || block == null) null else choice.layers != null
    // Without the turn of the screen (MC-12, plan T-091) the bar builds no layer and keeps no space, as when off.
    val turn = rememberAnchoredAdTurn(ads, placement, allowed)
    return BottomAdBarState(
        if (turn) choice else BottomAdBarChoice(layers = null, onSlotHasAdChange = choice.onSlotHasAdChange),
        flags.expandCollapAfterSeconds,
    )
}

/** The layers of a bar, and what to call when its slot has an ad or not ([BottomAdBarHost.onSlotHasAdChange]). */
internal class BottomAdBarChoice(val layers: BottomAdBarLayers?, val onSlotHasAdChange: (Boolean) -> Unit)

/**
 * The one place the layers of a bar are decided (L-072), for [BottomAdBarScaffold] and its tests: keeps whether the
 * banner slot has an ad, so a banner with an ad keeps the bar when consent is withdrawn (B-06).
 */
@Composable
internal fun rememberBottomAdBarLayers(
    canRequestAds: Boolean,
    enabled: Boolean,
    flags: AdsGlobalFlags,
    collapsibleLayer: Boolean,
): BottomAdBarChoice {
    var bannerHadAd by remember { mutableStateOf(false) }
    val layers = bottomAdBarLayers(canRequestAds, enabled, flags, bannerHadAd, collapsibleLayer)
    return BottomAdBarChoice(layers) { hasAd -> bannerHadAd = hasAd && layers?.slot == BottomSlotKind.BANNER }
}

/**
 * The bar for [layers], testable without an ads controller: [nativeSlot], [bannerSlot] and [layer] draw each layer.
 * The space kept is the space the slot reports it takes, whatever its availability: the native slot reports whether
 * it takes space (`onTakesSpace`: placeholder or ad), the banner slot the height its frame takes (`onHeight`). Both
 * start at none for each new slot, until the slot reports. [onLayersChange] gets each change of the layers, and
 * [onSlotHasAdChange] each change of whether the banner slot has an ad (B-06).
 *
 * D-07 (plan T-090): [layer] reports whether it shows its ad (`onShowingChange`); while it does, the native slot is
 * not called at all ([bottomSlotYields]), so it leaves the composition (its collection cancelled, its ad destroyed),
 * and the space kept stays as the slot last reported it. When the layer stops showing, [nativeSlot] is called anew,
 * with `placeholderWhileLoading` true when the slot took space as it went, false when it had shrunk to 0 (its first
 * call always true). [onSlotYieldChange] gets each change of whether the slot gives way, not the first false.
 */
@Composable
internal fun BottomAdBarHost(
    layers: BottomAdBarLayers?,
    nativeSlot: @Composable (
        firstRefreshExtraMillis: Long,
        onTakesSpace: (Boolean) -> Unit,
        Modifier,
        placeholderWhileLoading: Boolean,
    ) -> Unit,
    bannerSlot: @Composable (onAvailability: (Boolean) -> Unit, onHeight: (Dp) -> Unit, Modifier) -> Unit,
    layer: @Composable (Modifier, onShowingChange: (Boolean) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    onLayersChange: (BottomAdBarLayers?) -> Unit = {},
    onSlotHasAdChange: (Boolean) -> Unit = {},
    onSlotYieldChange: (Boolean) -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val kind = layers?.slot
    val collapsible = layers?.collapsible == true
    var slotHasAd by remember(kind) { mutableStateOf(false) }
    var nativeTakesSpace by remember(kind) { mutableStateOf(false) }
    var bannerHeight by remember(kind) { mutableStateOf(0.dp) }
    var layerShowing by remember { mutableStateOf(false) }

    // Whether the native slot gave way at least once: from then on, it comes back with the space it kept.
    var slotYielded by remember(kind) { mutableStateOf(false) }
    val yields = bottomSlotYields(kind, collapsible, layerShowing)
    val currentYields by rememberUpdatedState(yields)
    val currentOnLayersChange by rememberUpdatedState(onLayersChange)
    val currentOnSlotHasAdChange by rememberUpdatedState(onSlotHasAdChange)
    val currentOnSlotYieldChange by rememberUpdatedState(onSlotYieldChange)
    val yieldReport = remember { YieldReport() }
    LaunchedEffect(layers) { currentOnLayersChange(layers) }
    LaunchedEffect(slotHasAd) { currentOnSlotHasAdChange(slotHasAd) }
    LaunchedEffect(yields) {
        if (yields) slotYielded = true
        if (yields != yieldReport.yielded) {
            yieldReport.yielded = yields
            currentOnSlotYieldChange(yields)
        }
    }
    val onAvailability: (Boolean) -> Unit = { available -> if (available != slotHasAd) slotHasAd = available }
    val slot: (@Composable (Modifier) -> Unit)? = when (kind) {
        null -> null
        BottomSlotKind.NATIVE -> { slotModifier ->
            key(kind) {
                // Giving way leaves the composition; coming back is a new slot, with a new stream (O-01: its stock
                // first), and the placeholder pinned for this entry from the space kept.
                if (!yields) {
                    val placeholderWhileLoading = remember { !slotYielded || nativeTakesSpace }
                    nativeSlot(
                        bottomSlotFirstRefreshExtraMillis(layers.collapsible),
                        { takesSpace ->
                            // A slot that left gives no report: the space stays as it was while it gives way.
                            if (!currentYields && takesSpace != nativeTakesSpace) nativeTakesSpace = takesSpace
                        },
                        slotModifier,
                        placeholderWhileLoading,
                    )
                }
            }
        }
        BottomSlotKind.BANNER -> { slotModifier ->
            key(kind) { bannerSlot(onAvailability, { bannerHeight = it }, slotModifier) }
        }
    }
    BottomAdBarLayout(
        reservation = bottomBarReservation(kind, nativeTakesSpace, bannerHeight, rememberSmallNativeHeight()),
        slot = slot,
        layer = if (collapsible) {
            { layerModifier -> layer(layerModifier) { showing -> layerShowing = showing } }
        } else {
            null
        },
        modifier = modifier,
        slotMargin = if (kind == BottomSlotKind.BANNER) 0.dp else BottomAdBarDimens.SlotMargin,
        content = content,
    )
}

/** The log line of a change of the layers of the bar of [placement] (ADR-0019). */
internal fun bottomBarLayersLog(placement: AdPlacement, layers: BottomAdBarLayers?): String = if (layers == null) {
    "Bottom bar ${placement.key}: off"
} else {
    "Bottom bar ${placement.key}: slot=${layers.slot} collapsible=${layers.collapsible}"
}

/** The last value [BottomAdBarHost] reported to `onSlotYieldChange`; not state, nothing is drawn from it. */
private class YieldReport {
    var yielded = false
}

/**
 * Whether the bottom slot gives way to the collapsible layer (D-07, plan T-090): only the native slot, with the layer
 * on the bar ([collapsible]) and showing its ad ([layerShowing], [collapsibleShowing]). The banner never does (D-01).
 */
internal fun bottomSlotYields(kind: BottomSlotKind?, collapsible: Boolean, layerShowing: Boolean): Boolean =
    kind == BottomSlotKind.NATIVE && collapsible && layerShowing

/** The log line of the native slot of the bar of [placement] giving way to the layer, or coming back (ADR-0019). */
internal fun bottomSlotYieldLog(placement: AdPlacement, yielded: Boolean): String = if (yielded) {
    "Bottom bar ${placement.key}: slot yields to the collapsible layer"
} else {
    "Bottom bar ${placement.key}: slot back after the collapsible layer"
}
