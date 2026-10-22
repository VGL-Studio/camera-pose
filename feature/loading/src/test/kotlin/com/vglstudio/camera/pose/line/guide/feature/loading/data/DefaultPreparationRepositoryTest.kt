package com.vglstudio.camera.pose.line.guide.feature.loading.data

import com.vglstudio.camera.pose.line.guide.core.config.RemoteConfigRefresher
import com.vglstudio.camera.pose.line.guide.core.poses.CatalogState
import com.vglstudio.camera.pose.line.guide.core.poses.LoadFailureReason
import com.vglstudio.camera.pose.line.guide.core.poses.PoseCatalogRepository
import com.vglstudio.camera.pose.line.guide.core.poses.PoseContent
import com.vglstudio.camera.pose.line.guide.core.poses.PoseFileResult
import com.vglstudio.camera.pose.line.guide.core.poses.PoseLoadFailure
import com.vglstudio.camera.pose.line.guide.core.poses.StarterSetResult
import com.vglstudio.camera.pose.line.guide.core.poses.model.Pose
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseCatalog
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseFile
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The repository has no RemoteAssets any more, so the three sample files test/v1/... can no longer be read. */
class DefaultPreparationRepositoryTest {
    private val refresher = FakeRefresher()
    private val poses = FakePoseCatalogRepository()
    private val repository = DefaultPreparationRepository(refresher, poses)

    @Test
    fun remoteConfigTask_usesRefresher() = runTest {
        assertTrue(repository.run(PreparationTask.REMOTE_CONFIG) { _, _ -> })
        refresher.result = false
        assertFalse(repository.run(PreparationTask.REMOTE_CONFIG) { _, _ -> })
        assertEquals(2, refresher.calls)
        assertEquals(0, poses.catalogCalls)
        assertEquals(0, poses.starterSetCalls)
    }

    @Test
    fun catalogReady_returnsTrue() = runTest {
        assertTrue(repository.run(PreparationTask.CATALOG) { _, _ -> })
        assertEquals(1, poses.catalogCalls)
        assertEquals(0, refresher.calls)
    }

    @Test
    fun catalogFailed_returnsFalse_forEveryReason() = runTest {
        LoadFailureReason.entries.forEach { reason ->
            poses.catalogResult = CatalogState.Failed(PoseLoadFailure(PoseContent.CATALOG, reason))
            assertFalse("$reason", repository.run(PreparationTask.CATALOG) { _, _ -> })
        }
    }

    @Test
    fun starterSetComplete_returnsTrue_andForwardsProgress() = runTest {
        poses.starterSetProgress = listOf(0 to 2, 1 to 2, 2 to 2)
        val received = mutableListOf<Pair<Int, Int>>()

        assertTrue(repository.run(PreparationTask.STARTER_SET) { done, total -> received += done to total })

        assertEquals(listOf(0 to 2, 1 to 2, 2 to 2), received)
        assertEquals(1, poses.starterSetCalls)
    }

    @Test
    fun starterSetIncomplete_returnsFalse() = runTest {
        poses.starterSetResult = StarterSetResult.Incomplete(
            failedFiles = 1,
            failure = PoseLoadFailure(PoseContent.THUMBNAIL, LoadFailureReason.TIMEOUT),
        )
        assertFalse(repository.run(PreparationTask.STARTER_SET) { _, _ -> })
    }
}

private class FakeRefresher : RemoteConfigRefresher {
    var result = true
    var calls = 0

    override suspend fun refresh(): Boolean {
        calls++
        return result
    }
}

private class FakePoseCatalogRepository : PoseCatalogRepository {
    var catalogResult: CatalogState = CatalogState.Ready(PoseCatalog("v1", emptyList(), emptyList()))
    var starterSetResult: StarterSetResult = StarterSetResult.Complete
    var starterSetProgress: List<Pair<Int, Int>> = emptyList()
    var catalogCalls = 0
    var starterSetCalls = 0

    override val catalog: StateFlow<CatalogState> = MutableStateFlow(CatalogState.NotLoaded)
    override val failures: SharedFlow<PoseLoadFailure> = MutableSharedFlow()

    override suspend fun loadCatalog(): CatalogState {
        catalogCalls++
        return catalogResult
    }

    override suspend fun loadFile(pose: Pose, file: PoseFile): PoseFileResult = error("Not used by loading")

    override suspend fun loadStarterSet(onProgress: (done: Int, total: Int) -> Unit): StarterSetResult {
        starterSetCalls++
        starterSetProgress.forEach { (done, total) -> onProgress(done, total) }
        return starterSetResult
    }
}
