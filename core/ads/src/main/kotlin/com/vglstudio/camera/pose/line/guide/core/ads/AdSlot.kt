package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
 * - [AdFormat.BANNER]: a fixed banner at the anchored adaptive size (B-01, ADR-0038), refreshed by the SDK at the
 *   rate of its ad unit in AdMob; the app only retries after a failure (B-02). [AdFormat.BANNER_COLLAPSIBLE]: a
 *   collapsible banner at the large anchored adaptive size, loaded again by :core:ads 30 s after each load, each time
 *   as a new expanded banner (B-03); place it at the bottom of the screen. Any other format throws
 *   [IllegalArgumentException]. The size comes from [bannerAdSize].
 * - [AdFormat.BANNER_MREC]: a 300 x 250 dp banner centred in the content of the screen, refreshed by its ad unit
 *   (B-08). Its frame keeps 250 dp while it loads and takes no space when a load fails before it had any ad (it still
 *   retries); in a place narrower than 300 dp it is not shown at all.
 * - It takes no space until ads are allowed (consent given, placement switched on remotely, a unit id; B-01, B-04).
 *   The height is reserved before the ad loads, so the screen does not jump when it appears.
 * - Requests are sent only while the screen shows (resumed), no full-screen ad is drawn over the app (KB-27, B-07)
 *   and the consent gate allows them right then; a retry or reload due meanwhile is sent once when the screen shows
 *   again (B-05, AR-03). The banner is paused, not destroyed, while hidden or covered.
 * - Consent withdrawn after the banner had an ad: the frame keeps its height, the ad is hidden and paused; consent
 *   given again loads a new ad in the same frame (B-06).
 * - With a [separator] (a line above the banner, for example; spec poses, KB-6 of this app): the separator is drawn
 *   above the frame while the frame takes room. When the last load failed, the frame shrinks to 0 dp and the
 *   separator goes, so no empty strip stays; the view stays in the tree, so the retry of B-02 still runs and a load
 *   that succeeds brings both back. Without a separator the frame keeps its height after a failure (B-02).
 * - [AdFormat.BANNER] and [AdFormat.BANNER_MREC] load from AppLovin MAX and AdMob in the order of
 *   `ads_mediation_order` (KB-31, ADR-0037, plan T-071): the first platform without an ad hands the turn to the
 *   second one in the same frame, as tall as the tallest of them; a collapsible banner is AdMob only.
 */
@Composable
fun AdSlot(placement: AdPlacement, modifier: Modifier = Modifier, separator: (@Composable () -> Unit)? = null) {
    BannerSlot(placement, placement.format, modifier, separator = separator)
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
    separator: (@Composable () -> Unit)? = null,
) {
    val refresh = bannerRefreshOf(format)
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
                refresh = refresh,
                visible = visible,
                // The published flag: no gap while an interstitial hands over to its native ad (KB-27).
                covered = ads.fullScreenAdShowing,
                mayRequestNow = ads::mayRequestNow,
                retryResets = ads.retryResets,
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
                                    refresh = refresh,
                                    placement = placement,
                                    unitId = id,
                                    telemetry = ads.telemetry,
                                    now = ads::metricsNowMillis,
                                    format = format,
                                    networkLog = ads.networkLog,
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
                separator = separator,
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
 * [separator]: see [AdSlot]; [onLoadResult] gets the result of every send (true loaded, false failed).
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
    refresh: BannerRefresh,
    visible: Flow<Boolean>,
    covered: Flow<Boolean>,
    mayRequestNow: () -> Boolean,
    retryResets: StateFlow<Int>,
    logger: AppLogger,
    placementKey: String,
    newView: () -> BannerAdView,
    modifier: Modifier = Modifier,
    onAdAvailabilityChange: (Boolean) -> Unit = {},
    separator: (@Composable () -> Unit)? = null,
    onLoadResult: (loaded: Boolean) -> Unit = {},
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
    // With a separator of this app the content sets the height: the separator above the frame, both gone after a
    // failed load (see [AdSlot]).
    val slotModifier = if (separator == null) modifier.height(slotHeightDp.dp) else modifier
    Box(modifier = slotModifier, contentAlignment = Alignment.TopCenter) {
        // One ad view per placement, refresh kind and ad unit: two placements may share a unit (staging test ids).
        key(placementKey, refresh, unitId) {
            BannerFrameContent(
                frame = frame,
                heightDp = heightDp,
                refresh = refresh,
                visible = window,
                mayRequestNow = mayRequestNow,
                retryResets = retryResets,
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
                separator = separator,
                onLoadResult = onLoadResult,
                onFailed = { if (!hadAd) failedBeforeAd = true },
            )
        }
    }
}

