package com.vglstudio.camera.pose.line.guide.core.poses.internal

/** Remote Config key of the catalog path (a string). */
internal const val CATALOG_PATH_KEY = "poses_catalog_path"

/** Sample catalog of the staging bucket, used when Remote Config has no valid path. */
internal const val DEFAULT_CATALOG_PATH = "poses/catalog/v1/catalog-00000000.json"

/** A load that gets no answer within this time fails (ADR-1001). */
internal const val LOAD_TIMEOUT_MILLIS = 15_000L

internal const val RECENT_LIMIT = 20

internal const val STARTER_PARALLELISM = 4

internal const val LOG_TAG = "Poses"
