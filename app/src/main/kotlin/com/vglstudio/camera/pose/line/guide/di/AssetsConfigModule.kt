package com.vglstudio.camera.pose.line.guide.di

import com.vglstudio.camera.pose.line.guide.BuildConfig
import com.vglstudio.camera.pose.line.guide.core.assets.AssetsConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** The R2 address of the build flavor, set in app/build.gradle.kts (ADR-0007, ADR-0015). */
@Module
@InstallIn(SingletonComponent::class)
object AssetsConfigModule {
    @Provides
    fun assetsConfig(): AssetsConfig = AssetsConfig(BuildConfig.ASSETS_BASE_URL)
}
