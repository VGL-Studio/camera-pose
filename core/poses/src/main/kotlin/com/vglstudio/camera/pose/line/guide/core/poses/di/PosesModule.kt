package com.vglstudio.camera.pose.line.guide.core.poses.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.vglstudio.camera.pose.line.guide.core.assets.RemoteAssets
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlags
import com.vglstudio.camera.pose.line.guide.core.config.RemoteSettings
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.core.poses.PoseCatalogRepository
import com.vglstudio.camera.pose.line.guide.core.poses.PoseHistoryRepository
import com.vglstudio.camera.pose.line.guide.core.poses.PoseLibraryRepository
import com.vglstudio.camera.pose.line.guide.core.poses.PremiumPoseRepository
import com.vglstudio.camera.pose.line.guide.core.poses.internal.CatalogParser
import com.vglstudio.camera.pose.line.guide.core.poses.internal.DataStorePoseHistoryRepository
import com.vglstudio.camera.pose.line.guide.core.poses.internal.DataStorePremiumPoseRepository
import com.vglstudio.camera.pose.line.guide.core.poses.internal.DefaultPoseCatalogRepository
import com.vglstudio.camera.pose.line.guide.core.poses.internal.DefaultPoseLibraryRepository
import com.vglstudio.camera.pose.line.guide.core.poses.internal.PosesClock
import com.vglstudio.camera.pose.line.guide.core.poses.internal.TimedAssetLoader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class PosesIoDispatcher

/** Scope of the downloads that go on after a caller stopped waiting. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class PosesScope

@Module
@InstallIn(SingletonComponent::class)
internal object PosesModule {
    @Provides
    @PosesIoDispatcher
    fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    @PosesScope
    fun scope(@PosesIoDispatcher dispatcher: CoroutineDispatcher): CoroutineScope =
        CoroutineScope(SupervisorJob() + dispatcher)

    @Provides
    fun clock(): PosesClock = PosesClock { System.currentTimeMillis() }

    @Provides
    @Singleton
    fun catalogRepository(
        assets: RemoteAssets,
        @PosesScope scope: CoroutineScope,
        settings: RemoteSettings,
        logger: AppLogger,
        @PosesIoDispatcher dispatcher: CoroutineDispatcher,
    ): PoseCatalogRepository = DefaultPoseCatalogRepository(
        loader = TimedAssetLoader(assets, scope),
        settings = settings,
        parser = CatalogParser(logger),
        logger = logger,
        ioDispatcher = dispatcher,
    )

    @Provides
    @Singleton
    fun historyRepository(
        dataStore: DataStore<Preferences>,
        clock: PosesClock,
        logger: AppLogger,
    ): PoseHistoryRepository = DataStorePoseHistoryRepository(dataStore, clock, logger)

    @Provides
    @Singleton
    fun premiumRepository(
        dataStore: DataStore<Preferences>,
        flags: FeatureFlags,
        clock: PosesClock,
        logger: AppLogger,
    ): PremiumPoseRepository = DataStorePremiumPoseRepository(dataStore, flags, clock, logger)

    @Provides
    @Singleton
    fun libraryRepository(
        catalog: PoseCatalogRepository,
        history: PoseHistoryRepository,
        logger: AppLogger,
    ): PoseLibraryRepository = DefaultPoseLibraryRepository(catalog, history, logger)
}
