package com.vglstudio.camera.pose.line.guide.core.poses

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlag
import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import com.vglstudio.camera.pose.line.guide.core.poses.internal.DataStorePremiumPoseRepository
import com.vglstudio.camera.pose.line.guide.core.poses.internal.PosesClock
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class DataStorePremiumPoseRepositoryTest {
    private val dataStore = InMemoryDataStore()
    private val flags = FakeFeatureFlags(setOf(FeatureFlag.PREMIUM_POSES))
    private val logger = RecordingAppLogger()
    private var now = 1_000L
    private val clock = PosesClock { now }

    private val premium = Fixtures.pose("p1", premium = true)
    private val free = Fixtures.pose("f1")
    private val unlockedKey = stringPreferencesKey("poses_unlocked")

    private fun repository() = DataStorePremiumPoseRepository(dataStore, flags, clock, logger)

    private fun flagOff() {
        flags.enabled.value = emptySet()
    }

    private fun flagOn() {
        flags.enabled.value = setOf(FeatureFlag.PREMIUM_POSES)
    }

    @Test
    fun flagOn_premiumLocked_freeUsable() = runTest {
        val access = repository().access.first()

        assertTrue(access.enabled)
        assertTrue(access.isLocked(premium))
        assertTrue(access.isUsable(free))
    }

    @Test
    fun flagOff_everyPoseUsable_andUnlockWritesNothing() = runTest {
        flagOff()
        val repository = repository()

        assertTrue(repository.isUsable(premium).first())
        assertFalse(repository.unlock("p1"))
        assertNull(dataStore.data.first()[unlockedKey])

        // Control: with the flag on the same pose is locked, so nothing was unlocked while it was off.
        flagOn()
        assertFalse(repository.isUsable(premium).first())
        assertTrue(repository.unlock("p1"))
    }

    @Test
    fun access_followsFlagOffAndOnAgain() = runTest {
        val repository = repository()
        val unlocked = Fixtures.pose("p2", premium = true)
        repository.unlock("p2")

        repository.isUsable(premium).test {
            assertEquals(false, awaitItem())
            flagOff()
            assertEquals(true, awaitItem())
            flagOn()
            assertEquals(false, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        // The unlocked list is not cleared while the flag is off.
        assertTrue(repository.isUsable(unlocked).first())
    }

    @Test
    fun unlock_thenNewRepository_staysUnlocked() = runTest {
        assertTrue(repository().unlock("p1"))

        // A new repository on the same storage, as after the app restarts.
        val reopened = repository()
        assertTrue(reopened.isUsable(premium).first())
        // Control: a premium pose that was not unlocked stays locked.
        assertFalse(reopened.isUsable(Fixtures.pose("p3", premium = true)).first())
    }

    @Test
    fun unlock_storesTimeNewestFirst_secondUnlockKeepsFirstTime() = runTest {
        val repository = repository()
        repository.unlock("a")
        now = 2_000
        repository.unlock("b")
        now = 5_000
        assertTrue(repository.unlock("a"))

        assertEquals(
            """[{"id":"b","at":2000},{"id":"a","at":1000}]""",
            dataStore.data.first()[unlockedKey],
        )
    }

    @Test
    fun unlockedId_keptWhateverTheCatalog() = runTest {
        val repository = repository()
        repository.unlock("gone")

        val access = repository.access.first()
        assertEquals(setOf("gone"), access.unlockedIds)
        // A pose with that id that comes back to the catalog later is still unlocked.
        assertFalse(access.isLocked(Fixtures.pose("gone", premium = true)))
    }

    @Test
    fun isUsable_turnsTrueAfterUnlock() = runTest {
        val repository = repository()

        repository.isUsable(premium).test {
            assertEquals(false, awaitItem())
            repository.unlock("p1")
            assertEquals(true, awaitItem())
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun unlock_writeFails_throwsIOException() = runTest {
        dataStore.failWrites = true

        val error = try {
            repository().unlock("p1")
            fail("Expected an IOException")
            null
        } catch (e: IOException) {
            e
        }
        assertTrue(error is IOException)
    }

    @Test
    fun readFailsOnce_lockedThenStoredValue() = runTest {
        val repository = repository()
        repository.unlock("p1")
        dataStore.readFailures = 1

        assertEquals(
            listOf(emptySet(), setOf("p1")),
            repository.access.take(2).toList().map { it.unlockedIds },
        )
    }

    @Test
    fun corruptedUnlockedList_isEmpty_andLogsWarningWithoutIds() = runTest {
        dataStore.edit { it[unlockedKey] = "[{\"id\":\"p1\"" }

        assertEquals(emptySet<String>(), repository().access.first().unlockedIds)
        assertTrue(logger.entries.any { it.level == LogLevel.WARN && it.tag == "Poses" })
        assertTrue(logger.entries.none { "p1" in it.message })
    }

    @Test
    fun unlock_logsWithoutPoseId() = runTest {
        repository().unlock("p1")
        assertTrue(
            logger.entries.any {
                it.level == LogLevel.INFO && it.tag == "Poses" && it.message == "A premium pose was unlocked"
            },
        )

        flagOff()
        repository().unlock("p1")
        assertTrue(
            logger.entries.any {
                it.level == LogLevel.INFO && it.tag == "Poses" && it.message == "Unlock skipped: flag_off"
            },
        )
        assertTrue(logger.entries.none { "p1" in it.message })
    }
}
