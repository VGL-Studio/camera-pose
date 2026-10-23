package com.vglstudio.camera.pose.line.guide.feature.camera.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
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

/** Whether the step card is hidden, kept across app launches (spec camera, KB-2; key camera_steps_hidden). */
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

    private companion object {
        val STEPS_HIDDEN = booleanPreferencesKey("camera_steps_hidden")
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
