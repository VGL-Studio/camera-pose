package com.vglstudio.camera.pose.line.guide.core.ads

import android.content.Context
import com.applovin.sdk.AppLovinMediationProvider
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkInitializationConfiguration
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The AppLovin MAX SDK key of the build (ADR-0037), given by :app: `-PmaxSdkKey` in production, `ads.maxSdkKey` of
 * local.properties in staging. Without a key MAX never starts. [toString] hides the key, so it reaches no log.
 *
 * [adUnits] holds the MAX ad unit of each slot of the app, `<slot>=<unit>;...` with the slot named by [AdFormat.code]
 * (`banner`, `banner_mrec`, plan T-071; `interstitial`, plan T-069; `rewarded`, plan T-106; `app_open`, plan T-070):
 * `-PmaxAdUnits` in production, `ads.maxAdUnits` of local.properties in staging. [toString] counts the slots with a unit and never shows one.
 */
data class MaxConfig(val sdkKey: String, val adUnits: String = "") {
    val hasKey: Boolean get() = sdkKey.isNotBlank()

    /** Units by slot code; a malformed entry (no `=`, empty slot or unit) is skipped: that slot runs without MAX. */
    private val units: Map<String, String> by lazy {
        adUnits.split(';').mapNotNull { entry ->
            val slot = entry.substringBefore('=', missingDelimiterValue = "").trim()
            val unit = entry.substringAfter('=', missingDelimiterValue = "").trim()
            if (slot.isEmpty() || unit.isEmpty()) null else slot to unit
        }.toMap()
    }

    /**
     * The MAX ad unit of [format]: [AdFormat.BANNER], [AdFormat.BANNER_MREC], [AdFormat.INTERSTITIAL],
     * [AdFormat.REWARDED] and [AdFormat.APP_OPEN] (plan T-070); none without an SDK key.
     */
    fun unitFor(format: AdFormat): String? {
        if (!hasKey || format !in MAX_FORMATS) return null
        return units[format.code]
    }

    override fun toString(): String {
        val slots = MAX_FORMATS.count { unitFor(it) != null }
        return if (hasKey) "MaxConfig(sdkKey=${sdkKey.take(4)}…, units=$slots)" else "MaxConfig(no key)"
    }

    companion object {
        val NONE = MaxConfig("")

        /** The formats that may load from MAX (plans T-071, T-069, T-106, T-070). */
        internal val MAX_FORMATS =
            listOf(AdFormat.BANNER, AdFormat.BANNER_MREC, AdFormat.INTERSTITIAL, AdFormat.REWARDED, AdFormat.APP_OPEN)
    }
}

/** The AppLovin MAX SDK as :core:ads uses it; a fake in tests. */
internal interface MaxPlatform {
    /** True once the shared AppLovin SDK object is initialized, by MAX or by the AppLovin adapter of AdMob. */
    val isInitialized: Boolean

    fun initialize(sdkKey: String, onInitialized: () -> Unit)

    /** A new MAX interstitial of [adUnitId] (plan T-069); built once MAX is ready. */
    fun newInterstitial(adUnitId: String): MaxInterstitialSdkAd

    /**
     * The one MAX rewarded of [adUnitId] (plan T-106): the SDK keeps one object per ad unit, so the same wrapper comes
     * back for the same unit until it is destroyed; built once MAX is ready.
     */
    fun rewarded(adUnitId: String): MaxInterstitialSdkAd

    /**
     * A new MAX app open ad of [adUnitId] (plan T-070), a new object at each call like [newInterstitial]; built once
     * MAX is ready.
     */
    fun newAppOpen(adUnitId: String): MaxInterstitialSdkAd
}

/**
 * The real SDK. Consent is not set here: MAX reads the TCF and AC strings that UMP writes (plan T-068, section 2), so
 * neither `AppLovinPrivacySettings` nor the consent flow of MAX is used.
 */
internal class AppLovinMaxPlatform(private val context: Context) : MaxPlatform {
    override val isInitialized: Boolean get() = AppLovinSdk.getInstance(context).isInitialized

    override fun initialize(sdkKey: String, onInitialized: () -> Unit) {
        val configuration = AppLovinSdkInitializationConfiguration.builder(sdkKey)
            .setMediationProvider(AppLovinMediationProvider.MAX)
            .build()
        AppLovinSdk.getInstance(context).initialize(configuration) { onInitialized() }
    }

    override fun newInterstitial(adUnitId: String): MaxInterstitialSdkAd = AppLovinMaxInterstitial(adUnitId, context)

    /** One wrapper per ad unit, like the SDK object behind it (L-068); dropped when destroyed. */
    private val rewardeds = mutableMapOf<String, AppLovinMaxRewarded>()

    override fun rewarded(adUnitId: String): MaxInterstitialSdkAd = rewardeds.getOrPut(adUnitId) {
        AppLovinMaxRewarded(adUnitId, context, onDestroyed = { rewardeds.remove(adUnitId) })
    }

    override fun newAppOpen(adUnitId: String): MaxInterstitialSdkAd = AppLovinMaxAppOpen(adUnitId, context)
}

/**
 * Starts AppLovin MAX once (plan T-068, KB-31, ADR-0037): after the consent gate opened and AdMob is ready
 * ([onAdmobReady], called once by `startSdk`), with an SDK key, while `ads_mediation_order` has MAX, and only while the
 * consent gate is open at that moment ([gateAllowed], question 9 of the plan): a gate closed while `startSdk` waited
 * for AdMob, or when the order changed, starts nothing; the gate open again asks once more ([onOrderChanged]). There
 * is no stop: withdrawn consent after the start closes the gate, which every request of MAX asks (S-15). Main thread.
 */
internal class MaxSdkStarter(
    private val config: MaxConfig,
    private val platform: MaxPlatform,
    private val logger: AppLogger,
    private val gateAllowed: () -> Boolean,
) {
    private var gateOpened = false
    private var started = false
    private val _ready = MutableStateFlow(false)

    /** True once MAX answered its initialization. */
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    /** The consent gate opened and AdMob is ready (S-21). */
    fun onAdmobReady(order: MediationOrder) {
        if (gateOpened) return
        gateOpened = true
        if (!config.hasKey) logger.info(TAG, "MAX skipped: no SDK key")
        maybeStart(order)
    }

    /** `ads_mediation_order` changed remotely, or the consent gate opened again (S-14). */
    fun onOrderChanged(order: MediationOrder) = maybeStart(order)

    /** The only call of [MaxPlatform.initialize] (L-072). */
    private fun maybeStart(order: MediationOrder) {
        if (!gateOpened || started || !config.hasKey || !order.usesMax || !gateAllowed()) return
        started = true
        if (platform.isInitialized) {
            logger.warn(
                TAG,
                "MAX: AppLovin SDK already started by AdMob mediation; " +
                    "remove AppLovin from the AdMob mediation groups (ADR-0037)",
            )
        }
        logger.info(TAG, "MAX started")
        platform.initialize(config.sdkKey) {
            _ready.value = true
            logger.info(TAG, "MAX ready")
        }
    }

    private companion object {
        const val TAG = "Ads"
    }
}
