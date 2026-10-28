package com.vglstudio.camera.pose.line.guide.core.poses.internal

/** Remote Config key of the catalog path (a string). */
internal const val CATALOG_PATH_KEY = "poses_catalog_path"

/**
 * Catalog of real poses on the staging bucket (the file Remote Config `poses_catalog_path` points to), used when
 * Remote Config has no valid path.
 */
internal const val DEFAULT_CATALOG_PATH = "poses/catalog/v3/catalog-512aa695.json"

/** A load that gets no answer within this time fails (ADR-1001). */
internal const val LOAD_TIMEOUT_MILLIS = 15_000L

internal const val RECENT_LIMIT = 20

internal const val STARTER_PARALLELISM = 4

internal const val LOG_TAG = "Poses"
