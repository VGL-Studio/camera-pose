package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import kotlin.math.max
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

private const val TAG = "Ads"

internal object NativeFlowStepTestTags {
    /** The root of [NativeFullScreenStepGate]: the area the app gives the page. */
    const val FULL_PAGE = "nativeFlowStepFullPage"

    /** The static placeholder of the page while it waits for an ad (plan T-041). */
    const val FULL_PAGE_PLACEHOLDER = "nativeFlowStepFullPagePlaceholder"

    /** The block of the head of the ad in the placeholder of the page (plan T-049). */
    const val PLACEHOLDER_HEADER = "nativeFlowStepPlaceholderHeader"

    /** The block of the call to action in the placeholder of the page (plan T-049). */
    const val PLACEHOLDER_CTA = "nativeFlowStepPlaceholderCta"
}

/** Numbers of the page of a flow step ([NativeFullScreenStep]) for the app (plan T-041). */
object NativeFullScreenStepDefaults {
    /**
     * The least height of the page at font scale 1.0 (plan T-049): the page takes no ad in a shorter area. With larger
     * text the least height grows with the head of the ad; read [minHeight] for the number at the current font scale.
     */
    val MinHeight: Dp = 152.dp

    /**
     * The least height of the page at the current font scale (plan T-049): [MinHeight] at 1.0, more with larger text
     * (about 178 dp at 2.0). The page takes no ad and draws nothing in a shorter area, so an app that may give it less
     * (a phone in landscape with its system bars: about 187 dp at 873 x 393 dp) compares its area with this function,
     * never with the constant, and shows content of its own when the area is shorter.
     */
    @Composable
    fun minHeight(): Dp {
        val density = LocalDensity.current
        val typography = MaterialTheme.typography
        return remember(density, typography) { flowStepMetrics(density, typography).minHeight }
    }
}

/** Numbers of the card of a flow step ([NativeStepAd]) for the app (plan T-045). */
object NativeStepAdDefaults {
    /**
     * The height of the step card at the current font scale: 250 dp at the default font scale (up to about 1.3);
     * with larger text the card grows so its media keeps 120 dp and its call to action holds its text (270 dp at
     * 2.0). The card, its placeholder and the space it keeps all take this height. An app that puts the card in a
     * fixed frame, so the Next button does not move between steps (OB-01), sets that frame with this function, never
     * with a fixed 250 dp.
     */
    @Composable
    fun height(): Dp {
        val measurer = rememberTextMeasurer()
        val typography = MaterialTheme.typography
        val density = LocalDensity.current
        return remember(measurer, typography, density) { stepCardHeight(measurer, typography, density) }
    }
}

/**
 * The height of the step card for one line of each of its texts, measured with [measurer] at [density] (the same
 * measuring `Text` does, so the non-linear font scale of Android 14 is in it): 12 dp margin, media at its 120 dp
 * floor, the head (32 dp icon, or the 13 sp headline, 2 dp and the "Ad" line), the call to action (36 dp or one line
 * of `labelLarge`), 8 dp gaps; never under the 250 dp of the design.
 */
internal fun stepCardHeight(measurer: TextMeasurer, typography: Typography, density: Density): Dp = with(density) {
    fun line(style: TextStyle): Int = measurer.measure(LINE_SAMPLE, style, maxLines = 1, density = density).size.height
    val headline = line(typography.titleSmall.merge(TextStyle(fontSize = NativeAdDimens.StepHeadlineTextSize)))
    val badgeLine = max(line(typography.labelSmall), line(typography.bodySmall))
    val text = headline + NativeAdDimens.HeadlineGap.roundToPx() + badgeLine
    val head = max(NativeAdDimens.StepIconSize.roundToPx(), text)
    val callToAction = max(NativeAdDimens.StepCallToActionHeight.roundToPx(), line(typography.labelLarge))
    val margin = NativeAdDimens.InnerPadding.roundToPx()
    val gap = Spacing.sm.roundToPx()
    val card = margin + NativeAdDimens.MinMediaSize.roundToPx() + gap + head + gap + callToAction + margin
    max(NativeAdDimens.StepHeight.roundToPx(), card).toDp()
}

/** Text measured for one line; the height of a line depends on the style, not on the text. */
private const val LINE_SAMPLE = "Ad"

