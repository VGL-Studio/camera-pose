package com.vglstudio.camera.pose.line.guide.feature.settings.di

import com.vglstudio.camera.pose.line.guide.feature.settings.data.AppVersionProvider
import com.vglstudio.camera.pose.line.guide.feature.settings.data.LanguageSettingsMode
import com.vglstudio.camera.pose.line.guide.feature.settings.data.PackageManagerAppVersionProvider
import com.vglstudio.camera.pose.line.guide.feature.settings.data.SdkLanguageSettingsMode
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SettingsModule {
    @Binds
    abstract fun bindAppVersionProvider(impl: PackageManagerAppVersionProvider): AppVersionProvider

    companion object {
        @Provides
        fun provideLanguageSettingsMode(): LanguageSettingsMode = SdkLanguageSettingsMode
    }
}
