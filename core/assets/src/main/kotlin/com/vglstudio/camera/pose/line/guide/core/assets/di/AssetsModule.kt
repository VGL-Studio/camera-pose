package com.vglstudio.camera.pose.line.guide.core.assets.di

import android.content.Context
import android.util.Log
import com.vglstudio.camera.pose.line.guide.core.assets.AssetsConfig
import com.vglstudio.camera.pose.line.guide.core.assets.DefaultRemoteAssets
import com.vglstudio.camera.pose.line.guide.core.assets.HttpAssetDownloader
import com.vglstudio.camera.pose.line.guide.core.assets.RemoteAssets
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers

/** :app provides [AssetsConfig] for its build flavor. */
@Module
@InstallIn(SingletonComponent::class)
internal object AssetsModule {
    @Provides
    @Singleton
    fun remoteAssets(@ApplicationContext context: Context, config: AssetsConfig): RemoteAssets = DefaultRemoteAssets(
        config = config,
        // Not the cache dir: the system may clear that, and files must stay available offline.
        // Not backed up: they can always be downloaded again.
        cacheDir = File(context.noBackupFilesDir, "r2-assets"),
        downloader = HttpAssetDownloader(),
        ioDispatcher = Dispatchers.IO,
        onDownloadFailed = { path, error -> Log.w("Assets", "Could not download ${path.value}", error) },
    )
}
