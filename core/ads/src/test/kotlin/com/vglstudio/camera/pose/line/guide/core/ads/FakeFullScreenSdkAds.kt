package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Activity
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.ResponseInfo
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.rewarded.OnAdMetadataChangedListener
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.ServerSideVerificationOptions

/**
 * Full-screen ads of the SDK with no SDK behind them (plan T-055): they count the calls of `show` and keep the activity
 * each one got; the test fires the callbacks through [FakeFullScreenSdkAd.callback]. Put in a slot with
 * [AdsController.putLoadedInterstitialForTest] and the like.
 */
internal interface FakeFullScreenSdkAd {
    val shows: Int
    val shownOn: List<Activity>
    val callback: FullScreenContentCallback?
}

/** A response with no adapter, which the ad of the SDK gives when it has no mediation info. */
private fun emptyResponseInfo(): ResponseInfo {
    val constructor = ResponseInfo::class.java.declaredConstructors.single { it.parameterCount == 1 }
    constructor.isAccessible = true
    return constructor.newInstance(null) as ResponseInfo
}

internal class FakeInterstitialAd :
    InterstitialAd(),
    FakeFullScreenSdkAd {
    override val shownOn = mutableListOf<Activity>()
    override val shows: Int get() = shownOn.size
    override var callback: FullScreenContentCallback? = null
    private var paid: OnPaidEventListener? = null
    private var placement = 0L

    override fun getAdUnitId(): String = UNIT_ID

    override fun show(activity: Activity) {
        shownOn += activity
    }

    override fun setFullScreenContentCallback(callback: FullScreenContentCallback?) {
        this.callback = callback
    }

    override fun getFullScreenContentCallback(): FullScreenContentCallback? = callback

    override fun setImmersiveMode(immersive: Boolean) = Unit

    override fun getResponseInfo(): ResponseInfo = emptyResponseInfo()

    override fun setOnPaidEventListener(listener: OnPaidEventListener?) {
        paid = listener
    }

    override fun getOnPaidEventListener(): OnPaidEventListener? = paid

    override fun getPlacementId(): Long = placement

    override fun setPlacementId(id: Long) {
        placement = id
    }
}

internal class FakeAppOpenAd :
    AppOpenAd(),
    FakeFullScreenSdkAd {
    override val shownOn = mutableListOf<Activity>()
    override val shows: Int get() = shownOn.size
    override var callback: FullScreenContentCallback? = null
    private var paid: OnPaidEventListener? = null
    private var placement = 0L

    override fun show(activity: Activity) {
        shownOn += activity
    }

    override fun getResponseInfo(): ResponseInfo = emptyResponseInfo()

    override fun getAdUnitId(): String = UNIT_ID

    override fun setFullScreenContentCallback(callback: FullScreenContentCallback?) {
        this.callback = callback
    }

    override fun getFullScreenContentCallback(): FullScreenContentCallback? = callback

    override fun setOnPaidEventListener(listener: OnPaidEventListener?) {
        paid = listener
    }

    override fun getOnPaidEventListener(): OnPaidEventListener? = paid

    override fun setImmersiveMode(immersive: Boolean) = Unit

    override fun getPlacementId(): Long = placement

    override fun setPlacementId(id: Long) {
        placement = id
    }
}

internal class FakeRewardedAd :
    RewardedAd(),
    FakeFullScreenSdkAd {
    override val shownOn = mutableListOf<Activity>()
    override val shows: Int get() = shownOn.size
    override var callback: FullScreenContentCallback? = null

    /** The reward listener of the last show; [earnReward] calls it as the SDK does. */
    private var rewardListener: OnUserEarnedRewardListener? = null
    private var paid: OnPaidEventListener? = null
    private var metadataListener: OnAdMetadataChangedListener? = null
    private var placement = 0L

    /** The SDK reports the reward of the last show. */
    fun earnReward() {
        rewardListener!!.onUserEarnedReward(RewardItem.DEFAULT_REWARD)
    }

    override fun setServerSideVerificationOptions(options: ServerSideVerificationOptions?) = Unit

    override fun setOnAdMetadataChangedListener(listener: OnAdMetadataChangedListener?) {
        metadataListener = listener
    }

    override fun getOnAdMetadataChangedListener(): OnAdMetadataChangedListener? = metadataListener

    override fun getAdMetadata(): android.os.Bundle = android.os.Bundle()

    override fun show(activity: Activity, listener: OnUserEarnedRewardListener) {
        shownOn += activity
        rewardListener = listener
    }

    override fun getRewardItem(): RewardItem = RewardItem.DEFAULT_REWARD

    override fun getResponseInfo(): ResponseInfo = emptyResponseInfo()

    override fun setOnPaidEventListener(listener: OnPaidEventListener?) {
        paid = listener
    }

    override fun getOnPaidEventListener(): OnPaidEventListener? = paid

    override fun setFullScreenContentCallback(callback: FullScreenContentCallback?) {
        this.callback = callback
    }

    override fun getFullScreenContentCallback(): FullScreenContentCallback? = callback

    override fun getAdUnitId(): String = UNIT_ID

    override fun setImmersiveMode(immersive: Boolean) = Unit

    override fun getPlacementId(): Long = placement

    override fun setPlacementId(id: Long) {
        placement = id
    }
}

/** Google's test unit of the format does not matter here: the fake never loads. */
private const val UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
