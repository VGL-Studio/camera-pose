package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Whether the collapsible layer is collapsed, and since when ([collapsedAtMillis], on the monotonic clock of the ad
 * rules): the timer that opens it again counts from that mark (CL-03). It lives outside the ad stream, so a refreshed
 * ad never reads or changes it (CL-04, L-A12).
 */
@Stable
class CollapsibleAdState internal constructor(collapsed: Boolean, collapsedAtMillis: Long? = null) {
    var collapsed by mutableStateOf(collapsed)
        private set

    var collapsedAtMillis by mutableStateOf(collapsedAtMillis)
        private set

    /** Collapses at [nowMillis]; collapsing again keeps the first mark. */
    fun collapse(nowMillis: Long) {
        if (collapsed) return
        collapsed = true
        collapsedAtMillis = nowMillis
    }

    /** Opens again (CL-03) and clears the mark. */
    internal fun expand() {
        collapsed = false
        collapsedAtMillis = null
    }

    internal companion object {
        private const val NO_MARK = Long.MIN_VALUE

        val Saver: Saver<CollapsibleAdState, Any> = listSaver(
            save = { listOf(it.collapsed, it.collapsedAtMillis ?: NO_MARK) },
            restore = { CollapsibleAdState(it[0] as Boolean, (it[1] as Long).takeIf { mark -> mark != NO_MARK }) },
        )
    }
}

/**
 * The collapsed state of the collapsible layer and its mark, kept for the screen entry: a configuration change or
 * coming back to the screen from the back stack keeps both, so the timer is not started again; a new entry of the
 * screen starts open.
 */
@Composable
fun rememberCollapsibleAdState(): CollapsibleAdState =
    rememberSaveable(saver = CollapsibleAdState.Saver) { CollapsibleAdState(collapsed = false) }

/**
 * The open collapsible layer with fixed content, for screenshots and previews (the app shows real ads in the bottom
 * bar, [BottomAdBarScaffold]): 299 dp high on a 411 x 891 dp window, a 27 dp collapse button in the top start corner,
 * a marker where the SDK pins AdChoices in the top end corner. Sized like [CollapsibleNativeAdLayout]: on a window
 * with no room for the layer (landscape phone, split screen) it draws nothing, as the real layer.
 */
@Composable
fun CollapsibleNativeAdCard(assets: NativeAdAssets, onCollapse: () -> Unit, modifier: Modifier = Modifier) {
    CollapsibleNativeAdLayout(
        assets = assets,
        onCollapse = onCollapse,
        binder = PreviewNativeAdBinder,
        showAdChoicesMarker = true,
        modifier = modifier,
    )
}

/** Sizes of the open layer, from design/context/AdGalleryScreen_collapsible-expanded_light.md (design T-064). */
internal object CollapsibleNativeAdDimens {
    val CollapseButtonSize = 27.dp
    val CollapseIconSize = 17.dp
    val CollapseButtonInset = 4.dp

    /** Margin of the media inside its area: top and sides, none below (the small native content follows). */
    val MediaMargin = 12.dp

    /** Where the SDK pins AdChoices (ADCHOICES_TOP_RIGHT): nothing to tap in this corner. */
    val AdChoicesCorner = 24.dp
}

internal object CollapsibleNativeAdTestTags {
    const val AD = "collapsible_ad"
    const val MEDIA = "collapsible_ad_media"
    const val COLLAPSE = "collapsible_ad_collapse"
    const val CONTENT = "collapsible_ad_content"
}

/** The 40% black of the design behind the collapse icon, from the scrim token (like the network dialog). */
private const val COLLAPSE_BUTTON_ALPHA = 0.4f

/** White on the 40% scrim, from the design; no colour role of the scheme is white in both themes (plan T-030, Q8). */
private val CollapseIconTint = Color.White

/**
 * The open layer (design T-064), sized by [NativeAdDimens.collapsibleLayer] of the window: the media area (the
 * MediaView 12 dp in at the top and sides: 161 dp in a 173 dp area at 411 x 891 dp) over the content of the small
 * native ([SmallLayout], as high as the bottom slot); 299 dp at 411 x 891 dp. With its 16 dp bottom margin it never
 * takes more than half the window height: the media area shrinks first (MediaView 146 dp at 411 x 600 dp, never below
 * 120 dp); a window with no room draws nothing. The content follows the rules of small at large text
 * (its text column never goes past the slot), so the layer keeps its height at every font scale. The collapse button
 * floats over the top start corner. Each ad part goes through [binder], so a fake ad and a real ad bound to the SDK
 * share it. The collapse button is not an ad asset: a tap on it is not a click on the ad. AdChoices is drawn by the
 * SDK in the top end corner, never in Compose.
 */
