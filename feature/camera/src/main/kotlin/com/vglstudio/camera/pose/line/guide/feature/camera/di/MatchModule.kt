package com.vglstudio.camera.pose.line.guide.feature.camera.di

import com.vglstudio.camera.pose.line.guide.feature.camera.match.MlKitPoseAnalyzer
import com.vglstudio.camera.pose.line.guide.feature.camera.match.PoseFrameSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Bindings of the pose matching of the camera (plan T-017; kept apart from CameraModule and PoseModule). */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class MatchModule {
    @Binds
    abstract fun bindFrameSource(impl: MlKitPoseAnalyzer): PoseFrameSource
}
