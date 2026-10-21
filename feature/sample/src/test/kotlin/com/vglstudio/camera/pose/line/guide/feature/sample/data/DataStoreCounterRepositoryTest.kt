package com.vglstudio.camera.pose.line.guide.feature.sample.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
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

class DataStoreCounterRepositoryTest {
    @Test
    fun whenNothingStored_thenCountIsZero() = runTest {
        val repository = DataStoreCounterRepository(InMemoryDataStore())
        assertEquals(0, repository.count.first())
    }

    @Test
    fun whenIncrementTwice_thenCountIsTwo() = runTest {
        val repository = DataStoreCounterRepository(InMemoryDataStore())
        repository.increment()
        repository.increment()
        assertEquals(2, repository.count.first())
    }

    @Test
    fun whenStorageReadFailsOnce_thenCountRecoversAndKeepsUpdating() = runTest {
        val repository = DataStoreCounterRepository(FlakyDataStore(failuresBeforeSuccess = 1, storedCount = 5))
        assertEquals(listOf(0, 5), repository.count.take(2).toList())
    }

    @Test
    fun whenStorageCannotBeRead_thenCountFallsBackToZeroInsteadOfCrashing() = runTest {
        val repository = DataStoreCounterRepository(UnreadableDataStore())
        assertEquals(0, repository.count.first())
    }
}

private class InMemoryDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow<Preferences>(emptyPreferences())
    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}

/** Simulates a preferences file that cannot be read (I/O error). */
private class UnreadableDataStore : DataStore<Preferences> {
    override val data: Flow<Preferences> = flow { throw IOException("disk read failed") }

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        throw IOException("disk write failed")
}

/** Fails to read [failuresBeforeSuccess] times, then serves a stored count (a transient I/O error). */
private class FlakyDataStore(private var failuresBeforeSuccess: Int, storedCount: Int) : DataStore<Preferences> {
    private val stored = mutablePreferencesOf(intPreferencesKey("sample_count") to storedCount)

    override val data: Flow<Preferences> = flow {
        if (failuresBeforeSuccess > 0) {
            failuresBeforeSuccess--
            throw IOException("transient read failure")
        }
        emit(stored)
    }

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = transform(stored)
}
