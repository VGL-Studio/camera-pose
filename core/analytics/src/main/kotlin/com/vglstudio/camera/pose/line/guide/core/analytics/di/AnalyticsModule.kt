package com.vglstudio.camera.pose.line.guide.core.analytics.di

import com.vglstudio.camera.pose.line.guide.core.analytics.ActivityCountForeground
import com.vglstudio.camera.pose.line.guide.core.analytics.AdFunnel
import com.vglstudio.camera.pose.line.guide.core.analytics.AdMetrics
import com.vglstudio.camera.pose.line.guide.core.analytics.Analytics
import com.vglstudio.camera.pose.line.guide.core.analytics.AndroidCountrySource
import com.vglstudio.camera.pose.line.guide.core.analytics.AppForeground
import com.vglstudio.camera.pose.line.guide.core.analytics.AppsFlyerSink
import com.vglstudio.camera.pose.line.guide.core.analytics.AppsFlyerStartup
import com.vglstudio.camera.pose.line.guide.core.analytics.AppsFlyerTracker
import com.vglstudio.camera.pose.line.guide.core.analytics.AttributionConsent
import com.vglstudio.camera.pose.line.guide.core.analytics.CountrySource
import com.vglstudio.camera.pose.line.guide.core.analytics.FanOutAdFunnel
import com.vglstudio.camera.pose.line.guide.core.analytics.FanOutAdMetrics
import com.vglstudio.camera.pose.line.guide.core.analytics.FanOutAppsFlyerStartup
import com.vglstudio.camera.pose.line.guide.core.analytics.FirebaseAdMetrics
import com.vglstudio.camera.pose.line.guide.core.analytics.FirebaseAnalyticsLogger
import com.vglstudio.camera.pose.line.guide.core.analytics.NoOpAppsFlyerSink
import com.vglstudio.camera.pose.line.guide.core.analytics.TenjinLibSink
import com.vglstudio.camera.pose.line.guide.core.analytics.TenjinSink
import com.vglstudio.camera.pose.line.guide.core.analytics.TenjinTracker
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * The analytics of the app; [com.vglstudio.camera.pose.line.guide.core.analytics.AppsFlyerConfig] and
 * [com.vglstudio.camera.pose.line.guide.core.analytics.TenjinConfig] come from :app (ADR-0027, ADR-1010).
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class AnalyticsModule {
    @Binds
    abstract fun bindAnalytics(impl: FirebaseAnalyticsLogger): Analytics

    @Binds
    abstract fun bindAppsFlyerSink(impl: NoOpAppsFlyerSink): AppsFlyerSink

    @Binds
    abstract fun bindCountrySource(impl: AndroidCountrySource): CountrySource

    @Binds
    abstract fun bindTenjinSink(impl: TenjinLibSink): TenjinSink

    @Binds
    abstract fun bindAppForeground(impl: ActivityCountForeground): AppForeground

    @Binds
    abstract fun bindAttributionConsent(impl: TenjinTracker): AttributionConsent
}

/**
 * Every ad metric goes to Firebase, then to AppsFlyer (section 8.3), then to Tenjin (ADR-1010); a failing one never
 * stops the others. The start-up of the attribution SDKs reaches AppsFlyer, then Tenjin, in the same way; the ad
 * funnel reaches AppsFlyer, then Tenjin, in the same way.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object AdMetricsModule {
    @Provides
    fun adMetrics(
        firebase: FirebaseAdMetrics,
        appsFlyer: AppsFlyerTracker,
        tenjin: TenjinTracker,
        logger: AppLogger,
    ): AdMetrics = FanOutAdMetrics(listOf(firebase, appsFlyer, tenjin), logger)

    @Provides
    fun adFunnel(appsFlyer: AppsFlyerTracker, tenjin: TenjinTracker, logger: AppLogger): AdFunnel =
        FanOutAdFunnel(listOf(appsFlyer, tenjin), logger)

    @Provides
    fun attributionStartup(appsFlyer: AppsFlyerTracker, tenjin: TenjinTracker, logger: AppLogger): AppsFlyerStartup =
        FanOutAppsFlyerStartup(listOf(appsFlyer, tenjin), logger)
}
