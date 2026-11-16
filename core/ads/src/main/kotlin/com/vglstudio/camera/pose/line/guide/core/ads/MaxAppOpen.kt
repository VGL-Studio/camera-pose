package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Activity
import android.content.Context
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxAppOpenAd

/**
 * The real `MaxAppOpenAd` (plan T-070), for the resume ad only (KB-31, criterion 3). Built only once MAX is ready (the
 * SDK is initialized), on the first load of the slot, like [AppLovinMaxInterstitial]. The SDK has no show on a given
 * activity for app open: it shows on the activity it follows; the slot checks the activity of the app (F-08) before.
 */
internal class AppLovinMaxAppOpen(adUnitId: String, context: Context) : MaxInterstitialSdkAd {
    private val ad = MaxAppOpenAd(adUnitId, context)

    override var listener: MaxInterstitialListener? = null

    override var onRevenue: ((MaxAdInfo) -> Unit)? = null

    init {
        ad.setRevenueListener { onRevenue?.invoke(maxAdInfoOf(it)) }
        ad.setListener(
            object : MaxAdListener {
                override fun onAdLoaded(maxAd: MaxAd) {
                    listener?.onLoaded(maxAdInfoOf(maxAd))
                }

                override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
                    listener?.onLoadFailed(error.code, maxNetworkAttemptsOf(error.waterfall))
                }

                override fun onAdDisplayed(maxAd: MaxAd) {
                    listener?.onDisplayed()
                }

                override fun onAdHidden(maxAd: MaxAd) {
                    listener?.onHidden()
                }

                override fun onAdDisplayFailed(maxAd: MaxAd, error: MaxError) {
                    listener?.onDisplayFailed(error.code)
                }

                override fun onAdClicked(maxAd: MaxAd) {
                    listener?.onClicked()
                }
            },
        )
    }

    override fun load() = ad.loadAd()

    // MaxAppOpenAd 13.6.4 has showAd() and showAd(String) only: the SDK picks the activity.
    override fun show(activity: Activity) = ad.showAd()

    override fun destroy() = ad.destroy()
}
