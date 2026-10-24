package com.vglstudio.camera.pose.line.guide.core.review

import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The time of the last review request in the shared DataStore (plan T-019). */
class DataStoreReviewHistoryTest {
    private val dataStore = InMemoryDataStore()
    private val history = DataStoreReviewHistory(dataStore, RecordingAppLogger())

    @Test
    fun recordThenRead() = runTest {
        assertNull(history.lastAskedAt())
        history.recordAsked(42)
        assertEquals(42L, history.lastAskedAt())
    }

    @Test
    fun readError_isNull() = runTest {
        history.recordAsked(42)
        dataStore.readFailures = 1
        assertNull(history.lastAskedAt())
        // Only that read failed: the stored value is still there.
        assertEquals(42L, history.lastAskedAt())
    }

    @Test
    fun writeError_isSwallowed() = runTest {
        dataStore.failWrites = true
        history.recordAsked(7)
        dataStore.failWrites = false
        assertNull(history.lastAskedAt())
        history.recordAsked(8)
        assertEquals(8L, history.lastAskedAt())
    }
}
