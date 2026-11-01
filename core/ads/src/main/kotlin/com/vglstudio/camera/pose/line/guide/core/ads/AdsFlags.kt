package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.config.RemoteSettings
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * The ad keys of Remote Config ([AdsSettings]) as flows, the only way :core:ads reads them (plan T-019). Every flow
 * emits the current value at once, then again on every change, so a value switched remotely applies without an
 * update of the app (ADR-0003, KB-7). Public for the app shell (app open at the loading screen, T-024).
 */
interface AdsFlags {
    /**
     * Global ad switches, master switch (is_show_ads && !adFree, KB-30) already applied. Emits the current value at
     * once, then on every change.
     */
    val global: Flow<AdsGlobalFlags>

    /** is_show_ads && !adFree && is_show_<code> (adFree: [AdFreeStatus], KB-30). */
    fun placementEnabled(placement: AdPlacement): Flow<Boolean>

    /** ads_interval for DEFAULT, ads_interval_<name> otherwise, in seconds, never negative. */
    fun intervalSeconds(group: AdIntervalGroup): Flow<Long>

    /** native_every_<code>, or null when the placement declares no nativeEvery. */
    fun nativeEvery(placement: AdPlacement): Flow<Long?>
}

/** The global ad keys of docs/ops/ads-business-rules.md, section 10.1, read together. */
data class AdsGlobalFlags(
    /** is_show_ads && !adFree: the master switch, off too while the user is ad-free ([AdFreeStatus], KB-30). */
    val adsEnabled: Boolean,
    /** adsEnabled && show_open_ads. */
    val showOpenAds: Boolean,
    /** adsEnabled && show_open_ads_first_open. */
    val showOpenAdsFirstOpen: Boolean,
    /** open_ad_type. */
    val openAdType: OpenAdType,
    /** adsEnabled && show_resum_ads. */
    val showResumeAds: Boolean,
    /** resume_ad_type. */
    val resumeAdType: OpenAdType,
    /** ads_interval, negative read as 0. */
    val adsIntervalSeconds: Long,
    /** adsEnabled && is_show_native_after_inter. */
    val showNativeAfterInter: Boolean,
    /** is_show_native_bottom. */
    val showNativeBottom: Boolean,
    /** is_show_native_bottom && is_show_native_collap (D-01). */
    val showNativeCollap: Boolean,
    /** time_expand_collap_ad, negative read as 0; 0 means never. */
    val expandCollapAfterSeconds: Long,
    /** is_enable_consent_shortcut. */
    val consentShortcutEnabled: Boolean,
    /** is_log_ad_fullscreen_events. */
    val logFullScreenEvents: Boolean,
    /** is_log_ad_slot_events. */
    val logSlotEvents: Boolean,
    /** time_close_native, 1 to 30, any other value read as 5. */
    val closeNativeCountdownSeconds: Long,
    /** ads_mediation_order, read as written; [AdsController.mediationOrder] applies the SDK key. */
    val mediationOrder: MediationOrder = MediationOrder.DEFAULT,
)

/** The ad shown when the app opens or comes back (open_ad_type, resume_ad_type). */
enum class OpenAdType {
    APP_OPEN,
    INTERSTITIAL,
    ;

    internal companion object {
        /** Only the exact value `inter` is an interstitial; any other one (`INTER`, empty) is app open (A-12). */
        fun of(value: String): OpenAdType = if (value == "inter") INTERSTITIAL else APP_OPEN
    }
}

/**
 * The flags read from [settings], with the ad-free signal of the app ([adFreeStatus], KB-30) in the master switch. The
 * default of [adFreeStatus] is for JVM tests that build this class directly; Hilt always injects the binding of :app.
 */
