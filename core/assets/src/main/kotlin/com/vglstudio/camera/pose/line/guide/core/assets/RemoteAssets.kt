package com.vglstudio.camera.pose.line.guide.core.assets

import java.io.File

/**
 * Static files stored on R2 (ADR-0006). Features use this, never a URL or an HTTP client.
 *
 * A file is downloaded once and kept on the device, so it is available offline afterwards.
 * Files never change on R2, so a kept copy is never checked again.
 */
interface RemoteAssets {
    /**
     * Returns the local copy of [path], downloading it first if needed. Never throws for network errors:
     * call it from a ViewModel and put [AssetResult.Unavailable] into the UI state, with a way to retry.
     */
    suspend fun load(path: AssetPath): AssetResult
}

sealed interface AssetResult {
    /** The file is on the device. Read it, do not modify or delete it. */
    data class Ready(val file: File) : AssetResult

    /** Not on the device yet and could not be downloaded (offline, file missing on R2). Retrying may succeed. */
    data object Unavailable : AssetResult
}

/** Where the files of the current environment live, given by :app per build flavor (ADR-0007). */
class AssetsConfig(baseUrl: String) {
    /** Base address, always ending with a slash. */
    val baseUrl: String = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

    init {
        require(baseUrl.startsWith("https://")) { "Assets base URL must use https: \"$baseUrl\"" }
    }

    fun urlOf(path: AssetPath): String = baseUrl + path.value
}
