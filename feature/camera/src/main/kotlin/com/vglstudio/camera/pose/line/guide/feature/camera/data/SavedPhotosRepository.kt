package com.vglstudio.camera.pose.line.guide.feature.camera.data

import kotlinx.coroutines.flow.Flow

/** Photos saved to the gallery by the app: the latest one (camera thumbnail) and how many since install. */
interface SavedPhotosRepository {
    /** Content uri of the latest saved photo, null before the first save. */
    val lastSavedPhoto: Flow<String?>

    /** Photos saved since install (T-018, T-019). */
    val savedCount: Flow<Int>

    /** Throws IOException when storage fails. */
    suspend fun recordSaved(uri: String)
}
