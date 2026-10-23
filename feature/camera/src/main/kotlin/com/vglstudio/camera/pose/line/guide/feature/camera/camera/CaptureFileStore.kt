package com.vglstudio.camera.pose.line.guide.feature.camera.camera

import java.io.File

/**
 * Temporary folder for the photo just taken, until the review screen saves it (T-016). Each new capture deletes the
 * older files, so unsaved photos never pile up, also those left behind when the app was closed.
 */
class CaptureFileStore(private val dir: File) {
    fun newFile(nowMillis: Long): File {
        dir.mkdirs()
        dir.listFiles()?.forEach { it.delete() }
        return File(dir, "capture_$nowMillis.jpg")
    }

    /** Deletes [file] when it is in the capture folder; any other file is left alone. */
    fun delete(file: File) {
        if (file.parentFile?.canonicalFile == dir.canonicalFile) file.delete()
    }
}
