package com.vglstudio.camera.pose.line.guide.core.ads

import kotlinx.coroutines.withTimeoutOrNull

/** Platforms a rewarded ad may load and show from, in order (KB-31, question 41; plan T-106); no unit, no platform. */
internal fun rewardedPlatforms(order: MediationOrder, hasAdmobUnit: Boolean, hasMaxUnit: Boolean): List<AdPlatform> =
    order.platforms.filter {
        when (it) {
            AdPlatform.ADMOB -> hasAdmobUnit
            AdPlatform.MAX -> hasMaxUnit
        }
    }

/**
 * The MAX rewarded slot may send (plan T-106): MAX in [order] and a rewarded placement on, the switches read for at
 * most [RewardedTurns.DECISION_WAIT_MILLIS] (rules, section 11): not read in time is a no, no retry.
 */
internal suspend fun maxRewardedWanted(order: MediationOrder, anyRewardedOn: suspend () -> Boolean): Boolean =
    AdPlatform.MAX in order.platforms &&
        withTimeoutOrNull(RewardedTurns.DECISION_WAIT_MILLIS) { anyRewardedOn() } == true

/**
 * Platforms an interstitial of [placement] may load and show from, in order (KB-31, question 41; plan T-069): the
 * startup interstitial (`open_app_inter`) is AdMob only, like the startup app open ad; any other one follows [order].
 * A platform without its ad unit is dropped, like [bannerPlatforms].
 */
internal fun interstitialPlatforms(
    placement: AdPlacement,
    order: MediationOrder,
    hasAdmobUnit: Boolean,
    hasMaxUnit: Boolean,
): List<AdPlatform> {
    val wanted = if (placement == AdPlacement.OPEN_APP_INTER) listOf(AdPlatform.ADMOB) else order.platforms
    return wanted.filter {
        when (it) {
            AdPlatform.ADMOB -> hasAdmobUnit
            AdPlatform.MAX -> hasMaxUnit
        }
    }
}

/**
 * Platforms an app open ad of [placement] may load and show from, in order (KB-31, question 41; plan T-070): only the
 * resume ad (`resume`) follows [order]; the startup one (`open_app`) and any other app open placement are AdMob only.
 * A platform without its ad unit is dropped.
 */
internal fun appOpenPlatforms(
    placement: AdPlacement,
    order: MediationOrder,
    hasAdmobUnit: Boolean,
    hasMaxUnit: Boolean,
): List<AdPlatform> {
    val wanted = if (placement == AdPlacement.RESUME) order.platforms else listOf(AdPlatform.ADMOB)
    return wanted.filter {
        when (it) {
            AdPlatform.ADMOB -> hasAdmobUnit
            AdPlatform.MAX -> hasMaxUnit
        }
    }
}

/** The first of [platforms] holding an ad now, or null (ADR-0045, section 2.2). */
internal fun readyPlatform(platforms: List<AdPlatform>, admobReady: Boolean, maxReady: Boolean): AdPlatform? =
    platforms.firstOrNull {
        when (it) {
            AdPlatform.ADMOB -> admobReady
            AdPlatform.MAX -> maxReady
        }
    }

/** The load state of a turn: Ready when a platform holds an ad, else the state of the first platform; Idle when none. */
internal fun turnLoadState(
    platforms: List<AdPlatform>,
    admob: FullScreenLoadState,
    max: FullScreenLoadState,
): FullScreenLoadState {
    val states = platforms.map { if (it == AdPlatform.ADMOB) admob else max }
    return when {
        FullScreenLoadState.Ready in states -> FullScreenLoadState.Ready
        else -> states.firstOrNull() ?: FullScreenLoadState.Idle
    }
}

/** For the preload plan (S-22): Loading while any of [platforms] loads, else [turnLoadState]. */
internal fun preloadSettleState(
    platforms: List<AdPlatform>,
    admob: FullScreenLoadState,
    max: FullScreenLoadState,
): FullScreenLoadState {
    val loading = platforms.any { (if (it == AdPlatform.ADMOB) admob else max) == FullScreenLoadState.Loading }
    return if (loading) FullScreenLoadState.Loading else turnLoadState(platforms, admob, max)
}
