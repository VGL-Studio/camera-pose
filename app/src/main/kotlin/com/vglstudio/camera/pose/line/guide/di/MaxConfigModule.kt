package com.vglstudio.camera.pose.line.guide.di

import com.vglstudio.camera.pose.line.guide.BuildConfig
import com.vglstudio.camera.pose.line.guide.core.ads.MaxConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * The AppLovin MAX SDK key of the build flavor, set in app/build.gradle.kts (ADR-0037, plan T-068): `-PmaxSdkKey` in
 * production, `ads.maxSdkKey` of local.properties in staging; none on CI, so MAX never starts there. With it the MAX
 * ad units of the slots (plan T-071): `-PmaxAdUnits` in production, `ads.maxAdUnits` of local.properties in staging.
 */
@Module
@InstallIn(SingletonComponent::class)
object MaxConfigModule {
    @Provides
    fun maxConfig(): MaxConfig = MaxConfig(BuildConfig.MAX_SDK_KEY, BuildConfig.MAX_AD_UNITS)
}
