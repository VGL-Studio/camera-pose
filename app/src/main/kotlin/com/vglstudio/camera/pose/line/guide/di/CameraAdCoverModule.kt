package com.vglstudio.camera.pose.line.guide.di

import com.vglstudio.camera.pose.line.guide.core.ads.AdsController
import com.vglstudio.camera.pose.line.guide.feature.camera.FullScreenAdCover
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * The camera does not run under a full-screen ad (spec camera, section 5; plan T-097): the feature reads the flag of
 * the ads controller through its own interface, never the controller (ADR-0045, section 4).
 */
@Module
@InstallIn(SingletonComponent::class)
object CameraAdCoverModule {
    @Provides
    fun fullScreenAdCover(ads: AdsController): FullScreenAdCover {
        // Skeleton for the tests written first (plan T-097, section 3.2): never covered.
        val never = MutableStateFlow(false)
        return FullScreenAdCover { never }
    }
}
