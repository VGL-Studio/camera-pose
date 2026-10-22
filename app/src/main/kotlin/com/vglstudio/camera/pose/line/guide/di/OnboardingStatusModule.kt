package com.vglstudio.camera.pose.line.guide.di

import com.vglstudio.camera.pose.line.guide.onboarding.DataStoreOnboardingStatus
import com.vglstudio.camera.pose.line.guide.onboarding.OnboardingStatus
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class OnboardingStatusModule {
    @Binds
    abstract fun bindOnboardingStatus(status: DataStoreOnboardingStatus): OnboardingStatus
}
