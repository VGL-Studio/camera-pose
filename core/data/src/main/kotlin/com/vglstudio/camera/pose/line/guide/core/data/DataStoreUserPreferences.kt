package com.vglstudio.camera.pose.line.guide.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.IOException
import javax.inject.Inject
import kotlin.math.min
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen

/** The settings choices in the shared DataStore, under keys of their own (plan T-025, section 3.3). */
internal class DataStoreUserPreferences @Inject constructor(private val dataStore: DataStore<Preferences>) :
    UserPreferences {
    // A read error must never crash a screen: show the defaults, wait, then read again (as the sample feature).
    private val preferences: Flow<Preferences> = dataStore.data
        .retryWhen { cause, attempt ->
            if (cause !is IOException) return@retryWhen false
            emit(emptyPreferences())
            delay(retryDelayMillis(attempt))
            true
        }

    override val themeMode: Flow<ThemeMode> = preferences
        .map { stored -> ThemeMode.entries.firstOrNull { it.name == stored[THEME_KEY] } ?: ThemeMode.SYSTEM }
        .distinctUntilChanged()

    override val shutterSoundEnabled: Flow<Boolean> = preferences
        .map { it[SOUND_KEY] ?: true }
        .distinctUntilChanged()

    override val appLanguage: Flow<String> = preferences
        .map { stored -> stored[LANGUAGE_KEY]?.takeIf { it in AppLanguages.RELEASE } ?: AppLanguages.SYSTEM }
        .distinctUntilChanged()

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_KEY] = mode.name }
    }

    override suspend fun setShutterSoundEnabled(enabled: Boolean) {
        dataStore.edit { it[SOUND_KEY] = enabled }
    }

    override suspend fun setAppLanguage(tag: String) {
        dataStore.edit { it[LANGUAGE_KEY] = tag }
    }

    private companion object {
        val THEME_KEY = stringPreferencesKey("settings_theme_mode")
        val SOUND_KEY = booleanPreferencesKey("settings_shutter_sound")
        val LANGUAGE_KEY = stringPreferencesKey("settings_app_language")
        const val MAX_RETRY_DELAY_MILLIS = 30_000L

        fun retryDelayMillis(attempt: Long): Long =
            min(1_000L shl attempt.coerceAtMost(5).toInt(), MAX_RETRY_DELAY_MILLIS)
    }
}
