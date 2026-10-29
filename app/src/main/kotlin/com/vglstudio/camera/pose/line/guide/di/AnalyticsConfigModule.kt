package com.vglstudio.camera.pose.line.guide.di

import com.vglstudio.camera.pose.line.guide.core.analytics.AppsFlyerConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * AppsFlyer is not in this app (spec ads, question 3): no SDK, no key, in every build. With an empty key the tracker of
 * :core:analytics never starts and nothing is sent (ADR-0027, decision 2).
 */
@Module
@InstallIn(SingletonComponent::class)
object AnalyticsConfigModule {
    @Provides
    fun appsFlyerConfig(): AppsFlyerConfig = AppsFlyerConfig("")
}
