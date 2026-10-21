package com.vglstudio.camera.pose.line.guide.core.poses

import com.vglstudio.camera.pose.line.guide.core.poses.model.LibraryState
import kotlinx.coroutines.flow.Flow

/** The catalog with saved and recently used poses applied. */
interface PoseLibraryRepository {
    /** Starts loading the catalog when nothing has loaded it yet. */
    val library: Flow<LibraryState>

    suspend fun retry()
}
