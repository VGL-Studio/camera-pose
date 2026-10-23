package com.vglstudio.camera.pose.line.guide.feature.camera.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.DEFAULT_OPACITY
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.clampOpacity
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.clampScale
import java.io.IOException
import javax.inject.Inject
import kotlin.math.min
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen

/**
 * Pose overlay settings in the shared DataStore of :core:data (spec camera, section 6): the step card choice (key
 * camera_steps_hidden) and the outline adjustment (keys camera_overlay_opacity, camera_overlay_scale,
 * camera_overlay_mirrored, camera_overlay_locked, camera_overlay_hidden).
 */
internal class DataStorePoseOverlaySettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : PoseOverlaySettingsRepository {
    // A read error must never crash the screen or freeze it: use the default, wait, then read again.
    private val data: Flow<Preferences> = dataStore.data
        .retryWhen { cause, attempt ->
            if (cause !is IOException) return@retryWhen false
            emit(emptyPreferences())
            delay(retryDelayMillis(attempt))
            true
        }

    override val stepsHidden: Flow<Boolean> = data
        .map { it[STEPS_HIDDEN_KEY] ?: false }
        .distinctUntilChanged()

    override suspend fun setStepsHidden(hidden: Boolean) {
        dataStore.edit { it[STEPS_HIDDEN_KEY] = hidden }
    }

    // Values out of range (edited by hand, an older build) are clamped on read.
    override val overlay: Flow<OverlaySettings> = data
        .map { preferences ->
            OverlaySettings(
                opacity = clampOpacity(preferences[OPACITY_KEY].orDefault(DEFAULT_OPACITY)),
                scale = clampScale(preferences[SCALE_KEY].orDefault(1f)),
                mirrored = preferences[MIRRORED_KEY] ?: false,
                locked = preferences[LOCKED_KEY] ?: false,
                hidden = preferences[HIDDEN_KEY] ?: false,
            )
        }
        .distinctUntilChanged()

    override suspend fun setOverlay(settings: OverlaySettings) {
        dataStore.edit {
            it[OPACITY_KEY] = settings.opacity
            it[SCALE_KEY] = settings.scale
            it[MIRRORED_KEY] = settings.mirrored
            it[LOCKED_KEY] = settings.locked
            it[HIDDEN_KEY] = settings.hidden
        }
    }

    private companion object {
        val STEPS_HIDDEN_KEY = booleanPreferencesKey("camera_steps_hidden")
        val OPACITY_KEY = floatPreferencesKey("camera_overlay_opacity")
        val SCALE_KEY = floatPreferencesKey("camera_overlay_scale")
        val MIRRORED_KEY = booleanPreferencesKey("camera_overlay_mirrored")
        val LOCKED_KEY = booleanPreferencesKey("camera_overlay_locked")
        val HIDDEN_KEY = booleanPreferencesKey("camera_overlay_hidden")
        const val MAX_RETRY_DELAY_MILLIS = 30_000L

        fun retryDelayMillis(attempt: Long): Long =
            min(1_000L shl attempt.coerceAtMost(5).toInt(), MAX_RETRY_DELAY_MILLIS)

        /** A stored NaN counts as not stored. */
        fun Float?.orDefault(default: Float): Float = if (this == null || isNaN()) default else this
    }
}