@Composable
internal fun CollapsibleNativeAdLayout(
    assets: NativeAdAssets,
    onCollapse: () -> Unit,
    binder: NativeAdBinder,
    showAdChoicesMarker: Boolean,
    modifier: Modifier = Modifier,
) {
    val layer = NativeAdDimens.collapsibleLayer(nativeScreenSize()) ?: return
    val slotHeight = layer.slot
    NativeAdFrame(
        modifier = modifier
            .fillMaxWidth()
            .testTag(CollapsibleNativeAdTestTags.AD),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(layer.mediaArea)
                    .padding(
                        start = CollapsibleNativeAdDimens.MediaMargin,
                        top = CollapsibleNativeAdDimens.MediaMargin,
                        end = CollapsibleNativeAdDimens.MediaMargin,
                    ),
            ) {
                NativeAdMediaArea(binder, Modifier.fillMaxSize().testTag(CollapsibleNativeAdTestTags.MEDIA))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(slotHeight)
                    .testTag(CollapsibleNativeAdTestTags.CONTENT),
            ) {
                SmallLayout(assets, binder, slotHeight)
            }
        }
        if (showAdChoicesMarker) AdChoicesMarker()
        CollapseButton(onCollapse)
    }
}

/**
 * A 27 dp round button, 4 dp from the top start corner, drawn and laid out at 27 dp: Compose widens its touch area to
 * 48 dp without moving anything (as the close button of the native full screen, plan T-027).
 */
@Composable
private fun BoxScope.CollapseButton(onCollapse: () -> Unit) {
    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(
                top = CollapsibleNativeAdDimens.CollapseButtonInset,
                start = CollapsibleNativeAdDimens.CollapseButtonInset,
            )
            .size(CollapsibleNativeAdDimens.CollapseButtonSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = COLLAPSE_BUTTON_ALPHA))
            .clickable(role = Role.Button, onClick = onCollapse)
            .testTag(CollapsibleNativeAdTestTags.COLLAPSE),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ads_ic_collapse),
            contentDescription = stringResource(R.string.ads_collapse),
            tint = CollapseIconTint,
            modifier = Modifier.size(CollapsibleNativeAdDimens.CollapseIconSize),
        )
    }
}

/** Where the SDK pins AdChoices on this layer; drawn on fake ads only, so screenshots match the design. */
@Composable
private fun BoxScope.AdChoicesMarker() {
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = Spacing.xs, end = Spacing.xs)
            .size(NativeAdDimens.AdChoicesSize)
            .clip(RoundedCornerShape(NativeAdDimens.BadgeCorner))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .testTag(NativeAdTestTags.AD_CHOICES_AREA),
    )
}

/**
 * The collapsible layer of a bottom bar for the ad states of [states], testable without an ads controller.
 *
 * - [states] is collected while in composition, also while collapsed, so the ad is kept to be shown again; leaving
 *   the composition destroys it (L-061). Loading and no ad draw nothing: the slot below stays seen (CL-01, D-04).
 * - Open ([state]) with an ad: [renderAd] draws it, one frame per ad (a refreshed ad is bound in a fresh frame). A tap
 *   on the collapse button collapses at [clock]'s time and calls [onCollapsed] once.
 * - Collapsed: nothing is drawn. [expandAfterSeconds] after the mark it opens again ([collapsibleReexpandAtMillis]),
 *   or as soon as an ad is there (CL-03), and calls [onReexpanded]; 0 never. The wait is worked out again from the
 *   kept mark each time this runs, so a restored state goes on with the time left.
 * - [allowed] false (no ad unit for the layer): nothing collected, nothing sent.
 * - [hasRoom] false (must match the real window; the window has no room for the layer, [NativeAdDimens.collapsibleLayer]; CL-05): as not
 *   [allowed]: nothing collected (no ad taken from the stock, no request), nothing drawn, no timer and no
 *   [onReexpanded]. [state] stays as it is, so an open layer opens again with room, and a collapsed one keeps its mark:
 *   with room again the timer goes on from the mark (due already: open at once with an ad). Losing room while
 *   collecting stops the collection, which destroys the ad held (L-061).
 */
