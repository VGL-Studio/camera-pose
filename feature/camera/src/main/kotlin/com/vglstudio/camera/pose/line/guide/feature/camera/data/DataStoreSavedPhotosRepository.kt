package com.vglstudio.camera.pose.line.guide.feature.camera.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Saved photos in the shared DataStore of :core:data. */
internal class DataStoreSavedPhotosRepository @Inject constructor(private val dataStore: DataStore<Preferences>) :
    SavedPhotosRepository {
    override val lastSavedPhoto: Flow<String?> = flowOf(null)

    override val savedCount: Flow<Int> = flowOf(0)

    override suspend fun recordSaved(uri: String) {
    }
}
