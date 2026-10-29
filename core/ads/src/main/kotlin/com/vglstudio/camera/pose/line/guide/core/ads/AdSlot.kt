package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * The ads controller of the running app, provided by :app's activity. Tests and previews provide nothing,
 * so [AdSlot] and [PrivacyOptionsButton] render nothing there and screenshots never contain an ad.
 */
val LocalAdsController = staticCompositionLocalOf<AdsController?> { null }

/**
 * Shows the banner of [placement] in a screen. Features call this, never an ads SDK directly.
 *
 * - [AdFormat.BANNER]: a fixed banner, refreshed by the SDK at the rate of its ad unit in AdMob; the app only retries
 *   after a failure (B-02). [AdFormat.BANNER_COLLAPSIBLE]: a collapsible banner, loaded again by :core:ads 30 s after
 *   each load, each time as a new expanded banner (B-03); place it at the bottom of the screen. Any other format
 *   throws [IllegalArgumentException].
 * - It takes no space until ads are allowed (consent given, placement switched on remotely, a unit id; B-01, B-04).
 *   The height is reserved before the ad loads, so the screen does not jump when it appears.
 * - Requests are sent only while the screen shows (resumed) and the consent gate allows them right then; a retry or
 *   reload due meanwhile waits for the screen to show again (B-05, AR-03). The banner is paused while hidden.
 * - Consent withdrawn after the banner had an ad: the frame keeps its height, the ad is hidden and paused; consent
 *   given again loads a new ad in the same frame (B-06).
 * - With a [separator] (a line above the banner, for example; spec poses, KB-6 of this app): the separator is drawn
 *   above the frame while the frame takes room. When the last load failed, the frame shrinks to 0 dp and the
 *   separator goes, so no empty strip stays; the view stays in the tree, so the retry of B-02 still runs and a load
 *   that succeeds brings both back. Without a separator the frame keeps its height after a failure (B-02).
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
    val unitId = remember(placement, format) { ads.unitId(placement, format) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val visible = remember(lifecycle) { lifecycle.resumedFlow() }
    val currentOnHeight by rememberUpdatedState(onHeight)
    BoxWithConstraints(modifier = modifier) {
        // The activity of the screen: Unity and ironSource need an Activity to load a banner.
        val context = LocalContext.current
        val screenWidthDp = LocalConfiguration.current.screenWidthDp
        val widthDp = if (constraints.hasBoundedWidth) maxWidth.value.toInt() else screenWidthDp
        val adSize = remember(widthDp) { AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, widthDp) }
        LaunchedEffect(adSize) { currentOnHeight(adSize.height.dp) }
        key(adSize) {
            BannerSlotHost(
                gateOpen = canRequestAds,
                enabled = enabled,
                unitId = unitId,
                heightDp = adSize.height,
                refresh = refresh,
                visible = visible,
                mayRequestNow = ads::mayRequestNow,
                retryResets = ads.retryResets,
                logger = ads.logger,
                placementKey = nativeSlotKey(placement, format),
                newView = {
                    val id = checkNotNull(unitId) { "${placement.key} has no ad unit" }
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
                    )
                },
                onAdAvailabilityChange = onAdAvailabilityChange,
                separator = separator,
            )
        }
    }
}

/**
 * The banner slot: its frame ([bannerFrame]), one ad view per frame ([newView], destroyed when the frame goes),
 * pause and resume ([bannerShouldRun]) and the sends ([runBannerLoads]) while the frame is active.
 * [onAdAvailabilityChange] gets true on the first ad of a frame, and false once when the frame goes after that; a
 * frame kept with its height (B-06) reports nothing, so a bottom bar keeps the space.
 * [separator]: see [AdSlot]; [onLoadResult] gets every load result of the view (true loaded, false failed).
 */
@Composable
internal fun BannerSlotHost(
    gateOpen: Boolean,
    enabled: Boolean,
    unitId: String?,
    heightDp: Int,
    refresh: BannerRefresh,
    visible: Flow<Boolean>,
    mayRequestNow: () -> Boolean,
    retryResets: StateFlow<Int>,
    logger: AppLogger,
    placementKey: String,
    newView: () -> BannerAdView,
    modifier: Modifier = Modifier,
    onAdAvailabilityChange: (Boolean) -> Unit = {},
    separator: (@Composable () -> Unit)? = null,
    onLoadResult: (loaded: Boolean) -> Unit = {},
) {
    var hadAd by remember { mutableStateOf(false) }
    val currentOnAvailability by rememberUpdatedState(onAdAvailabilityChange)
    val frame = bannerFrame(gateOpen, enabled, unitId, hadAd)
    if (frame == BannerFrame.NONE) {
        // The space is given back and the ad view destroyed: a later frame starts with no ad.
        LaunchedEffect(Unit) {
            if (hadAd) currentOnAvailability(false)
            hadAd = false
        }
        Box(modifier = modifier)
        return
    }
    Box(modifier = modifier) {
        // One ad view per placement, refresh kind and ad unit: two placements may share a unit (staging test ids).
        key(placementKey, refresh, unitId) {
            BannerFrameContent(
                frame = frame,
                heightDp = heightDp,
                refresh = refresh,
                visible = visible,
                mayRequestNow = mayRequestNow,
                retryResets = retryResets,
                logger = logger,
                placementKey = placementKey,
                newView = newView,
                onLoaded = {
                    if (!hadAd) {
                        hadAd = true
                        currentOnAvailability(true)
                    }
                },
                separator = separator,
                onLoadResult = onLoadResult,
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
    onLoaded: () -> Unit,
    separator: (@Composable () -> Unit)?,
    onLoadResult: (loaded: Boolean) -> Unit,
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
    // The result of the last load, for the separator of this app (AdSlot): placed before the sends, so the result of
    // the first one is seen.
    var lastLoadFailed by remember(view) { mutableStateOf(false) }
    val currentOnLoadResult by rememberUpdatedState(onLoadResult)
    LaunchedEffect(view) {
        view.results.collect { result ->
            val loaded = result == BannerLoadResult.Loaded
            lastLoadFailed = !loaded
            currentOnLoadResult(loaded)
        }
    }
    LaunchedEffect(view, active) {
        if (active) {
            runBannerLoads(
                refresh = refresh,
                view = view,
                visible = visible,
                mayRequestNow = { currentMayRequestNow() },
                retryResets = retryResets,
                onLoaded = { currentOnLoaded() },
                logger = logger,
                placementKey = placementKey,
            )
        }
    }
    if (separator == null) {
        view.Content(showAd = active, modifier = Modifier.fillMaxWidth().height(heightDp.dp))
    } else {
        Column {
            if (!lastLoadFailed) separator()
            view.Content(
                showAd = active,
                modifier = Modifier.fillMaxWidth().height(if (lastLoadFailed) 0.dp else heightDp.dp),
            )
        }
    }
}

/**
 * The banner slot of a bottom bar ([BottomAdBarScaffold], D-01): the fixed banner (refreshed by the SDK, B-01, B-02)
 * as a layer of the bar's [placement], with the unit of [AdFormat.BANNER] for it; no unit (production today) sends
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
