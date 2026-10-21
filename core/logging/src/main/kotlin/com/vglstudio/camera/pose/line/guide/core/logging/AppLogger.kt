package com.vglstudio.camera.pose.line.guide.core.logging

/**
 * Records what the app does, so a failure can be explained afterwards (ADR-0019).
 * Every entry goes to logcat under the tag `VGL.<tag>`; CI keeps the emulator's logcat when a device test fails.
 * Entries at [info] level and above are also kept by Crashlytics and sent only with a crash report.
 *
 * Log state changes, outcomes of user actions, caught errors and retries. Never log personal data, file contents,
 * identifiers or anything typed by the user.
 */
interface AppLogger {
    /** Detail useful while developing. Logcat only, never sent with a crash report. */
    fun debug(tag: String, message: String)

    fun info(tag: String, message: String)

    fun warn(tag: String, message: String, error: Throwable? = null)

    fun error(tag: String, message: String, error: Throwable? = null)
}

enum class LogLevel { DEBUG, INFO, WARN, ERROR }

/** One logged entry, as kept by [RecordingAppLogger]. */
data class LogEntry(val level: LogLevel, val tag: String, val message: String, val error: Throwable? = null)

/** Keeps entries in memory. For unit tests, which check what a ViewModel logs without Android. */
class RecordingAppLogger : AppLogger {
    private val recorded = mutableListOf<LogEntry>()

    val entries: List<LogEntry> get() = synchronized(recorded) { recorded.toList() }

    override fun debug(tag: String, message: String) = record(LogEntry(LogLevel.DEBUG, tag, message))

    override fun info(tag: String, message: String) = record(LogEntry(LogLevel.INFO, tag, message))

    override fun warn(tag: String, message: String, error: Throwable?) =
        record(LogEntry(LogLevel.WARN, tag, message, error))

    override fun error(tag: String, message: String, error: Throwable?) =
        record(LogEntry(LogLevel.ERROR, tag, message, error))

    private fun record(entry: LogEntry) {
        synchronized(recorded) { recorded += entry }
    }
}
