package com.vglstudio.camera.pose.line.guide.feature.loading.data

import com.vglstudio.camera.pose.line.guide.core.assets.AssetPath
import com.vglstudio.camera.pose.line.guide.core.assets.AssetResult
import com.vglstudio.camera.pose.line.guide.core.assets.RemoteAssets
import com.vglstudio.camera.pose.line.guide.core.config.RemoteConfigRefresher
import javax.inject.Inject

internal class DefaultPreparationRepository @Inject constructor(
    private val refresher: RemoteConfigRefresher,
    private val assets: RemoteAssets,
) : PreparationRepository {
    override suspend fun run(task: PreparationTask): Boolean = when (task) {
        PreparationTask.REMOTE_CONFIG -> refresher.refresh()
        PreparationTask.HELLO_FILE -> loadFile(HELLO_PATH)
        PreparationTask.CONTENT_FILE -> loadFile(CONTENT_PATH)
        PreparationTask.GOODBYE_FILE -> loadFile(GOODBYE_PATH)
    }

    private suspend fun loadFile(path: AssetPath): Boolean = assets.load(path) is AssetResult.Ready

    private companion object {
        val HELLO_PATH = AssetPath("test/v1/hello.txt")
        val CONTENT_PATH = AssetPath("test/v1/content.txt")
        val GOODBYE_PATH = AssetPath("test/v1/goodbye.txt")
    }
}