/**
 * The native card of a flow step (MC-06, OB-01, OB-02; plan T-032): 250 dp at the default font scale, more with
 * large text ([NativeStepAdDefaults.height], plan T-045), from the pool of its placement (`native_step`, P-02), drawn
 * outside the pager. Pass the key of the step on screen as [step] (for example `pagerState.settledPage`), and call it
 * only on the steps the app's spec chose. Each time [step] changes the card takes another ad of the pool, and gives
 * the old one back only once it has the new one: the pool never hands out an ad in use (P-01), so two steps next to
 * each other never show the same ad (OB-02). Until the new ad comes it shows the placeholder of the same height (for
 * about one frame when the pool has an ad ready; the height stays, so the Next button does not move), or nothing
 * while the pool is exhausted (P-06), and keeps asking. Like [NativeAdSlot], it takes no space until ads are allowed
 * (consent given, `is_show_<placement>` on, an ad unit), and gives its ad back when it leaves the screen or the
 * placement turns off (L-061). [onAdAvailabilityChange] works as for [NativeAdSlot]. It records no `ad_opportunity`
 * (P-07). Placements of another format are an [IllegalArgumentException].
 */
@Composable
fun NativeStepAd(
    placement: AdPlacement,
    step: Any,
    modifier: Modifier = Modifier,
    onAdAvailabilityChange: (Boolean) -> Unit = {},
) {
    requireStepFormat(placement.format)
    val ads = LocalAdsController.current
    if (ads == null) {
        Box(modifier = modifier)
        return
    }
    key(placement) {
        NativeStepAdHost(
            allowed = rememberStepAllowed(placement, ads.canRequestAds, ads::isEnabled) { ads.unitId(it) },
            step = step,
            newStates = { steps -> ads.stepAd(placement, steps) },
            modifier = modifier,
            onAdAvailabilityChange = onAdAvailabilityChange,
        )
    }
}

/**
 * Whether the step card of [placement] may take an ad now: consent given, `is_show_<placement>` on (the master switch
 * applied, off too while ad-free, [AdFreeStatus]), an ad unit (the gate [NativeStepAd] reads before it takes any
 * space). False with no ads controller, and false as soon as ads stop being allowed (L-061). An app that holds its
 * Next button while the card of the first step waits for an ad (plan T-083) waits only while this is true. Placements
 * of another format are an [IllegalArgumentException].
 */
@Composable
fun rememberNativeStepAdAllowed(placement: AdPlacement): Boolean {
    // Skeleton of plan T-083, step 3.
    return false
}

/**
 * [NativeStepAd] without an ads controller: [step] goes into one stream of steps, read by the states [newStates]
 * builds once ([stepAdStates] in the app); drawn by the gate of every native slot at the size of the step card.
 */
@Composable
internal fun NativeStepAdHost(
    allowed: Boolean,
    step: Any,
    newStates: (steps: Flow<Any>) -> Flow<NativeAdState>,
    modifier: Modifier = Modifier,
    onAdAvailabilityChange: (Boolean) -> Unit = {},
    adContent: @Composable (LoadedNativeAd) -> Unit = { BoundNativeAd(it, NativeAdSize.STEP) },
) {
    val steps = remember { MutableStateFlow(step) }
    SideEffect { steps.value = step }
    // A cold flow: nothing is claimed until the gate opens and the host collects it.
    val states = remember { newStates(steps) }
    NativeAdSlotGate(
        allowed = allowed,
        states = states,
        size = NativeAdSize.STEP,
        keepSpaceWhenEmpty = false,
        onAdAvailabilityChange = onAdAvailabilityChange,
        modifier = modifier,
        adContent = adContent,
    )
}

/**
 * The states of the step card on [pool] (OB-02, P-06, P-01a): Loading at each new step of [steps], then the ad
 * claimed for [holder]. The ad of the step before stays claimed until the new one comes, then goes back to the pool,
 * so no claim of the new step can get it. With no ad, asks again every 500 ms, or every 2 s while the pool is
 * exhausted, when it shows Unavailable (once per step). A step that changes while the card waits drops that wait and
 * keeps the old ad held. When the collection ends, every ad held goes back to the pool. Logs never hold the step key
 * (it may be data of the app).
 */
