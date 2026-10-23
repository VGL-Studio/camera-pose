package com.vglstudio.camera.pose.line.guide.feature.camera.photo

import android.system.ErrnoException
import android.system.OsConstants
import java.io.File
import java.io.IOException

/** The device gallery. Saved photos are addressed by their content uri, kept as a string. */
interface PhotoStore {
    /** Copies [file] into the app's own folder of the gallery. Throws [StorageFullException] or [IOException]. */
    suspend fun save(file: File, takenAtMillis: Long): String

    /** False when the photo was deleted from the gallery, or cannot be read. */
    suspend fun exists(uri: String): Boolean
}

class StorageFullException(cause: Throwable? = null) : IOException("No space left on device", cause)

/** True when [error] or one of its causes says the device storage is full (ENOSPC). */
fun isStorageFull(error: Throwable): Boolean {
    val seen = mutableSetOf<Throwable>()
    var current: Throwable? = error
    while (current != null && seen.add(current)) {
        if (current is StorageFullException) return true
        // Checked before the message: ErrnoException builds its message from the errno.
        if (current is ErrnoException) {
            if (current.errno == OsConstants.ENOSPC) return true
        } else if (current.message?.contains(ENOSPC_NAME) == true) {
            return true
        }
        current = current.cause
    }
    return false
}

private const val ENOSPC_NAME = "ENOSPC"
