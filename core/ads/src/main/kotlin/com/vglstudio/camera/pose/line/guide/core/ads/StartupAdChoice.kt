package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.config.RemoteSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
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
 * [startupAdPlacement] with the current values of [settings], read as :core:ads reads them (`is_show_ads` applied).
 * Public only so :app can check the local values of its builds against the framework's own choice (plan T-074,
 * section 4, `firstOpen_choosesNoStartupAd`); nothing else calls it.
 */
suspend fun startupAdPlacementFor(settings: RemoteSettings, firstOpen: Boolean): AdPlacement? =
    startupAdPlacement(DefaultAdsFlags(settings).global.first(), firstOpen)

/**
 * The switch of [placement]: for `native_combo`, `is_show_native_after_inter` (plan T-026); for `resume` and
 * `resume_inter`, whether [resumeAdPlacement] chooses it (plan T-025); for another app-wide placement, whether
 * [startupAdPlacement] chooses it (waits for [firstOpen]); for a [AdPlacement.firstOpenOnly] placement,
 * `is_show_ads && is_show_<code>` in the first open only (waits for [firstOpen], plan T-083); for any other placement,
 * `is_show_ads && is_show_<code>` as before. Every load and show path reads it through AdsController.isEnabled (L-072).
 */
internal fun placementSwitch(placement: AdPlacement, flags: AdsFlags, firstOpen: Flow<Boolean>): Flow<Boolean> {
    if (!placement.appWide) {
        if (!placement.firstOpenOnly) return flags.placementEnabled(placement)
        // Shown only in the first open (plan T-083): `is_show_<code>` and the first-open mark.
        return combine(flags.placementEnabled(placement), firstOpen) { on, first -> on && first }.distinctUntilChanged()
    }
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
