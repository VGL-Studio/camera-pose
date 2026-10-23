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

    @Provides
    @IntoSet
    @LocallyEnabledFlags
    fun networkDialog(): FeatureFlag = FeatureFlag.NETWORK_DIALOG

    @Provides
    @IntoSet
    @LocallyEnabledFlags
    fun camera(): FeatureFlag = FeatureFlag.CAMERA

    @Provides
    @IntoSet
    @LocallyEnabledFlags
    fun onboarding(): FeatureFlag = FeatureFlag.ONBOARDING

    @Provides
    @IntoSet
    @LocallyEnabledFlags
    fun poses(): FeatureFlag = FeatureFlag.POSES

    @Provides
    @IntoSet
    @LocallyEnabledFlags
    fun settings(): FeatureFlag = FeatureFlag.SETTINGS

    @Provides
    @IntoSet
    @LocallyEnabledFlags
    fun poseMatching(): FeatureFlag = FeatureFlag.POSE_MATCHING
}
