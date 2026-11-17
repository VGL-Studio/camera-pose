package com.vglstudio.camera.pose.line.guide.core.analytics

import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The start-up of every attribution SDK of the module (plan T-099, step 8): AppsFlyer then Tenjin, each caught on its
 * own, so Tenjin failing never keeps AppsFlyer from starting, nor the other way round.
 */
class FanOutAppsFlyerStartupTest {
    private val calls = mutableListOf<String>()
    private val logger = RecordingAppLogger()

    private inner class Target(val name: String, val fails: Boolean = false) : AppsFlyerStartup {
        override fun prepare() {
            calls += "$name.prepare"
            if (fails) throw IllegalStateException("prepare")
        }

        override fun start() {
            calls += "$name.start"
            if (fails) throw IllegalStateException("start")
        }
    }

    @Test
    fun callsEveryTarget_inOrder() {
        val startup = FanOutAppsFlyerStartup(listOf(Target("a"), Target("b")), logger)
        startup.prepare()
        startup.start()

        assertEquals(listOf("a.prepare", "b.prepare", "a.start", "b.start"), calls)
    }

    @Test
    fun oneTargetThrows_othersStillCalled() {
        val startup = FanOutAppsFlyerStartup(listOf(Target("a", fails = true), Target("b")), logger)
        startup.prepare()
        startup.start()

        assertEquals(listOf("a.prepare", "b.prepare", "a.start", "b.start"), calls)
        val warnings = logger.entries.filter { it.level == LogLevel.WARN }
        assertEquals(2, warnings.size)
        warnings.forEach { assertEquals(it.message, true, it.message.endsWith(": IllegalStateException")) }
    }
}
