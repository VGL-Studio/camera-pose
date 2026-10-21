package com.vglstudio.camera.pose.line.guide.core.poses.internal

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.core.poses.PoseHistoryRepository
import com.vglstudio.camera.pose.line.guide.core.poses.model.SavedPose
import com.vglstudio.camera.pose.line.guide.core.poses.model.UsedPose
import java.io.IOException
import kotlin.math.min
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Serializable
private data class StoredPose(val id: String, val at: Long)

/**
 * Each list is one JSON string in the shared preferences: `[{"id":"w01","at":1729500000000}]`.
 * A string that cannot be read counts as an empty list.
 */
internal class DataStorePoseHistoryRepository(
    private val dataStore: DataStore<Preferences>,
    private val clock: PosesClock,
    private val logger: AppLogger,
) : PoseHistoryRepository {
    private val json = Json { ignoreUnknownKeys = true }

    override val saved: Flow<List<SavedPose>> = stored(SAVED_KEY)
        .map { list -> list.sortedByDescending { it.at }.map { SavedPose(it.id, it.at) } }

    override val recent: Flow<List<UsedPose>> = stored(RECENT_KEY)
        .map { list -> list.sortedByDescending { it.at }.map { UsedPose(it.id, it.at) } }

    override suspend fun setSaved(poseId: String, saved: Boolean) {
        dataStore.edit { preferences ->
            val list = decode(preferences[SAVED_KEY]).toMutableList()
            val index = list.indexOfFirst { it.id == poseId }
            if (saved) {
                if (index < 0) list.add(0, StoredPose(poseId, clock.nowMillis()))
            } else if (index >= 0) {
                list.removeAt(index)
            }
            preferences[SAVED_KEY] = encode(list)
        }
        logger.info(LOG_TAG, if (saved) "A pose was saved" else "A pose was unsaved")
    }

    override suspend fun markUsed(poseId: String) {
        dataStore.edit { preferences ->
            val list = decode(preferences[RECENT_KEY]).filter { it.id != poseId }
            preferences[RECENT_KEY] = encode((listOf(StoredPose(poseId, clock.nowMillis())) + list).take(RECENT_LIMIT))
        }
    }

    // A read error must never crash the screen or freeze it: show empty data, wait, then read again.
    private fun stored(key: Preferences.Key<String>): Flow<List<StoredPose>> = dataStore.data
        .retryWhen { cause, attempt ->
            if (cause !is IOException) return@retryWhen false
            logger.warn(LOG_TAG, "Could not read the saved poses, trying again")
            emit(emptyPreferences())
            delay(retryDelayMillis(attempt))
            true
        }
        .map { preferences -> decode(preferences[key]) }
        .distinctUntilChanged()

    private fun decode(value: String?): List<StoredPose> {
        if (value == null) return emptyList()
        return try {
            json.decodeFromString(ListSerializer(StoredPose.serializer()), value)
        } catch (e: IllegalArgumentException) {
            logger.warn(LOG_TAG, "A stored pose list is corrupted, ignoring it")
            emptyList()
        }
    }

    private fun encode(list: List<StoredPose>): String =
        json.encodeToString(ListSerializer(StoredPose.serializer()), list)

    private companion object {
        val SAVED_KEY = stringPreferencesKey("poses_saved")
        val RECENT_KEY = stringPreferencesKey("poses_recent")
        const val MAX_RETRY_DELAY_MILLIS = 30_000L

        fun retryDelayMillis(attempt: Long): Long =
            min(1_000L shl attempt.coerceAtMost(5).toInt(), MAX_RETRY_DELAY_MILLIS)
    }
}
