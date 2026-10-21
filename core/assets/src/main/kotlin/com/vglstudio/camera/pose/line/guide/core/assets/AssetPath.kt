package com.vglstudio.camera.pose.line.guide.core.assets

/**
 * Path of a static file on R2, relative to the base address of the environment (ADR-0006, ADR-0015).
 * Files on R2 are never overwritten, so a path must name one exact version of the content:
 * - a version folder: `images/v2/hero.webp`
 * - or a version or content hash in the file name: `images/hero.v2.webp`, `images/hero.3f2a9c1e.webp`
 *
 * A changed file gets a new path. The base address is never written in a feature: [RemoteAssets] adds it.
 */
@JvmInline
value class AssetPath(val value: String) {
    init {
        require(isValid(value)) {
            "Invalid asset path \"$value\": use a relative path with a version folder (v2/) " +
                "or a version or hash in the file name (name.v2.ext, name.3f2a9c1e.ext)"
        }
    }

    private companion object {
        // Starts with a letter or digit: no "." or "..", and no clash with the cache's own folders.
        val SEGMENT = Regex("[A-Za-z0-9][A-Za-z0-9._-]*")
        val VERSION_FOLDER = Regex("v\\d+(\\.\\d+)*")
        val VERSIONED_NAME = Regex(".+[._-](v\\d+(\\.\\d+)*|[0-9a-f]{8,})\\.[A-Za-z0-9]+")

        fun isValid(path: String): Boolean {
            val segments = path.split('/')
            if (segments.any { !SEGMENT.matches(it) }) return false
            return segments.dropLast(1).any { VERSION_FOLDER.matches(it) } || VERSIONED_NAME.matches(segments.last())
        }
    }
}
