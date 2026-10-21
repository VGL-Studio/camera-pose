package com.vglstudio.camera.pose.line.guide.core.logging

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject

/** Where log entries go. Kept internal: features only see [AppLogger]. */
internal fun interface LogSink {
    fun write(entry: LogEntry)
}

/** Writes every entry to each sink; a failing sink never stops the others or the app. */
internal class DefaultAppLogger(private val sinks: List<LogSink>) : AppLogger {
    @Inject
    constructor() : this(listOf(LogcatSink, CrashlyticsSink { FirebaseCrashlytics.getInstance().log(it) }))

    override fun debug(tag: String, message: String) = write(LogEntry(LogLevel.DEBUG, tag, message))

    override fun info(tag: String, message: String) = write(LogEntry(LogLevel.INFO, tag, message))

    override fun warn(tag: String, message: String, error: Throwable?) =
        write(LogEntry(LogLevel.WARN, tag, message, error))

    override fun error(tag: String, message: String, error: Throwable?) =
        write(LogEntry(LogLevel.ERROR, tag, message, error))

    private fun write(entry: LogEntry) {
        sinks.forEach { sink ->
            try {
                sink.write(entry)
            } catch (e: RuntimeException) {
                // Logging must never break the app.
            }
        }
    }
}

/** Logcat, under `VGL.<tag>` so CI and developers can filter the app's own entries. */
internal object LogcatSink : LogSink {
    override fun write(entry: LogEntry) {
        val tag = logcatTag(entry.tag)
        when (entry.level) {
            LogLevel.DEBUG -> Log.d(tag, entry.message)
            LogLevel.INFO -> Log.i(tag, entry.message)
            LogLevel.WARN -> Log.w(tag, entry.message, entry.error)
            LogLevel.ERROR -> Log.e(tag, entry.message, entry.error)
        }
    }
}

/**
 * Crashlytics keeps the latest entries and sends them only with a crash report (ADR-0012, ADR-0019).
 * Debug entries are left out.
 */
internal class CrashlyticsSink(private val breadcrumb: (String) -> Unit) : LogSink {
    override fun write(entry: LogEntry) {
        if (entry.level == LogLevel.DEBUG) return
        val error = entry.error?.let { " (${it.javaClass.simpleName})" }.orEmpty()
        breadcrumb("${entry.level.name.first()}/${logcatTag(entry.tag)}: ${entry.message}$error")
    }
}

internal fun logcatTag(tag: String): String = "VGL.$tag"
