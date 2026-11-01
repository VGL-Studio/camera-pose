package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.vglstudio.camera.pose.line.guide.core.analytics.AdFunnel
import com.vglstudio.camera.pose.line.guide.core.analytics.AdMetrics
import com.vglstudio.camera.pose.line.guide.core.analytics.AppsFlyerStartup
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.core.network.InternetConnectivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The only class that talks to the ads SDKs (ADR-0004). :app calls [start] once in `Application.onCreate` (HD-01),
 * [gatherConsent] from its activity, places [AdsHost] at the root of its UI
 * (it provides [LocalAdsController], G-04), and draws native full-screen ads with [FullScreenAdHost]. Features use
 * [AdSlot], [NativeAdSlot], [rememberShowFullScreenAd], [rememberRewardedAction], [rememberAdSuppression],
 * [SuppressResumeAds], [rememberMarkLeavingApp] and [PrivacyOptionsButton], never this class or an SDK directly.
 *
 * Full-screen ads follow docs/ops/ads-business-rules.md, sections 3, 4 and 6 (plan T-022): one decision order
 * ([decideFullScreenShow]), a showing flag with a cap ([FullScreenAdPresence]), `ads_interval` marks per interval
 * group written when an interstitial closes ([FullScreenAdGuard], [MonotonicClock]), stuck recovery
 * ([StuckFullScreenRecovery]) and a completion that runs once ([FullScreenCompletion]). Lifecycle events of the app's
 * activities come from [AdsActivityTracker], so :app calls nothing more for them.
 *
 * Ad metrics follow sections 8.1 and 8.2 (plan T-023): every format listens to the SDK's paid events and its load,
 * click and show callbacks, and hands what it reads to [AdTelemetry], which decides what is logged through
 * [AdMetrics]. Probing the state ([isFullScreenAdReady], [loadFullScreenAdIfNeeded]) records nothing.
 *
 * Consent follows the PARTNER rules S-01 to S-19 ([ConsentCoordinator], [ConsentGate]): the ads SDK is initialized the
 * first time the consent gate opens, and every request asks the gate right before it is sent ([mayRequestNow]).
 *
 * Preloads follow S-20 to S-25 (plan T-021): no slot loads by itself; every preload, reload when Internet comes back
 * and native refill goes through the plan ([AdPreloadPlan], [AdPreloader]) once the SDK is ready. Failed loads retry
 * by F-05 and O-05 ([FullScreenReloads], [retryingNativeAdStates]).
 *
 * A native small slot (plan T-040) takes its ad from the single-ad source of its placement ([NativeAdSources],
 * O-01 to O-04) and replaces it in place every 60 s in foreground ([refreshingNativeAdStates], CL-02); every send of
 * it goes through one gate ([awaitSendAllowed], L-072). Native ads are not shown 55 minutes after loading (O-04).
 *
 * Native medium and in-collection cards come from pools (plan T-028, P-01 to P-08): one [NativeAdPool] per (format,
 * ad unit), managed by [NativeAdPools], filled one request at a time through the same send gate and by the preload
 * plan (HD-02, S-22, S-25). [NativeAdSlot] of a medium placement, a medium row of [rememberInListAds] and
 * [NativeAdCell] are cells of a pool ([pooledCell]): they claim an ad while on screen, give it back when they leave,
 * and record their opportunity (P-07). A load of a pool logs `ad_placement = pool` (PL-02).
 *
 * Banners follow B-01 to B-06 (plan T-029): [AdSlot] keeps every send in one loop ([runBannerLoads]) that waits for
 * the screen to show and asks [mayRequestNow] right before it sends; a collapsible banner reloads every 30 s.
 * Mediation adapters (AppLovin, plan T-015) are initialized by the SDK at the same time, with no code of their own;
 * their status is logged.
 *
 * AppLovin MAX (plan T-068, ADR-0037) is the second ad platform. [MaxSdkStarter] initializes it once, after the consent
 * gate opened and once [sdkReady] of AdMob is on (its callback or the 35 s cap of S-21), never before
 * ([startAdsSdks]), and only while the gate is open at that moment: a gate closed meanwhile starts nothing, the gate
 * open again starts it if it has not started (S-14). No consent flag is set: MAX reads the TCF and AC strings of UMP.
 * Without an SDK key in the build ([MaxConfig]) MAX never starts and [mediationOrder] (`ads_mediation_order`) is AdMob
 * only.
 *
 * The ad of the loading screen (plan T-024, KB-13) is [StartupAds]: [awaitStartupAd] waits within the limits of A-03
 * ([awaitStartupAd] of StartupAdWait.kt), and [showStartupAd] shows the app open ad of the app open slot (A-07 to
 * A-09) or the interstitial of `open_app_inter`. App-wide placements are switched by [placementSwitch].
 *
 * When `is_show_native_after_inter` is on as an interstitial turn ends (plan T-026, KB-16, K-01 to K-08), a native
 * full-screen ad of `native_combo` follows it with a countdown of `time_close_native`, 5 s by default
 * ([NativeFullScreenTurns]), drawn by
 * [FullScreenAdHost]; the completion of the interstitial then runs when that ad closes. The startup interstitial never
 * opens the combo. Every interstitial shown is counted since install ([InterDisplayCounter], N-05).
 *
 * A rewarded placement holds an action of the user (plan T-031, MC-05, R-01 to R-06): [rememberRewardedAction] asks
 * with its dialog and calls [showRewarded]; every rule of the turn and of its loads is in [RewardedTurns], which this
 * class ties to the SDK ([RewardedAd]). The rewarded slot never reads nor moves `ads_interval` (R-05), shows even
 * while another full-screen ad holds the showing flag (R-06), and is loaded with the resumed activity of the app.
 *
 * The resume ad (plan T-025, KB-14, A-10 to A-15) is decided each time the whole process comes to foreground
 * ([AdsActivityTracker], all activities of the process, a rotation is not a return), except the first time, by the
 * gates of [decideResumeAd], and shown on the next resume of an app activity ([ResumeAdTurns], [ActivityResumeSteps]):
 * app open of `resume` through the app open slot, or the interstitial of `resume_inter` with the combo, which skips
 * the `ads_interval` gate. It stays silent until the startup ad turn of the loading screen is over. A click on any ad
 * ([AdTelemetry.clicked]) skips the next return (A-14).
 *
 * AppsFlyer (plan T-033, ADR-0027) is started through [AppsFlyerStartup] at the first of: the SDK started, an answer of
 * the consent round, or 10 s after [start] with no consent form open ([awaitAttributionStart]). Its funnel goes through
 * [AdTelemetry] to [AdFunnel]: eligible when an interstitial or native full-screen turn opens and at each tap on
 * "Watch ad" of a rewarded ad, `api_called` at each load, `displayed` when the ad shows ([NativeFunnel] for native
 * ads), and the display count of interstitials (N-05). Native slots report eligible with [ReportNativeEligible].
 */
