package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kotlinx.coroutines.launch

/**
 * The ads controller of the running app, provided by :app's activity. Tests and previews provide nothing,
 * so [AdSlot] and [PrivacyOptionsButton] render nothing there and screenshots never contain an ad.
 */
val LocalAdsController = staticCompositionLocalOf<AdsController?> { null }

/**
 * Shows the banner of [placement] in a screen. Features call this, never an ads SDK directly.
 * It takes no space until ads are allowed (consent given, SDK started, placement switched on remotely).
 */
@Composable
fun AdSlot(placement: AdPlacement, modifier: Modifier = Modifier) {
    require(placement.format == AdFormat.BANNER) { "${placement.key} is not a banner placement" }
    val ads = LocalAdsController.current
    if (ads == null) {
        Box(modifier = modifier)
        return
    }
    val canRequestAds by ads.canRequestAds.collectAsStateWithLifecycle()
    val enabled by remember(placement) { ads.isEnabled(placement) }.collectAsStateWithLifecycle(initialValue = false)
    val unitId = remember(placement) { ads.unitId(placement) }
    if (!canRequestAds || !enabled || unitId == null) {
        Box(modifier = modifier)
        return
    }
    BannerAd(ads = ads, unitId = unitId, modifier = modifier)
}

@Composable
private fun BannerAd(ads: AdsController, unitId: String, modifier: Modifier) {
    BoxWithConstraints(modifier = modifier) {
        val context = LocalContext.current
        val screenWidthDp = LocalConfiguration.current.screenWidthDp
        val widthDp = if (constraints.hasBoundedWidth) maxWidth.value.toInt() else screenWidthDp
        val adSize = remember(widthDp) { AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, widthDp) }
        val adView = remember(unitId, adSize) {
            AdView(context).apply {
                adUnitId = unitId
                setAdSize(adSize)
            }
        }
        LaunchedEffect(adView) { adView.loadAd(ads.adRequest()) }

        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(adView, lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> adView.pause()
                    Lifecycle.Event.ON_RESUME -> adView.resume()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                adView.destroy()
            }
        }

        // The height is reserved before the ad loads, so the screen does not jump when it appears.
        AndroidView(factory = { adView }, modifier = Modifier.fillMaxWidth().height(adSize.height.dp))
    }
}

/**
 * Returns an action that shows the full-screen ad of [placement] if the guardrails allow it (ADR-0004).
 * Call the action at a natural break only. It does nothing in tests and previews.
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
 * that shows the consent form again. Skeleton for the tests written first: always draws [content].
 */
@Composable
fun PrivacyOptionsButton(modifier: Modifier = Modifier, content: @Composable (onClick: () -> Unit) -> Unit) {
    val ads = LocalAdsController.current
    val activity = LocalActivity.current
    Box(modifier) { content { if (ads != null && activity != null) ads.showPrivacyOptionsForm(activity) } }
}
