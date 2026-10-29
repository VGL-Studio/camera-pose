package com.vglstudio.camera.pose.line.guide.core.config

import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultRemoteSettingsTest {
    private val remote = FakeRemoteFlagSource()
    private val settings = settings()

    private fun settings(localValues: Set<LocalRemoteValue> = emptySet()) = DefaultRemoteSettings(remote, localValues)

    private val someSwitch = RemoteKey.boolean("some_switch", true, "A switch.")
    private val someInterval = RemoteKey.long("some_interval", 120L, "An interval.")
    private val someRatio = RemoteKey.double("some_ratio", 0.5, "A ratio.")
    private val otherRatio = RemoteKey.double("other_ratio", 0.5, "Another ratio.")
    private val someLanguages = RemoteKey.string("some_languages", "vi,en", "Languages.")

    private val testSwitch = RemoteKey.boolean("test_switch", default = false, description = "A test switch.")
    private val otherSwitch = RemoteKey.boolean("other_test_switch", default = false, description = "Another switch.")
    private val testInterval = RemoteKey.long("test_interval_seconds", default = 120L, description = "An interval.")
    private val testRatio = RemoteKey.double("test_ratio", default = 0.5, description = "A ratio.")
    private val testLanguages = RemoteKey.string("test_languages", default = "vi,en", description = "Languages.")

    @Test
    fun booleanAndLong_withoutRemoteValue_useDefault() = runTest {
        // Control: values of other keys sit next to the keys read, and must not leak into them.
        remote.booleans.value = mapOf("other_switch" to false)
        remote.longs.value = mapOf("other_interval" to 30L)
        assertTrue(settings.value(someSwitch).first())
        assertEquals(120L, settings.value(someInterval).first())
    }

    @Test
    fun booleanAndLong_remoteValue_replacesDefault() = runTest {
        remote.booleans.value = mapOf("some_switch" to false)
        remote.longs.value = mapOf("some_interval" to 30L)
        assertFalse(settings.value(someSwitch).first())
        assertEquals(30L, settings.value(someInterval).first())
    }

    @Test
    fun remoteValueActivatedLater_reachesCollectors() = runTest {
        settings.value(someInterval).test {
            assertEquals(120L, awaitItem())
            remote.longs.value = mapOf("some_interval" to 45L)
            assertEquals(45L, awaitItem())
        }
    }

    @Test
    fun doubleKey_withoutRemoteValue_usesDefault() = runTest {
        // Control: another double key has a remote value, which this key must not take.
        remote.doubles.value = mapOf("other_ratio" to 0.25)
        assertEquals(0.5, settings.value(someRatio).first(), 0.0)
        assertEquals(0.25, settings.value(otherRatio).first(), 0.0)
    }

    @Test
    fun doubleKey_remoteValue_replacesDefault() = runTest {
        remote.doubles.value = mapOf("some_ratio" to 0.25)
        assertEquals(0.25, settings.value(someRatio).first(), 0.0)
    }

    @Test
    fun stringKey_withoutRemoteValue_usesDefault() = runTest {
        // Control: another string key has a remote value, which this key must not take.
        remote.strings.value = mapOf("other_languages" to "hi")
        assertEquals("vi,en", settings.value(someLanguages).first())
    }

    @Test
    fun stringKey_remoteValue_replacesDefault() = runTest {
        remote.strings.value = mapOf("some_languages" to "hi")
        assertEquals("hi", settings.value(someLanguages).first())
    }

    @Test
    fun stringKey_remoteEmptyString_isKept() = runTest {
        remote.strings.value = mapOf("some_languages" to "")
        assertEquals("", settings.value(someLanguages).first())
    }

    @Test
    fun valueOfAnotherType_usesDefault() = runTest {
        remote.longs.value = mapOf("some_ratio" to 3L)
        // Control: a double value of another key is read, so doubles do reach the settings.
        remote.doubles.value = mapOf("other_ratio" to 0.25)
        assertEquals(0.5, settings.value(someRatio).first(), 0.0)
        assertEquals(0.25, settings.value(otherRatio).first(), 0.0)
    }

    @Test
    fun withoutRemoteOrLocalValue_usesDefault() = runTest {
        // Control: a local value of another key and a remote value of another key sit next to the key read.
        remote.booleans.value = mapOf("unrelated_switch" to true)
        val settings = settings(setOf(LocalRemoteValue.of(otherSwitch, true)))
        // Control: the local value is read for its own key, so local values do reach the settings.
        assertTrue(settings.value(otherSwitch).first())
        assertFalse(settings.value(testSwitch).first())
        assertEquals(120L, settings.value(testInterval).first())
    }

    @Test
    fun localValue_replacesDefault_whenNoRemoteValue() = runTest {
        val settings = settings(
            setOf(
                LocalRemoteValue.of(testSwitch, true),
                LocalRemoteValue.of(testInterval, 30L),
                LocalRemoteValue.of(testRatio, 0.25),
                LocalRemoteValue.of(testLanguages, "hi"),
            ),
        )
        assertTrue(settings.value(testSwitch).first())
        assertEquals(30L, settings.value(testInterval).first())
        assertEquals(0.25, settings.value(testRatio).first(), 0.0)
        assertEquals("hi", settings.value(testLanguages).first())
    }

    @Test
    fun remoteValue_winsOverLocalValue() = runTest {
        remote.booleans.value = mapOf("test_switch" to false)
        val settings = settings(setOf(LocalRemoteValue.of(testSwitch, true), LocalRemoteValue.of(testInterval, 30L)))
        // Control: a key with only a local value takes it, so local values are read at all.
        assertEquals(30L, settings.value(testInterval).first())
        assertFalse(settings.value(testSwitch).first())
    }

    @Test
    fun remoteValueActivatedThenRemoved_fallsBackToLocalValue() = runTest {
        val settings = settings(setOf(LocalRemoteValue.of(testSwitch, true)))
        settings.value(testSwitch).test {
            assertTrue(awaitItem())
            remote.booleans.value = mapOf("test_switch" to false)
            assertFalse(awaitItem())
            remote.booleans.value = emptyMap()
            // Back to the local value, never the default (false).
            assertTrue(awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun localValueOfOtherKey_doesNotChangeThisKey() = runTest {
        val settings = settings(setOf(LocalRemoteValue.of(otherSwitch, true)))
        // Control: the local value does reach its own key.
        assertTrue(settings.value(otherSwitch).first())
        assertFalse(settings.value(testSwitch).first())
    }

    @Test
    fun withoutLocalValues_readsRemoteThenDefault_asBefore() = runTest {
        val settings = settings(emptySet())
        assertFalse(settings.value(testSwitch).first())
        assertEquals(120L, settings.value(testInterval).first())
        remote.booleans.value = mapOf("test_switch" to true)
        remote.longs.value = mapOf("test_interval_seconds" to 45L)
        assertTrue(settings.value(testSwitch).first())
        assertEquals(45L, settings.value(testInterval).first())
    }

    @Test
    fun duplicateLocalKeyNames_failAtCreation() {
        val sameNameAsLong = RemoteKey.long("test_switch", default = 1L, description = "Same name, other type.")
        val error = assertThrows(IllegalArgumentException::class.java) {
            settings(setOf(LocalRemoteValue.of(testSwitch, true), LocalRemoteValue.of(sameNameAsLong, 2L)))
        }
        assertTrue(error.message.orEmpty().contains("test_switch"))
    }

    // The tests of this app before RemoteKey (plan T-074, section 3.5): same checks, read through value(RemoteKey).
    private val somePath = RemoteKey.string("some_path", "default/path", "A path.")

    @Test
    fun withoutRemoteValue_usesDefault() = runTest {
        assertTrue(settings.value(someSwitch).first())
        assertEquals(120L, settings.value(someInterval).first())
    }

    @Test
    fun remoteValue_replacesDefault() = runTest {
        remote.booleans.value = mapOf("some_switch" to false)
        remote.longs.value = mapOf("some_interval" to 30L)
        assertFalse(settings.value(someSwitch).first())
        assertEquals(30L, settings.value(someInterval).first())
    }

    @Test
    fun stringWithoutRemoteValue_usesDefault() = runTest {
        assertEquals("default/path", settings.value(somePath).first())
    }

    @Test
    fun stringRemoteValue_replacesDefault() = runTest {
        remote.strings.value = mapOf("some_path" to "remote/path")
        assertEquals("remote/path", settings.value(somePath).first())
    }
}