@Singleton
class AdsController @Inject internal constructor(
    @param:ApplicationContext private val context: Context,
    private val unitIds: AdUnitIds,
    consentDebug: ConsentDebugOptions,
    private val debugOptions: AdsDebugOptions,
    private val flags: AdsFlags,
    private val connectivity: InternetConnectivity,
    private val shortcutConfig: ConsentShortcutConfig,
    metrics: AdMetrics,
    internal val logger: AppLogger,
    private val firstOpenMark: FirstOpenMark,
    private val interDisplayCounter: InterDisplayCounter,
    private val attribution: AppsFlyerStartup,
    funnel: AdFunnel,
    private val maxConfig: MaxConfig,
    maxPlatform: MaxPlatform,
) : StartupAds {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** One log line per network of every load the SDK answers, at staging only (plan T-058, KB-28). */
    internal val networkLog = AdNetworkLog(debugOptions.logNetworkResponses, logger)

    // Touched on the main thread only.
    private val clock: MonotonicClock = SystemMonotonicClock
    private val guard = FullScreenAdGuard()
    private val loadedInterstitials = mutableMapOf<AdPlacement, InterstitialAd>()

    /** Foreground and the published showing flag, read by every in-screen ad (KB-27, plan T-054). */
    private val inScreenAdWindowState = InScreenAdWindow(logger)
    private val presence = FullScreenAdPresence(onPublished = inScreenAdWindowState::onCovered)
    private val stuck = StuckFullScreenRecovery()
    private val preloadedNativeFullScreen = nativeFullScreenCache<LoadedNativeAd>(
        onDiscard = {
            nativeFullScreenCells -= it
            it.destroy()
        },
    )

    /** The rewarded slot (plan T-031): an ad an hour old is dropped when read, like app open. */
    private val loadedRewardeds = FreshAdCache<RewardedAd>(onDiscard = { rewardedCells -= it })

    /** The rewarded ad taken out of the slot for the turn on screen, kept for a second show (R-06). */
    private val showingRewardeds = mutableMapOf<AdPlacement, RewardedAd>()

    /** The app open slot (A-07): a load replaces the ad kept before it, an ad an hour old is dropped when read. */
    private val loadedAppOpens = FreshAdCache<AppOpenAd>(APP_OPEN_MAX_AGE_MILLIS, onDiscard = { appOpenCells -= it })

    /** When the last app open ad closed, for the lock of A-08; null before the first one. */
    private var lastAppOpenClosedAt: Long? = null

    /**
     * The startup ad turn of the loading screen is over (question 7 of plan T-025): [awaitStartupAd] returned null,
     * the completion of [showStartupAd] ran, or 45 s passed since [start]. The resume ad stays silent until then.
     */
    private val startupFinished = MutableStateFlow(false)

    /** When [start] ran: the origin of the three limits of A-03 (question 4 of plan T-024); null before it. */
    private var startedAtMillis: Long? = null

    /**
     * Whether this process is the first open since install (A-01): the mark is read and written once, the first time
     * a switch of an app-wide placement is read, which is in [start] (question 6 of plan T-024).
     */
    private val firstOpenResult by lazy { scope.async { firstOpenMark.consume() } }
    private val firstOpen: Flow<Boolean> = flow { emit(firstOpenResult.await()) }

    /** The placement of the startup ad the keys choose now, collected from [start]; [startupChoiceKnown] once read. */
    private val startupChoice = MutableStateFlow<AdPlacement?>(null)
    private val startupChoiceKnown = MutableStateFlow(false)

    /** Loading and waiting for a retry, per full-screen placement; ready and idle are read from the loaded ads. */
    private val loadStates = mutableMapOf<AdPlacement, FullScreenLoadState>()

    /** The `load_state` label of the last load error per full-screen placement, cleared by a load (section 8.2). */
    private val lastLoadErrors = mutableMapOf<AdPlacement, String>()

    /** The placement a loaded full-screen ad is shown for, set at each show and read by its paid event (PL-03). */
    private val interstitialCells = mutableMapOf<InterstitialAd, PlacementCell>()
    private val nativeFullScreenCells = mutableMapOf<LoadedNativeAd, PlacementCell>()
    private val appOpenCells = mutableMapOf<AppOpenAd, PlacementCell>()
    private val rewardedCells = mutableMapOf<RewardedAd, PlacementCell>()

    /** The completion of the full-screen turn on screen (interstitial or app open). */
    private var currentTurn: FullScreenCompletion? = null

    private val gate = ConsentGate()
    private var tracker: AdsActivityTracker? = null
    private val sdkStarted = MutableStateFlow(false)

    /** True while an app activity is started, as [AdsActivityTracker] reports it; native requests wait for it. */
    private val foregroundState = MutableStateFlow(false)

    /** When `MobileAds.initialize` was called (S-21), and whether its callback came. */
    private var sdkInitStartedAt: Long? = null
    private var sdkCallbackArrived = false

    /** Failures in a row per load slot: the metrics and the retry waits read the same counter (plan T-021). */
    private val loadCounter = LoadAttemptCounter()

    /** The switch of each full-screen placement, as last read in [start]; off until then (ADR-0014). */
    private val enabledNow = mutableMapOf<AdPlacement, Boolean>()

    /** Latest values read by the metrics, collected from [start]; before it nothing is served (S-19). */
    private val latestGlobalFlags = MutableStateFlow<AdsGlobalFlags?>(null)

    /**
     * The latest global ad switches ([AdsFlags.global]), the defaults until the first value: the bottom bar reads
     * `is_show_native_bottom`, `is_show_native_collap` and `time_expand_collap_ad` from it (plan T-030).
     */
    internal val globalFlags: StateFlow<AdsGlobalFlags> = latestGlobalFlags
        .map { it ?: DEFAULT_GLOBAL_FLAGS }
        .stateIn(scope, SharingStarted.Eagerly, latestGlobalFlags.value ?: DEFAULT_GLOBAL_FLAGS)
    private val online = MutableStateFlow(true)

    /** Starts AppLovin MAX after the consent gate and AdMob (plan T-068). */
    private val maxStarter = MaxSdkStarter(maxConfig, maxPlatform, logger) { gate.state.value.allowed }

    /**
     * The order of the two ad platforms that applies (`ads_mediation_order`, ADR-0037): [MediationOrder.ADMOB] in a
     * build without an AppLovin MAX SDK key. Read by the loads of MAX (T-069 to T-071).
     */
    internal val mediationOrder: StateFlow<MediationOrder> = globalFlags
        .map { MediationOrder.effective(it.mediationOrder, maxConfig.hasKey) }
        .stateIn(scope, SharingStarted.Eagerly, MediationOrder.effective(MediationOrder.DEFAULT, maxConfig.hasKey))

    /** True once AppLovin MAX answered its initialization (T-069). */
    internal val maxReady: StateFlow<Boolean> = maxStarter.ready

    /** Blocks the ads that show by themselves while a consent form is open (S-16, G-01); read by T-022, T-025. */
    internal val foregroundAdSuppressor = ForegroundAdSuppressor()

    /** The block of the ads that show by themselves, with the leave mark and the shell's report (G-02 to G-05). */
    internal val resumeSuppression = ResumeSuppression(foregroundAdSuppressor, logger)

    /** True while resume ads must not show (KB-23); read by the resume ad (T-025). */
    internal val resumeAdsSuppressed: StateFlow<Boolean> = resumeSuppression.resumeAdsSuppressed

    private val coordinator = ConsentCoordinator(
        platform = UmpConsentPlatform(context, consentDebug),
        gate = gate,
        suppressor = foregroundAdSuppressor,
        isActivityGone = { it.isFinishing || it.isDestroyed || it.isChangingConfigurations },
        isResumed = { it === tracker?.resumedActivity },
        // The inner state: inside the hand-over of an interstitial (publishAfter) the published flag is still true.
        fullScreenShowing = { presence.showingNow },
        allowReset = consentDebug.allowConsentReset,
        logger = logger,
        onGateOpened = ::onGateOpened,
    )

    /** G-06: stuck recovery before the regather of consent (S-09), then the resume ad (T-025), then the leave mark. */
    private val resumeSteps = ActivityResumeSteps(
        recoverStuck = { recoverStuckFullScreen(it) },
        regatherConsent = { coordinator.onActivityResumed(it) },
        showResumeAd = { resumeAds.onAppActivityResumed(it) },
        releaseLeaveMark = { resumeSuppression.onActivityResumed() },
    )

    /**
     * True while a full-screen ad (interstitial or native full screen) is on screen. The network popup (ADR-0024,
     * T-017) waits for it to close. The in-screen ads read it through [inScreenAdWindow] (KB-27); an interstitial that
     * hands over to its native ad keeps it true with no gap ([FullScreenAdPresence.publishAfter], plan T-054).
     */
    val fullScreenAdShowing: StateFlow<Boolean> = presence.showing

    /**
     * True while the consent gate is open and the SDK is started. Turns false at once when the gate closes (S-10,
     * S-14), so slots drop the ads they show (S-11, S-15). False before [start] (S-19).
     */
    val canRequestAds: StateFlow<Boolean> =
        combine(gate.state, sdkStarted) { state, started -> state.allowed && started }
            .stateIn(scope, SharingStarted.Eagerly, false)

    /** The recorder of ad metrics (plan T-023); the banner of [AdSlot] and the native loads report to it too. */
    internal val telemetry = AdTelemetry(
        metrics = metrics,
        funnel = funnel,
        lifecycleSwitches = { latestGlobalFlags.value ?: DEFAULT_GLOBAL_FLAGS },
        serving = { canRequestAds.value },
        offline = { !online.value },
        counter = loadCounter,
        logger = logger,
        // A-14: a click on any ad skips the next return to the app (plan T-025).
        onClicked = { resumeSuppression.markAdClicked() },
    )

    /** How ads were allowed in this process (S-13); null before the first result (S-19). */
    val consentPath: StateFlow<ConsentPath?> =
        gate.state.map { it.path }.stateIn(scope, SharingStarted.Eagerly, null)

    /**
     * True once the consent round of this start has its final result, or the result was settled before. The loading
     * screen waits for it, at most 10 seconds (T-024).
     */
    val consentFlowFinished: StateFlow<Boolean> = coordinator.finished

    private val _sdkReady = MutableStateFlow(false)

    /**
     * True once the ads SDK has reported its initialization (`MobileAds.initialize` callback), or 35 s after it was
     * started (S-21). Only the preload plan waits for it; [canRequestAds] does not (question 10 of plan T-021).
     */
    val sdkReady: StateFlow<Boolean> = _sdkReady.asStateFlow()

    private val fullScreenReloads = FullScreenReloads(
        scope = scope,
        clock = clock,
        failuresInARow = { loadCounter.failuresInARow(preloadCounterKey(it.key)) },
        loadIfNeeded = { loadFullScreenAdIfNeeded(it) },
        // Foreground, not the in-screen window: the screen over the app waits for this very load (K-04, plan T-054).
        foreground = foregroundState,
        logger = logger,
    )

    /** "A load of the native full-screen slot ended", per placement (L-068): the combo waits for its own. */
    private val nativeLoadResults = NativeLoadResults()

    /** The native full-screen turns: the combo after an interstitial and the standalone turn (plan T-026). */
    private val nativeTurns = NativeFullScreenTurns(
        port = object : NativeFullScreenPort {
            override suspend fun comboEnabled(): Boolean = flags.global.first().showNativeAfterInter

            override fun beginPresence(): Boolean =
                presence.tryBegin(AdPlacement.NATIVE_AFTER_INTER, NO_HOST_ACTIVITY, clock.nowMillis())

            override fun endPresence() = presence.onClosed()

            override fun holdResumeAds(): ForegroundAdSuppressor.Hold =
                resumeSuppression.hold(ResumeSuppression.FULL_SCREEN_PAGE)

            override fun releaseResumeAds(hold: ForegroundAdSuppressor.Hold) = resumeSuppression.release(hold)

            // SDK activities are filtered out by AdsActivityTracker, so this is an activity of the app.
            override fun interGone(): Boolean = tracker?.resumedActivity != null

            override fun consentOpen(): Boolean = canRequestAds.value && gate.state.value.allowed

            // Called once, when the native full-screen turn after an interstitial opens: its eligible event (T-033).
            override fun loadStateAtOpen(): String {
                telemetry.eligible(AdFormat.NATIVE_FULL_SCREEN)
                return opportunityLoadState(AdPlacement.NATIVE_AFTER_INTER)
            }

            override fun takeAd(): LoadedNativeAd? =
                takePreloadedNativeFullScreen(AdPlacement.NATIVE_AFTER_INTER, clock.nowMillis())

            override fun ensureLoading(): Boolean {
                loadFullScreenAdIfNeeded(AdPlacement.NATIVE_AFTER_INTER)
                return loadStates[AdPlacement.NATIVE_AFTER_INTER] == FullScreenLoadState.Loading
            }

            override fun loadGeneration(placement: AdPlacement): Int = nativeLoadResults.generation(placement)

            override suspend fun awaitLoadResult(placement: AdPlacement, since: Int) =
                nativeLoadResults.await(placement, since)

            override fun recordIntervalMark(source: AdPlacement) = this@AdsController.recordIntervalMark(source)

            override fun recordOpportunity(result: String, loadState: String) = telemetry.opportunity(
                AdFormat.NATIVE_FULL_SCREEN,
                AdPlacement.NATIVE_AFTER_INTER,
                result,
                loadState,
            )

            override fun reload(placement: AdPlacement) = fullScreenReloads.onClosedOrShowFailed(placement)

            override fun closeCountdownSeconds(): Long = globalFlags.value.closeNativeCountdownSeconds
        },
        clock = clock,
        scope = scope,
        logger = logger,
    )

    /** The resume ad (plan T-025): decided when the process comes to foreground, shown on the next resume. */
    private val resumeAds = ResumeAdTurns(
        port = object : ResumeAdPort {
            override fun readGate(): ResumeGate = readResumeGate()

            override fun loadStateOf(placement: AdPlacement): String = opportunityLoadState(placement)

            override fun recordOpportunity(placement: AdPlacement, result: String, loadState: String) =
                telemetry.opportunity(placement.format, placement, result, loadState)

            override fun loadIfNeeded(placement: AdPlacement) = loadFullScreenAdIfNeeded(placement)

            override fun placementNow(): AdPlacement? = latestGlobalFlags.value?.let(::resumeAdPlacement)

            override fun suppressedNow(): Boolean = resumeAdsSuppressed.value

            override fun showAppOpen(activity: Activity, placement: AdPlacement) {
                scope.launch { this@AdsController.showAppOpen(activity, placement, onFinished = {}) }
            }

            override fun showInterstitial(activity: Activity, placement: AdPlacement, combo: Boolean) {
                scope.launch { showInterstitialTurn(activity, placement, combo, onFinished = {}) }
            }
        },
        logger = logger,
    )

    /** The native full-screen turn on screen, drawn by [FullScreenAdHost]; null when none (HD-05). */
    internal val nativeFullScreenView: StateFlow<NativeFullScreenView?> = nativeTurns.view

    /** The placement of each rewarded declaration. */
    private val rewardedPlacements: Map<AdPlacementDeclaration, AdPlacement> =
        AdPlacement.entries.filter { it.format == AdFormat.REWARDED }.associateBy { it.declaration }

    private fun AdPlacementDeclaration.placement(): AdPlacement = rewardedPlacements.getValue(this)

    /** The rewarded turns and load rounds (plan T-031); this port ties them to the SDK. */
    private val rewardedTurns = RewardedTurns(
        port = object : RewardedPort {
            override suspend fun enabled(declaration: AdPlacementDeclaration): Boolean =
                isEnabled(declaration.placement()).first()

            override fun consentOpen(): Boolean = canRequestAds.value && gate.state.value.allowed

            override fun mayRequestNow(): Boolean = this@AdsController.mayRequestNow()

            override fun offline(): Boolean = !online.value

            override fun ready(declaration: AdPlacementDeclaration): Boolean =
                isFullScreenAdReady(declaration.placement())

            override fun needsLoad(declaration: AdPlacementDeclaration): Boolean =
                // The gate is asked by the round itself (rewardedLoadStep).
                needsLoad(fullScreenLoadState(declaration.placement()), clock.nowMillis(), gateOpen = true)

            override fun loadState(declaration: AdPlacementDeclaration): String =
                opportunityLoadState(declaration.placement())

            override suspend fun loadIfNeeded(declaration: AdPlacementDeclaration) =
                loadFullScreenAdIfNeeded(declaration.placement())

            override fun clearLoading(declaration: AdPlacementDeclaration) {
                loadStates -= declaration.placement()
            }

            override fun send(declaration: AdPlacementDeclaration) = sendRewardedLoad(declaration.placement())

            override fun sendBlocked(declaration: AdPlacementDeclaration) {
                val placement = declaration.placement()
                loadStates[placement] = fullScreenReloads.onSendBlocked(placement)
            }

            override fun take(declaration: AdPlacementDeclaration): Boolean {
                val placement = declaration.placement()
                val ad = loadedRewardeds.take(placement, clock.nowMillis()) ?: return false
                rewardedCells.remove(ad)?.value = placement
                showingRewardeds[placement] = ad
                return true
            }

            override fun show(declaration: AdPlacementDeclaration, activity: Activity?) =
                showRewardedAd(declaration, activity)

            override fun showAgain(declaration: AdPlacementDeclaration, activity: Activity?) =
                showRewardedAd(declaration, activity)

            override fun raisePresence(declaration: AdPlacementDeclaration, activity: Activity?): Boolean =
                presence.tryBegin(
                    declaration.placement(),
                    activity?.let { System.identityHashCode(it) } ?: NO_HOST_ACTIVITY,
                    clock.nowMillis(),
                )

            override fun lowerPresence() = presence.onClosed()

            override fun recordOpportunity(declaration: AdPlacementDeclaration, result: String, loadState: String) =
                telemetry.opportunity(AdFormat.REWARDED, declaration.placement(), result, loadState)

            override fun recordReward(declaration: AdPlacementDeclaration) =
                telemetry.rewardEarned(declaration.placement())

            override fun hostUsable(activity: Activity?): Boolean = isHostUsable(activity)

            override fun recordShowFailed(declaration: AdPlacementDeclaration, code: Int) =
                telemetry.showFailed(AdFormat.REWARDED, declaration.placement(), code)
        },
        logger = logger,
    )

    /** One slot of the preload plan per full-screen placement. */
    private val preloadSlots: List<PreloadSlot> = fullScreenPlacements().map { placement ->
        FullScreenPreloadSlot(
            placement = placement,
            enabled = { enabledNow[placement] == true },
            state = { fullScreenLoadState(placement) },
            load = { loadFullScreenAdIfNeeded(it) },
        )
    }

    private val preloader = AdPreloader(
        scope = scope,
        runner = PreloadPlanRunner(clock, logger),
        // Full-screen placements, then the slot of each native pool (HD-02, S-22); a pool slot is asked lazily.
        slotsOf = { format -> preloadSlots.filter { it.format == format } + nativePools.preloadSlots(format) },
        gate = {
            PreloadGate(
                sdkStarted = sdkStarted.value,
                sdkReady = _sdkReady.value,
                mayRequest = mayRequestNow(),
                consentDenied = gate.state.value.path == ConsentPath.DENIED_OR_REQUIRED,
            )
        },
        askConsentAgain = { coordinator.onPreloadRequested() },
        logger = logger,
    )

    /**
     * True when the user must be able to reopen the consent form ([PrivacyOptionsButton]).
     * This is also how the module knows the user is in a region where the law requires consent.
     */
    val privacyOptionsRequired: StateFlow<Boolean> = coordinator.privacyOptionsRequired

    /**
     * Starts :core:ads, once per process, from `Application.onCreate` before any screen (S-01, HD-01): follows the
     * activities that [appActivities] accepts, the consent shortcut (S-12, HD-04) and Internet (S-09). A second call
     * does nothing.
     */
    fun start(application: Application, appActivities: AppActivityFilter) {
        if (!coordinator.start()) {
            logger.info(TAG, "Ads already started")
            return
        }
        logger.info(TAG, "Ads started")
        missingUnitsWarning(unitIds)?.let { logger.warn(TAG, it) }
        missingMaxUnits()?.let { logger.info(TAG, it) }
        startedAtMillis = clock.nowMillis()
        // AppsFlyer starts once consent allows it (ADR-0027, decision 3; plan T-033).
        scope.launch {
            val consentAnswered = combine(coordinator.finished, gate.state) { done, state -> done && state.settled }
            awaitAttributionStart(sdkStarted, consentAnswered, coordinator.consentFormOpen, ATTRIBUTION_WAIT_MILLIS)
            logger.info(TAG, "AppsFlyer may start")
            attribution.start()
        }
        val activityTracker = AdsActivityTracker(
            appActivities,
            object : AdsActivityTracker.Listener {
                override fun onForegroundChanged(foreground: Boolean) {
                    foregroundState.value = foreground
                    inScreenAdWindowState.onForeground(foreground)
                    coordinator.onForegroundChanged(foreground)
                }

                override fun onProcessForegroundChanged(foreground: Boolean) =
                    this@AdsController.onProcessForegroundChanged(foreground)

                override fun onActivityResumed(activity: Activity) = onAppActivityResumed(activity)

                override fun onActivityPaused(activity: Activity) = onAppActivityPaused(activity)

                override fun onActivityDestroyed(activity: Activity) = onAppActivityDestroyed(activity)
            },
        )
        tracker = activityTracker
        application.registerActivityLifecycleCallbacks(activityTracker)
        // An app with no loading screen does not keep the resume ad silent for ever (question 7 of plan T-025).
        scope.launch {
            delay(STARTUP_FINISHED_CAP_MILLIS)
            if (!startupFinished.value) logger.info(TAG, "Startup ad turn over: 45 s cap")
            startupFinished.value = true
        }
        scope.launch {
            flags.global
                .map { shortcutActive(shortcutConfig.enabledInBuild, it.consentShortcutEnabled) }
                .distinctUntilChanged()
                .collect { coordinator.onShortcutChanged(it) }
        }
        scope.launch {
            connectivity.connected.collect {
                online.value = it
                coordinator.onInternetChanged(it)
            }
        }
        scope.launch { flags.global.collect { latestGlobalFlags.value = it } }
        scope.launch { mediationOrder.collect { maxStarter.onOrderChanged(it) } }
        scope.launch {
            combine(flags.global, firstOpen) { global, first -> startupAdPlacement(global, first) }.collect {
                startupChoice.value = it
                startupChoiceKnown.value = true
            }
        }
        collectPreloadTriggers()
    }

    /**
     * What asks the plan for a preload besides the SDK getting ready: a full-screen placement switched on remotely
     * (S-20), a change of the gate for a held preload (S-23), Internet back (S-24).
     */
    private fun collectPreloadTriggers() {
        // Full-screen placements and pooled ones: switching either on remotely asks for a preload (S-20).
        val placements = fullScreenPlacements() + pooledPlacements()
        scope.launch {
            combine(placements.map { placement -> isEnabled(placement).map { placement to it } }) { it.toMap() }
                .collect { switches ->
                    val switchedOn = switches.any { (placement, on) -> on && enabledNow[placement] != true }
                    enabledNow.putAll(switches)
                    if (switchedOn) preloader.request(PreloadReason.PLACEMENT_ENABLED)
                }
        }
        scope.launch {
            combine(canRequestAds, sdkReady, consentPath, foregroundState) { _, _, _, _ -> }
                .collect { preloader.onGateChanged() }
        }
        // The value read in start, so Internet is watched once (S-24, T-016).
        scope.launch { networkReturns(online).collect { preloader.onNetworkReturned() } }
    }

    /**
     * The startup ad of the loading screen (KB-13): waits within the limits of A-03, counted from [start], probing
     * without recording (A-04), and returns the turn to show, or null; null at once when the key of this open is off.
     * Null before [start] (tests, previews).
     */
    override suspend fun awaitStartupAd(): StartupAdTurn? = withContext(Dispatchers.Main.immediate) {
        val startedAt = startedAtMillis
        if (startedAt == null) {
            logger.info(TAG, "startup ad: ads not started")
            startupFinished.value = true
            return@withContext null
        }
        startupChoiceKnown.first { it }
        val probe = object : StartupAdProbe {
            override fun nowMillis(): Long = clock.nowMillis()

            override val consentFinished: Boolean get() = consentFlowFinished.value

            override val consentOpen: Boolean get() = gate.state.value.allowed

            override val sdkReady: Boolean get() = _sdkReady.value

            override fun chosen(): AdPlacement? = startupChoice.value

            override fun isReady(placement: AdPlacement): Boolean = isFullScreenAdReady(placement)

            override fun ensureLoading(placement: AdPlacement) = loadFullScreenAdIfNeeded(placement)

            override fun recordNotReady(placement: AdPlacement) = telemetry.opportunity(
                placement.format,
                placement,
                FullScreenShowDecision.NOT_READY.opportunityResult(),
                opportunityLoadState(placement),
            )
        }
        val turn = awaitStartupAd(probe, startedAt, logger = logger)?.let { StartupAdTurn(it) }
        // No ad to show: the startup turn is over, the resume ad may show (question 7 of plan T-025).
        if (turn == null) startupFinished.value = true
        turn
    }

    /**
     * Shows [turn] ([rememberShowStartupAd]) in the scope of this controller, so a recreated activity does not cancel
     * it: `open_app` through the app open slot, `open_app_inter` as an interstitial that skips the `ads_interval` gate
     * and moves its mark when it closes (A-02). [onFinished] runs once on the main thread, with true only when the
     * interstitial was shown. An [activity] that is no longer usable is checked by those two paths, after their gate
     * (F-08, plan T-055): a turn the gate lets show records `shown` and `ad_show_fail` `app_not_foreground`.
     */
    internal fun showStartupAd(activity: Activity, turn: StartupAdTurn, onFinished: (interShown: Boolean) -> Unit) {
        scope.launch {
            var interShown = false
            val completion = FullScreenCompletion(logger).apply {
                set {
                    startupFinished.value = true
                    onFinished(interShown)
                }
            }
            val finish = { completion.complete("startup_ad") }
            when (turn.placement.format) {
                AdFormat.APP_OPEN -> showAppOpen(activity, turn.placement, finish)
                AdFormat.INTERSTITIAL -> {
                    // The startup interstitial never opens the combo (question 5 of plan T-026).
                    val shown = showInterstitialTurn(activity, turn.placement, combo = false, finish)
                    interShown = shown
                }
                else -> {
                    logger.warn(TAG, "Startup ad ${turn.placement.key}: format not shown")
                    completion.complete("format")
                }
            }
        }
    }

    /** Sets the preload plan (HD-02); call it in `Application.onCreate`, before consent has a result. */
    fun setPreloadPlan(plan: AdPreloadPlan) {
        preloader.setPlan(plan)
    }

    /** Milliseconds of the monotonic clock, for the load latency of the ad metrics. */
    internal fun metricsNowMillis(): Long = clock.nowMillis()

    /**
     * Moves when usable Internet comes back (S-24): a banner waiting for its retry sends at once, like a native slot.
     */
    internal val retryResets: StateFlow<Int> get() = preloader.nativeRetryResets

    /**
     * Updates the consent status and shows the consent form when required (S-02). Call it in the activity's
     * `onCreate` at every start.
     */
    fun gatherConsent(activity: Activity) {
        coordinator.gather(activity)
    }

    /** Opens the privacy options form, so the user can change their consent at any time (S-14). */
    fun showPrivacyOptionsForm(activity: Activity) {
        coordinator.showPrivacyOptions(activity)
    }

    /** Clears the stored consent, staging only (S-18, [rememberResetConsent]). */
    internal fun resetConsent() {
        coordinator.reset()
    }

    /**
     * Whether a request may be sent right now (S-08, S-15): the gate is open, the SDK started, and a right that only
     * ERROR_ALLOWED gave is used in foreground only. Asked right before every request is sent.
     */
    internal fun mayRequestNow(): Boolean = sdkStarted.value && coordinator.mayRequestNow(tracker?.foreground ?: false)

    /**
     * F-03, first step of every resume (G-06): the activity that showed the full-screen ad was paused and is resumed
     * while the ad still reads as showing. Lowers the flag, writes the mark of the ad's group now and runs the
     * waiting completion once; the ad's screen itself is left alone.
     */
    private fun recoverStuckFullScreen(activity: Activity) {
        val shown = presence.current
        if (!stuck.onActivityResumed(System.identityHashCode(activity), shown?.hostActivityId)) return
        val placement = shown?.placement ?: return
        recordIntervalMark(placement)
        logger.warn(TAG, "Full-screen ${placement.key} stuck: flag released")
        endCurrentTurn("stuck_recovered")
        // A rewarded turn is not the current turn: it ends here, with no outcome (F-03).
        if (placement.format == AdFormat.REWARDED) rewardedTurns.onStuck()
    }

    /**
     * The gate of the resume ad read now (plan T-025): what [ResumeAdTurns] reads when the process comes to
     * foreground. Before the keys are read, resume counts as off.
     */
    internal fun readResumeGate(): ResumeGate {
        val now = clock.nowMillis()
        val placement = latestGlobalFlags.value?.let(::resumeAdPlacement)
        return ResumeGate(
            firstForeground = false,
            startupFinished = startupFinished.value,
            serving = canRequestAds.value,
            suppressed = resumeAdsSuppressed.value,
            placement = placement,
            busy = presence.isBusy(now),
            appOpenLockPassed = appOpenLockPassed(lastAppOpenClosedAt, now),
            ready = placement != null && isFullScreenAdReady(placement),
        )
    }

    /** The whole process came to foreground or went to background ([AdsActivityTracker]): the resume ad turn. */
    internal fun onProcessForegroundChanged(foreground: Boolean) {
        logger.info(TAG, if (foreground) "App in foreground" else "App in background")
        resumeAds.onProcessForegroundChanged(foreground)
    }

    /**
     * An activity of the app was resumed ([AdsActivityTracker]): a turn whose host was recreated for a configuration
     * change becomes this activity's (plan T-055), then the steps of G-06, then S-25. The pause mark of the old host is
     * not moved, so the first resume of the new activity never recovers the turn (F-03).
     */
    internal fun onAppActivityResumed(activity: Activity) {
        if (presence.rehostIfAwaiting(System.identityHashCode(activity))) {
            logger.info(TAG, "Full-screen ${presence.current?.placement?.key}: new host resumed")
        }
        resumeSteps.onResumed(activity)
        // S-25, after the steps of G-06.
        preloader.onActivityResumed(presence.showing.value)
    }

    /** An activity of the app was paused: the host of a full-screen ad may be stuck (F-03). */
    internal fun onAppActivityPaused(activity: Activity) {
        stuck.onActivityPaused(System.identityHashCode(activity), presence.current?.hostActivityId)
    }

    /**
     * N-04, HD-03: the activity that showed the full-screen ad is destroyed before the ad reported its close. Lowers
     * the flag and runs the waiting completion once (forced completion). A destruction for a configuration change
     * (rotation, `isChangingConfigurations`) is not the loss of the host (KB-26, criterion 4; plan T-055): the turn is
     * kept, the next resumed activity of the app takes it ([onAppActivityResumed]), and the real callback ends it.
     */
    internal fun onAppActivityDestroyed(activity: Activity) {
        val shown = presence.current ?: return
        val action = hostDestroyedAction(
            System.identityHashCode(activity),
            shown.hostActivityId,
            activity.isChangingConfigurations,
        )
        when (action) {
            HostDestroyedAction.IGNORE -> Unit
            HostDestroyedAction.HAND_OVER -> {
                logger.info(TAG, "Full-screen ${shown.placement.key}: host recreated, turn kept")
                presence.handOver()
            }
            HostDestroyedAction.FORCE_COMPLETE -> {
                logger.warn(TAG, "Full-screen ${shown.placement.key}: host destroyed before close")
                endCurrentTurn("host_destroyed")
                if (shown.placement.format == AdFormat.REWARDED) rewardedTurns.onStuck()
            }
        }
    }

    /** The consent gate opened: the first time starts the SDK, later times preload again (S-10, S-14). */
    private fun onGateOpened(firstTime: Boolean) {
        if (firstTime) {
            startSdk()
        } else if (sdkStarted.value) {
            logger.info(TAG, "Consent gate opened again: preload")
            preloader.request(PreloadReason.GATE_REOPENED)
            // S-14: MAX starts now if the gate was closed when it was due (plan T-068, question 9).
            maxStarter.onOrderChanged(mediationOrder.value)
        }
    }

    private fun startSdk() {
        sdkInitStartedAt = clock.nowMillis()
        // S-21: the SDK counts as ready 35 s after it was started, even without its callback.
        scope.launch {
            delay(SDK_READY_CAP_MILLIS)
            updateSdkReady("35 s cap")
        }
        scope.launch {
            // The developer's test devices of a staging build, before the SDK starts and sends anything (T-058).
            applyTestDevices(
                debugOptions,
                MobileAds::getRequestConfiguration,
                MobileAds::setRequestConfiguration,
                logger,
            )
            startAdsSdks(
                initAdmob = {
                    // Google recommends starting the SDK off the main thread.
                    withContext(Dispatchers.IO) {
                        // The listener runs once every adapter is initialized or timed out (KB-9).
                        MobileAds.initialize(context) { status ->
                            adapterStatusLines(status.adapterStatusMap).forEach { logger.info(TAG, it) }
                            scope.launch {
                                sdkCallbackArrived = true
                                updateSdkReady("callback")
                            }
                        }
                    }
                    sdkStarted.value = true
                },
                // The SDK both started and ready (S-20, S-21).
                awaitAdmobReady = { _sdkReady.first { it } },
                // AppLovin MAX only once AdMob's adapters are initialized (plan T-068, startAdsSdks).
                startMax = { maxStarter.onAdmobReady(mediationOrder.value) },
                // The first preload of the plan.
                preload = { preloader.request(PreloadReason.SDK_READY) },
            )
        }
    }

    /** Main thread. */
    private fun updateSdkReady(source: String) {
        if (_sdkReady.value || !sdkReadyAt(sdkInitStartedAt, sdkCallbackArrived, clock.nowMillis())) return
        logger.info(TAG, "SDK ready: $source")
        _sdkReady.value = true
    }

    private fun fullScreenPlacements() = AdPlacement.entries.filter { it.format.hasFullScreenSlot }

    private fun pooledPlacements() = AdPlacement.entries.filter { it.format.usesPool }

    /**
     * Opens AdMob's Ad Inspector ([rememberOpenAdInspector], staging ad gallery only). Google opens it only on an
     * emulator or a registered test device; elsewhere [onResult] gets [AdInspectorResult.Failed].
     */
    internal fun openAdInspector(activity: Activity, onResult: (AdInspectorResult) -> Unit) {
        if (!canRequestAds.value) {
            logger.info(TAG, "Ad Inspector skipped: ads not started")
            onResult(AdInspectorResult.NotReady)
            return
        }
        logger.info(TAG, "Ad Inspector opened")
        MobileAds.openAdInspector(activity) { error ->
            val result = adInspectorResult(error?.code)
            when (result) {
                is AdInspectorResult.Failed -> logger.warn(TAG, "Ad Inspector failed: ${result.code}")
                else -> logger.info(TAG, "Ad Inspector closed")
            }
            onResult(result)
        }
    }

    /**
     * Shows the interstitial of [placement] if the full-screen gate allows it, and returns whether it was shown.
     * [onFinished] runs exactly once, on the main thread: at once when the turn is blocked or no ad is loaded (N-02,
     * F-07) or [activity] is not usable (F-08: finishing, destroyed or not the resumed activity of the app; the ad
     * stays in its slot, `shown` then `ad_show_fail` `app_not_foreground`, no native ad after it); else when the ad
     * closes, fails to show, is found stuck or loses its activity (N-04; a recreation for a configuration change keeps
     * the turn, plan T-055), or, when `is_show_native_after_inter` is on at that moment, when the native full-screen ad
     * that follows it closes (KB-16, K-01). Call it at a natural break (end of a level, after saving), never when the
     * app opens or closes.
     */
    suspend fun showInterstitial(activity: Activity, placement: AdPlacement, onFinished: () -> Unit = {}): Boolean =
        showInterstitialTurn(activity, placement, combo = true, onFinished)

    /** [showInterstitial], with the combo after the interstitial only when [combo] (never for the startup ad). */
    private suspend fun showInterstitialTurn(
        activity: Activity,
        placement: AdPlacement,
        combo: Boolean,
        onFinished: () -> Unit,
    ): Boolean = withContext(Dispatchers.Main.immediate) {
        require(placement.format == AdFormat.INTERSTITIAL) { "${placement.key} is not a full-screen placement" }
        val decision = decideFullScreenTurn(placement)
        val loadState = opportunityLoadState(placement)
        if (decision != FullScreenShowDecision.SHOW) {
            return@withContext skipTurn(placement, decision, loadState, onFinished)
        }
        // F-08: before the turn opens, so no `af_inters_ad_eligible` and no combo.
        if (!isHostUsable(activity)) return@withContext skipHostGone(placement, onFinished)
        val turn = beginInterstitialTurn(placement, System.identityHashCode(activity), combo, onFinished)
            ?: return@withContext skipTurn(placement, FullScreenShowDecision.BUSY, loadState, onFinished)
        val ad = loadedInterstitials.remove(placement)
        if (ad == null) {
            onInterstitialLost(placement, turn, loadState)
            return@withContext false
        }
        interstitialCells.remove(ad)?.value = placement
        recordShownOpportunity(placement)
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                telemetry.displayed(
                    AdFormat.INTERSTITIAL,
                    ad.adUnitId,
                    ad.responseInfo.loadedAdapterResponseInfo?.adapterClassName,
                )
                // N-05: counted for T-033; a failed count never touches the turn and sends nothing.
                scope.launch {
                    val count = interDisplayCounter.increment()
                    telemetry.interDisplayed(count)
                    if (count != null) logger.info(TAG, "Interstitial displays since install: $count")
                }
            }

            override fun onAdDismissedFullScreenContent() = onInterstitialClosed(placement, turn)

            override fun onAdFailedToShowFullScreenContent(error: AdError) =
                onInterstitialShowFailed(placement, turn, error.code)

            override fun onAdClicked() = telemetry.clicked(AdFormat.INTERSTITIAL, placement)
        }
        logger.info(TAG, "Interstitial shown: ${placement.key}")
        ad.show(activity)
        true
    }

    /**
     * Starts an interstitial turn of [placement] that passed the gate, on [hostActivityId]: raises the showing flag
     * (null when another full-screen ad shows) and returns its completion. With [combo], every end of the turn goes
     * through [NativeFullScreenTurns.afterInterstitial] (closed, show failed, ad lost, stuck, host destroyed, cap
     * expired: KB-16, criterion 7), which runs [onFinished] once the native ad closes or at once when it is off.
     */
    internal fun beginInterstitialTurn(
        placement: AdPlacement,
        hostActivityId: Int,
        combo: Boolean,
        onFinished: () -> Unit,
    ): FullScreenCompletion? {
        if (!presence.tryBegin(placement, hostActivityId, clock.nowMillis())) return null
        // The turn opens: `af_inters_ad_eligible` (T-033), for every interstitial turn, the startup one included.
        telemetry.eligible(AdFormat.INTERSTITIAL)
        val finish: () -> Unit = if (combo) {
            { nativeTurns.afterInterstitial(placement, onFinished) }
        } else {
            onFinished
        }
        return beginTurn(finish)
    }

    /** The interstitial of [turn] closed (SDK). */
    internal fun onInterstitialClosed(placement: AdPlacement, turn: FullScreenCompletion) {
        // First line (I-05): the mark is written before anything else can fail.
        recordIntervalMark(placement)
        endTurn(turn, "closed")
        fullScreenReloads.onClosedOrShowFailed(placement)
    }

    /** The interstitial of [turn] failed to show (SDK). */
    internal fun onInterstitialShowFailed(placement: AdPlacement, turn: FullScreenCompletion, code: Int) {
        logger.warn(TAG, "Interstitial ${placement.key} failed to show: $code")
        telemetry.showFailed(AdFormat.INTERSTITIAL, placement, code)
        endTurn(turn, "show_failed")
        fullScreenReloads.onClosedOrShowFailed(placement)
    }

    /**
     * The ad of [turn] was gone between the decision and the show: `not_ready` with the [loadState] of the decision,
     * a load (F-07), then the end of the turn.
     */
    internal fun onInterstitialLost(placement: AdPlacement, turn: FullScreenCompletion, loadState: String) {
        logger.info(TAG, "Full-screen ${placement.key} skipped: ${FullScreenShowDecision.NOT_READY} (ad lost)")
        telemetry.opportunity(
            placement.format,
            placement,
            FullScreenShowDecision.NOT_READY.opportunityResult(),
            loadState,
        )
        loadFullScreenAdIfNeeded(placement)
        endTurn(turn, "ad_lost")
    }

    /**
     * Shows the app open ad of [placement] (A-05): the full-screen gate of [decideFullScreenShow], with the lock of
     * one second after the previous app open ad closed (A-08) in place of the `ads_interval` mark, which app open
     * never reads or moves (I-02). The ad leaves the slot when it shows, so an ad loaded while it is on screen is kept
     * (A-09); closing it or failing to show loads again at once (F-06). [onFinished] runs once, as for
     * [showInterstitial].
     */
    private suspend fun showAppOpen(activity: Activity, placement: AdPlacement, onFinished: () -> Unit): Boolean =
        withContext(Dispatchers.Main.immediate) {
            val enabled = isEnabled(placement).first()
            val now = clock.nowMillis()
            val decision = decideFullScreenShow(
                consentOpen = canRequestAds.value && gate.state.value.allowed,
                enabled = enabled,
                busy = presence.isBusy(now),
                ready = isFullScreenAdReady(placement),
                intervalPassed = appOpenLockPassed(lastAppOpenClosedAt, now),
            )
            val loadState = opportunityLoadState(placement)
            if (decision != FullScreenShowDecision.SHOW) {
                return@withContext skipTurn(placement, decision, loadState, onFinished)
            }
            // F-08: before the flag rises and before the ad leaves its slot.
            if (!isHostUsable(activity)) return@withContext skipHostGone(placement, onFinished)
            if (!presence.tryBegin(placement, System.identityHashCode(activity), now)) {
                return@withContext skipTurn(placement, FullScreenShowDecision.BUSY, loadState, onFinished)
            }
            val ad = loadedAppOpens.take(placement, now)
            if (ad == null) {
                presence.onClosed()
                return@withContext skipTurn(placement, FullScreenShowDecision.NOT_READY, loadState, onFinished)
            }
            appOpenCells.remove(ad)?.value = placement
            recordShownOpportunity(placement)
            val turn = beginTurn(onFinished)
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    // First line, as for interstitials; no `ads_interval` mark (I-02).
                    lastAppOpenClosedAt = clock.nowMillis()
                    endTurn(turn, "closed")
                    fullScreenReloads.onClosedOrShowFailed(placement)
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    logger.warn(TAG, "App open ${placement.key} failed to show: ${error.code}")
                    telemetry.showFailed(AdFormat.APP_OPEN, placement, error.code)
                    endTurn(turn, "show_failed")
                    fullScreenReloads.onClosedOrShowFailed(placement)
                }

                override fun onAdClicked() = telemetry.clicked(AdFormat.APP_OPEN, placement)
            }
            logger.info(TAG, "App open shown: ${placement.key}")
            ad.show(activity)
            true
        }

    /**
     * Shows the app open ad of a staging-only [placement] for the staging ad gallery ([rememberShowAppOpenAd], plan
     * T-036) through [showAppOpen], unchanged: the same gate, lock and metrics as the app open of the loading screen.
     */
    internal suspend fun showStagingAppOpen(
        activity: Activity,
        placement: AdPlacement,
        onFinished: () -> Unit = {},
    ): Boolean {
        require(placement.declaration.acceptsStagingAppOpen()) {
            "${placement.key} is not a staging app open placement"
        }
        return showAppOpen(activity, placement, onFinished)
    }

    /**
     * Shows the full-screen ad of [placement], interstitial or native full screen, if the full-screen gate allows it,
     * and returns whether it was shown ([rememberShowFullScreenAd]). [onFinished] runs once, as for
     * [showInterstitial].
     */
    internal suspend fun showFullScreenAd(
        activity: Activity,
        placement: AdPlacement,
        onFinished: () -> Unit = {},
    ): Boolean = when (placement.format) {
        AdFormat.INTERSTITIAL -> showInterstitial(activity, placement, onFinished)
        AdFormat.NATIVE_FULL_SCREEN -> showNativeFullScreen(placement, onFinished)
        else -> throw IllegalArgumentException("${placement.key} is not a full-screen placement")
    }

    /**
     * Shows the preloaded native full-screen ad of [placement] through [FullScreenAdHost], with the same gate as
     * interstitials ([decideFullScreenTurn]); it reads and writes the `ads_interval` mark like one (question 2 of plan
     * T-022). Returns at once, showing nothing, when the gate says no; never waits for an ad to load (KB-5). The ad
     * can be closed after the countdown of `time_close_native`, 5 s by default ([NativeFullScreenTurns], question 3 of
     * plan T-026). It is drawn by the
     * composition, not by an activity of its own, so it has no host activity for stuck recovery.
     */
    private suspend fun showNativeFullScreen(placement: AdPlacement, onFinished: () -> Unit): Boolean =
        withContext(Dispatchers.Main.immediate) {
            val decision = decideFullScreenTurn(placement)
            val loadState = opportunityLoadState(placement)
            if (decision != FullScreenShowDecision.SHOW) {
                return@withContext skipTurn(placement, decision, loadState, onFinished)
            }
            val takeAd = { takePreloadedNativeFullScreen(placement, clock.nowMillis()) }
            if (!beginStandaloneNativeTurn(placement, takeAd, onFinished)) {
                val skipped = if (nativeTurns.busy || presence.isBusy(clock.nowMillis())) {
                    FullScreenShowDecision.BUSY
                } else {
                    FullScreenShowDecision.NOT_READY
                }
                return@withContext skipTurn(placement, skipped, loadState, onFinished)
            }
            true
        }

    /**
     * Starts a standalone native full-screen turn of [placement] that passed the gate: raises the showing flag and
     * takes the ad with [takeAd]; false, with the flag down, when another full-screen ad or native full-screen turn
     * shows (even past the cap of F-02) or no ad is there (the caller skips the turn, KB-5). The turn has no host activity and is not the [currentTurn], so stuck recovery,
     * forced completion and the cap of F-02 never end it; only its close lowers the flag. [onFinished] runs once.
     */
    internal fun beginStandaloneNativeTurn(
        placement: AdPlacement,
        takeAd: () -> LoadedNativeAd?,
        onFinished: () -> Unit,
    ): Boolean {
        // A turn still open past the cap of F-02 keeps the screen: rejected before any ad is taken or recorded.
        if (nativeTurns.busy) return false
        if (!presence.tryBegin(placement, NO_HOST_ACTIVITY, clock.nowMillis())) return false
        val ad = takeAd()
        if (ad == null) {
            presence.onClosed()
            return false
        }
        nativeFullScreenCells.remove(ad)?.value = placement
        recordShownOpportunity(placement)
        // The turn opens with an ad: `af_native_ad_eligible` (T-033).
        telemetry.eligible(AdFormat.NATIVE_FULL_SCREEN)
        val completion = FullScreenCompletion(logger).apply { set(onFinished) }
        nativeTurns.showStandalone(placement, ad) { completion.complete("closed") }
        return true
    }

    /**
     * Whether a tap on the action of the rewarded [placement] opens its dialog ([rememberRewardedAction]): the
     * placement switches ([isEnabled]) and the consent gate ([canRequestAds]). A closed gate counts as switched off,
     * so the action runs at once with no dialog (question 8 of plan T-031, option (a)).
     */
    internal fun rewardedSwitch(placement: AdPlacement): Flow<Boolean> =
        combine(isEnabled(placement), canRequestAds) { on, consentOpen -> on && consentOpen }

    /**
     * One tap on "Watch ad" of the rewarded [placement], shown on [activity] ([RewardedTurns.request]), with [policy]
     * when no ad can be shown (the line's own policy, or the one picked on the staging ad gallery, plan T-036). Runs in
     * the scope of this controller, so a recreated screen does not cancel it; [onOutcome] runs on the main thread.
     */
    internal fun showRewarded(
        activity: Activity,
        placement: AdPlacement,
        policy: RewardedNoAdPolicy = placement.declaration.effectiveNoAdPolicy,
        onOutcome: (RewardedOutcome) -> Unit,
    ) {
        require(placement.format == AdFormat.REWARDED) { "${placement.key} is not a rewarded placement" }
        // Every tap on "Watch ad", busy or with no ad too (T-033, AdsKit RewardedAdManagerImpl).
        telemetry.eligible(AdFormat.REWARDED)
        scope.launch { rewardedTurns.request(placement.declaration, activity, onOutcome, policy) }
    }

    /** `reward_dialog` of the rewarded [placement], [action] `show` or `dismiss` (section 8.2). */
    internal fun recordRewardDialog(placement: AdPlacement, action: String) {
        telemetry.rewardDialog(placement, action)
    }

    /**
     * Shows the rewarded ad taken for the turn of [declaration] on [activity]; the SDK callbacks go to
     * [rewardedTurns]. Closing or failing to show loads again 2 s later (F-06); no `ads_interval` mark (R-05).
     */
    private fun showRewardedAd(declaration: AdPlacementDeclaration, activity: Activity?) {
        val placement = declaration.placement()
        val ad = showingRewardeds[placement]
        if (ad == null || activity == null) {
            logger.warn(TAG, "Rewarded ${placement.key}: nothing to show")
            rewardedTurns.onShowFailed(declaration, SHOW_FAILED_INTERNAL)
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                telemetry.displayed(
                    AdFormat.REWARDED,
                    ad.adUnitId,
                    ad.responseInfo.loadedAdapterResponseInfo?.adapterClassName,
                )
                rewardedTurns.onShowed(declaration)
            }

            override fun onAdDismissedFullScreenContent() {
                if (showingRewardeds[placement] === ad) showingRewardeds -= placement
                rewardedTurns.onDismissed(declaration)
                fullScreenReloads.onClosedOrShowFailed(placement)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                logger.warn(TAG, "Rewarded ${placement.key} failed to show: ${error.code}")
                telemetry.showFailed(AdFormat.REWARDED, placement, error.code)
                rewardedTurns.onShowFailed(declaration, error.code)
                fullScreenReloads.onClosedOrShowFailed(placement)
            }

            override fun onAdClicked() = telemetry.clicked(AdFormat.REWARDED, placement)
        }
        // R-02: the reward is given in the SDK's callback, before the ad closes.
        ad.show(activity) { rewardedTurns.onReward(declaration) }
    }

    /** The close button of the native full-screen turn on screen; works once its countdown is over (K-03, K-04). */
    internal fun closeNativeFullScreen() = nativeTurns.close()

    /** The back button over the native full-screen turn on screen (K-08). */
    internal fun onNativeFullScreenBack() = nativeTurns.onBack()

    /** Tests only: opens the consent gate and marks the SDK started, with no UMP form and no SDK (plan T-026). */
    @VisibleForTesting
    internal fun openConsentGateForTest() {
        gate.onPrivacyFormClosed(umpCanRequestAds = true)
        sdkStarted.value = true
    }

    /** Tests only: the user withdraws consent in the privacy options form (S-14, plan T-068). */
    @VisibleForTesting
    internal fun closeConsentGateForTest() {
        gate.onPrivacyFormClosed(umpCanRequestAds = false)
    }

    /** Tests only: the consent gate opens again, as `ConsentCoordinator` reports it (S-14, plan T-068). */
    @VisibleForTesting
    internal fun reopenConsentGateForTest() {
        gate.onPrivacyFormClosed(umpCanRequestAds = true)
        onGateOpened(firstTime = false)
    }

    /** Tests only: AdMob is ready (S-21) and MAX is asked to start, as `startAdsSdks` does (plan T-068). */
    @VisibleForTesting
    internal fun admobReadyForTest() {
        markSdkReadyForTest()
        maxStarter.onAdmobReady(mediationOrder.value)
    }

    /**
     * True while the in-screen ads (banner, native small, collapsible layer, pool cells) may send, refresh or retry:
     * an app activity is started and no full-screen ad is on screen ([inScreenAdsMayRun], KB-27, plan T-054). Not
     * read by the loads of the native full-screen ad itself, which the screen over the app waits for (K-04).
     */
    internal val inScreenAdWindow: StateFlow<Boolean> = inScreenAdWindowState.open

    /** The one send gate of the in-screen native sources and pools: the window, then [mayRequestNow] (L-072). */
    internal val inScreenSendGate: suspend () -> Boolean = { awaitSendAllowed(inScreenAdWindow, ::mayRequestNow) }

    /** `canLoadNow` of the pools: SDK ready, the in-screen window open and the gate open (P-05, KB-27). */
    internal fun poolCanLoadNow(): Boolean = _sdkReady.value && inScreenAdWindow.value && mayRequestNow()

    /** Tests only: the loader of the in-screen native slots instead of the SDK's (plan T-054). */
    @VisibleForTesting
    internal var nativeLoaderForTest: NativeAdLoader? = null

    /**
     * The loader of one in-screen native load: [nativeLoaderForTest] when set, else the SDK's, which logs its networks
     * under [logKey], the key the load logs under (plan T-058).
     */
    private fun newNativeLoader(
        events: NativeAdEvents?,
        options: NativeRequestOptions?,
        logKey: String,
    ): NativeAdLoader = nativeLoaderForTest
        ?: SdkNativeAdLoader(context, adRequest(), events, options, networkLog = networkLog, logKey = logKey)

    /** Tests only: the app comes to foreground or goes to background, as [AdsActivityTracker] reports it. */
    @VisibleForTesting
    internal fun setAppForegroundForTest(foreground: Boolean) {
        foregroundState.value = foreground
        inScreenAdWindowState.onForeground(foreground)
    }

    /** Tests only: the SDK reported its initialization (S-21). */
    @VisibleForTesting
    internal fun markSdkReadyForTest() {
        _sdkReady.value = true
    }

    /** Tests only: puts [ad] in the preloaded native full-screen slot of [placement], as a load would (plan T-044). */
    @VisibleForTesting
    internal fun putPreloadedNativeFullScreenForTest(placement: AdPlacement, ad: LoadedNativeAd) {
        preloadedNativeFullScreen.put(placement, ad, clock.nowMillis())
    }

    /** Tests only: puts [ad] in the interstitial slot of [placement], as a load would (plan T-055). */
    @VisibleForTesting
    internal fun putLoadedInterstitialForTest(placement: AdPlacement, ad: InterstitialAd) {
        loadedInterstitials[placement] = ad
    }

    /** Tests only: puts [ad] in the app open slot of [placement], loaded now, as a load would (plan T-055). */
    @VisibleForTesting
    internal fun putLoadedAppOpenForTest(placement: AdPlacement, ad: AppOpenAd) {
        loadedAppOpens.put(placement, ad, clock.nowMillis())
    }

    /** Tests only: puts [ad] in the rewarded slot of [placement], loaded now, as a load would (plan T-055). */
    @VisibleForTesting
    internal fun putLoadedRewardedForTest(placement: AdPlacement, ad: RewardedAd) {
        loadedRewardeds.put(placement, ad, clock.nowMillis())
    }

    /**
     * The full-screen gate of [placement], shared by every full-screen format, in the order of [decideFullScreenShow]:
     * consent (S-11), the placement switches, another full-screen ad on screen (F-01, F-02), a loaded ad (F-04), then
     * the `ads_interval` mark of the placement's group unless it declares [AdPlacement.ignoreInterval] (I-01, I-04,
     * MC-04). Reads the flags, never loads.
     */
    internal suspend fun decideFullScreenTurn(placement: AdPlacement): FullScreenShowDecision =
        withContext(Dispatchers.Main.immediate) {
            val enabled = isEnabled(placement).first()
            val intervalMillis = flags.intervalSeconds(placement.intervalGroup).first() * 1_000
            val now = clock.nowMillis()
            decideFullScreenShow(
                consentOpen = canRequestAds.value && gate.state.value.allowed,
                enabled = enabled,
                busy = presence.isBusy(now),
                ready = isFullScreenAdReady(placement),
                intervalPassed = !placement.declaration.readsInterval() ||
                    guard.allows(placement.intervalGroup, intervalMillis, now),
            )
        }

    /**
     * Takes the preloaded native full-screen ad of [placement] out of its slot (the one place that does), and sets
     * the placement its metrics read (P-04): the turn after an interstitial, the standalone turn and the page of a
     * flow step. Null when the slot has no fresh ad.
     */
    private fun takePreloadedNativeFullScreen(placement: AdPlacement, now: Long): LoadedNativeAd? =
        preloadedNativeFullScreen.take(placement, now)?.also { nativeFullScreenCells.remove(it)?.value = placement }

    /**
     * The states of the page of a flow step of [placement] ([NativeFullScreenStep], plan T-032): the preloaded ad of
     * its slot, a load asked when there is none, the ad destroyed and the slot loaded again when the page leaves. No
     * gate of full-screen ads, no `ads_interval` mark, no showing flag: the page is part of the flow (question 4).
     */
    internal fun fullScreenStep(placement: AdPlacement): Flow<NativeAdState> = fullScreenStepStates(
        take = { takeStepFullScreenAd(placement) },
        ensureLoading = { loadFullScreenAdIfNeeded(placement) },
        release = { releaseStepFullScreenAd(placement, it) },
    )

    /** S-11: nothing is taken while consent does not allow ads. */
    private fun takeStepFullScreenAd(placement: AdPlacement): LoadedNativeAd? =
        stepFullScreenTake(canRequestAds.value) { takePreloadedNativeFullScreen(placement, clock.nowMillis()) }
            ?.also { logger.info(TAG, "Flow step full page shown: ${placement.key}") }

    /** The page left: its ad is destroyed and the slot loads again, as after a close. */
    private fun releaseStepFullScreenAd(placement: AdPlacement, ad: LoadedNativeAd) {
        ad.destroy()
        logger.info(TAG, "Flow step full page left: ${placement.key}")
        fullScreenReloads.onClosedOrShowFailed(placement)
    }

    /** Whether the page of a flow step of [placement] has an ad now ([rememberNativeFullScreenStepReady]). */
    internal fun stepFullScreenReady(placement: AdPlacement): Boolean = isFullScreenAdReady(placement)

    /**
     * Whether a full-screen ad of [placement] is loaded and may be shown now. Reads the state only, never loads
     * (F-04); for the loading screen (T-024) and the retry rules (T-021).
     */
    internal fun isFullScreenAdReady(placement: AdPlacement): Boolean =
        fullScreenLoadState(placement) == FullScreenLoadState.Ready

    /** Loads a full-screen ad of [placement] unless one is loading, loaded, or waiting for a retry (F-04). */
    internal fun loadFullScreenAdIfNeeded(placement: AdPlacement) {
        if (needsLoad(fullScreenLoadState(placement), clock.nowMillis(), canRequestAds.value)) preloadNow(placement)
    }

    private fun fullScreenLoadState(placement: AdPlacement): FullScreenLoadState = when {
        placement.format == AdFormat.INTERSTITIAL && placement in loadedInterstitials -> FullScreenLoadState.Ready
        placement.format == AdFormat.NATIVE_FULL_SCREEN &&
            preloadedNativeFullScreen.hasFresh(placement, clock.nowMillis()) -> FullScreenLoadState.Ready
        placement.format == AdFormat.APP_OPEN &&
            loadedAppOpens.hasFresh(placement, clock.nowMillis()) -> FullScreenLoadState.Ready
        placement.format == AdFormat.REWARDED &&
            loadedRewardeds.hasFresh(placement, clock.nowMillis()) -> FullScreenLoadState.Ready
        else -> loadStates[placement] ?: FullScreenLoadState.Idle
    }

    /** `load_state` of an opportunity of [placement], taken at the decision (section 8.2). */
    private fun opportunityLoadState(placement: AdPlacement): String =
        loadStateAtDecision(fullScreenLoadState(placement), lastLoadErrors[placement])

    /** `shown` always goes with `ready` (section 8.2). */
    private fun recordShownOpportunity(placement: AdPlacement) {
        telemetry.opportunity(placement.format, placement, FullScreenShowDecision.SHOW.opportunityResult(), READY)
    }

    /** I-02, I-05: the mark of [placement]'s group moves now, if the placement pushes a mark (MC-04). */
    private fun recordIntervalMark(placement: AdPlacement) {
        if (placement.declaration.pushesIntervalMark()) guard.record(placement.intervalGroup, clock.nowMillis())
    }

    /** The completion of a turn that starts showing now; a turn left over after the cap of F-02 finishes first. */
    private fun beginTurn(onFinished: () -> Unit): FullScreenCompletion {
        currentTurn?.complete("cap_expired")
        return FullScreenCompletion(logger).also {
            it.set(onFinished)
            currentTurn = it
        }
    }

    /**
     * Ends [turn]: lowers the flag only when it is still the turn on screen, then runs its completion once. The flag
     * is published once after both (plan T-054): a combo that takes it again in the completion shows no false, so no
     * in-screen ad sends between the interstitial and its native ad.
     */
    private fun endTurn(turn: FullScreenCompletion, reason: String) {
        presence.publishAfter {
            if (currentTurn === turn) {
                currentTurn = null
                presence.onClosed()
            }
            turn.complete(reason)
        }
    }

    /** Ends the turn on screen, from stuck recovery or forced completion. */
    private fun endCurrentTurn(reason: String) {
        val turn = currentTurn
        if (turn != null) endTurn(turn, reason) else presence.onClosed()
    }

    /**
     * A turn that does not show: logs [decision] and its opportunity with the [loadState] taken at the decision,
     * loads when no ad is ready (F-07), and finishes at once (N-02).
     */
    private fun skipTurn(
        placement: AdPlacement,
        decision: FullScreenShowDecision,
        loadState: String,
        onFinished: () -> Unit,
    ): Boolean {
        logger.info(TAG, "Full-screen ${placement.key} skipped: $decision")
        // Before the load of this turn changes the state (section 8.2).
        telemetry.opportunity(placement.format, placement, decision.opportunityResult(), loadState)
        if (decision == FullScreenShowDecision.NOT_READY) loadFullScreenAdIfNeeded(placement)
        FullScreenCompletion(logger).apply { set(onFinished) }.complete(decision.name.lowercase())
        return false
    }

    /**
     * F-08 (KB-26, criterion 3; plan T-055): whether a full-screen ad of the SDK may be shown on [activity] now
     * ([fullScreenHostUsable]): not finishing, not destroyed, and the resumed activity of the tracker. The tracker sets
     * it before it reports the resume, so the resume ad shown from [onAppActivityResumed] passes; `Lifecycle` reaches
     * `RESUMED` only after that on API 29 and up. False before [start]. Every path that calls `show` of an SDK asks it
     * first: interstitial, app open, rewarded, and the MAX formats of T-069.
     */
    internal fun isHostUsable(activity: Activity?): Boolean = activity != null &&
        fullScreenHostUsable(activity.isFinishing, activity.isDestroyed, activity === tracker?.resumedActivity)

    /**
     * A turn that passed the gate on an activity that is not usable (F-08): the ad stays in its slot, `shown` then
     * `ad_show_fail` `app_not_foreground` (question 1 of plan T-055), and [onFinished] runs once at once. No flag, no
     * `ads_interval` mark, no load, no native ad after an interstitial.
     */
    private fun skipHostGone(placement: AdPlacement, onFinished: () -> Unit): Boolean {
        logger.info(TAG, "Full-screen ${placement.key} skipped: host not usable")
        recordShownOpportunity(placement)
        telemetry.showFailed(placement.format, placement, SHOW_FAILED_APP_NOT_FOREGROUND)
        FullScreenCompletion(logger).apply { set(onFinished) }.complete("host_not_usable")
        return false
    }

    /**
     * Whether [placement] may load and show ads: the master switch `is_show_ads` (off too while ad-free, KB-30,
     * [AdFreeStatus]) and the placement's own switch
     * `is_show_<code>` (KB-7, ADR-0014); for an app-wide placement, whether the keys of this open choose it
     * ([placementSwitch], A-01, A-02). Every ad format, including native ones added later, must check this before
     * loading or showing an ad.
     */
    internal fun isEnabled(placement: AdPlacement): Flow<Boolean> = placementSwitch(placement, flags, firstOpen)

    /**
     * N of the in-list ads of [placement] ([rememberInListAds]): `native_every_<code>`, or null while no ad row may
     * be added (no consent yet, master switch or placement off, bad remote value, or a placement that declares no
     * [AdPlacement.nativeEvery]).
     */
    internal fun inListEvery(placement: AdPlacement): Flow<Int?> {
        if (placement.nativeEvery == null) {
            logger.warn(TAG, "In-list placement has no nativeEvery: ${placement.key}")
            return flowOf(null)
        }
        return combine(
            canRequestAds,
            isEnabled(placement),
            flags.nativeEvery(placement),
        ) { canRequest, enabled, everyCount -> everyCount?.let { inListEvery(canRequest && enabled, it) } }
            .distinctUntilChanged()
    }

    /**
     * The line logged once at [start] when the build has a MAX SDK key but no MAX unit for some slots (plan T-071):
     * the slots only, never a unit.
     */
    private fun missingMaxUnits(): String? {
        if (!maxConfig.hasKey) return null
        val missing = MaxConfig.MAX_FORMATS.filter { maxConfig.unitFor(it) == null }
        if (missing.isEmpty()) return null
        return "MAX ad units missing for ${missing.joinToString(", ") { it.code }}; those slots run without MAX"
    }

    /**
     * The ad unit of [placement] drawn as [format]: its own format, or a layer of its bottom bar (plan T-030, question
     * 2). Null shows no ad.
     */
    internal fun unitId(placement: AdPlacement, format: AdFormat = placement.format): String? =
        unitIds.of(placement, format).also {
            if (it == null) Log.e(TAG, "No ad unit for placement ${nativeSlotKey(placement, format)}; it shows no ad")
        }

    /** The AppLovin MAX ad unit of the slot of [format] (plan T-071): banner and MREC; null without one or a key. */
    internal fun maxUnitId(format: AdFormat): String? = maxConfig.unitFor(format)

    /** The height of a MAX banner of a kind and width in tests, where the SDK of MAX gives the same as AdMob. */
    @VisibleForTesting
    internal var maxBannerHeightForTest: ((BannerSizeKind, Int) -> Int)? = null

    /**
     * The single-ad sources of native small slots (plan T-040), one per (placement, format), shut down when the
     * placement is switched off ([isEnabled]) or consent withdrawn ([canRequestAds]). Each load gets fresh metrics
     * events with the use the source gives it.
     */
    private val nativeSources by lazy {
        NativeAdSources(
            enabled = ::isEnabled,
            canRequestAds = canRequestAds,
            newLoad = { placement, format, use -> nativeLoadOf(placement, format, use) },
            sendGate = inScreenSendGate,
            clock = clock,
            scope = scope,
            logger = logger,
        )
    }

    /**
     * The pools of native medium and in-collection placements (plan T-028), one per (format, ad unit), closed when
     * every placement of a pool is switched off ([isEnabled]) or consent withdrawn ([canRequestAds]). Every send goes
     * through the send gate of native ads ([awaitSendAllowed], L-072).
     */
    private val nativePools: NativeAdPools by lazy {
        var made = 0
        NativeAdPools(
            placements = pooledPlacements(),
            unitId = ::unitId,
            enabled = ::isEnabled,
            canRequestAds = canRequestAds,
            newPool = { key, placement ->
                made++
                NativeAdPool(
                    key = key,
                    capacity = requireNotNull(key.format.poolCapacity()),
                    load = { poolLoad(key, placement) },
                    sendGate = inScreenSendGate,
                    canLoadNow = ::poolCanLoadNow,
                    canShowNow = { canRequestAds.value },
                    retryResets = preloader.nativeRetryResets,
                    clock = clock,
                    scope = scope,
                    logger = logger,
                    logKey = "${key.format.code}#$made",
                )
            },
            scope = scope,
            logger = logger,
        )
    }

    /**
     * One load of a pool: a fresh [PlacementCell] for the placement of the ad, set when a cell claims it (P-04); the
     * metrics of the load report `pool` with no `load_attempt` (PL-02). Null when no ad came.
     */
    private suspend fun poolLoad(key: NativePoolKey, placement: AdPlacement): PoolLoad? {
        val size = key.format.nativeLoadSize() ?: return null
        val holder = PlacementCell()
        val state = nativeLoadOnce(
            newEvents = {
                NativeAdMetrics(
                    format = key.format,
                    unitId = key.unitId,
                    load = telemetry.nativeLoadReport(placement, NativeLoadUse.POOL),
                    placement = { holder.value },
                )
            },
            newLoader = { events -> newNativeLoader(events, null, POOL_LOG_KEY) },
            unitId = key.unitId,
            size = size,
            timeoutMillis = NATIVE_LOAD_TIMEOUT_MILLIS,
            logger = logger,
            placementKey = POOL_LOG_KEY,
        )()
        return (state as? NativeAdState.Loaded)?.let { PoolLoad(it.ad, holder) }
    }

    /**
     * The states of one cell of the pool of [placement] ([pooledCellStates]); Unavailable when the placement has no
     * pool (no ad unit). The caller checks consent and [isEnabled] first, like [NativeAdSlot].
     */
    internal fun pooledCell(placement: AdPlacement): Flow<NativeAdState> =
        nativePools.pool(placement)?.let { pooledCellStates(it, placement, logger, placement.key) }
            ?: flowOf(NativeAdState.Unavailable)

    /** P-05 for the cells of [placement]: true when it has no pool (no ad unit), like [pooledCell]. */
    internal fun poolExhausted(placement: AdPlacement): Boolean = nativePools.pool(placement)?.exhausted ?: true

    /**
     * The states of the card of a flow step of [placement] on its pool ([stepAdStates], plan T-032); Unavailable when
     * the placement has no pool (no ad unit). The caller checks consent and [isEnabled] first, like [NativeStepAd].
     */
    internal fun stepAd(placement: AdPlacement, steps: Flow<Any>): Flow<NativeAdState> =
        nativePools.pool(placement)?.let { stepAdStates(it, placement, steps, logger, placement.key) }
            ?: flowOf(NativeAdState.Unavailable)

    /** The `ad_opportunity` of one cell of a pool (P-07). */
    internal fun recordPoolCellOpportunity(placement: AdPlacement, result: String) {
        telemetry.poolCellOpportunity(placement, result)
    }

    /**
     * The states of one native ad of [placement], drawn as [format], for a slot on screen: loading, then loaded, or
     * unavailable and a new try 5 s later (O-05), sent in foreground only and when [mayRequestNow]; Internet back cuts
     * the wait short (S-24). Each try waits at most [NATIVE_LOAD_TIMEOUT_MILLIS] and gets its own metrics events
     * (T-023). A native small slot ([usesSingleAdSource]) claims its ad from the source of its placement and replaces
     * it in place every 60 s in foreground, the first time [firstRefreshDelayMillis] after the first ad (plan T-040);
     * a pooled format (medium) is a cell of its pool ([pooledCell], plan T-028); other formats keep their ad, destroyed
     * when the collection is cancelled. The caller checks consent and [isEnabled] first, like [NativeAdSlot].
     */
    internal fun nativeAd(
        placement: AdPlacement,
        format: AdFormat = placement.format,
        firstRefreshDelayMillis: Long = NativeAdPolicy.REFRESH_INTERVAL_MILLIS,
    ): Flow<NativeAdState> {
        val size = requireNotNull(format.nativeLoadSize()) { "${placement.key} is not a native placement" }
        if (format == placement.format && format.usesPool) return pooledCell(placement)
        return flow {
            val unitId = unitId(placement, format)
            if (unitId == null) {
                emit(NativeAdState.Unavailable)
                return@flow
            }
            if (format.usesSingleAdSource()) {
                emitAll(
                    singleAdSlotStates(
                        source = nativeSources.source(placement, format),
                        foreground = inScreenAdWindow,
                        mayRequestNow = ::mayRequestNow,
                        retryResets = preloader.nativeRetryResets,
                        clock = clock,
                        logger = logger,
                        placementKey = nativeSlotKey(placement, format),
                        firstTickDelayMillis = firstRefreshDelayMillis,
                    ),
                )
                return@flow
            }
            emitAll(
                retryingNativeAdStates(
                    load = nativeLoadOnce(
                        newEvents = { nativeAdMetrics(placement, unitId, NativeLoadUse.SINGLE_AD, format) },
                        newLoader = { events -> newNativeLoader(events, null, placement.key) },
                        unitId = unitId,
                        size = size,
                        timeoutMillis = NATIVE_LOAD_TIMEOUT_MILLIS,
                        logger = logger,
                        placementKey = placement.key,
                    ),
                    foreground = inScreenAdWindow,
                    mayRequestNow = ::mayRequestNow,
                    retryResets = preloader.nativeRetryResets,
                    logger = logger,
                    placementKey = placement.key,
                ),
            )
        }
    }

    /** One load of a single-ad source: [nativeLoadOnce] with fresh metrics events of [use] (L-A07). */
    private fun nativeLoadOf(
        placement: AdPlacement,
        format: AdFormat,
        use: NativeLoadUse,
    ): suspend () -> NativeAdState {
        val unitId = unitId(placement, format) ?: return { NativeAdState.Unavailable }
        val size = format.nativeLoadSize() ?: return { NativeAdState.Unavailable }
        // The collapsible layer has the collapse button in its top right corner: AdChoices goes top left (T-030).
        val options = if (format == AdFormat.NATIVE_COLLAPSIBLE) collapsibleNativeRequestOptions() else null
        return nativeLoadOnce(
            newEvents = { nativeAdMetrics(placement, unitId, use, format) },
            newLoader = { events -> newNativeLoader(events, options, nativeSlotKey(placement, format)) },
            unitId = unitId,
            size = size,
            timeoutMillis = NATIVE_LOAD_TIMEOUT_MILLIS,
            logger = logger,
            placementKey = nativeSlotKey(placement, format),
        )
    }

    /**
     * The user collapsed the collapsible layer of the bottom bar of [placement] ([BottomAdBarScaffold]). Logged only:
     * no analytics event (spec section 6).
     */
    internal fun onNativeCollapsed(placement: AdPlacement) {
        logger.info(TAG, "native_collapsed placement=${placement.key}")
    }

    /** The monotonic clock of the ad rules, for the timer that opens the collapsible layer again (CL-03). */
    internal val adsClock: MonotonicClock get() = clock

    /** The layers of the bottom bar of [placement] changed ([BottomAdBarScaffold]). Logged only (ADR-0019). */
    internal fun onBottomBarLayers(placement: AdPlacement, layers: BottomAdBarLayers?) {
        logger.info(TAG, bottomBarLayersLog(placement, layers))
    }

    /** The collapsible layer of [placement] opened again [seconds] after the user collapsed it (CL-03). Logged only. */
    internal fun onNativeReexpanded(placement: AdPlacement, seconds: Long) {
        logger.info(TAG, "Collapsible ${placement.key} re-expanded after $seconds s")
    }

    /**
     * The states of the native small row of [slot] of a list of [placement] ([inListRowStates]): its own `LIST_ROW`
     * load on each try, tried again like a native slot (5 s, the in-screen window, the gate right before the send,
     * Internet back) but never refreshed and never through a single-ad source (plan T-059, section 3.1). Logs under
     * `<code>#<slot>`, so rows loading at once do not mix; the metrics keep [placement]. [rowShown] is true while the
     * row is drawn. The caller checks consent and [isEnabled] first ([inListEvery]).
     */
    internal fun inListRowStates(
        placement: AdPlacement,
        slot: Int,
        rowShown: StateFlow<Boolean>,
    ): Flow<NativeAdState> = flow {
        val size = requireNotNull(placement.format.nativeLoadSize()) { "${placement.key} is not a native placement" }
        val unitId = unitId(placement)
        if (unitId == null) {
            emit(NativeAdState.Unavailable)
            return@flow
        }
        val key = "${placement.key}#$slot"
        emitAll(
            inListRowStates(
                load = nativeLoadOnce(
                    newEvents = { nativeAdMetrics(placement, unitId, NativeLoadUse.LIST_ROW) },
                    newLoader = { events -> newNativeLoader(events, null, key) },
                    unitId = unitId,
                    size = size,
                    timeoutMillis = NATIVE_LOAD_TIMEOUT_MILLIS,
                    logger = logger,
                    placementKey = key,
                ),
                window = inScreenAdWindow,
                rowShown = rowShown,
                mayRequestNow = ::mayRequestNow,
                retryResets = preloader.nativeRetryResets,
                logger = logger,
                placementKey = key,
            ),
        )
    }

    /**
     * The metrics of one native load of [placement] used as [use]: the load report comes from [nativeLoadReport]
     * (a list row gets its own report on each call, so rows loading at once do not mix: question 6 of plan T-023),
     * drawn as [format].
     */
    private fun nativeAdMetrics(
        placement: AdPlacement,
        unitId: String,
        use: NativeLoadUse,
        format: AdFormat = placement.format,
    ): NativeAdEvents = NativeAdMetrics(
        format = format,
        unitId = unitId,
        load = telemetry.nativeLoadReport(placement, use, format),
        placement = { placement },
    )

    /**
     * Hands the SDK callbacks of one native ad to [telemetry]: load result, paid event, impression with the
     * zero-value fallback of T-02 (a timer in [scope], cancelled when the ad is destroyed), click.
     */
    private inner class NativeAdMetrics(
        private val format: AdFormat,
        private val unitId: String,
        private val load: LoadReport,
        private val placement: () -> AdPlacement?,
        private val onLoadFailed: (label: String) -> Unit = {},
    ) : NativeAdEvents {
        private val fallback = NativeRevenueFallback()
        private var fallbackJob: Job? = null
        private val funnel = NativeFunnel(telemetry, format, unitId)

        @Volatile private var adSource: String? = null

        @Volatile private var adapterClassName: String? = null

        override fun onRequestSent() = load.sent(clock.nowMillis())

        override fun onLoaded(ad: NativeAd) {
            val adapter = ad.responseInfo?.loadedAdapterResponseInfo
            adSource = adapter?.adSourceName
            adapterClassName = adapter?.adapterClassName
            load.succeeded(clock.nowMillis())
            funnel.onLoaded(adapter?.adapterClassName)
        }

        override fun onFailed(code: Int, sdkMessage: String?) {
            onLoadFailed(load.failed(code, sdkMessage, clock.nowMillis()))
        }

        override fun onPaid(value: AdValue) {
            if (!fallback.onPaidEvent()) return
            fallbackJob?.cancel()
            telemetry.revenue(
                format,
                unitId,
                placement,
                value.valueMicros,
                value.currencyCode,
                adSource,
                adapterClassName,
            )
        }

        override fun onImpression() {
            funnel.onImpression()
            val due = fallback.onImpression(clock.nowMillis())
            fallbackJob?.cancel()
            fallbackJob = scope.launch {
                delay((due - clock.nowMillis()).coerceAtLeast(0))
                if (fallback.onDue(clock.nowMillis())) {
                    telemetry.revenueFallback(format, unitId, placement, adSource, adapterClassName)
                }
            }
        }

        override fun onClicked() = telemetry.clicked(format, placement())

        override fun onDestroyed() {
            fallbackJob?.cancel()
        }
    }

    /** Personalization follows the consent string of UMP only (question 8 of the ads spec): no `npa` extra. */
    internal fun adRequest(): AdRequest = AdRequest.Builder().build()

    /**
     * Preloads one full-screen ad of [placement] now; each loader checks the switches and the gate itself. A rewarded
     * round decides in [RewardedTurns.loadIfNeeded] (L-072).
     */
    private fun preloadNow(placement: AdPlacement) {
        when (fullScreenLoaderOf(placement.format)) {
            FullScreenLoader.NATIVE_FULL_SCREEN -> preloadNativeFullScreen(placement)
            FullScreenLoader.APP_OPEN -> preloadAppOpen(placement)
            FullScreenLoader.INTERSTITIAL -> preloadInterstitial(placement)
            FullScreenLoader.REWARDED -> scope.launch { rewardedTurns.loadIfNeeded(placement.declaration) }
        }
    }

    /**
     * Loads one native full-screen ad of [placement] for later, unless a fresh one is kept, one is loading or a retry
     * is not due (F-04), or the placement is off. Media of any aspect ratio, video muted, AdChoices in the top right
     * corner. Sent in foreground only, not by the in-screen window ([inScreenAdWindow], KB-27): the screen over the app
     * waits for this very load (K-04, plan T-054). A failure retries by O-05 ([FullScreenReloads]).
     */
    private fun preloadNativeFullScreen(placement: AdPlacement) {
        // The gate is asked right before sending, below.
        if (!needsLoad(fullScreenLoadState(placement), clock.nowMillis(), gateOpen = true)) return
        val unitId = unitId(placement) ?: return
        loadStates[placement] = FullScreenLoadState.Loading
        scope.launch {
            // O-05, L-A19: a native request, the first one too, is never sent in background; the load is put off until
            // the app is in foreground again. The slot stays Loading meanwhile, so the plan and loadIfNeeded do not
            // start a second load (F-04); the gate and the switch are read once the wait is over.
            if (!foregroundState.value) logger.info(TAG, "Native full-screen ${placement.key} waits for foreground")
            foregroundState.first { it }
            if (!canRequestAds.value || !isEnabled(placement).first()) {
                loadStates -= placement
                // Nothing was sent: a combo waiting for this placement fails (K-04).
                nativeLoadResults.signal(placement)
                return@launch
            }
            val cell = PlacementCell()
            val events = NativeAdMetrics(
                format = AdFormat.NATIVE_FULL_SCREEN,
                unitId = unitId,
                load = telemetry.countedLoad(
                    AdFormat.NATIVE_FULL_SCREEN,
                    preloadCounterKey(placement.key),
                    loadPlacementOf(AdFormat.NATIVE_FULL_SCREEN, placement),
                ),
                placement = { cell.value },
                onLoadFailed = { lastLoadErrors[placement] = it },
            )
            // Asked right before sending (S-15): a closed gate ends the round, no request, no retry.
            if (!mayRequestNow()) {
                loadStates[placement] = fullScreenReloads.onSendBlocked(placement)
                logger.info(TAG, "Native full-screen ${placement.key} not requested: consent gate closed")
                nativeLoadResults.signal(placement)
                return@launch
            }
            try {
                // O-06, shared with every native request (NativeRequestOptions.kt).
                val options = fullScreenNativeRequestOptions().toNativeAdOptions()
                val loader = AdLoader.Builder(context, unitId)
                    .forNativeAd { ad ->
                        networkLog.loaded(placement.key, ad.responseInfo)
                        loadStates -= placement
                        lastLoadErrors -= placement
                        ad.setOnPaidEventListener { events.onPaid(it) }
                        events.onLoaded(ad)
                        val loaded = SdkLoadedNativeAd(ad, events)
                        nativeFullScreenCells[loaded] = cell
                        preloadedNativeFullScreen.put(placement, loaded, clock.nowMillis())
                        // L-068: a combo waiting for this placement takes the ad.
                        nativeLoadResults.signal(placement)
                    }
                    .withAdListener(
                        object : AdListener() {
                            override fun onAdFailedToLoad(error: LoadAdError) {
                                networkLog.failed(placement.key, error)
                                logger.warn(TAG, "Native full-screen ${placement.key} failed to load: ${error.code}")
                                onNativeFullScreenLoadFailed(placement, events, error.code, error.message)
                            }

                            override fun onAdImpression() = events.onImpression()

                            override fun onAdClicked() = events.onClicked()
                        },
                    )
                    .withNativeAdOptions(options)
                    .build()
                val request = adRequest()
                events.onRequestSent()
                loader.loadAd(request)
            } catch (e: Exception) {
                // O-09: the request could not be built or sent; never the message of the error.
                logger.warn(TAG, "Native full-screen ${placement.key} failed to load: 0 (request not built)")
                onNativeFullScreenLoadFailed(placement, events, 0, null)
            }
        }
    }

    /** The load of the native full-screen ad of [placement] failed: counted (T-023), then a retry (O-05). */
    private fun onNativeFullScreenLoadFailed(
        placement: AdPlacement,
        events: NativeAdEvents,
        code: Int,
        message: String?,
    ) {
        loadStates -= placement
        events.onFailed(code, message)
        loadStates[placement] = fullScreenReloads.onLoadFailed(placement, code)
        // L-068: a combo waiting for this placement fails.
        nativeLoadResults.signal(placement)
    }

    /**
     * Loads one interstitial of [placement] for later, unless one is kept, one is loading or a retry is not due
     * (F-04), or the placement is off. Loaded with the resumed activity of the app when there is one (question 4 of
     * plan T-034). A failure retries by F-05 ([FullScreenReloads]).
     */
    private fun preloadInterstitial(placement: AdPlacement) {
        // The gate is asked right before sending, below.
        if (!needsLoad(fullScreenLoadState(placement), clock.nowMillis(), gateOpen = true)) return
        val unitId = unitId(placement) ?: return
        loadStates[placement] = FullScreenLoadState.Loading
        scope.launch {
            if (!isEnabled(placement).first()) {
                loadStates -= placement
                return@launch
            }
            // Asked right before sending (S-15): a closed gate ends the round, no request, no retry.
            if (!mayRequestNow()) {
                loadStates[placement] = fullScreenReloads.onSendBlocked(placement)
                logger.info(TAG, "Interstitial ${placement.key} not requested: consent gate closed")
                return@launch
            }
            val slot = preloadCounterKey(placement.key)
            val loadPlacement = loadPlacementOf(AdFormat.INTERSTITIAL, placement)
            val callback = object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    networkLog.loaded(placement.key, ad.responseInfo)
                    loadStates -= placement
                    lastLoadErrors -= placement
                    telemetry.loadSucceeded(AdFormat.INTERSTITIAL, slot, loadPlacement, clock.nowMillis())
                    val cell = PlacementCell()
                    val adapter = ad.responseInfo.loadedAdapterResponseInfo
                    telemetry.apiCalled(AdFormat.INTERSTITIAL, unitId, adapter?.adapterClassName)
                    ad.setOnPaidEventListener { value ->
                        telemetry.revenue(
                            AdFormat.INTERSTITIAL,
                            unitId,
                            { cell.value },
                            value.valueMicros,
                            value.currencyCode,
                            adapter?.adSourceName,
                            adapter?.adapterClassName,
                        )
                    }
                    interstitialCells[ad] = cell
                    loadedInterstitials[placement] = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    networkLog.failed(placement.key, error)
                    logger.warn(TAG, "Interstitial ${placement.key} failed to load: ${error.code}")
                    onInterstitialLoadFailed(placement, slot, loadPlacement, error.code, error.message)
                }
            }
            try {
                val request = adRequest()
                telemetry.requestSent(slot, clock.nowMillis())
                InterstitialAd.load(fullScreenLoadContext(tracker?.resumedActivity, context), unitId, request, callback)
            } catch (e: Exception) {
                // O-09: the request could not be built or sent; never the message of the error.
                logger.warn(TAG, "Interstitial ${placement.key} failed to load: 0 (request not built)")
                onInterstitialLoadFailed(placement, slot, loadPlacement, 0, null)
            }
        }
    }

    /**
     * Loads one app open ad of [placement] for the app open slot, unless one is kept, one is loading or a retry is
     * not due (F-04), or the placement is off; like [preloadInterstitial]. Its paid events carry the placement it is
     * shown for (PL-03). A failure retries by F-05 ([FullScreenReloads]).
     */
    private fun preloadAppOpen(placement: AdPlacement) {
        // The gate is asked right before sending, below.
        if (!needsLoad(fullScreenLoadState(placement), clock.nowMillis(), gateOpen = true)) return
        val unitId = unitId(placement) ?: return
        loadStates[placement] = FullScreenLoadState.Loading
        scope.launch {
            if (!isEnabled(placement).first()) {
                loadStates -= placement
                return@launch
            }
            // Asked right before sending (S-15): a closed gate ends the round, no request, no retry.
            if (!mayRequestNow()) {
                loadStates[placement] = fullScreenReloads.onSendBlocked(placement)
                logger.info(TAG, "App open ${placement.key} not requested: consent gate closed")
                return@launch
            }
            val slot = preloadCounterKey(placement.key)
            val loadPlacement = loadPlacementOf(AdFormat.APP_OPEN, placement)
            val callback = object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    networkLog.loaded(placement.key, ad.responseInfo)
                    loadStates -= placement
                    lastLoadErrors -= placement
                    telemetry.loadSucceeded(AdFormat.APP_OPEN, slot, loadPlacement, clock.nowMillis())
                    val cell = PlacementCell()
                    val adapter = ad.responseInfo.loadedAdapterResponseInfo
                    ad.setOnPaidEventListener { value ->
                        telemetry.revenue(
                            AdFormat.APP_OPEN,
                            unitId,
                            { cell.value },
                            value.valueMicros,
                            value.currencyCode,
                            adapter?.adSourceName,
                            adapter?.adapterClassName,
                        )
                    }
                    appOpenCells[ad] = cell
                    loadedAppOpens.put(placement, ad, clock.nowMillis())
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    networkLog.failed(placement.key, error)
                    logger.warn(TAG, "App open ${placement.key} failed to load: ${error.code}")
                    onFullScreenLoadFailed(AdFormat.APP_OPEN, placement, slot, loadPlacement, error.code, error.message)
                }
            }
            try {
                val request = adRequest()
                telemetry.requestSent(slot, clock.nowMillis())
                AppOpenAd.load(fullScreenLoadContext(tracker?.resumedActivity, context), unitId, request, callback)
            } catch (e: Exception) {
                // O-09: the request could not be built or sent; never the message of the error.
                logger.warn(TAG, "App open ${placement.key} failed to load: 0 (request not built)")
                onFullScreenLoadFailed(AdFormat.APP_OPEN, placement, slot, loadPlacement, 0, null)
            }
        }
    }

    /**
     * Sends one rewarded load of [placement] ([RewardedTurns.loadIfNeeded] decided it), with the resumed activity of
     * the app when there is one, like an interstitial (Unity, ironSource). Its paid events carry the placement it is
     * shown for (PL-03). A failure is counted, retried by F-05 and kept for the network rule of R-03.
     */
    private fun sendRewardedLoad(placement: AdPlacement) {
        val unitId = unitId(placement) ?: return
        loadStates[placement] = FullScreenLoadState.Loading
        val slot = preloadCounterKey(placement.key)
        val loadPlacement = loadPlacementOf(AdFormat.REWARDED, placement)
        val callback = object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                networkLog.loaded(placement.key, ad.responseInfo)
                loadStates -= placement
                lastLoadErrors -= placement
                telemetry.loadSucceeded(AdFormat.REWARDED, slot, loadPlacement, clock.nowMillis())
                val cell = PlacementCell()
                val adapter = ad.responseInfo.loadedAdapterResponseInfo
                telemetry.apiCalled(AdFormat.REWARDED, unitId, adapter?.adapterClassName)
                ad.setOnPaidEventListener { value ->
                    telemetry.revenue(
                        AdFormat.REWARDED,
                        unitId,
                        { cell.value },
                        value.valueMicros,
                        value.currencyCode,
                        adapter?.adSourceName,
                        adapter?.adapterClassName,
                    )
                }
                rewardedCells[ad] = cell
                loadedRewardeds.put(placement, ad, clock.nowMillis())
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                networkLog.failed(placement.key, error)
                logger.warn(TAG, "Rewarded ${placement.key} failed to load: ${error.code}")
                onRewardedLoadFailed(placement, slot, loadPlacement, error.code, error.message)
            }
        }
        try {
            val request = adRequest()
            telemetry.requestSent(slot, clock.nowMillis())
            RewardedAd.load(fullScreenLoadContext(tracker?.resumedActivity, context), unitId, request, callback)
        } catch (e: Exception) {
            // O-09: the request could not be built or sent; never the message of the error.
            logger.warn(TAG, "Rewarded ${placement.key} failed to load: 0 (request not built)")
            onRewardedLoadFailed(placement, slot, loadPlacement, 0, null)
        }
    }

    /** A rewarded load of [placement] failed: counted and retried (F-05), and the network flag of R-03 set. */
    private fun onRewardedLoadFailed(
        placement: AdPlacement,
        slot: String,
        loadPlacement: String?,
        code: Int,
        message: String?,
    ) {
        onFullScreenLoadFailed(AdFormat.REWARDED, placement, slot, loadPlacement, code, message)
        rewardedTurns.onLoadFailed(placement.declaration, code, offline = !online.value)
    }

    /** A load of the interstitial of [placement] failed: counted first (T-023), then a retry (F-05). */
    private fun onInterstitialLoadFailed(
        placement: AdPlacement,
        slot: String,
        loadPlacement: String?,
        code: Int,
        message: String?,
    ) = onFullScreenLoadFailed(AdFormat.INTERSTITIAL, placement, slot, loadPlacement, code, message)

    /** A load of the interstitial or app open ad of [placement] failed: counted first (T-023), then a retry (F-05). */
    private fun onFullScreenLoadFailed(
        format: AdFormat,
        placement: AdPlacement,
        slot: String,
        loadPlacement: String?,
        code: Int,
        message: String?,
    ) {
        loadStates -= placement
        lastLoadErrors[placement] = telemetry.loadFailed(
            format,
            slot,
            loadPlacement,
            code,
            message,
            clock.nowMillis(),
        )
        loadStates[placement] = fullScreenReloads.onLoadFailed(placement, code)
    }

    private companion object {
        const val TAG = "Ads"

        /** `load_state` of a shown full-screen ad. */
        const val READY = "ready"

        /**
         * The global flags read by the metrics before [AdsFlags.global] first emits: the defaults of T-019, so both
         * lifecycle switches off (nothing is served then anyway, S-19).
         */
        val DEFAULT_GLOBAL_FLAGS = AdsGlobalFlags(
            adsEnabled = AdsSettings.IS_SHOW_ADS.default,
            showOpenAds = AdsSettings.IS_SHOW_ADS.default && AdsSettings.SHOW_OPEN_ADS.default,
            showOpenAdsFirstOpen = AdsSettings.IS_SHOW_ADS.default && AdsSettings.SHOW_OPEN_ADS_FIRST_OPEN.default,
            openAdType = OpenAdType.of(AdsSettings.OPEN_AD_TYPE.default),
            showResumeAds = AdsSettings.IS_SHOW_ADS.default && AdsSettings.SHOW_RESUM_ADS.default,
            resumeAdType = OpenAdType.of(AdsSettings.RESUME_AD_TYPE.default),
            adsIntervalSeconds = AdsSettings.ADS_INTERVAL.default.coerceAtLeast(0),
            showNativeAfterInter = AdsSettings.IS_SHOW_ADS.default && AdsSettings.IS_SHOW_NATIVE_AFTER_INTER.default,
            showNativeBottom = AdsSettings.IS_SHOW_NATIVE_BOTTOM.default,
            showNativeCollap = AdsSettings.IS_SHOW_NATIVE_BOTTOM.default && AdsSettings.IS_SHOW_NATIVE_COLLAP.default,
            expandCollapAfterSeconds = AdsSettings.TIME_EXPAND_COLLAP_AD.default.coerceAtLeast(0),
            consentShortcutEnabled = AdsSettings.IS_ENABLE_CONSENT_SHORTCUT.default,
            logFullScreenEvents = AdsSettings.IS_LOG_AD_FULLSCREEN_EVENTS.default,
            logSlotEvents = AdsSettings.IS_LOG_AD_SLOT_EVENTS.default,
            closeNativeCountdownSeconds = nativeCloseCountdownSeconds(AdsSettings.TIME_CLOSE_NATIVE.default),
        )

        /** Host of a full-screen ad drawn by the composition; `System.identityHashCode` gives 0 only for null. */
        const val NO_HOST_ACTIVITY = 0

        /** The startup ad turn counts as over this long after [start] (question 7 of plan T-025, A-03 is 40 s). */
        const val STARTUP_FINISHED_CAP_MILLIS = 45_000L

        /**
         * AppsFlyer starts at the latest this long after [start] when no consent form is open: the consent wait of
         * the loading screen ([StartupAdWait], ADR-0027, decision 3).
         */
        const val ATTRIBUTION_WAIT_MILLIS = 10_000L

        /** Time to wait for a native ad before its space is removed (kept from wave 2 until T-027). */
        const val NATIVE_LOAD_TIMEOUT_MILLIS = 10_000L

        /** `FullScreenContentCallback` code "internal error", for a rewarded show with no ad or no activity. */
        const val SHOW_FAILED_INTERNAL = 0

        /** Log key of the loads of a pool: never the ad unit (ADR-0019). */
        const val POOL_LOG_KEY = "pool"
    }
}

