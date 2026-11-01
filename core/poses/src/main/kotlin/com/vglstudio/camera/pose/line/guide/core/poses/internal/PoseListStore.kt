package com.vglstudio.camera.pose.line.guide.core.poses.internal

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
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
internal data class StoredPose(val id: String, val at: Long)

/**
 * Lists of pose ids in the shared preferences, each one JSON string: `[{"id":"w01","at":1729500000000}]`.
 * A string that cannot be read counts as an empty list.
 */
internal class PoseListStore(private val dataStore: DataStore<Preferences>, private val logger: AppLogger) {
    private val json = Json { ignoreUnknownKeys = true }

    // A read error must never crash the screen or freeze it: show empty data, wait, then read again.
    fun read(key: Preferences.Key<String>): Flow<List<StoredPose>> = dataStore.data
        .retryWhen { cause, attempt ->
            if (cause !is IOException) return@retryWhen false
            logger.warn(LOG_TAG, "Could not read a stored pose list, trying again")
            emit(emptyPreferences())
            delay(retryDelayMillis(attempt))
            true
        }
        .map { preferences -> decode(preferences[key]) }
        .distinctUntilChanged()

    /** Writes [transform] of the list at [key]. Throws [IOException] when it cannot be written. */
    suspend fun update(key: Preferences.Key<String>, transform: (List<StoredPose>) -> List<StoredPose>) {
        dataStore.edit { preferences -> preferences[key] = encode(transform(decode(preferences[key]))) }
    }

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
        const val MAX_RETRY_DELAY_MILLIS = 30_000L

        fun retryDelayMillis(attempt: Long): Long =
            min(1_000L shl attempt.coerceAtMost(5).toInt(), MAX_RETRY_DELAY_MILLIS)
    }
}
