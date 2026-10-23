package com.vglstudio.camera.pose.line.guide.feature.camera.di

import android.content.Context
import com.vglstudio.camera.pose.line.guide.feature.camera.data.DataStoreSavedPhotosRepository
import com.vglstudio.camera.pose.line.guide.feature.camera.data.SavedPhotosRepository
import com.vglstudio.camera.pose.line.guide.feature.camera.photo.AndroidPhotoDecoder
import com.vglstudio.camera.pose.line.guide.feature.camera.photo.AndroidStoragePermission
import com.vglstudio.camera.pose.line.guide.feature.camera.photo.MediaStorePhotoStore
import com.vglstudio.camera.pose.line.guide.feature.camera.photo.PhotoDecoder
import com.vglstudio.camera.pose.line.guide.feature.camera.photo.PhotoStore
import com.vglstudio.camera.pose.line.guide.feature.camera.photo.StoragePermission
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

/** Bindings of the review screen (plan T-016; kept apart from CameraModule). */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class ReviewModule {
    @Binds
    abstract fun bindPhotoStore(impl: MediaStorePhotoStore): PhotoStore

    @Binds
    abstract fun bindPhotoDecoder(impl: AndroidPhotoDecoder): PhotoDecoder

    @Binds
    abstract fun bindSavedPhotosRepository(impl: DataStoreSavedPhotosRepository): SavedPhotosRepository

    companion object {
        @Provides
        fun provideStoragePermission(@ApplicationContext context: Context): StoragePermission =
            AndroidStoragePermission(context)
    }
}
