package com.vglstudio.camera.pose.line.guide.feature.loading.di

import android.os.SystemClock
import com.vglstudio.camera.pose.line.guide.feature.loading.LoadingClock
import com.vglstudio.camera.pose.line.guide.feature.loading.data.DefaultPreparationRepository
import com.vglstudio.camera.pose.line.guide.feature.loading.data.PreparationRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class LoadingModule {
    @Binds
    abstract fun bindPreparationRepository(impl: DefaultPreparationRepository): PreparationRepository

    companion object {
        @Provides
        fun loadingClock(): LoadingClock = LoadingClock { SystemClock.elapsedRealtime() }
    }
}
