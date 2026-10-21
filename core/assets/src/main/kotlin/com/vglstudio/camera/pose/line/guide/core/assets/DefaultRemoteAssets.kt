package com.vglstudio.camera.pose.line.guide.core.assets

import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Keeps each file in [cacheDir] under its own path. A file is written to a temporary name first and renamed
 * when complete, so an interrupted download never leaves a partial file that looks ready.
 */
internal class DefaultRemoteAssets(
    private val config: AssetsConfig,
    private val cacheDir: File,
    private val downloader: AssetDownloader,
    private val ioDispatcher: CoroutineDispatcher,
    private val onDownloadFailed: (AssetPath, IOException) -> Unit = { _, _ -> },
) : RemoteAssets {
    private val locks = ConcurrentHashMap<AssetPath, Mutex>()

    override suspend fun load(path: AssetPath): AssetResult = withContext(ioDispatcher) {
        val file = File(cacheDir, path.value)
        if (file.isFile) return@withContext AssetResult.Ready(file)
        // One download per path at a time; whoever waited finds the file already there.
        locks.getOrPut(path) { Mutex() }.withLock {
            if (file.isFile) return@withLock AssetResult.Ready(file)
            val temp = File(cacheDir, "$TEMP_DIR/${UUID.randomUUID()}")
            try {
                temp.parentFile?.mkdirs()
                downloader.download(config.urlOf(path), temp)
                file.parentFile?.mkdirs()
                if (!temp.renameTo(file)) throw IOException("Could not move ${path.value} into the cache")
                AssetResult.Ready(file)
            } catch (e: IOException) {
                onDownloadFailed(path, e)
                AssetResult.Unavailable()
            } finally {
                temp.delete()
            }
        }
    }

    private companion object {
        // Starts with a dot, which no AssetPath segment does, so it never clashes with a cached file.
        const val TEMP_DIR = ".download"
    }
}
