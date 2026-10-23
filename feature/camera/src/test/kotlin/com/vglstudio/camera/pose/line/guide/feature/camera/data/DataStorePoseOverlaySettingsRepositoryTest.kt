package com.vglstudio.camera.pose.line.guide.feature.camera.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Whether the step card is hidden, kept across app launches (spec camera, KB-2; key camera_steps_hidden), and the
 * outline adjustment (spec camera, section 6; keys camera_overlay_*).
 */
class DataStorePoseOverlaySettingsRepositoryTest {
    @Test
    fun nothingStored_isShown() = runTest {
        val store = MemoryPreferencesStore()
        val repository = DataStorePoseOverlaySettingsRepository(store)
        assertEquals(false, repository.stepsHidden.first())

        // The value is read from the key, not fixed: a stored true reads as hidden.
        store.state.value = mutablePreferencesOf(STEPS_HIDDEN to true)
        assertEquals(true, repository.stepsHidden.first())
    }

    @Test
    fun setStepsHidden_isReadBack() = runTest {
        val store = MemoryPreferencesStore()
        val repository = DataStorePoseOverlaySettingsRepository(store)
        repository.setStepsHidden(true)
        assertEquals(true, repository.stepsHidden.first())
        assertEquals(true, store.state.value[STEPS_HIDDEN])

        repository.setStepsHidden(false)
        assertEquals(false, repository.stepsHidden.first())
    }

    @Test
    fun readFailsOnce_recovers() = runTest {
        val repository = DataStorePoseOverlaySettingsRepository(
            FailingOncePreferencesStore(mutablePreferencesOf(STEPS_HIDDEN to true)),
        )
        assertEquals(listOf(false, true), repository.stepsHidden.take(2).toList())
    }

    // Outline adjustment (spec camera, section 6; plan T-015)

    @Test
    fun nothingStored_isDefaultOverlay() = runTest {
        val store = MemoryPreferencesStore()
        val repository = DataStorePoseOverlaySettingsRepository(store)
        assertEquals(OverlaySettings(), repository.overlay.first())

        // The values are read from the keys, not fixed: stored values read back.
        store.state.value = mutablePreferencesOf(
            OPACITY to 0.5f,
            SCALE to 1.3f,
            MIRRORED to true,
            LOCKED to true,
            HIDDEN to true,
        )
        val expected = OverlaySettings(0.5f, 1.3f, mirrored = true, locked = true, hidden = true)
        assertEquals(expected, repository.overlay.first())
    }

    @Test
    fun setOverlay_isReadBack() = runTest {
        val store = MemoryPreferencesStore()
        val repository = DataStorePoseOverlaySettingsRepository(store)
        // Every field differs from its default.
        val settings = OverlaySettings(opacity = 0.6f, scale = 0.7f, mirrored = true, locked = true, hidden = true)
        repository.setOverlay(settings)
        assertEquals(settings, repository.overlay.first())
        assertEquals(0.6f, store.state.value[OPACITY])
        assertEquals(0.7f, store.state.value[SCALE])
        assertEquals(true, store.state.value[MIRRORED])
        assertEquals(true, store.state.value[LOCKED])
        assertEquals(true, store.state.value[HIDDEN])

        val back = OverlaySettings()
        repository.setOverlay(back)
        assertEquals(back, repository.overlay.first())
    }

    @Test
    fun outOfRangeStored_isClamped() = runTest {
        val store = MemoryPreferencesStore()
        val repository = DataStorePoseOverlaySettingsRepository(store)
        store.updateData { it.toMutablePreferences().apply { set(SCALE, 5.0f) } }
        store.updateData { it.toMutablePreferences().apply { set(OPACITY, 0.0f) } }
        store.updateData { it.toMutablePreferences().apply { set(LOCKED, true) } }
        val settings = repository.overlay.first()
        assertEquals(1.6f, settings.scale, 0f)
        assertEquals(0.2f, settings.opacity, 0f)
        // The other fields are read as stored.
        assertEquals(true, settings.locked)
        assertEquals(false, settings.mirrored)
    }

    @Test
    fun setOverlay_keepsStepsHidden() = runTest {
        val repository = DataStorePoseOverlaySettingsRepository(MemoryPreferencesStore())
        repository.setStepsHidden(true)
        val settings = OverlaySettings(opacity = 0.6f, scale = 0.7f, mirrored = true, locked = true, hidden = true)
        repository.setOverlay(settings)
        assertEquals(settings, repository.overlay.first())
        assertEquals(true, repository.stepsHidden.first())

        repository.setStepsHidden(false)
        assertEquals(settings, repository.overlay.first())
    }

    @Test
    fun overlayReadFailsOnce_recovers() = runTest {
        val repository = DataStorePoseOverlaySettingsRepository(
            FailingOncePreferencesStore(mutablePreferencesOf(SCALE to 1.3f)),
        )
        assertEquals(listOf(OverlaySettings(), OverlaySettings(scale = 1.3f)), repository.overlay.take(2).toList())
    }

    private companion object {
        val STEPS_HIDDEN = booleanPreferencesKey("camera_steps_hidden")
        val OPACITY = floatPreferencesKey("camera_overlay_opacity")
        val SCALE = floatPreferencesKey("camera_overlay_scale")
        val MIRRORED = booleanPreferencesKey("camera_overlay_mirrored")
        val LOCKED = booleanPreferencesKey("camera_overlay_locked")
        val HIDDEN = booleanPreferencesKey("camera_overlay_hidden")
    }
}

private class MemoryPreferencesStore(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
    val state = MutableStateFlow(initial)
    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}

/** Fails to read once, then serves [stored] (a transient I/O error). */
private class FailingOncePreferencesStore(private val stored: Preferences) : DataStore<Preferences> {
    private var failed = false

    override val data: Flow<Preferences> = flow {
        if (!failed) {
            failed = true
            throw IOException("transient read failure")
        }
        emit(stored)
    }

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = transform(stored)
}
