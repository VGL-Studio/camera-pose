package com.vglstudio.camera.pose.line.guide.feature.camera.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Pose overlay settings in the shared DataStore of :core:data. */
internal class DataStorePoseOverlaySettingsRepository @Inject constructor(
    @Suppress("unused") private val dataStore: DataStore<Preferences>,
) : PoseOverlaySettingsRepository {
    override val stepsHidden: Flow<Boolean> = flowOf(false)

    override suspend fun setStepsHidden(hidden: Boolean) {
    }
}
