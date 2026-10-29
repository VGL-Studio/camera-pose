package com.vglstudio.camera.pose.line.guide.core.poses

import com.vglstudio.camera.pose.line.guide.core.config.RemoteKey
import com.vglstudio.camera.pose.line.guide.core.config.RemoteSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Remote settings held in memory: a string key missing from [strings] has no remote value; other keys never do. */
internal class FakeRemoteSettings : RemoteSettings {
    val strings = MutableStateFlow<Map<String, String>>(emptyMap())

    // Safe cast: a STRING key is a RemoteKey<String> (factories of RemoteKey).
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> value(key: RemoteKey<T>): Flow<T> = when (key.type) {
        RemoteKey.Type.STRING -> strings.map { (it[key.name] ?: key.default) as T }
        else -> flowOf(key.default)
    }
}
