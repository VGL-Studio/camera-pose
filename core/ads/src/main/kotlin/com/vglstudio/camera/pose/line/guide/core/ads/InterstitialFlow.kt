package com.vglstudio.camera.pose.line.guide.core.ads

/** The steps [runInterstitial] takes, as lambdas so that the order can be tested without the SDK. */
internal class InterstitialSteps(
    val canRequestAds: () -> Boolean,
    val placementEnabled: suspend () -> Boolean,
    val networkDialogShown: () -> Boolean,
    val breakCap: suspend () -> BreakCap,
    val hasLoadedAd: () -> Boolean,
    val preload: () -> Unit,
    val waitUntilQuiet: suspend () -> Boolean,
    val show: () -> Unit,
    val nowMillis: () -> Long,
    val log: (String) -> Unit,
)

/** Stub: behavior comes with the code. */
internal suspend fun runInterstitial(
    placement: AdPlacement,
    breaks: InterstitialBreaks,
    steps: InterstitialSteps,
): Boolean = false
