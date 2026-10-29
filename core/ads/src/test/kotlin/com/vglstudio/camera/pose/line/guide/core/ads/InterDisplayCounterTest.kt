package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import java.io.File
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** The count of interstitials shown since install (N-05, plan T-026), for T-033. */
class InterDisplayCounterTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val logger = RecordingAppLogger()

    private fun TestScope.fileDataStore(): DataStore<Preferences> {
        val file = File(folder.root, "ads.preferences_pb")
        return PreferenceDataStoreFactory.create(scope = backgroundScope) { file }
    }

    @Test
    fun firstDisplay_isOne_thenTwo() = runTest {
        val dataStore = fileDataStore()
        val counter = InterDisplayCounter(dataStore, logger)
        assertEquals(1, counter.increment())
        assertEquals(2, counter.increment())
        // Control (L-055): a new counter on the same store goes on from what was written.
        assertEquals(3, InterDisplayCounter(dataStore, logger).increment())
    }

    @Test
    fun readOrWriteError_returnsNull_noThrow() = runTest {
        assertNull(InterDisplayCounter(BrokenDataStore(), logger).increment())
        val warnings = logger.entries.filter { it.level == LogLevel.WARN }
        assertEquals(1, warnings.size)
        assertEquals("Ads", warnings.single().tag)
    }
}

/** A preferences file that is read empty but cannot be written. */
private class BrokenDataStore : DataStore<Preferences> {
    override val data: Flow<Preferences> = MutableStateFlow(emptyPreferences())

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        throw IOException("disk write failed")
}
