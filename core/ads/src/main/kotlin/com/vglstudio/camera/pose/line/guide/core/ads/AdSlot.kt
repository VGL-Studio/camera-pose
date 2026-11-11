package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdView
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private const val TAG = "Ads"

/**
 * The ads controller of the running app, provided by :app's activity. Tests and previews provide nothing,
 * so [AdSlot] and [PrivacyOptionsButton] render nothing there and screenshots never contain an ad.
 */
val LocalAdsController = staticCompositionLocalOf<AdsController?> { null }

/**
 * Shows the banner of [placement] in a screen. Features call this, never an ads SDK directly.
 *
 * - [AdFormat.BANNER]: a fixed banner at the anchored adaptive size (B-01, ADR-0038), loaded again by :core:ads after
 *   each load at the interval of `time_reload_banner` (60 s by default; 0 keeps the banner), never by the refresh of
 *   its ad unit; a failure is retried (B-02). [AdFormat.BANNER_COLLAPSIBLE]: a
 *   collapsible banner at the large anchored adaptive size, loaded again by :core:ads after each load at the interval
 *   of `time_reload_banner_collap` (30 s by default; 0 keeps the banner), each time as a new expanded banner (B-03,
 *   KB-34); place it at the bottom of the screen. Any other format throws
 *   [IllegalArgumentException]. The size comes from [bannerAdSize].
 * - [AdFormat.BANNER_MREC]: a 300 x 250 dp banner centred in the content of the screen, loaded again by :core:ads at
 *   the interval of `time_reload_banner_mrec` (60 s by default; 0 keeps the banner; B-08). Its frame keeps 250 dp while it loads and takes no space when a load fails before it had any ad (it still
 *   retries); in a place narrower than 300 dp it is not shown at all.
 * - It takes no space until ads are allowed (consent given, placement switched on remotely, a unit id; B-01, B-04).
 *   The height is reserved before the ad loads, so the screen does not jump when it appears.
 * - Requests are sent only while the screen shows (resumed), no full-screen ad is drawn over the app (KB-27, B-07)
 *   and the consent gate allows them right then; a retry or reload due meanwhile is sent once when the screen shows
 *   again (B-05, AR-03). The banner is paused, not destroyed, while hidden or covered.
 * - Consent withdrawn after the banner had an ad: the frame keeps its height, the ad is hidden and paused; consent
 *   given again loads a new ad in the same frame (B-06).
 * - [AdFormat.BANNER] and [AdFormat.BANNER_MREC] load from AppLovin MAX and AdMob in the order of
 *   `ads_mediation_order` (KB-31, ADR-0037, plan T-071): the first platform without an ad hands the turn to the
 *   second one in the same frame, as tall as the tallest of them; a collapsible banner is AdMob only.
 */
@Composable
fun AdSlot(placement: AdPlacement, modifier: Modifier = Modifier) {
    BannerSlot(placement, placement.format, modifier)
}

/**
 * The banner of [placement] drawn as [format] (the placement's own, or the banner layer of a bottom bar): unit, frame
 * and metrics of that format, reported under [placement] (D-05). [onAdAvailabilityChange] and [onHeight] (the frame's
 * height) let a bottom bar keep the banner's space.
 */
