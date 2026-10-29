package com.vglstudio.camera.pose.line.guide.config

import com.vglstudio.camera.pose.line.guide.core.config.RemoteKey

/** Every Remote Config key of the app, by owning module (ADR-0023). */
object AppRemoteKeys {
    val byModule: Map<String, List<RemoteKey<*>>> = linkedMapOf()
}
