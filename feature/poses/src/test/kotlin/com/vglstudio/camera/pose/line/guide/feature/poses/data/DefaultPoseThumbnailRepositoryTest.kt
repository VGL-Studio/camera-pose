package com.vglstudio.camera.pose.line.guide.feature.poses.data

import androidx.compose.ui.graphics.ImageBitmap
import com.vglstudio.camera.pose.line.guide.core.poses.CatalogState
import com.vglstudio.camera.pose.line.guide.core.poses.LoadFailureReason
import com.vglstudio.camera.pose.line.guide.core.poses.PoseCatalogRepository
import com.vglstudio.camera.pose.line.guide.core.poses.PoseContent
import com.vglstudio.camera.pose.line.guide.core.poses.PoseFileResult
import com.vglstudio.camera.pose.line.guide.core.poses.PoseLoadFailure
import com.vglstudio.camera.pose.line.guide.core.poses.StarterSetResult
import com.vglstudio.camera.pose.line.guide.core.poses.model.Pose
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseFile
import com.vglstudio.camera.pose.line.guide.feature.poses.testPose
import io.mockk.mockk
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Thumbnails through :core:poses, decoded off the main thread (plan T-021, section 3.1). */
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultPoseThumbnailRepositoryTest {
    private val catalog = FakeCatalogRepository()
    private val decodes = mutableListOf<Pair<File, Int>>()
    private var decoded: ImageBitmap? = mockk()
    private val decoder = PoseImageDecoder { file, width ->
        decodes += file to width
        decoded
    }

    private fun repository(scheduler: TestCoroutineScheduler) =
        DefaultPoseThumbnailRepository(catalog, decoder, UnconfinedTestDispatcher(scheduler))

    @Test
    fun load_requestsThumbnailFile_andDecodes() = runTest {
        val result = repository(testScheduler).load(testPose("w01"))
        assertEquals(listOf("w01" to PoseFile.THUMBNAIL), catalog.loads)
        // 162 dp wide at xxhdpi.
        assertEquals(listOf(File("w01-THUMBNAIL") to 486), decodes)
        assertEquals(ThumbnailResult.Ready(decoded!!), result)
    }

    @Test
    fun loadFileFailed_returnsSameFailure() = runTest {
        val failure = PoseLoadFailure(PoseContent.THUMBNAIL, LoadFailureReason.TIMEOUT)
        catalog.result = PoseFileResult.Failed(failure)
        val result = repository(testScheduler).load(testPose("w01"))
        assertEquals(ThumbnailResult.Failed(failure), result)
        assertTrue(decodes.isEmpty())
    }

    @Test
    fun decoderReturnsNull_returnsFailedOther() = runTest {
        decoded = null
        val result = repository(testScheduler).load(testPose("w01"))
        assertEquals(
            ThumbnailResult.Failed(PoseLoadFailure(PoseContent.THUMBNAIL, LoadFailureReason.OTHER)),
            result,
        )
    }

    private class FakeCatalogRepository : PoseCatalogRepository {
        val loads = mutableListOf<Pair<String, PoseFile>>()
        var result: PoseFileResult? = null

        override val catalog: StateFlow<CatalogState> = MutableStateFlow(CatalogState.NotLoaded)

        override suspend fun loadCatalog(): CatalogState = catalog.value

        override suspend fun loadFile(pose: Pose, file: PoseFile): PoseFileResult {
            loads += pose.id to file
            return result ?: PoseFileResult.Ready(File("${pose.id}-$file"))
        }

        override suspend fun loadStarterSet(onProgress: (done: Int, total: Int) -> Unit): StarterSetResult =
            StarterSetResult.Complete

        override val failures: SharedFlow<PoseLoadFailure> = MutableSharedFlow()
    }
}
