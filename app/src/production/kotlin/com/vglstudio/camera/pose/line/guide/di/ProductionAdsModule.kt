package com.vglstudio.camera.pose.line.guide.di

import com.vglstudio.camera.pose.line.guide.BuildConfig
import com.vglstudio.camera.pose.line.guide.core.ads.AdUnitIds
import com.vglstudio.camera.pose.line.guide.core.ads.ConfiguredAdUnitIds
import com.vglstudio.camera.pose.line.guide.core.ads.ConsentDebugOptions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Production uses the real ad units given when the release is built (ADR-0009, ADR-0013). */
@Module
@InstallIn(SingletonComponent::class)
object ProductionAdsModule {
    @Provides
    fun adUnitIds(): AdUnitIds = ConfiguredAdUnitIds(BuildConfig.AD_UNITS)

    @Provides
    fun consentDebugOptions(): ConsentDebugOptions = ConsentDebugOptions.NONE
}
