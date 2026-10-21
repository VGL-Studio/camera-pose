package com.vglstudio.camera.pose.line.guide.core.poses.internal

import com.vglstudio.camera.pose.line.guide.core.config.RemoteSettings
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.core.poses.CatalogState
import com.vglstudio.camera.pose.line.guide.core.poses.LoadFailureReason
import com.vglstudio.camera.pose.line.guide.core.poses.PoseCatalogRepository
import com.vglstudio.camera.pose.line.guide.core.poses.PoseContent
import com.vglstudio.camera.pose.line.guide.core.poses.PoseFileResult
import com.vglstudio.camera.pose.line.guide.core.poses.PoseLoadFailure
import com.vglstudio.camera.pose.line.guide.core.poses.StarterSetResult
import com.vglstudio.camera.pose.line.guide.core.poses.model.Pose
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

internal class DefaultPoseCatalogRepository(
    private val loader: TimedAssetLoader,
    private val settings: RemoteSettings,
    private val parser: CatalogParser,
    private val logger: AppLogger,
    private val ioDispatcher: CoroutineDispatcher,
) : PoseCatalogRepository {
    private val state = MutableStateFlow<CatalogState>(CatalogState.NotLoaded)
    override val catalog: StateFlow<CatalogState> = state

    override val failures: SharedFlow<PoseLoadFailure> = MutableSharedFlow()

    override suspend fun loadCatalog(): CatalogState = state.value

    override suspend fun loadFile(pose: Pose, file: PoseFile): PoseFileResult =
        PoseFileResult.Failed(PoseLoadFailure(PoseContent.CATALOG, LoadFailureReason.OTHER))

    override suspend fun loadStarterSet(onProgress: (done: Int, total: Int) -> Unit): StarterSetResult =
        StarterSetResult.Incomplete(0, PoseLoadFailure(PoseContent.CATALOG, LoadFailureReason.OTHER))
}
