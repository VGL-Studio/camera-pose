package com.vglstudio.camera.pose.line.guide.core.ads.di

import android.content.Context
import com.vglstudio.camera.pose.line.guide.core.ads.AppLovinMaxPlatform
import com.vglstudio.camera.pose.line.guide.core.ads.MaxPlatform
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

/** The AppLovin MAX SDK behind [MaxPlatform] (plan T-068, ADR-0037). */
@Module
@InstallIn(SingletonComponent::class)
internal object MaxPlatformModule {
    @Provides
    fun maxPlatform(@ApplicationContext context: Context): MaxPlatform = AppLovinMaxPlatform(context)
}
