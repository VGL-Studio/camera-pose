package com.vglstudio.camera.pose.line.guide.core.network

import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlag
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlags
import com.vglstudio.camera.pose.line.guide.core.config.RemoteSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Network state held in memory; [collectors] tells whether the system callback would be registered. */
internal class FakeConnectivitySource(connected: Boolean = true) : ConnectivitySource {
    val connected = MutableStateFlow(connected)

    val collectors: Int get() = connected.subscriptionCount.value

    override fun isConnected(): Boolean = connected.value

    override fun connectivity(): Flow<Boolean> = connected
}

internal class FakeFeatureFlags(enabled: Set<FeatureFlag> = emptySet()) : FeatureFlags {
    val enabled = MutableStateFlow(enabled)

    override fun enabled(flag: FeatureFlag): Flow<Boolean> = enabled.map { flag in it }
}

/** A key missing from [longs] has no remote value, so the default is used. */
internal class FakeRemoteSettings : RemoteSettings {
    val longs = MutableStateFlow<Map<String, Long>>(emptyMap())

    override fun boolean(key: String, default: Boolean): Flow<Boolean> = MutableStateFlow(default)

    override fun long(key: String, default: Long): Flow<Long> = longs.map { it[key] ?: default }

    override fun string(key: String, default: String): Flow<String> = MutableStateFlow(default)
}
