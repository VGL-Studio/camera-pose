package com.vglstudio.camera.pose.line.guide.feature.camera.camera

import java.io.File

/** Temporary folder for the photo just taken, until the review screen saves it (T-016). */
class CaptureFileStore(private val dir: File) {
    fun newFile(nowMillis: Long): File = File(dir, "capture_$nowMillis.jpg")
}
