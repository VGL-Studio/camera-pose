package com.vglstudio.camera.pose.line.guide.di

import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlag
import com.vglstudio.camera.pose.line.guide.core.config.LocallyEnabledFlags
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Flags turned on in staging builds only, so the team can test features before they are approved.
 * Production builds have no such module: every flag there keeps its default (off) until remote config turns it on.
 */
@Module
@InstallIn(SingletonComponent::class)
object StagingFeatureFlagsModule {
    @Provides
    @IntoSet
    @LocallyEnabledFlags
    fun sample(): FeatureFlag = FeatureFlag.SAMPLE

    @Provides
    @IntoSet
    @LocallyEnabledFlags
    fun loading(): FeatureFlag = FeatureFlag.LOADING
}
