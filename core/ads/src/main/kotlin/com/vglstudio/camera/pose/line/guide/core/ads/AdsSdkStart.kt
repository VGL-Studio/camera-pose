package com.vglstudio.camera.pose.line.guide.core.ads

/**
 * Starts the ad SDKs in order (plan T-068): AdMob, then AppLovin MAX once AdMob is ready (its callback or the 35 s cap
 * of S-21), then the first preload. The Google adapter of MAX calls `MobileAds.disableMediationAdapterInitialization`
 * before its own `MobileAds.initialize`, so MAX before AdMob's adapters are initialized would leave the mediation
 * adapters of AdMob uninitialized. The call that starts AdMob returns at once: waiting for it is not enough.
 */
internal suspend fun startAdsSdks(
    initAdmob: suspend () -> Unit,
    awaitAdmobReady: suspend () -> Unit,
    startMax: () -> Unit,
    preload: () -> Unit,
) {
    initAdmob()
    awaitAdmobReady()
    startMax()
    preload()
}
