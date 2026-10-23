package com.vglstudio.camera.pose.line.guide.feature.camera.photo

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** File names of saved photos: `CameraPose_<yyyyMMdd_HHmmss>.jpg` in the local time of the capture (question 4). */
internal object PhotoFileNames {
    private const val PREFIX = "CameraPose_"
    private const val EXTENSION = ".jpg"

    fun displayName(takenAtMillis: Long, zone: TimeZone): String {
        val format = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).apply { timeZone = zone }
        return PREFIX + format.format(Date(takenAtMillis)) + EXTENSION
    }

    /** [name] in [dir] when free, otherwise the first free `<name>_1.jpg`, `<name>_2.jpg`, and so on. */
    fun uniqueFile(dir: File, name: String): File {
        val first = File(dir, name)
        if (!first.exists()) return first
        val base = name.removeSuffix(EXTENSION)
        return generateSequence(1) { it + 1 }
            .map { File(dir, "${base}_$it$EXTENSION") }
            .first { !it.exists() }
    }
}
