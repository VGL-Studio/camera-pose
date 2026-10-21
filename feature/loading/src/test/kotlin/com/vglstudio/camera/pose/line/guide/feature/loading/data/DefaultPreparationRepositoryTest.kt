package com.vglstudio.camera.pose.line.guide.feature.loading.data

import com.vglstudio.camera.pose.line.guide.core.assets.AssetPath
import com.vglstudio.camera.pose.line.guide.core.assets.AssetResult
import com.vglstudio.camera.pose.line.guide.core.assets.RemoteAssets
import com.vglstudio.camera.pose.line.guide.core.config.RemoteConfigRefresher
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultPreparationRepositoryTest {
    private val refresher = FakeRefresher()
    private val assets = FakeAssets()
    private val repository = DefaultPreparationRepository(refresher, assets)

    @Test
    fun fileTasks_loadTheThreeVersionedPaths() = runTest {
        assertTrue(repository.run(PreparationTask.HELLO_FILE))
        assertTrue(repository.run(PreparationTask.CONTENT_FILE))
        assertTrue(repository.run(PreparationTask.GOODBYE_FILE))
        assertEquals(
            listOf(
                AssetPath("test/v1/hello.txt"),
                AssetPath("test/v1/content.txt"),
                AssetPath("test/v1/goodbye.txt"),
            ),
            assets.requested,
        )
        assertEquals(0, refresher.calls)
    }

    @Test
    fun unavailableFile_returnsFalse() = runTest {
        assets.result = AssetResult.Unavailable()
        assertFalse(repository.run(PreparationTask.HELLO_FILE))
    }

    @Test
    fun remoteConfigTask_usesRefresher() = runTest {
        assertTrue(repository.run(PreparationTask.REMOTE_CONFIG))
        refresher.result = false
        assertFalse(repository.run(PreparationTask.REMOTE_CONFIG))
        assertEquals(2, refresher.calls)
        assertEquals(emptyList<AssetPath>(), assets.requested)
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

private class FakeAssets : RemoteAssets {
    var result: AssetResult = AssetResult.Ready(File("unused"))
    val requested = mutableListOf<AssetPath>()

    override suspend fun load(path: AssetPath): AssetResult {
        requested += path
        return result
    }
}
