package com.vglstudio.camera.pose.line.guide.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The choices of the settings screen kept on the device (spec settings, KB-2, KB-3, KB-4; plan T-025, section 4). */
class DataStoreUserPreferencesTest {
    @Test
    fun nothingStored_returnsDefaults() = runTest {
        val preferences = DataStoreUserPreferences(InMemoryDataStore())
        assertEquals(ThemeMode.SYSTEM, preferences.themeMode.first())
        assertTrue(preferences.shutterSoundEnabled.first())
        assertEquals("", preferences.appLanguage.first())

        // Control: the defaults come from an empty store, not from constants. Stored values are read.
        val stored = DataStoreUserPreferences(
            InMemoryDataStore(
                mutablePreferencesOf(THEME_KEY to "LIGHT", SOUND_KEY to false, LANGUAGE_KEY to "th"),
            ),
        )
        assertEquals(ThemeMode.LIGHT, stored.themeMode.first())
        assertFalse(stored.shutterSoundEnabled.first())
        assertEquals("th", stored.appLanguage.first())
    }

    @Test
    fun eachSetter_isReadBack() = runTest {
        val store = InMemoryDataStore()
        val preferences = DataStoreUserPreferences(store)
        preferences.setThemeMode(ThemeMode.DARK)
        preferences.setShutterSoundEnabled(false)
        preferences.setAppLanguage("vi")

        assertEquals(ThemeMode.DARK, preferences.themeMode.first())
        assertFalse(preferences.shutterSoundEnabled.first())
        assertEquals("vi", preferences.appLanguage.first())
        // Kept under the keys of the plan, so a new instance (the app opened again) reads the same choices.
        assertEquals("DARK", store.state.value[THEME_KEY])
        assertEquals(false, store.state.value[SOUND_KEY])
        assertEquals("vi", store.state.value[LANGUAGE_KEY])
    }

    @Test
    fun unknownStoredValue_fallsBackToDefault() = runTest {
        val preferences = DataStoreUserPreferences(
            InMemoryDataStore(mutablePreferencesOf(THEME_KEY to "SEPIA", LANGUAGE_KEY to "fr")),
        )
        assertEquals(ThemeMode.SYSTEM, preferences.themeMode.first())
        assertEquals("", preferences.appLanguage.first())

        // Control: a known value under the same keys is read as stored.
        val known = DataStoreUserPreferences(
            InMemoryDataStore(mutablePreferencesOf(THEME_KEY to "DARK", LANGUAGE_KEY to "es-419")),
        )
        assertEquals(ThemeMode.DARK, known.themeMode.first())
        assertEquals("es-419", known.appLanguage.first())
    }

    @Test
    fun readFailsOnce_recovers() = runTest {
        val preferences = DataStoreUserPreferences(
            FlakyDataStore(mutablePreferencesOf(THEME_KEY to "DARK", SOUND_KEY to false, LANGUAGE_KEY to "hi")),
        )
        assertEquals(listOf(ThemeMode.SYSTEM, ThemeMode.DARK), preferences.themeMode.take(2).toList())
        assertEquals(listOf(true, false), preferences.shutterSoundEnabled.take(2).toList())
        assertEquals(listOf("", "hi"), preferences.appLanguage.take(2).toList())
    }

    private companion object {
        val THEME_KEY = stringPreferencesKey("settings_theme_mode")
        val SOUND_KEY = booleanPreferencesKey("settings_shutter_sound")
        val LANGUAGE_KEY = stringPreferencesKey("settings_app_language")
    }
}

private class InMemoryDataStore(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
    val state = MutableStateFlow(initial)
    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}

/**
 * A transient I/O error: every other read fails, starting with the first, so each new reader fails once and then
 * reads [stored] on its retry.
 */
private class FlakyDataStore(private val stored: Preferences) : DataStore<Preferences> {
    private var reads = 0

    override val data: Flow<Preferences> = flow {
        if (reads++ % 2 == 0) throw IOException("transient read failure")
        emit(stored)
    }

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = transform(stored)
}
