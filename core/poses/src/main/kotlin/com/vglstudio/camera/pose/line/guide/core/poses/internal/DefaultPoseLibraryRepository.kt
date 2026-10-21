package com.vglstudio.camera.pose.line.guide.core.poses.internal

import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.core.poses.CatalogState
import com.vglstudio.camera.pose.line.guide.core.poses.PoseCatalogRepository
import com.vglstudio.camera.pose.line.guide.core.poses.PoseHistoryRepository
import com.vglstudio.camera.pose.line.guide.core.poses.PoseLibraryRepository
import com.vglstudio.camera.pose.line.guide.core.poses.model.LibraryState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

internal class DefaultPoseLibraryRepository(
    private val catalogRepository: PoseCatalogRepository,
    private val history: PoseHistoryRepository,
    private val logger: AppLogger,
) : PoseLibraryRepository {
    override val library: Flow<LibraryState> = channelFlow {
        // Nobody loaded the catalog yet (for example the loading screen is off): start it, and show Loading.
        if (catalogRepository.catalog.value is CatalogState.NotLoaded) {
            launch { catalogRepository.loadCatalog() }
        }
        combine(catalogRepository.catalog, history.saved, history.recent) { catalog, saved, recent ->
            when (catalog) {
                is CatalogState.NotLoaded, is CatalogState.Loading -> LibraryState.Loading
                is CatalogState.Failed -> LibraryState.Failed(catalog.failure)
                is CatalogState.Ready -> LibraryState.Ready(buildPoseLibrary(catalog.catalog, saved, recent))
            }
        }
            .distinctUntilChanged()
            .collect { send(it) }
    }

    override suspend fun retry() {
        logger.info(LOG_TAG, "Retrying to load the catalog")
        catalogRepository.loadCatalog()
    }
}
