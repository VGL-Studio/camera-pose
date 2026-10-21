package com.vglstudio.camera.pose.line.guide.core.poses.internal

import com.vglstudio.camera.pose.line.guide.core.assets.AssetPath
import com.vglstudio.camera.pose.line.guide.core.config.RemoteSettings
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.core.poses.CatalogState
import com.vglstudio.camera.pose.line.guide.core.poses.LoadFailureReason
import com.vglstudio.camera.pose.line.guide.core.poses.PoseCatalogRepository
import com.vglstudio.camera.pose.line.guide.core.poses.PoseContent
import com.vglstudio.camera.pose.line.guide.core.poses.PoseFileResult
import com.vglstudio.camera.pose.line.guide.core.poses.PoseLoadFailure
import com.vglstudio.camera.pose.line.guide.core.poses.StarterSetResult
import com.vglstudio.camera.pose.line.guide.core.poses.model.Pose
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseFile
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

private const val FAILURE_BUFFER = 16

internal class DefaultPoseCatalogRepository(
    private val loader: TimedAssetLoader,
    private val settings: RemoteSettings,
    private val parser: CatalogParser,
    private val logger: AppLogger,
    private val ioDispatcher: CoroutineDispatcher,
) : PoseCatalogRepository {
    private val state = MutableStateFlow<CatalogState>(CatalogState.NotLoaded)
    override val catalog: StateFlow<CatalogState> = state

    private val failureEvents = MutableSharedFlow<PoseLoadFailure>(
        extraBufferCapacity = FAILURE_BUFFER,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val failures: SharedFlow<PoseLoadFailure> = failureEvents.asSharedFlow()

    private val catalogLock = Mutex()
    private var loadedPath: String? = null

    // Starter files that finished in this run of the app, so a retry only loads what is missing.
    private val starterLock = Any()
    private val completedStarterFiles = mutableSetOf<String>()

    override suspend fun loadCatalog(): CatalogState = catalogLock.withLock {
        val path = currentCatalogPath()
        val before = state.value
        if (before is CatalogState.Ready && loadedPath == path.value) return@withLock before
        state.value = CatalogState.Loading
        try {
            val result = readCatalog(path)
            if (result is CatalogState.Ready) loadedPath = path.value
            state.value = result
            result
        } catch (e: CancellationException) {
            state.value = before
            throw e
        }
    }

    override suspend fun loadFile(pose: Pose, file: PoseFile): PoseFileResult = when (file) {
        PoseFile.THUMBNAIL -> loadTracked(pose.thumbnailPath, PoseContent.THUMBNAIL)
        PoseFile.IMAGE -> loadTracked(pose.imagePath, PoseContent.IMAGE)
        PoseFile.OUTLINE -> loadTracked(pose.outlinePath, PoseContent.OUTLINE)
    }

    override suspend fun loadStarterSet(onProgress: (done: Int, total: Int) -> Unit): StarterSetResult {
        val catalog = when (val loaded = loadCatalog()) {
            is CatalogState.Ready -> loaded.catalog
            is CatalogState.Failed -> return StarterSetResult.Incomplete(0, loaded.failure)
            else -> return StarterSetResult.Incomplete(0, PoseLoadFailure(PoseContent.CATALOG, LoadFailureReason.OTHER))
        }
        val files = catalog.poses.filter { it.starter }.flatMap {
            listOf(it.thumbnailPath to PoseContent.THUMBNAIL, it.outlinePath to PoseContent.OUTLINE)
        }
        var done = synchronized(starterLock) { files.count { it.first.value in completedStarterFiles } }
        onProgress(done, files.size)
        val failed = mutableListOf<Pair<Int, PoseLoadFailure>>()
        val semaphore = Semaphore(STARTER_PARALLELISM)
        coroutineScope {
            files.withIndex()
                .filter { (_, file) -> synchronized(starterLock) { file.first.value !in completedStarterFiles } }
                .map { (index, file) ->
                    async {
                        val result = semaphore.withPermit { loadTracked(file.first, file.second) }
                        synchronized(starterLock) {
                            when (result) {
                                is PoseFileResult.Ready -> {
                                    completedStarterFiles += file.first.value
                                    done++
                                    onProgress(done, files.size)
                                }
                                is PoseFileResult.Failed -> failed += index to result.failure
                            }
                        }
                    }
                }
                .awaitAll()
        }
        val firstFailure = failed.minByOrNull { it.first }?.second
        if (firstFailure == null) {
            logger.info(LOG_TAG, "Starter set is complete: ${files.size} files")
            return StarterSetResult.Complete
        }
        return StarterSetResult.Incomplete(failed.size, firstFailure)
    }

    private suspend fun currentCatalogPath(): AssetPath {
        val configured = settings.string(CATALOG_PATH_KEY, DEFAULT_CATALOG_PATH).first()
        return try {
            AssetPath(configured)
        } catch (e: IllegalArgumentException) {
            logger.warn(LOG_TAG, "The catalog path from Remote Config is not valid, using the default", e)
            AssetPath(DEFAULT_CATALOG_PATH)
        }
    }

    private suspend fun readCatalog(path: AssetPath): CatalogState {
        val file = when (val loaded = loadTracked(path, PoseContent.CATALOG)) {
            is PoseFileResult.Ready -> loaded.file
            is PoseFileResult.Failed -> return CatalogState.Failed(loaded.failure)
        }
        val text = try {
            withContext(ioDispatcher) { file.readText() }
        } catch (e: IOException) {
            logger.warn(LOG_TAG, "Could not read the catalog file", e)
            return fail(PoseLoadFailure(PoseContent.CATALOG, LoadFailureReason.OTHER))
        }
        return when (val parsed = parser.parse(text)) {
            is CatalogParseResult.Valid -> {
                val catalog = parsed.catalog
                logger.info(
                    LOG_TAG,
                    "Catalog is ready: ${catalog.categories.size} categories, ${catalog.poses.size} poses, " +
                        "${parsed.droppedPoses} dropped",
                )
                CatalogState.Ready(catalog)
            }
            is CatalogParseResult.Invalid, is CatalogParseResult.UnsupportedSchema ->
                fail(PoseLoadFailure(PoseContent.CATALOG, LoadFailureReason.OTHER))
        }
    }

    /** Loads one file; a failed load is logged and sent to [failures]. */
    private suspend fun loadTracked(path: AssetPath, content: PoseContent): PoseFileResult {
        val result = loader.load(path, content)
        if (result is PoseFileResult.Failed) fail(result.failure)
        return result
    }

    private fun fail(failure: PoseLoadFailure): CatalogState.Failed {
        logger.warn(LOG_TAG, "Could not load ${failure.content.analyticsValue}: ${failure.reason.analyticsValue}")
        failureEvents.tryEmit(failure)
        return CatalogState.Failed(failure)
    }
}
