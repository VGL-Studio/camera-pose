package com.vglstudio.camera.pose.line.guide.core.poses.internal

import com.vglstudio.camera.pose.line.guide.core.config.RemoteKey

/** Remote Config key of the catalog path (a string). */
internal const val CATALOG_PATH_KEY = "poses_catalog_path"

/**
 * Catalog of real poses on the staging bucket (the file Remote Config `poses_catalog_path` points to), used when
 * Remote Config has no valid path.
 */
internal const val DEFAULT_CATALOG_PATH = "poses/catalog/v3/catalog-512aa695.json"

/** The Remote Config key of the catalog path, built from the two constants above (ADR-0023). */
internal val CATALOG_PATH: RemoteKey<String> = RemoteKey.string(
    CATALOG_PATH_KEY,
    DEFAULT_CATALOG_PATH,
    "Path on R2 of the pose catalog; an invalid path falls back to the default.",
)

/** A load that gets no answer within this time fails (ADR-1001). */
internal const val LOAD_TIMEOUT_MILLIS = 15_000L

internal const val RECENT_LIMIT = 20

internal const val STARTER_PARALLELISM = 4

internal const val LOG_TAG = "Poses"
