package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
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
 * bar, [BottomAdBarScaffold]): 250 dp high at the default font scale, a 32 dp collapse button in the top end
 * corner, a marker where the SDK pins AdChoices in the top start corner.
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

/** Sizes of the open layer, from design/context/AdGalleryScreen_collapsible-expanded_light.md. */
internal object CollapsibleNativeAdDimens {
    val LayerHeight = 250.dp
    val CollapseButtonSize = 32.dp
    val CollapseIconSize = 20.dp
    val CollapseButtonInset = 4.dp
    val LayerIconSize = 32.dp
    val LayerCallToActionHeight = 36.dp
    val LayerHeadlineSize = 13.sp

    /** Gap between media, head and call to action. */
    val LayerGap = Spacing.sm

    /** Where the SDK pins AdChoices (ADCHOICES_TOP_LEFT): nothing to tap in this corner. */
    val AdChoicesCorner = 24.dp
}

internal object CollapsibleNativeAdTestTags {
    const val AD = "collapsible_ad"
    const val MEDIA = "collapsible_ad_media"
    const val COLLAPSE = "collapsible_ad_collapse"
}

/** Top and bottom margin inside the border, so the parts add up to the 250 dp of the design (media 134 dp). */
private val LayerInnerVertical = NativeAdDimens.BorderWidth + NativeAdDimens.InnerPadding

/** The 40% black of the design behind the collapse icon, from the scrim token (like the network dialog). */
private const val COLLAPSE_BUTTON_ALPHA = 0.4f

/** White on the 40% scrim, from the design; no colour role of the scheme is white in both themes (plan T-030, Q8). */
private val CollapseIconTint = Color.White

/**
 * The open layer: media filling the height left, head (32 dp icon, 13 sp headline, "Ad" line), 36 dp call to action
 * as wide as the layer; the collapse button over the top end corner. 250 dp at the default font scale; with larger
 * text the media shrinks first, never below 120 dp, then the layer grows (272 dp at font scale 2.0;
 * [MediaShrinksFirstLayout], plan T-045), and the call to action is as high as its line of text. Each ad part goes
 * through [binder], so a fake ad and a real ad bound to the SDK share it. The collapse button is not an ad asset: a
 * tap on it is not a click on the ad. AdChoices is drawn by the SDK in the top start corner, never in Compose.
 */
@Composable
internal fun CollapsibleNativeAdLayout(
    assets: NativeAdAssets,
    onCollapse: () -> Unit,
    binder: NativeAdBinder,
    showAdChoicesMarker: Boolean,
    modifier: Modifier = Modifier,
) {
    NativeAdFrame(
        modifier = modifier
            .fillMaxWidth()
            // 250 dp; more only when the media reaches its 120 dp floor with large text (plan T-045).
            .heightIn(min = CollapsibleNativeAdDimens.LayerHeight)
            .testTag(CollapsibleNativeAdTestTags.AD),
    ) {
        MediaShrinksFirstLayout(
            innerHeight = CollapsibleNativeAdDimens.LayerHeight - LayerInnerVertical * 2,
            gap = CollapsibleNativeAdDimens.LayerGap,
            mediaIndex = 0,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = NativeAdDimens.InnerPadding,
                    top = LayerInnerVertical,
                    end = NativeAdDimens.InnerPadding,
                    bottom = LayerInnerVertical,
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(CollapsibleNativeAdTestTags.MEDIA),
            ) {
                NativeAdMediaArea(binder, Modifier.fillMaxSize())
            }
            LayerHeader(assets, binder)
            val ctaModifier = Modifier.fillMaxWidth()
            val callToAction = assets.callToAction
            if (callToAction != null) {
                NativeAdCallToAction(
                    text = callToAction,
                    binder = binder,
                    modifier = ctaModifier,
                    height = CollapsibleNativeAdDimens.LayerCallToActionHeight,
                )
            } else {
                Box(ctaModifier.heightIn(min = CollapsibleNativeAdDimens.LayerCallToActionHeight))
            }
        }
        if (showAdChoicesMarker) AdChoicesMarker()
        CollapseButton(onCollapse)
    }
}

/** Icon, 13 sp headline and the "Ad" line with the advertiser, 38 dp high. */
@Composable
private fun LayerHeader(assets: NativeAdAssets, binder: NativeAdBinder) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NativeAdDimens.PartGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NativeAdIcon(assets.icon, binder, size = CollapsibleNativeAdDimens.LayerIconSize)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(NativeAdDimens.HeadlineGap),
        ) {
            NativeAdHeadline(assets.headline, binder, fontSize = CollapsibleNativeAdDimens.LayerHeadlineSize)
            NativeAdBadgeLine(assets, binder, bodyWhenNoAdvertiser = false)
        }
    }
}

/**
 * A 32 dp round button, 4 dp from the top end corner, drawn and laid out at 32 dp: Compose widens its touch area to
 * 48 dp without moving anything (as the close button of the native full screen, plan T-027).
 */
@Composable
private fun BoxScope.CollapseButton(onCollapse: () -> Unit) {
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(
                top = CollapsibleNativeAdDimens.CollapseButtonInset,
                end = CollapsibleNativeAdDimens.CollapseButtonInset,
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
            .align(Alignment.TopStart)
            .padding(top = Spacing.xs, start = Spacing.xs)
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
    renderAd: @Composable (ad: LoadedNativeAd, onCollapse: () -> Unit) -> Unit = { ad, onCollapse ->
        BoundNativeAd(ad) { binder ->
            CollapsibleNativeAdLayout(ad.assets, onCollapse, binder, showAdChoicesMarker = false)
        }
    },
) {
    if (!allowed) {
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
 * over the bottom slot. Its ads are format [AdFormat.NATIVE_COLLAPSIBLE] of the bar's placement, from their own
 * single-ad source refreshed every 60 s (CL-02); the bar checks consent and the placement first.
 */
@Composable
internal fun CollapsibleNativeAdLayer(placement: AdPlacement, expandAfterSeconds: Long, modifier: Modifier = Modifier) {
    val ads = LocalAdsController.current ?: return
    // Outside any branch that depends on the ad: a new ad never opens it again.
    val state = rememberCollapsibleAdState()
    val unitId = remember(placement) { ads.unitId(placement, AdFormat.NATIVE_COLLAPSIBLE) }
    // `af_native_ad_eligible` once each time the layer comes in: the bar checked consent and its key (plan T-033).
    ReportNativeEligible(unitId != null)
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
    )
}

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
