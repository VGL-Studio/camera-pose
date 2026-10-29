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

/** What the bottom slot of a bottom bar draws (D-01): the 80 dp native small, or the fixed banner. */
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
 */
internal fun bottomAdBarLayers(
    canRequestAds: Boolean,
    enabled: Boolean,
    flags: AdsGlobalFlags,
    bannerHadAd: Boolean,
): BottomAdBarLayers? {
    if (!bottomAdBarAllowed(canRequestAds, enabled, bannerHadAd)) return null
    if (!canRequestAds) return BottomAdBarLayers(BottomSlotKind.BANNER, collapsible = false)
    return bottomAdBarLayers(allowed = true, flags = flags)
}

/**
 * The space the content of the screen keeps at the bottom (D-04): only once the slot has an ad; the native slot with
 * its bottom margin ([NativeAdDimens.BottomSlotHeight], AR-02), the banner its own height. The floating layer keeps
 * none.
 */
internal fun bottomBarReservation(slot: BottomSlotKind?, slotHasAd: Boolean, bannerHeight: Dp): Dp = when {
    slot == null || !slotHasAd -> 0.dp
    slot == BottomSlotKind.NATIVE -> NativeAdDimens.BottomSlotHeight + BottomAdBarDimens.SlotMargin
    else -> bannerHeight
}

/**
 * The first refresh of the native bottom slot (section 5.7): 500 ms after the one of the collapsible layer above it,
 * so both requests are not sent together; the plain 60 s without the layer.
 */
internal fun bottomSlotFirstRefreshDelayMillis(collapsible: Boolean): Long = NativeAdPolicy.REFRESH_INTERVAL_MILLIS +
    if (collapsible) NativeAdPolicy.BOTTOM_REFRESH_DELAY_AFTER_COLLAPSIBLE_MILLIS else 0L

/** Sizes of the bottom bar, from design/context/AdGalleryScreen_collapsible-*.md. */
object BottomAdBarDimens {
    /** Start, end and bottom margin of the native slot and of the collapsible layer; the banner has none. */
    val SlotMargin: Dp = Spacing.md
}

/** Values for screens and tests that draw a bottom bar with fake ads ([BottomAdBarLayout]). */
object BottomAdBarDefaults {
    /** The space kept for the native slot with an ad: the 80 dp slot and its 16 dp margin below (D-04). */
    val nativeReservation: Dp = NativeAdDimens.BottomSlotHeight + BottomAdBarDimens.SlotMargin
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
 * - The bottom slot is native small (80 dp) with `is_show_native_bottom`, the fixed banner otherwise; with
 *   `is_show_native_collap` too, the 250 dp collapsible layer floats over the slot, opens at its first ad, collapses on
 *   a tap and opens again `time_expand_collap_ad` seconds later (CL-01 to CL-04). The layer is 250 dp at the default
 *   font scale and grows with large text, so its media keeps 120 dp (272 dp at font scale 2.0, plan T-045).
 * - [content] gets the slot's height as bottom padding once the slot has an ad, never for the layer (D-04), so a
 *   taller layer covers more of the content but moves nothing.
 * - Nothing is drawn, and nothing kept, without consent or with `is_show_<code>` off; tests and previews (no ads
 *   controller) draw only the content. Turning the bar on or off never rebuilds [content].
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
        nativeSlot = { delay, onAvailability, slotModifier ->
            BottomNativeSlot(placement, delay, onAvailability, slotModifier)
        },
        bannerSlot = { onAvailability, onHeight, slotModifier ->
            BottomBannerSlot(placement, onAvailability, onHeight, slotModifier)
        },
        layer = { layerModifier -> CollapsibleNativeAdLayer(placement, bar.expandAfterSeconds, layerModifier) },
        modifier = modifier,
        onLayersChange = { ads?.onBottomBarLayers(placement, it) },
        onSlotHasAdChange = bar.choice.onSlotHasAdChange,
        content = content,
    )
}

/** What the bar of [placement] shows now, read from [ads]; nothing without a controller (tests, previews). */
private class BottomAdBarState(val choice: BottomAdBarChoice, val expandAfterSeconds: Long)

@Composable
private fun rememberBottomAdBarState(ads: AdsController?, placement: AdPlacement): BottomAdBarState {
    if (ads == null) return BottomAdBarState(BottomAdBarChoice(layers = null, onSlotHasAdChange = {}), 0)
    val canRequestAds by ads.canRequestAds.collectAsStateWithLifecycle()
    val enabled by remember(placement) { ads.isEnabled(placement) }.collectAsStateWithLifecycle(initialValue = false)
    val flags by ads.globalFlags.collectAsStateWithLifecycle()
    return BottomAdBarState(rememberBottomAdBarLayers(canRequestAds, enabled, flags), flags.expandCollapAfterSeconds)
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
): BottomAdBarChoice {
    var bannerHadAd by remember { mutableStateOf(false) }
    val layers = bottomAdBarLayers(canRequestAds, enabled, flags, bannerHadAd)
    return BottomAdBarChoice(layers) { hasAd -> bannerHadAd = hasAd && layers?.slot == BottomSlotKind.BANNER }
}

/**
 * The bar for [layers], testable without an ads controller: [nativeSlot], [bannerSlot] and [layer] draw each layer.
 * The space kept follows the availability the slot reports, changed only when it differs (a refreshed slot reports
 * true again on each new ad); it starts at none for each new slot, also after the activity is rebuilt, since the ad
 * does not survive it. [onLayersChange] and [onSlotHasAdChange] get each change.
 */
@Composable
internal fun BottomAdBarHost(
    layers: BottomAdBarLayers?,
    nativeSlot: @Composable (firstRefreshDelayMillis: Long, onAvailability: (Boolean) -> Unit, Modifier) -> Unit,
    bannerSlot: @Composable (onAvailability: (Boolean) -> Unit, onHeight: (Dp) -> Unit, Modifier) -> Unit,
    layer: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    onLayersChange: (BottomAdBarLayers?) -> Unit = {},
    onSlotHasAdChange: (Boolean) -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val kind = layers?.slot
    var slotHasAd by remember(kind) { mutableStateOf(false) }
    var bannerHeight by remember { mutableStateOf(0.dp) }
    val currentOnLayersChange by rememberUpdatedState(onLayersChange)
    val currentOnSlotHasAdChange by rememberUpdatedState(onSlotHasAdChange)
    LaunchedEffect(layers) { currentOnLayersChange(layers) }
    LaunchedEffect(slotHasAd) { currentOnSlotHasAdChange(slotHasAd) }
    val onAvailability: (Boolean) -> Unit = { available -> if (available != slotHasAd) slotHasAd = available }
    val slot: (@Composable (Modifier) -> Unit)? = when (kind) {
        null -> null
        BottomSlotKind.NATIVE -> { slotModifier ->
            key(kind) {
                nativeSlot(bottomSlotFirstRefreshDelayMillis(layers.collapsible), onAvailability, slotModifier)
            }
        }
        BottomSlotKind.BANNER -> { slotModifier ->
            key(kind) { bannerSlot(onAvailability, { bannerHeight = it }, slotModifier) }
        }
    }
    BottomAdBarLayout(
        reservation = bottomBarReservation(kind, slotHasAd, bannerHeight),
        slot = slot,
        layer = if (layers?.collapsible == true) layer else null,
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
