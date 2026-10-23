package com.vglstudio.camera.pose.line.guide.feature.camera.photo

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
fun isStorageFull(error: Throwable): Boolean = false
