package com.vglstudio.camera.pose.line.guide.core.poses.internal

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.core.poses.PoseHistoryRepository
import com.vglstudio.camera.pose.line.guide.core.poses.model.SavedPose
import com.vglstudio.camera.pose.line.guide.core.poses.model.UsedPose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

internal class DataStorePoseHistoryRepository(
    private val dataStore: DataStore<Preferences>,
    private val clock: PosesClock,
    private val logger: AppLogger,
) : PoseHistoryRepository {
    override val saved: Flow<List<SavedPose>> = emptyFlow()

    override val recent: Flow<List<UsedPose>> = emptyFlow()

    override suspend fun setSaved(poseId: String, saved: Boolean) = Unit

    override suspend fun markUsed(poseId: String) = Unit
}
