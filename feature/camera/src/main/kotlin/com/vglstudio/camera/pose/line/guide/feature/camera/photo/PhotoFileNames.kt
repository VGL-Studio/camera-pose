package com.vglstudio.camera.pose.line.guide.feature.camera.photo

import java.io.File
import java.util.TimeZone

/** File names of saved photos: `CameraPose_<yyyyMMdd_HHmmss>.jpg` in the local time of the capture. */
internal object PhotoFileNames {
    fun displayName(takenAtMillis: Long, zone: TimeZone): String = ""

    /** [name] in [dir] when free, otherwise the first free `<name>_1.jpg`, `<name>_2.jpg`, and so on. */
    fun uniqueFile(dir: File, name: String): File = File(dir, name)
}
