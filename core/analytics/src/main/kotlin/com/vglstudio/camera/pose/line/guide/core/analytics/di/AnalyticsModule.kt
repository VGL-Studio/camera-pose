package com.vglstudio.camera.pose.line.guide.core.analytics.di

import com.vglstudio.camera.pose.line.guide.core.analytics.AdFunnel
import com.vglstudio.camera.pose.line.guide.core.analytics.AdMetrics
import com.vglstudio.camera.pose.line.guide.core.analytics.Analytics
import com.vglstudio.camera.pose.line.guide.core.analytics.AndroidCountrySource
import com.vglstudio.camera.pose.line.guide.core.analytics.AppsFlyerLibSink
import com.vglstudio.camera.pose.line.guide.core.analytics.AppsFlyerSink
import com.vglstudio.camera.pose.line.guide.core.analytics.AppsFlyerStartup
import com.vglstudio.camera.pose.line.guide.core.analytics.AppsFlyerTracker
import com.vglstudio.camera.pose.line.guide.core.analytics.CountrySource
import com.vglstudio.camera.pose.line.guide.core.analytics.FanOutAdMetrics
import com.vglstudio.camera.pose.line.guide.core.analytics.FirebaseAdMetrics
import com.vglstudio.camera.pose.line.guide.core.analytics.FirebaseAnalyticsLogger
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** The analytics of the app; [com.vglstudio.camera.pose.line.guide.core.analytics.AppsFlyerConfig] comes from :app (ADR-0027). */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class AnalyticsModule {
    @Binds
    abstract fun bindAnalytics(impl: FirebaseAnalyticsLogger): Analytics

    @Binds
    abstract fun bindAdFunnel(impl: AppsFlyerTracker): AdFunnel

    @Binds
    abstract fun bindAppsFlyerStartup(impl: AppsFlyerTracker): AppsFlyerStartup

    @Binds
    abstract fun bindAppsFlyerSink(impl: AppsFlyerLibSink): AppsFlyerSink

    @Binds
    abstract fun bindCountrySource(impl: AndroidCountrySource): CountrySource
}

/** Every ad metric goes to Firebase, then to AppsFlyer (section 8.3); a failing one never stops the other. */
@Module
@InstallIn(SingletonComponent::class)
internal object AdMetricsModule {
    @Provides
    fun adMetrics(firebase: FirebaseAdMetrics, appsFlyer: AppsFlyerTracker, logger: AppLogger): AdMetrics =
        FanOutAdMetrics(listOf(firebase, appsFlyer), logger)
}
