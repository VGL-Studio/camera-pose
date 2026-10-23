package com.vglstudio.camera.pose.line.guide.core.ads

/**
 * What [runInterstitial] needs, as lambdas, so that the order of the steps is tested without the ads SDK.
 * [AdsController] connects each one to the SDK, Remote Config, the network status and [FullScreenAdGuard].
 */
internal class InterstitialSteps(
    val canRequestAds: () -> Boolean,
    val placementEnabled: suspend () -> Boolean,
    val networkDialogShown: () -> Boolean,
    val breakCap: suspend () -> BreakCap,
    val hasLoadedAd: () -> Boolean,
    val preload: () -> Unit,
    /** True when the shared guard lets an ad show now, after waiting for the user to stop touching the screen. */
    val waitUntilQuiet: suspend () -> Boolean,
    val show: () -> Unit,
    val nowMillis: () -> Long,
    val log: (String) -> Unit,
)

/**
 * One request to show the full-screen ad of [placement] at a natural break. Returns whether it was shown.
 * This is the only place that holds the order of the steps (plan T-018, version 2).
 */
internal suspend fun runInterstitial(
    placement: AdPlacement,
    breaks: InterstitialBreaks,
    steps: InterstitialSteps,
): Boolean {
    // Every call is a break, counted even when no ad follows.
    val breakNumber = breaks.onBreak(placement)
    if (!interstitialAllowed(steps.canRequestAds(), steps.placementEnabled(), steps.networkDialogShown())) {
        steps.log("skipped: not_allowed")
        return false
    }
    if (!breaks.allows(placement, breakNumber, steps.nowMillis(), steps.breakCap())) {
        steps.log("skipped: cap")
        return false
    }
    // No ad loaded: do not make the user wait for the quiet period.
    if (!steps.hasLoadedAd()) {
        steps.preload()
        steps.log("skipped: not_loaded")
        return false
    }
    if (!steps.waitUntilQuiet()) {
        steps.log("skipped: guard")
        return false
    }
    // The network dialog may have opened while waiting.
    if (steps.networkDialogShown()) {
        steps.log("skipped: network_dialog")
        return false
    }
    steps.show()
    breaks.onShown(placement, breakNumber, steps.nowMillis())
    return true
}
