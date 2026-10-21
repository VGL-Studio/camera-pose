package com.vglstudio.camera.pose.line.guide.feature.sample.di

import com.vglstudio.camera.pose.line.guide.feature.sample.data.CounterRepository
import com.vglstudio.camera.pose.line.guide.feature.sample.data.DataStoreCounterRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SampleModule {
    @Binds
    abstract fun bindCounterRepository(impl: DataStoreCounterRepository): CounterRepository
}
