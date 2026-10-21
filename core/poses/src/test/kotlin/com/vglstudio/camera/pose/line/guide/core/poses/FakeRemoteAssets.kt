package com.vglstudio.camera.pose.line.guide.core.poses

import com.vglstudio.camera.pose.line.guide.core.assets.AssetFailureReason
import com.vglstudio.camera.pose.line.guide.core.assets.AssetPath
import com.vglstudio.camera.pose.line.guide.core.assets.AssetResult
import com.vglstudio.camera.pose.line.guide.core.assets.RemoteAssets
import java.io.File
import kotlinx.coroutines.delay

/**
 * R2 files in memory. A path with an entry in [failures] is unavailable; [delays] makes a load take virtual time.
 * A path that loaded once is kept, like the real cache: later loads return at once and do not count as downloads.
 */
internal class FakeRemoteAssets(private val folder: File) : RemoteAssets {
    val failures = mutableMapOf<String, AssetFailureReason>()
    val delays = mutableMapOf<String, Long>()

    /** Text of a file, by path. Files without an entry contain their own path. */
    val contents = mutableMapOf<String, String>()

    /** Every call of [load], in order. */
    val calls = mutableListOf<String>()

    /** Number of completed downloads, by path. */
    val downloads = mutableMapOf<String, Int>()

    /** A path whose load throws an unexpected error. */
    var crashOn: String? = null

    var maxConcurrent = 0
        private set

    private val kept = mutableSetOf<String>()
    private var running = 0

    override suspend fun load(path: AssetPath): AssetResult {
        calls += path.value
        if (path.value == crashOn) throw IllegalStateException("unexpected")
        val file = File(folder, path.value)
        if (path.value in kept) return AssetResult.Ready(file)
        running++
        maxConcurrent = maxOf(maxConcurrent, running)
        try {
            delay(delays[path.value] ?: 0L)
        } finally {
            running--
        }
        failures[path.value]?.let { return AssetResult.Unavailable(it) }
        file.parentFile?.mkdirs()
        file.writeText(contents[path.value] ?: path.value)
        kept += path.value
        downloads[path.value] = (downloads[path.value] ?: 0) + 1
        return AssetResult.Ready(file)
    }
}
