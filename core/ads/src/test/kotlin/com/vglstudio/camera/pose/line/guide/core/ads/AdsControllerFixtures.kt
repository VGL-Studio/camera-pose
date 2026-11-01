package com.vglstudio.camera.pose.line.guide.core.ads

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.test.core.app.ApplicationProvider
import com.vglstudio.camera.pose.line.guide.core.analytics.AdFunnel
import com.vglstudio.camera.pose.line.guide.core.analytics.AdMetrics
import com.vglstudio.camera.pose.line.guide.core.analytics.AppsFlyerStartup
import com.vglstudio.camera.pose.line.guide.core.analytics.RecordingAdFunnel
import com.vglstudio.camera.pose.line.guide.core.analytics.RecordingAdMetrics
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import com.vglstudio.camera.pose.line.guide.core.network.InternetConnectivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * The real [AdsController] for tests, with no SDK ad (plan T-033): every test builds it here, so a new parameter of
 * the controller is added in one place. Defaults: Google's test ad units, no consent debug, no ads debug (no test device, no per-network log), the defaults of every
 * Remote Config key, Internet on, no consent shortcut, recording metrics, funnel and logger, empty preferences, no
 * AppLovin MAX SDK key (MAX never starts, plan T-068).
 */
internal fun testAdsController(
    context: Context = ApplicationProvider.getApplicationContext(),
    unitIds: AdUnitIds = GoogleTestAdUnitIds,
    flags: AdsFlags = DefaultAdsFlags(FakeRemoteSettings()),
    metrics: AdMetrics = RecordingAdMetrics(),
    funnel: AdFunnel = RecordingAdFunnel(),
    attribution: AppsFlyerStartup = FakeAppsFlyerStartup(),
    logger: AppLogger = RecordingAppLogger(),
    connectivity: InternetConnectivity = AlwaysConnected,
    consentDebug: ConsentDebugOptions = ConsentDebugOptions.NONE,
    debugOptions: AdsDebugOptions = AdsDebugOptions.NONE,
    shortcutConfig: ConsentShortcutConfig = ConsentShortcutConfig.OFF,
    firstOpenMark: FirstOpenMark = FirstOpenMark(EmptyPreferencesForTests(), RecordingAppLogger()),
    interDisplayCounter: InterDisplayCounter = InterDisplayCounter(EmptyPreferencesForTests(), RecordingAppLogger()),
    maxConfig: MaxConfig = MaxConfig.NONE,
    maxPlatform: MaxPlatform = FakeMaxPlatform(),
): AdsController = AdsController(
    context = context,
    unitIds = unitIds,
    consentDebug = consentDebug,
    debugOptions = debugOptions,
    flags = flags,
    connectivity = connectivity,
    shortcutConfig = shortcutConfig,
    metrics = metrics,
    logger = logger,
    firstOpenMark = firstOpenMark,
    interDisplayCounter = interDisplayCounter,
    attribution = attribution,
    funnel = funnel,
    maxConfig = maxConfig,
    maxPlatform = maxPlatform,
)

/** Counts the calls of :core:ads to AppsFlyer's start-up. */
internal class FakeAppsFlyerStartup : AppsFlyerStartup {
    var prepares = 0
        private set
    var starts = 0
        private set

    override fun prepare() {
        prepares++
    }

    override fun start() {
        starts++
    }
}

private object AlwaysConnected : InternetConnectivity {
    override val connected: Flow<Boolean> = flowOf(true)
}

/** A preferences store that is always empty and drops every write. */
internal class EmptyPreferencesForTests : DataStore<Preferences> {
    override val data: Flow<Preferences> = flowOf(emptyPreferences())

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        transform(emptyPreferences())
}
