package com.vglstudio.camera.pose.line.guide.core.poses

import app.cash.turbine.test
import com.vglstudio.camera.pose.line.guide.core.assets.AssetFailureReason
import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import com.vglstudio.camera.pose.line.guide.core.poses.internal.CATALOG_PATH_KEY
import com.vglstudio.camera.pose.line.guide.core.poses.internal.CatalogParser
import com.vglstudio.camera.pose.line.guide.core.poses.internal.DEFAULT_CATALOG_PATH
import com.vglstudio.camera.pose.line.guide.core.poses.internal.DefaultPoseCatalogRepository
import com.vglstudio.camera.pose.line.guide.core.poses.internal.TimedAssetLoader
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DefaultPoseCatalogRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var assets: FakeRemoteAssets
    private val settings = FakeRemoteSettings()
    private val logger = RecordingAppLogger()

    private fun TestScope.repository(catalogText: String = Fixtures.text("valid.json")): DefaultPoseCatalogRepository {
        assets = FakeRemoteAssets(folder.root).apply { contents[DEFAULT_CATALOG_PATH] = catalogText }
        val dispatcher = StandardTestDispatcher(testScheduler)
        return DefaultPoseCatalogRepository(
            loader = TimedAssetLoader(assets, CoroutineScope(dispatcher + SupervisorJob())),
            settings = settings,
            parser = CatalogParser(logger),
            logger = logger,
            ioDispatcher = dispatcher,
        )
    }

    // Starter poses of valid.json: w01, w02 and m01. Each has a thumbnail and an outline.
    private val w01Thumbnail = "poses/img/w01/preview_540-1a2b3c01.webp"
    private val w02Thumbnail = "poses/img/w02/preview_540-1a2b3c02.webp"
    private val w02Outline = "poses/img/w02/outline-1a2b3c02.webp"
    private val starterFiles = setOf(
        "poses/img/w01/preview_540-1a2b3c01.webp",
        "poses/img/w01/outline-1a2b3c01.webp",
        "poses/img/w02/preview_540-1a2b3c02.webp",
        "poses/img/w02/outline-1a2b3c02.webp",
        "poses/img/m01/preview_540-1a2b3c04.webp",
        "poses/img/m01/outline-1a2b3c04.webp",
    )

    @Test
    fun loadCatalog_usesPathFromRemoteSettings() = runTest {
        val repository = repository()
        val remotePath = "poses/catalog/v1/catalog-12345678.json"
        assets.contents[remotePath] = Fixtures.text("valid.json")
        settings.strings.value = mapOf(CATALOG_PATH_KEY to remotePath)

        val state = repository.loadCatalog()

        assertTrue(state is CatalogState.Ready)
        assertEquals(listOf(remotePath), assets.calls)
    }

    @Test
    fun invalidRemotePath_usesDefaultPath_andLogsWarning() = runTest {
        val repository = repository()
        settings.strings.value = mapOf(CATALOG_PATH_KEY to "catalog.json")

        val state = repository.loadCatalog()

        assertTrue(state is CatalogState.Ready)
        assertEquals(listOf(DEFAULT_CATALOG_PATH), assets.calls)
        assertTrue(logger.entries.any { it.level == LogLevel.WARN && it.tag == "Poses" })
    }

    @Test
    fun loadCatalog_ready_updatesStateToReady() = runTest {
        val repository = repository()
        assertEquals(CatalogState.NotLoaded, repository.catalog.value)

        repository.loadCatalog()

        val state = repository.catalog.value as CatalogState.Ready
        assertEquals(6, state.catalog.poses.size)
    }

    @Test
    fun loadCatalog_unavailableNetwork_failsWithCatalogNetwork_andEmitsFailure() = runTest {
        val repository = repository()
        assets.failures[DEFAULT_CATALOG_PATH] = AssetFailureReason.NETWORK
        val expected = PoseLoadFailure(PoseContent.CATALOG, LoadFailureReason.NETWORK)

        repository.failures.test {
            assertEquals(CatalogState.Failed(expected), repository.loadCatalog())
            assertEquals(expected, awaitItem())
        }
        assertEquals(CatalogState.Failed(expected), repository.catalog.value)
    }

    @Test
    fun loadCatalog_timeout_failsWithTimeout() = runTest {
        val repository = repository()
        assets.delays[DEFAULT_CATALOG_PATH] = 60_000

        val state = repository.loadCatalog()

        assertEquals(CatalogState.Failed(PoseLoadFailure(PoseContent.CATALOG, LoadFailureReason.TIMEOUT)), state)
        assertEquals(15_000L, currentTime)
    }

    @Test
    fun loadCatalog_malformed_failsWithOther() = runTest {
        val repository = repository(Fixtures.text("malformed.json"))
        assertEquals(
            CatalogState.Failed(PoseLoadFailure(PoseContent.CATALOG, LoadFailureReason.OTHER)),
            repository.loadCatalog(),
        )
    }

    @Test
    fun loadCatalog_newerSchema_failsWithOther() = runTest {
        val repository = repository(Fixtures.text("newer-schema.json"))
        assertEquals(
            CatalogState.Failed(PoseLoadFailure(PoseContent.CATALOG, LoadFailureReason.OTHER)),
            repository.loadCatalog(),
        )
    }

    @Test
    fun loadCatalog_whenReady_doesNotLoadAgain() = runTest {
        val repository = repository()
        repository.loadCatalog()
        repository.loadCatalog()
        assertEquals(1, assets.calls.size)
    }

    @Test
    fun loadCatalog_concurrentCalls_loadOnce() = runTest {
        val repository = repository()
        assets.delays[DEFAULT_CATALOG_PATH] = 1_000

        val states = List(3) { async { repository.loadCatalog() } }.awaitAll()

        assertTrue(states.all { it is CatalogState.Ready })
        assertEquals(1, assets.calls.size)
    }

    @Test
    fun loadCatalog_afterFailure_loadsAgain() = runTest {
        val repository = repository()
        assets.failures[DEFAULT_CATALOG_PATH] = AssetFailureReason.NETWORK
        assertTrue(repository.loadCatalog() is CatalogState.Failed)

        assets.failures.clear()
        assertTrue(repository.loadCatalog() is CatalogState.Ready)
        assertEquals(2, assets.calls.size)
    }

    @Test
    fun loadFile_usesCatalogPathOfEachFileKind() = runTest {
        val repository = repository()
        val pose = (repository.loadCatalog() as CatalogState.Ready).catalog.poses.first { it.id == "w01" }
        assets.calls.clear()

        val results = PoseFile.entries.map { repository.loadFile(pose, it) }

        assertTrue(results.all { it is PoseFileResult.Ready })
        assertEquals(
            listOf(
                "poses/img/w01/preview_540-1a2b3c01.webp",
                "poses/img/w01/preview_1200-1a2b3c01.webp",
                "poses/img/w01/outline-1a2b3c01.webp",
            ),
            assets.calls,
        )
    }

    @Test
    fun loadFile_failure_emitsContentAndReason() = runTest {
        val repository = repository()
        val pose = (repository.loadCatalog() as CatalogState.Ready).catalog.poses.first { it.id == "w01" }
        assets.failures["poses/img/w01/preview_1200-1a2b3c01.webp"] = AssetFailureReason.NOT_FOUND
        val expected = PoseLoadFailure(PoseContent.IMAGE, LoadFailureReason.NOT_FOUND)

        repository.failures.test {
            assertEquals(PoseFileResult.Failed(expected), repository.loadFile(pose, PoseFile.IMAGE))
            assertEquals(expected, awaitItem())
        }
    }

    @Test
    fun starterSet_loadsThumbnailAndOutlineOfStarterPosesOnly() = runTest {
        val repository = repository()

        val result = repository.loadStarterSet { _, _ -> }

        assertEquals(StarterSetResult.Complete, result)
        assertEquals(starterFiles, assets.calls.filter { it != DEFAULT_CATALOG_PATH }.toSet())
    }

    @Test
    fun starterSet_reportsProgressAfterEachFile_neverDecreasing() = runTest {
        val repository = repository()
        val progress = mutableListOf<Pair<Int, Int>>()

        repository.loadStarterSet { done, total -> progress += done to total }

        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6), progress.map { it.first })
        assertTrue(progress.all { it.second == 6 })
    }

    @Test
    fun starterSet_retryAfterFailure_loadsOnlyMissingFiles_andProgressStartsFromDone() = runTest {
        val repository = repository()
        assets.failures[w02Thumbnail] = AssetFailureReason.NETWORK
        val first = mutableListOf<Pair<Int, Int>>()

        val failed = repository.loadStarterSet { done, total -> first += done to total }

        assertEquals(
            StarterSetResult.Incomplete(1, PoseLoadFailure(PoseContent.THUMBNAIL, LoadFailureReason.NETWORK)),
            failed,
        )
        assertEquals(5 to 6, first.last())

        assets.failures.clear()
        assets.calls.clear()
        val second = mutableListOf<Pair<Int, Int>>()
        val retried = repository.loadStarterSet { done, total -> second += done to total }

        assertEquals(StarterSetResult.Complete, retried)
        assertEquals(5 to 6, second.first())
        assertEquals(6 to 6, second.last())
        assertEquals(listOf(w02Thumbnail), assets.calls)
    }

    @Test
    fun starterSet_fileWithoutAnswer_failsAfter15s_othersComplete() = runTest {
        val repository = repository()
        assets.delays[w02Outline] = 60_000

        val result = repository.loadStarterSet { _, _ -> }

        assertEquals(
            StarterSetResult.Incomplete(1, PoseLoadFailure(PoseContent.OUTLINE, LoadFailureReason.TIMEOUT)),
            result,
        )
        assertEquals(15_000L, currentTime)
        assertTrue(assets.maxConcurrent <= 4)
    }

    @Test
    fun starterSet_catalogFails_returnsIncompleteWithCatalogFailure() = runTest {
        val repository = repository()
        assets.failures[DEFAULT_CATALOG_PATH] = AssetFailureReason.NETWORK

        val result = repository.loadStarterSet { _, _ -> }

        assertEquals(
            StarterSetResult.Incomplete(0, PoseLoadFailure(PoseContent.CATALOG, LoadFailureReason.NETWORK)),
            result,
        )
    }

    @Test
    fun logs_haveTagPoses_andNoPathsOrPoseIds() = runTest {
        val repository = repository()
        assets.failures[w01Thumbnail] = AssetFailureReason.NETWORK
        repository.loadStarterSet { _, _ -> }
        settings.strings.value = mapOf(CATALOG_PATH_KEY to "bad")
        repository.loadCatalog()

        assertTrue(logger.entries.isNotEmpty())
        assertTrue(logger.entries.all { it.tag == "Poses" })
        val ids = listOf("w01", "w02", "w03", "m01", "m02", "m03")
        assertTrue(logger.entries.none { entry -> "/" in entry.message || ids.any { it in entry.message } })
    }
}
