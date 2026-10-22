package com.vglstudio.camera.pose.line.guide.feature.camera.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.vglstudio.camera.pose.line.guide.feature.camera.model.CameraSettings
import com.vglstudio.camera.pose.line.guide.feature.camera.model.FlashMode
import com.vglstudio.camera.pose.line.guide.feature.camera.model.Lens
import com.vglstudio.camera.pose.line.guide.feature.camera.model.TimerSetting
import java.io.IOException
import javax.inject.Inject
import kotlin.math.min
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen

/** Capture tools in the shared DataStore of :core:data. An unknown stored value reads as the default. */
internal class DataStoreCameraSettingsRepository @Inject constructor(private val dataStore: DataStore<Preferences>) :
    CameraSettingsRepository {
    // A read error must never crash the screen or freeze it: use the defaults, wait, then read again.
    override val settings: Flow<CameraSettings> = dataStore.data
        .retryWhen { cause, attempt ->
            if (cause !is IOException) return@retryWhen false
            emit(emptyPreferences())
            delay(retryDelayMillis(attempt))
            true
        }
        .map { preferences ->
            val defaults = CameraSettings()
            CameraSettings(
                flash = FlashMode.entries.firstOrNull { it.name == preferences[FLASH_KEY] } ?: defaults.flash,
                timer = TimerSetting.entries.firstOrNull { it.seconds == preferences[TIMER_KEY] } ?: defaults.timer,
                gridVisible = preferences[GRID_KEY] ?: defaults.gridVisible,
                lens = Lens.entries.firstOrNull { it.name == preferences[LENS_KEY] } ?: defaults.lens,
            )
        }
        .distinctUntilChanged()

    override suspend fun setFlash(mode: FlashMode) {
        dataStore.edit { it[FLASH_KEY] = mode.name }
    }

    override suspend fun setTimer(timer: TimerSetting) {
        dataStore.edit { it[TIMER_KEY] = timer.seconds }
    }

    override suspend fun setGridVisible(visible: Boolean) {
        dataStore.edit { it[GRID_KEY] = visible }
    }

    override suspend fun setLens(lens: Lens) {
        dataStore.edit { it[LENS_KEY] = lens.name }
    }

    private companion object {
        val FLASH_KEY = stringPreferencesKey("camera_flash")
        val TIMER_KEY = intPreferencesKey("camera_timer")
        val GRID_KEY = booleanPreferencesKey("camera_grid")
        val LENS_KEY = stringPreferencesKey("camera_lens")
        const val MAX_RETRY_DELAY_MILLIS = 30_000L

        fun retryDelayMillis(attempt: Long): Long =
            min(1_000L shl attempt.coerceAtMost(5).toInt(), MAX_RETRY_DELAY_MILLIS)
    }
}
