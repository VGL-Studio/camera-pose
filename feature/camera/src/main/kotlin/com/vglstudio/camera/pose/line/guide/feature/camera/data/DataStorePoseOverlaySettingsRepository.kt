package com.vglstudio.camera.pose.line.guide.feature.camera.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import java.io.IOException
import javax.inject.Inject
import kotlin.math.min
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen

/** Pose overlay settings in the shared DataStore of :core:data (key camera_steps_hidden, spec camera, section 6). */
internal class DataStorePoseOverlaySettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : PoseOverlaySettingsRepository {
    // A read error must never crash the screen or freeze it: use the default, wait, then read again.
    override val stepsHidden: Flow<Boolean> = dataStore.data
        .retryWhen { cause, attempt ->
            if (cause !is IOException) return@retryWhen false
            emit(emptyPreferences())
            delay(retryDelayMillis(attempt))
            true
        }
        .map { it[STEPS_HIDDEN_KEY] ?: false }
        .distinctUntilChanged()

    override suspend fun setStepsHidden(hidden: Boolean) {
        dataStore.edit { it[STEPS_HIDDEN_KEY] = hidden }
    }

    private companion object {
        val STEPS_HIDDEN_KEY = booleanPreferencesKey("camera_steps_hidden")
        const val MAX_RETRY_DELAY_MILLIS = 30_000L

        fun retryDelayMillis(attempt: Long): Long =
            min(1_000L shl attempt.coerceAtMost(5).toInt(), MAX_RETRY_DELAY_MILLIS)
    }
}
