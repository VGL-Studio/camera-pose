package com.vglstudio.camera.pose.line.guide.core.ads

import android.content.Context
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd

/**
 * The SDK callbacks of one native ad that the ad metrics read (plan T-023). Only reads data of the SDK; every
 * decision is made by [AdTelemetry].
 */
internal interface NativeAdEvents {
    fun onRequestSent()

    fun onLoaded(ad: NativeAd)

    /** [sdkMessage] is read on the device only, never sent (section 8.2). */
    fun onFailed(code: Int, sdkMessage: String?)

    fun onPaid(value: AdValue)

    fun onImpression()

    fun onClicked()

    /** The ad is destroyed: the zero-value fallback of T-02 is cancelled. */
    fun onDestroyed()
}

/**
 * Loads native ads with the Google Mobile Ads SDK. Main thread only, like the SDK. [events] gets the callbacks the
 * ad metrics read; one loader loads one ad. [options] are the request options, those of the size by default (the
 * collapsible layer passes its own, plan T-030).
 */
internal class SdkNativeAdLoader(
    private val context: Context,
    private val request: AdRequest,
    private val events: NativeAdEvents? = null,
    private val options: NativeRequestOptions? = null,
) : NativeAdLoader {
    override fun load(
        unitId: String,
        size: NativeAdSize,
        onLoaded: (LoadedNativeAd) -> Unit,
        onFailed: (code: Int) -> Unit,
    ) {
        val options = (options ?: nativeRequestOptions(size)).toNativeAdOptions()
        val listener = NativeAdLoadListener(events, onFailed)
        val loader = AdLoader.Builder(context, unitId)
            .forNativeAd { ad ->
                events?.let { metrics ->
                    // Attached at load (T-01), before the ad can be shown.
                    ad.setOnPaidEventListener { metrics.onPaid(it) }
                    metrics.onLoaded(ad)
                }
                val loaded = SdkLoadedNativeAd(ad, events)
                listener.loaded = loaded
                onLoaded(loaded)
            }
            .withAdListener(listener)
            .withNativeAdOptions(options)
            .build()
        events?.onRequestSent()
        loader.loadAd(request)
    }
}

/**
 * The SDK listener of one native load: hands the load error, impression and click to [events], and marks the
 * impression on the [loaded] ad, so a pool destroys it instead of showing it again (P-01a, plan T-028).
 */
internal class NativeAdLoadListener(private val events: NativeAdEvents?, private val onFailed: (Int) -> Unit) :
    AdListener() {
    /** The ad of this load, set as soon as the SDK returns it. */
    var loaded: SdkLoadedNativeAd? = null

    override fun onAdFailedToLoad(error: LoadAdError) {
        events?.onFailed(error.code, error.message)
        onFailed(error.code)
    }

    override fun onAdImpression() {
        events?.onImpression()
        loaded?.markImpression()
    }

    override fun onAdClicked() {
        events?.onClicked()
    }
}

/** A native ad from the SDK; [nativeAd] is bound to the NativeAdView by [BoundNativeAd]. */
internal class SdkLoadedNativeAd(val nativeAd: NativeAd, private val events: NativeAdEvents? = null) :
    LoadedNativeAd {
    override val assets = NativeAdAssets(
        headline = nativeAd.headline.orEmpty(),
        advertiser = nativeAd.advertiser,
        body = nativeAd.body,
        callToAction = nativeAd.callToAction,
        // A drawable without its own size (a plain color) cannot become a bitmap: the icon block is drawn instead.
        icon = nativeAd.icon?.drawable?.let { runCatching { it.toBitmap().asImageBitmap() }.getOrNull() },
        hasMedia = nativeAd.mediaContent != null,
        starRating = nativeAd.starRating,
    )

    override var impressionRecorded: Boolean = false
        private set

    /** The SDK recorded an impression of this ad (P-01a). */
    fun markImpression() {
        impressionRecorded = true
    }

    override fun destroy() {
        events?.onDestroyed()
        nativeAd.destroy()
    }
}
