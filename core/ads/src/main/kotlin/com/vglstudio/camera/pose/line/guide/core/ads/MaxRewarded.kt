package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Activity
import android.content.Context
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxError
import com.applovin.mediation.MaxReward
import com.applovin.mediation.MaxRewardedAdListener
import com.applovin.mediation.ads.MaxRewardedAd

/**
 * The real `MaxRewardedAd` of [adUnitId] (plan T-106). The SDK keeps one object per ad unit (`getInstance`), and drops
 * a load sent while that object shows; [destroy] removes it from the SDK, so the next `getInstance` builds a new one,
 * and [onDestroyed] lets the platform drop this wrapper too. Built only once MAX is ready (the SDK is initialized), on
 * the first load of the slot.
 */
internal class AppLovinMaxRewarded(adUnitId: String, context: Context, private val onDestroyed: () -> Unit) :
    MaxInterstitialSdkAd {
    private val ad = MaxRewardedAd.getInstance(adUnitId, context)

    override var listener: MaxInterstitialListener? = null

    override var onRevenue: ((MaxAdInfo) -> Unit)? = null

    init {
        ad.setRevenueListener { onRevenue?.invoke(maxAdInfoOf(it)) }
        ad.setListener(
            object : MaxRewardedAdListener {
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

                // R-02: the reward, before the ad closes.
                override fun onUserRewarded(maxAd: MaxAd, reward: MaxReward) {
                    listener?.onRewarded()
                }
            },
        )
    }

    override fun load() = ad.loadAd()

    override fun show(activity: Activity) = ad.showAd(activity)

    override fun destroy() {
        ad.destroy()
        onDestroyed()
    }
}
