package com.vglstudio.camera.pose.line.guide.feature.camera.di

import android.content.Context
import com.vglstudio.camera.pose.line.guide.core.network.NetworkStatus
import com.vglstudio.camera.pose.line.guide.feature.camera.CaptureClock
import com.vglstudio.camera.pose.line.guide.feature.camera.CountdownInterruptions
import com.vglstudio.camera.pose.line.guide.feature.camera.camera.CaptureFileStore
import com.vglstudio.camera.pose.line.guide.feature.camera.data.CameraSettingsRepository
import com.vglstudio.camera.pose.line.guide.feature.camera.data.DataStoreCameraSettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File

@Module
@InstallIn(SingletonComponent::class)
internal abstract class CameraModule {
    @Binds
    abstract fun bindSettingsRepository(impl: DataStoreCameraSettingsRepository): CameraSettingsRepository

    companion object {
        @Provides
        fun provideCaptureFileStore(@ApplicationContext context: Context): CaptureFileStore =
            CaptureFileStore(File(context.cacheDir, "captures"))

        @Provides
        fun provideCaptureClock(): CaptureClock = CaptureClock { System.currentTimeMillis() }

        // The countdown stops while the network dialog covers the screen (spec network, KB-1; plan T-013, option A).
        @Provides
        fun provideCountdownInterruptions(networkStatus: NetworkStatus): CountdownInterruptions =
            CountdownInterruptions { networkStatus.dialogShown }
    }
}
