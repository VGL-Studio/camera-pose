package com.vglstudio.camera.pose.line.guide.feature.poses.di

import com.vglstudio.camera.pose.line.guide.feature.poses.data.BitmapPoseImageDecoder
import com.vglstudio.camera.pose.line.guide.feature.poses.data.DefaultPoseThumbnailRepository
import com.vglstudio.camera.pose.line.guide.feature.poses.data.DefaultPosesLibrarySource
import com.vglstudio.camera.pose.line.guide.feature.poses.data.PoseImageDecoder
import com.vglstudio.camera.pose.line.guide.feature.poses.data.PoseThumbnailRepository
import com.vglstudio.camera.pose.line.guide.feature.poses.data.PosesLibrarySource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** The dispatcher that decodes thumbnails: Dispatchers.IO in the app, a test dispatcher in tests. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class PosesIoDispatcher

/** Bindings of the pose library (named apart from PosesModule of :core:poses). */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class PosesFeatureModule {
    @Binds
    abstract fun bindLibrarySource(impl: DefaultPosesLibrarySource): PosesLibrarySource

    @Binds
    abstract fun bindThumbnailRepository(impl: DefaultPoseThumbnailRepository): PoseThumbnailRepository

    @Binds
    abstract fun bindImageDecoder(impl: BitmapPoseImageDecoder): PoseImageDecoder

    companion object {
        @Provides
        @PosesIoDispatcher
        fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
    }
}
