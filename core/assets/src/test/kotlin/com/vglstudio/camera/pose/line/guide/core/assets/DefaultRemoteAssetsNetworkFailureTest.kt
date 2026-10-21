package com.vglstudio.camera.pose.line.guide.core.assets

import com.vglstudio.camera.pose.line.guide.core.network.NetworkFailureReporter
import java.io.IOException
import java.net.UnknownHostException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** A download that fails because of the network is reported, so the network dialog shows (spec network, KB-1). */
class DefaultRemoteAssetsNetworkFailureTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val path = AssetPath("images/v2/hero.webp")
    private val reported = mutableListOf<String>()
    private val reporter = NetworkFailureReporter { reported += it }

    private fun assets(testScheduler: TestCoroutineScheduler, downloader: AssetDownloader) = DefaultRemoteAssets(
        AssetsConfig("https://assets.example.com"),
        folder.root,
        downloader,
        StandardTestDispatcher(testScheduler),
        networkFailures = reporter,
    )

    @Test
    fun networkError_returnsUnavailable_andReportsOnce() = runTest {
        val assets = assets(testScheduler) { _, _ -> throw UnknownHostException("assets.example.com") }

        assertTrue(assets.load(path) is AssetResult.Unavailable)
        assertEquals(listOf("assets"), reported)
    }

    @Test
    fun http404_isNotReported() = runTest {
        val assets = assets(testScheduler) { url, _ -> throw HttpStatusException(404, url) }

        assertTrue(assets.load(path) is AssetResult.Unavailable)
        assertEquals(emptyList<String>(), reported)
    }

    @Test
    fun otherError_isNotReported() = runTest {
        val assets = assets(testScheduler) { _, _ -> throw IOException("HTTP 500") }

        assets.load(path)

        assertEquals(emptyList<String>(), reported)
    }

    @Test
    fun keptFile_isNotDownloaded_andNotReported() = runTest {
        assets(testScheduler) { _, target -> target.writeText("content") }.load(path)
        reported.clear()

        val offline = assets(testScheduler) { _, _ -> throw UnknownHostException("offline") }

        assertTrue(offline.load(path) is AssetResult.Ready)
        assertEquals(emptyList<String>(), reported)
    }

    @Test
    fun success_isNotReported() = runTest {
        val assets = assets(testScheduler) { _, target -> target.writeText("content") }

        assertTrue(assets.load(path) is AssetResult.Ready)
        assertEquals(emptyList<String>(), reported)
    }
}
