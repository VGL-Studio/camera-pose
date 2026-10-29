package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The mark "the app was opened once" of A-01 (plan T-024, question 6). */
class FirstOpenMarkTest {
    private val logger = RecordingAppLogger()
    private val key = booleanPreferencesKey("ads_first_open_done")

    @Test
    fun firstConsume_isTrue_thenFalse() = runTest {
        val dataStore = MemoryDataStore()
        val mark = FirstOpenMark(dataStore, logger)
        assertTrue(mark.consume())
        assertFalse(mark.consume())
        assertEquals(true, dataStore.state.value[key])
    }

    @Test
    fun existingMark_isFalse() = runTest {
        val dataStore = MemoryDataStore(preferencesOf(key to true))
        assertFalse(FirstOpenMark(dataStore, logger).consume())
        // Control (L-055): an empty store is a first open.
        assertTrue(FirstOpenMark(MemoryDataStore(), logger).consume())
    }

    @Test
    fun readError_isLaterOpen() = runTest {
        assertFalse(FirstOpenMark(UnreadableDataStore(), logger).consume())
        val warnings = logger.entries.filter { it.level == LogLevel.WARN }
        assertEquals(1, warnings.size)
        assertEquals("Ads", warnings.single().tag)
    }

    @Test
    fun writeError_stillFirstOpen() = runTest {
        assertTrue(FirstOpenMark(UnwritableDataStore(), logger).consume())
        val warnings = logger.entries.filter { it.level == LogLevel.WARN }
        assertEquals(1, warnings.size)
        assertEquals("Ads", warnings.single().tag)
    }
}

private class MemoryDataStore(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
    val state = MutableStateFlow(initial)
    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}

/** A preferences file that cannot be read (I/O error). */
private class UnreadableDataStore : DataStore<Preferences> {
    override val data: Flow<Preferences> = flow { throw IOException("disk read failed") }

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        throw IOException("disk write failed")
}

/** A preferences file that is read empty but cannot be written. */
private class UnwritableDataStore : DataStore<Preferences> {
    override val data: Flow<Preferences> = MutableStateFlow(emptyPreferences())

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        throw IOException("disk write failed")
}
