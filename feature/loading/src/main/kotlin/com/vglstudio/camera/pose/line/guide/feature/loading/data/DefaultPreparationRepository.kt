package com.vglstudio.camera.pose.line.guide.feature.loading.data

import com.vglstudio.camera.pose.line.guide.core.config.RemoteConfigRefresher
import com.vglstudio.camera.pose.line.guide.core.poses.CatalogState
import com.vglstudio.camera.pose.line.guide.core.poses.PoseCatalogRepository
import com.vglstudio.camera.pose.line.guide.core.poses.StarterSetResult
import javax.inject.Inject

/** :core:poses logs why a load failed (tag Poses); the view model logs the result of each task. */
internal class DefaultPreparationRepository @Inject constructor(
    private val refresher: RemoteConfigRefresher,
    private val poses: PoseCatalogRepository,
) : PreparationRepository {
    override suspend fun run(task: PreparationTask, onProgress: (done: Int, total: Int) -> Unit): Boolean =
        when (task) {
            PreparationTask.REMOTE_CONFIG -> refresher.refresh()
            PreparationTask.CATALOG -> poses.loadCatalog() is CatalogState.Ready
            PreparationTask.STARTER_SET -> poses.loadStarterSet(onProgress) is StarterSetResult.Complete
        }
}
