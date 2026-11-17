package com.vglstudio.camera.pose.line.guide.di

import com.vglstudio.camera.pose.line.guide.BuildConfig
import com.vglstudio.camera.pose.line.guide.core.analytics.AppsFlyerConfig
import com.vglstudio.camera.pose.line.guide.core.analytics.TenjinConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * AppsFlyer is not in this app (spec ads, question 3): no SDK, no key, in every build. With an empty key the tracker of
 * :core:analytics never starts and nothing is sent (ADR-0027, decision 2). Tenjin (ADR-1010) takes the key of the
 * build: `tenjin.sdkKey` of local.properties for staging, `-PtenjinSdkKey` for production; none, and Tenjin never
 * starts.
 */
@Module
@InstallIn(SingletonComponent::class)
object AnalyticsConfigModule {
    @Provides
    fun appsFlyerConfig(): AppsFlyerConfig = AppsFlyerConfig("")

    @Provides
    fun tenjinConfig(): TenjinConfig = TenjinConfig(BuildConfig.TENJIN_SDK_KEY)
}
