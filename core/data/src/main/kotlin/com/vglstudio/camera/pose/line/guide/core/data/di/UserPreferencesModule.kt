package com.vglstudio.camera.pose.line.guide.core.data.di

import com.vglstudio.camera.pose.line.guide.core.data.DataStoreUserPreferences
import com.vglstudio.camera.pose.line.guide.core.data.UserPreferences
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class UserPreferencesModule {
    @Binds
    abstract fun bindUserPreferences(impl: DataStoreUserPreferences): UserPreferences
}
