package com.vglstudio.camera.pose.line.guide.feature.camera.di

import com.vglstudio.camera.pose.line.guide.feature.camera.data.DataStorePoseOverlaySettingsRepository
import com.vglstudio.camera.pose.line.guide.feature.camera.data.PoseOverlaySettingsRepository
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.BitmapPoseImageDecoder
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.PoseImageDecoder
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Bindings of the pose part of the camera (plan T-014; kept apart from CameraModule). */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class PoseModule {
    @Binds
    abstract fun bindOverlaySettingsRepository(
        impl: DataStorePoseOverlaySettingsRepository,
    ): PoseOverlaySettingsRepository

    @Binds
    abstract fun bindImageDecoder(impl: BitmapPoseImageDecoder): PoseImageDecoder
}