@Composable
private fun BannerSlot(
    placement: AdPlacement,
    format: AdFormat,
    modifier: Modifier,
    onAdAvailabilityChange: (Boolean) -> Unit = {},
    onHeight: (Dp) -> Unit = {},
) {
    val collapsible = bannerRequestIsCollapsible(format)
    val ads = LocalAdsController.current
    if (ads == null) {
        Box(modifier = modifier)
        return
    }
    val canRequestAds by ads.canRequestAds.collectAsStateWithLifecycle()
    val enabled by remember(placement) { ads.isEnabled(placement) }.collectAsStateWithLifecycle(initialValue = false)
    val order by ads.mediationOrder.collectAsStateWithLifecycle()
    val admobUnit = remember(placement, format) { ads.unitId(placement, format) }
    val maxUnit = remember(format) { ads.maxUnitId(format) }
    // The platforms of the frame (plan T-071, KB-31): a new order or unit builds a new frame (question 7 of the plan).
    val platforms = remember(format, order, admobUnit, maxUnit) { bannerPlatforms(format, order, admobUnit, maxUnit) }
    val frameKey = remember(platforms, admobUnit, maxUnit) { bannerSlotUnitKey(platforms, admobUnit, maxUnit) }
    // The reload interval by hand of the format: every banner is reloaded by :core:ads (B-02, B-03, B-08, KB-34).
    val reloadMillis = remember(format) { ads.reloadIntervalMillis(format) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val visible = remember(lifecycle) { lifecycle.resumedFlow() }
    val currentOnHeight by rememberUpdatedState(onHeight)
    BoxWithConstraints(modifier = modifier) {
        // The activity of the screen: Unity and ironSource need an Activity to load a banner.
        val context = LocalContext.current
        val screenWidthDp = LocalConfiguration.current.screenWidthDp
        val widthDp = if (constraints.hasBoundedWidth) maxWidth.value.toInt() else screenWidthDp
        val adSize = remember(widthDp, format) { bannerAdSize(context, format, widthDp) }
        val kind = bannerSizeKind(format)
        val hasMax = AdPlatform.MAX in platforms
        val maxHeightDp = remember(kind, widthDp, hasMax) {
            if (hasMax) {
                ads.maxBannerHeightForTest?.invoke(
                    kind,
                    widthDp,
                ) ?: maxBannerHeightDp(context, kind, widthDp)
            } else {
                0
            }
        }
        // The frame is as tall as its tallest platform, so it does not jump when the platform shown changes.
        val heightDp = bannerFrameHeightDp(platforms, adSize.height, maxHeightDp)
        LaunchedEffect(heightDp) { currentOnHeight(heightDp.dp) }
        key(adSize) {
            BannerSlotHost(
                gateOpen = canRequestAds,
                enabled = enabled,
                unitId = frameKey,
                heightDp = heightDp,
                visible = visible,
                // The published flag: no gap while an interstitial hands over to its native ad (KB-27).
                covered = ads.fullScreenAdShowing,
                mayRequestNow = ads::mayRequestNow,
                retryResets = ads.retryResets,
                reloadMillis = reloadMillis,
                logger = ads.logger,
                placementKey = nativeSlotKey(placement, format),
                newView = {
                    val children = platforms.map { platform ->
                        platform to when (platform) {
                            AdPlatform.ADMOB -> {
                                val id = checkNotNull(admobUnit) { "${placement.key} has no ad unit" }
                                val adView = AdView(context).apply {
                                    adUnitId = id
                                    setAdSize(adSize)
                                }
                                SdkBannerAdView(
                                    adView = adView,
                                    collapsible = collapsible,
                                    placement = placement,
                                    unitId = id,
                                    telemetry = ads.telemetry,
                                    now = ads::metricsNowMillis,
                                    format = format,
                                    networkLog = ads.networkLog,
                                    loadErrorLog = ads.loadErrorLog,
                                )
                            }

                            AdPlatform.MAX -> {
                                val id = checkNotNull(maxUnit) { "${placement.key} has no MAX ad unit" }
                                MaxBannerAdView(
                                    // Built on the first send to MAX only (G2 of the plan).
                                    newSdkView = { AppLovinMaxBannerView(id, kind, widthDp, context) },
                                    placement = placement,
                                    format = format,
                                    telemetry = ads.telemetry,
                                    now = ads::metricsNowMillis,
                                    logger = ads.logger,
                                    loadErrorLog = ads.loadErrorLog,
                                )
                            }
                        }
                    }
                    OrderedBannerAdView(
                        children = children,
                        maxReady = { ads.maxReady.value },
                        logger = ads.logger,
                        placementKey = nativeSlotKey(placement, format),
                        heightsDp = mapOf(AdPlatform.ADMOB to adSize.height, AdPlatform.MAX to maxHeightDp),
                    )
                },
                onAdAvailabilityChange = onAdAvailabilityChange,
                kind = kind,
                fits = bannerFitsWidth(kind, widthDp),
            )
        }
    }
}

/**
 * The banner slot: its frame ([bannerFrame]), one ad view per frame ([newView], destroyed when the frame goes),
 * pause and resume ([bannerShouldRun]) and the sends ([runBannerLoads]) while the frame is active. Both read the
 * window of the slot: [visible] and not [covered] by a full-screen ad ([inScreenAdWindowOf], KB-27, plan T-054).
 * [onAdAvailabilityChange] gets true on the first ad of a frame, and false once when the frame goes after that; a
 * frame kept with its height (B-06) reports nothing, so a bottom bar keeps the space.
 *
 * [kind] sets the frame's width ([bannerFrameWidthDp], centred) and height ([bannerSlotHeightDp]): an MREC that failed
 * before its first ad takes no space (B-08). A banner that does not [fits] its place has no frame: no ad view, no
 * request, no space, one log line.
 */
@Composable
internal fun BannerSlotHost(
    gateOpen: Boolean,
    enabled: Boolean,
    unitId: String?,
    heightDp: Int,
    visible: Flow<Boolean>,
    covered: Flow<Boolean>,
    mayRequestNow: () -> Boolean,
    retryResets: StateFlow<Int>,
    reloadMillis: StateFlow<Long>,
    logger: AppLogger,
    placementKey: String,
    newView: () -> BannerAdView,
    modifier: Modifier = Modifier,
    onAdAvailabilityChange: (Boolean) -> Unit = {},
    kind: BannerSizeKind = BannerSizeKind.ANCHORED,
    fits: Boolean = true,
) {
    var hadAd by remember { mutableStateOf(false) }
    var failedBeforeAd by remember { mutableStateOf(false) }
    val currentOnAvailability by rememberUpdatedState(onAdAvailabilityChange)
    val window = remember(visible, covered) { inScreenAdWindowOf(visible, covered) }
    if (!fits) {
        LaunchedEffect(placementKey) { logger.info(TAG, "Banner $placementKey too narrow") }
    }
    val frame = if (fits) bannerFrame(gateOpen, enabled, unitId, hadAd) else BannerFrame.NONE
    if (frame == BannerFrame.NONE) {
        // The space is given back and the ad view destroyed: a later frame starts with no ad and no failure.
        LaunchedEffect(Unit) {
            if (hadAd) currentOnAvailability(false)
            hadAd = false
            failedBeforeAd = false
        }
        Box(modifier = modifier)
        return
    }
    val slotHeightDp = bannerSlotHeightDp(frame, kind, heightDp, failedBeforeAd)
    Box(modifier = modifier.height(slotHeightDp.dp), contentAlignment = Alignment.TopCenter) {
        // One ad view per placement key (placement and format of the layer) and ad unit: two placements may share a
        // unit (staging test ids).
        key(placementKey, unitId) {
            BannerFrameContent(
                frame = frame,
                heightDp = heightDp,
                visible = window,
                mayRequestNow = mayRequestNow,
                retryResets = retryResets,
                reloadMillis = reloadMillis,
                logger = logger,
                placementKey = placementKey,
                newView = newView,
                widthDp = bannerFrameWidthDp(kind),
                onLoaded = {
                    failedBeforeAd = false
                    if (!hadAd) {
                        hadAd = true
                        currentOnAvailability(true)
                    }
                },
                onFailed = { if (!hadAd) failedBeforeAd = true },
            )
        }
    }
}

@Composable
private fun BannerFrameContent(
    frame: BannerFrame,
    heightDp: Int,
    visible: Flow<Boolean>,
    mayRequestNow: () -> Boolean,
    retryResets: StateFlow<Int>,
    reloadMillis: StateFlow<Long>,
    logger: AppLogger,
    placementKey: String,
    newView: () -> BannerAdView,
    widthDp: Int?,
    onLoaded: () -> Unit,
    onFailed: () -> Unit,
) {
    val view = remember { newView() }
    DisposableEffect(view) {
        onDispose { view.destroy() }
    }
    val currentFrame by rememberUpdatedState(frame)
    LaunchedEffect(view, visible) {
        // Paused when it should not run; resumed only after a pause, so a view built running gets neither call.
        var running: Boolean? = null
        combine(visible, snapshotFlow { currentFrame }) { shows, current -> bannerShouldRun(shows, current) }
            .distinctUntilChanged()
            .collect { run ->
                when {
                    !run -> view.pause()
                    running != null -> view.resume()
                }
                running = run
            }
    }
    val active = frame == BannerFrame.ACTIVE
    val currentMayRequestNow by rememberUpdatedState(mayRequestNow)
    val currentOnLoaded by rememberUpdatedState(onLoaded)
    val currentOnFailed by rememberUpdatedState(onFailed)
    LaunchedEffect(view, active) {
        if (active) {
            runBannerLoads(
                view = view,
                visible = visible,
                mayRequestNow = { currentMayRequestNow() },
                retryResets = retryResets,
                reloadMillis = reloadMillis,
                onLoaded = { currentOnLoaded() },
                logger = logger,
                placementKey = placementKey,
                onFailed = { currentOnFailed() },
            )
        }
    }
    val width = if (widthDp != null) Modifier.width(widthDp.dp) else Modifier.fillMaxWidth()
    view.Content(showAd = active, modifier = width.height(heightDp.dp))
}

/**
 * The banner slot of a bottom bar ([BottomAdBarScaffold], D-01): the fixed banner at the anchored adaptive size
 * (loaded again by :core:ads at the interval of `time_reload_banner`, B-01, B-02, ADR-0038) as a layer of the bar's
 * [placement], with the unit of [AdFormat.BANNER] for it; no unit (production without the `banner` slot key) sends
 * nothing and takes no space (B-04). [onHeight] gets the frame's height for the space the bar keeps.
 */
@Composable
internal fun BottomBannerSlot(
    placement: AdPlacement,
    onAdAvailabilityChange: (Boolean) -> Unit,
    onHeight: (Dp) -> Unit,
    modifier: Modifier = Modifier,
) {
    BannerSlot(placement, AdFormat.BANNER, modifier, onAdAvailabilityChange, onHeight)
}

/**
 * Returns an action that shows the full-screen ad of [placement] if the guardrails allow it (ADR-0004).
 * Call the action at a natural break only. It does nothing in tests and previews.
 * New code uses [rememberShowFullScreenAd], which shows both interstitial and native full-screen placements.
 */
@Composable
fun rememberShowInterstitial(placement: AdPlacement): () -> Unit {
    require(placement.format == AdFormat.INTERSTITIAL) { "${placement.key} is not a full-screen placement" }
    val ads = LocalAdsController.current
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    return remember(ads, activity, placement) {
        { if (ads != null && activity != null) scope.launch { ads.showInterstitial(activity, placement) } }
    }
}

/**
 * Entry point to change consent, required by Google where the consent form applies.
 * Place it in the app's settings or main screen. It renders nothing where it is not required.
 */
@Composable
fun PrivacyOptionsButton(modifier: Modifier = Modifier) {
    val ads = LocalAdsController.current ?: return
    val activity = LocalActivity.current ?: return
    val required by ads.privacyOptionsRequired.collectAsStateWithLifecycle()
    if (required) {
        TextButton(onClick = { ads.showPrivacyOptionsForm(activity) }, modifier = modifier) {
            Text(text = stringResource(R.string.ads_privacy_options))
        }
    }
}
