package com.vglstudio.camera.pose.line.guide.core.config

import com.google.firebase.remoteconfig.FirebaseRemoteConfigFetchThrottledException
import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteConfigRefresherTest {
    private var fetchCount = 0
    private val logger = RecordingAppLogger()

    // The real constructor calls Android's TextUtils, which is not available in JVM unit tests.
    private val throttled = mockk<FirebaseRemoteConfigFetchThrottledException>(relaxed = true)

    private fun policy(isFake: Boolean = false, fetch: suspend () -> Unit = {}) = RemoteConfigRefreshPolicy(
        isFakeConfig = { isFake },
        fetchAndActivate = {
            fetchCount++
            fetch()
        },
        logger = logger,
    )

    @Test
    fun fakeConfig_returnsTrueWithoutFetching() = runTest {
        assertTrue(policy(isFake = true).refresh())
        assertEquals(0, fetchCount)
    }

    @Test
    fun fetchSucceeds_returnsTrue() = runTest {
        assertTrue(policy().refresh())
        assertEquals(1, fetchCount)
    }

    @Test
    fun fetchFails_returnsFalse() = runTest {
        assertFalse(policy(fetch = { throw IOException("no network") }).refresh())
        assertEquals(1, fetchCount)
    }

    @Test
    fun fetchFails_logsWarning() = runTest {
        policy(fetch = { throw IOException("no network") }).refresh()
        val entry = logger.entries.single()
        assertEquals(LogLevel.WARN, entry.level)
        assertEquals(RemoteConfigRefreshPolicy.LOG_TAG, entry.tag)
        // Coroutines may copy the exception to recover its stack trace, so compare its type.
        assertTrue(entry.error is IOException)
    }

    @Test
    fun fetchThrottled_returnsTrue() = runTest {
        assertTrue(policy(fetch = { throw throttled }).refresh())
        assertEquals(listOf(LogLevel.INFO), logger.entries.map { it.level })
    }

    @Test
    fun fetchLongerThanFifteenSeconds_returnsFalse() = runTest {
        assertFalse(policy(fetch = { delay(15_001) }).refresh())
        assertEquals(listOf(LogLevel.WARN), logger.entries.map { it.level })
    }

    @Test
    fun fetchJustUnderFifteenSeconds_returnsTrue() = runTest {
        assertTrue(policy(fetch = { delay(14_999) }).refresh())
    }

    /** ADR-1001: this app waits 15 seconds, not the 10 of the template project. */
    @Test
    fun fetchBetweenTenAndFifteenSeconds_returnsTrue() = runTest {
        assertTrue(policy(fetch = { delay(12_000) }).refresh())
    }
}
