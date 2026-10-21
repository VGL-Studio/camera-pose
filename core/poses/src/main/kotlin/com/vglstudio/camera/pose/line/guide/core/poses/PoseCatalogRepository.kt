package com.vglstudio.camera.pose.line.guide.core.poses

import com.vglstudio.camera.pose.line.guide.core.poses.model.Pose
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseFile
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/** Loads the pose catalog and the images of its poses from R2. Every load waits at most 15 seconds (ADR-1001). */
interface PoseCatalogRepository {
    val catalog: StateFlow<CatalogState>

    /**
     * Loads the catalog at the path in Remote Config. Returns at once when it is already loaded from that path;
     * calls made at the same time load it once.
     */
    suspend fun loadCatalog(): CatalogState

    suspend fun loadFile(pose: Pose, file: PoseFile): PoseFileResult

    /**
     * Loads the thumbnail and the outline of every starter pose, at most four files at a time.
     * [onProgress] is called when it starts and after each file, on any thread, so keep it light; `done` never goes down, also across retries.
     */
    suspend fun loadStarterSet(onProgress: (done: Int, total: Int) -> Unit): StarterSetResult

    /** Every failed load, for the app shell to react to (for example a network dialog). */
    val failures: SharedFlow<PoseLoadFailure>
}
