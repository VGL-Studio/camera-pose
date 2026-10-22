package com.vglstudio.camera.pose.line.guide.feature.camera.di

import com.vglstudio.camera.pose.line.guide.feature.camera.camera.CameraController
import com.vglstudio.camera.pose.line.guide.feature.camera.camera.CameraXController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

/** One camera per camera view model, so it lives and dies with the screen's view model. */
@Module
@InstallIn(ViewModelComponent::class)
internal abstract class CameraControllerModule {
    @Binds
    @ViewModelScoped
    abstract fun bindCameraController(impl: CameraXController): CameraController
}
