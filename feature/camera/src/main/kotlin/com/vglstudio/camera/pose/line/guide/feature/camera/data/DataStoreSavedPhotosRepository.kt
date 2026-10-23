package com.vglstudio.camera.pose.line.guide.feature.camera.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.IOException
import javax.inject.Inject
import kotlin.math.min
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen

/** Saved photos in the shared DataStore of :core:data: two new keys, no older data to move (plan T-016). */
internal class DataStoreSavedPhotosRepository @Inject constructor(private val dataStore: DataStore<Preferences>) :
    SavedPhotosRepository {
    // A read error must never crash the screen or freeze it: read as nothing saved, wait, then read again.
    private val preferences: Flow<Preferences> = dataStore.data
        .retryWhen { cause, attempt ->
            if (cause !is IOException) return@retryWhen false
            emit(emptyPreferences())
            delay(retryDelayMillis(attempt))
            true
        }

    override val lastSavedPhoto: Flow<String?> = preferences.map { it[LAST_PHOTO_KEY] }.distinctUntilChanged()

    override val savedCount: Flow<Int> = preferences.map { it[COUNT_KEY] ?: 0 }.distinctUntilChanged()

    override suspend fun recordSaved(uri: String) {
        dataStore.edit {
            it[LAST_PHOTO_KEY] = uri
            it[COUNT_KEY] = (it[COUNT_KEY] ?: 0) + 1
        }
    }

    private companion object {
        val LAST_PHOTO_KEY = stringPreferencesKey("camera_last_saved_photo")
        val COUNT_KEY = intPreferencesKey("camera_saved_photo_count")
        const val MAX_RETRY_DELAY_MILLIS = 30_000L

        fun retryDelayMillis(attempt: Long): Long =
            min(1_000L shl attempt.coerceAtMost(5).toInt(), MAX_RETRY_DELAY_MILLIS)
    }
}