/**
 * The size a native ad of this format is loaded at, `null` for formats that are not native. Exhaustive on purpose,
 * with no `else`: a new [AdFormat] must decide here how it loads.
 */
internal fun AdFormat.nativeLoadSize(): NativeAdSize? = when (this) {
    AdFormat.BANNER -> null
    AdFormat.INTERSTITIAL -> null
    AdFormat.NATIVE_SMALL -> NativeAdSize.SMALL
    AdFormat.NATIVE_MEDIUM -> NativeAdSize.MEDIUM
    // Opens expanded, with media, so it loads as medium.
    AdFormat.NATIVE_COLLAPSIBLE -> NativeAdSize.MEDIUM
    // Preloaded by its own loader with full-screen options, never through nativeAd or inListRowStates.
    AdFormat.NATIVE_FULL_SCREEN -> null
    // App open (its own slot, T-024) and rewarded are not native. The pool of T-028 loads the in-collection cards
    // with portrait media, and the cards of a flow step at their 250 dp size, media wide (T-032).
    AdFormat.APP_OPEN -> null
    AdFormat.REWARDED -> null
    AdFormat.BANNER_COLLAPSIBLE -> null
    AdFormat.BANNER_MREC -> null
    AdFormat.NATIVE_STEP -> NativeAdSize.STEP
    AdFormat.NATIVE_IN_COLLECTION -> NativeAdSize.CARD
}
