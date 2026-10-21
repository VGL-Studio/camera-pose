package com.vglstudio.camera.pose.line.guide.core.assets

import java.io.IOException
import java.net.UnknownHostException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DefaultRemoteAssetsFailureReasonTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val path = AssetPath("images/v2/hero.webp")

    private fun assets(testScheduler: TestCoroutineScheduler, downloader: AssetDownloader) = DefaultRemoteAssets(
        AssetsConfig("https://assets.example.com"),
        folder.root,
        downloader,
        StandardTestDispatcher(testScheduler),
    )

    @Test
    fun downloaderThrowsUnknownHost_returnsUnavailableNetwork() = runTest {
        val assets = assets(testScheduler) { _, _ -> throw UnknownHostException("assets.example.com") }
        assertEquals(AssetResult.Unavailable(AssetFailureReason.NETWORK), assets.load(path))
    }

    @Test
    fun downloaderThrowsHttp404_returnsUnavailableNotFound() = runTest {
        val assets = assets(testScheduler) { url, _ -> throw HttpStatusException(404, url) }
        assertEquals(AssetResult.Unavailable(AssetFailureReason.NOT_FOUND), assets.load(path))
    }

    @Test
    fun downloaderThrowsPlainIOException_returnsUnavailableOther() = runTest {
        val assets = assets(testScheduler) { _, _ -> throw IOException("disk full") }
        assertEquals(AssetResult.Unavailable(AssetFailureReason.OTHER), assets.load(path))
    }

    @Test
    fun keptFile_returnsReadyWithoutReason() = runTest {
        val assets = assets(testScheduler) { _, target -> target.writeText("content") }
        assets.load(path)

        // A second instance finds the kept file even though its downloader would fail.
        val offline = assets(testScheduler) { _, _ -> throw UnknownHostException("offline") }
        val result = offline.load(path)

        assertEquals(true, result is AssetResult.Ready)
    }
}
