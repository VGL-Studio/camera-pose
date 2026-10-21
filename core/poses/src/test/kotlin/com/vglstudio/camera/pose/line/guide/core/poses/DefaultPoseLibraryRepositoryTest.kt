package com.vglstudio.camera.pose.line.guide.core.poses

import app.cash.turbine.test
import com.vglstudio.camera.pose.line.guide.core.assets.AssetFailureReason
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import com.vglstudio.camera.pose.line.guide.core.poses.internal.CatalogParser
import com.vglstudio.camera.pose.line.guide.core.poses.internal.DEFAULT_CATALOG_PATH
import com.vglstudio.camera.pose.line.guide.core.poses.internal.DataStorePoseHistoryRepository
import com.vglstudio.camera.pose.line.guide.core.poses.internal.DefaultPoseCatalogRepository
import com.vglstudio.camera.pose.line.guide.core.poses.internal.DefaultPoseLibraryRepository
import com.vglstudio.camera.pose.line.guide.core.poses.internal.TimedAssetLoader
import com.vglstudio.camera.pose.line.guide.core.poses.model.LibraryState
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DefaultPoseLibraryRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val logger = RecordingAppLogger()
    private lateinit var assets: FakeRemoteAssets
    private lateinit var catalog: DefaultPoseCatalogRepository
    private lateinit var history: DataStorePoseHistoryRepository
    private lateinit var library: DefaultPoseLibraryRepository

    private fun TestScope.setUp() {
        assets = FakeRemoteAssets(folder.root).apply { contents[DEFAULT_CATALOG_PATH] = Fixtures.text("valid.json") }
        val dispatcher = StandardTestDispatcher(testScheduler)
        catalog = DefaultPoseCatalogRepository(
            loader = TimedAssetLoader(assets, CoroutineScope(dispatcher + SupervisorJob())),
            settings = FakeRemoteSettings(),
            parser = CatalogParser(logger),
            logger = logger,
            ioDispatcher = dispatcher,
        )
        var now = 0L
        history = DataStorePoseHistoryRepository(InMemoryDataStore(), { ++now }, logger)
        library = DefaultPoseLibraryRepository(catalog, history, logger)
    }

    @Test
    fun library_catalogNotLoaded_startsLoading_thenReady() = runTest {
        setUp()

        library.library.test {
            assertEquals(LibraryState.Loading, awaitItem())
            val ready = awaitItem() as LibraryState.Ready
            assertEquals(6, ready.library.totalPoses)
            assertEquals(listOf("women", "men"), ready.library.categories.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun library_updatesWhenSavedOrRecentChange() = runTest {
        setUp()
        catalog.loadCatalog()

        library.library.test {
            val first = awaitItem() as LibraryState.Ready
            assertEquals("w01", first.library.posesFor(PoseFilter.All).first().id)

            history.setSaved("m02", true)
            val afterSave = awaitItem() as LibraryState.Ready
            assertEquals(listOf("m02"), afterSave.library.posesFor(PoseFilter.Saved).map { it.id })
            assertEquals("m02", afterSave.library.posesFor(PoseFilter.All).first().id)

            history.markUsed("w03")
            val afterUse = awaitItem() as LibraryState.Ready
            assertEquals(listOf("m02", "w03"), afterUse.library.posesFor(PoseFilter.All).take(2).map { it.id })
            assertEquals("w03", afterUse.library.lastUsedPose?.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun library_catalogFailed_emitsFailed_thenRetryEmitsReady() = runTest {
        setUp()
        assets.failures[DEFAULT_CATALOG_PATH] = AssetFailureReason.NETWORK

        library.library.test {
            assertEquals(LibraryState.Loading, awaitItem())
            val failed = awaitItem() as LibraryState.Failed
            assertEquals(PoseLoadFailure(PoseContent.CATALOG, LoadFailureReason.NETWORK), failed.failure)

            assets.failures.clear()
            library.retry()

            assertEquals(LibraryState.Loading, awaitItem())
            assertTrue(awaitItem() is LibraryState.Ready)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
