package com.vglstudio.camera.pose.line.guide.core.poses

import com.vglstudio.camera.pose.line.guide.core.assets.AssetFailureReason
import com.vglstudio.camera.pose.line.guide.core.assets.AssetPath
import com.vglstudio.camera.pose.line.guide.core.poses.internal.TimedAssetLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TimedAssetLoaderTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val path = AssetPath("poses/img/p1/preview_540-1a2b3c4d.webp")

    private fun TestScope.loaderOver(assets: FakeRemoteAssets) =
        TimedAssetLoader(assets, CoroutineScope(StandardTestDispatcher(testScheduler) + SupervisorJob()))

    private fun failure(reason: LoadFailureReason) =
        PoseFileResult.Failed(PoseLoadFailure(PoseContent.THUMBNAIL, reason))

    @Test
    fun ready_returnsFile() = runTest {
        val assets = FakeRemoteAssets(folder.root)
        val result = loaderOver(assets).load(path, PoseContent.THUMBNAIL)

        result as PoseFileResult.Ready
        assertTrue(result.file.isFile)
    }

    @Test
    fun unavailableNetwork_returnsFailedWithReasonNetwork() = runTest {
        val assets = FakeRemoteAssets(folder.root).apply { failures[path.value] = AssetFailureReason.NETWORK }
        assertEquals(failure(LoadFailureReason.NETWORK), loaderOver(assets).load(path, PoseContent.THUMBNAIL))
    }

    @Test
    fun unavailableNotFound_returnsFailedWithReasonNotFound() = runTest {
        val assets = FakeRemoteAssets(folder.root).apply { failures[path.value] = AssetFailureReason.NOT_FOUND }
        assertEquals(failure(LoadFailureReason.NOT_FOUND), loaderOver(assets).load(path, PoseContent.THUMBNAIL))
    }

    @Test
    fun unavailableOther_returnsFailedWithReasonOther() = runTest {
        val assets = FakeRemoteAssets(folder.root).apply { failures[path.value] = AssetFailureReason.OTHER }
        assertEquals(failure(LoadFailureReason.OTHER), loaderOver(assets).load(path, PoseContent.THUMBNAIL))
    }

    @Test
    fun noAnswerWithin15s_returnsTimeoutAtExactly15s() = runTest {
        val assets = FakeRemoteAssets(folder.root).apply { delays[path.value] = 60_000 }

        val result = loaderOver(assets).load(path, PoseContent.THUMBNAIL)

        assertEquals(failure(LoadFailureReason.TIMEOUT), result)
        assertEquals(15_000L, currentTime)
    }

    @Test
    fun answerAt14999ms_returnsReady() = runTest {
        val assets = FakeRemoteAssets(folder.root).apply { delays[path.value] = 14_999 }

        val result = loaderOver(assets).load(path, PoseContent.THUMBNAIL)

        assertTrue(result is PoseFileResult.Ready)
        assertEquals(14_999L, currentTime)
    }

    @Test
    fun afterTimeout_downloadContinues_andNextLoadIsReadyWithoutNewDownload() = runTest {
        val assets = FakeRemoteAssets(folder.root).apply { delays[path.value] = 60_000 }
        val loader = loaderOver(assets)
        assertEquals(failure(LoadFailureReason.TIMEOUT), loader.load(path, PoseContent.THUMBNAIL))

        advanceUntilIdle()
        val timeAfterDownload = currentTime
        val result = loader.load(path, PoseContent.THUMBNAIL)

        assertTrue(result is PoseFileResult.Ready)
        assertEquals(60_000L, timeAfterDownload)
        assertEquals(timeAfterDownload, currentTime)
        assertEquals(1, assets.downloads[path.value])
    }
}
