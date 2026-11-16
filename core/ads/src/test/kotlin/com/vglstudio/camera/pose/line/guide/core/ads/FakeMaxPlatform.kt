package com.vglstudio.camera.pose.line.guide.core.ads

/**
 * [MaxPlatform] for unit tests (plan T-068): records every [initialize] with its key, and calls the callbacks only
 * when the test calls [finishInitialization], like the SDK that answers later. Every interstitial built is kept in
 * [interstitials] (plan T-069); every app open built in [appOpens] (plan T-070).
 */
internal class FakeMaxPlatform(override var isInitialized: Boolean = false) : MaxPlatform {
    private val keys = mutableListOf<String>()
    private val callbacks = mutableListOf<() -> Unit>()

    /** The keys passed to [initialize], one per call. */
    val initializedKeys: List<String> get() = keys.toList()

    val calls: Int get() = keys.size

    override fun initialize(sdkKey: String, onInitialized: () -> Unit) {
        keys += sdkKey
        callbacks += onInitialized
    }

    /** The MAX interstitials built, in order (plan T-069). */
    val interstitials = mutableListOf<FakeMaxInterstitialSdkAd>()

    override fun newInterstitial(adUnitId: String): MaxInterstitialSdkAd =
        FakeMaxInterstitialSdkAd(adUnitId).also { interstitials += it }

    /** The distinct MAX rewarded objects built, in order (plan T-106). */
    val rewardeds = mutableListOf<FakeMaxInterstitialSdkAd>()

    /** The calls of [rewarded] (plan T-106). */
    var rewardedCalls = 0
        private set

    private val rewardedByUnit = mutableMapOf<String, FakeMaxInterstitialSdkAd>()

    /** Like `MaxRewardedAd.getInstance`: the same object per unit until it is destroyed (plan T-106). */
    override fun rewarded(adUnitId: String): MaxInterstitialSdkAd {
        rewardedCalls++
        return rewardedByUnit.getOrPut(adUnitId) {
            FakeMaxInterstitialSdkAd(
                adUnitId,
                ignoreLoadWhileShowing = true,
                onDestroy = { rewardedByUnit.remove(adUnitId) },
            ).also { rewardeds += it }
        }
    }

    /** The MAX app open ads built, in order (plan T-070): a new object at each call, like `MaxAppOpenAd(...)`. */
    val appOpens = mutableListOf<FakeMaxInterstitialSdkAd>()

    override fun newAppOpen(adUnitId: String): MaxInterstitialSdkAd =
        FakeMaxInterstitialSdkAd(adUnitId).also { appOpens += it }

    fun finishInitialization() {
        callbacks.forEach { it() }
        callbacks.clear()
    }
}