@Composable
internal fun CollapsibleNativeAdLayerHost(
    states: Flow<NativeAdState>,
    expandAfterSeconds: Long,
    clock: MonotonicClock,
    state: CollapsibleAdState,
    onCollapsed: () -> Unit,
    onReexpanded: () -> Unit,
    modifier: Modifier = Modifier,
    allowed: Boolean = true,
    hasRoom: Boolean = NativeAdDimens.collapsibleLayer(nativeScreenSize()) != null,
    renderAd: @Composable (ad: LoadedNativeAd, onCollapse: () -> Unit) -> Unit = { ad, onCollapse ->
        BoundNativeAd(ad) { binder ->
            CollapsibleNativeAdLayout(ad.assets, onCollapse, binder, showAdChoicesMarker = false)
        }
    },
) {
    if (!allowed || !hasRoom) {
        Box(modifier = modifier)
        return
    }
    val adState by states.collectAsState(initial = NativeAdState.Loading)
    val current = adState
    val hasAd by rememberUpdatedState(current is NativeAdState.Loaded)
    val currentOnCollapsed by rememberUpdatedState(onCollapsed)
    val currentOnReexpanded by rememberUpdatedState(onReexpanded)
    LaunchedEffect(state.collapsedAtMillis, expandAfterSeconds) {
        val due = collapsibleReexpandAtMillis(state.collapsedAtMillis, expandAfterSeconds) ?: return@LaunchedEffect
        delay((due - clock.nowMillis()).coerceAtLeast(0))
        snapshotFlow { shouldReexpand(clock.nowMillis(), due, hasAd) }.first { it }
        state.expand()
        currentOnReexpanded()
    }
    val onCollapse: () -> Unit = remember(state, clock) {
        {
            if (!state.collapsed) {
                state.collapse(clock.nowMillis())
                currentOnCollapsed()
            }
        }
    }
    Box(modifier = modifier) {
        if (current is NativeAdState.Loaded && !state.collapsed) {
            key(current.ad) { renderAd(current.ad, onCollapse) }
        }
    }
}

/**
 * The collapsible layer of the bottom bar of [placement] (D-01: it exists only there), drawn by [BottomAdBarScaffold]
 * over the bottom slot. Its ads are format [AdFormat.NATIVE_COLLAPSIBLE] of the bar's placement, from the
 * `native_collap` stock shared by every bottom bar with the same ad unit (KB-33): its first ad shows at once when the
 * stock holds one, then it refreshes at the interval of `time_reload_collap_ad`, staying collapsed (CL-02, KB-34);
 * the bar checks consent and the placement first. On a window with no room for it (CL-05: 299 dp at 411 x 891 dp, the
 * layer with its margin at most half the window) it takes no ad, reports no display opportunity and logs one line;
 * its state is kept, so it opens again when the window has room.
 */
@Composable
internal fun CollapsibleNativeAdLayer(placement: AdPlacement, expandAfterSeconds: Long, modifier: Modifier = Modifier) {
    val ads = LocalAdsController.current ?: return
    // Outside any branch that depends on the ad: a new ad never opens it again.
    val state = rememberCollapsibleAdState()
    val unitId = remember(placement) { ads.unitId(placement, AdFormat.NATIVE_COLLAPSIBLE) }
    val window = nativeScreenSize()
    val hasRoom = NativeAdDimens.collapsibleLayer(window) != null
    // `af_native_ad_eligible` once each time the layer comes in: the bar checked consent and its key (plan T-033); a
    // window with no room is no display opportunity (CL-05, MC-14).
    ReportNativeEligible(unitId != null && hasRoom)
    val currentWindow by rememberUpdatedState(window)
    LaunchedEffect(hasRoom) {
        if (unitId != null && !hasRoom) ads.onCollapsibleNoRoom(placement, currentWindow)
    }
    // Collected while in composition; leaving it cancels the collection, which destroys the ad.
    val states = remember(placement) { ads.nativeAd(placement, AdFormat.NATIVE_COLLAPSIBLE) }
    CollapsibleNativeAdLayerHost(
        states = states,
        expandAfterSeconds = expandAfterSeconds,
        clock = ads.adsClock,
        state = state,
        onCollapsed = { ads.onNativeCollapsed(placement) },
        onReexpanded = { ads.onNativeReexpanded(placement, expandAfterSeconds) },
        modifier = modifier,
        allowed = unitId != null,
        hasRoom = hasRoom,
    )
}

/** The log line of a collapsible layer with no room on its window (ADR-0019, CL-05). */
internal fun collapsibleNoRoomLog(placement: AdPlacement, window: DpSize): String =
    "Collapsible ${placement.key} not shown: window ${window.width.value.roundToInt()}x" +
        "${window.height.value.roundToInt()} dp has no room"

@Preview
@Composable
private fun CollapsibleNativeAdCardPreview() {
    val assets = NativeAdAssets(
        headline = "Headline of the ad",
        advertiser = "Advertiser",
        body = null,
        callToAction = "Install",
        icon = null,
        hasMedia = true,
    )
    AppTheme {
        CollapsibleNativeAdCard(assets, onCollapse = {})
    }
}