internal fun stepAdStates(
    pool: PooledNativeAds,
    holder: AdPlacement,
    steps: Flow<Any>,
    logger: AppLogger,
    placementKey: String,
): Flow<NativeAdState> = channelFlow {
    // The ad of the step on screen, and the ad of the step before, still held until the new one comes.
    var current: LoadedNativeAd? = null
    var previous: LoadedNativeAd? = null
    var firstStep = true
    try {
        steps.distinctUntilChanged().collectLatest {
            if (!firstStep) logger.info(TAG, "Step $placementKey: step changed")
            firstStep = false
            current?.let { shown ->
                // An older ad still held here was replaced before it went back: it is not on screen any more.
                previous?.let(pool::release)
                previous = shown
                current = null
            }
            send(NativeAdState.Loading)
            var unavailableShown = false
            while (true) {
                val ad = pool.claim(holder)
                if (ad != null) {
                    current = ad
                    logger.info(TAG, "Step $placementKey: got a new ad")
                    send(NativeAdState.Loaded(ad))
                    previous?.let(pool::release)
                    previous = null
                    break
                }
                if (pool.exhausted) {
                    if (!unavailableShown) {
                        unavailableShown = true
                        logger.info(TAG, "Step $placementKey: no ad (pool exhausted)")
                        send(NativeAdState.Unavailable)
                    }
                    delay(PoolCellPolicy.EXHAUSTED_RETRY_MILLIS)
                } else {
                    delay(PoolCellPolicy.RETRY_MILLIS)
                }
            }
        }
    } finally {
        current?.let(pool::release)
        previous?.let(pool::release)
    }
}.buffer(Channel.RENDEZVOUS)

/**
 * The native page of a flow step (MC-06; plan T-032): the preloaded ad of the `native_fullscreen` slot of [placement],
 * drawn in [modifier], the area of the step the app gives it, with no close button and no countdown; the app keeps its
 * Next button outside that area, where it is on the other steps. Pass [shown] = `pagerState.settledPage == index`:
 * the page takes an ad from the slot only while it is the page on screen, and gives it up (the ad is destroyed and
 * the slot loads again) once it is not; a page built next to the one on screen takes nothing. While it shows its ad,
 * the ads that show by themselves are blocked (G-03, way 2). With no ad it draws a static placeholder, reports false,
 * and shows an ad that comes later. When the activity is built again (rotation, fold, unfold) the page leaves like any
 * native slot (O-01, T-040): its ad is destroyed, the slot loads again, and the page built again shows the placeholder
 * until the new ad comes. Its layout follows the height of [modifier] and the font scale (plan T-041): the column of
 * the design, or a compact row on short areas; under [NativeFullScreenStepDefaults.MinHeight] (152 dp at font scale
 * 1.0; more with larger text, [NativeFullScreenStepDefaults.minHeight]) it takes no ad, draws nothing and records no
 * `af_native_ad_eligible`: an app whose area may be shorter reads [NativeFullScreenStepDefaults.minHeight] and shows
 * content of its own there. Give it 360 dp or more for the column. It takes no ad until ads are allowed (consent given, `is_show_<placement>` on, an ad unit), and
 * gives it up when the placement turns off (L-061). It does not go through the gate of full-screen ads: no
 * `ads_interval`, no mark, no `ad_opportunity` (plan T-032, question 4). Build the list of steps with
 * [rememberNativeFullScreenStepReady], read once, so the page is there only when the slot has an ad. Placements of
 * another format are an [IllegalArgumentException].
 */
@Composable
fun NativeFullScreenStep(
    placement: AdPlacement,
    modifier: Modifier = Modifier,
    shown: Boolean = true,
    onAdAvailabilityChange: (Boolean) -> Unit = {},
) {
    requireFullScreenStepFormat(placement.format)
    val ads = LocalAdsController.current
    if (ads == null) {
        Box(modifier = modifier)
        return
    }
    key(placement) {
        NativeFullScreenStepGate(
            allowed = rememberStepAllowed(placement, ads.canRequestAds, ads::isEnabled) { ads.unitId(it) },
            // A cold flow: nothing is taken until the gate opens and the page collects it.
            states = remember { ads.fullScreenStep(placement) },
            modifier = modifier,
            shown = shown,
            onAdAvailabilityChange = onAdAvailabilityChange,
        )
    }
}

