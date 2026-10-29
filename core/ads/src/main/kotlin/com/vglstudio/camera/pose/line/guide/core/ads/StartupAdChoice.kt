package com.vglstudio.camera.pose.line.guide.core.ads

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * A-01, A-02: the placement shown when the app opens, or null when the key of this open is off. The first open
 * reads only `show_open_ads_first_open`, later opens only `show_open_ads` (both with `is_show_ads` applied);
 * `open_ad_type` = `inter` chooses the interstitial.
 */
internal fun startupAdPlacement(flags: AdsGlobalFlags, firstOpen: Boolean): AdPlacement? {
    val on = if (firstOpen) flags.showOpenAdsFirstOpen else flags.showOpenAds
    if (!on) return null
    return if (flags.openAdType == OpenAdType.INTERSTITIAL) AdPlacement.OPEN_APP_INTER else AdPlacement.OPEN_APP
}

/**
 * The switch of [placement]: for `native_combo`, `is_show_native_after_inter` (plan T-026); for `resume` and
 * `resume_inter`, whether [resumeAdPlacement] chooses it (plan T-025); for another app-wide placement, whether
 * [startupAdPlacement] chooses it (waits for [firstOpen]); for any other placement,
 * `is_show_ads && is_show_<code>` as before. Every load and show path reads it through AdsController.isEnabled (L-072).
 */
internal fun placementSwitch(placement: AdPlacement, flags: AdsFlags, firstOpen: Flow<Boolean>): Flow<Boolean> {
    if (!placement.appWide) return flags.placementEnabled(placement)
    // The combo (plan T-026): `is_show_native_after_inter`, with `is_show_ads` applied; no first-open mark.
    if (placement == AdPlacement.NATIVE_AFTER_INTER) {
        return flags.global.map { it.showNativeAfterInter }.distinctUntilChanged()
    }
    // The resume ad (plan T-025): `show_resum_ads` and `resume_ad_type`; no first-open mark.
    if (placement == AdPlacement.RESUME || placement == AdPlacement.RESUME_INTER) {
        return flags.global.map { resumeAdPlacement(it) == placement }.distinctUntilChanged()
    }
    return combine(flags.global, firstOpen) { global, first -> startupAdPlacement(global, first) == placement }
        .distinctUntilChanged()
}

/**
 * A-12: the placement of the resume ad, or null when `show_resum_ads` (with `is_show_ads`) is off; `resume_ad_type` =
 * `inter` chooses the interstitial (plan T-025).
 */
internal fun resumeAdPlacement(flags: AdsGlobalFlags): AdPlacement? {
    if (!flags.showResumeAds) return null
    return if (flags.resumeAdType == OpenAdType.INTERSTITIAL) AdPlacement.RESUME_INTER else AdPlacement.RESUME
}
