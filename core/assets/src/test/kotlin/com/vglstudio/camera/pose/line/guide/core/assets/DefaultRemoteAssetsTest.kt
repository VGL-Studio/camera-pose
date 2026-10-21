package com.vglstudio.camera.pose.line.guide.core.assets

import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DefaultRemoteAssetsTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val path = AssetPath("images/v2/hero.webp")
    private val requestedUrls = mutableListOf<String>()
    private val downloads = AtomicInteger()
    private var online = true

    private val downloader = AssetDownloader { url, target ->
        if (!online) throw IOException("offline")
        downloads.incrementAndGet()
        requestedUrls += url
        target.writeText("content of $url")
    }

    private fun assets(dispatcher: TestDispatcher, cacheDir: File = folder.root) =
        DefaultRemoteAssets(AssetsConfig("https://assets.example.com"), cacheDir, downloader, dispatcher)

    @Test
    fun firstLoad_downloadsFromTheBaseUrl() = runTest {
        val result = assets(StandardTestDispatcher(testScheduler)).load(path)

        assertEquals(listOf("https://assets.example.com/images/v2/hero.webp"), requestedUrls)
        result as AssetResult.Ready
        assertEquals("content of https://assets.example.com/images/v2/hero.webp", result.file.readText())
    }

    @Test
    fun keptFile_isUsedOfflineWithoutDownloadingAgain() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        assets(dispatcher).load(path)
        online = false

        // A new instance, as after the app restarts.
        val result = assets(dispatcher).load(path)

        assertTrue(result is AssetResult.Ready)
        assertEquals(1, downloads.get())
    }

    @Test
    fun offlineWithoutKeptFile_isUnavailable_thenRetrySucceeds() = runTest {
        val assets = assets(StandardTestDispatcher(testScheduler))
        online = false
        assertEquals(AssetResult.Unavailable, assets.load(path))

        online = true
        assertTrue(assets.load(path) is AssetResult.Ready)
    }

    @Test
    fun failedDownload_leavesNoFileBehind() = runTest {
        val failing = DefaultRemoteAssets(
            AssetsConfig("https://assets.example.com/"),
            folder.root,
            { _, target ->
                target.writeText("partial")
                throw IOException("connection lost")
            },
            StandardTestDispatcher(testScheduler),
        )

        assertEquals(AssetResult.Unavailable, failing.load(path))
        assertFalse(File(folder.root, path.value).exists())
        assertEquals(emptyList<String>(), folder.root.walk().filter { it.isFile }.toList().map { it.name })
    }

    @Test
    fun concurrentLoadsOfOneFile_downloadOnce() = runTest {
        // Real threads and a slow download, so the loads overlap.
        val assets = DefaultRemoteAssets(
            AssetsConfig("https://assets.example.com"),
            folder.root,
            { url, target ->
                Thread.sleep(50)
                downloader.download(url, target)
            },
            Dispatchers.IO,
        )

        val results = List(5) { async(Dispatchers.IO) { assets.load(path) } }.awaitAll()

        assertTrue(results.all { it is AssetResult.Ready })
        assertEquals(1, downloads.get())
    }

    @Test(expected = IllegalArgumentException::class)
    fun baseUrlWithoutHttps_isRejected() {
        AssetsConfig("http://assets.example.com")
    }
}