/**
 * [NativeFullScreenStep] without an ads controller: collects [states] only while [allowed], [shown] and the area is
 * high enough for the page ([FlowStepMetrics.minHeight] at the font scale, 152 dp at 1.0, as
 * [NativeFullScreenStepDefaults.minHeight] gives the app; no height bound always is); draws the ad
 * with no controls and blocks the ads that show by themselves while it has one; draws the static placeholder while it
 * waits for an ad (plan T-041), nothing otherwise. Reports availability like [NativeAdSlotGate]: true on an ad, false
 * on no ad, and false when [allowed] turns false or the area gets too short after true was reported (L-061). A page
 * that is only not [shown] reports nothing. `af_native_ad_eligible` only while the page could take an ad.
 */
@Composable
internal fun NativeFullScreenStepGate(
    allowed: Boolean,
    states: Flow<NativeAdState>,
    modifier: Modifier = Modifier,
    shown: Boolean = true,
    onAdAvailabilityChange: (Boolean) -> Unit = {},
    adContent: @Composable (LoadedNativeAd) -> Unit = { FlowStepFullPage(it) },
) {
    val currentOnChange by rememberUpdatedState(onAdAvailabilityChange)
    val reported = remember { StepAvailability() }
    val metrics = flowStepMetrics(LocalDensity.current, MaterialTheme.typography)
    BoxWithConstraints(modifier = modifier.testTag(NativeFlowStepTestTags.FULL_PAGE)) {
        // A page too short for the ad is not a chance to show one (plan T-041, question 6).
        val fits = !constraints.hasBoundedHeight || maxHeight >= metrics.minHeight
        // `af_native_ad_eligible` once each time the page comes on screen allowed (plan T-033).
        ReportNativeEligible(allowed && shown && fits)
        if (!allowed || !fits) {
            SideEffect {
                if (reported.available) {
                    reported.available = false
                    currentOnChange(false)
                }
            }
        } else if (shown) {
            FullPageOnScreen(states, adContent) { available ->
                reported.available = available
                currentOnChange(available)
            }
        }
    }
}

/**
 * The page while it is allowed, high enough and on screen: leaving the composition cancels the collection, which
 * frees the ad. While it waits for an ad (Loading, Unavailable), the static placeholder of the page, which blocks no
 * ad (plan T-041).
 */
@Composable
private fun FullPageOnScreen(
    states: Flow<NativeAdState>,
    adContent: @Composable (LoadedNativeAd) -> Unit,
    onAvailability: (Boolean) -> Unit,
) {
    val state by states.collectAsState(initial = NativeAdState.Loading)
    val currentOnAvailability by rememberUpdatedState(onAvailability)
    // The value of this composition, not the delegate: the effect may run after a newer state arrived.
    val drawn = state
    LaunchedEffect(drawn) {
        when (drawn) {
            NativeAdState.Loading -> Unit
            is NativeAdState.Loaded -> currentOnAvailability(true)
            NativeAdState.Unavailable -> currentOnAvailability(false)
        }
    }
    if (drawn is NativeAdState.Loaded) {
        SuppressResumeAds(active = true)
        key(drawn.ad) { adContent(drawn.ad) }
    } else {
        FlowStepPagePlaceholder(Modifier.fillMaxSize().testTag(NativeFlowStepTestTags.FULL_PAGE_PLACEHOLDER))
    }
}

/** The last availability reported by [NativeFullScreenStepGate]; not state, nothing is drawn from it. */
private class StepAvailability {
    var available = false
}

/** A loaded ad as the page of a flow step: the full-screen layout with no controls, the SDK's media view in it. */
@Composable
private fun FlowStepFullPage(ad: LoadedNativeAd) {
    BoundNativeAd(ad, Modifier.fillMaxSize()) { binder ->
        NativeFullScreenAdLayout(
            assets = ad.assets,
            onClose = null,
            binder = binder,
            // The SDK draws AdChoices in the top right corner itself; the adChoices slot stays empty.
            media = { binder.Media(Modifier.fillMaxSize()) },
        )
    }
}

