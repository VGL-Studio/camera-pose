package com.vglstudio.camera.pose.line.guide.feature.camera.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
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
import org.junit.Assert.assertNull
import org.junit.Test

class DataStoreSavedPhotosRepositoryTest {
    @Test
    fun nothingStored_nullAndZero() = runTest {
        val repository = DataStoreSavedPhotosRepository(MemoryPhotosDataStore())
        assertNull(repository.lastSavedPhoto.first())
        assertEquals(0, repository.savedCount.first())
        // Control: the same repository reads back what it stores.
        repository.recordSaved(FIRST)
        assertEquals(FIRST, repository.lastSavedPhoto.first())
        assertEquals(1, repository.savedCount.first())
    }

    @Test
    fun recordSaved_incrementsAndKeepsLatest() = runTest {
        val store = MemoryPhotosDataStore()
        val repository = DataStoreSavedPhotosRepository(store)

        repository.recordSaved(FIRST)
        repository.recordSaved(SECOND)

        assertEquals(2, repository.savedCount.first())
        assertEquals(SECOND, repository.lastSavedPhoto.first())
        // The keys of plan T-016, section 3.1.
        assertEquals(SECOND, store.current[stringPreferencesKey("camera_last_saved_photo")])
        assertEquals(2, store.current[intPreferencesKey("camera_saved_photo_count")])
    }

    @Test
    fun readFailsOnce_recovers() = runTest {
        val stored = mutablePreferencesOf(
            stringPreferencesKey("camera_last_saved_photo") to SECOND,
            intPreferencesKey("camera_saved_photo_count") to 3,
        )
        val forPhoto = DataStoreSavedPhotosRepository(FlakyPhotosDataStore(failuresBeforeSuccess = 1, stored))
        assertEquals(listOf(null, SECOND), forPhoto.lastSavedPhoto.take(2).toList())
        val forCount = DataStoreSavedPhotosRepository(FlakyPhotosDataStore(failuresBeforeSuccess = 1, stored))
        assertEquals(listOf(0, 3), forCount.savedCount.take(2).toList())
    }

    private companion object {
        const val FIRST = "content://media/external/images/media/7"
        const val SECOND = "content://media/external/images/media/8"
    }
}

private class MemoryPhotosDataStore(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
    private val state = MutableStateFlow(initial)
    val current: Preferences get() = state.value
    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}

/** Fails to read [failuresBeforeSuccess] times, then serves [stored] (a transient I/O error). */
private class FlakyPhotosDataStore(private var failuresBeforeSuccess: Int, private val stored: Preferences) :
    DataStore<Preferences> {
    override val data: Flow<Preferences> = flow {
        if (failuresBeforeSuccess > 0) {
            failuresBeforeSuccess--
            throw IOException("transient read failure")
        }
        emit(stored)
    }

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = transform(stored)
}
