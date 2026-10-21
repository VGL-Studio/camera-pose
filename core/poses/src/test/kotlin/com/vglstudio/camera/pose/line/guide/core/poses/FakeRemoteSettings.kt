package com.vglstudio.camera.pose.line.guide.core.poses

import com.vglstudio.camera.pose.line.guide.core.config.RemoteSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Remote settings held in memory: a key missing from [strings] has no remote value. */
internal class FakeRemoteSettings : RemoteSettings {
    val strings = MutableStateFlow<Map<String, String>>(emptyMap())

    override fun boolean(key: String, default: Boolean): Flow<Boolean> = flowOf(default)

    override fun long(key: String, default: Long): Flow<Long> = flowOf(default)

    override fun string(key: String, default: String): Flow<String> = strings.map { it[key] ?: default }
}