/**
 * The states of the page of a flow step: the ad [take] gives, held until the collection ends, then given to
 * [release]. With no ad, asks for a load with [ensureLoading], shows Unavailable once, and takes again every 500 ms,
 * so an ad that comes while the page is on screen is shown (P-06 pace).
 */
internal fun fullScreenStepStates(
    take: () -> LoadedNativeAd?,
    ensureLoading: () -> Unit,
    release: (LoadedNativeAd) -> Unit,
): Flow<NativeAdState> = flow {
    var unavailableShown = false
    while (true) {
        val ad = take()
        if (ad != null) {
            try {
                emit(NativeAdState.Loaded(ad))
                awaitCancellation()
            } finally {
                release(ad)
            }
        }
        ensureLoading()
        if (!unavailableShown) {
            unavailableShown = true
            emit(NativeAdState.Unavailable)
        }
        delay(PoolCellPolicy.RETRY_MILLIS)
    }
}

/** S-11: the page takes the preloaded ad out of the slot only while consent allows ads. */
internal fun stepFullScreenTake(canRequestAds: Boolean, take: () -> LoadedNativeAd?): LoadedNativeAd? =
    if (canRequestAds) take() else null

/**
 * Whether the page of a flow step of [placement] would have an ad now: ads allowed and an ad preloaded in the slot,
 * read again every 500 ms; false with no ads controller, and as soon as ads stop being allowed (L-061). Read it once
 * when building the list of steps, so the page is there only with an ad and the number of pages does not change while
 * the flow runs. Placements of another format are an [IllegalArgumentException].
 */
@Composable
fun rememberNativeFullScreenStepReady(placement: AdPlacement): Boolean {
    requireFullScreenStepFormat(placement.format)
    val ads = LocalAdsController.current ?: return false
    val allowed = rememberStepAllowed(placement, ads.canRequestAds, ads::isEnabled) { ads.unitId(it) }
    return rememberStepReady(allowed) { ads.stepFullScreenReady(placement) }
}

/** [rememberNativeFullScreenStepReady] without an ads controller: [ready] read every 500 ms while [allowed]. */
@Composable
internal fun rememberStepReady(allowed: Boolean, ready: () -> Boolean): Boolean {
    val currentReady by rememberUpdatedState(ready)
    val polled by produceState(initialValue = false, allowed) {
        if (!allowed) {
            value = false
            return@produceState
        }
        while (true) {
            value = currentReady()
            delay(PoolCellPolicy.RETRY_MILLIS)
        }
    }
    return allowed && polled
}

/**
 * The gate of the ads of a flow step (KB-1, MC-03): consent, the switch of [placement] (`is_show_<placement>`, no
 * switch of its own for the flow) and an ad unit; [enabled] and [unitId] are asked for [placement] only.
 */
@Composable
internal fun rememberStepAllowed(
    placement: AdPlacement,
    canRequestAds: StateFlow<Boolean>,
    enabled: (AdPlacement) -> Flow<Boolean>,
    unitId: (AdPlacement) -> String?,
): Boolean {
    val canRequest by canRequestAds.collectAsStateWithLifecycle()
    val on by remember(placement) { enabled(placement) }.collectAsStateWithLifecycle(initialValue = false)
    val unit = remember(placement) { unitId(placement) }
    return nativeSlotAllowed(canRequest, on, unit)
}

/** The step card draws `native_step` placements only. */
internal fun requireStepFormat(format: AdFormat) {
    require(format == AdFormat.NATIVE_STEP) { "$format is not drawn by NativeStepAd" }
}

/** The page of a flow step draws `native_fullscreen` placements only. */
internal fun requireFullScreenStepFormat(format: AdFormat) {
    require(format == AdFormat.NATIVE_FULL_SCREEN) { "$format is not drawn by NativeFullScreenStep" }
}

/**
 * The page of a flow step with fixed content, for previews, tests and the gallery screen: the layout of
 * [NativeFullScreenStep] with no controls, in [modifier]. Real ads go through [NativeFullScreenStep].
 */
@Composable
fun NativeFullScreenStepContent(
    assets: NativeAdAssets,
    modifier: Modifier = Modifier,
    media: @Composable BoxScope.() -> Unit = {},
    adChoices: @Composable () -> Unit = {},
) {
    NativeFullScreenAdLayout(assets, onClose = null, PreviewNativeAdBinder, modifier, media, adChoices)
}
