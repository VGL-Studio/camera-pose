package com.vglstudio.camera.pose.line.guide.core.config.di

import com.vglstudio.camera.pose.line.guide.core.config.AppLinks
import com.vglstudio.camera.pose.line.guide.core.config.DefaultAppLinks
import com.vglstudio.camera.pose.line.guide.core.config.DefaultFeatureFlags
import com.vglstudio.camera.pose.line.guide.core.config.DefaultRemoteSettings
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlag
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlags
import com.vglstudio.camera.pose.line.guide.core.config.FirebaseRemoteConfigRefresher
import com.vglstudio.camera.pose.line.guide.core.config.FirebaseRemoteFlagSource
import com.vglstudio.camera.pose.line.guide.core.config.LocallyEnabledFlags
import com.vglstudio.camera.pose.line.guide.core.config.RemoteConfigRefresher
import com.vglstudio.camera.pose.line.guide.core.config.RemoteFlagSource
import com.vglstudio.camera.pose.line.guide.core.config.RemoteSettings
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ConfigModule {
    @Binds
    abstract fun bindFeatureFlags(impl: DefaultFeatureFlags): FeatureFlags

    @Binds
    abstract fun bindRemoteSettings(impl: DefaultRemoteSettings): RemoteSettings

    @Binds
    abstract fun bindAppLinks(impl: DefaultAppLinks): AppLinks

    @Binds
    abstract fun bindRemoteFlagSource(impl: FirebaseRemoteFlagSource): RemoteFlagSource

    @Binds
    abstract fun bindRemoteConfigRefresher(impl: FirebaseRemoteConfigRefresher): RemoteConfigRefresher

    /** Declares the set so it exists (empty) in builds that enable nothing locally, such as production. */
    @Multibinds
    @LocallyEnabledFlags
    abstract fun locallyEnabledFlags(): Set<FeatureFlag>
}
