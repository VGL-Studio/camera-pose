package com.vglstudio.camera.pose.line.guide.core.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RemoteKeyTest {
    @Test
    fun factories_keepNameTypeDefaultAndDescription() {
        val switch = RemoteKey.boolean("some_switch", true, "A switch.")
        assertEquals("some_switch", switch.name)
        assertEquals(RemoteKey.Type.BOOLEAN, switch.type)
        assertEquals(true, switch.default)
        assertEquals("A switch.", switch.description)

        val interval = RemoteKey.long("some_interval_seconds", 45L, "An interval.")
        assertEquals("some_interval_seconds", interval.name)
        assertEquals(RemoteKey.Type.LONG, interval.type)
        assertEquals(45L, interval.default)
        assertEquals("An interval.", interval.description)

        val ratio = RemoteKey.double("some_ratio_percent", 0.5, "A ratio.")
        assertEquals("some_ratio_percent", ratio.name)
        assertEquals(RemoteKey.Type.DOUBLE, ratio.type)
        assertEquals(0.5, ratio.default, 0.0)
        assertEquals("A ratio.", ratio.description)

        val languages = RemoteKey.string("some_languages", "vi,en", "Languages.")
        assertEquals("some_languages", languages.name)
        assertEquals(RemoteKey.Type.STRING, languages.type)
        assertEquals("vi,en", languages.default)
        assertEquals("Languages.", languages.description)
    }

    @Test
    fun invalidName_isRejected() {
        listOf("Ads_Enabled", "ads-enabled", "_ads", "ads__enabled", "").forEach { name ->
            assertThrows("'$name' must be rejected", IllegalArgumentException::class.java) {
                RemoteKey.boolean(name, false, "x")
            }
        }
        // Control: a name that follows ADR-0023 is accepted.
        assertEquals("ads_enabled", RemoteKey.boolean("ads_enabled", false, "x").name)
    }

    @Test
    fun blankDescription_isRejected() {
        listOf("", "   ").forEach { description ->
            assertThrows(IllegalArgumentException::class.java) { RemoteKey.long("a_b", 1L, description) }
        }
    }

    @Test
    fun descriptionOver256Chars_isRejected() {
        assertThrows(IllegalArgumentException::class.java) { RemoteKey.string("a_b", "", "x".repeat(257)) }
        assertEquals(256, RemoteKey.string("a_b", "", "x".repeat(256)).description.length)
    }

    @Test
    fun sameFields_areEqual() {
        val first = RemoteKey.boolean("a_b", false, "x")
        val second = RemoteKey.boolean("a_b", false, "x")
        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
        assertNotEquals(first, RemoteKey.boolean("a_b", true, "x"))
        assertEquals("a_b", first.toString())
    }
}
