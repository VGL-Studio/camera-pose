package com.vglstudio.camera.pose.line.guide.core.config

import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultRemoteSettingsTest {
    private val remote = FakeRemoteFlagSource()
    private val settings = DefaultRemoteSettings(remote)

    @Test
    fun withoutRemoteValue_usesDefault() = runTest {
        assertTrue(settings.boolean("some_switch", default = true).first())
        assertEquals(120L, settings.long("some_interval", default = 120L).first())
    }

    @Test
    fun remoteValue_replacesDefault() = runTest {
        remote.booleans.value = mapOf("some_switch" to false)
        remote.longs.value = mapOf("some_interval" to 30L)
        assertFalse(settings.boolean("some_switch", default = true).first())
        assertEquals(30L, settings.long("some_interval", default = 120L).first())
    }

    @Test
    fun remoteValueActivatedLater_reachesCollectors() = runTest {
        settings.long("some_interval", default = 120L).test {
            assertEquals(120L, awaitItem())
            remote.longs.value = mapOf("some_interval" to 45L)
            assertEquals(45L, awaitItem())
        }
    }

    @Test
    fun stringWithoutRemoteValue_usesDefault() = runTest {
        assertEquals("default/path", settings.string("some_path", default = "default/path").first())
    }

    @Test
    fun stringRemoteValue_replacesDefault() = runTest {
        remote.strings.value = mapOf("some_path" to "remote/path")
        assertEquals("remote/path", settings.string("some_path", default = "default/path").first())
    }
}