@Composable
private fun BannerFrameContent(
    frame: BannerFrame,
    heightDp: Int,
    refresh: BannerRefresh,
    visible: Flow<Boolean>,
    mayRequestNow: () -> Boolean,
    retryResets: StateFlow<Int>,
    logger: AppLogger,
    placementKey: String,
    newView: () -> BannerAdView,
    widthDp: Int?,
    onLoaded: () -> Unit,
    separator: (@Composable () -> Unit)?,
    onLoadResult: (loaded: Boolean) -> Unit,
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
    // The result of the last load, for the separator of this app (AdSlot), from the callbacks of runBannerLoads: a
    // banner of two platforms (OrderedBannerAdView) allows a single collection of view.results, and a platform handing
    // the turn to the next one is not a failed load.
    var lastLoadFailed by remember(view) { mutableStateOf(false) }
    val currentOnLoadResult by rememberUpdatedState(onLoadResult)
    val currentOnFailed by rememberUpdatedState(onFailed)
    LaunchedEffect(view, active) {
        if (active) {
            runBannerLoads(
                refresh = refresh,
                view = view,
                visible = visible,
                mayRequestNow = { currentMayRequestNow() },
                retryResets = retryResets,
                onLoaded = {
                    lastLoadFailed = false
                    currentOnLoadResult(true)
                    currentOnLoaded()
                },
                logger = logger,
                placementKey = placementKey,
                onFailed = {
                    lastLoadFailed = true
                    currentOnLoadResult(false)
                    currentOnFailed()
                },
            )
        }
    }
    val width = if (widthDp != null) Modifier.width(widthDp.dp) else Modifier.fillMaxWidth()
    if (separator == null) {
        view.Content(showAd = active, modifier = width.height(heightDp.dp))
    } else {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (!lastLoadFailed) separator()
            view.Content(
                showAd = active,
                modifier = width.height(if (lastLoadFailed) 0.dp else heightDp.dp),
            )
        }
    }
}

/**
 * The banner slot of a bottom bar ([BottomAdBarScaffold], D-01): the fixed banner at the anchored adaptive size
 * (refreshed by the SDK, B-01, B-02, ADR-0038) as a layer of the bar's [placement], with the unit of [AdFormat.BANNER] for it; no unit (production without the `banner` slot key) sends
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
    PrivacyOptionsButton(modifier = modifier) { onClick ->
        TextButton(onClick = onClick) {
            Text(text = stringResource(R.string.ads_privacy_options))
        }
    }
}

/**
 * The same entry point drawn by the caller, for example as a settings row (plan T-025): [content] receives the action
 * that shows the consent form again. As the button, it decides alone whether to show: nothing is drawn where consent
 * options are not required, nor without an ads controller (tests, previews).
 */
@Composable
fun PrivacyOptionsButton(modifier: Modifier = Modifier, content: @Composable (onClick: () -> Unit) -> Unit) {
    val ads = LocalAdsController.current ?: return
    val activity = LocalActivity.current ?: return
    val required by ads.privacyOptionsRequired.collectAsStateWithLifecycle()
    if (required) Box(modifier) { content { ads.showPrivacyOptionsForm(activity) } }
}
