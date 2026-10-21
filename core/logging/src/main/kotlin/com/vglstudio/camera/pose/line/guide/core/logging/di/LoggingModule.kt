package com.vglstudio.camera.pose.line.guide.core.logging.di

import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.core.logging.DefaultAppLogger
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class LoggingModule {
    @Binds
    abstract fun bindAppLogger(impl: DefaultAppLogger): AppLogger
}
