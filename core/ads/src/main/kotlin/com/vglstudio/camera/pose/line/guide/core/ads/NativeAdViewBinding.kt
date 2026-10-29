package com.vglstudio.camera.pose.line.guide.core.ads

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAdView

/** The parts of a native ad that the SDK must know as views, so it can count clicks on them. */
internal enum class NativeAdAsset { HEADLINE, ADVERTISER, BODY, CALL_TO_ACTION, ICON }

/** Lets a native ad layout of any shape mark its parts for the SDK. */
internal interface NativeAdBinder {
    /** Draws [content] as the view of [asset]. The content must not handle clicks itself: the SDK does. */
    @Composable
    fun Asset(asset: NativeAdAsset, modifier: Modifier, content: @Composable () -> Unit)

    /** The ad's image or video, sized by [modifier]. */
    @Composable
    fun Media(modifier: Modifier)
}

/** Fake ads, tests and previews: draws the content as is; Media draws the surfaceContainer block. */
internal object PreviewNativeAdBinder : NativeAdBinder {
    @Composable
    override fun Asset(asset: NativeAdAsset, modifier: Modifier, content: @Composable () -> Unit) {
        // Min constraints pass through, so a part sized by its modifier (a wide call to action) fills it, like the
        // nested view does in the SDK binding.
        Box(modifier = modifier, propagateMinConstraints = true) { content() }
    }

    @Composable
    override fun Media(modifier: Modifier) {
        Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainer))
    }
}

/**
 * Draws a loaded ad with a layout of any shape. A real SDK ad is wrapped in a NativeAdView; a fake ad (tests) is drawn
 * with [PreviewNativeAdBinder]. Features never see NativeAdView (ADR-0022, decision 5).
 */
@Composable
internal fun BoundNativeAd(
    ad: LoadedNativeAd,
    modifier: Modifier = Modifier,
    content: @Composable (NativeAdBinder) -> Unit,
) {
    if (ad is SdkLoadedNativeAd) {
        NativeAdViewHost(bind = { it.setNativeAd(ad.nativeAd) }, modifier = modifier, content = content)
    } else {
        Box(modifier = modifier) { content(PreviewNativeAdBinder) }
    }
}

/**
 * Draws a loaded ad of [size] (small, medium, or the card of a collection or of a flow step, which bind their media
 * like medium); used by [NativeAdSlot], [NativeAdCell], [NativeStepAd] and by ads inside lists. [ctaLarge] is the
 * large call to action of medium.
 */
@Composable
internal fun BoundNativeAd(
    ad: LoadedNativeAd,
    size: NativeAdSize,
    modifier: Modifier = Modifier,
    ctaLarge: Boolean = false,
) {
    BoundNativeAd(ad, modifier) { binder ->
        NativeAdLayout(ad.assets, size, binder, showAdChoicesMarker = false, ctaLarge = ctaLarge)
    }
}

/**
 * One NativeAdView around [content]. Each asset is a real view inside it, registered with the SDK. [bind] runs after
 * the asset views change, at most once per frame, so `setNativeAd` always sees every view registered (an SDK rule),
 * and runs again when the layout changes (a collapsible ad removes its media). [newMediaView] makes the media view;
 * tests pass one that records its calls.
 */
@Composable
internal fun NativeAdViewHost(
    bind: (NativeAdView) -> Unit,
    modifier: Modifier = Modifier,
    newMediaView: (Context) -> MediaView = ::MediaView,
    content: @Composable (NativeAdBinder) -> Unit,
) {
    val context = LocalContext.current
    val adView = remember { NativeAdView(context) }
    val composeView = remember { ComposeView(context) }
    val currentBind by rememberUpdatedState(bind)
    val binder = remember(adView) { SdkNativeAdBinder(adView, newMediaView) { currentBind(adView) } }
    AndroidView(
        factory = {
            if (composeView.parent == null) {
                adView.addView(
                    composeView,
                    FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
                )
            }
            adView
        },
        modifier = modifier,
        update = { composeView.setContent { content(binder) } },
    )
    DisposableEffect(adView) {
        // The NativeAd itself is destroyed by whoever holds it (NativeAdSlotHost, list ads).
        onDispose { adView.destroy() }
    }
}

/** Registers each asset view with the NativeAdView and asks for one bind after the changes of a frame. */
private class SdkNativeAdBinder(
    private val adView: NativeAdView,
    private val newMediaView: (Context) -> MediaView,
    private val bind: () -> Unit,
) : NativeAdBinder {
    private var bindPending = false

    @Composable
    override fun Asset(asset: NativeAdAsset, modifier: Modifier, content: @Composable () -> Unit) {
        AndroidView(
            factory = { ComposeView(it) },
            modifier = modifier,
            update = { view ->
                view.setContent(content)
                register(asset, view)
            },
            onRelease = { view -> if (registered(asset) === view) register(asset, null) },
        )
    }

    @Composable
    override fun Media(modifier: Modifier) {
        AndroidView(
            // O-07: the scale type is set when the view is made, before it is registered and the ad is bound.
            factory = { newMediaView(it).apply { setImageScaleType(ImageView.ScaleType.FIT_CENTER) } },
            modifier = modifier,
            update = { view ->
                if (adView.mediaView !== view) {
                    adView.mediaView = view
                    requestBind()
                }
            },
            onRelease = { view ->
                if (adView.mediaView === view) {
                    adView.mediaView = null
                    requestBind()
                }
            },
        )
    }

    private fun registered(asset: NativeAdAsset): View? = when (asset) {
        NativeAdAsset.HEADLINE -> adView.headlineView
        NativeAdAsset.ADVERTISER -> adView.advertiserView
        NativeAdAsset.BODY -> adView.bodyView
        NativeAdAsset.CALL_TO_ACTION -> adView.callToActionView
        NativeAdAsset.ICON -> adView.iconView
    }

    private fun register(asset: NativeAdAsset, view: View?) {
        if (registered(asset) === view) return
        when (asset) {
            NativeAdAsset.HEADLINE -> adView.headlineView = view
            NativeAdAsset.ADVERTISER -> adView.advertiserView = view
            NativeAdAsset.BODY -> adView.bodyView = view
            NativeAdAsset.CALL_TO_ACTION -> adView.callToActionView = view
            NativeAdAsset.ICON -> adView.iconView = view
        }
        requestBind()
    }

    /** Groups the registrations of one frame into a single bind, run once the views are attached. */
    private fun requestBind() {
        if (bindPending) return
        bindPending = true
        adView.post {
            bindPending = false
            bind()
        }
    }
}
