package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation.PrivacyOptionsRequirementStatus
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.vglstudio.camera.pose.line.guide.core.config.RemoteSettings
import com.vglstudio.camera.pose.line.guide.core.network.NetworkStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The only class that talks to the ads SDKs (ADR-0004). :app calls [gatherConsent] and [onUserInteraction]
 * from its activity and provides this controller to the UI through [LocalAdsController]. Features use [AdSlot],
 * [rememberShowInterstitial] and [PrivacyOptionsButton], never this class or an SDK directly.
 *
 * The ads SDK is initialized only once the consent form allows ads to be requested.
 */
@Singleton
class AdsController @Inject internal constructor(
    @param:ApplicationContext private val context: Context,
    private val unitIds: AdUnitIds,
    private val consentDebug: ConsentDebugOptions,
    private val settings: RemoteSettings,
    private val networkStatus: NetworkStatus,
) {
    private val consentInformation by lazy { UserMessagingPlatform.getConsentInformation(context) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val initialized = AtomicBoolean(false)

    // Touched on the main thread only.
    private val guard = FullScreenAdGuard()
    private val breaks = InterstitialBreaks()
    private val loadedInterstitials = mutableMapOf<AdPlacement, InterstitialAd>()

    private val _canRequestAds = MutableStateFlow(false)

    /** True once consent allows ads and the SDK is started. */
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)

    /**
     * True when the user must be able to reopen the consent form ([PrivacyOptionsButton]).
     * This is also how the module knows the user is in a region where the law requires consent.
     */
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    /**
     * Updates the consent status and shows the consent form when required. Call it in the activity's
     * `onCreate` at every start. Consent given in an earlier session lets ads start while the update runs.
     */
    fun gatherConsent(activity: Activity) {
        consentInformation.requestConsentInfoUpdate(
            activity,
            consentParameters(activity),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                    error?.let { Log.w(TAG, "Consent form failed: ${it.errorCode} ${it.message}") }
                    onConsentUpdated()
                }
            },
            { error ->
                Log.w(TAG, "Consent update failed: ${error.errorCode} ${error.message}")
                onConsentUpdated()
            },
        )
        onConsentUpdated()
    }

    /** Opens the privacy options form, so the user can change their consent at any time. */
    fun showPrivacyOptionsForm(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            error?.let { Log.w(TAG, "Privacy options form failed: ${it.errorCode} ${it.message}") }
            onConsentUpdated()
        }
    }

    /** Call from `Activity.onUserInteraction`: full-screen ads wait until the user stops touching the screen. */
    fun onUserInteraction() {
        guard.onUserInteraction(SystemClock.elapsedRealtime())
    }

    /**
     * Shows the full-screen ad of [placement] if the guardrails allow it, and returns whether it was shown.
     * Call it at a natural break (end of a level, after saving), never when the app opens or closes.
     */
    suspend fun showInterstitial(activity: Activity, placement: AdPlacement): Boolean =
        withContext(Dispatchers.Main.immediate) {
            require(placement.format == AdFormat.INTERSTITIAL) { "${placement.key} is not a full-screen placement" }
            runInterstitial(placement, breaks, interstitialSteps(activity, placement))
        }

    private fun interstitialSteps(activity: Activity, placement: AdPlacement) = InterstitialSteps(
        canRequestAds = { canRequestAds.value },
        placementEnabled = { isEnabled(placement).first() },
        networkDialogShown = { networkStatus.dialogShown.value },
        breakCap = { breakCapOf(placement) },
        hasLoadedAd = { placement in loadedInterstitials },
        preload = { preloadInterstitial(placement) },
        waitUntilQuiet = { guardAllowsShowing() },
        show = { showLoaded(activity, placement) },
        nowMillis = { SystemClock.elapsedRealtime() },
        log = { Log.i(TAG, "Interstitial ${placement.key} $it") },
    )

    private suspend fun breakCapOf(placement: AdPlacement): BreakCap {
        val defaults = AdsSettings.breakCapDefaults(placement)
        return BreakCap(
            firstBreak = settings.long(
                AdsSettings.placementFirstBreakKey(placement),
                defaults.firstBreak.toLong(),
            ).first().toInt(),
            breaksBetween = settings.long(
                AdsSettings.placementBreaksBetweenKey(placement),
                defaults.breaksBetween.toLong(),
            ).first().toInt(),
            minIntervalSeconds = settings.long(
                AdsSettings.placementMinIntervalKey(placement),
                defaults.minIntervalSeconds,
            ).first(),
        )
    }

    /** True when the shared guard lets a full-screen ad show, after waiting at most [FullScreenAdGuard.MAX_WAIT_MILLIS]. */
    private suspend fun guardAllowsShowing(): Boolean {
        val minIntervalMillis = settings.long(
            AdsSettings.INTERSTITIAL_MIN_INTERVAL_SECONDS,
            AdsSettings.INTERSTITIAL_MIN_INTERVAL_SECONDS_DEFAULT,
        ).first() * 1_000
        val deadline = SystemClock.elapsedRealtime() + FullScreenAdGuard.MAX_WAIT_MILLIS
        while (true) {
            val now = SystemClock.elapsedRealtime()
            when (val decision = guard.decide(now, minIntervalMillis)) {
                FullScreenAdGuard.Decision.TooSoon -> return false
                is FullScreenAdGuard.Decision.Wait ->
                    if (now + decision.millis > deadline) return false else delay(decision.millis)
                FullScreenAdGuard.Decision.Show -> return true
            }
        }
    }

    private fun showLoaded(activity: Activity, placement: AdPlacement) {
        val ad = loadedInterstitials.remove(placement) ?: return
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = preloadInterstitial(placement)

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Interstitial ${placement.key} failed to show: ${error.message}")
                preloadInterstitial(placement)
            }
        }
        guard.onShown(SystemClock.elapsedRealtime())
        ad.show(activity)
    }

    internal fun isEnabled(placement: AdPlacement): Flow<Boolean> =
        settings.boolean(AdsSettings.placementEnabledKey(placement), AdsSettings.PLACEMENT_ENABLED_DEFAULT)

    internal fun unitId(placement: AdPlacement): String? = unitIds.of(placement).also {
        if (it == null) Log.e(TAG, "No ad unit for placement ${placement.key}; it shows no ad")
    }

    internal suspend fun adRequest(): AdRequest {
        val nonPersonalized = requestsNonPersonalizedAds(
            inRegulatedRegion = privacyOptionsRequired.value,
            personalizedInRegulatedRegions = settings.boolean(
                AdsSettings.PERSONALIZED_IN_REGULATED_REGIONS,
                AdsSettings.PERSONALIZED_IN_REGULATED_REGIONS_DEFAULT,
            ).first(),
        )
        val builder = AdRequest.Builder()
        if (nonPersonalized) {
            builder.addNetworkExtrasBundle(AdMobAdapter::class.java, Bundle().apply { putString("npa", "1") })
        }
        return builder.build()
    }

    private fun consentParameters(activity: Activity): ConsentRequestParameters {
        val builder = ConsentRequestParameters.Builder()
        if (consentDebug.forceEea) {
            builder.setConsentDebugSettings(
                ConsentDebugSettings.Builder(activity)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                    .setForceTesting(true)
                    .build(),
            )
        }
        return builder.build()
    }

    private fun onConsentUpdated() {
        _privacyOptionsRequired.value =
            consentInformation.privacyOptionsRequirementStatus == PrivacyOptionsRequirementStatus.REQUIRED
        if (consentInformation.canRequestAds() && initialized.compareAndSet(false, true)) {
            scope.launch {
                // Google recommends starting the SDK off the main thread.
                withContext(Dispatchers.IO) { MobileAds.initialize(context) }
                _canRequestAds.value = true
                AdPlacement.entries.filter { it.format == AdFormat.INTERSTITIAL }.forEach(::preloadWhenEnabled)
            }
        }
    }

    /** Placements are off by default (ADR-0014): load an ad only once the placement is switched on remotely. */
    private fun preloadWhenEnabled(placement: AdPlacement) {
        scope.launch {
            isEnabled(placement).distinctUntilChanged().filter { it }.collect { preloadInterstitial(placement) }
        }
    }

    private fun preloadInterstitial(placement: AdPlacement) {
        val unitId = unitId(placement) ?: return
        scope.launch {
            if (placement in loadedInterstitials || !isEnabled(placement).first()) return@launch
            InterstitialAd.load(
                context,
                unitId,
                adRequest(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        loadedInterstitials[placement] = ad
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w(TAG, "Interstitial ${placement.key} failed to load: ${error.message}")
                    }
                },
            )
        }
    }

    private companion object {
        const val TAG = "Ads"
    }
}
