package com.vglstudio.camera.pose.line.guide.onboarding

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** "Onboarding done" in the shared DataStore (spec onboarding, KB-3; plan T-012, section 4). */
class DataStoreOnboardingStatusTest {
    private val logger = RecordingAppLogger()

    // Passes on the skeleton too; kept as a guard (plan T-012, section 4).
    @Test
    fun nothingStored_isNotDone() = runTest {
        assertFalse(DataStoreOnboardingStatus(InMemoryDataStore(), logger).isDone())
    }

    @Test
    fun markDone_thenIsDone() = runTest {
        val status = DataStoreOnboardingStatus(InMemoryDataStore(), logger)
        status.markDone()
        assertTrue(status.isDone())
    }

    @Test
    fun markDone_storesOnboardingDoneKey() = runTest {
        val dataStore = InMemoryDataStore()
        DataStoreOnboardingStatus(dataStore, logger).markDone()
        assertEquals(true, dataStore.data.first()[booleanPreferencesKey("onboarding_done")])
    }

    @Test
    fun markDone_writeFails_logsWarning_doesNotThrow() = runTest {
        DataStoreOnboardingStatus(UnwritableDataStore(), logger).markDone()
        val warning = logger.entries.single { it.level == LogLevel.WARN }
        assertEquals("Onboarding", warning.tag)
        assertTrue(warning.error is IOException)
    }

    @Test
    fun isDone_readFails_throwsIOException() = runTest {
        val status = DataStoreOnboardingStatus(UnreadableDataStore(), logger)
        val exception = try {
            status.isDone()
            null
        } catch (e: Exception) {
            e
        }
        if (exception == null) fail("isDone() did not throw")
        assertTrue(exception is IOException)
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

/** Reads an empty file but cannot write (disk full, I/O error). */
private class UnwritableDataStore : DataStore<Preferences> {
    override val data: Flow<Preferences> = MutableStateFlow(emptyPreferences())

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        throw IOException("disk write failed")
}
