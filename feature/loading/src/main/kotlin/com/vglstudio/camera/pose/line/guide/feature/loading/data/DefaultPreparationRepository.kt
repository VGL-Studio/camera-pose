package com.vglstudio.camera.pose.line.guide.feature.loading.data

import com.vglstudio.camera.pose.line.guide.core.config.RemoteConfigRefresher
import com.vglstudio.camera.pose.line.guide.core.poses.PoseCatalogRepository
import javax.inject.Inject

internal class DefaultPreparationRepository @Inject constructor(
    private val refresher: RemoteConfigRefresher,
    private val poses: PoseCatalogRepository,
) : PreparationRepository {
    override suspend fun run(task: PreparationTask, onProgress: (done: Int, total: Int) -> Unit): Boolean =
        when (task) {
            PreparationTask.REMOTE_CONFIG -> refresher.refresh()
            PreparationTask.CATALOG -> false
            PreparationTask.STARTER_SET -> false
        }
}
