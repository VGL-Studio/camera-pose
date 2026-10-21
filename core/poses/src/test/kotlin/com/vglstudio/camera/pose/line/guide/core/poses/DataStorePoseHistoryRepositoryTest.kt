package com.vglstudio.camera.pose.line.guide.core.poses

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import com.vglstudio.camera.pose.line.guide.core.poses.internal.DataStorePoseHistoryRepository
import com.vglstudio.camera.pose.line.guide.core.poses.internal.PosesClock
import com.vglstudio.camera.pose.line.guide.core.poses.model.SavedPose
import com.vglstudio.camera.pose.line.guide.core.poses.model.UsedPose
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class DataStorePoseHistoryRepositoryTest {
    private val dataStore = InMemoryDataStore()
    private val logger = RecordingAppLogger()
    private var now = 1_000L
    private val clock = PosesClock { now }

    private fun repository(store: InMemoryDataStore = dataStore) = DataStorePoseHistoryRepository(store, clock, logger)

    @Test
    fun nothingStored_savedAndRecentAreEmpty() = runTest {
        val repository = repository()
        assertEquals(emptyList<SavedPose>(), repository.saved.first())
        assertEquals(emptyList<UsedPose>(), repository.recent.first())
    }

    @Test
    fun setSavedTrueThenFalse_persistsAcrossRepositoryInstances() = runTest {
        repository().setSaved("w01", true)
        now = 2_000
        repository().setSaved("w02", true)

        // A new repository on the same storage, as after the app restarts.
        assertEquals(listOf(SavedPose("w02", 2_000), SavedPose("w01", 1_000)), repository().saved.first())

        repository().setSaved("w02", false)
        assertEquals(listOf(SavedPose("w01", 1_000)), repository().saved.first())
    }

    @Test
    fun setSavedTwice_keepsFirstSavedTime() = runTest {
        val repository = repository()
        repository.setSaved("w01", true)
        now = 5_000
        repository.setSaved("w01", true)

        assertEquals(listOf(SavedPose("w01", 1_000)), repository.saved.first())
    }

    @Test
    fun markUsed_movesPoseToFront_withClockTime() = runTest {
        val repository = repository()
        repository.markUsed("a")
        now = 2_000
        repository.markUsed("b")
        now = 3_000
        repository.markUsed("a")

        assertEquals(listOf(UsedPose("a", 3_000), UsedPose("b", 2_000)), repository.recent.first())
    }

    @Test
    fun markUsed_keepsAtMost20_dropsOldest() = runTest {
        val repository = repository()
        repeat(25) { index ->
            now = 1_000L + index
            repository.markUsed("p$index")
        }

        val recent = repository.recent.first()
        assertEquals(20, recent.size)
        assertEquals("p24", recent.first().id)
        assertEquals("p5", recent.last().id)
    }

    @Test
    fun writeFails_throwsIOException() = runTest {
        dataStore.failWrites = true
        val repository = repository()

        val error = try {
            repository.setSaved("w01", true)
            fail("Expected an IOException")
            null
        } catch (e: IOException) {
            e
        }
        assertTrue(error is IOException)

        val markError = try {
            repository.markUsed("w01")
            null
        } catch (e: IOException) {
            e
        }
        assertTrue(markError is IOException)
    }

    @Test
    fun readFailsOnce_emitsEmptyThenStoredValues() = runTest {
        val repository = repository()
        repository.setSaved("w01", true)
        dataStore.readFailures = 1

        assertEquals(listOf(emptyList(), listOf(SavedPose("w01", 1_000))), repository.saved.take(2).toList())
    }

    @Test
    fun corruptedStoredValue_isEmpty_andLogsWarning() = runTest {
        dataStore.edit { it[stringPreferencesKey("poses_saved")] = "{not json" }

        assertEquals(emptyList<SavedPose>(), repository().saved.first())
        assertTrue(logger.entries.any { it.level == LogLevel.WARN && it.tag == "Poses" })
    }
}
