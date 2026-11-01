package com.vglstudio.camera.pose.line.guide.di

import com.vglstudio.camera.pose.line.guide.core.ads.AdFreeStatus
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** The template sells no ad removal: never ad-free (KB-30). An app with a purchase provides its own here. */
@Module
@InstallIn(SingletonComponent::class)
object AdFreeModule {
    @Provides
    fun adFreeStatus(): AdFreeStatus = AdFreeStatus.NONE
}
