package com.vglstudio.camera.pose.line.guide.core.poses

import com.vglstudio.camera.pose.line.guide.core.config.RemoteKey
import com.vglstudio.camera.pose.line.guide.core.poses.internal.CATALOG_PATH

/**
 * Remote Config keys of the poses module, listed by :app in AppRemoteKeys (ADR-0023). The keys stay internal to the
 * module; this object only hands the list out.
 */
object PosesRemoteKeys {
    fun all(): List<RemoteKey<*>> = listOf(CATALOG_PATH)
}
