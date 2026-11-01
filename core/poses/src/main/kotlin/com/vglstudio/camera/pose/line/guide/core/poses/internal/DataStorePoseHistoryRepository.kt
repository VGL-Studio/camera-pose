package com.vglstudio.camera.pose.line.guide.core.poses.internal

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.core.poses.PoseHistoryRepository
import com.vglstudio.camera.pose.line.guide.core.poses.model.SavedPose
import com.vglstudio.camera.pose.line.guide.core.poses.model.UsedPose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Saved and recent poses, each one list of [PoseListStore]. */
internal class DataStorePoseHistoryRepository(
    dataStore: DataStore<Preferences>,
    private val clock: PosesClock,
    private val logger: AppLogger,
) : PoseHistoryRepository {
    private val store = PoseListStore(dataStore, logger)

    override val saved: Flow<List<SavedPose>> = store.read(SAVED_KEY)
        .map { list -> list.sortedByDescending { it.at }.map { SavedPose(it.id, it.at) } }

    override val recent: Flow<List<UsedPose>> = store.read(RECENT_KEY)
        .map { list -> list.sortedByDescending { it.at }.map { UsedPose(it.id, it.at) } }

    override suspend fun setSaved(poseId: String, saved: Boolean) {
        store.update(SAVED_KEY) { stored ->
            val list = stored.toMutableList()
            val index = list.indexOfFirst { it.id == poseId }
            if (saved) {
                if (index < 0) list.add(0, StoredPose(poseId, clock.nowMillis()))
            } else if (index >= 0) {
                list.removeAt(index)
            }
            list
        }
        logger.info(LOG_TAG, if (saved) "A pose was saved" else "A pose was unsaved")
    }

    override suspend fun markUsed(poseId: String) {
        store.update(RECENT_KEY) { stored ->
            val list = stored.filter { it.id != poseId }
            (listOf(StoredPose(poseId, clock.nowMillis())) + list).take(RECENT_LIMIT)
        }
    }

    private companion object {
        val SAVED_KEY = stringPreferencesKey("poses_saved")
        val RECENT_KEY = stringPreferencesKey("poses_recent")
    }
}
