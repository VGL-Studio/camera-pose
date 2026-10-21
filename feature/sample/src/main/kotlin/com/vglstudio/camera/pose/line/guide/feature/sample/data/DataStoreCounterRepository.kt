package com.vglstudio.camera.pose.line.guide.feature.sample.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import java.io.IOException
import javax.inject.Inject
import kotlin.math.min
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen

internal class DataStoreCounterRepository @Inject constructor(private val dataStore: DataStore<Preferences>) :
    CounterRepository {
    // A read error must never crash the screen or freeze it: show empty data, wait, then read again.
    // Corrupted files are handled once for the whole app in :core:data (DataStoreModule).
    override val count: Flow<Int> = dataStore.data
        .retryWhen { cause, attempt ->
            if (cause !is IOException) return@retryWhen false
            emit(emptyPreferences())
            delay(retryDelayMillis(attempt))
            true
        }
        .map { preferences -> preferences[COUNT_KEY] ?: 0 }
        .distinctUntilChanged()

    /** Throws [IOException] when the value cannot be written; the caller decides how to tell the user. */
    override suspend fun increment() {
        dataStore.edit { preferences ->
            preferences[COUNT_KEY] = (preferences[COUNT_KEY] ?: 0) + 1
        }
    }

    private companion object {
        val COUNT_KEY = intPreferencesKey("sample_count")
        const val MAX_RETRY_DELAY_MILLIS = 30_000L

        fun retryDelayMillis(attempt: Long): Long =
            min(1_000L shl attempt.coerceAtMost(5).toInt(), MAX_RETRY_DELAY_MILLIS)
    }
}