internal class DefaultAdsFlags @Inject constructor(
    private val settings: RemoteSettings,
    private val adFreeStatus: AdFreeStatus = AdFreeStatus.NONE,
) : AdsFlags {
    /** is_show_ads, off too while the user is ad-free (KB-30): the one master switch of every flag below. */
    private val masterSwitch: Flow<Boolean> =
        combine(settings.value(AdsSettings.IS_SHOW_ADS), adFreeStatus.adFree) { on, adFree -> on && !adFree }
            .distinctUntilChanged()

    override val global: Flow<AdsGlobalFlags> = combine(
        listOf(
            masterSwitch,
            settings.value(AdsSettings.SHOW_OPEN_ADS),
            settings.value(AdsSettings.SHOW_OPEN_ADS_FIRST_OPEN),
            settings.value(AdsSettings.OPEN_AD_TYPE),
            settings.value(AdsSettings.SHOW_RESUM_ADS),
            settings.value(AdsSettings.RESUME_AD_TYPE),
            settings.value(AdsSettings.ADS_INTERVAL),
            settings.value(AdsSettings.IS_SHOW_NATIVE_AFTER_INTER),
            settings.value(AdsSettings.IS_SHOW_NATIVE_BOTTOM),
            settings.value(AdsSettings.IS_SHOW_NATIVE_COLLAP),
            settings.value(AdsSettings.TIME_EXPAND_COLLAP_AD),
            settings.value(AdsSettings.IS_ENABLE_CONSENT_SHORTCUT),
            settings.value(AdsSettings.IS_LOG_AD_FULLSCREEN_EVENTS),
            settings.value(AdsSettings.IS_LOG_AD_SLOT_EVENTS),
            settings.value(AdsSettings.TIME_CLOSE_NATIVE),
            settings.value(AdsSettings.ADS_MEDIATION_ORDER),
        ),
    ) { values ->
        val adsEnabled = values[0] as Boolean
        val showNativeBottom = values[8] as Boolean
        AdsGlobalFlags(
            adsEnabled = adsEnabled,
            showOpenAds = adsEnabled && values[1] as Boolean,
            showOpenAdsFirstOpen = adsEnabled && values[2] as Boolean,
            openAdType = OpenAdType.of(values[3] as String),
            showResumeAds = adsEnabled && values[4] as Boolean,
            resumeAdType = OpenAdType.of(values[5] as String),
            adsIntervalSeconds = seconds(values[6] as Long),
            showNativeAfterInter = adsEnabled && values[7] as Boolean,
            showNativeBottom = showNativeBottom,
            showNativeCollap = showNativeBottom && values[9] as Boolean,
            expandCollapAfterSeconds = seconds(values[10] as Long),
            consentShortcutEnabled = values[11] as Boolean,
            logFullScreenEvents = values[12] as Boolean,
            logSlotEvents = values[13] as Boolean,
            closeNativeCountdownSeconds = nativeCloseCountdownSeconds(values[14] as Long),
            mediationOrder = MediationOrder.of(values.last() as String),
        )
    }.distinctUntilChanged()

    override fun placementEnabled(placement: AdPlacement): Flow<Boolean> = placementEnabled(placement.declaration)

    internal fun placementEnabled(declaration: AdPlacementDeclaration): Flow<Boolean> = combine(
        masterSwitch,
        settings.value(AdsSettings.placementEnabled(declaration)),
    ) { adsEnabled, placementEnabled -> adsEnabled && placementEnabled }
        .distinctUntilChanged()

    override fun intervalSeconds(group: AdIntervalGroup): Flow<Long> =
        settings.value(AdsSettings.interval(group)).map(::seconds).distinctUntilChanged()

    override fun nativeEvery(placement: AdPlacement): Flow<Long?> = nativeEvery(placement.declaration)

    internal fun nativeEvery(declaration: AdPlacementDeclaration): Flow<Long?> {
        val key = AdsSettings.nativeEvery(declaration) ?: return flowOf(null)
        return settings.value(key).distinctUntilChanged()
    }

    /** Negative remote values count as 0; the cap keeps a conversion to milliseconds from overflowing. */
    private fun seconds(value: Long): Long = value.coerceIn(0, MAX_SECONDS)

    private companion object {
        const val MAX_SECONDS = Long.MAX_VALUE / 1_000
    }
}
