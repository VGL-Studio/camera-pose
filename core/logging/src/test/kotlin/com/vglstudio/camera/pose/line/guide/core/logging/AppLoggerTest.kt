package com.vglstudio.camera.pose.line.guide.core.logging

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Test

class AppLoggerTest {
    @Test
    fun everyEntry_reachesEverySink_evenWhenOneSinkFails() {
        val written = mutableListOf<LogEntry>()
        val logger = DefaultAppLogger(listOf(LogSink { error("broken sink") }, LogSink { written += it }))

        logger.info("Loading", "started")
        logger.warn("Loading", "file failed", IOException())

        assertEquals(listOf(LogLevel.INFO, LogLevel.WARN), written.map { it.level })
    }

    @Test
    fun crashlytics_keepsInfoAndAbove_withTagAndErrorType() {
        val breadcrumbs = mutableListOf<String>()
        val logger = DefaultAppLogger(listOf(CrashlyticsSink { breadcrumbs += it }))

        logger.debug("Loading", "detail")
        logger.info("Loading", "started")
        logger.error("Loading", "config failed", IOException("offline"))

        assertEquals(
            listOf("I/VGL.Loading: started", "E/VGL.Loading: config failed (IOException)"),
            breadcrumbs,
        )
    }

    @Test
    fun crashlytics_neverReceivesTheErrorMessage() {
        val breadcrumbs = mutableListOf<String>()
        CrashlyticsSink { breadcrumbs += it }.write(LogEntry(LogLevel.WARN, "T", "m", IOException("secret path")))

        assertEquals(listOf("W/VGL.T: m (IOException)"), breadcrumbs)
    }

    @Test
    fun recordingLogger_keepsEntriesInOrder() {
        val logger = RecordingAppLogger()

        logger.info("A", "one")
        logger.debug("B", "two")

        assertEquals(listOf(LogEntry(LogLevel.INFO, "A", "one"), LogEntry(LogLevel.DEBUG, "B", "two")), logger.entries)
    }
}
